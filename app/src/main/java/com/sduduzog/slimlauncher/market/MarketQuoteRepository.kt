package com.sduduzog.slimlauncher.market

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.sduduzog.slimlauncher.wallpaper.WallpaperEntryPoint
import dagger.hilt.android.EntryPointAccessors
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

class MarketQuoteRepository(
    context: Context,
    private val client: OkHttpClient,
    private val store: MarketQuoteStore = MarketQuoteStore(context)
) {
    fun interface Listener {
        fun onQuote(kind: String, quote: MarketQuote?)
    }

    private val app = context.applicationContext
    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<Listener>()
    private var liveKinds: Set<String> = emptySet()
    private val tick = object : Runnable {
        override fun run() {
            refreshDue(liveKinds, force = false)
            if (liveKinds.isNotEmpty()) main.postDelayed(this, 30_000L)
        }
    }

    fun quote(kind: String): MarketQuote? = store.load(kind)

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun setLivePolling(enabled: Boolean, kinds: Set<String>) {
        main.removeCallbacks(tick)
        liveKinds = if (enabled) kinds.filter { MarketKinds.isBuiltin(it) }.toSet() else emptySet()
        if (liveKinds.isNotEmpty()) {
            refreshDue(liveKinds, force = false)
            main.postDelayed(tick, 30_000L)
        }
    }

    fun refreshDue(kinds: Set<String>, force: Boolean) {
        io.execute { refreshDueSync(kinds, force) }
    }

    fun refreshDueSync(kinds: Set<String>, force: Boolean) {
        val now = System.currentTimeMillis()
        val open = MarketQuoteLogic.isNseOpen(now)
        for (kind in kinds) {
            if (!MarketKinds.isBuiltin(kind)) continue
            val last = store.lastSuccessMs(kind)
            if (!MarketQuoteLogic.shouldFetch(kind, last, now, open, force)) {
                notify(kind, store.load(kind))
                continue
            }
            refreshKind(kind, now)
        }
    }

    fun refresh(kind: String, force: Boolean = true) {
        refreshDue(setOf(kind), force)
    }

    private fun refreshKind(kind: String, now: Long) {
        val previous = store.load(kind)
        val next = runCatching {
            val body = fetch(MarketQuoteLogic.url(kind))
            MarketQuoteLogic.parse(kind, body, now)
        }.getOrNull()
        if (next != null) store.save(next)
        notify(kind, next ?: previous)
    }

    private fun fetch(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", MarketSources.USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            return response.body?.string() ?: error("empty")
        }
    }

    private fun notify(kind: String, quote: MarketQuote?) {
        main.post { listeners.forEach { it.onQuote(kind, quote) } }
    }

    companion object {
        @Volatile
        private var instance: MarketQuoteRepository? = null

        fun get(context: Context): MarketQuoteRepository {
            instance?.let { return it }
            synchronized(this) {
                instance?.let { return it }
                val app = context.applicationContext
                val client = EntryPointAccessors.fromApplication(
                    app,
                    WallpaperEntryPoint::class.java
                ).okHttp()
                return MarketQuoteRepository(app, client).also { instance = it }
            }
        }
    }
}
