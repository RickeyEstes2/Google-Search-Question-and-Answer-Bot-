package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService(
    private var customApiKey: String? = null
) {
    companion object {
        private const val TAG = "GeminiService"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun updateApiKey(key: String) {
        customApiKey = key.trim()
    }

    fun getEffectiveApiKey(): String {
        val custom = customApiKey
        if (!custom.isNullOrBlank()) return custom
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
    }

    suspend fun generateChatResponse(
        conversation: List<ChatMessage>,
        mode: BotMode,
        supplementaryContext: String? = null
    ): BotResponse = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        val model = mode.modelName

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext BotResponse(
                text = "⚠️ Gemini API key is missing or set to placeholder. Please configure your API key in the AI Studio Secrets panel or enter an active key in the settings bar above.\n\n" +
                        "When configured, this bot will connect live to Google Search with '${mode.displayName}' using model '${mode.modelName}'.",
                modelUsed = model,
                isSuccess = false,
                errorMessage = "API key not configured"
            )
        }

        val requestJson = JSONObject()

        // 1. Build conversation contents
        val contentsArray = JSONArray()
        // Take up to the last 12 messages to keep context focused
        val recentMessages = conversation.takeLast(12)

        for (i in recentMessages.indices) {
            val msg = recentMessages[i]
            val contentObj = JSONObject()
            contentObj.put("role", if (msg.role == "user") "user" else "model")

            var msgText = msg.content
            // If this is the last user message and we have scraped web context, append it
            if (i == recentMessages.lastIndex && msg.role == "user" && !supplementaryContext.isNullOrBlank()) {
                msgText += "\n\n--- ADDITIONAL SCRAPED WEB CONTEXT ---\n$supplementaryContext"
            }

            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", msgText)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)

            contentsArray.put(contentObj)
        }
        requestJson.put("contents", contentsArray)

        // 2. System instruction based on mode and bot role
        val sysInstructionObj = JSONObject()
        val sysParts = JSONArray()
        val sysPart = JSONObject()
        val systemPrompt = when (mode) {
            BotMode.SEARCH_GROUNDED -> (
                "You are Google Search Q&A Bot, an expert research and fact-finding assistant. " +
                "You have access to real-time Google Search data. Always ground your responses in up-to-date facts, " +
                "clearly explain information, cite sources accurately, and structure responses with clear formatting and markdown."
            )
            BotMode.HIGH_THINKING -> (
                "You are Google Search Q&A Bot operating in High-Thinking Reasoning Mode. " +
                "Provide comprehensive, structured, step-by-step reasoning for complex questions, mathematics, coding, " +
                "architecture, and deep scientific inquiries. Think deeply and present thorough, analytical answers."
            )
            BotMode.FAST_LITE -> (
                "You are Google Search Q&A Bot operating in Fast Mode. " +
                "Deliver rapid, concise, and direct answers to the user's questions without unnecessary filler."
            )
        }
        sysPart.put("text", systemPrompt)
        sysParts.put(sysPart)
        sysInstructionObj.put("parts", sysParts)
        requestJson.put("systemInstruction", sysInstructionObj)

        // 3. Configure tools for Search Grounding
        if (mode == BotMode.SEARCH_GROUNDED) {
            val toolsArray = JSONArray()
            val searchTool = JSONObject()
            searchTool.put("google_search", JSONObject())
            toolsArray.put(searchTool)
            requestJson.put("tools", toolsArray)
        }

        // 4. Configure thinkingConfig for High Thinking
        if (mode == BotMode.HIGH_THINKING) {
            val genConfig = JSONObject()
            val thinkingConfig = JSONObject()
            // High thinking configuration per guidelines: gemini-3.1-pro-preview with thinkingLevel HIGH
            thinkingConfig.put("thinkingLevel", "HIGH")
            genConfig.put("thinkingConfig", thinkingConfig)
            // Note: Do not set maxOutputTokens per specification
            requestJson.put("generationConfig", genConfig)
        }

        val url = "$BASE_URL/$model:generateContent?key=$apiKey"
        val body = requestJson.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API error ($response): $responseBody")
                val errMsg = try {
                    val errJson = JSONObject(responseBody).getJSONObject("error")
                    errJson.optString("message", "HTTP ${response.code}")
                } catch (e: Exception) {
                    "HTTP ${response.code}: $responseBody"
                }
                return@withContext BotResponse(
                    text = "Request failed: $errMsg",
                    modelUsed = model,
                    isSuccess = false,
                    errorMessage = errMsg
                )
            }

            val resObj = JSONObject(responseBody)
            val candidates = resObj.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext BotResponse(
                    text = "No response generated by the model.",
                    modelUsed = model,
                    isSuccess = false,
                    errorMessage = "Empty candidates"
                )
            }

            val cand = candidates.getJSONObject(0)
            val contentObj = cand.optJSONObject("content")
            val parts = contentObj?.optJSONArray("parts")
            val textBuilder = StringBuilder()
            if (parts != null) {
                for (j in 0 until parts.length()) {
                    val p = parts.getJSONObject(j)
                    if (p.has("text")) {
                        textBuilder.append(p.getString("text"))
                    }
                }
            }

            // Extract Google Search Grounding Metadata
            val sources = mutableListOf<SearchCitation>()
            val webQueries = mutableListOf<String>()

            val groundingMeta = cand.optJSONObject("groundingMetadata")
            if (groundingMeta != null) {
                // Extract search queries
                val queriesArray = groundingMeta.optJSONArray("webSearchQueries")
                if (queriesArray != null) {
                    for (q in 0 until queriesArray.length()) {
                        val queryStr = queriesArray.optString(q)
                        if (queryStr.isNotBlank()) webQueries.add(queryStr)
                    }
                }

                // Extract grounding chunks / source URLs
                val chunksArray = groundingMeta.optJSONArray("groundingChunks")
                if (chunksArray != null) {
                    for (c in 0 until chunksArray.length()) {
                        val chunkObj = chunksArray.getJSONObject(c)
                        val webObj = chunkObj.optJSONObject("web")
                        if (webObj != null) {
                            val uri = webObj.optString("uri")
                            val title = webObj.optString("title", uri)
                            if (uri.isNotBlank()) {
                                sources.add(SearchCitation(title = title, url = uri))
                            }
                        }
                    }
                }
            }

            BotResponse(
                text = textBuilder.toString(),
                modelUsed = model,
                sources = sources.distinctBy { it.url },
                webQueries = webQueries,
                isSuccess = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Network call failed", e)
            BotResponse(
                text = "Network error: ${e.localizedMessage ?: e.message}",
                modelUsed = model,
                isSuccess = false,
                errorMessage = e.message
            )
        }
    }
}
