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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import com.example.util.DatabaseBackupHelper
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
import com.example.ui.SectionsScreenView
import com.example.ui.CrashRecoveryScreen
import com.example.util.ErrorLogger
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

            androidx.compose.runtime.LaunchedEffect(Unit) {
                val savedId = prefs.getLong("linked_account_id_level_2_3", -1L)
                if (savedId != -1L) {
                    viewModel.linkedAccountIdForLevel23.value = savedId
                }
            }

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
                        var crashOccurred by remember { mutableStateOf(intent.getBooleanExtra("crash_occurred", false)) }
                        val errorMessage = remember { intent.getStringExtra("error_message") ?: "" }
                        val stackTrace = remember { intent.getStringExtra("stack_trace") ?: "" }

                        if (crashOccurred) {
                            CrashRecoveryScreen(
                                context = context,
                                errorMessage = errorMessage,
                                stackTrace = stackTrace,
                                onDismiss = {
                                    intent.removeExtra("crash_occurred")
                                    crashOccurred = false
                                }
                            )
                        } else {
                            var isAuthenticated by remember { mutableStateOf(false) }
                            if (!isAuthenticated) {
                                LoginScreen(
                                    prefs = prefs,
                                    onLoginSuccess = { level ->
                                        viewModel.userLevel.value = level
                                        isAuthenticated = true
                                    }
                                )
                            } else {
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

    val userLevel by viewModel.userLevel.collectAsState()
    var showApologyDialog by remember { mutableStateOf(false) }
    var apologyMessage by remember { mutableStateOf("") }

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
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 8.dp
            ) {
                val tabs = listOf(
                    Triple("HOME", "الرئيسية", Icons.Filled.Home),
                    Triple("SECTIONS", "الاقسام", Icons.Filled.GridView)
                )

                tabs.forEach { (tabId, tabName, tabIcon) ->
                    val isSelected = selectedTab == tabId || (tabId == "SECTIONS" && selectedTab != "HOME")
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
                                fontSize = 11.sp,
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
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                // Dropdown & Header Menu States
                var showAccountsMenu by remember { mutableStateOf(false) }

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
                            .clickable {
                                if (userLevel == 1) {
                                    showAccountsMenu = true
                                } else {
                                    showApologyDialog = true
                                    apologyMessage = "عذراً، لا تمتلك الصلاحية للتنقل بين المحلات أو إدارتها. يعرض لك فقط هذا المحل المخصص لك."
                                }
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = currentAccount?.name ?: "تحميل الحساب...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                letterSpacing = (-0.5).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "تبديل الحساب",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "انقر لتبديل المحل التجاري أو الحساب",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
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
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

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
                                                color = if (account.id == currentAccount?.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (account.id == currentAccount?.id) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "نشط",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectAccount(account.id)
                                        showAccountsMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Header shortcuts
                    val localContext = LocalContext.current
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), shape = CircleShape)
                                .clickable { (localContext as? android.app.Activity)?.finish() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ExitToApp,
                                contentDescription = "خروج من النظام",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
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
                        expenses = totalExpenses,
                        maskData = (userLevel == 3)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "الوصول السريع للأقسام",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Section 1: Income Screen
                        Card(
                            onClick = { selectedTab = "INCOME_SCREEN" },
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFE8F5E9), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = "واردات الدخل",
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "واردات الدخل",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "تسجيل الأرباح والواردات",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Section 2: Expense Screen
                        Card(
                            onClick = { selectedTab = "EXPENSE_SCREEN" },
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFFFEBEE), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingDown,
                                        contentDescription = "مصروفات المنصرف",
                                        tint = Color(0xFFC62828),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "مصروفات المنصرف",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "تسجيل المصاريف والمنصرف",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Section 3: Clients Screen
                        Card(
                            onClick = { selectedTab = "CLIENTS_SCREEN" },
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFE3F2FD), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.People,
                                        contentDescription = "دفتر العملاء",
                                        tint = Color(0xFF1565C0),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "دفتر العملاء",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "البيع الآجل والديون",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Section 4: Reports Screen
                        Card(
                            onClick = { selectedTab = "REPORTS_SCREEN" },
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                                horizontalAlignment = Alignment.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFF3E5F5), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PieChart,
                                        contentDescription = "التقارير والإحصائيات",
                                        tint = Color(0xFF6A1B9A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "التقارير والإحصائيات",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "التقارير اليومية والشهرية",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Extra section: Profit Screen
                    Card(
                        onClick = { selectedTab = "PROFIT_SCREEN" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFFFFF3E0), shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachMoney,
                                    contentDescription = "الأرباح والخصومات",
                                    tint = Color(0xFFEF6C00),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "حساب صافي الأرباح والخصومات",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "تصفية الخصومات اليومية وحساب الأرباح تلقائياً",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
            }
            "SECTIONS" -> {
                Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                    SectionsScreenView(
                        onSectionClick = { sectionId ->
                            selectedTab = sectionId
                        }
                    )
                }
            }
            "INCOME_SCREEN" -> {
                IncomeScreenView(
                    viewModel = viewModel,
                    innerPadding = innerPadding,
                    onNavigateBack = { selectedTab = "SECTIONS" }
                )
            }
            "EXPENSE_SCREEN" -> {
                ExpenseScreenView(
                    viewModel = viewModel,
                    innerPadding = innerPadding,
                    onNavigateBack = { selectedTab = "SECTIONS" }
                )
            }
            "REPORTS_SCREEN" -> {
                Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                    ReportsScreenView(
                        viewModel = viewModel,
                        onNavigateBack = { selectedTab = "SECTIONS" }
                    )
                }
            }
            "CLIENTS_SCREEN" -> {
                Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                    ClientsScreenView(
                        viewModel = viewModel,
                        onNavigateBack = { selectedTab = "SECTIONS" }
                    )
                }
            }
            "PROFIT_SCREEN" -> {
                if (currentAccount != null) {
                    com.example.ui.ProfitScreenView(
                        viewModel = viewModel,
                        account = currentAccount!!,
                        innerPadding = innerPadding,
                        onNavigateBack = { selectedTab = "SECTIONS" }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("الرجاء إضافة محل تجاري أولاً")
                    }
                }
            }
            "SETTINGS" -> {
                // Settings screen inside Scaffold
                Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                    AccountsSettingsView(
                        viewModel = viewModel,
                        isDarkMode = isDarkMode,
                        onThemeChange = onThemeChange,
                        fontScale = fontScale,
                        onFontScaleChange = onFontScaleChange,
                        onNavigateBack = { selectedTab = "SECTIONS" }
                    )
                }
            }
        }

        if (showApologyDialog) {
            AlertDialog(
                onDismissRequest = { showApologyDialog = false },
                title = {
                    Text(
                        "تنبيه الصلاحيات",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Text(
                        text = apologyMessage,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showApologyDialog = false },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("حسناً، فهمت")
                    }
                }
            )
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

    val userLevel by viewModel.userLevel.collectAsState()
    var showApologyDialog by remember { mutableStateOf(false) }
    var apologyMessage by remember { mutableStateOf("") }

    var showAddAccountDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }
    var accountToDelete by remember { mutableStateOf<Account?>(null) }
    var accountToClear by remember { mutableStateOf<Account?>(null) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showLogsDialog by remember { mutableStateOf(false) }
    var currentLogsText by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                val success = DatabaseBackupHelper.backupDatabase(context, it)
                Toast.makeText(context, if (success) "تم النسخ الاحتياطي بنجاح" else "فشل النسخ الاحتياطي", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                val success = DatabaseBackupHelper.restoreDatabase(context, it)
                if (success) {
                    Toast.makeText(context, "تمت الاستعادة بنجاح، يرجى إعادة تشغيل التطبيق", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "فشل في الاستعادة", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 40.dp)
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
                .clickable {
                    if (userLevel == 1) {
                        showAddAccountDialog = true
                    } else {
                        showApologyDialog = true
                        apologyMessage = "عذراً، لا تمتلك الصلاحية لإنشاء حساب أو محل تجاري جديد. هذه الصلاحية مخصصة لمدير النظام فقط."
                    }
                },
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
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                
                // Change Password Row
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (userLevel == 1) {
                            showChangePasswordDialog = true
                        } else {
                            showApologyDialog = true
                            apologyMessage = "عذراً، تعديل كلمات المرور متاح فقط لمدير النظام (المستوى الأول)."
                        }
                    }.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تغيير كلمة المرور",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "تحديث كلمة المرور المستخدمة لتسجيل الدخول",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "تغيير كلمة المرور",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Card to link user level 2 and 3 permissions to a specific account / store name
        Spacer(modifier = Modifier.height(16.dp))

        val linkedAccountId by viewModel.linkedAccountIdForLevel23.collectAsState()
        val linkedAccount = accounts.find { it.id == linkedAccountId }
        var showAccountSelectorMenu by remember { mutableStateOf(false) }
        val prefs = remember { context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE) }

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
                    text = "صلاحيات المستويين الثاني والثالث",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Column {
                    Text(
                        text = "ربط دخول المحاسبين والمراقبين بمحل تجاري محدد",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "يتيح هذا الإعداد تحديد محل تجاري واحد فقط يظهر تلقائياً للمستويين الثاني والثالث عند تسجيل الدخول، ولا يمكنهم تغييره.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "المحل المرتبط حالياً:",
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = linkedAccount?.name ?: "تلقائي (أول محل في القائمة)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (linkedAccount != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box {
                        Button(
                            onClick = {
                                if (userLevel == 1) {
                                    showAccountSelectorMenu = true
                                } else {
                                    showApologyDialog = true
                                    apologyMessage = "عذراً، ربط الحسابات وتخصيص الصلاحيات متاح فقط لمدير النظام (المستوى الأول)."
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("تغيير المحل", fontSize = 12.sp)
                        }

                        DropdownMenu(
                            expanded = showAccountSelectorMenu,
                            onDismissRequest = { showAccountSelectorMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("تلقائي (أول محل في القائمة)") },
                                onClick = {
                                    viewModel.linkedAccountIdForLevel23.value = null
                                    prefs.edit().remove("linked_account_id_level_2_3").apply()
                                    showAccountSelectorMenu = false
                                    Toast.makeText(context, "تم إلغاء الربط (تلقائي)", Toast.LENGTH_SHORT).show()
                                }
                            )

                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text(acc.name) },
                                    onClick = {
                                        viewModel.linkedAccountIdForLevel23.value = acc.id
                                        prefs.edit().putLong("linked_account_id_level_2_3", acc.id).apply()
                                        showAccountSelectorMenu = false
                                        Toast.makeText(context, "تم ربط الصلاحيات بمحل: ${acc.name}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "النسخ الاحتياطي والاستعادة",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Text(
            text = "احفظ ملف النسخة في Google Drive من خلال نافذة النظام",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { 
                    if (userLevel == 1) {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", java.util.Locale.getDefault())
                        val timestamp = sdf.format(java.util.Date())
                        backupLauncher.launch("simple_ledger_backup_$timestamp.zip") 
                    } else {
                        showApologyDialog = true
                        apologyMessage = "عذراً، عمليات النسخ الاحتياطي والاستعادة متاحة فقط لمدير النظام (المستوى الأول)."
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = "نسخ", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("نسخ احتياطي", fontSize = 12.sp)
            }
            Button(
                onClick = { 
                    if (userLevel == 1) {
                        restoreLauncher.launch(arrayOf("application/zip")) 
                    } else {
                        showApologyDialog = true
                        apologyMessage = "عذراً، عمليات النسخ الاحتياطي والاستعادة متاحة فقط لمدير النظام (المستوى الأول)."
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = "استعادة", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("استعادة", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Error Logs and Self-Healing Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clickable {
                    currentLogsText = ErrorLogger.getLogs(context)
                    showLogsDialog = true
                }
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.08f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = "سجل الأخطاء والتصحيح الذاتي",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "سجل الأخطاء الفنية والتصحيح الذاتي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "استعراض الأخطاء الفنية المحفوظة وتفاصيل الحماية والوقاية",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "تفاصيل",
                    tint = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (showLogsDialog) {
            Dialog(onDismissRequest = { showLogsDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .heightIn(max = 500.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "سجل الأخطاء الفنية والتصحيح الذاتي",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    Text(
                                        text = currentLogsText,
                                        fontSize = 10.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Crash Logs", currentLogsText)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "تم نسخ السجل للذاكرة", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("نسخ", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    ErrorLogger.clearLogs(context)
                                    currentLogsText = ErrorLogger.getLogs(context)
                                    Toast.makeText(context, "تم مسح السجل", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("مسح", fontSize = 11.sp, color = MaterialTheme.colorScheme.onError)
                            }

                            TextButton(
                                onClick = { showLogsDialog = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("إغلاق", fontSize = 11.sp)
                            }
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            accounts.forEach { account ->
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
                                    onClick = {
                                        if (userLevel == 1) {
                                            viewModel.selectAccount(account.id)
                                        } else {
                                            showApologyDialog = true
                                            apologyMessage = "عذراً، لا تمتلك الصلاحية لتنشيط حساب آخر. يجب عليك تصفح هذا المحل المخصص لك فقط."
                                        }
                                    },
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
                                onClick = {
                                    if (userLevel == 1) {
                                        accountToEdit = account
                                    } else {
                                        showApologyDialog = true
                                        apologyMessage = "عذراً، لا تمتلك الصلاحية لتعديل أسماء المحلات التجارية."
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "تعديل الاسم",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (userLevel == 1) {
                                        accountToClear = account
                                    } else {
                                        showApologyDialog = true
                                        apologyMessage = "عذراً، لا تمتلك الصلاحية لتفريغ بيانات المحلات التجارية."
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "تفريغ الحساب",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (userLevel == 1) {
                                        accountToDelete = account
                                    } else {
                                        showApologyDialog = true
                                        apologyMessage = "عذراً، لا تمتلك الصلاحية لحذف المحلات التجارية."
                                    }
                                },
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
            prefs = context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE),
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
            prefs = context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE),
            onDismiss = { accountToDelete = null },
            onConfirm = {
                viewModel.deleteAccount(accountToDelete!!)
                accountToDelete = null
            }
        )
    }

    if (accountToClear != null) {
        ClearAccountWarningDialog(
            accountName = accountToClear!!.name,
            prefs = context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE),
            onDismiss = { accountToClear = null },
            onConfirm = {
                viewModel.clearAccountData(accountToClear!!)
                accountToClear = null
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

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE),
            onDismiss = { showChangePasswordDialog = false }
        )
    }

    if (showApologyDialog) {
        AlertDialog(
            onDismissRequest = { showApologyDialog = false },
            title = {
                Text(
                    "تنبيه الصلاحيات",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = apologyMessage,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = { showApologyDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("حسناً، فهمت")
                }
            }
        )
    }
}

@Composable
fun EditAccountNameDialog(
    account: Account,
    prefs: android.content.SharedPreferences,
    onDismiss: () -> Unit,
    onConfirm: (newName: String) -> Unit
) {
    var name by remember { mutableStateOf(account.name) }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val savedPassword = prefs.getString("login_password", "12345") ?: "12345"

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
                        errorMessage = ""
                    },
                    label = { Text("اسم المحل أو النشاط التجاري الجديد") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = ""
                    },
                    label = { Text("كلمة المرور لتأكيد التعديل") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation()
                )

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
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
                            if (name.isBlank()) {
                                errorMessage = "الرجاء إدخال اسم صحيح للحساب!"
                            } else if (password != savedPassword) {
                                errorMessage = "كلمة المرور غير صحيحة"
                            } else {
                                onConfirm(name.trim())
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
    prefs: android.content.SharedPreferences,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val savedPassword = prefs.getString("login_password", "12345") ?: "12345"

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

                if (!isLastAccount) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = ""
                        },
                        label = { Text("كلمة المرور لتأكيد الحذف") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation()
                    )
                    
                    if (errorMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

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
                            onClick = {
                                if (password != savedPassword) {
                                    errorMessage = "كلمة المرور غير صحيحة"
                                } else {
                                    onConfirm()
                                }
                            },
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
fun ClearAccountWarningDialog(
    accountName: String,
    prefs: android.content.SharedPreferences,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val savedPassword = prefs.getString("login_password", "12345") ?: "12345"

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
                            MaterialTheme.colorScheme.errorContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🧹",
                        fontSize = 28.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "تأكيد تفريغ الحساب",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "هل أنت متأكد من تفريغ كافة بيانات الحساب '$accountName'؟\n\nتنبيه: سيتم حذف كافة المعاملات، العملاء، الديون، والمصروفات المسجلة تحت هذا الحساب بشكل نهائي ولا يمكن التراجع عن هذا الإجراء!",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = ""
                    },
                    label = { Text("كلمة المرور لتأكيد التفريغ") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation()
                )
                  
                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

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
                            if (password != savedPassword) {
                                errorMessage = "كلمة المرور غير صحيحة"
                            } else {
                                onConfirm()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("تأكيد التفريغ", fontWeight = FontWeight.Bold)
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
    expenses: Double,
    maskData: Boolean = false
) {
    val formatter = androidx.compose.runtime.remember { java.text.DecimalFormat("#,##0.00", java.text.DecimalFormatSymbols(java.util.Locale.US)) }
    val formattedBalance = if (maskData) "████" else formatter.format(balance)
    val formattedIncome = if (maskData) "████" else formatter.format(income)
    val formattedExpenses = if (maskData) "████" else formatter.format(expenses)

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
    val formatter = androidx.compose.runtime.remember { java.text.DecimalFormat("#,##0.00", java.text.DecimalFormatSymbols(java.util.Locale.US)) }
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

@Composable
fun LoginScreen(prefs: android.content.SharedPreferences, onLoginSuccess: (Int) -> Unit) {
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    
    val savedPassword1 = prefs.getString("login_password", "12345") ?: "12345"
    val savedPassword2 = prefs.getString("login_password_level_2", "22222") ?: "22222"
    val savedPassword3 = prefs.getString("login_password_level_3", "33333") ?: "33333"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, shape = RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Modern accounting logo
                    Image(
                        painter = painterResource(id = R.drawable.img_app_logo_1784062923194),
                        contentDescription = "شعار الدخول",
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "دفتر الحسابات الذكي",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "سجل مبيعاتك ومشترياتك بكل سهولة وأمان",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it
                            errorMessage = ""
                        },
                        label = { Text("كلمة المرور") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation(),
                        isError = errorMessage.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(top = 4.dp, start = 8.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Button(
                        onClick = {
                            val input = password.trim()
                            if (input == savedPassword1) {
                                onLoginSuccess(1)
                            } else if (input == savedPassword2) {
                                onLoginSuccess(2)
                            } else if (input == savedPassword3) {
                                onLoginSuccess(3)
                            } else {
                                errorMessage = "كلمة المرور غير صحيحة"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("تسجيل الدخول", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            // Removed password alerts from login screen as requested by user
        }
    }
}

@Composable
fun ChangePasswordDialog(prefs: android.content.SharedPreferences, onDismiss: () -> Unit) {
    var currentPassword1 by remember { mutableStateOf(prefs.getString("login_password", "12345") ?: "12345") }
    var currentPassword2 by remember { mutableStateOf(prefs.getString("login_password_level_2", "22222") ?: "22222") }
    var currentPassword3 by remember { mutableStateOf(prefs.getString("login_password_level_3", "33333") ?: "33333") }
    
    var editPassword1 by remember { mutableStateOf("") }
    var editPassword2 by remember { mutableStateOf("") }
    var editPassword3 by remember { mutableStateOf("") }
    
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "إدارة كلمات مرور النظام",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 18.sp
            ) 
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    "بصفتك مديراً للنظام، يمكنك تحديث كلمات المرور لجميع مستويات الصلاحيات هنا.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                
                // Level 1 Password
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("كلمة مرور المستوى الأول (كامل الصلاحيات):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    OutlinedTextField(
                        value = editPassword1,
                        onValueChange = { editPassword1 = it; errorMessage = ""; successMessage = "" },
                        placeholder = { Text("أدخل كلمة المرور الجديدة") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Level 2 Password
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("كلمة مرور المستوى الثاني (عرض محل واحد وبدون تعديل):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    OutlinedTextField(
                        value = editPassword2,
                        onValueChange = { editPassword2 = it; errorMessage = ""; successMessage = "" },
                        placeholder = { Text("أدخل كلمة المرور الجديدة") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Level 3 Password
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("كلمة مرور المستوى الثالث (عرض محل واحد، بيانات مشفرة):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                    OutlinedTextField(
                        value = editPassword3,
                        onValueChange = { editPassword3 = it; errorMessage = ""; successMessage = "" },
                        placeholder = { Text("أدخل كلمة المرور الجديدة") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (successMessage.isNotEmpty()) {
                    Text(
                        text = successMessage,
                        color = Color(0xFF2E7D32),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val editor = prefs.edit()
                    var changed = false
                    if (editPassword1.isNotBlank()) {
                        editor.putString("login_password", editPassword1.trim())
                        currentPassword1 = editPassword1.trim()
                        editPassword1 = ""
                        changed = true
                    }
                    if (editPassword2.isNotBlank()) {
                        editor.putString("login_password_level_2", editPassword2.trim())
                        currentPassword2 = editPassword2.trim()
                        editPassword2 = ""
                        changed = true
                    }
                    if (editPassword3.isNotBlank()) {
                        editor.putString("login_password_level_3", editPassword3.trim())
                        currentPassword3 = editPassword3.trim()
                        editPassword3 = ""
                        changed = true
                    }
                    if (changed) {
                        editor.apply()
                        successMessage = "تم تحديث كلمات المرور المدخلة بنجاح!"
                    } else {
                        errorMessage = "يرجى إدخال كلمة مرور جديدة لتحديثها."
                    }
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("تحديث")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
