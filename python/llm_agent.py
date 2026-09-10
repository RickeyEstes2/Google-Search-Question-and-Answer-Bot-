#!/usr/bin/env python3
"""
Google Search Question and Answer Bot - LLM Integration.
Provides grounded Q&A over live web search and deep thinking reasoning with Gemini.
"""

import os
import sys
from typing import Optional, List, Dict
import requests

from scraper import WebScraper

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")

class GoogleSearchQABot:
    def __init__(self, api_key: Optional[str] = None):
        self.api_key = api_key or os.getenv("GEMINI_API_KEY", "")
        self.scraper = WebScraper()
        self.history: List[Dict[str, str]] = []
        self.base_url = "https://generativelanguage.googleapis.com/v1beta/models"

    def answer_question(
        self,
        question: str,
        mode: str = "search_grounded",
        scrape_url: Optional[str] = None
    ) -> Dict[str, any]:
        """
        Answers a question using Gemini with either Google Search Grounding,
        high-thinking reasoning, or direct scraped web page context.

        Modes:
          - 'search_grounded': uses gemini-3.5-flash with googleSearch tool
          - 'high_thinking': uses gemini-3.1-pro-preview with thinkingLevel='HIGH'
          - 'fast': uses gemini-3.1-flash-lite-preview
        """
        if not self.api_key or self.api_key == "dummy_ci_key" or self.api_key == "test-mock-key":
            return {
                "answer": (
                    f"[Simulated Answer for '{question}']\n"
                    f"Mode: {mode}\n"
                    "Configure GEMINI_API_KEY in your environment to fetch live grounded answers from Google Search."
                ),
                "sources": [
                    {"title": "Google Search Grounding Index", "url": "https://google.com"}
                ],
                "model_used": "mock-simulator"
            }

        # Select model and configurations
        if mode == "high_thinking":
            model = "gemini-3.1-pro-preview"
            payload = {
                "contents": [{"role": "user", "parts": [{"text": question}]}],
                "generationConfig": {
                    "thinkingConfig": {"thinkingLevel": "HIGH"}
                },
                "systemInstruction": {
                    "parts": [{"text": "You are Google Search Q&A Bot. Reason step-by-step through complex queries with high depth and clarity."}]
                }
            }
        elif mode == "fast":
            model = "gemini-3.1-flash-lite-preview"
            payload = {
                "contents": [{"role": "user", "parts": [{"text": question}]}],
                "systemInstruction": {
                    "parts": [{"text": "You are Google Search Q&A Bot. Answer concisely and rapidly."}]
                }
            }
        else: # Default search_grounded
            model = "gemini-3.5-flash"
            scraped_context = ""
            if scrape_url:
                scrape_res = self.scraper.scrape_url(scrape_url)
                scraped_context = f"\nScraped context from {scrape_url}:\n{scrape_res['content']}\n"

            payload = {
                "contents": [
                    {
                        "role": "user",
                        "parts": [{"text": f"{question}\n{scraped_context}".strip()}]
                    }
                ],
                "tools": [
                    {"google_search": {}}
                ],
                "systemInstruction": {
                    "parts": [{
                        "text": "You are Google Search Q&A Bot, an expert research assistant. Synthesize real-time search knowledge accurately with verified citations."
                    }]
                }
            }

        endpoint = f"{self.base_url}/{model}:generateContent?key={self.api_key}"
        try:
            resp = requests.post(endpoint, json=payload, timeout=45)
            resp.raise_for_status()
            data = resp.json()

            answer_text = ""
            sources = []

            candidates = data.get("candidates", [])
            if candidates:
                cand = candidates[0]
                parts = cand.get("content", {}).get("parts", [])
                answer_text = "\n".join([p.get("text", "") for p in parts if "text" in p])

                # Extract Google Search Grounding Metadata
                grounding_metadata = cand.get("groundingMetadata", {})
                chunks = grounding_metadata.get("groundingChunks", [])
                for ch in chunks:
                    web = ch.get("web", {})
                    if web.get("uri"):
                        sources.append({
                            "title": web.get("title", "Web Source"),
                            "url": web.get("uri")
                        })

            return {
                "answer": answer_text or "No response generated.",
                "sources": sources,
                "model_used": model,
                "status": "success"
            }
        except Exception as e:
            return {
                "answer": f"Error communicating with Gemini: {str(e)}",
                "sources": [],
                "model_used": model,
                "status": "error"
            }

if __name__ == "__main__":
    bot = GoogleSearchQABot()
    query = " ".join(sys.argv[1:]) if len(sys.argv) > 1 else "What are the latest discoveries from the James Webb Space Telescope?"
    print(f"🤖 Querying Google Search Q&A Bot: {query}...\n")
    res = bot.answer_question(query)
    print(f"--- Answer ({res['model_used']}) ---")
    print(res["answer"])
    if res.get("sources"):
        print("\n--- Sources ---")
        for s in res["sources"]:
            print(f"- {s['title']}: {s['url']}")
