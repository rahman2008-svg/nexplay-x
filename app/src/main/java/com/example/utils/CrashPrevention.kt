package com.example.utils

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import java.io.PrintWriter
import java.io.StringWriter

object LensoraLog {
    private const val TAG = "LensoraStudioX"
    private val logs = mutableListOf<String>()

    fun d(message: String) {
        val formatted = "[DEBUG] $message"
        Log.d(TAG, message)
        addLog(formatted)
    }

    fun e(message: String, throwable: Throwable? = null) {
        val trace = throwable?.let {
            val sw = StringWriter()
            it.printStackTrace(PrintWriter(sw))
            "\n" + sw.toString()
        } ?: ""
        val formatted = "[ERROR] $message$trace"
        Log.e(TAG, message, throwable)
        addLog(formatted)
    }

    fun i(message: String) {
        val formatted = "[INFO] $message"
        Log.i(TAG, message)
        addLog(formatted)
    }

    @Synchronized
    private fun addLog(log: String) {
        logs.add("${System.currentTimeMillis()}: $log")
        if (logs.size > 500) {
            logs.removeAt(0)
        }
    }

    @Synchronized
    fun getCrashLogs(): List<String> = logs.toList()
}

// Global Custom exception handler for Coroutines
val lensoraCoroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
    LensoraLog.e("Uncaught Coroutine exception", exception)
}

open class LensoraException(message: String, cause: Throwable? = null) : Exception(message, cause)

class ImageProcessingException(message: String, cause: Throwable? = null) : LensoraException(message, cause) {
    init {
        LensoraLog.e("ImageProcessingException: $message", cause)
    }
}

class ExportException(message: String, cause: Throwable? = null) : LensoraException(message, cause) {
    init {
        LensoraLog.e("ExportException: $message", cause)
    }
}

/**
 * Executes a block of code, safely catching image processing errors and mapping them.
 */
inline fun <T> runSafeImageProcessing(action: () -> T): T {
    try {
        return action()
    } catch (e: Exception) {
        throw ImageProcessingException("Failed to transform bitmap safely", e)
    }
}

/**
 * Executes a block of code, safely catching photo export exceptions.
 */
inline fun <T> runSafeExport(action: () -> T): T {
    try {
        return action()
    } catch (e: Exception) {
        throw ExportException("Failed to encode/export photo or design", e)
    }
}
