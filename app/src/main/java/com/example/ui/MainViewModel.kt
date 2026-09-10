package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.QASessionEntity
import com.example.data.remote.BotMode
import com.example.data.remote.ChatMessage
import com.example.data.remote.GeminiService
import com.example.data.remote.ScrapedWebContent
import com.example.data.remote.SearchCitation
import com.example.data.remote.WebScraperService
import com.example.data.repository.QARepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = QARepository(db.qaDao())
    val geminiService = GeminiService()
    val scraperService = WebScraperService()

    // Navigation Tab
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    fun setCurrentTab(tab: Int) {
        _currentTab.value = tab
    }

    // Bot Mode
    private val _currentMode = MutableStateFlow(BotMode.SEARCH_GROUNDED)
    val currentMode: StateFlow<BotMode> = _currentMode.asStateFlow()

    fun setMode(mode: BotMode) {
        _currentMode.value = mode
    }

    // Chat State
    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                role = "model",
                content = "👋 Hello! I'm **Google Search Q&A Bot**.\n\n" +
                        "I combine live **Google Search Grounding** with Google Gemini intelligence to give you verified, up-to-date answers with real citations.\n\n" +
                        "Ask me any question, switch to **High-Thinking** for deep analysis, or use the **Web Scraper** tab to extract and synthesize live websites!",
                modelUsed = "gemini-3.5-flash",
                sources = listOf(
                    SearchCitation("Google Search Documentation", "https://google.com"),
                    SearchCitation("Gemini AI Overview", "https://deepmind.google/technologies/gemini/")
                ),
                webQueries = listOf("Google Search Q&A Bot overview")
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _inputQuery = MutableStateFlow("")
    val inputQuery: StateFlow<String> = _inputQuery.asStateFlow()

    fun setInputQuery(query: String) {
        _inputQuery.value = query
    }

    // API Key State
    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    val isApiKeyAvailable: Boolean
        get() = geminiService.getEffectiveApiKey().isNotBlank() &&
                geminiService.getEffectiveApiKey() != "MY_GEMINI_API_KEY"

    fun setCustomApiKey(key: String) {
        _customApiKey.value = key
        geminiService.updateApiKey(key)
    }

    // Send chat question
    fun sendQuestion(query: String, supplementaryContext: String? = null) {
        val trimmed = query.trim()
        if (trimmed.isBlank() || _isGenerating.value) return

        val userMessage = ChatMessage(
            role = "user",
            content = trimmed
        )
        _messages.value = _messages.value + userMessage
        _inputQuery.value = ""
        _isGenerating.value = true

        val mode = _currentMode.value

        viewModelScope.launch {
            val response = geminiService.generateChatResponse(
                conversation = _messages.value,
                mode = mode,
                supplementaryContext = supplementaryContext
            )

            val modelMessage = ChatMessage(
                role = "model",
                content = response.text,
                modelUsed = response.modelUsed,
                sources = response.sources,
                webQueries = response.webQueries,
                isThinking = mode == BotMode.HIGH_THINKING,
                error = !response.isSuccess
            )

            _messages.value = _messages.value + modelMessage
            _isGenerating.value = false

            // Automatically persist successful answers into Room
            if (response.isSuccess) {
                saveToHistory(trimmed, response.text, response.modelUsed, mode.id, response.webQueries, response.sources)
            }
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
    }

    // Scraper State
    private val _scraperUrl = MutableStateFlow("https://en.wikipedia.org/wiki/Artificial_intelligence")
    val scraperUrl: StateFlow<String> = _scraperUrl.asStateFlow()

    private val _isScraping = MutableStateFlow(false)
    val isScraping: StateFlow<Boolean> = _isScraping.asStateFlow()

    private val _scrapedContent = MutableStateFlow<ScrapedWebContent?>(null)
    val scrapedContent: StateFlow<ScrapedWebContent?> = _scrapedContent.asStateFlow()

    private val _analysisResult = MutableStateFlow<String?>(null)
    val analysisResult: StateFlow<String?> = _analysisResult.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    fun setScraperUrl(url: String) {
        _scraperUrl.value = url
    }

    fun scrapeCurrentUrl() {
        val url = _scraperUrl.value.trim()
        if (url.isBlank() || _isScraping.value) return

        _isScraping.value = true
        _analysisResult.value = null

        viewModelScope.launch {
            val result = scraperService.scrapeUrl(url)
            _scrapedContent.value = result
            _isScraping.value = false
        }
    }

    fun analyzeScrapedContent(promptType: String) {
        val content = _scrapedContent.value ?: return
        if (content.text.isBlank() || _isAnalyzing.value) return

        _isAnalyzing.value = true
        val prompt = when (promptType) {
            "summarize" -> "Summarize the core arguments and key takeaways of the following webpage into 4-5 bullet points:\n\n${content.text}"
            "fact_check" -> "Fact-check the main claims in this scraped article using Google Search knowledge:\n\n${content.text}"
            "qa" -> "Extract 3 critical questions and authoritative answers based on this scraped article:\n\n${content.text}"
            else -> "Analyze this scraped webpage:\n\n${content.text}"
        }

        viewModelScope.launch {
            val response = geminiService.generateChatResponse(
                conversation = listOf(ChatMessage(role = "user", content = prompt)),
                mode = BotMode.SEARCH_GROUNDED
            )
            _analysisResult.value = response.text
            _isAnalyzing.value = false
        }
    }

    fun sendScrapedToChat() {
        val content = _scrapedContent.value ?: return
        _currentTab.value = 0
        sendQuestion(
            query = "Analyze and explain this page from ${content.title} (${content.url})",
            supplementaryContext = content.text
        )
    }

    // Room Database History
    private val _historySearchQuery = MutableStateFlow("")
    val historySearchQuery: StateFlow<String> = _historySearchQuery.asStateFlow()

    fun setHistorySearchQuery(q: String) {
        _historySearchQuery.value = q
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val historySessions: StateFlow<List<QASessionEntity>> = _historySearchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allSessions
            } else {
                repository.searchSessions(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun saveToHistory(
        question: String,
        answer: String,
        model: String,
        mode: String,
        queries: List<String>,
        sources: List<SearchCitation>
    ) {
        viewModelScope.launch {
            val sourcesArray = JSONArray()
            for (s in sources) {
                val obj = JSONObject()
                obj.put("title", s.title)
                obj.put("url", s.url)
                sourcesArray.put(obj)
            }

            val session = QASessionEntity(
                question = question,
                answer = answer,
                modelUsed = model,
                mode = mode,
                webQueries = queries.joinToString(", "),
                sourcesJson = sourcesArray.toString(),
                timestamp = System.currentTimeMillis()
            )
            repository.saveSession(session)
        }
    }

    fun toggleFavorite(session: QASessionEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(session)
        }
    }

    fun deleteSession(session: QASessionEntity) {
        viewModelScope.launch {
            repository.deleteSession(session)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun reloadSessionIntoChat(session: QASessionEntity) {
        val restoredSources = mutableListOf<SearchCitation>()
        try {
            if (session.sourcesJson.isNotBlank()) {
                val arr = JSONArray(session.sourcesJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    restoredSources.add(SearchCitation(obj.optString("title"), obj.optString("url")))
                }
            }
        } catch (e: Exception) {
            // Ignore parse errors
        }

        _messages.value = _messages.value + listOf(
            ChatMessage(role = "user", content = session.question),
            ChatMessage(
                role = "model",
                content = session.answer,
                modelUsed = session.modelUsed,
                sources = restoredSources,
                webQueries = session.webQueries.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            )
        )
        _currentTab.value = 0
    }
}
