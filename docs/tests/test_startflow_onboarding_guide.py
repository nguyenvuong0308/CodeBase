from __future__ import annotations

import re
import unittest
from html.parser import HTMLParser
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[2]
GUIDE_PATH = REPOSITORY_ROOT / "docs" / "startflow-onboarding-ui-customization.html"


class GuideParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.ids: list[str] = []
        self.hrefs: list[str] = []
        self.external_sources: list[str] = []
        self.tags: list[tuple[str, dict[str, str]]] = []
        self.document_language: str | None = None
        self.has_viewport = False
        self.has_title = False
        self._inside_title = False

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        attributes = {name: value or "" for name, value in attrs}
        self.tags.append((tag, attributes))
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


class StartFlowOnboardingGuideTest(unittest.TestCase):
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

    def test_step_sections_are_present_in_learning_order(self) -> None:
        expected_sections = [
            "choose",
            "resources",
            "content",
            "branding",
            "hilt",
            "remote-config",
            "verify",
            "renderer",
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
            target = (GUIDE_PATH.parent / href).resolve()
            self.assertTrue(target.is_file(), f"Broken relative file link: {href}")

    def test_version_tabs_have_accessible_wiring(self) -> None:
        tags = self.parser.tags
        tab_buttons = [attrs for tag, attrs in tags if tag == "button" and attrs.get("role") == "tab"]
        tab_panels = [attrs for tag, attrs in tags if attrs.get("role") == "tabpanel"]
        self.assertEqual({tab["id"] for tab in tab_buttons}, {"tab-v1", "tab-v2", "tab-v3"})
        self.assertEqual({panel["id"] for panel in tab_panels}, {"panel-v1", "panel-v2", "panel-v3"})
        for tab in tab_buttons:
            self.assertIn(tab.get("aria-controls"), {panel["id"] for panel in tab_panels})
            self.assertIn(tab.get("aria-selected"), {"true", "false"})

    def test_guide_covers_core_customization_contracts(self) -> None:
        required_terms = [
            "OnBoardingContentProvider",
            "OnBoardingV1UiCustomizer",
            "OnBoardingV2UiCustomizer",
            "OnBoardingV3UiCustomizer",
            "OnBoardingV3PageRenderer",
            "@IntoSet",
            "intro_data_v3",
            "realPosition",
            "onPrimaryAction()",
            "onBannerNativeResult",
        ]
        for term in required_terms:
            self.assertIn(term, self.html, f"Missing required onboarding concept: {term}")

    def test_page_contains_visual_step_by_step_aids(self) -> None:
        required_visual_classes = ["phone-stage", "path", "flow-visual", "mini-preview", "reading-progress"]
        for class_name in required_visual_classes:
            self.assertRegex(
                self.html,
                rf'class="[^"]*\b{re.escape(class_name)}\b',
                f"Missing visual aid: {class_name}",
            )
        self.assertGreaterEqual(self.html.count('class="step-number"'), 7)


if __name__ == "__main__":
    unittest.main()
