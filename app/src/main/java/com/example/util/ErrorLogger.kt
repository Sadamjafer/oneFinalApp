package com.example.util

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ErrorLogger {
    private const val LOG_FILE_NAME = "crash_error_logs.txt"
    private const val TAG = "ErrorLogger"

    fun logError(context: Context, exception: Throwable) {
        try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            val sw = StringWriter()
            exception.printStackTrace(PrintWriter(sw))
            val stackTrace = sw.toString()

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val dateStr = sdf.format(Date())

            val deviceDetails = "Model: ${Build.MODEL}, SDK: ${Build.VERSION.SDK_INT}, Brand: ${Build.BRAND}"

            val logEntry = """
                =========================================
                TIME: $dateStr
                DEVICE: $deviceDetails
                ERROR: ${exception.localizedMessage ?: exception.message}
                EXCEPTION TYPE: ${exception.javaClass.name}
                STACK TRACE:
                $stackTrace
                =========================================
                
            """.trimIndent()

            FileWriter(file, true).use { writer ->
                writer.append(logEntry)
            }
            Log.e(TAG, "Successfully logged uncaught exception")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write error log", e)
        }
    }

    fun getLogs(context: Context): String {
        return try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists()) {
                file.readText()
            } else {
                "لا توجد أخطاء مسجلة حالياً."
            }
        } catch (e: Exception) {
            "فشل في قراءة سجل الأخطاء: ${e.message}"
        }
    }

    fun clearLogs(context: Context): Boolean {
        return try {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}
