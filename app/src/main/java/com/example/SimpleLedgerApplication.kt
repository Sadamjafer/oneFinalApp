package com.example

import android.app.Application
import android.content.Intent
import com.example.data.AppDatabase
import com.example.data.TransactionRepository
import com.example.util.ErrorLogger
import java.io.PrintWriter
import java.io.StringWriter

class SimpleLedgerApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { TransactionRepository(database.transactionDao()) }

    override fun onCreate() {
        super.onCreate()
        
        // Setup Uncaught Exception Handler to capture all future unhandled exceptions
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // 1. Log the error to local file
            ErrorLogger.logError(applicationContext, throwable)
            
            // 2. Prepare restart Intent to MainActivity with crash details
            try {
                val intent = Intent(applicationContext, MainActivity::class.java).apply {
                    putExtra("crash_occurred", true)
                    putExtra("error_message", throwable.localizedMessage ?: throwable.toString())
                    val sw = StringWriter()
                    throwable.printStackTrace(PrintWriter(sw))
                    putExtra("stack_trace", sw.toString())
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                applicationContext.startActivity(intent)
            } catch (e: Exception) {
                // Fallback to default handler if relaunching fails
                defaultHandler?.uncaughtException(thread, throwable)
            }
            
            // 3. Terminate current crashed process safely
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(10)
        }
    }
}

