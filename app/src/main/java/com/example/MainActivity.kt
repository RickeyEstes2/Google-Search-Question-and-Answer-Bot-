package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.screens.ChatQAScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.RepoInfoScreen
import com.example.ui.screens.WebScraperScreen
import com.example.ui.theme.GoogleBlue
import com.example.ui.theme.GoogleGreen
import com.example.ui.theme.GoogleRed
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val currentMode by viewModel.currentMode.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val inputQuery by viewModel.inputQuery.collectAsStateWithLifecycle()

    val scraperUrl by viewModel.scraperUrl.collectAsStateWithLifecycle()
    val isScraping by viewModel.isScraping.collectAsStateWithLifecycle()
    val scrapedContent by viewModel.scrapedContent.collectAsStateWithLifecycle()
    val analysisResult by viewModel.analysisResult.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()

    val historySessions by viewModel.historySessions.collectAsStateWithLifecycle()
    val historySearchQuery by viewModel.historySearchQuery.collectAsStateWithLifecycle()

    var showKeyDialog by remember { mutableStateOf(false) }
    var keyInputText by remember { mutableStateOf("") }

    if (showKeyDialog) {
        AlertDialog(
            onDismissRequest = { showKeyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = GoogleBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gemini API Key")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Configure your Google Gemini API key to enable live Google Search Grounding and High-Thinking reasoning.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = keyInputText,
                        onValueChange = { keyInputText = it },
                        placeholder = { Text("AIzaSy...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Keys can also be configured automatically via the Secrets panel in AI Studio.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (keyInputText.isNotBlank()) {
                            viewModel.setCustomApiKey(keyInputText)
                        }
                        showKeyDialog = false
                    }
                ) {
                    Text("Save Key")
                }
            },
            dismissButton = {
                TextButton(onClick = { showKeyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = GoogleBlue.copy(alpha = 0.12f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = GoogleBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google Search Q&A",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                actions = {
                    // API Key Status Pill Button
                    IconButton(
                        onClick = { showKeyDialog = true },
                        modifier = Modifier.testTag("api_key_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "API Key Settings",
                            tint = if (viewModel.isApiKeyAvailable) GoogleGreen else GoogleRed
                        )
                    }

                    // Clear Chat action when on Chat tab
                    if (currentTab == 0 && messages.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearChat() },
                            modifier = Modifier.testTag("clear_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear Chat",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { viewModel.setCurrentTab(0) },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Chat Q&A"
                        )
                    },
                    label = { Text("Search Bot", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GoogleBlue,
                        indicatorColor = GoogleBlue.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.testTag("nav_chat_tab")
                )

                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { viewModel.setCurrentTab(1) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Web Scraper"
                        )
                    },
                    label = { Text("Web Scraper", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GoogleBlue,
                        indicatorColor = GoogleBlue.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.testTag("nav_scraper_tab")
                )

                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { viewModel.setCurrentTab(2) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "History"
                        )
                    },
                    label = { Text("Saved Q&A", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GoogleBlue,
                        indicatorColor = GoogleBlue.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.testTag("nav_history_tab")
                )

                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { viewModel.setCurrentTab(3) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Repo & CI"
                        )
                    },
                    label = { Text("Repo & CI", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GoogleBlue,
                        indicatorColor = GoogleBlue.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.testTag("nav_repo_tab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> ChatQAScreen(
                    messages = messages,
                    isGenerating = isGenerating,
                    inputQuery = inputQuery,
                    currentMode = currentMode,
                    isApiKeyConfigured = viewModel.isApiKeyAvailable,
                    onInputQueryChanged = { viewModel.setInputQuery(it) },
                    onSendQuery = { viewModel.sendQuestion(it) },
                    onModeChanged = { viewModel.setMode(it) },
                    onConfigureApiKeyClick = { showKeyDialog = true }
                )
                1 -> WebScraperScreen(
                    url = scraperUrl,
                    isScraping = isScraping,
                    scrapedContent = scrapedContent,
                    analysisResult = analysisResult,
                    isAnalyzing = isAnalyzing,
                    onUrlChanged = { viewModel.setScraperUrl(it) },
                    onScrapeClick = { viewModel.scrapeCurrentUrl() },
                    onAnalyzeClick = { viewModel.analyzeScrapedContent(it) },
                    onSendToChatClick = { viewModel.sendScrapedToChat() }
                )
                2 -> HistoryScreen(
                    sessions = historySessions,
                    searchQuery = historySearchQuery,
                    onSearchQueryChanged = { viewModel.setHistorySearchQuery(it) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onDeleteSession = { viewModel.deleteSession(it) },
                    onClearAllHistory = { viewModel.clearAllHistory() },
                    onLoadSessionToChat = { viewModel.reloadSessionIntoChat(it) }
                )
                3 -> RepoInfoScreen()
            }
        }
    }
}

/**
 * Backward compatibility component for screenshot unit tests.
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}
