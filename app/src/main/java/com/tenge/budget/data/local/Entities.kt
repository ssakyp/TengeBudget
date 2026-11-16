package com.tenge.budget.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tenge.budget.data.ContributionKind
import com.tenge.budget.data.GoalStatus
import com.tenge.budget.data.TxType
import java.time.LocalDate

/** A place money lives (bank card, cash, brokerage…). Its balance changes only through transactions. */
@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val balance: Double = 0.0,
    /** Three-letter code, e.g. KZT. */
    val currency: String = "KZT"
)

/** User-managed category; [type] is INCOME or EXPENSE. */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TxType
)

/**
 * Every transaction belongs to an account ([accountId]). A TRANSFER also has a
 * [targetAccountId]; INCOME/EXPENSE may have a [categoryId]. Foreign keys are
 * RESTRICT: an account or category that is still referenced can't be deleted.
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(entity = Account::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Account::class, parentColumns = ["id"], childColumns = ["targetAccountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Category::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("accountId"), Index("targetAccountId"), Index("categoryId")]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val categoryId: Long? = null,
    val targetAccountId: Long? = null,
    val title: String,
    val amount: Double,
    val type: TxType,
    val date: LocalDate = LocalDate.now(),
    val note: String = ""
)

/** Monthly spending limit for one EXPENSE category; unique per (category, month, year). */
@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(entity = Category::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index(value = ["categoryId", "month", "year"], unique = true)]
)
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val limitAmount: Double,
    val month: Int,
    val year: Int
)

/** A user-defined savings goal. The saved amount is NOT stored: it is the sum of its [Contribution]s. */
@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val createdAt: LocalDate = LocalDate.now()
)

/** One entry in a goal's history. Deleting a goal deletes its history too. */
@Entity(
    tableName = "contributions",
    foreignKeys = [
        ForeignKey(entity = SavingsGoal::class, parentColumns = ["id"], childColumns = ["goalId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("goalId")]
)
data class Contribution(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val kind: ContributionKind,
    val amount: Double,
    /** Epoch millis — used for ordering and for display. */
    val timestamp: Long = System.currentTimeMillis()
)

/** Query result: a transaction joined with the names it refers to. */
data class TransactionWithInfo(
    @Embedded val transaction: Transaction,
    @ColumnInfo(name = "accountName") val accountName: String,
    @ColumnInfo(name = "currency") val currency: String,
    @ColumnInfo(name = "targetAccountName") val targetAccountName: String?,
    @ColumnInfo(name = "categoryName") val categoryName: String?
)

/** Query result: a budget plus its category's name. */
data class BudgetWithCategory(
    @Embedded val budget: Budget,
    @ColumnInfo(name = "categoryName") val categoryName: String
)

/** Query result: a goal plus how much has been saved into it so far. */
data class GoalWithProgress(
    @Embedded val goal: SavingsGoal,
    @ColumnInfo(name = "savedAmount") val savedAmount: Double
) {
    val remaining: Double get() = (goal.targetAmount - savedAmount).coerceAtLeast(0.0)
    val fraction: Float
        get() = if (goal.targetAmount <= 0.0) 0f else (savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
}
