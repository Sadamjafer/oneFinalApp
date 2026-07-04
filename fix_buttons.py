with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    text = f.read()

bad_part = """@Composablefun QuickActionButton(    icon: String,fun TransactionItem(    transaction: Transaction) {"""
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
                .background(MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(16.dp))
                .shadow(elevation = 1.dp, shape = RoundedCornerShape(16.dp))
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
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction
) {"""
text = text.replace(bad_part.replace('\n', ''), good_part)
with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(text)
