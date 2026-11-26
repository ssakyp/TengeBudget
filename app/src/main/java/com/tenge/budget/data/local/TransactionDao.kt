package com.tenge.budget.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query(
        """
        SELECT t.*, a.name AS accountName, a.currency AS currency, ta.name AS targetAccountName, c.name AS categoryName
        FROM transactions t
        INNER JOIN accounts a ON a.id = t.accountId
        LEFT JOIN accounts ta ON ta.id = t.targetAccountId
        LEFT JOIN categories c ON c.id = t.categoryId
        ORDER BY t.date DESC, t.id DESC
        """
    )
    fun observeAll(): Flow<List<TransactionWithInfo>>

    @Query(
        """
        SELECT t.*, a.name AS accountName, a.currency AS currency, ta.name AS targetAccountName, c.name AS categoryName
        FROM transactions t
        INNER JOIN accounts a ON a.id = t.accountId
        LEFT JOIN accounts ta ON ta.id = t.targetAccountId
        LEFT JOIN categories c ON c.id = t.categoryId
        WHERE t.id = :id
        """
    )
    fun observeById(id: Long): Flow<TransactionWithInfo?>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)
}
