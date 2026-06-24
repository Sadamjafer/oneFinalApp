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
            .background(Color(0xFFF7F9FC))
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
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
        
        val filteredTransactions = remember(selectedTab, allTransactions, startDateStr, endDateStr) {
            when (selectedTab) {
                0 -> { // Daily
                    val todayStart = calendar.apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    allTransactions.filter { it.timestamp >= todayStart }
                }
                1 -> { // Monthly
                    val monthStart = calendar.apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    allTransactions.filter { it.timestamp >= monthStart }
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
        
        // Summary Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SummaryCard(
                title = "إجمالي الإيرادات",
                amount = "${decimalFormat.format(totalIncome)} ج.س",
                color = Color(0xFF2E7D32),
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "إجمالي المصروفات",
                amount = "${decimalFormat.format(totalExpense)} ج.س",
                color = Color(0xFFC62828),
                modifier = Modifier.weight(1f)
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (netBalance >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("صافي الربح / الخسارة", fontSize = 14.sp, color = Color.Gray)
                Text(
                    text = "${if (netBalance >= 0) "+" else ""}${decimalFormat.format(netBalance)} ج.س",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (netBalance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "تفاصيل الحركات",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredTransactions.sortedByDescending { it.timestamp }) { transaction ->
                TransactionReportItem(transaction, decimalFormat)
            }
        }
    }
}

@Composable
fun SummaryCard(title: String, amount: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(amount, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun TransactionReportItem(transaction: Transaction, decimalFormat: DecimalFormat) {
    val isIncome = transaction.type == "INCOME"
    val color = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
    val sdf = remember { SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault()) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                    .clip(RoundedCornerShape(20.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isIncome) "+" else "-", color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(transaction.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(transaction.category, fontSize = 12.sp, color = Color.Gray)
                Text(sdf.format(Date(transaction.timestamp)), fontSize = 10.sp, color = Color.Gray)
            }
            
            Text(
                text = "${decimalFormat.format(transaction.amount)} ج.س",
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 16.sp
            )
        }
    }
}
