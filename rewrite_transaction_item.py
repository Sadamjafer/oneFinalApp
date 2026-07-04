import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    text = f.read()

good_part = """@Composable
fun TransactionItem(
    transaction: com.example.data.Transaction
) {
    val formatter = androidx.compose.runtime.remember { java.text.DecimalFormat("#,##0.00") }
    val formattedAmount = formatter.format(transaction.amount)
    val formattedDate = androidx.compose.runtime.remember(transaction.timestamp) {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        sdf.format(java.util.Date(transaction.timestamp))
    }

    val isIncome = transaction.type == "INCOME"
    val typeColor = if (isIncome) androidx.compose.ui.graphics.Color(0xFF10B981) else androidx.compose.ui.graphics.Color(0xFFF43F5E)
    val iconBg = if (isIncome) androidx.compose.ui.graphics.Color(0xFFD1FAE5) else androidx.compose.ui.graphics.Color(0xFFFFE4E6)
    val iconContentColor = if (isIncome) androidx.compose.ui.graphics.Color(0xFF047857) else androidx.compose.ui.graphics.Color(0xFFBE123C)
    val categoryEmoji = getCategoryEmoji(transaction.category, transaction.type)

    androidx.compose.material3.Card(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), spotColor = androidx.compose.ui.graphics.Color(0x1A000000))
            .testTag("transaction_item_${transaction.id}"),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, androidx.compose.material3.MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = androidx.compose.ui.Modifier
                    .size(48.dp)
                    .background(iconBg, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = categoryEmoji, fontSize = 22.sp)
            }
            Spacer(modifier = androidx.compose.ui.Modifier.width(16.dp))
            Column(
                modifier = androidx.compose.ui.Modifier.weight(1f)
            ) {
                Text(
                    text = transaction.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = transaction.category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        modifier = androidx.compose.ui.Modifier
                            .background(
                                androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (transaction.notes.isNotEmpty()) {
                    Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
                    Text(
                        text = transaction.notes,
                        fontSize = 12.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${if (isIncome) "+" else "-"}$formattedAmount ج.س",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = typeColor
                )
            }
        }
    }
}"""

# Find the start of TransactionItem
start_idx = text.find('@Composable\nfun TransactionItem')
if start_idx == -1:
    print("Could not find start of TransactionItem")
    exit(1)

# Find the end of TransactionItem
# We look for the start of the next function
next_idx = text.find('\nfun getCategoryEmoji(', start_idx)
if next_idx == -1:
    print("Could not find end of TransactionItem")
    exit(1)

new_text = text[:start_idx] + good_part + text[next_idx:]
with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(new_text)
