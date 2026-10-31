package com.sduduzog.slimlauncher.diag

import android.os.Build
import android.os.StrictMode
import android.util.Log
import java.util.concurrent.Executors

/**
 * Debug StrictMode that keeps disk/network detection but only logs
 * violations whose stack includes our package. Framework layout
 * inflation disk reads are otherwise enormous on every frame.
 */
object ForgeStrictMode {
    const val TAG = "ForgeStrict"
    const val APP_PREFIX = "com.sduduzog.slimlauncher"

    fun shouldLog(stack: Array<StackTraceElement>): Boolean {
        return stack.any { it.className.startsWith(APP_PREFIX) }
    }

    fun install() {
        val builder = StrictMode.ThreadPolicy.Builder()
            .detectDiskReads()
            .detectDiskWrites()
            .detectNetwork()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val executor = Executors.newSingleThreadExecutor()
            builder.penaltyListener(executor) { violation ->
                if (shouldLog(violation.stackTrace)) {
                    Log.w(TAG, violation.toString(), violation)
                }
            }
        } else {
            builder.penaltyLog()
        }
        StrictMode.setThreadPolicy(builder.build())
    }
}
