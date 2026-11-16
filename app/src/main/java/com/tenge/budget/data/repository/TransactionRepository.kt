package com.tenge.budget.data.repository

import androidx.room.withTransaction
import com.tenge.budget.data.TxType
import com.tenge.budget.data.local.AppDatabase
import com.tenge.budget.data.local.Transaction
import com.tenge.budget.data.local.TransactionWithInfo
import com.tenge.budget.util.round2
import kotlinx.coroutines.flow.Flow

private class Rejection(message: String) : Exception(message)

private fun reject(message: String): Nothing = throw Rejection(message)

/**
 * Transactions are the only thing that changes account balances:
 *  - INCOME   adds to the account,
 *  - EXPENSE  subtracts from it,
 *  - TRANSFER subtracts from the source and adds to the destination.
 * Editing or deleting reverses the old effect first. Everything runs in one
 * Room transaction, and a broken rule (e.g. a balance going negative) throws,
 * which rolls the whole thing back — so balances can never end up half-updated.
 */
class TransactionRepository(private val db: AppDatabase) {

    private val dao = db.transactionDao()
    private val accounts = db.accountDao()
    private val categories = db.categoryDao()

    fun observeAll(): Flow<List<TransactionWithInfo>> = dao.observeAll()
    fun observe(id: Long): Flow<TransactionWithInfo?> = dao.observeById(id)
    suspend fun get(id: Long): Transaction? = dao.getById(id)

    suspend fun add(transaction: Transaction): OpResult = atomically {
        val clean = validate(transaction)
        applyEffect(clean, +1)
        dao.insert(clean)
    }

    suspend fun update(transaction: Transaction): OpResult = atomically {
        val old = dao.getById(transaction.id) ?: reject("This transaction no longer exists.")
        applyEffect(old, -1)
        val clean = validate(transaction)
        applyEffect(clean, +1)
        dao.update(clean)
    }

    suspend fun delete(transaction: Transaction): OpResult = atomically {
        val old = dao.getById(transaction.id) ?: reject("This transaction no longer exists.")
        applyEffect(old, -1)
        dao.delete(old)
    }

    /** Puts a deleted transaction back (with its original id) and re-applies its effect — used by "Undo". */
    suspend fun restore(transaction: Transaction): OpResult = add(transaction)

    private suspend fun atomically(block: suspend () -> Unit): OpResult =
        try {
            db.withTransaction { block() }
            OpResult.Success
        } catch (e: Rejection) {
            OpResult.Rejected(e.message ?: "Couldn't save the transaction.")
        }

    private suspend fun validate(tx: Transaction): Transaction {
        if (tx.title.isBlank()) reject("Title is required.")
        if (tx.amount <= 0.0) reject("Amount must be greater than zero.")
        val source = accounts.getById(tx.accountId) ?: reject("The selected account no longer exists.")

        if (tx.type == TxType.TRANSFER) {
            if (tx.categoryId != null) reject("Transfers can't have a category.")
            val targetId = tx.targetAccountId ?: reject("Choose a destination account.")
            if (targetId == tx.accountId) reject("Source and destination accounts must be different.")
            val target = accounts.getById(targetId) ?: reject("The destination account no longer exists.")
            if (target.currency != source.currency) reject("Transfers between different currencies aren't supported.")
            return tx
        }

        val categoryId = tx.categoryId
        if (categoryId != null) {
            val category = categories.getById(categoryId) ?: reject("The selected category no longer exists.")
            if (category.type != tx.type) {
                reject("\"${category.name}\" is a ${category.type.label.lowercase()} category.")
            }
        }
        return tx.copy(targetAccountId = null)
    }

    /** sign = +1 applies the transaction to balances, -1 reverses it. Balances may never go below zero. */
    private suspend fun applyEffect(tx: Transaction, sign: Int) {
        val deltas = linkedMapOf<Long, Double>()
        fun add(accountId: Long, delta: Double) { deltas[accountId] = (deltas[accountId] ?: 0.0) + delta }

        when (tx.type) {
            TxType.INCOME -> add(tx.accountId, sign * tx.amount)
            TxType.EXPENSE -> add(tx.accountId, -sign * tx.amount)
            TxType.TRANSFER -> {
                add(tx.accountId, -sign * tx.amount)
                add(tx.targetAccountId ?: reject("Choose a destination account."), sign * tx.amount)
            }
        }

        for ((accountId, delta) in deltas) {
            val account = accounts.getById(accountId) ?: reject("An account used by this transaction no longer exists.")
            val newBalance = round2(account.balance + delta)
            if (newBalance < 0.0) reject("Not enough balance in \"${account.name}\".")
            accounts.update(account.copy(balance = newBalance))
        }
    }
}
