package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.PictureAsPdf
import com.example.utils.PdfGenerator

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
    innerPadding: PaddingValues,
    onNavigateBack: (() -> Unit)? = null
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val profitDeductions by viewModel.allProfitDeductions.collectAsState()

    val userLevel by viewModel.userLevel.collectAsState()
    var showApologyDialog by remember { mutableStateOf(false) }
    var apologyMessage by remember { mutableStateOf("") }

    val decimalFormat = remember { DecimalFormat("#,##0.00", java.text.DecimalFormatSymbols(java.util.Locale.US)) }
    
    var pdfUriToExport by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            pdfUriToExport = uri
        }
    }
    
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

    var showConfirmDeleteDeduction by remember { mutableStateOf<ProfitDeduction?>(null) }
    var showConfirmEditDeduction by remember { mutableStateOf<ProfitDeduction?>(null) }
    var pendingDeductionAmount by remember { mutableStateOf(0.0) }
    var pendingDeductionTitle by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = innerPadding.calculateBottomPadding())
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "سجل الأرباح والخصومات",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "حركة صافي الربح اليومي والخصومات",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
            val sdfForPdf = remember { java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", java.util.Locale.getDefault()) }
            IconButton(onClick = {
                val timestamp = sdfForPdf.format(java.util.Date())
                pdfLauncher.launch("profit_$timestamp.pdf")
            }) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = "تصدير كملف PDF",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = {
                    if (userLevel == 1) {
                        deductionToEdit = null
                        deductionAmount = ""
                        deductionTitle = ""
                        showDeductionDialog = true
                    } else {
                        showApologyDialog = true
                        apologyMessage = "عذراً، لا تمتلك الصلاحية لخصم مبالغ من الأرباح."
                    }
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
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
            ) {
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
                    Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline))
                    Text(
                        text = "البيان",
                        modifier = Modifier.weight(1.5f).padding(vertical = 12.dp, horizontal = 4.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline))
                    Text(
                        text = "المبلغ",
                        modifier = Modifier.weight(1f).padding(vertical = 12.dp, horizontal = 4.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline))
                    Text(
                        text = "الرصيد",
                        modifier = Modifier.weight(1f).padding(vertical = 12.dp, horizontal = 4.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
                
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
                
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    itemsIndexed(entriesWithBalance) { index, (entry, balance) ->
                        val displaySdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                        val displayDate = displaySdf.format(Date(entry.timestamp))
                        
                        val isDeduction = entry is DeductionEntry
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                                .clickable {
                                    if (entry is DeductionEntry) {
                                        if (userLevel == 1) {
                                            deductionToEdit = entry.deduction
                                            showEditDeleteDialog = true
                                        } else {
                                            showApologyDialog = true
                                            apologyMessage = "عذراً، لا تمتلك الصلاحية لتعديل أو حذف خصومات الأرباح."
                                        }
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
                            Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline))
                            Text(
                                text = if (entry is DailyProfitEntry) "صافي ربح ${entry.dateString}" else (entry as DeductionEntry).deduction.title,
                                modifier = Modifier.weight(1.5f).padding(vertical = 10.dp, horizontal = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = if (isDeduction) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isDeduction) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Start
                            )
                            Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline))
                            Text(
                                text = if (userLevel == 3) "████" else decimalFormat.format(entry.amount),
                                modifier = Modifier.weight(1f).padding(vertical = 10.dp, horizontal = 4.dp),
                                fontSize = 11.sp,
                                color = if (entry.amount >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.SemiBold
                            )
                            Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline))
                            Text(
                                text = if (userLevel == 3) "████" else decimalFormat.format(balance),
                                modifier = Modifier.weight(1f).padding(vertical = 10.dp, horizontal = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (balance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (index < entriesWithBalance.lastIndex) {
                            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
                        }
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
                                pendingDeductionAmount = amount
                                pendingDeductionTitle = title
                                showConfirmEditDeduction = deductionToEdit
                            } else {
                                viewModel.addProfitDeduction(account.id, amount, title)
                                showDeductionDialog = false
                            }
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
                        showConfirmDeleteDeduction = deductionToEdit
                        showEditDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            }
        )
    }

    if (showConfirmDeleteDeduction != null) {
        val deductionToDelete = showConfirmDeleteDeduction!!
        AlertDialog(
            onDismissRequest = { showConfirmDeleteDeduction = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تأكيد حذف الخصم", color = MaterialTheme.colorScheme.error)
                }
            },
            text = { Text("هل أنت متأكد من حذف هذا الخصم بقيمة (${decimalFormat.format(deductionToDelete.amount)} ج.س) والبيان (${deductionToDelete.title})؟\nسيتم حذفه نهائياً وتحديث الأرصدة.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProfitDeduction(deductionToDelete)
                        showConfirmDeleteDeduction = null
                        deductionToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("تأكيد الحذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteDeduction = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showConfirmEditDeduction != null) {
        val deductionToSave = showConfirmEditDeduction!!
        AlertDialog(
            onDismissRequest = { showConfirmEditDeduction = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تأكيد تعديل الخصم", color = MaterialTheme.colorScheme.primary)
                }
            },
            text = { Text("هل تريد حفظ التعديلات على هذا الخصم؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProfitDeduction(
                            deduction = deductionToSave,
                            newAmount = pendingDeductionAmount,
                            newTitle = pendingDeductionTitle
                        )
                        showConfirmEditDeduction = null
                        showDeductionDialog = false
                        deductionToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("تأكيد الحفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmEditDeduction = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    LaunchedEffect(pdfUriToExport) {
        pdfUriToExport?.let { uri ->
            val title = "سجل الأرباح والخصومات"
            val displaySdf = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault())

            val headers = listOf("الرصيد", "المبلغ", "البيان", "التاريخ")
            val data = mutableListOf<List<String>>()
            
            entriesWithBalance.forEach { (entry, balance) ->
                val displayDate = displaySdf.format(java.util.Date(entry.timestamp)).replace(" 00:00", "")
                val isDeduction = entry is DeductionEntry
                val statement = if (entry is DailyProfitEntry) "صافي ربح ${entry.dateString}" else (entry as DeductionEntry).deduction.title
                val amountStr = decimalFormat.format(entry.amount)
                
                data.add(listOf(
                    decimalFormat.format(balance),
                    amountStr,
                    statement,
                    displayDate
                ))
            }
            
            PdfGenerator.generatePdf(
                context = context,
                uri = uri,
                title = title,
                headers = headers,
                data = data,
                summary = listOf("الرصيد النهائي: ${decimalFormat.format(entriesWithBalance.firstOrNull()?.second ?: 0.0)}")
            )
            pdfUriToExport = null
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
