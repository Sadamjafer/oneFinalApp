package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Account
import com.example.data.ProfitDeduction
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

sealed class ProfitLedgerEntry {
    abstract val timestamp: Long
    abstract val amount: Double
}

data class DailyProfitEntry(
    override val timestamp: Long,
    val dateString: String,
    val income: Double,
    val expense: Double,
    override val amount: Double
) : ProfitLedgerEntry()

data class DeductionEntry(
    val deduction: ProfitDeduction,
    override val timestamp: Long = deduction.timestamp,
    override val amount: Double = -deduction.amount
) : ProfitLedgerEntry()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitScreenView(
    viewModel: TransactionViewModel,
    account: Account,
    innerPadding: PaddingValues
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val profitDeductions by viewModel.allProfitDeductions.collectAsState()

    val decimalFormat = remember { DecimalFormat("#,##0.00") }
    
    val entriesWithBalance = remember(transactions, profitDeductions) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val groupedTransactions = transactions.groupBy { sdf.format(Date(it.timestamp)) }
        
        val dailyProfitEntries = groupedTransactions.map { (dateStr, dayTransactions) ->
            val income = dayTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
            val expense = dayTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            val profit = income - expense
            val parsedDate = sdf.parse(dateStr)
            val timestamp = parsedDate?.time ?: dayTransactions.first().timestamp
            
            DailyProfitEntry(
                timestamp = timestamp,
                dateString = dateStr,
                income = income,
                expense = expense,
                amount = profit
            )
        }

        val deductionEntries = profitDeductions.map {
            DeductionEntry(deduction = it)
        }

        val allEntries = (dailyProfitEntries + deductionEntries).sortedBy { it.timestamp }
        
        var currentBalance = 0.0
        val withBalance = allEntries.map { entry ->
            currentBalance += entry.amount
            Pair(entry, currentBalance)
        }
        withBalance.reversed()
    }

    var showDeductionDialog by remember { mutableStateOf(false) }
    var deductionAmount by remember { mutableStateOf("") }
    var deductionTitle by remember { mutableStateOf("") }
    var deductionToEdit by remember { mutableStateOf<ProfitDeduction?>(null) }
    var showEditDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = innerPadding.calculateBottomPadding())
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "سجل الأرباح والخصومات",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "حركة صافي الربح اليومي والخصومات",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = {
                    deductionToEdit = null
                    deductionAmount = ""
                    deductionTitle = ""
                    showDeductionDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = "خصم")
                Spacer(modifier = Modifier.width(4.dp))
                Text("خصم من الربح")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Table Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "التاريخ",
                        modifier = Modifier.weight(1f).padding(vertical = 12.dp, horizontal = 4.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)))
                    Text(
                        text = "البيان",
                        modifier = Modifier.weight(1.5f).padding(vertical = 12.dp, horizontal = 4.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)))
                    Text(
                        text = "المبلغ",
                        modifier = Modifier.weight(1f).padding(vertical = 12.dp, horizontal = 4.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)))
                    Text(
                        text = "الرصيد",
                        modifier = Modifier.weight(1f).padding(vertical = 12.dp, horizontal = 4.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f))
                
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    items(entriesWithBalance) { (entry, balance) ->
                        val displaySdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                        val displayDate = displaySdf.format(Date(entry.timestamp))
                        
                        val isDeduction = entry is DeductionEntry
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                                .clickable {
                                    if (entry is DeductionEntry) {
                                        deductionToEdit = entry.deduction
                                        showEditDeleteDialog = true
                                    }
                                }
                                .background(
                                    if (isDeduction) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) 
                                    else Color.Transparent
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = displayDate.replace(" 00:00", ""),
                                modifier = Modifier.weight(1f).padding(vertical = 10.dp, horizontal = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)))
                            Text(
                                text = if (entry is DailyProfitEntry) "صافي ربح ${entry.dateString}" else (entry as DeductionEntry).deduction.title,
                                modifier = Modifier.weight(1.5f).padding(vertical = 10.dp, horizontal = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = if (isDeduction) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isDeduction) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Start
                            )
                            Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)))
                            Text(
                                text = decimalFormat.format(entry.amount),
                                modifier = Modifier.weight(1f).padding(vertical = 10.dp, horizontal = 4.dp),
                                fontSize = 11.sp,
                                color = if (entry.amount >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.SemiBold
                            )
                            Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)))
                            Text(
                                text = decimalFormat.format(balance),
                                modifier = Modifier.weight(1f).padding(vertical = 10.dp, horizontal = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (balance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }

    if (showDeductionDialog) {
        AlertDialog(
            onDismissRequest = { showDeductionDialog = false },
            title = { Text(if (deductionToEdit != null) "تعديل الخصم" else "إضافة خصم من الربح") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = deductionAmount,
                        onValueChange = { 
                            if (it.all { char -> char.isDigit() || char == '.' }) {
                                deductionAmount = it
                            }
                        },
                        label = { Text("مبلغ الخصم") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        visualTransformation = NumberCommaTransformation()
                    )
                    OutlinedTextField(
                        value = deductionTitle,
                        onValueChange = { deductionTitle = it },
                        label = { Text("البيان (مثال: سحب مالك, ضيافة)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = deductionAmount.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            val title = if (deductionTitle.isNotBlank()) deductionTitle else "خصم"
                            if (deductionToEdit != null) {
                                viewModel.updateProfitDeduction(deductionToEdit!!, amount, title)
                            } else {
                                viewModel.addProfitDeduction(account.id, amount, title)
                            }
                            showDeductionDialog = false
                        }
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeductionDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showEditDeleteDialog && deductionToEdit != null) {
        AlertDialog(
            onDismissRequest = { showEditDeleteDialog = false },
            title = { Text("خيارات الخصم") },
            text = { Text("ماذا تريد أن تفعل بهذا الخصم؟") },
            confirmButton = {
                Button(
                    onClick = {
                        deductionAmount = deductionToEdit!!.amount.toString()
                        deductionTitle = deductionToEdit!!.title
                        showEditDeleteDialog = false
                        showDeductionDialog = true
                    }
                ) {
                    Text("تعديل")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        viewModel.deleteProfitDeduction(deductionToEdit!!)
                        showEditDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            }
        )
    }
}
