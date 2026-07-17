package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.data.Transaction
import com.example.util.DateUtils
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.PictureAsPdf

data class ReportRowData(
    val statement: String,
    val amount: Double,
    val transactions: List<Transaction> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreenView(viewModel: TransactionViewModel, onNavigateBack: (() -> Unit)? = null) {
    val allTransactions by viewModel.allTransactions.collectAsState()
    val userLevel by viewModel.userLevel.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    
    val tabs = listOf("يومي", "شهري", "فترة محددة")
    
    // For custom date range
    var startDateStr by remember { mutableStateOf("") }
    var endDateStr by remember { mutableStateOf("") }
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val startDatePickerState = rememberDatePickerState()
    val endDatePickerState = rememberDatePickerState()

    var pdfUriToExport by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            pdfUriToExport = uri
        }
    }

    val currentCalendar = remember { Calendar.getInstance() }
    var selectedMonth by remember { mutableStateOf(currentCalendar.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableStateOf(currentCalendar.get(Calendar.YEAR)) }
    
    val decimalFormat = remember { DecimalFormat("#,##0.##", java.text.DecimalFormatSymbols(java.util.Locale.US)) }
    val sdf = remember { SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH) }
    val utcSdf = remember { SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH).apply { timeZone = TimeZone.getTimeZone("UTC") } }

    var dailyDateStr by remember { mutableStateOf(sdf.format(Date())) }
    var showDailyDatePicker by remember { mutableStateOf(false) }
    val dailyDatePickerState = rememberDatePickerState()

    if (showDailyDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDailyDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dailyDatePickerState.selectedDateMillis?.let {
                        dailyDateStr = utcSdf.format(Date(it))
                    }
                    showDailyDatePicker = false
                }) {
                    Text("موافق")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDailyDatePicker = false }) {
                    Text("إلغاء")
                }
            }
        ) {
            DatePicker(state = dailyDatePickerState)
        }
    }

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDatePickerState.selectedDateMillis?.let {
                        startDateStr = utcSdf.format(Date(it))
                    }
                    showStartDatePicker = false
                }) {
                    Text("موافق")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("إلغاء")
                }
            }
        ) {
            DatePicker(state = startDatePickerState)
        }
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDatePickerState.selectedDateMillis?.let {
                        endDateStr = utcSdf.format(Date(it))
                    }
                    showEndDatePicker = false
                }) {
                    Text("موافق")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("إلغاء")
                }
            }
        ) {
            DatePicker(state = endDatePickerState)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = "التقارير والإحصائيات",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            val sdfForPdf = remember { SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()) }
            IconButton(onClick = {
                val timestamp = sdfForPdf.format(Date())
                pdfLauncher.launch("reports_$timestamp.pdf")
            }) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = "تصدير كملف PDF",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = FontWeight.Bold) }
                )
            }
        }
        
        val calendar = Calendar.getInstance()
        
        val filteredTransactions = remember(selectedTab, allTransactions, dailyDateStr, startDateStr, endDateStr, selectedMonth, selectedYear) {
            when (selectedTab) {
                0 -> { // Daily
                    try {
                        val d = sdf.parse(dailyDateStr)
                        if (d != null) {
                            val startCal = calendar.apply {
                                time = d
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                            val endCal = calendar.apply {
                                time = d
                                set(Calendar.HOUR_OF_DAY, 23)
                                set(Calendar.MINUTE, 59)
                                set(Calendar.SECOND, 59)
                                set(Calendar.MILLISECOND, 999)
                            }.timeInMillis
                            allTransactions.filter { it.timestamp in startCal..endCal }
                        } else {
                            emptyList()
                        }
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
                1 -> { // Monthly
                    val monthStart = calendar.apply {
                        set(Calendar.YEAR, selectedYear)
                        set(Calendar.MONTH, selectedMonth)
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val monthEnd = calendar.apply {
                        set(Calendar.YEAR, selectedYear)
                        set(Calendar.MONTH, selectedMonth)
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                    allTransactions.filter { it.timestamp in monthStart..monthEnd }
                }
                2 -> { // Custom
                    try {
                        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH)
                        val start = if (startDateStr.isNotBlank()) sdf.parse(startDateStr)?.time ?: 0L else 0L
                        val end = if (endDateStr.isNotBlank()) {
                            val d = sdf.parse(endDateStr)
                            if (d != null) {
                                val cal = Calendar.getInstance().apply { time = d }
                                cal.set(Calendar.HOUR_OF_DAY, 23)
                                cal.set(Calendar.MINUTE, 59)
                                cal.set(Calendar.SECOND, 59)
                                cal.timeInMillis
                            } else Long.MAX_VALUE
                        } else Long.MAX_VALUE
                        allTransactions.filter { it.timestamp in start..end }
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
                else -> emptyList()
            }
        }
        
        val totalIncome = filteredTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = filteredTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val netBalance = totalIncome - totalExpense
        
        if (selectedTab == 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { 
                    try {
                        val d = sdf.parse(dailyDateStr)
                        if (d != null) {
                            val cal = Calendar.getInstance().apply {
                                time = d
                                add(Calendar.DAY_OF_MONTH, -1)
                            }
                            dailyDateStr = sdf.format(cal.time)
                        }
                    } catch (e: Exception) {}
                }) {
                    Text("▶", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showDailyDatePicker = true }
                ) {
                    Text(
                        text = dailyDateStr,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("📅", fontSize = 18.sp)
                }
                
                IconButton(onClick = { 
                    try {
                        val d = sdf.parse(dailyDateStr)
                        if (d != null) {
                            val cal = Calendar.getInstance().apply {
                                time = d
                                add(Calendar.DAY_OF_MONTH, 1)
                            }
                            dailyDateStr = sdf.format(cal.time)
                        }
                    } catch (e: Exception) {}
                }) {
                    Text("◀", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        
        if (selectedTab == 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { 
                    if (selectedMonth == 0) {
                        selectedMonth = 11
                        selectedYear--
                    } else {
                        selectedMonth--
                    }
                }) {
                    Text("▶", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                }
                
                val monthName = remember(selectedMonth, selectedYear) {
                    SimpleDateFormat("MMMM yyyy", Locale("ar")).format(
                        Calendar.getInstance().apply {
                            set(Calendar.YEAR, selectedYear)
                            set(Calendar.MONTH, selectedMonth)
                            set(Calendar.DAY_OF_MONTH, 1)
                        }.time
                    )
                }
                
                Text(
                    text = monthName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                IconButton(onClick = { 
                    if (selectedMonth == 11) {
                        selectedMonth = 0
                        selectedYear++
                    } else {
                        selectedMonth++
                    }
                }) {
                    Text("◀", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        
        if (selectedTab == 2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = startDateStr,
                    onValueChange = {},
                    label = { Text("من (yyyy/MM/dd)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    readOnly = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = { showStartDatePicker = true }) {
                            Text("📅")
                        }
                    }
                )
                OutlinedTextField(
                    value = endDateStr,
                    onValueChange = {},
                    label = { Text("إلى (yyyy/MM/dd)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    readOnly = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = { showEndDatePicker = true }) {
                            Text("📅")
                        }
                    }
                )
            }
        }
        
        val incomesData = if (selectedTab == 0) {
            filteredTransactions.filter { it.type == "INCOME" }.sortedByDescending { it.timestamp }.map {
                ReportRowData(statement = "${it.title} (${it.category})", amount = it.amount, transactions = listOf(it))
            }
        } else {
            filteredTransactions.filter { it.type == "INCOME" }
                .groupBy { it.category }
                .map { (category, txs) ->
                    ReportRowData(statement = category, amount = txs.sumOf { it.amount }, transactions = txs.sortedByDescending { it.timestamp })
                }
                .sortedByDescending { it.amount }
        }

        val expensesData = if (selectedTab == 0) {
            filteredTransactions.filter { it.type == "EXPENSE" }.sortedByDescending { it.timestamp }.map {
                ReportRowData(statement = "${it.title} (${it.category})", amount = it.amount, transactions = listOf(it))
            }
        } else {
            filteredTransactions.filter { it.type == "EXPENSE" }
                .groupBy { it.category }
                .map { (category, txs) ->
                    ReportRowData(statement = category, amount = txs.sumOf { it.amount }, transactions = txs.sortedByDescending { it.timestamp })
                }
                .sortedByDescending { it.amount }
        }
        
        ReportTableView(
            incomes = incomesData,
            expenses = expensesData,
            decimalFormat = decimalFormat,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netBalance = netBalance,
            isExpandable = selectedTab != 0,
            userLevel = userLevel
        )
        
        LaunchedEffect(pdfUriToExport) {
            pdfUriToExport?.let { uri ->
                val accountName = viewModel.currentAccount.value?.name ?: ""
                val title = "تقرير حركة الخزينة" + if (accountName.isNotEmpty()) " - $accountName" else ""
                val headers = listOf("المبلغ", "البيان")
                val data = mutableListOf<List<String>>()
                
                data.add(listOf("", "=== الإيرادات ==="))
                incomesData.forEach { data.add(listOf(decimalFormat.format(it.amount), it.statement)) }
                
                data.add(listOf("", "=== المصروفات ==="))
                expensesData.forEach { data.add(listOf(decimalFormat.format(it.amount), it.statement)) }
                
                val summary = listOf(
                    "إجمالي الإيرادات: ${decimalFormat.format(totalIncome)}",
                    "إجمالي المصروفات: ${decimalFormat.format(totalExpense)}",
                    "صافي الربح: ${decimalFormat.format(netBalance)}"
                )
                
                com.example.utils.PdfGenerator.generatePdf(context, uri, title, headers, data, summary)
                pdfUriToExport = null
            }
        }
    }
}

@Composable
fun ReportTableView(
    incomes: List<ReportRowData>,
    expenses: List<ReportRowData>,
    decimalFormat: DecimalFormat,
    totalIncome: Double,
    totalExpense: Double,
    netBalance: Double,
    isExpandable: Boolean = false,
    userLevel: Int = 1
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 350.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Table 1: Incomes
        item {
            ReportTable(
                title = "جدول الإيرادات",
                rows = incomes,
                totalAmount = totalIncome,
                decimalFormat = decimalFormat,
                headerColor = MaterialTheme.colorScheme.primary,
                isExpandable = isExpandable,
                userLevel = userLevel
            )
        }

        // Table 2: Expenses
        item {
            ReportTable(
                title = "جدول المصروفات",
                rows = expenses,
                totalAmount = totalExpense,
                decimalFormat = decimalFormat,
                headerColor = MaterialTheme.colorScheme.error,
                isExpandable = isExpandable,
                userLevel = userLevel
            )
        }

        // Table 3: Summary
        item(span = { GridItemSpan(maxLineSpan) }) {
            SummaryTable(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                netBalance = netBalance,
                decimalFormat = decimalFormat,
                userLevel = userLevel
            )
        }
    }
}

@Composable
fun ReportTable(
    title: String,
    rows: List<ReportRowData>,
    totalAmount: Double,
    decimalFormat: DecimalFormat,
    headerColor: Color,
    isExpandable: Boolean = false,
    userLevel: Int = 1
) {
    var expandedRows by remember { mutableStateOf(setOf<String>()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerColor.copy(alpha = 0.08f))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (headerColor == MaterialTheme.colorScheme.primary) "📈" else "📉",
                    fontSize = 18.sp
                )
                Text(
                    text = title,
                    fontWeight = FontWeight.ExtraBold,
                    color = headerColor,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                // Pill badge for count of items
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = headerColor.copy(alpha = 0.15f),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "${rows.size} بند",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = headerColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Column Headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(vertical = 10.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "البيان والنوع",
                    modifier = Modifier.weight(1.5f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "المبلغ",
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Rows
            if (rows.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد بيانات مسجلة",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                rows.forEachIndexed { index, row ->
                    val isExpanded = expandedRows.contains(row.statement)
                    val canExpand = isExpandable && row.transactions.isNotEmpty()
                    val rowBg = if (isExpanded) {
                        headerColor.copy(alpha = 0.03f)
                    } else if (index % 2 == 0) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBg)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (canExpand) {
                                        Modifier.clickable {
                                            expandedRows = if (isExpanded) {
                                                expandedRows - row.statement
                                            } else {
                                                expandedRows + row.statement
                                            }
                                        }
                                    } else Modifier
                                )
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1.5f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = row.statement,
                                    fontSize = 13.sp,
                                    fontWeight = if (isExpanded) FontWeight.Bold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (canExpand) {
                                    Text(
                                        text = if (isExpanded) "▼ التفاصيل" else "◀ التفاصيل",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = headerColor.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (userLevel == 3) "████ ج.س" else "${decimalFormat.format(row.amount)} ج.س",
                                modifier = Modifier.weight(1f),
                                fontSize = 13.sp,
                                color = headerColor,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Expanded Details (Transactions)
                        if (isExpanded && canExpand) {
                            row.transactions.forEach { tx ->
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(headerColor.copy(alpha = 0.05f))
                                        .padding(vertical = 8.dp, horizontal = 24.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1.5f)) {
                                        Text(
                                            text = "📅 ${com.example.util.DateUtils.formatLocal(tx.timestamp)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (tx.notes.isNotBlank()) {
                                            Text(
                                                text = "📝 ${tx.notes}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (userLevel == 3) "████ ج.س" else "${decimalFormat.format(tx.amount)} ج.س",
                                        modifier = Modifier.weight(1f),
                                        fontSize = 12.sp,
                                        color = headerColor.copy(alpha = 0.8f),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
            }

            // Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerColor.copy(alpha = 0.04f))
                    .padding(vertical = 14.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "إجمالي البند",
                    modifier = Modifier.weight(1.5f),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (userLevel == 3) "████ ج.س" else "${decimalFormat.format(totalAmount)} ج.س",
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = headerColor,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SummaryTable(
    totalIncome: Double,
    totalExpense: Double,
    netBalance: Double,
    decimalFormat: DecimalFormat,
    userLevel: Int = 1
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column {
            // Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF3F51B5).copy(alpha = 0.08f))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("📊", fontSize = 18.sp)
                Text(
                    text = "جدول الخلاصة المالية",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF3F51B5),
                    fontSize = 15.sp
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Income Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1.5f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🟢", fontSize = 12.sp)
                    Text(
                        text = "إجمالي الإيرادات",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = if (userLevel == 3) "████ ج.س" else "${decimalFormat.format(totalIncome)} ج.س",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Expense Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1.5f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🔴", fontSize = 12.sp)
                    Text(
                        text = "إجمالي المصروفات",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = if (userLevel == 3) "████ ج.س" else "${decimalFormat.format(totalExpense)} ج.س",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Net Balance Row
            val netColor = if (netBalance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            val netBg = if (netBalance >= 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(netBg)
                    .padding(vertical = 14.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1.5f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(if (netBalance >= 0) "💰" else "⚠️", fontSize = 14.sp)
                    Text(
                        text = "صافي الربح",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = if (userLevel == 3) "████ ج.س" else "${if (netBalance >= 0) "+" else ""}${decimalFormat.format(netBalance)} ج.س",
                    modifier = Modifier.weight(1f),
                    color = netColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}