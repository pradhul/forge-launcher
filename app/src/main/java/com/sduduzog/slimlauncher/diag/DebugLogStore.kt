package com.sduduzog.slimlauncher.diag

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.sduduzog.slimlauncher.BuildConfig
import com.sduduzog.slimlauncher.R
import java.io.File
import java.util.concurrent.Executors

object DebugLogStore {
    const val TAG = "ForgeDiag"

    private val io = Executors.newSingleThreadExecutor()
    private val lock = Any()

    fun file(context: Context): File {
        return File(context.applicationContext.filesDir, DebugLogLogic.FILE_NAME)
    }

    fun append(context: Context, line: String) {
        val app = context.applicationContext
        io.execute {
            synchronized(lock) {
                runCatching {
                    val target = file(app)
                    val next = buildString {
                        if (target.exists()) append(target.readText())
                        if (isNotEmpty() && !endsWith('\n')) append('\n')
                        append(line)
                        if (!line.endsWith('\n')) append('\n')
                    }
                    target.writeText(DebugLogLogic.trimToMax(next))
                }.onFailure {
                    Log.w(TAG, "append failed: ${it.message}")
                }
            }
        }
    }

    fun dumpStartup(context: Context) {
        val app = context.applicationContext
        io.execute {
            val header = DebugLogLogic.header(
                versionName = BuildConfig.VERSION_NAME,
                versionCode = BuildConfig.VERSION_CODE,
                sdk = Build.VERSION.SDK_INT,
                device = "${Build.MANUFACTURER} ${Build.MODEL}"
            )
            appendSync(app, "START $header")
            dumpExitInfoSync(app)
        }
    }

    fun buildReport(context: Context): String {
        val app = context.applicationContext
        synchronized(lock) {
            val header = DebugLogLogic.header(
                versionName = BuildConfig.VERSION_NAME,
                versionCode = BuildConfig.VERSION_CODE,
                sdk = Build.VERSION.SDK_INT,
                device = "${Build.MANUFACTURER} ${Build.MODEL}"
            )
            val body = runCatching { file(app).takeIf { it.exists() }?.readText() }.getOrNull()
            return buildString {
                appendLine(header)
                if (!body.isNullOrBlank()) {
                    appendLine()
                    append(body.trim())
                    appendLine()
                }
            }
        }
    }

    fun copyAndShare(context: Context) {
        val text = buildReport(context)
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText("Forge debug log", text))
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.about_copy_debug_log))
        }
        val chooser = Intent.createChooser(send, context.getString(R.string.about_copy_debug_log))
        if (context !is android.app.Activity) {
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(chooser) }
        Log.i(TAG, "copied debug log chars=${text.length}")
    }

    private fun dumpExitInfoSync(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val manager = context.getSystemService(ActivityManager::class.java) ?: return
        val exits = runCatching {
            manager.getHistoricalProcessExitReasons(null, 0, DebugLogLogic.EXIT_LIMIT)
        }.getOrNull().orEmpty()
        if (exits.isEmpty()) {
            appendSync(context, "EXIT none")
            return
        }
        for (info in exits) {
            val trace = if (DebugLogLogic.shouldIncludeAnrTrace(info.reason)) {
                runCatching {
                    info.traceInputStream?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            } else {
                null
            }
            appendSync(
                context,
                DebugLogLogic.formatExit(
                    timestamp = info.timestamp,
                    reason = info.reason,
                    description = info.description,
                    trace = trace
                )
            )
        }
    }

    private fun appendSync(context: Context, line: String) {
        synchronized(lock) {
            runCatching {
                val target = file(context)
                val next = buildString {
                    if (target.exists()) append(target.readText())
                    if (isNotEmpty() && !endsWith('\n')) append('\n')
                    append(line)
                    if (!line.endsWith('\n')) append('\n')
                }
                target.writeText(DebugLogLogic.trimToMax(next))
            }
        }
    }
}
