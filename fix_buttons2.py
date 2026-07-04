import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    text = f.read()

good_part = """@Composable
fun QuickActionButton(
    icon: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                .shadow(elevation = 1.dp, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 24.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun TransactionItem(
    transaction: com.example.data.Transaction
) {"""

# Replace from @Composable\nfun QuickActionButton to fun TransactionItem(
text = re.sub(r'@Composable\s*fun QuickActionButton.*?fun TransactionItem\(\s*transaction: Transaction\s*\)\s*\{', good_part + " {", text, flags=re.DOTALL)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(text)
