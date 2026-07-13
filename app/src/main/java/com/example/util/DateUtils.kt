package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {
    private fun getLocalFormat() = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH)
    private fun getUtcFormat() = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun localTimeToUtcTime(localTime: Long): Long {
        val dateStr = getLocalFormat().format(Date(localTime))
        return try {
            getUtcFormat().parse(dateStr)?.time ?: localTime
        } catch (e: Exception) {
            localTime
        }
    }

    fun utcTimeToLocalTime(utcTime: Long): Long {
        val dateStr = getUtcFormat().format(Date(utcTime))
        return try {
            getLocalFormat().parse(dateStr)?.time ?: utcTime
        } catch (e: Exception) {
            utcTime
        }
    }

    fun formatLocal(time: Long): String {
        return getLocalFormat().format(Date(time))
    }

    fun parseLocal(dateStr: String): Long {
        return try {
            getLocalFormat().parse(dateStr.trim())?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
