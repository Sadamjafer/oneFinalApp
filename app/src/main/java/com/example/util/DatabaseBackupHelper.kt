package com.example.util

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.example.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.io.File

object DatabaseBackupHelper {
    private const val DB_NAME = "simple_ledger_database"

    suspend fun backupDatabase(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            // Checkpoint database to ensure all data is in the main file
            val db = AppDatabase.getDatabase(context)
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL);")
            
            val dbFile = context.getDatabasePath(DB_NAME)
            val walFile = File(dbFile.path + "-wal")
            val shmFile = File(dbFile.path + "-shm")

            context.contentResolver.openOutputStream(uri)?.use { os ->
                ZipOutputStream(os).use { zos ->
                    val filesToBackup = listOf(dbFile, walFile, shmFile)
                    for (file in filesToBackup) {
                        if (file.exists()) {
                            zos.putNextEntry(ZipEntry(file.name))
                            FileInputStream(file).use { fis ->
                                fis.copyTo(zos)
                            }
                            zos.closeEntry()
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreDatabase(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            // Close the database before restoring
            AppDatabase.getDatabase(context).close()

            val dbPath = context.getDatabasePath(DB_NAME).parentFile
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val outFile = File(dbPath, entry.name)
                        FileOutputStream(outFile).use { fos ->
                            zis.copyTo(fos)
                        }
                        entry = zis.nextEntry
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
