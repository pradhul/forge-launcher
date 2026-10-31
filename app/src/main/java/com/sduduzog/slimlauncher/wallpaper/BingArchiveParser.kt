package com.sduduzog.slimlauncher.wallpaper

import org.json.JSONObject

data class BingImage(
    val title: String,
    val copyright: String,
    val url: String,
    val urlbase: String,
    val startdate: String
)

object BingArchiveParser {
    const val HOST = "https://www.bing.com"
    const val ARCHIVE_PATH = "/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=en-IN"
    const val PORTRAIT_SUFFIX = "_1080x1920.jpg"
    const val UHD_SUFFIX = "_UHD.jpg"

    fun archiveUrl(): String = HOST + ARCHIVE_PATH

    fun parse(json: String): BingImage {
        val images = JSONObject(json).getJSONArray("images")
        require(images.length() > 0) { "Bing archive returned no images" }
        val first = images.getJSONObject(0)
        return BingImage(
            title = first.optString("title"),
            copyright = first.optString("copyright"),
            url = first.optString("url"),
            urlbase = first.optString("urlbase"),
            startdate = first.optString("startdate")
        )
    }

    fun imageUrl(image: BingImage): String {
        val base = image.urlbase.trim()
        if (base.isNotEmpty()) {
            return absolute(base + PORTRAIT_SUFFIX)
        }
        return absolute(image.url)
    }

    fun candidateUrls(image: BingImage): List<String> {
        val urls = ArrayList<String>(3)
        val base = image.urlbase.trim()
        if (base.isNotEmpty()) {
            urls += absolute(base + PORTRAIT_SUFFIX)
            urls += absolute(base + UHD_SUFFIX)
        }
        if (image.url.isNotBlank()) {
            urls += absolute(image.url)
        }
        return urls.distinct()
    }

    fun absolute(path: String): String {
        val trimmed = path.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        return HOST + if (trimmed.startsWith("/")) trimmed else "/$trimmed"
    }
}
