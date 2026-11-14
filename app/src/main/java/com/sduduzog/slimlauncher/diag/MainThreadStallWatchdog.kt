package com.sduduzog.slimlauncher.diag

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Choreographer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

/**
 * Logs main-thread stalls without blocking the UI.
 *
 * A Choreographer callback records the last frame time. A background
 * sampler dumps the main thread stack when no frame arrives for
 * [DebugLogLogic.STALL_THRESHOLD_MS] while the process is foreground.
 */
object MainThreadStallWatchdog {
    private val started = AtomicBoolean(false)
    private val lastFrameMs = AtomicLong(0L)
    private val startedActivities = AtomicInteger(0)
    private val logging = AtomicBoolean(false)

    fun start(application: Application) {
        if (!started.compareAndSet(false, true)) return
        lastFrameMs.set(SystemClock.uptimeMillis())
        application.registerActivityLifecycleCallbacks(lifecycle)
        Choreographer.getInstance().postFrameCallback(frameCallback)
        thread(name = "forge-stall", isDaemon = true) {
            while (true) {
                try {
                    Thread.sleep(250L)
                    val gap = SystemClock.uptimeMillis() - lastFrameMs.get()
                    val foreground = startedActivities.get() > 0
                    if (!DebugLogLogic.shouldLogStall(gap, foreground)) {
                        logging.set(false)
                        continue
                    }
                    if (!logging.compareAndSet(false, true)) continue
                    val stack = Looper.getMainLooper().thread.stackTrace
                        .joinToString("\n") { "  at $it" }
                    val line = DebugLogLogic.formatStall(
                        gapMs = gap,
                        stack = stack,
                        nowMs = System.currentTimeMillis()
                    )
                    Log.w(DebugLogStore.TAG, line)
                    DebugLogStore.append(application, line)
                } catch (_: InterruptedException) {
                    return@thread
                } catch (_: Throwable) {
                }
            }
        }
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            lastFrameMs.set(SystemClock.uptimeMillis())
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private val lifecycle = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityStarted(activity: Activity) {
            startedActivities.incrementAndGet()
            lastFrameMs.set(SystemClock.uptimeMillis())
        }

        override fun onActivityStopped(activity: Activity) {
            startedActivities.updateAndGet { (it - 1).coerceAtLeast(0) }
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }
}
