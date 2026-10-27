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
            r'"is_show_click_guide":\s*true,\s*'
            r'"v1_confirm_delay_ms":\s*3000,\s*'
            r'"v1_action_use_text":\s*false\s*\})</code></pre>',
            self.html,
        )

        self.assertIsNotNone(match, "Missing language_activity_config JSON example")
        config = json.loads(unescape(match.group(1)))
        self.assertIs(config["is_show_click_guide"], True)
        self.assertEqual(config["v1_confirm_delay_ms"], 3000)
        self.assertIs(config["v1_action_use_text"], False)

    def test_click_guide_config_documents_complete_boolean_contract(self) -> None:
        self.assertIn("<code>is_show_click_guide</code>", self.html)
        self.assertRegex(
            self.html,
            r"is_show_click_guide[\s\S]*?Boolean[\s\S]*?"
            r"<code>true</code>[\s\S]*?<code>false</code>[\s\S]*?mặc định bật",
        )
        self.assertIn("LanguageActivityV21", self.html)
        self.assertIn("group thứ 3", self.html)

    def test_low_device_example_controls_same_type_and_unit_per_placement(self) -> None:
        section = re.search(
            r'<h3 id="low-device-ads">([\s\S]*?)<h3>Versioned ad places</h3>',
            self.html,
        )
        self.assertIsNotNone(section, "Missing per-placement low-device guide")
        blocks = re.findall(r"<pre><code>([\s\S]*?)</code></pre>", section.group(1))
        examples = [
            json.loads(unescape(block))
            for block in blocks
            if unescape(block).strip().startswith("[")
        ]
        self.assertEqual(len(examples), 1)
        blocked, allowed = examples[0]
        self.assertNotEqual(blocked["place_name"], allowed["place_name"])
        self.assertEqual(blocked["ad_type"], allowed["ad_type"])
        self.assertEqual(blocked["ad_id"], allowed["ad_id"])
        self.assertIs(blocked["is_disable_for_low_device"], True)
        self.assertIs(allowed["is_disable_for_low_device"], False)
        self.assertTrue(all(place["is_enable"] for place in examples[0]))

    def test_low_device_field_only_appears_on_placement_json_objects(self) -> None:
        def check(value: object) -> int:
            if isinstance(value, dict):
                count = 0
                if "is_disable_for_low_device" in value:
                    self.assertIn("place_name", value)
                    self.assertIn("ad_type", value)
                    self.assertIsInstance(value["is_disable_for_low_device"], bool)
                    count += 1
                return count + sum(check(child) for child in value.values())
            if isinstance(value, list):
                return sum(check(child) for child in value)
            return 0

        count = 0
        for block in re.findall(r"<pre><code>([\s\S]*?)</code></pre>", self.html):
            content = unescape(block).strip()
            if content.startswith(("{", "[")):
                count += check(json.loads(content))
        self.assertGreaterEqual(count, 2)

    def test_low_device_setup_defaults_logs_and_optional_provider_are_documented(self) -> None:
        for marker in (
            "AdmobAdsInitializer.isLowDevice</code> mặc định <code>false</code>",
            "@Inject lateinit var admobAdsInitializer: AdmobAdsInitializer",
            "admobAdsInitializer.isLowDevice = true",
            "trước khi bắt đầu luồng ads, kể cả preload",
            "Thiếu hoặc <code>null</code> mặc định <code>false</code>",
            "không kế thừa giá trị từ parent",
            "AdmobManager",
            "LowDeviceAds: blocked operation=",
            "mặc định đang được comment",
            "Nếu không đăng ký provider",
        ):
            self.assertIn(marker, self.html)


if __name__ == "__main__":
    unittest.main()
