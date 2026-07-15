package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Account
import com.example.data.Transaction
import com.example.data.IncomeType
import com.example.data.ExpenseType
import com.example.data.Client
import com.example.data.ClientOperation
import com.example.data.ProfitDeduction
import com.example.data.TransactionRepository
import com.example.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModel(private val repository: TransactionRepository) : ViewModel() {

    val allAccounts: StateFlow<List<Account>> = repository.allAccounts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedAccountId = MutableStateFlow<Long?>(null)
    val selectedAccountId: StateFlow<Long?> = _selectedAccountId.asStateFlow()

    // Dynamic ID of the active account (user selected OR first in list)
    val activeAccountId: StateFlow<Long?> = combine(allAccounts, _selectedAccountId) { accounts, selectedId ->
        selectedId ?: accounts.firstOrNull()?.id
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val currentAccount: StateFlow<Account?> = combine(allAccounts, activeAccountId) { accounts, activeId ->
        accounts.find { it.id == activeId }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Load transactions for the currently active account

    val allProfitDeductions: StateFlow<List<ProfitDeduction>> = activeAccountId
        .flatMapLatest { id ->
            if (id != null) repository.getProfitDeductionsForAccount(id) else flowOf(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    val allTransactions: StateFlow<List<Transaction>> = activeAccountId
        .flatMapLatest { id ->
            if (id != null) repository.getTransactionsForAccount(id) else flowOf(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Load income types for the currently active account
    val incomeTypes: StateFlow<List<IncomeType>> = activeAccountId
        .flatMapLatest { id ->
            if (id != null) repository.getIncomeTypesForAccount(id) else flowOf(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Load expense types for the currently active account
    val expenseTypes: StateFlow<List<ExpenseType>> = activeAccountId
        .flatMapLatest { id ->
            if (id != null) repository.getExpenseTypesForAccount(id) else flowOf(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Load clients for the currently active account
    val clients: StateFlow<List<Client>> = activeAccountId
        .flatMapLatest { id ->
            if (id != null) repository.getClientsForAccount(id) else flowOf(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allClientOperations: StateFlow<List<ClientOperation>> = activeAccountId
        .flatMapLatest { id ->
            if (id != null) repository.getClientOperationsForAccount(id) else flowOf(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalIncome: StateFlow<Double> = activeAccountId
        .flatMapLatest { id ->
            if (id != null) repository.getTotalIncomeForAccount(id) else flowOf(null)
        }
        .map { it ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val totalExpenses: StateFlow<Double> = activeAccountId
        .flatMapLatest { id ->
            if (id != null) repository.getTotalExpensesForAccount(id) else flowOf(null)
        }
        .map { it ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    val currentBalance: StateFlow<Double> = combine(totalIncome, totalExpenses) { income, expenses ->
        income - expenses
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    // Ensure we have at least one default account when DB is empty
    init {
        viewModelScope.launch {
            repository.allAccounts.first { true }.let { accounts ->
                if (accounts.isEmpty()) {
                    addAccount("الحساب الرئيسي")
                }
            }
            try {
                // Auto-repair existing IncomeTypes and corresponding Transactions
                // whose timestamps do not match the parsed date in name (which is the actual date, e.g. "2026/07/10")
                val incomes = repository.getAllIncomeTypesDirect()
                val txs = repository.getAllTransactionsDirect().associateBy { it.id }
                incomes.forEach { income ->
                    val parsedTimestamp = DateUtils.parseLocal(income.name)
                    val isIncomeTimeMismatch = java.lang.Math.abs(income.timestamp - parsedTimestamp) > 60000
                    val matchingTx = income.transactionId?.let { txs[it] }
                    val isTxTimeMismatch = matchingTx != null && java.lang.Math.abs(matchingTx.timestamp - parsedTimestamp) > 60000

                    if (isIncomeTimeMismatch || isTxTimeMismatch) {
                        repository.updateIncomeType(income.copy(timestamp = parsedTimestamp))
                        if (matchingTx != null) {
                            repository.update(matchingTx.copy(timestamp = parsedTimestamp))
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore any database check error on startup
            }
        }
    }

    fun selectAccount(accountId: Long) {
        _selectedAccountId.value = accountId
    }

    fun addAccount(name: String) {
        viewModelScope.launch {
            val newId = repository.insertAccount(Account(name = name))
            _selectedAccountId.value = newId
        }
    }

    fun updateAccountName(account: Account, newName: String) {
        viewModelScope.launch {
            repository.updateAccount(account.copy(name = newName))
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            repository.deleteAccount(account)
            if (_selectedAccountId.value == account.id) {
                _selectedAccountId.value = null
            }
        }
    }

    fun clearAccountData(account: Account) {
        viewModelScope.launch {
            repository.clearAccountData(account.id)
        }
    }

    fun addTransaction(title: String, amount: Double, type: String, category: String, notes: String = "", timestamp: Long = System.currentTimeMillis()) {
        val accountId = activeAccountId.value ?: return
        viewModelScope.launch {
            val transaction = Transaction(
                accountId = accountId,
                title = title,
                amount = amount,
                type = type,
                category = category,
                notes = notes,
                timestamp = timestamp
            )
            repository.insert(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteIncomeTypeByTransactionId(transaction.id)
            repository.delete(transaction)
        }
    }

    fun deleteTransactionById(id: Long) {
        viewModelScope.launch {
            repository.deleteIncomeTypeByTransactionId(id)
            repository.deleteById(id)
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.update(transaction)
        }
    }

    // Income Type operations
    fun addIncomeType(name: String, consumedBags: Int, amount: Double, notes: String) {
        val accountId = activeAccountId.value ?: return
        val parsedTimestamp = DateUtils.parseLocal(name)
        viewModelScope.launch {
            val transactionId = repository.insert(
                Transaction(
                    accountId = accountId,
                    title = "إيراد يوم $name",
                    amount = amount,
                    type = "INCOME",
                    category = "شوالات: $consumedBags",
                    notes = notes,
                    timestamp = parsedTimestamp
                )
            )
            repository.insertIncomeType(
                IncomeType(
                    accountId = accountId,
                    name = name,
                    consumedBags = consumedBags,
                    amount = amount,
                    notes = notes,
                    transactionId = transactionId,
                    timestamp = parsedTimestamp
                )
            )
        }
    }

    fun updateIncomeType(incomeType: IncomeType, newName: String, newConsumedBags: Int, newAmount: Double, newNotes: String) {
        val accountId = activeAccountId.value ?: return
        val parsedTimestamp = DateUtils.parseLocal(newName)
        viewModelScope.launch {
            val tId = incomeType.transactionId
            if (tId != null) {
                repository.update(
                    Transaction(
                        id = tId,
                        accountId = accountId,
                        title = "إيراد يوم $newName",
                        amount = newAmount,
                        type = "INCOME",
                        category = "شوالات: $newConsumedBags",
                        notes = newNotes,
                        timestamp = parsedTimestamp
                    )
                )
                repository.updateIncomeType(
                    incomeType.copy(
                        name = newName,
                        consumedBags = newConsumedBags,
                        amount = newAmount,
                        notes = newNotes,
                        timestamp = parsedTimestamp
                    )
                )
            } else {
                val newTId = repository.insert(
                    Transaction(
                        accountId = accountId,
                        title = "إيراد يوم $newName",
                        amount = newAmount,
                        type = "INCOME",
                        category = "شوالات: $newConsumedBags",
                        notes = newNotes,
                        timestamp = parsedTimestamp
                    )
                )
                repository.updateIncomeType(
                    incomeType.copy(
                        name = newName,
                        consumedBags = newConsumedBags,
                        amount = newAmount,
                        notes = newNotes,
                        transactionId = newTId,
                        timestamp = parsedTimestamp
                    )
                )
            }
        }
    }

    fun deleteIncomeType(incomeType: IncomeType) {
        viewModelScope.launch {
            val tId = incomeType.transactionId
            if (tId != null) {
                repository.deleteById(tId)
            }
            repository.deleteIncomeType(incomeType)
        }
    }

    // Expense Type operations
    fun addExpenseType(name: String) {
        val accountId = activeAccountId.value ?: return
        viewModelScope.launch {
            repository.insertExpenseType(ExpenseType(accountId = accountId, name = name))
        }
    }

    fun updateExpenseType(expenseType: ExpenseType, newName: String) {
        viewModelScope.launch {
            repository.updateExpenseType(expenseType.copy(name = newName), expenseType.name)
        }
    }

    fun deleteExpenseType(expenseType: ExpenseType) {
        viewModelScope.launch {
            repository.deleteExpenseType(expenseType)
        }
    }

    // Client operations
    fun addClient(name: String, linkedExpenseCategory: String) {
        val accountId = activeAccountId.value ?: return
        viewModelScope.launch {
            repository.insertClient(Client(accountId = accountId, name = name, linkedExpenseCategory = linkedExpenseCategory))
        }
    }

    fun updateClient(client: Client, newName: String, newLinkedExpenseCategory: String) {
        viewModelScope.launch {
            repository.updateClient(client.copy(name = newName, linkedExpenseCategory = newLinkedExpenseCategory))
        }
    }

    fun deleteClient(client: Client) {
        viewModelScope.launch {
            repository.deleteClient(client)
        }
    }

    // Client operations
    fun getClientOperations(clientId: Long): Flow<List<ClientOperation>> {
        return repository.getOperationsForClient(clientId)
    }

    fun addClientOperation(clientId: Long, type: String, amount: Double, title: String) {
        viewModelScope.launch {
            repository.insertClientOperation(
                ClientOperation(
                    clientId = clientId,
                    type = type,
                    amount = amount,
                    title = title
                )
            )
        }
    }

    fun updateClientOperation(operation: ClientOperation, newType: String, newAmount: Double, newTitle: String) {
        viewModelScope.launch {
            repository.updateClientOperation(
                operation.copy(
                    type = newType,
                    amount = newAmount,
                    title = newTitle
                )
            )
        }
    }

    fun deleteClientOperation(operation: ClientOperation) {
        viewModelScope.launch {
            repository.deleteClientOperation(operation)
        }
    }


    // Profit Deduction operations
    fun addProfitDeduction(accountId: Long, amount: Double, title: String) {
        viewModelScope.launch {
            repository.insertProfitDeduction(
                ProfitDeduction(
                    accountId = accountId,
                    amount = amount,
                    title = title,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateProfitDeduction(deduction: ProfitDeduction, newAmount: Double, newTitle: String) {
        viewModelScope.launch {
            repository.updateProfitDeduction(
                deduction.copy(
                    amount = newAmount,
                    title = newTitle
                )
            )
        }
    }

    fun deleteProfitDeduction(deduction: ProfitDeduction) {
        viewModelScope.launch {
            repository.deleteProfitDeduction(deduction)
        }
    }

}

class TransactionViewModelFactory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
