package com.sduduzog.slimlauncher.icons

import android.util.Base64
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.coroutines.coroutineContext

class GeminiIconClient(
    private val httpClient: OkHttpClient,
    private val apiKeyStore: ApiKeyStore,
    private val model: String = GeminiApi.IMAGE_MODEL
) {

    suspend fun generateIcon(
        appName: String,
        stylePrompt: String,
        referencePng: ByteArray?
    ): ByteArray {
        val apiKey = apiKeyStore.get()?.takeIf { it.isNotBlank() }
            ?: throw GeminiException("Add a Google AI Studio API key first", isFatal = true)
        val body = GeminiApi.buildRequestBody(
            appName = appName,
            stylePrompt = stylePrompt,
            referencePngBase64 = referencePng?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
        )
        var attempt = 0
        var backoffMs = INITIAL_BACKOFF_MS
        while (true) {
            coroutineContext.ensureActive()
            val request = Request.Builder()
                .url(GeminiApi.generateContentUrl(model))
                .addHeader("x-goog-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody(JSON))
                .build()
            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                when {
                    response.isSuccessful -> return GeminiApi.parseImageBytes(responseBody)
                    response.code == 429 && attempt < MAX_RATE_LIMIT_RETRIES -> {
                        val retryAfterSeconds = response.header("Retry-After")?.toLongOrNull()
                        val waitMs = retryAfterSeconds?.times(1000L) ?: backoffMs
                        delay(waitMs)
                        backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
                        attempt++
                    }
                    response.code in 500..599 && attempt < MAX_SERVER_RETRIES -> {
                        delay(backoffMs)
                        backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
                        attempt++
                    }
                    else -> {
                        val message = GeminiApi.parseErrorMessage(responseBody).ifBlank {
                            "Gemini request failed (${response.code})"
                        }
                        throw GeminiException(
                            message,
                            isFatal = GeminiApi.isFatalStatus(response.code)
                        )
                    }
                }
            }
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private const val INITIAL_BACKOFF_MS = 1_000L
        private const val MAX_BACKOFF_MS = 30_000L
        private const val MAX_RATE_LIMIT_RETRIES = 5
        private const val MAX_SERVER_RETRIES = 3
    }
}
