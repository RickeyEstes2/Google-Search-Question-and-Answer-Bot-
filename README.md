# Google Search Question and Answer Bot

A powerful hybrid AI application combining a modern **Android Native Client** (Jetpack Compose, Room, Material 3) and a **Python Web Scraper & LLM Integration**, backed by Google Gemini and live Google Search Grounding. Fully configured with **GitHub Actions** CI pipelines.

---

## 🌟 Key Highlights

- 🔍 **Google Search Grounding**: Live, up-to-the-minute web search answers using Gemini with verified source citations and clickable URLs.
- 🧠 **High-Thinking Mode**: Deep reasoning engine powered by `gemini-3.1-pro-preview` with `ThinkingLevel.HIGH` for complex logic, math, analysis, and research queries.
- ⚡ **Multi-Model Intelligence**: Seamlessly switch between:
  - **`gemini-3.5-flash`** (with Google Search Grounding for live web queries)
  - **`gemini-3.1-pro-preview`** (with high thinking for complex problem solving)
  - **`gemini-3.1-flash-lite-preview`** (for ultra-fast responses)
- 🕷️ **Python Web Scraper**: Built-in scraper module (`python/scraper.py`) to fetch web pages, clean HTML boilerplate, extract key paragraphs, and feed clean context directly into Gemini LLM.
- 💾 **Local Offline Persistence**: Room Database stores complete search Q&A threads, queries, citations, and favorite bookmarks.
- 🚀 **GitHub Actions Pushable**: Automated CI workflows in `.github/workflows/ci.yml` verifying both the Python suite (pytest) and the Android Gradle test suite.

---

## 📁 Repository Structure

```
├── .github/
│   └── workflows/
│       └── ci.yml               # GitHub Actions CI workflow (Python & Android)
├── app/                         # Android Native Application
│   ├── src/main/java/com/example/
│   │   ├── data/
│   │   │   ├── local/           # Room Database, DAO, Entities
│   │   │   └── remote/          # Gemini REST API, Search Grounding, Scraper
│   │   ├── ui/                  # Compose screens (Chat, Scraper, History, Repo)
│   │   └── MainActivity.kt      # Main Entry Point & Edge-to-Edge Navigation
├── python/                      # Python Web Scraper & LLM Integration
│   ├── scraper.py               # Web scraper & search query extractor
│   ├── llm_agent.py             # Gemini LLM Q&A agent with search grounding
│   ├── requirements.txt         # Python dependencies
│   └── test_scraper_and_bot.py  # Pytest test suite
├── metadata.json                # AI Studio platform configuration
└── README.md
```

---

## 🛠️ Python Scraper & LLM Usage

### Installation

```bash
cd python
pip install -r requirements.txt
```

### Running the Q&A Bot with Google Search Grounding

```bash
export GEMINI_API_KEY="your-gemini-api-key"

# Live Google Search Grounded query
python llm_agent.py "What are the key highlights from Google I/O 2026?"

# Deep thinking reasoning
python -c 'from llm_agent import GoogleSearchQABot; bot = GoogleSearchQABot(); print(bot.answer_question("Explain quantum error correction algorithms", mode="high_thinking")["answer"])'
```

### Running Tests

```bash
pytest python/ -v
```

---

## 📱 Android App Architecture

- **UI Framework**: 100% Jetpack Compose with Material Design 3 and dynamic theming.
- **Networking**: OkHttp 4 & Retrofit 2 communicating directly with Gemini's `v1beta/models` REST endpoints.
- **Database**: Room Database with Coroutines & Kotlin Flow (`AppDatabase`, `QADao`, `QARepository`).
- **Secrets**: `BuildConfig.GEMINI_API_KEY` seamlessly injected via Secrets Gradle Plugin and AI Studio Secrets panel.
