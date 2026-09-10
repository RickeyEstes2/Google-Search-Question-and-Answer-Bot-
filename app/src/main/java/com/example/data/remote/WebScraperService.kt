package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class WebScraperService {
    companion object {
        private const val TAG = "WebScraperService"
        private val USER_AGENT = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun scrapeUrl(rawUrl: String, maxChars: Int = 5000): ScrapedWebContent = withContext(Dispatchers.IO) {
        var url = rawUrl.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext ScrapedWebContent(
                    url = url,
                    title = "Failed to load",
                    text = "",
                    wordCount = 0,
                    isSuccess = false,
                    errorMessage = "HTTP error code: ${response.code}"
                )
            }

            val html = response.body?.string().orEmpty()
            val parsed = parseHtmlContent(html, url, maxChars)
            parsed
        } catch (e: Exception) {
            Log.e(TAG, "Error scraping $url", e)
            ScrapedWebContent(
                url = url,
                title = "Error connecting",
                text = "",
                wordCount = 0,
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "Unknown error"
            )
        }
    }

    private fun parseHtmlContent(html: String, url: String, maxChars: Int): ScrapedWebContent {
        // Extract <title>
        val titleMatcher = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(html)
        val title = if (titleMatcher.find()) {
            cleanHtmlEntities(titleMatcher.group(1).orEmpty().trim())
        } else {
            url
        }

        // Strip scripts, styles, comments, navigation, footer
        var clean = html
        clean = Pattern.compile("<script.*?</script>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(clean).replaceAll(" ")
        clean = Pattern.compile("<style.*?</style>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(clean).replaceAll(" ")
        clean = Pattern.compile("<noscript.*?</noscript>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(clean).replaceAll(" ")
        clean = Pattern.compile("<header.*?</header>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(clean).replaceAll(" ")
        clean = Pattern.compile("<nav.*?</nav>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(clean).replaceAll(" ")
        clean = Pattern.compile("<footer.*?</footer>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(clean).replaceAll(" ")
        clean = Pattern.compile("<!--.*?-->", Pattern.DOTALL).matcher(clean).replaceAll(" ")

        // Extract headings for structure
        val headings = mutableListOf<String>()
        val headingMatcher = Pattern.compile("<h[1-3][^>]*>(.*?)</h[1-3]>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(clean)
        while (headingMatcher.find() && headings.size < 8) {
            val hText = stripAllTags(headingMatcher.group(1).orEmpty()).trim()
            if (hText.length in 5..100) {
                headings.add(hText)
            }
        }

        // Extract paragraphs and text blocks
        val paragraphs = mutableListOf<String>()
        val pMatcher = Pattern.compile("<p[^>]*>(.*?)</p>", Pattern.CASE_INSENSITIVE or Pattern.DOTALL).matcher(clean)
        while (pMatcher.find()) {
            val pText = stripAllTags(pMatcher.group(1).orEmpty()).trim()
            if (pText.length > 25) {
                paragraphs.add(pText)
            }
        }

        var fullText = if (paragraphs.isNotEmpty()) {
            paragraphs.joinToString("\n\n")
        } else {
            // Fallback: strip all tags from cleaned html
            stripAllTags(clean).split("\n")
                .map { it.trim() }
                .filter { it.length > 20 }
                .joinToString("\n\n")
        }

        // Normalize whitespace
        fullText = fullText.replace(Regex("\\s+"), " ")
            .replace(Regex("(\\n\\s*){3,}"), "\n\n")
            .trim()

        if (fullText.length > maxChars) {
            fullText = fullText.substring(0, maxChars) + "... [Content Truncated]"
        }

        val wordCount = if (fullText.isBlank()) 0 else fullText.split("\\s+".toRegex()).size

        return ScrapedWebContent(
            url = url,
            title = title,
            text = fullText,
            wordCount = wordCount,
            headings = headings,
            isSuccess = fullText.isNotBlank(),
            errorMessage = if (fullText.isBlank()) "No readable text extracted from page" else null
        )
    }

    private fun stripAllTags(html: String): String {
        val noTags = Pattern.compile("<[^>]+>").matcher(html).replaceAll(" ")
        return cleanHtmlEntities(noTags)
    }

    private fun cleanHtmlEntities(text: String): String {
        return text.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .trim()
    }
}
