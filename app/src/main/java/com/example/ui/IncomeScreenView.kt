package com.example.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.IncomeType
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeScreenView(
    viewModel: TransactionViewModel,
    innerPadding: PaddingValues,
    onNavigateBack: () -> Unit
) {
    val incomeTypes by viewModel.incomeTypes.collectAsState()
    val currentAccount by viewModel.currentAccount.collectAsState()

    var showAddTypeDialog by remember { mutableStateOf(false) }
    var showEditTypeDialog by remember { mutableStateOf<IncomeType?>(null) }
    var showDeleteTypeDialog by remember { mutableStateOf<IncomeType?>(null) }

    var showConfirmEditTypeDialog by remember { mutableStateOf<IncomeType?>(null) }
    var pendingNewName by remember { mutableStateOf("") }
    var pendingConsumedBags by remember { mutableStateOf(0) }
    var pendingNewAmount by remember { mutableStateOf(0.0) }
    var pendingNewNotes by remember { mutableStateOf("") }

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
                    text = "مسجل الإيرادات اليومية",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "إدارة الإيرادات حسب التواريخ، كمية الشوالات، ومبالغ الدخل",
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

        // Main Action Button: Add Today's Revenue
        Button(
            onClick = { showAddTypeDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "تسجيل إيراد يوم جديد"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "تسجيل إيراد يوم جديد",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // List Label
        Text(
            text = "سجل الإيرادات اليومية الموثقة:",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )

        // Main Content Area
        if (incomeTypes.isEmpty()) {
            // Empty State
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
                            text = "📈",
                            fontSize = 50.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Text(
                            text = "سجل الإيرادات فارغ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لم تقم بتسجيل أي إيرادات يومية بعد.\nاضغط على زر 'تسجيل إيراد يوم جديد' في الأعلى لتوثيق تاريخ اليوم، عدد الشوالات المستهلكة، ومبلغ الإيراد الإجمالي.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(incomeTypes, key = { it.id }) { incomeType ->
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
                            // Header with Date (name)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📅", fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "إيراد تاريخ: ${incomeType.name}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "تم التوثيق: ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(incomeType.timestamp))}",
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
                                Spacer(modifier = Modifier.height(12.dp))

                                // Main Fields Container
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // 1. Consumed Bags
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🌾", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "عدد الشوالات المستهلكة:",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = "${incomeType.consumedBags} شوال",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))

                                    // 2. Revenue Amount
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("💰", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "مبلغ إيرادات اليوم:",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = "${decimalFormat.format(incomeType.amount)} ج.س",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF2E7D32) // Forest green for positive income
                                        )
                                    }

                                    if (incomeType.notes.isNotBlank()) {
                                        Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))

                                        // 3. Daily Notes
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("📝", fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "ملاحظة إيرادات اليوم:",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = incomeType.notes,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                                lineHeight = 18.sp,
                                                modifier = Modifier.padding(start = 22.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Edit / Delete Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showEditTypeDialog = incomeType },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.primary
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "تعديل الإيراد",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("تعديل البيانات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { showDeleteTypeDialog = incomeType },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف الإيراد",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("حذف الإيراد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS ---

    // 1. Add Daily Revenue Dialog
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
                val todayDate = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date()) }
                var dateStr by remember { mutableStateOf(todayDate) }
                var bagsStr by remember { mutableStateOf("") }
                var amountStr by remember { mutableStateOf("") }
                var notesStr by remember { mutableStateOf("") }

                var showDateError by remember { mutableStateOf(false) }
                var showAmountError by remember { mutableStateOf(false) }

                var showDatePicker by remember { mutableStateOf(false) }
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let {
                                    dateStr = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(it))
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
                        modifier = Modifier
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "تسجيل إيراد يوم جديد",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // 1. Date Field (Defaults to Today)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = dateStr,
                                onValueChange = {},
                                label = { Text("التاريخ (اسم الإيراد)") },
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

                        // 2. Consumed Bags
                        OutlinedTextField(
                            value = bagsStr,
                            onValueChange = {
                                if (it.all { char -> char.isDigit() }) {
                                    bagsStr = it
                                }
                            },
                            label = { Text("عدد الشوالات المستهلكة") },
                            placeholder = { Text("أدخل الرقم (مثال: 5)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = NumberCommaTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Text("🌾", modifier = Modifier.padding(start = 8.dp)) }
                        )

                        // 3. Revenue Amount
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = {
                                if (it.all { char -> char.isDigit() || char == '.' }) {
                                    amountStr = it
                                    showAmountError = false
                                }
                            },
                            label = { Text("مبلغ إيرادات اليوم (بالجنيه السوداني)") },
                            placeholder = { Text("مثال: 150000") },
                            singleLine = true,
                            isError = showAmountError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = NumberCommaTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Text("💰", modifier = Modifier.padding(start = 8.dp)) }
                        )
                        if (showAmountError) {
                            Text(
                                text = "الرجاء إدخال مبلغ صحيح أكبر من الصفر",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // 4. Daily Notes
                        OutlinedTextField(
                            value = notesStr,
                            onValueChange = { notesStr = it },
                            label = { Text("ملاحظة إيرادات اليوم") },
                            placeholder = { Text("أي تفاصيل إضافية عن إنتاج أو بيع اليوم...") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

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
                                    val amountValue = amountStr.toDoubleOrNull()
                                    val bagsValue = bagsStr.toIntOrNull() ?: 0
                                    val isDateValid = dateStr.isNotBlank()
                                    val isAmountValid = amountValue != null && amountValue >= 0.0

                                    if (!isDateValid) showDateError = true
                                    if (!isAmountValid) showAmountError = true

                                    if (isDateValid && isAmountValid) {
                                        viewModel.addIncomeType(
                                            name = dateStr.trim(),
                                            consumedBags = bagsValue,
                                            amount = amountValue!!,
                                            notes = notesStr.trim()
                                        )
                                        showAddTypeDialog = false
                                    }
                                },
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("تسجيل وحفظ", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // 2. Edit Daily Revenue Dialog
    if (showEditTypeDialog != null) {
        val incomeType = showEditTypeDialog!!
        Dialog(onDismissRequest = { showEditTypeDialog = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                var dateStr by remember { mutableStateOf(incomeType.name) }
                var bagsStr by remember { mutableStateOf(incomeType.consumedBags.toString()) }
                var amountStr by remember { mutableStateOf(incomeType.amount.toString()) }
                var notesStr by remember { mutableStateOf(incomeType.notes) }

                var showDateError by remember { mutableStateOf(false) }
                var showAmountError by remember { mutableStateOf(false) }

                var showDatePicker by remember { mutableStateOf(false) }
                val initialTime = remember(incomeType) {
                    try {
                        SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).parse(incomeType.name)?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }
                }
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialTime)

                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let {
                                    dateStr = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(it))
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
                        modifier = Modifier
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "تعديل إيراد اليوم",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // 1. Date Field
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = dateStr,
                                onValueChange = {},
                                label = { Text("التاريخ (اسم الإيراد)") },
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

                        // 2. Consumed Bags
                        OutlinedTextField(
                            value = bagsStr,
                            onValueChange = {
                                if (it.all { char -> char.isDigit() }) {
                                    bagsStr = it
                                }
                            },
                            label = { Text("عدد الشوالات المستهلكة") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = NumberCommaTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Text("🌾", modifier = Modifier.padding(start = 8.dp)) }
                        )

                        // 3. Revenue Amount
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = {
                                if (it.all { char -> char.isDigit() || char == '.' }) {
                                    amountStr = it
                                    showAmountError = false
                                }
                            },
                            label = { Text("مبلغ إيرادات اليوم (بالجنيه السوداني)") },
                            singleLine = true,
                            isError = showAmountError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = NumberCommaTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Text("💰", modifier = Modifier.padding(start = 8.dp)) }
                        )
                        if (showAmountError) {
                            Text(
                                text = "الرجاء إدخال مبلغ صحيح أكبر من الصفر",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // 4. Daily Notes
                        OutlinedTextField(
                            value = notesStr,
                            onValueChange = { notesStr = it },
                            label = { Text("ملاحظة إيرادات اليوم") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

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
                                    val amountValue = amountStr.toDoubleOrNull()
                                    val bagsValue = bagsStr.toIntOrNull() ?: 0
                                    val isDateValid = dateStr.isNotBlank()
                                    val isAmountValid = amountValue != null && amountValue >= 0.0

                                    if (!isDateValid) showDateError = true
                                    if (!isAmountValid) showAmountError = true

                                    if (isDateValid && isAmountValid) {
                                        pendingNewName = dateStr.trim()
                                        pendingConsumedBags = bagsValue
                                        pendingNewAmount = amountValue!!
                                        pendingNewNotes = notesStr.trim()
                                        showConfirmEditTypeDialog = incomeType
                                    }
                                },
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("حفظ التعديلات", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // 3. Delete Daily Revenue Dialog
    if (showDeleteTypeDialog != null) {
        val incomeType = showDeleteTypeDialog!!
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
                            text = "حذف الإيراد اليومي",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.error
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "هل أنت متأكد من حذف إيراد تاريخ '${incomeType.name}'؟\n\nتنبيه: سيؤدي هذا الإجراء إلى حذف المعاملة المالية المرتبطة به تلقائياً من قائمة المعاملات وتحديث رصيد الحساب.",
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
                                    viewModel.deleteIncomeType(incomeType)
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

    if (showConfirmEditTypeDialog != null) {
        val typeToEdit = showConfirmEditTypeDialog!!
        AlertDialog(
            onDismissRequest = { showConfirmEditTypeDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تأكيد تعديل الإيراد", color = MaterialTheme.colorScheme.primary)
                }
            },
            text = { Text("هل أنت متأكد من حفظ التعديلات على هذا الإيراد اليومي؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateIncomeType(
                            incomeType = typeToEdit,
                            newName = pendingNewName,
                            newConsumedBags = pendingConsumedBags,
                            newAmount = pendingNewAmount,
                            newNotes = pendingNewNotes
                        )
                        showConfirmEditTypeDialog = null
                        showEditTypeDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("تأكيد الحفظ")
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
