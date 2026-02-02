package app.grip_gains_companion.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Application-wide logger that writes to both Logcat and an in-memory buffer
 * Users can view and share logs from within the app
 */
object AppLogger {
    
    private const val MAX_LOG_ENTRIES = 500
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    
    data class LogEntry(
        val timestamp: String,
        val level: Level,
        val tag: String,
        val message: String
    ) {
        override fun toString(): String {
            return "$timestamp ${level.prefix} $tag: $message"
        }
    }
    
    enum class Level(val prefix: String) {
        VERBOSE("V"),
        DEBUG("D"),
        INFO("I"),
        WARNING("W"),
        ERROR("E")
    }
    
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()
    
    private var isEnabled = true
    
    fun enable() {
        isEnabled = true
    }
    
    fun disable() {
        isEnabled = false
    }
    
    fun v(tag: String, message: String) {
        log(Level.VERBOSE, tag, message)
        Log.v(tag, message)
    }
    
    fun d(tag: String, message: String) {
        log(Level.DEBUG, tag, message)
        Log.d(tag, message)
    }
    
    fun i(tag: String, message: String) {
        log(Level.INFO, tag, message)
        Log.i(tag, message)
    }
    
    fun w(tag: String, message: String) {
        log(Level.WARNING, tag, message)
        Log.w(tag, message)
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val fullMessage = if (throwable != null) {
            "$message\n${throwable.stackTraceToString()}"
        } else {
            message
        }
        log(Level.ERROR, tag, fullMessage)
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
    
    private fun log(level: Level, tag: String, message: String) {
        if (!isEnabled) return
        
        val timestamp = dateFormat.format(Date())
        val entry = LogEntry(timestamp, level, tag, message)
        
        _logs.value = (_logs.value + entry).takeLast(MAX_LOG_ENTRIES)
    }
    
    fun clear() {
        _logs.value = emptyList()
    }
    
    fun getAllLogsAsString(): String {
        return _logs.value.joinToString("\n") { it.toString() }
    }
    
    fun saveToFile(context: Context): File {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val filename = "grip_gains_logs_$timestamp.txt"
        val file = File(context.cacheDir, filename)
        
        file.writeText(getAllLogsAsString())
        return file
    }
}
