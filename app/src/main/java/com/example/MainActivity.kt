package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.example.data.Account
import com.example.data.Transaction
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.TransactionViewModel
import com.example.ui.TransactionViewModelFactory
import com.example.ui.IncomeScreenView
import com.example.ui.ExpenseScreenView
import com.example.ui.ReportsScreenView
import com.example.ui.ClientsScreenView
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private val viewModel: TransactionViewModel by viewModels {
        TransactionViewModelFactory((application as SimpleLedgerApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }
            var isDarkMode by remember { mutableStateOf(prefs.getBoolean("is_dark_mode", false)) }
            var fontScale by remember { mutableStateOf(prefs.getFloat("font_scale", 1.0f)) }

            val onThemeChange: (Boolean) -> Unit = { dark ->
                isDarkMode = dark
                prefs.edit().putBoolean("is_dark_mode", dark).apply()
            }
            val onFontScaleChange: (Float) -> Unit = { scale ->
                fontScale = scale
                prefs.edit().putFloat("font_scale", scale).apply()
            }

            MyApplicationTheme(darkTheme = isDarkMode) {
                val currentDensity = LocalDensity.current
                val customDensity = Density(
                    density = currentDensity.density,
                    fontScale = currentDensity.fontScale * fontScale
                )
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                    LocalDensity provides customDensity
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        LedgerDashboard(
                            viewModel = viewModel,
                            isDarkMode = isDarkMode,
                            onThemeChange = onThemeChange,
                            fontScale = fontScale,
                            onFontScaleChange = onFontScaleChange
                        )
                    }
                }
            }
        }
    }
}

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
        when (selectedTab) {
            "HOME" -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                // Dropdown & Header Menu States
                var showAccountsMenu by remember { mutableStateOf(false) }
                var showAddAccountDialog by remember { mutableStateOf(false) }

                // Top Custom Header with Shop Dropdown selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showAccountsMenu = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = currentAccount?.name ?: "تحميل الحساب...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = Color(0xFF1D1B20),
                                letterSpacing = (-0.5).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "تبديل الحساب",
                                tint = Color(0xFF6750A4),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "انقر لتبديل أو إضافة محل تجاري جديد",
                            fontSize = 12.sp,
                            color = Color(0xFF49454F).copy(alpha = 0.8f)
                        )

                        // Dropdown menu showing all shops
                        DropdownMenu(
                            expanded = showAccountsMenu,
                            onDismissRequest = { showAccountsMenu = false },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface)
                                .widthIn(min = 220.dp)
                        ) {
                            Text(
                                text = "المحلات والحسابات المسجلة",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6750A4),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            HorizontalDivider(color = Color(0xFFCAC4D0).copy(alpha = 0.4f))

                            accounts.forEach { account ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = account.name,
                                                fontWeight = if (account.id == currentAccount?.id) FontWeight.Bold else FontWeight.Normal,
                                                color = if (account.id == currentAccount?.id) Color(0xFF6750A4) else Color(0xFF1D1B20),
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (account.id == currentAccount?.id) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "نشط",
                                                    tint = Color(0xFF6750A4),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectAccount(account.id)
                                        showAccountsMenu = false
                                    },
                                    trailingIcon = {
                                        if (accounts.size > 1) {
                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteAccount(account)
                                                    showAccountsMenu = false
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "حذف الحساب",
                                                    tint = Color.Red.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                )
                            }

                            HorizontalDivider(color = Color(0xFFCAC4D0).copy(alpha = 0.4f))
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "إضافة حساب",
                                            tint = Color(0xFF6750A4),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "إنشاء حساب/محل جديد",
                                            color = Color(0xFF6750A4),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                },
                                onClick = {
                                    showAccountsMenu = false
                                    showAddAccountDialog = true
                                }
                            )
                        }
                    }

                    // Header shop-icon shortcut
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFFE8DEF8), shape = CircleShape)
                            .clickable { showAddAccountDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = "المحلات التجارية",
                            tint = Color(0xFF6750A4),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Dialogue popup for adding new account
                if (showAddAccountDialog) {
                    AddAccountDialog(
                        onDismiss = { showAddAccountDialog = false },
                        onConfirm = { name ->
                            viewModel.addAccount(name)
                            showAddAccountDialog = false
                        }
                    )
                }

                // Top section: Balance and Quick Actions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    // Balance Card
                    BalanceCard(
                        balance = currentBalance,
                        income = totalIncome,
                        expenses = totalExpenses
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Section: Recent Transactions Sheet
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(
                            color = Color(0xFFF7F2FA),
                            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                        )
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                            clip = false
                        )
                        .padding(horizontal = 24.dp)
                        .padding(top = 24.dp)
                ) {
                    // Header inside the sheet
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "أحدث العمليات",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF1D1B20)
                        )

                        // Minimal Filter selection chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val filters = listOf(
                                "ALL" to "الكل",
                                "INCOME" to "الدخل",
                                "EXPENSE" to "المصروف"
                            )
                            filters.forEach { (type, label) ->
                                val isSelected = filterType == type
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) Color(0xFFE8DEF8) else Color.Transparent
                                        )
                                        .clickable { filterType = type }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color(0xFF21005D) else Color(0xFF49454F)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Records Feed
                    if (filteredTransactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFCAC4D0),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "لا توجد أي عمليات مسجلة حالياً لهذا المحل.",
                                    color = Color(0xFF49454F).copy(alpha = 0.6f),
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(filteredTransactions, key = { it.id }) { transaction ->
                                TransactionItem(
                                    transaction = transaction
                                )
                            }
                        }
                    }
                }
            }
            }
            "INCOME_SCREEN" -> {
                IncomeScreenView(
                    viewModel = viewModel,
                    innerPadding = innerPadding,
                    onNavigateBack = { selectedTab = "HOME" }
                )
            }
            "EXPENSE_SCREEN" -> {
                ExpenseScreenView(
                    viewModel = viewModel,
                    innerPadding = innerPadding,
                    onNavigateBack = { selectedTab = "HOME" }
                )
            }
            "REPORTS_SCREEN" -> {
                Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                    ReportsScreenView(viewModel = viewModel)
                }
            }
            "CLIENTS_SCREEN" -> {
                Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                    ClientsScreenView(viewModel = viewModel)
                }
            }
            "SETTINGS" -> {
                // Settings screen inside Scaffold
                AccountsSettingsView(
                    viewModel = viewModel,
                    isDarkMode = isDarkMode,
                    onThemeChange = onThemeChange,
                    fontScale = fontScale,
                    onFontScaleChange = onFontScaleChange,
                    onNavigateBack = { selectedTab = "HOME" }
                )
            }
        }
    }
}

@Composable
fun AccountsSettingsView(
    viewModel: TransactionViewModel,
    isDarkMode: Boolean,
    onThemeChange: (Boolean) -> Unit,
    fontScale: Float,
    onFontScaleChange: (Float) -> Unit,
    onNavigateBack: () -> Unit
) {
    val accounts by viewModel.allAccounts.collectAsState()
    val currentAccount by viewModel.currentAccount.collectAsState()

    var showAddAccountDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Custom Header for Settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "الرجوع للرئيسية",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "إعدادات التطبيق والمحلات",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "تخصيص المظهر، حجم الخط، وإدارة المحلات التجارية",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Create new Account Card at the top of content
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clickable { showAddAccountDialog = true },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "إنشاء حساب/محل تجاري جديد",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "إنشاء حساب أو محل تجاري جديد",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Appearance & Settings Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "إعدادات المظهر والخط",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                // Dark Mode Switch Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "المظهر الداكن (Dark Mode)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "تفعيل المظهر المظلم لحماية العين وتوفير الطاقة",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = onThemeChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Font Size Adjust Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تغيير حجم الخط",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "تكبير أو تصغير حجم الخط لجميع شاشات التطبيق",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { onFontScaleChange(fontScale + 0.1f) },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape)
                                .size(32.dp)
                        ) {
                            Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Text(
                            text = String.format("%.1fx", fontScale),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = { onFontScaleChange(maxOf(0.7f, fontScale - 0.1f)) },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape)
                                .size(32.dp)
                        ) {
                            Text("-", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "المحلات والحسابات المسجلة حالياً:",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // List of all accounts
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(accounts, key = { it.id }) { account ->
                val isActive = account.id == currentAccount?.id

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isActive) 1.5.dp else 1.dp,
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🏪", fontSize = 18.sp)
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = account.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isActive) {
                                    Text(
                                        text = "نشط حالياً",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "تم إنشاؤه في: ${SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(account.timestamp))}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (!isActive) {
                                IconButton(
                                    onClick = { viewModel.selectAccount(account.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "تنشيط الحساب",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { accountToEdit = account },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "تعديل الاسم",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(
                                onClick = { accountToDelete = account },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "حذف الحساب",
                                    tint = Color.Red.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (accountToEdit != null) {
        EditAccountNameDialog(
            account = accountToEdit!!,
            onDismiss = { accountToEdit = null },
            onConfirm = { newName ->
                viewModel.updateAccountName(accountToEdit!!, newName)
                accountToEdit = null
            }
        )
    }

    if (accountToDelete != null) {
        val isLastAccount = accounts.size <= 1
        DeleteAccountWarningDialog(
            accountName = accountToDelete!!.name,
            isLastAccount = isLastAccount,
            onDismiss = { accountToDelete = null },
            onConfirm = {
                viewModel.deleteAccount(accountToDelete!!)
                accountToDelete = null
            }
        )
    }

    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onConfirm = { name ->
                viewModel.addAccount(name)
                showAddAccountDialog = false
            }
        )
    }
}

@Composable
fun EditAccountNameDialog(
    account: Account,
    onDismiss: () -> Unit,
    onConfirm: (newName: String) -> Unit
) {
    var name by remember { mutableStateOf(account.name) }
    var showError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "تعديل اسم الحساب / المحل",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showError = false
                    },
                    label = { Text("اسم المحل أو النشاط التجاري الجديد") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                if (showError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "الرجاء إدخال اسم صحيح للحساب!",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(name.trim())
                            } else {
                                showError = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("حفظ التعديل", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteAccountWarningDialog(
    accountName: String,
    isLastAccount: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            if (isLastAccount) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isLastAccount) "⚠️" else "🚨",
                        fontSize = 28.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isLastAccount) "تنبيه هام" else "تأكيد حذف الحساب",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (isLastAccount) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isLastAccount) {
                        "لا يمكن حذف الحساب '$accountName' لأنه الحساب الوحيد المتبقي في التطبيق. يجب أن يحتوي التطبيق على حساب واحد على الأقل."
                    } else {
                        "هل أنت متأكد من حذف الحساب '$accountName'؟\n\nتنبيه: سيتم حذف هذا الحساب وكافة المعاملات والعمليات المالية المسجلة تحته بشكل نهائي ولا يمكن استرجاع هذه البيانات!"
                    },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isLastAccount) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("حسنًا", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("تأكيد الحذف", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
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
}
@Composable
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
}
fun getCategoryEmoji(category: String, type: String): String {
    return when (category) {
        "راتب" -> "💼"
        "استثمار" -> "📈"
        "بيع" -> "🏷️"
        "طعام" -> "🛒"
        "إيجار" -> "🏠"
        "مواصلات" -> "🚗"
        "تسوق" -> "🛍️"
        "فواتير" -> "📄"
        else -> if (type == "INCOME") "💰" else "💸"
    }
}

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "إنشاء حساب / محل جديد",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showError = false
                    },
                    label = { Text("اسم المحل أو النشاط التجاري") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                if (showError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "الرجاء إدخال اسم صحيح للحساب!",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(name.trim())
                            } else {
                                showError = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إنشاء", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
