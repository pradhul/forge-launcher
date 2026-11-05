package com.sduduzog.slimlauncher.icons

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

object GeminiApi {
    const val IMAGE_MODEL = "gemini-2.5-flash-image"
    const val GENERATE_CONTENT_PATH = "v1beta/models/%s:generateContent"
    const val HOST = "https://generativelanguage.googleapis.com"

    fun generateContentUrl(model: String = IMAGE_MODEL): String {
        return "$HOST/${GENERATE_CONTENT_PATH.format(model)}"
    }

    fun buildRequestBody(
        appName: String,
        stylePrompt: String,
        referencePngBase64: String?
    ): String {
        val prompt = buildString {
            append("Create a square Android launcher icon for the app named \"")
            append(appName)
            append("\". Style: ")
            append(stylePrompt.ifBlank { "clean, minimal, flat" })
            append(". ")
            if (referencePngBase64 != null) {
                append("Use the attached image as the original app icon for reference. ")
                append("Keep it recognizable as that app, but restyle it completely. ")
            }
            append("Single centered subject, simple background, no text, no letters, ")
            append("no watermark, no device chrome. Output a 1:1 square icon.")
        }
        val parts = JSONArray().put(JSONObject().put("text", prompt))
        if (!referencePngBase64.isNullOrBlank()) {
            parts.put(
                JSONObject().put(
                    "inline_data",
                    JSONObject()
                        .put("mime_type", "image/png")
                        .put("data", referencePngBase64)
                )
            )
        }
        val generationConfig = JSONObject()
            .put("responseModalities", JSONArray().put("TEXT").put("IMAGE"))
            .put("imageConfig", JSONObject().put("aspectRatio", "1:1"))
        return JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("parts", parts)
                )
            )
            .put("generationConfig", generationConfig)
            .toString()
    }

    fun extractImageBase64(responseJson: String): String {
        val root = JSONObject(responseJson)
        if (root.has("error")) {
            throw GeminiException(parseErrorMessage(root), isFatal = isFatalError(root))
        }
        val candidates = root.optJSONArray("candidates")
            ?: throw GeminiException("Gemini returned no image candidates")
        if (candidates.length() == 0) {
            throw GeminiException("Gemini returned no image candidates")
        }
        val parts = candidates.getJSONObject(0)
            .optJSONObject("content")
            ?.optJSONArray("parts")
            ?: throw GeminiException("Gemini returned an empty response")
        for (index in 0 until parts.length()) {
            val part = parts.getJSONObject(index)
            val inline = part.optJSONObject("inlineData") ?: part.optJSONObject("inline_data")
            val data = inline?.optString("data").orEmpty()
            if (data.isNotBlank()) {
                return data
            }
        }
        throw GeminiException("Gemini returned text but no image")
    }

    fun parseImageBytes(responseJson: String): ByteArray {
        return Base64.decode(extractImageBase64(responseJson), Base64.DEFAULT)
    }

    fun parseErrorMessage(responseJson: String): String {
        return try {
            parseErrorMessage(JSONObject(responseJson))
        } catch (_: Exception) {
            "Gemini request failed"
        }
    }

    private fun parseErrorMessage(root: JSONObject): String {
        val error = root.optJSONObject("error") ?: return "Gemini request failed"
        val message = error.optString("message").ifBlank { "Gemini request failed" }
        return sanitizeError(message)
    }

    const val FREE_TIER_IMAGE_QUOTA_MESSAGE =
        "Your key is on the free tier, which has no image quota. Enable billing in AI Studio or use On-device styles."

    fun isFatalStatus(httpCode: Int): Boolean = httpCode == 400 || httpCode == 401 || httpCode == 403

    fun isFreeTierNoImageQuota(responseJson: String, message: String = ""): Boolean {
        val haystack = "$responseJson $message".lowercase()
        val mentionsFree = haystack.contains("free_tier") ||
            haystack.contains("freetier") ||
            haystack.contains("free tier") ||
            haystack.contains("generate_content_free_tier")
        val mentionsZeroLimit = haystack.contains("limit: 0") ||
            haystack.contains("limit:0") ||
            haystack.contains("limit = 0") ||
            haystack.contains("\"limit\": 0") ||
            haystack.contains("\"limit\":0")
        return mentionsFree && (mentionsZeroLimit || haystack.contains("quota") || haystack.contains("resource_exhausted"))
    }

    fun parseRetryDelayMs(responseJson: String): Long? {
        return try {
            val root = JSONObject(responseJson)
            val details = root.optJSONObject("error")?.optJSONArray("details") ?: return null
            for (index in 0 until details.length()) {
                val item = details.optJSONObject(index) ?: continue
                val delay = item.optString("retryDelay").ifBlank { item.optString("retry_delay") }
                parseDurationMs(delay)?.let { return it }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun parseDurationMs(raw: String): Long? {
        val match = Regex("""^\s*(\d+(?:\.\d+)?)(s|ms)?\s*$""").find(raw) ?: return null
        val value = match.groupValues[1].toDoubleOrNull() ?: return null
        val unit = match.groupValues[2]
        val ms = if (unit == "ms") value else value * 1000.0
        return ms.toLong().takeIf { it > 0 }
    }

    private fun isFatalError(root: JSONObject): Boolean {
        val status = root.optJSONObject("error")?.optString("status").orEmpty()
        return status == "UNAUTHENTICATED" ||
            status == "PERMISSION_DENIED" ||
            status == "INVALID_ARGUMENT"
    }

    fun sanitizeError(message: String): String {
        return message
            .replace(Regex("key=[A-Za-z0-9_\\-]+"), "key=***")
            .replace(Regex("AIza[0-9A-Za-z_\\-]{10,}"), "***")
    }
}

class GeminiException(
    message: String,
    val isFatal: Boolean = false
) : Exception(GeminiApi.sanitizeError(message))
