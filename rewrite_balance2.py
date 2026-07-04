import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    text = f.read()

good_part = """@Composable
fun BalanceCard(
    balance: Double,
    income: Double,
    expenses: Double
) {
    val formatter = androidx.compose.runtime.remember { java.text.DecimalFormat("#,##0.00") }
    val formattedBalance = formatter.format(balance)
    val formattedIncome = formatter.format(income)
    val formattedExpenses = formatter.format(expenses)

    androidx.compose.material3.Card(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp), spotColor = androidx.compose.material3.MaterialTheme.colorScheme.primary),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = androidx.compose.ui.Modifier
                .padding(28.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "الرصيد الإجمالي المتبقي",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
            Text(
                text = "$formattedBalance ج.س",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 38.sp,
                letterSpacing = (-1).sp,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = androidx.compose.ui.Modifier.height(28.dp))
            Row(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Income
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = androidx.compose.ui.Modifier
                                .size(24.dp)
                                .background(androidx.compose.ui.graphics.Color(0xFFD1FAE5), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Income",
                                tint = androidx.compose.ui.graphics.Color(0xFF047857),
                                modifier = androidx.compose.ui.Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                        Text(
                            text = "الدخل",
                            fontSize = 12.sp,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
                    Text(
                        text = "$formattedIncome ج.س",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // Expenses
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "المنصرف",
                            fontSize = 12.sp,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                        Box(
                            modifier = androidx.compose.ui.Modifier
                                .size(24.dp)
                                .background(androidx.compose.ui.graphics.Color(0xFFFFE4E6), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = "Expense",
                                tint = androidx.compose.ui.graphics.Color(0xFFBE123C),
                                modifier = androidx.compose.ui.Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
                    Text(
                        text = "$formattedExpenses ج.س",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}"""

# Find the start of BalanceCard
start_idx = text.find('fun BalanceCard')
if start_idx == -1:
    print("Could not find start of BalanceCard")
    exit(1)

# Find the end of BalanceCard
next_idx = text.find('\n@Composable\nfun QuickActionButton', start_idx)
if next_idx == -1:
    print("Could not find end of BalanceCard")
    exit(1)

new_text = text[:start_idx] + good_part + text[next_idx:]
with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(new_text)
