package com.sduduzog.slimlauncher.wallpaper

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class BingArchiveParserTest {

    private val sampleJson = """
        {
          "images": [
            {
              "startdate": "20261031",
              "url": "/th?id=OHR.Dummy_EN-IN1920x1080.jpg&rf=LaDigue_1920x1080.jpg&pid=hp",
              "urlbase": "/th?id=OHR.Dummy_EN-IN",
              "copyright": "Dummy place (\u00a9 Photographer)",
              "title": "Dummy place"
            }
          ]
        }
    """.trimIndent()

    @Test
    fun archiveUrlUsesOfficialEnInEndpoint() {
        assertThat(BingArchiveParser.archiveUrl()).isEqualTo(
            "https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=en-IN"
        )
    }

    @Test
    fun parseReadsTitleCopyrightAndUrlFields() {
        val image = BingArchiveParser.parse(sampleJson)
        assertThat(image.title).isEqualTo("Dummy place")
        assertThat(image.copyright).isEqualTo("Dummy place (© Photographer)")
        assertThat(image.startdate).isEqualTo("20261031")
        assertThat(image.url).startsWith("/th?id=OHR.Dummy_EN-IN")
        assertThat(image.urlbase).isEqualTo("/th?id=OHR.Dummy_EN-IN")
    }

    @Test
    fun imageUrlPrefersPortraitVariantFromUrlbase() {
        val image = BingArchiveParser.parse(sampleJson)
        assertThat(BingArchiveParser.imageUrl(image)).isEqualTo(
            "https://www.bing.com/th?id=OHR.Dummy_EN-IN_1080x1920.jpg"
        )
    }

    @Test
    fun absolutePrependsBingHostToRelativePaths() {
        assertThat(BingArchiveParser.absolute("/th?id=foo"))
            .isEqualTo("https://www.bing.com/th?id=foo")
        assertThat(BingArchiveParser.absolute("https://cdn.example/x.jpg"))
            .isEqualTo("https://cdn.example/x.jpg")
    }

    @Test
    fun imageUrlFallsBackToOriginalUrlWhenUrlbaseMissing() {
        val image = BingImage(
            title = "X",
            copyright = "Y",
            url = "/th?id=fallback.jpg",
            urlbase = "",
            startdate = "20261031"
        )
        assertThat(BingArchiveParser.imageUrl(image))
            .isEqualTo("https://www.bing.com/th?id=fallback.jpg")
    }

    @Test
    fun candidateUrlsIncludePortraitUhdAndOriginal() {
        val image = BingArchiveParser.parse(sampleJson)
        val urls = BingArchiveParser.candidateUrls(image)
        assertThat(urls).containsExactly(
            "https://www.bing.com/th?id=OHR.Dummy_EN-IN_1080x1920.jpg",
            "https://www.bing.com/th?id=OHR.Dummy_EN-IN_UHD.jpg",
            "https://www.bing.com/th?id=OHR.Dummy_EN-IN1920x1080.jpg&rf=LaDigue_1920x1080.jpg&pid=hp"
        ).inOrder()
    }

    @Test
    fun parseRejectsEmptyImagesArray() {
        assertThrows(IllegalArgumentException::class.java) {
            BingArchiveParser.parse("""{"images":[]}""")
        }
    }
}
