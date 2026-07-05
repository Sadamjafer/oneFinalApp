import re

with open('app/src/main/java/com/example/ui/TransactionViewModel.kt', 'r') as f:
    text = f.read()

funcs = """
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
"""

text = text.replace(funcs, '')

# insert before class TransactionViewModelFactory
insert_pos = text.find('class TransactionViewModelFactory')
if insert_pos != -1:
    text = text[:insert_pos-1] + funcs + "\n}\n\n" + text[insert_pos:]

with open('app/src/main/java/com/example/ui/TransactionViewModel.kt', 'w') as f:
    f.write(text)
