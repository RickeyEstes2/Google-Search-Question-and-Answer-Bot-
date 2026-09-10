package com.example.data.remote

data class SearchCitation(
    val title: String,
    val url: String,
    val snippet: String = ""
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String, // "user" or "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String? = null,
    val sources: List<SearchCitation> = emptyList(),
    val webQueries: List<String> = emptyList(),
    val isThinking: Boolean = false,
    val error: Boolean = false
)

enum class BotMode(
    val id: String,
    val displayName: String,
    val modelName: String,
    val description: String,
    val badgeIcon: String
) {
    SEARCH_GROUNDED(
        id = "search_grounded",
        displayName = "Google Search",
        modelName = "gemini-3.5-flash",
        description = "Live web search grounding with real-time sources",
        badgeIcon = "🔍"
    ),
    HIGH_THINKING(
        id = "high_thinking",
        displayName = "High Thinking",
        modelName = "gemini-3.1-pro-preview",
        description = "Deep multi-step reasoning for complex research",
        badgeIcon = "🧠"
    ),
    FAST_LITE(
        id = "fast_lite",
        displayName = "Fast Lite",
        modelName = "gemini-3.1-flash-lite-preview",
        description = "Low-latency rapid answers",
        badgeIcon = "⚡"
    )
}

data class ScrapedWebContent(
    val url: String,
    val title: String,
    val text: String,
    val wordCount: Int,
    val headings: List<String> = emptyList(),
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)

data class BotResponse(
    val text: String,
    val modelUsed: String,
    val sources: List<SearchCitation> = emptyList(),
    val webQueries: List<String> = emptyList(),
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)
