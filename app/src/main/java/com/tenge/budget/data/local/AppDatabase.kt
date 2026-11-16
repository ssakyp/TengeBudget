package com.tenge.budget.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Account::class, Category::class, Transaction::class, Budget::class, SavingsGoal::class, Contribution::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun transactionDao(): TransactionDao
    abstract fun goalDao(): GoalDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "tenge_budget.db")
                // Version 1 (pre-accounts) only existed during development; rebuild rather than migrate.
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        seed(db)
                    }
                })
                .build()

        /** Starter categories and two accounts, inserted once when the database file is first created. */
        private fun seed(db: SupportSQLiteDatabase) {
            val income = listOf("Salary", "Freelance & Consulting", "Investments & Dividends", "Cashback & Rewards")
            val expense = listOf(
                "Groceries & Supermarket", "Restaurants & Dining", "Housing & Rent", "Utilities & Internet",
                "Transport & Auto", "Shopping & Retail", "Subscriptions & Software", "Education & Courses",
                "Healthcare & Pharmacy", "Entertainment & Leisure", "Family & Childcare"
            )
            income.forEach { db.execSQL("INSERT INTO categories (name, type) VALUES (?, 'INCOME')", arrayOf<Any>(it)) }
            expense.forEach { db.execSQL("INSERT INTO categories (name, type) VALUES (?, 'EXPENSE')", arrayOf<Any>(it)) }
            db.execSQL("INSERT INTO accounts (name, balance, currency) VALUES ('Cash', 45000.0, 'KZT')")
            db.execSQL("INSERT INTO accounts (name, balance, currency) VALUES ('Kaspi Gold', 345000.0, 'KZT')")
        }
    }
}
