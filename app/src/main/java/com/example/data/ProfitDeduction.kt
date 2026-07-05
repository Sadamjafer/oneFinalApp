package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profit_deductions")
data class ProfitDeduction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long = 1,
    val amount: Double,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)
