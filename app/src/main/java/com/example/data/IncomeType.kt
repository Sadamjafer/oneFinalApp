package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "income_types")
data class IncomeType(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long, // ربط نوع الإيراد بحساب/محل محدد
    val name: String,    // تاريخ الإيراد (مثلاً: 2026/06/24)
    val consumedBags: Int = 0, // عدد الشوالات المستهلكة
    val amount: Double = 0.0,   // مبلغ الإيرادات
    val notes: String = "",     // ملاحظة إيرادات اليوم
    val transactionId: Long? = null, // رقم المعاملة المرتبطة في جدول المعاملات
    val timestamp: Long = System.currentTimeMillis()
)
