package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    // Accounts operations
    @Query("SELECT * FROM accounts ORDER BY timestamp ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Update
    suspend fun updateAccount(account: Account)

    @Delete
    suspend fun deleteAccount(account: Account)

    @Query("DELETE FROM transactions WHERE accountId = :accountId")
    suspend fun deleteTransactionsByAccountId(accountId: Long)

    // Transactions operations (filtered by account)
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY timestamp DESC")
    fun getTransactionsByAccount(accountId: Long): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("SELECT SUM(amount) FROM transactions WHERE accountId = :accountId AND type = 'INCOME'")
    fun getTotalIncomeByAccount(accountId: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE accountId = :accountId AND type = 'EXPENSE'")
    fun getTotalExpensesByAccount(accountId: Long): Flow<Double?>

    // Income types operations
    @Query("SELECT * FROM income_types WHERE accountId = :accountId ORDER BY timestamp ASC")
    fun getIncomeTypesByAccount(accountId: Long): Flow<List<IncomeType>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomeType(incomeType: IncomeType): Long

    @Update
    suspend fun updateIncomeType(incomeType: IncomeType)

    @Delete
    suspend fun deleteIncomeType(incomeType: IncomeType)

    @Query("DELETE FROM income_types WHERE accountId = :accountId")
    suspend fun deleteIncomeTypesByAccountId(accountId: Long)

    @Query("DELETE FROM income_types WHERE transactionId = :transactionId")
    suspend fun deleteIncomeTypeByTransactionId(transactionId: Long)

    // Expense types operations
    @Query("SELECT * FROM expense_types WHERE accountId = :accountId ORDER BY timestamp ASC")
    fun getExpenseTypesByAccount(accountId: Long): Flow<List<ExpenseType>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseType(expenseType: ExpenseType): Long

    @Update
    suspend fun updateExpenseType(expenseType: ExpenseType)

    @Delete
    suspend fun deleteExpenseType(expenseType: ExpenseType)

    @Query("DELETE FROM expense_types WHERE accountId = :accountId")
    suspend fun deleteExpenseTypesByAccountId(accountId: Long)
}
