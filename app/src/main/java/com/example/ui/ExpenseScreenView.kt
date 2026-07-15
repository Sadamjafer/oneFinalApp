package com.example.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ExpenseType
import com.example.data.Transaction
import com.example.data.Client
import com.example.ui.TransactionViewModel
import com.example.util.DateUtils
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreenView(
    viewModel: TransactionViewModel,
    innerPadding: PaddingValues,
    onNavigateBack: () -> Unit
) {
    val expenseTypes by viewModel.expenseTypes.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val currentAccount by viewModel.currentAccount.collectAsState()

    var showAddTypeDialog by remember { mutableStateOf(false) }
    var showEditTypeDialog by remember { mutableStateOf<ExpenseType?>(null) }
    var showDeleteTypeDialog by remember { mutableStateOf<ExpenseType?>(null) }
    var showRegisterAmountDialog by remember { mutableStateOf<ExpenseType?>(null) }
    var showEditPaymentDialog by remember { mutableStateOf<Transaction?>(null) }
    var showConfirmDeleteTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showConfirmEditTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showConfirmEditTypeDialog by remember { mutableStateOf<ExpenseType?>(null) }
    var pendingExpenseTypeNewName by remember { mutableStateOf("") }

    val decimalFormat = remember { DecimalFormat("#,##0.##") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = innerPadding.calculateBottomPadding())
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
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
                    contentDescription = "العودة للرئيسية",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "مسجل المصروفات",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "تسجيل بنود المصروفات والمدفوعات وإدارة تفاصيلها",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }

        // Active Store Badge
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Store,
                    contentDescription = "المحل الحالي",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "الحساب / المحل النشط حالياً: ",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
                Text(
                    text = currentAccount?.name ?: "غير محدد",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Action Button: Add new Expense Type
        Button(
            onClick = { showAddTypeDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "إضافة اسم نوع مصروف"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "إضافة اسم نوع المصروف",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Expense Title List Label
        Text(
            text = "بطاقات المصروفات المسجلة:",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )

        // Main Content Area
        if (expenseTypes.isEmpty()) {
            // Beautiful Empty State
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                            RoundedCornerShape(24.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "💸",
                            fontSize = 50.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            text = "قائمة المصروفات فارغة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لم تقم بإضافة أي نوع مصروف حتى الآن.\nاضغط على زر 'إضافة اسم نوع المصروف' أعلاه لتتمكن من إنشاء تصنيفات وتثبيت مبالغ النفقات والمصروفات الخاصة بك.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        } else {
            // LazyVerticalGrid showing successive cards
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 350.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(expenseTypes, key = { it.id }) { expenseType ->
                    val typeTransactions = remember(transactions, expenseType.name) {
                        transactions.filter { it.type == "EXPENSE" && it.category == expenseType.name }
                    }
                    val totalAmount = remember(typeTransactions) {
                        typeTransactions.sumOf { it.amount }
                    }
                    val count = typeTransactions.size

                    val todayStr = remember { DateUtils.formatLocal(System.currentTimeMillis()) }
                    val todayTransactions = remember(typeTransactions, todayStr) {
                        typeTransactions.filter { DateUtils.formatLocal(it.timestamp) == todayStr }
                    }
                    val latestTodayTx = remember(todayTransactions) {
                        todayTransactions.maxByOrNull { it.timestamp }
                    }

                    var isExpanded by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { isExpanded = !isExpanded }
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                RoundedCornerShape(16.dp)
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // Card Header: Icon & Name
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📉", fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = expenseType.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (latestTodayTx != null) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "${decimalFormat.format(latestTodayTx.amount)} ج.س (${DateUtils.formatLocal(latestTodayTx.timestamp)})",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "تاريخ الإنشاء: ${SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH).format(Date(expenseType.timestamp))}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "إغلاق التفاصيل" else "عرض التفاصيل",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(14.dp))

                                // Stats container inside the card
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "إجمالي المصروفات:",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = "${decimalFormat.format(totalAmount)} ج.س",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }

                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "$count دفعات",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                if (typeTransactions.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "الدفعات الموثقة تحت هذا المصروف:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val sortedTypeTransactions = remember(typeTransactions) {
                                            typeTransactions.sortedByDescending { it.timestamp }
                                        }
                                        sortedTypeTransactions.forEach { transaction ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH).format(Date(transaction.timestamp)),
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        if (transaction.notes.isNotBlank()) {
                                                            Text(
                                                                text = "- ${transaction.notes}",
                                                                fontSize = 11.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(
                                                        text = "${decimalFormat.format(transaction.amount)} ج.س",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "تعديل الدفعة",
                                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                                        modifier = Modifier
                                                            .size(16.dp)
                                                            .clickable {
                                                                showEditPaymentDialog = transaction
                                                            }
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "حذف الدفعة",
                                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                        modifier = Modifier
                                                            .size(16.dp)
                                                            .clickable {
                                                                showConfirmDeleteTransaction = transaction
                                                            }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Action buttons at bottom of card
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Add Amount / Register Payment Button
                                    Button(
                                        onClick = { showRegisterAmountDialog = expenseType },
                                        modifier = Modifier.weight(1.3f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        ),
                                        contentPadding = PaddingValues(vertical = 4.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddCircle,
                                            contentDescription = "تسجيل مبلغ",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تسجيل مبلغ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Rename Button
                                    OutlinedButton(
                                        onClick = { showEditTypeDialog = expenseType },
                                        modifier = Modifier.weight(0.9f),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.primary
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "تعديل الاسم",
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تعديل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Delete Button
                                    OutlinedButton(
                                        onClick = { showDeleteTypeDialog = expenseType },
                                        modifier = Modifier.weight(0.9f),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف نوع المصروف",
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("حذف", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Dialogs ---

    // 1. Popup Dialog to Add Expense Type Name
    if (showAddTypeDialog) {
        Dialog(onDismissRequest = { showAddTypeDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                var name by remember { mutableStateOf("") }
                var showError by remember { mutableStateOf(false) }

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "إضافة اسم نوع المصروف",
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
                            label = { Text("اسم نوع المصروف") },
                            placeholder = { Text("مثال: رواتب، إيجار، كهرباء، بضاعة...") },
                            singleLine = true,
                            isError = showError,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (showError) {
                            Text(
                                text = "الرجاء إدخال اسم صحيح لنوع المصروف",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TextButton(
                                onClick = { showAddTypeDialog = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("إلغاء", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Button(
                                onClick = {
                                    if (name.isNotBlank()) {
                                        viewModel.addExpenseType(name.trim())
                                        showAddTypeDialog = false
                                    } else {
                                        showError = true
                                    }
                                },
                                modifier = Modifier.weight(1.5f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("حفظ", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // 2. Popup Dialog to Edit Expense Type Name
    if (showEditTypeDialog != null) {
        val expenseType = showEditTypeDialog!!
        Dialog(onDismissRequest = { showEditTypeDialog = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                var name by remember { mutableStateOf(expenseType.name) }
                var showError by remember { mutableStateOf(false) }

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "تعديل اسم نوع المصروف",
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
                            label = { Text("الاسم الجديد") },
                            singleLine = true,
                            isError = showError,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (showError) {
                            Text(
                                text = "الرجاء إدخال اسم صحيح وغير فارغ",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TextButton(
                                onClick = { showEditTypeDialog = null },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("إلغاء", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Button(
                                onClick = {
                                    if (name.isNotBlank()) {
                                        pendingExpenseTypeNewName = name.trim()
                                        showConfirmEditTypeDialog = expenseType
                                    } else {
                                        showError = true
                                    }
                                },
                                modifier = Modifier.weight(1.5f),
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
    }

    // 3. Popup Dialog to Delete Expense Type
    if (showDeleteTypeDialog != null) {
        val expenseType = showDeleteTypeDialog!!
        val hasTransactions = transactions.any { it.type == "EXPENSE" && it.category == expenseType.name }
        val linkedClientsList = clients.filter { it.linkedExpenseCategory == expenseType.name }
        val hasClients = linkedClientsList.isNotEmpty()
        val isPrevented = hasTransactions || hasClients

        Dialog(onDismissRequest = { showDeleteTypeDialog = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.errorContainer, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "تحذير الحذف",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (isPrevented) "غير مسموح بالحذف" else "حذف نوع المصروف",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.error
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isPrevented) {
                            val reasonMsg = buildString {
                                append("لا يمكن حذف بند المصروفات '${expenseType.name}' للأسباب التالية:\n\n")
                                if (hasTransactions) {
                                    append("• توجد مبالغ مالية ومعاملات مسجلة ومقيدة تحت هذا البند.\n")
                                }
                                if (hasClients) {
                                    append("• هذا المنصرف مرتبط مع حسابات العملاء الآتية:\n")
                                    linkedClientsList.forEach { client ->
                                        append("   - ${client.name}\n")
                                    }
                                }
                                append("\nيرجى تعديل أو حذف المعاملات المرتبطة أو تغيير ارتباط العملاء لتتمكن من حذف هذا البند.")
                            }
                            Text(
                                text = reasonMsg,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Start,
                                lineHeight = 20.sp,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = { showDeleteTypeDialog = null },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("حسنًا، مفهوم", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text(
                                text = "هل أنت متأكد من حذف نوع المصروف '${expenseType.name}'؟\n\nتنبيه: لن يتم حذف المعاملات المالية المقيدة في الصفحة الرئيسية، ولكن سيزول ربطها بهذا النوع.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TextButton(
                                    onClick = { showDeleteTypeDialog = null },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("إلغاء", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Button(
                                    onClick = {
                                        viewModel.deleteExpenseType(expenseType)
                                        showDeleteTypeDialog = null
                                    },
                                    modifier = Modifier.weight(1.5f),
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
    }

    // 4. Popup Dialog to Register Amount under Expense Type
    if (showRegisterAmountDialog != null) {
        val expenseType = showRegisterAmountDialog!!
        Dialog(onDismissRequest = { showRegisterAmountDialog = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                val todayDate = remember { SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH).format(Date()) }
                var dateStr by remember { mutableStateOf(todayDate) }
                var amountStr by remember { mutableStateOf("") }
                var noteStr by remember { mutableStateOf("") }
                var showDateError by remember { mutableStateOf(false) }
                var showError by remember { mutableStateOf(false) }

                var showDatePicker by remember { mutableStateOf(false) }
                val utcSdf = remember { SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH).apply { timeZone = TimeZone.getTimeZone("UTC") } }
                val initialTime = remember(todayDate) {
                    try {
                        utcSdf.parse(todayDate)?.time
                    } catch (e: Exception) {
                        null
                    } ?: System.currentTimeMillis()
                }
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialTime)

                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let {
                                    dateStr = utcSdf.format(Date(it))
                                }
                                showDatePicker = false
                            }) {
                                Text("موافق")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) {
                                Text("إلغاء")
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "تسجيل مبلغ مصروف جديد",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.error
                        )

                        Text(
                            text = "لنوع المصروف: ${expenseType.name}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Date Field (Defaults to Today)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = dateStr,
                                onValueChange = {},
                                label = { Text("تاريخ العملية") },
                                readOnly = true,
                                isError = showDateError,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                            )
                            // Transparent clickable overlay to trigger date picker anywhere on the field
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { showDatePicker = true }
                            )
                        }
                        if (showDateError) {
                            Text(
                                text = "الرجاء إدخال تاريخ صحيح غير فارغ",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Amount Input (Mandatory)
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = {
                                if (it.all { char -> char.isDigit() || char == '.' }) {
                                    amountStr = it
                                    showError = false
                                }
                            },
                            label = { Text("المبلغ (بالجنيه السوداني)") },
                            placeholder = { Text("أدخل قيمة المبلغ المدفوع") },
                            singleLine = true,
                            isError = showError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = NumberCommaTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.error,
                                focusedLabelColor = MaterialTheme.colorScheme.error
                            )
                        )

                        if (showError) {
                            Text(
                                text = "الرجاء إدخال مبلغ صحيح أكبر من الصفر",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Notes (Optional)
                        OutlinedTextField(
                            value = noteStr,
                            onValueChange = { noteStr = it },
                            label = { Text("الملاحظات (اختياري)") },
                            placeholder = { Text("تفاصيل إضافية عن الدفعة...") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TextButton(
                                onClick = { showRegisterAmountDialog = null },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("إلغاء", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Button(
                                onClick = {
                                    val amountValue = amountStr.toDoubleOrNull()
                                    val isDateValid = dateStr.isNotBlank()
                                    val isAmountValid = amountValue != null && amountValue > 0.0

                                    if (!isDateValid) showDateError = true
                                    if (!isAmountValid) showError = true

                                    if (isDateValid && isAmountValid) {
                                        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH)
                                        val dateObj = try { sdf.parse(dateStr.trim()) } catch(e: Exception) { null }
                                        val timestampValue = dateObj?.time ?: System.currentTimeMillis()

                                        viewModel.addTransaction(
                                            title = "مصروف: ${expenseType.name}",
                                            amount = amountValue!!,
                                            type = "EXPENSE",
                                            category = expenseType.name,
                                            notes = noteStr.trim(),
                                            timestamp = timestampValue
                                        )
                                        showRegisterAmountDialog = null
                                    }
                                },
                                modifier = Modifier.weight(1.5f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("حفظ المبلغ", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // 5. Popup Dialog to Edit Payment under Expense Type
    if (showEditPaymentDialog != null) {
        val transaction = showEditPaymentDialog!!
        Dialog(onDismissRequest = { showEditPaymentDialog = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                val initialDate = remember(transaction) {
                    SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH).format(Date(transaction.timestamp))
                }
                var dateStr by remember { mutableStateOf(initialDate) }
                var amountStr by remember { mutableStateOf(transaction.amount.toString()) }
                var noteStr by remember { mutableStateOf(transaction.notes) }
                var showDateError by remember { mutableStateOf(false) }
                var showError by remember { mutableStateOf(false) }

                var showDatePicker by remember { mutableStateOf(false) }
                val utcSdf = remember { SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH).apply { timeZone = TimeZone.getTimeZone("UTC") } }
                val initialTime = remember(transaction) {
                    try {
                        utcSdf.parse(initialDate)?.time
                    } catch (e: Exception) {
                        null
                    } ?: transaction.timestamp
                }
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialTime)

                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let {
                                    dateStr = utcSdf.format(Date(it))
                                }
                                showDatePicker = false
                            }) {
                                Text("موافق")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) {
                                Text("إلغاء")
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "تعديل دفعة المصروف",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "المصروف: ${transaction.category}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Date Field
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = dateStr,
                                onValueChange = {},
                                label = { Text("تاريخ العملية") },
                                readOnly = true,
                                isError = showDateError,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                            )
                            // Transparent clickable overlay to trigger date picker anywhere on the field
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { showDatePicker = true }
                            )
                        }
                        if (showDateError) {
                            Text(
                                text = "الرجاء إدخال تاريخ صحيح غير فارغ",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Amount Input
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = {
                                if (it.all { char -> char.isDigit() || char == '.' }) {
                                    amountStr = it
                                    showError = false
                                }
                            },
                            label = { Text("المبلغ (بالجنيه السوداني)") },
                            placeholder = { Text("أدخل قيمة المبلغ المدفوع") },
                            singleLine = true,
                            isError = showError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = NumberCommaTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (showError) {
                            Text(
                                text = "الرجاء إدخال مبلغ صحيح أكبر من الصفر",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Notes
                        OutlinedTextField(
                            value = noteStr,
                            onValueChange = { noteStr = it },
                            label = { Text("الملاحظات (اختياري)") },
                            placeholder = { Text("تفاصيل إضافية عن الدفعة...") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TextButton(
                                onClick = { showEditPaymentDialog = null },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("إلغاء", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Button(
                                onClick = {
                                    val amountValue = amountStr.toDoubleOrNull()
                                    val isDateValid = dateStr.isNotBlank()
                                    val isAmountValid = amountValue != null && amountValue > 0.0

                                    if (!isDateValid) showDateError = true
                                    if (!isAmountValid) showError = true

                                    if (isDateValid && isAmountValid) {
                                        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH)
                                        val dateObj = try { sdf.parse(dateStr.trim()) } catch(e: Exception) { null }
                                        val timestampValue = dateObj?.time ?: transaction.timestamp

                                        showConfirmEditTransaction = transaction.copy(
                                            amount = amountValue!!,
                                            notes = noteStr.trim(),
                                            timestamp = timestampValue
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1.5f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("تعديل المبلغ", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfirmDeleteTransaction != null) {
        val txToDelete = showConfirmDeleteTransaction!!
        AlertDialog(
            onDismissRequest = { showConfirmDeleteTransaction = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تأكيد حذف الدفعة", color = MaterialTheme.colorScheme.error)
                }
            },
            text = { Text("هل أنت متأكد من حذف هذه الدفعة بقيمة (${decimalFormat.format(txToDelete.amount)} ج.س)؟\nسيتم حذفها نهائياً ولا يمكن استرجاعها.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(txToDelete)
                        showConfirmDeleteTransaction = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("تأكيد الحذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteTransaction = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showConfirmEditTransaction != null) {
        val txToEdit = showConfirmEditTransaction!!
        AlertDialog(
            onDismissRequest = { showConfirmEditTransaction = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تأكيد تعديل الدفعة", color = MaterialTheme.colorScheme.primary)
                }
            },
            text = { Text("هل أنت متأكد من حفظ التعديلات على هذه الدفعة؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateTransaction(txToEdit)
                        showConfirmEditTransaction = null
                        showEditPaymentDialog = null
                    }
                ) {
                    Text("تأكيد")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmEditTransaction = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showConfirmEditTypeDialog != null) {
        val typeToEdit = showConfirmEditTypeDialog!!
        AlertDialog(
            onDismissRequest = { showConfirmEditTypeDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تأكيد تعديل اسم نوع المصروف", color = MaterialTheme.colorScheme.primary)
                }
            },
            text = { Text("هل تريد حفظ الاسم الجديد '${pendingExpenseTypeNewName}' لهذا المصروف؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateExpenseType(typeToEdit, pendingExpenseTypeNewName)
                        showConfirmEditTypeDialog = null
                        showEditTypeDialog = null
                    }
                ) {
                    Text("تأكيد")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmEditTypeDialog = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
