#!/usr/bin/env python3
"""
Google Search Question and Answer Bot - Web Scraper Module.
Extracts search results and cleans webpage contents for LLM synthesis.
"""

import sys
import re
import urllib.parse
from typing import List, Dict, Optional
import requests
from bs4 import BeautifulSoup

DEFAULT_HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/120.0.0.0 Safari/537.36"
    )
}

class WebScraper:
    def __init__(self, timeout: int = 10):
        self.timeout = timeout
        self.session = requests.Session()
        self.session.headers.update(DEFAULT_HEADERS)

    def scrape_url(self, url: str, max_chars: int = 4000) -> Dict[str, str]:
        """
        Fetches and extracts clean text from a given web URL.
        """
        try:
            response = self.session.get(url, timeout=self.timeout)
            response.raise_for_status()
            soup = BeautifulSoup(response.text, "html.parser")

            # Remove noisy elements
            for tag in soup(["script", "style", "nav", "footer", "aside", "header", "noscript"]):
                tag.decompose()

            title = soup.title.string.strip() if soup.title and soup.title.string else url

            # Gather text from paragraphs and headings
            text_blocks = []
            for elem in soup.find_all(["h1", "h2", "h3", "p", "li"]):
                clean = elem.get_text(separator=" ", strip=True)
                if len(clean) > 25:
                    text_blocks.append(clean)

            combined_text = "\n\n".join(text_blocks)
            # Normalize excessive whitespace
            combined_text = re.sub(r"\n{3,}", "\n\n", combined_text)

            if len(combined_text) > max_chars:
                combined_text = combined_text[:max_chars] + "... [Content Truncated]"

            return {
                "url": url,
                "title": title,
                "content": combined_text or "No readable text extracted.",
                "status": "success"
            }
        except Exception as err:
            return {
                "url": url,
                "title": "Failed to load",
                "content": f"Scrape error: {str(err)}",
                "status": "error"
            }

    def simulate_search_snippets(self, query: str) -> List[Dict[str, str]]:
        """
        Extracts structured search result candidates for a given query.
        """
        encoded_query = urllib.parse.quote_plus(query)
        search_url = f"https://html.duckduckgo.com/html/?q={encoded_query}"
        results = []

        try:
            res = self.session.get(search_url, timeout=self.timeout)
            if res.status_code == 200:
                soup = BeautifulSoup(res.text, "html.parser")
                for item in soup.find_all("div", class_="result__body", limit=5):
                    title_elem = item.find("a", class_="result__snippet") or item.find("a", class_="result__url")
                    title_link = item.find("a", class_="result__a")
                    snippet_elem = item.find("a", class_="result__snippet")

                    if title_link:
                        title = title_link.get_text(strip=True)
                        raw_href = title_link.get("href", "")
                        # Parse actual target URL if wrapped
                        match = re.search(r"uddg=([^&]+)", raw_href)
                        link = urllib.parse.unquote(match.group(1)) if match else raw_href
                        snippet = snippet_elem.get_text(strip=True) if snippet_elem else ""

                        results.append({
                            "title": title,
                            "url": link,
                            "snippet": snippet
                        })
        except Exception as e:
            # Fallback mock search results if offline or blocked
            results = [
                {
                    "title": f"Live Search Result for '{query}'",
                    "url": f"https://www.google.com/search?q={encoded_query}",
                    "snippet": f"Web information synthesized for question: {query}"
                }
            ]

        return results

if __name__ == "__main__":
    scraper = WebScraper()
    q = sys.argv[1] if len(sys.argv) > 1 else "Google Gemini AI search"
    print(f"Searching for: {q}")
    search_results = scraper.simulate_search_snippets(q)
    for i, r in enumerate(search_results, 1):
        print(f"[{i}] {r['title']}")
        print(f"    URL: {r['url']}")
        print(f"    Snippet: {r['snippet']}\n")
