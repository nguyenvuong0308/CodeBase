from __future__ import annotations

import re
import unittest
from html.parser import HTMLParser
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[2]
GUIDE_PATH = REPOSITORY_ROOT / "docs" / "startflow-language-ui-customization.html"
AD_PLACE_SOURCE = (
    REPOSITORY_ROOT
    / "core"
    / "config"
    / "src"
    / "main"
    / "java"
    / "com"
    / "core"
    / "config"
    / "domain"
    / "data"
    / "IAdPlaceName.kt"
)


class GuideParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.ids: list[str] = []
        self.hrefs: list[str] = []
        self.external_sources: list[str] = []
        self.document_language: str | None = None
        self.has_viewport = False
        self.has_title = False
        self._inside_title = False

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        attributes = {name: value or "" for name, value in attrs}
        if attributes.get("id"):
            self.ids.append(attributes["id"])
        if attributes.get("href"):
            self.hrefs.append(attributes["href"])
        if tag == "html":
            self.document_language = attributes.get("lang")
        if tag == "meta" and attributes.get("name") == "viewport":
            self.has_viewport = "width=device-width" in attributes.get("content", "")
        if tag == "title":
            self._inside_title = True
        for source_attribute in ("src", "href"):
            source = attributes.get(source_attribute, "")
            if source.startswith(("http://", "https://", "//")):
                self.external_sources.append(source)

    def handle_endtag(self, tag: str) -> None:
        if tag == "title":
            self._inside_title = False

    def handle_data(self, data: str) -> None:
        if self._inside_title and data.strip():
            self.has_title = True


class StartFlowLanguageGuideTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.html = GUIDE_PATH.read_text(encoding="utf-8")
        cls.parser = GuideParser()
        cls.parser.feed(cls.html)
        cls.parser.close()

    def test_document_has_required_metadata_and_is_offline_ready(self) -> None:
        self.assertRegex(self.html.lower(), r"^\s*<!doctype html>")
        self.assertEqual(self.parser.document_language, "vi")
        self.assertTrue(self.parser.has_viewport)
        self.assertTrue(self.parser.has_title)
        self.assertEqual(self.parser.external_sources, [])

    def test_step_sections_are_present_in_implementation_order(self) -> None:
        expected_sections = [
            "scope",
            "remote-config",
            "resources",
            "ad-places",
            "navigation",
            "verify",
            "pitfalls",
        ]
        positions = [self.html.index(f'id="{section_id}"') for section_id in expected_sections]
        self.assertEqual(positions, sorted(positions))

    def test_internal_links_point_to_unique_existing_ids(self) -> None:
        self.assertEqual(len(self.parser.ids), len(set(self.parser.ids)), "Duplicate HTML id found")
        known_ids = set(self.parser.ids)
        for href in self.parser.hrefs:
            if href.startswith("#"):
                self.assertIn(href[1:], known_ids, f"Missing anchor target for {href}")

    def test_relative_file_links_exist(self) -> None:
        for href in self.parser.hrefs:
            if href.startswith(("#", "mailto:", "tel:")):
                continue
            relative_path = href.partition("#")[0]
            target = (GUIDE_PATH.parent / relative_path).resolve()
            self.assertTrue(target.is_file(), f"Broken relative file link: {href}")

    def test_guide_covers_language_configuration_contract(self) -> None:
        required_terms = [
            "application_config",
            "language_activity_config",
            "is_enable_change_language_screen",
            "is_enable_introduction_screen",
            "startflow_language_v2_apply_use_text",
            "startflow_language_v2_apply_text_value",
            "startflow_language_v2_apply_background",
            "StartFlowNavigator",
            "LanguageActivityNavigator",
            "banner_native_ad_places",
            "rewarded_rewardedinter_inter_ad_places",
        ]
        for term in required_terms:
            self.assertIn(term, self.html, f"Missing required Language concept: {term}")

    def test_documented_language_ad_places_match_core_names(self) -> None:
        required_place_names = [
            "flow_tutorial_v1_201_language_1_n_native",
            "flow_tutorial_v1_201_language_2_n_native",
            "flow_tutorial_201_language_1_n_native",
            "flow_tutorial_201_language_2_n_native",
            "flow_tutorial_201_language_3_n_native",
            "anchored_change_language_v2_from_setting_native",
            "fullscreen_back_language_setting",
        ]
        source = AD_PLACE_SOURCE.read_text(encoding="utf-8")
        for place_name in required_place_names:
            self.assertIn(place_name, source, f"Core no longer declares {place_name}")
            self.assertIn(place_name, self.html, f"Guide does not document {place_name}")

    def test_page_contains_visual_step_by_step_aids(self) -> None:
        for class_name in ("reading-progress", "flow-map", "file-grid", "ad-map"):
            self.assertRegex(
                self.html,
                rf'class="[^"]*\b{re.escape(class_name)}\b',
                f"Missing visual aid: {class_name}",
            )
        self.assertGreaterEqual(self.html.count('class="step-number"'), 6)


if __name__ == "__main__":
    unittest.main()
