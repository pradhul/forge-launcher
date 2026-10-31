package com.sduduzog.slimlauncher.icons

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class GeminiApiTest {

    @Test
    fun generateContentUrlUsesV1BetaAndImageModel() {
        assertThat(GeminiApi.generateContentUrl())
            .isEqualTo(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-image:generateContent"
            )
    }

    @Test
    fun requestIncludesPromptStyleAndSquareConfig() {
        val body = GeminiApi.buildRequestBody(
            appName = "Messages",
            stylePrompt = "neon cyberpunk",
            referencePngBase64 = "AAA"
        )
        assertThat(body).contains("Messages")
        assertThat(body).contains("neon cyberpunk")
        assertThat(body).contains("inline_data")
        assertThat(body).contains("AAA")
        assertThat(body).contains("1:1")
        assertThat(body).contains("IMAGE")
    }

    @Test
    fun requestOmitsReferenceWhenMissing() {
        val body = GeminiApi.buildRequestBody("Phone", "ink", null)
        assertThat(body).doesNotContain("inline_data")
        assertThat(body).contains("Phone")
    }

    @Test
    fun extractsCamelCaseAndSnakeCaseImageData() {
        val camel = """
            {"candidates":[{"content":{"parts":[
              {"text":"ok"},
              {"inlineData":{"mimeType":"image/png","data":"QUJD"}}
            ]}}]}
        """.trimIndent()
        val snake = """
            {"candidates":[{"content":{"parts":[
              {"inline_data":{"mime_type":"image/png","data":"QUJD"}}
            ]}}]}
        """.trimIndent()
        assertThat(GeminiApi.extractImageBase64(camel)).isEqualTo("QUJD")
        assertThat(GeminiApi.extractImageBase64(snake)).isEqualTo("QUJD")
    }

    @Test
    fun missingImageThrows() {
        assertThrows(GeminiException::class.java) {
            GeminiApi.extractImageBase64("""{"candidates":[{"content":{"parts":[{"text":"nope"}]}}]}""")
        }
    }

    @Test
    fun sanitizesApiKeyMaterial() {
        val cleaned = GeminiApi.sanitizeError("bad key AIzaSyDummyKeyValue1234567890 and key=secret-token")
        assertThat(cleaned).doesNotContain("AIza")
        assertThat(cleaned).doesNotContain("secret-token")
        assertThat(cleaned).contains("***")
    }

    @Test
    fun fatalStatuses() {
        assertThat(GeminiApi.isFatalStatus(401)).isTrue()
        assertThat(GeminiApi.isFatalStatus(403)).isTrue()
        assertThat(GeminiApi.isFatalStatus(400)).isTrue()
        assertThat(GeminiApi.isFatalStatus(429)).isFalse()
        assertThat(GeminiApi.isFatalStatus(500)).isFalse()
    }
}
