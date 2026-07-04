package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.data.Transaction
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

data class ReportRowData(
    val statement: String,
    val amount: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreenView(viewModel: TransactionViewModel) {
    val allTransactions by viewModel.allTransactions.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    
    val tabs = listOf("يومي", "شهري", "فترة محددة")
    
    // For custom date range
    var startDateStr by remember { mutableStateOf("") }
    var endDateStr by remember { mutableStateOf("") }
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val startDatePickerState = rememberDatePickerState()
    val endDatePickerState = rememberDatePickerState()

    val currentCalendar = remember { Calendar.getInstance() }
    var selectedMonth by remember { mutableStateOf(currentCalendar.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableStateOf(currentCalendar.get(Calendar.YEAR)) }
    
    val decimalFormat = remember { DecimalFormat("#,##0.##") }
    val sdf = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDatePickerState.selectedDateMillis?.let {
                        startDateStr = sdf.format(Date(it))
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
                        endDateStr = sdf.format(Date(it))
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
        
        val filteredTransactions = remember(selectedTab, allTransactions, startDateStr, endDateStr, selectedMonth, selectedYear) {
            when (selectedTab) {
                0 -> { // Daily
                    val todayStart = calendar.apply {
                        timeInMillis = System.currentTimeMillis()
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    allTransactions.filter { it.timestamp >= todayStart }
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
                        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
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
                ReportRowData(statement = "${it.title} (${it.category})", amount = it.amount)
            }
        } else {
            filteredTransactions.filter { it.type == "INCOME" }
                .groupBy { it.category }
                .map { (category, txs) ->
                    ReportRowData(statement = category, amount = txs.sumOf { it.amount })
                }
                .sortedByDescending { it.amount }
        }

        val expensesData = if (selectedTab == 0) {
            filteredTransactions.filter { it.type == "EXPENSE" }.sortedByDescending { it.timestamp }.map {
                ReportRowData(statement = "${it.title} (${it.category})", amount = it.amount)
            }
        } else {
            filteredTransactions.filter { it.type == "EXPENSE" }
                .groupBy { it.category }
                .map { (category, txs) ->
                    ReportRowData(statement = category, amount = txs.sumOf { it.amount })
                }
                .sortedByDescending { it.amount }
        }
        
        ReportTableView(
            incomes = incomesData,
            expenses = expensesData,
            decimalFormat = decimalFormat,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netBalance = netBalance
        )
    }
}

@Composable
fun ReportTableView(
    incomes: List<ReportRowData>,
    expenses: List<ReportRowData>,
    decimalFormat: DecimalFormat,
    totalIncome: Double,
    totalExpense: Double,
    netBalance: Double
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Table 1: Incomes
        item {
            ReportTable(
                title = "جدول الإيرادات",
                rows = incomes,
                totalAmount = totalIncome,
                decimalFormat = decimalFormat,
                headerColor = MaterialTheme.colorScheme.primary
            )
        }

        // Table 2: Expenses
        item {
            ReportTable(
                title = "جدول المصروفات",
                rows = expenses,
                totalAmount = totalExpense,
                decimalFormat = decimalFormat,
                headerColor = MaterialTheme.colorScheme.error
            )
        }

        // Table 3: Summary
        item {
            SummaryTable(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                netBalance = netBalance,
                decimalFormat = decimalFormat
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
    headerColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(1.dp)
            .background(Color.LightGray) // simple border effect
            .padding(1.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerColor.copy(alpha = 0.1f))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(title, fontWeight = FontWeight.Bold, color = headerColor, fontSize = 16.sp)
        }

        // Headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp)
        ) {
            Text("البيان", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text("المبلغ", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        }
        
        Divider(color = Color.LightGray)

        // Rows
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = row.statement, 
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp
                )
                Text(
                    text = "${decimalFormat.format(row.amount)} ج.س", 
                    modifier = Modifier.weight(1f),
                    fontSize = 14.sp,
                    color = headerColor
                )
            }
            Divider(color = Color.LightGray)
        }

        // Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerColor.copy(alpha = 0.05f))
                .padding(12.dp)
        ) {
            Text("الإجمالي", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text(
                text = "${decimalFormat.format(totalAmount)} ج.س",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                color = headerColor
            )
        }
    }
}

@Composable
fun SummaryTable(
    totalIncome: Double,
    totalExpense: Double,
    netBalance: Double,
    decimalFormat: DecimalFormat
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.LightGray)
            .padding(1.dp)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF3F51B5).copy(alpha = 0.1f))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("جدول الخلاصة", fontWeight = FontWeight.Bold, color = Color(0xFF3F51B5), fontSize = 16.sp)
        }

        // Income Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text("إجمالي الإيرادات", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text(
                text = "${decimalFormat.format(totalIncome)} ج.س",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
        Divider(color = Color.LightGray)

        // Expense Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text("إجمالي المصروفات", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text(
                text = "${decimalFormat.format(totalExpense)} ج.س",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )
        }
        Divider(color = Color.LightGray)

        // Net Balance Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (netBalance >= 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer)
                .padding(12.dp)
        ) {
            Text("صافي الربح", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text(
                text = "${if (netBalance >= 0) "+" else ""}${decimalFormat.format(netBalance)} ج.س",
                modifier = Modifier.weight(1f),
                color = if (netBalance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }
    }
}