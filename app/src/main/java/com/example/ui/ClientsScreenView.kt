package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreenView(viewModel: TransactionViewModel) {
    val clients by viewModel.clients.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val expenseTypes by viewModel.expenseTypes.collectAsState()
    val decimalFormat = remember { DecimalFormat("#,##0.##") }

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
                    onClick = { showAddClientDialog = true },
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
                if (clients.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "لا يوجد عملاء مضافين",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(clients) { client ->
                            ClientCard(
                                client = client,
                                onClick = { selectedClient = client }
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
}

@Composable
fun ClientCard(client: Client, onClick: () -> Unit) {
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
            Column {
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
        }
    }
}

@Composable
fun ClientDetailsView(
    client: Client,
    allTransactions: List<com.example.data.Transaction>,
    decimalFormat: DecimalFormat,
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
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
    
    var showOperationDialog by remember { mutableStateOf(false) }
    var operationType by remember { mutableStateOf("DEBT") }
    var operationAmount by remember { mutableStateOf("") }
    var operationTitle by remember { mutableStateOf("") }
    var operationToEdit by remember { mutableStateOf<ClientOperation?>(null) }
    var showEditDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                color = MaterialTheme.colorScheme.onBackground
            )
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
                    text = "${decimalFormat.format(totalAvailableBalance)} ج.س",
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
                    operationToEdit = null
                    operationType = "DEBT"
                    operationTitle = "مديونية جديدة"
                    operationAmount = ""
                    showOperationDialog = true
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("تقييد مديونية", fontSize = 14.sp)
            }

            Button(
                onClick = {
                    operationToEdit = null
                    operationType = "PAYMENT"
                    operationTitle = "سداد"
                    operationAmount = ""
                    showOperationDialog = true
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("تقييد سداد", fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "العمليات المنجزة",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (operations.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد عمليات منجزة", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("التاريخ", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("البيان", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("مديونية", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
                        Text("سداد", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
                        Text("الرصيد", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurface)
                    }
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(operationsWithBalance) { (op, balance) ->
                            val sdf = java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault())
                            val isDebt = op.type == "DEBT"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = {
                                                operationToEdit = op
                                                showEditDeleteDialog = true
                                            }
                                        )
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sdf.format(java.util.Date(op.timestamp)),
                                    modifier = Modifier.weight(1.2f),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = op.title,
                                    modifier = Modifier.weight(1.5f),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isDebt) decimalFormat.format(op.amount) else "-",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDebt) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (!isDebt) decimalFormat.format(op.amount) else "-",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (!isDebt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = decimalFormat.format(balance),
                                    modifier = Modifier.weight(1.2f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.End
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
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
                        onValueChange = { operationAmount = it },
                        label = { Text("المبلغ") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
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
                                viewModel.updateClientOperation(
                                    operation = operationToEdit!!,
                                    newType = operationType,
                                    newAmount = amount,
                                    newTitle = finalTitle
                                )
                            } else {
                                viewModel.addClientOperation(
                                    clientId = client.id,
                                    type = operationType,
                                    amount = amount,
                                    title = finalTitle
                                )
                            }
                            showOperationDialog = false
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
                        viewModel.deleteClientOperation(operationToEdit!!)
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
