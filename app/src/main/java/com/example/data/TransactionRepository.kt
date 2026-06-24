package com.example.data

import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {
    
    // Accounts Operations
    val allAccounts: Flow<List<Account>> = transactionDao.getAllAccounts()

    suspend fun insertAccount(account: Account): Long {
        return transactionDao.insertAccount(account)
    }

    suspend fun updateAccount(account: Account) {
        transactionDao.updateAccount(account)
    }

    suspend fun deleteAccount(account: Account) {
        transactionDao.deleteAccount(account)
        transactionDao.deleteTransactionsByAccountId(account.id)
        transactionDao.deleteIncomeTypesByAccountId(account.id)
        transactionDao.deleteExpenseTypesByAccountId(account.id)
    }

    // Income Types Operations
    fun getIncomeTypesForAccount(accountId: Long): Flow<List<IncomeType>> {
        return transactionDao.getIncomeTypesByAccount(accountId)
    }

    suspend fun insertIncomeType(incomeType: IncomeType): Long {
        return transactionDao.insertIncomeType(incomeType)
    }

    suspend fun updateIncomeType(incomeType: IncomeType) {
        transactionDao.updateIncomeType(incomeType)
    }

    suspend fun deleteIncomeType(incomeType: IncomeType) {
        transactionDao.deleteIncomeType(incomeType)
    }

    suspend fun deleteIncomeTypeByTransactionId(transactionId: Long) {
        transactionDao.deleteIncomeTypeByTransactionId(transactionId)
    }

    // Expense Types Operations
    fun getExpenseTypesForAccount(accountId: Long): Flow<List<ExpenseType>> {
        return transactionDao.getExpenseTypesByAccount(accountId)
    }

    suspend fun insertExpenseType(expenseType: ExpenseType): Long {
        return transactionDao.insertExpenseType(expenseType)
    }

    suspend fun updateExpenseType(expenseType: ExpenseType) {
        transactionDao.updateExpenseType(expenseType)
    }

    suspend fun deleteExpenseType(expenseType: ExpenseType) {
        transactionDao.deleteExpenseType(expenseType)
    }

    // Transactions Operations (by account id)
    fun getTransactionsForAccount(accountId: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByAccount(accountId)
    }

    fun getTotalIncomeForAccount(accountId: Long): Flow<Double?> {
        return transactionDao.getTotalIncomeByAccount(accountId)
    }

    fun getTotalExpensesForAccount(accountId: Long): Flow<Double?> {
        return transactionDao.getTotalExpensesByAccount(accountId)
    }

    suspend fun insert(transaction: Transaction): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun update(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun delete(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteById(id: Long) {
        transactionDao.deleteTransactionById(id)
    }
}
