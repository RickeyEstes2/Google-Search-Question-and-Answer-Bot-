import pytest
from scraper import WebScraper
from llm_agent import GoogleSearchQABot

def test_scraper_clean_text():
    scraper = WebScraper()
    # Test scraping simulated or local text
    res = scraper.scrape_url("https://example.com")
    assert "url" in res
    assert "title" in res
    assert "content" in res

def test_scraper_search_snippets():
    scraper = WebScraper()
    snippets = scraper.simulate_search_snippets("Android Kotlin Compose")
    assert isinstance(snippets, list)
    assert len(snippets) > 0
    assert "title" in snippets[0]
    assert "url" in snippets[0]

def test_bot_initialization_and_mock():
    bot = GoogleSearchQABot(api_key="test-mock-key")
    res = bot.answer_question("What is Python?")
    assert "answer" in res
    assert "sources" in res
    assert res["model_used"] == "mock-simulator"

def test_bot_modes():
    bot = GoogleSearchQABot(api_key="test-mock-key")
    res_high = bot.answer_question("Solve Riemann hypothesis", mode="high_thinking")
    assert "Simulated Answer" in res_high["answer"]

    res_fast = bot.answer_question("Fast response", mode="fast")
    assert "Simulated Answer" in res_fast["answer"]
