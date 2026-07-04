with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if line.strip() == '"HOME" -> {':
        home_index = i
        break

# check lines around 100 for duplicates
start_index = 0
for i, line in enumerate(lines):
    if line.strip() == '@OptIn(ExperimentalMaterial3Api::class)':
        start_index = i
        break

lines[start_index] = ''
lines[start_index+1] = ''

lines.insert(home_index, '        when (selectedTab) {\n')

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.writelines(lines)
