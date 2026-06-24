package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expense_types")
data class ExpenseType(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long, // ربط نوع المصروف بحساب/محل محدد
    val name: String,    // اسم نوع المصروف (مثال: إيجار، رواتب، كهرباء، بضاعة، إلخ)
    val timestamp: Long = System.currentTimeMillis()
)
