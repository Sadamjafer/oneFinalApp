with open('app/src/main/java/com/example/ui/TransactionViewModel.kt', 'r') as f:
    text = f.read()

# find deleteClientOperation
idx = text.find('fun deleteClientOperation(operation: ClientOperation) {')
if idx != -1:
    end_idx = text.find('}\n}', idx)
    if end_idx != -1:
        # replace '}\n}' with '}\n'
        text = text[:end_idx] + '}\n' + text[end_idx+3:]

with open('app/src/main/java/com/example/ui/TransactionViewModel.kt', 'w') as f:
    f.write(text)
