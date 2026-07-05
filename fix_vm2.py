with open('app/src/main/java/com/example/ui/TransactionViewModel.kt', 'r') as f:
    text = f.read()

text = text.replace("}\n}\n    // Profit Deduction operations", "}\n    // Profit Deduction operations")

with open('app/src/main/java/com/example/ui/TransactionViewModel.kt', 'w') as f:
    f.write(text)
