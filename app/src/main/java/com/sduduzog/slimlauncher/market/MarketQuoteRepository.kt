package com.sduduzog.slimlauncher.market

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.sduduzog.slimlauncher.wallpaper.WallpaperEntryPoint
import dagger.hilt.android.EntryPointAccessors
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

class MarketQuoteRepository(
    context: Context,
    private val client: OkHttpClient,
    private val store: MarketQuoteStore = MarketQuoteStore(context)
) {
    fun interface Listener {
        fun onQuote(kind: String, ui: MarketQuoteUi)
    }

    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<Listener>()
    private val inFlight = Collections.synchronizedSet(mutableSetOf<String>())
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
                notify(kind, store.load(kind), refreshing = false, failed = false)
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
        if (!inFlight.add(kind)) {
            notify(kind, previous, refreshing = true, failed = false)
            return
        }
        notify(kind, previous, refreshing = true, failed = false)
        try {
            val next = if (kind == MarketKinds.GOLD) fetchGold(now) else fetchParsed(kind, now)
            if (next != null) store.save(next)
            notify(kind, next ?: previous, refreshing = false, failed = next == null)
        } finally {
            inFlight.remove(kind)
        }
    }

    private fun fetchGold(now: Long): MarketQuote? {
        val pages = listOf(MarketSources.GOLD_RETAIL_URL, MarketSources.GOLD_RETAIL_WIDGET_URL)
        for (url in pages) {
            val quote = runCatching {
                MarketQuoteLogic.parseGoodreturns22k1g(fetch(url, html = true), now)
            }.getOrNull()
            if (quote != null) return quote
        }
        val spot = runCatching { fetch(MarketSources.GOLD_SPOT_URL, html = false) }.getOrNull()
            ?: return null
        return runCatching { MarketQuoteLogic.parseGoldSpot22k1g(spot, now) }.getOrNull()
    }

    private fun fetchParsed(kind: String, now: Long): MarketQuote? {
        return runCatching {
            MarketQuoteLogic.parse(kind, fetch(MarketQuoteLogic.url(kind), html = false), now)
        }.getOrNull()
    }

    private fun fetch(url: String, html: Boolean): String {
        val request = Request.Builder()
            .url(url)
            .header("Accept", if (html) "text/html,application/xhtml+xml" else "application/json")
            .header("User-Agent", if (html) MarketSources.BROWSER_UA else MarketSources.USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            return response.body?.string() ?: error("empty")
        }
    }

    private fun notify(kind: String, quote: MarketQuote?, refreshing: Boolean, failed: Boolean) {
        val ui = MarketQuoteUi(quote, refreshing, failed)
        main.post { listeners.forEach { it.onQuote(kind, ui) } }
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
