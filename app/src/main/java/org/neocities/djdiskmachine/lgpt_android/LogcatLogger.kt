package org.neocities.djdiskmachine.lgpt_android

import android.util.Log
import java.io.File

class LogcatLogger(private val logFile: File) {
    private var logcatProcess: Process? = null
    private var logcatThread: Thread? = null
    private val tag = "LogcatLogger"

    fun start() {
        logcatThread = Thread {
            try {
                // Start fresh
                logFile.parentFile?.mkdirs()
                logFile.writeText("=== LittleGPTracker Log Session Started ===\n")
                
                // Capture logcat with timestamps
                val process = Runtime.getRuntime().exec(arrayOf(
                    "logcat",
                    "-v", "threadtime"
                ))
                logcatProcess = process
                
                process.inputStream.bufferedReader().use { reader ->
                    reader.forEachLine { line ->
                        try {
                            logFile.appendText("$line\n")
                        } catch (e: Exception) {
                            Log.e(tag, "Failed to write to log file", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Logcat capture error", e)
            }
        }.apply {
            isDaemon = true
            start()
        }
        Log.i(tag, "Logcat capture started: ${logFile.absolutePath}")
    }

    fun stop() {
        try {
            logcatProcess?.destroy()
            logcatThread?.join(1000)  // Wait max 1 second
            Log.i(tag, "Logcat capture stopped")
        } catch (e: Exception) {
            Log.e(tag, "Error stopping logcat", e)
        }
    }
}
