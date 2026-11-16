package com.tenge.budget.data.repository

import com.tenge.budget.data.local.Account
import com.tenge.budget.data.local.AccountDao
import com.tenge.budget.util.round2
import kotlinx.coroutines.flow.Flow

class AccountRepository(private val dao: AccountDao) {

    fun observeAll(): Flow<List<Account>> = dao.observeAll()
    fun observe(id: Long): Flow<Account?> = dao.observeById(id)
    suspend fun get(id: Long): Account? = dao.getById(id)

    /** The opening balance is set only here; afterwards the balance changes through transactions. */
    suspend fun create(name: String, openingBalance: Double, currency: String): OpResult {
        dao.insert(Account(name = name.trim(), balance = round2(openingBalance), currency = currency.uppercase()))
        return OpResult.Success
    }

    suspend fun update(id: Long, name: String, currency: String): OpResult {
        val account = dao.getById(id) ?: return OpResult.Rejected("This account no longer exists.")
        val newCurrency = currency.uppercase()
        if (newCurrency != account.currency && dao.countTransactions(id) > 0) {
            return OpResult.Rejected("Currency can't be changed once the account has transactions.")
        }
        dao.update(account.copy(name = name.trim(), currency = newCurrency))
        return OpResult.Success
    }

    suspend fun delete(id: Long): OpResult {
        val account = dao.getById(id) ?: return OpResult.Success
        val used = dao.countTransactions(id)
        if (used > 0) {
            return OpResult.Rejected("\"${account.name}\" has $used transaction(s). Delete or edit them first.")
        }
        dao.delete(account)
        return OpResult.Success
    }
}
