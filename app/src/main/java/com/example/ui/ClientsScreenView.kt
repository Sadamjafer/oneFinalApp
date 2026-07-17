package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Client
import com.example.data.ClientOperation
import java.text.DecimalFormat
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.PictureAsPdf
import com.example.utils.PdfGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreenView(viewModel: TransactionViewModel, onNavigateBack: (() -> Unit)? = null) {
    val clients by viewModel.clients.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val allClientOperations by viewModel.allClientOperations.collectAsState()
    val expenseTypes by viewModel.expenseTypes.collectAsState()
    val decimalFormat = remember { DecimalFormat("#,##0.##", java.text.DecimalFormatSymbols(java.util.Locale.US)) }

    val userLevel by viewModel.userLevel.collectAsState()
    var showApologyDialog by remember { mutableStateOf(false) }
    var apologyMessage by remember { mutableStateOf("") }

    var showAddClientDialog by remember { mutableStateOf(false) }
    var selectedClient by remember { mutableStateOf<Client?>(null) }

    if (selectedClient != null) {
        ClientDetailsView(
            client = selectedClient!!,
            allTransactions = allTransactions,
            decimalFormat = decimalFormat,
            viewModel = viewModel,
            onBack = { selectedClient = null }
        )
    } else {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        if (userLevel == 1) {
                            showAddClientDialog = true
                        } else {
                            showApologyDialog = true
                            apologyMessage = "عذراً، لا تمتلك الصلاحية لإضافة عملاء جدد."
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "إضافة عميل")
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Top Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
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
                        text = "دفتر العملاء والديون",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                if (clients.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "لا يوجد عملاء مضافين",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // LazyVerticalGrid for landscape support
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 350.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(clients) { client ->
                            val clientTransactions = remember(allTransactions, client) {
                                allTransactions.filter { it.type == "EXPENSE" && it.category == client.linkedExpenseCategory }
                            }
                            val totalExpense = remember(clientTransactions) {
                                clientTransactions.sumOf { it.amount }
                            }
                            val clientOps = remember(allClientOperations, client) {
                                allClientOperations.filter { it.clientId == client.id }
                            }
                            val totalPayment = remember(clientOps) {
                                clientOps.filter { it.type == "PAYMENT" }.sumOf { it.amount }
                            }
                            val totalDebt = remember(clientOps) {
                                clientOps.filter { it.type == "DEBT" }.sumOf { it.amount }
                            }
                            val cumulativeBalance = totalExpense + totalDebt - totalPayment

                            ClientCard(
                                client = client,
                                cumulativeBalance = cumulativeBalance,
                                decimalFormat = decimalFormat,
                                onClick = { selectedClient = client },
                                userLevel = userLevel
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddClientDialog) {
        var name by remember { mutableStateOf("") }
        var selectedExpenseCategory by remember { mutableStateOf("") }
        var showCategoryDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddClientDialog = false },
            title = { Text("إضافة عميل جديد") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("اسم العميل") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    ExposedDropdownMenuBox(
                        expanded = showCategoryDropdown,
                        onExpandedChange = { showCategoryDropdown = !showCategoryDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedExpenseCategory,
                            onValueChange = {},
                            label = { Text("النوع (قسم المنصرفات)") },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = showCategoryDropdown,
                            onDismissRequest = { showCategoryDropdown = false },
                        ) {
                            if (expenseTypes.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("لا توجد أنواع منصرفات، يرجى إضافتها أولاً") },
                                    onClick = { showCategoryDropdown = false }
                                )
                            } else {
                                expenseTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.name) },
                                        onClick = {
                                            selectedExpenseCategory = type.name
                                            showCategoryDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && selectedExpenseCategory.isNotBlank()) {
                            viewModel.addClient(name, selectedExpenseCategory)
                            showAddClientDialog = false
                        }
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddClientDialog = false }) {
                    Text("إلغاء")
                }
            }
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
fun ClientCard(
    client: Client,
    cumulativeBalance: Double,
    decimalFormat: DecimalFormat,
    onClick: () -> Unit,
    userLevel: Int = 1
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = client.name.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = client.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "النوع: ${client.linkedExpenseCategory}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "الرصيد التراكمي",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val absBalance = kotlin.math.abs(cumulativeBalance)
                val balanceText = if (userLevel == 3) "████" else when {
                    cumulativeBalance > 0 -> "${decimalFormat.format(absBalance)} (المتاح)"
                    cumulativeBalance < 0 -> "${decimalFormat.format(absBalance)} (عجز)"
                    else -> "خ خالص"
                }
                val balanceColor = when {
                    cumulativeBalance > 0 -> MaterialTheme.colorScheme.error
                    cumulativeBalance < 0 -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(
                    text = balanceText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = balanceColor
                )
            }
        }
    }
}

data class AvailableBalanceMovement(
    val timestamp: Long,
    val title: String,
    val type: String, // "EXPENSE" or "PAYMENT"
    val amount: Double,
    val balanceAfter: Double
)

@Composable
fun ClientDetailsView(
    client: Client,
    allTransactions: List<com.example.data.Transaction>,
    decimalFormat: DecimalFormat,
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val userLevel by viewModel.userLevel.collectAsState()
    var showApologyDialog by remember { mutableStateOf(false) }
    var apologyMessage by remember { mutableStateOf("") }

    // Filter expenses that match the linked category (and maybe containing client name? 
    // Wait, if it just links to an expense category, does it mean ALL expenses of that category belong to the client?
    // "تحتوي علي اجمالي الرصيد المتاح و هو اجمالي الصرف المقيد في المصروفات"
    // So yes, we filter transactions where type is "EXPENSE" and category is client.linkedExpenseCategory
    val clientTransactions = remember(allTransactions, client) {
        allTransactions.filter { it.type == "EXPENSE" && it.category == client.linkedExpenseCategory }
    }
    val totalExpense = remember(clientTransactions) {
        clientTransactions.sumOf { it.amount }
    }

    val operations by viewModel.getClientOperations(client.id).collectAsState(initial = emptyList())
    
    val totalAvailableBalance = remember(totalExpense, operations) {
        val totalPayment = operations.filter { it.type == "PAYMENT" }.sumOf { it.amount }
        totalExpense - totalPayment
    }
    
    val operationsWithBalance = remember(operations) {
        val sortedOps = operations.sortedBy { it.timestamp }
        var currentBalance = 0.0
        val result = mutableListOf<Pair<ClientOperation, Double>>()
        for (op in sortedOps) {
            if (op.type == "DEBT") {
                currentBalance -= op.amount
            } else {
                currentBalance += op.amount
            }
            result.add(op to currentBalance)
        }
        result.reversed()
    }

    val availableBalanceMovements = remember(clientTransactions, operations) {
        val expenseItems = clientTransactions.map { tx ->
            AvailableBalanceMovement(
                timestamp = tx.timestamp,
                title = tx.title,
                type = "EXPENSE",
                amount = tx.amount,
                balanceAfter = 0.0
            )
        }
        val paymentItems = operations.filter { it.type == "PAYMENT" }.map { op ->
            AvailableBalanceMovement(
                timestamp = op.timestamp,
                title = op.title,
                type = "PAYMENT",
                amount = op.amount,
                balanceAfter = 0.0
            )
        }
        val combinedSorted = (expenseItems + paymentItems).sortedBy { it.timestamp }
        
        var currentBalance = 0.0
        val movements = mutableListOf<AvailableBalanceMovement>()
        for (item in combinedSorted) {
            if (item.type == "EXPENSE") {
                currentBalance += item.amount
            } else {
                currentBalance -= item.amount
            }
            movements.add(item.copy(balanceAfter = currentBalance))
        }
        movements.reversed()
    }
    
    var showOperationDialog by remember { mutableStateOf(false) }
    var operationType by remember { mutableStateOf("DEBT") }
    var operationAmount by remember { mutableStateOf("") }
    var operationTitle by remember { mutableStateOf("") }
    var operationToEdit by remember { mutableStateOf<ClientOperation?>(null) }
    var showEditDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showEditConfirmDialog by remember { mutableStateOf(false) }
    var pendingOperationAmount by remember { mutableStateOf(0.0) }
    var pendingOperationTitle by remember { mutableStateOf("") }
    var pendingOperationType by remember { mutableStateOf("") }

    var pdfUriToExport by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            pdfUriToExport = uri
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("◀ عودة", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = client.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            val sdfForPdf = remember { java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm", java.util.Locale.getDefault()) }
            IconButton(onClick = {
                val timestamp = sdfForPdf.format(java.util.Date())
                pdfLauncher.launch("client_${client.name}_$timestamp.pdf")
            }) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = "تصدير كملف PDF",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "إجمالي الرصيد المتاح (المنصرف)",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (userLevel == 3) "████ ج.س" else "${decimalFormat.format(totalAvailableBalance)} ج.س",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    if (userLevel == 1) {
                        operationToEdit = null
                        operationType = "DEBT"
                        operationTitle = "مديونية جديدة"
                        operationAmount = ""
                        showOperationDialog = true
                    } else {
                        showApologyDialog = true
                        apologyMessage = "عذراً، لا تمتلك الصلاحية لتقييد مديونيات للعملاء."
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("تقييد مديونية", fontSize = 14.sp)
            }

            Button(
                onClick = {
                    if (userLevel == 1) {
                        operationToEdit = null
                        operationType = "PAYMENT"
                        operationTitle = "سداد"
                        operationAmount = ""
                        showOperationDialog = true
                    } else {
                        showApologyDialog = true
                        apologyMessage = "عذراً، لا تمتلك الصلاحية لتقييد سداد للعملاء."
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("تقييد سداد", fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "العمليات المنجزة",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "💡 اضغط للتعديل أو الحذف",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (operations.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد عمليات منجزة", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                            modifier = Modifier
                                .weight(1.2f)
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline))
                        Text(
                            text = "البيان",
                            modifier = Modifier
                                .weight(1.5f)
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline))
                        Text(
                            text = "مديونية",
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline))
                        Text(
                            text = "سداد",
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline))
                        Text(
                            text = "الرصيد",
                            modifier = Modifier
                                .weight(1.2f)
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        operationsWithBalance.forEachIndexed { index, (op, balance) ->
                            val sdf = java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault())
                            val isDebt = op.type == "DEBT"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min)
                                    .clickable {
                                        if (userLevel == 1) {
                                            operationToEdit = op
                                            showEditDeleteDialog = true
                                        } else {
                                            showApologyDialog = true
                                            apologyMessage = "عذراً، لا تمتلك الصلاحية لتعديل أو حذف مديونيات العملاء."
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sdf.format(java.util.Date(op.timestamp)),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline))
                                Text(
                                    text = op.title,
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Start
                                )
                                Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline))
                                Text(
                                    text = if (userLevel == 3) "████" else if (isDebt) decimalFormat.format(op.amount) else "-",
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDebt) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center
                                )
                                Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline))
                                Text(
                                    text = if (userLevel == 3) "████" else if (!isDebt) decimalFormat.format(op.amount) else "-",
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (!isDebt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center
                                )
                                Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline))
                                Text(
                                    text = if (userLevel == 3) "████" else decimalFormat.format(balance),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                            }
                            if (index < operationsWithBalance.lastIndex) {
                                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "حركة الرصيد المتاح (المنصرف والسداد)",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (availableBalanceMovements.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد حركة للرصيد المتاح", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                            modifier = Modifier
                                .weight(1.2f)
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline))
                        Text(
                            text = "البيان",
                            modifier = Modifier
                                .weight(1.5f)
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline))
                        Text(
                            text = "منصرف (+)",
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline))
                        Text(
                            text = "سداد (-)",
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline))
                        Text(
                            text = "الرصيد المتاح",
                            modifier = Modifier
                                .weight(1.2f)
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        availableBalanceMovements.forEachIndexed { index, movement ->
                            val sdf = java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault())
                            val isExpense = movement.type == "EXPENSE"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sdf.format(java.util.Date(movement.timestamp)),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline))
                                Text(
                                    text = movement.title,
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Start
                                )
                                Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline))
                                Text(
                                    text = if (userLevel == 3) "████" else if (isExpense) decimalFormat.format(movement.amount) else "-",
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isExpense) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center
                                )
                                Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline))
                                Text(
                                    text = if (userLevel == 3) "████" else if (!isExpense) decimalFormat.format(movement.amount) else "-",
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (!isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center
                                )
                                Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outline))
                                Text(
                                    text = if (userLevel == 3) "████" else decimalFormat.format(movement.balanceAfter),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                            }
                            if (index < availableBalanceMovements.lastIndex) {
                                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showOperationDialog) {
        val titleText = if (operationType == "DEBT") "تقييد مديونية جديدة" else "تقييد سداد"
        AlertDialog(
            onDismissRequest = { showOperationDialog = false },
            title = { Text(titleText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = operationAmount,
                        onValueChange = { 
                            if (it.all { char -> char.isDigit() || char == '.' }) {
                                operationAmount = it
                            }
                        },
                        label = { Text("المبلغ") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        visualTransformation = NumberCommaTransformation()
                    )
                    OutlinedTextField(
                        value = operationTitle,
                        onValueChange = { operationTitle = it },
                        label = { Text("البيان (اختياري)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = operationAmount.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            val finalTitle = if (operationTitle.isNotBlank()) operationTitle else (if (operationType == "DEBT") "مديونية" else "سداد")
                            if (operationToEdit != null) {
                                pendingOperationAmount = amount
                                pendingOperationTitle = finalTitle
                                pendingOperationType = operationType
                                showEditConfirmDialog = true
                            } else {
                                viewModel.addClientOperation(
                                    clientId = client.id,
                                    type = operationType,
                                    amount = amount,
                                    title = finalTitle
                                )
                                showOperationDialog = false
                            }
                        }
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOperationDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showEditDeleteDialog && operationToEdit != null) {
        AlertDialog(
            onDismissRequest = { showEditDeleteDialog = false },
            title = { Text("خيارات العملية") },
            text = { Text("ماذا تريد أن تفعل بهذه العملية؟") },
            confirmButton = {
                Button(
                    onClick = {
                        operationAmount = operationToEdit!!.amount.toString()
                        operationTitle = operationToEdit!!.title
                        operationType = operationToEdit!!.type
                        showEditDeleteDialog = false
                        showOperationDialog = true
                    }
                ) {
                    Text("تعديل")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = true
                        showEditDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            }
        )
    }

    if (showDeleteConfirmDialog && operationToEdit != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تأكيد الحذف", color = MaterialTheme.colorScheme.error)
                }
            },
            text = {
                Text("هل أنت متأكد من حذف هذه العملية بقيمة (${decimalFormat.format(operationToEdit!!.amount)} ج.س)؟\nسيتم حذف العملية نهائياً وتحديث حساب العميل.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteClientOperation(operationToEdit!!)
                        showDeleteConfirmDialog = false
                        operationToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("تأكيد الحذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showEditConfirmDialog && operationToEdit != null) {
        AlertDialog(
            onDismissRequest = { showEditConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تأكيد التعديل", color = MaterialTheme.colorScheme.primary)
                }
            },
            text = {
                Text("هل أنت متأكد من حفظ التغييرات على هذه العملية؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateClientOperation(
                            operation = operationToEdit!!,
                            newType = pendingOperationType,
                            newAmount = pendingOperationAmount,
                            newTitle = pendingOperationTitle
                        )
                        showEditConfirmDialog = false
                        showOperationDialog = false
                        operationToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("تأكيد التعديل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditConfirmDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    LaunchedEffect(pdfUriToExport) {
        pdfUriToExport?.let { uri ->
            val accountName = viewModel.currentAccount.value?.name ?: ""
            val title = "كشف حساب العميل: ${client.name}" + if (accountName.isNotEmpty()) " - $accountName" else ""
            val sdf = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())

            val operationsHeaders = listOf("الرصيد", "سداد", "مديونية", "البيان", "التاريخ")
            val operationsData = mutableListOf<List<String>>()
            operationsWithBalance.forEach { (op, balance) ->
                val isDebt = op.type == "DEBT"
                val dateStr = sdf.format(java.util.Date(op.timestamp))
                val debtAmt = if (isDebt) decimalFormat.format(op.amount) else "-"
                val payAmt = if (!isDebt) decimalFormat.format(op.amount) else "-"
                operationsData.add(listOf(
                    decimalFormat.format(balance),
                    payAmt,
                    debtAmt,
                    op.title,
                    dateStr
                ))
            }
            
            PdfGenerator.generatePdf(
                context = context,
                uri = uri,
                title = title,
                headers = operationsHeaders,
                data = operationsData,
                summary = listOf(
                    "إجمالي الديون: ${decimalFormat.format(operationsWithBalance.firstOrNull()?.second ?: 0.0)}",
                    "الرصيد المتاح: ${decimalFormat.format(availableBalanceMovements.lastOrNull()?.balanceAfter ?: 0.0)}"
                )
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
