@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerDashboard(
    viewModel: TransactionViewModel,
    isDarkMode: Boolean,
    onThemeChange: (Boolean) -> Unit,
    fontScale: Float,
    onFontScaleChange: (Float) -> Unit
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpenses by viewModel.totalExpenses.collectAsState()
    val currentBalance by viewModel.currentBalance.collectAsState()

    val accounts by viewModel.allAccounts.collectAsState()
    val currentAccount by viewModel.currentAccount.collectAsState()

    var filterType by remember { mutableStateOf("ALL") } // "ALL", "INCOME", "EXPENSE"
    var selectedTab by remember { mutableStateOf("HOME") } // "HOME" or "SETTINGS"

    val filteredTransactions = remember(transactions, filterType) {
        when (filterType) {
            "INCOME" -> transactions.filter { it.type == "INCOME" }
            "EXPENSE" -> transactions.filter { it.type == "EXPENSE" }
            else -> transactions
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.fillMaxWidth().shadow(8.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 8.dp
            ) {
                val tabs = listOf(
                    Triple("HOME", "الرئيسية", Icons.Filled.Home),
                    Triple("INCOME_SCREEN", "الإيرادات", Icons.Filled.TrendingUp),
                    Triple("EXPENSE_SCREEN", "المصروفات", Icons.Filled.TrendingDown),
                    Triple("CLIENTS_SCREEN", "العملاء", Icons.Filled.Person),
                    Triple("REPORTS_SCREEN", "التقارير", Icons.Filled.PieChart),
                    Triple("SETTINGS", "الإعدادات", Icons.Filled.Settings)
                )

                tabs.forEach { (tabId, tabName, tabIcon) ->
                    val isSelected = selectedTab == tabId
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tabId },
                        icon = {
                            Icon(
                                imageVector = tabIcon,
                                contentDescription = tabName
                            )
                        },
                        label = {
                            Text(
                                text = tabName,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
