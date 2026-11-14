package com.sduduzog.slimlauncher.diag

/**
 * Pure diagnostics helpers. Integer exit reasons match
 * [android.app.ApplicationExitInfo] so this class loads on API 23.
 */
object DebugLogLogic {
    const val STALL_THRESHOLD_MS = 500L
    const val STALL_BACKGROUND_GAP_MS = 10_000L
    const val FILE_NAME = "forge-debug.log"
    const val MAX_BYTES = 128 * 1024
    const val EXIT_LIMIT = 5

    const val REASON_UNKNOWN = 0
    const val REASON_EXIT_SELF = 1
    const val REASON_SIGNALED = 2
    const val REASON_LOW_MEMORY = 3
    const val REASON_CRASH = 4
    const val REASON_CRASH_NATIVE = 5
    const val REASON_ANR = 6
    const val REASON_INITIALIZATION_FAILURE = 7
    const val REASON_PERMISSION_CHANGE = 8
    const val REASON_EXCESSIVE_RESOURCE_USAGE = 9
    const val REASON_USER_REQUESTED = 10
    const val REASON_USER_STOPPED = 11
    const val REASON_DEPENDENCY_DIED = 12
    const val REASON_OTHER = 13

    fun shouldLogStall(gapMs: Long, foreground: Boolean): Boolean {
        return foreground &&
            gapMs > STALL_THRESHOLD_MS &&
            gapMs < STALL_BACKGROUND_GAP_MS
    }

    fun formatStall(gapMs: Long, stack: String, nowMs: Long): String {
        return "STALL gap=${gapMs}ms at=$nowMs\n$stack"
    }

    fun reasonName(reason: Int): String {
        return when (reason) {
            REASON_ANR -> "ANR"
            REASON_CRASH -> "CRASH"
            REASON_CRASH_NATIVE -> "CRASH_NATIVE"
            REASON_USER_REQUESTED -> "USER_REQUESTED"
            REASON_USER_STOPPED -> "USER_STOPPED"
            REASON_EXCESSIVE_RESOURCE_USAGE -> "EXCESSIVE_RESOURCE"
            REASON_LOW_MEMORY -> "LOW_MEMORY"
            REASON_SIGNALED -> "SIGNALED"
            REASON_EXIT_SELF -> "EXIT_SELF"
            REASON_OTHER -> "OTHER"
            else -> "REASON_$reason"
        }
    }

    fun shouldIncludeAnrTrace(reason: Int): Boolean = reason == REASON_ANR

    fun formatExit(
        timestamp: Long,
        reason: Int,
        description: String?,
        trace: String?
    ): String {
        val builder = StringBuilder()
        builder.append("EXIT at=").append(timestamp)
            .append(" reason=").append(reasonName(reason))
        if (!description.isNullOrBlank()) {
            builder.append(" desc=").append(description)
        }
        if (!trace.isNullOrBlank() && shouldIncludeAnrTrace(reason)) {
            builder.append('\n').append(trace.trim())
        }
        return builder.toString()
    }

    fun header(versionName: String, versionCode: Int, sdk: Int, device: String): String {
        return "Forge $versionName ($versionCode) sdk=$sdk $device"
    }

    fun trimToMax(text: String, maxBytes: Int = MAX_BYTES): String {
        val bytes = text.toByteArray(Charsets.UTF_8)
        if (bytes.size <= maxBytes) return text
        val keep = bytes.copyOfRange(bytes.size - maxBytes, bytes.size)
        return String(keep, Charsets.UTF_8)
    }

    fun exitLimit(sdkInt: Int): Int = if (sdkInt >= 30) EXIT_LIMIT else 0
}
