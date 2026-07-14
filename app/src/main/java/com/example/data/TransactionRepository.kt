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

    suspend fun getAllIncomeTypesDirect(): List<IncomeType> {
        return transactionDao.getAllIncomeTypesDirect()
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

    suspend fun updateExpenseType(expenseType: ExpenseType, oldName: String) {
        transactionDao.updateExpenseType(expenseType)
        if (oldName != expenseType.name) {
            transactionDao.updateTransactionCategoryName(expenseType.accountId, oldName, expenseType.name)
            transactionDao.updateClientLinkedCategory(expenseType.accountId, oldName, expenseType.name)
        }
    }

    suspend fun deleteExpenseType(expenseType: ExpenseType) {
        transactionDao.deleteExpenseType(expenseType)
    }

    // Transactions Operations (by account id)
    fun getTransactionsForAccount(accountId: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByAccount(accountId)
    }

    suspend fun getAllTransactionsDirect(): List<Transaction> {
        return transactionDao.getAllTransactionsDirect()
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

    // Clients Operations
    fun getClientsForAccount(accountId: Long): Flow<List<Client>> {
        return transactionDao.getClientsByAccount(accountId)
    }

    suspend fun insertClient(client: Client): Long {
        return transactionDao.insertClient(client)
    }

    suspend fun updateClient(client: Client) {
        transactionDao.updateClient(client)
    }

    suspend fun deleteClient(client: Client) {
        transactionDao.deleteClient(client)
    }

    // Client Operations operations
    fun getOperationsForClient(clientId: Long): Flow<List<ClientOperation>> {
        return transactionDao.getOperationsByClient(clientId)
    }

    fun getClientOperationsForAccount(accountId: Long): Flow<List<ClientOperation>> {
        return transactionDao.getClientOperationsForAccount(accountId)
    }

    suspend fun insertClientOperation(operation: ClientOperation): Long {
        return transactionDao.insertClientOperation(operation)
    }

    suspend fun updateClientOperation(operation: ClientOperation) {
        transactionDao.updateClientOperation(operation)
    }

    suspend fun deleteClientOperation(operation: ClientOperation) {
        transactionDao.deleteClientOperation(operation)
    }

    // Profit Deductions Operations
    fun getProfitDeductionsForAccount(accountId: Long): Flow<List<ProfitDeduction>> {
        return transactionDao.getProfitDeductionsByAccount(accountId)
    }

    suspend fun insertProfitDeduction(deduction: ProfitDeduction): Long {
        return transactionDao.insertProfitDeduction(deduction)
    }

    suspend fun updateProfitDeduction(deduction: ProfitDeduction) {
        transactionDao.updateProfitDeduction(deduction)
    }

    suspend fun deleteProfitDeduction(deduction: ProfitDeduction) {
        transactionDao.deleteProfitDeduction(deduction)
    }
}
