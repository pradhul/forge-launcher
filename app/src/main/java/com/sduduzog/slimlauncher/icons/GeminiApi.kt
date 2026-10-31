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

    fun isFatalStatus(httpCode: Int): Boolean = httpCode == 400 || httpCode == 401 || httpCode == 403

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
