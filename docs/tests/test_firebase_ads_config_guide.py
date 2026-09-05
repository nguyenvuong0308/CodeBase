from __future__ import annotations

import json
import re
import unittest
from html import unescape
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[2]
GUIDE_PATH = (
    REPOSITORY_ROOT
    / "docs"
    / "firebase-ads-guide"
    / "firebase_ads_config_guide.html"
)


class FirebaseAdsConfigGuideTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.html = GUIDE_PATH.read_text(encoding="utf-8")

    def test_language_activity_example_contains_click_guide_config(self) -> None:
        match = re.search(
            r'<pre><code>(\{\s*"version":\s*2,\s*'
            r'"time_show_loading_lfo":\s*3,\s*'
            r'"is_show_click_guide":\s*true\s*\})</code></pre>',
            self.html,
        )

        self.assertIsNotNone(match, "Missing language_activity_config JSON example")
        config = json.loads(unescape(match.group(1)))
        self.assertIs(config["is_show_click_guide"], True)

    def test_click_guide_config_documents_complete_boolean_contract(self) -> None:
        self.assertIn("<code>is_show_click_guide</code>", self.html)
        self.assertRegex(
            self.html,
            r"is_show_click_guide[\s\S]*?Boolean[\s\S]*?"
            r"<code>true</code>[\s\S]*?<code>false</code>[\s\S]*?mặc định bật",
        )
        self.assertIn("LanguageActivityV21", self.html)
        self.assertIn("group thứ 3", self.html)


if __name__ == "__main__":
    unittest.main()
