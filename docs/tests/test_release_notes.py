from __future__ import annotations

import re
import unittest
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[2]
README_PATH = REPOSITORY_ROOT / "README.md"
RELEASE_NOTES_PATH = REPOSITORY_ROOT / "docs" / "release-notes-3.1.1-3.3.0.md"


class ReleaseNotesTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.readme = README_PATH.read_text(encoding="utf-8")
        cls.release_notes = RELEASE_NOTES_PATH.read_text(encoding="utf-8")

    def test_readme_links_to_release_notes(self) -> None:
        self.assertIn("docs/release-notes-3.1.1-3.3.0.md", self.readme)

    def test_release_notes_cover_requested_versions_and_current_dependency(self) -> None:
        for version in ("3.1.1", "3.1.6", "3.2.0", "3.3.0"):
            self.assertIn(version, self.release_notes)
        self.assertIn(
            'implementation("com.github.nguyenvuong0308:CodeBase:3.3.0")',
            self.release_notes,
        )

    def test_release_notes_cover_migration_contracts(self) -> None:
        required_terms = [
            "BaseSplashViewModel",
            "startflow_language_v2_apply_use_text",
            "startflow_language_image_background",
            "meaningful_actions_between_interstitial",
            "small_banner_cta_right",
        ]
        for term in required_terms:
            self.assertIn(term, self.release_notes, f"Missing migration term: {term}")

    def test_relative_links_exist(self) -> None:
        for href in re.findall(r"\[[^\]]+\]\(([^)]+)\)", self.release_notes):
            if href.startswith(("http://", "https://", "#")):
                continue
            target = (RELEASE_NOTES_PATH.parent / href).resolve()
            self.assertTrue(target.is_file(), f"Broken relative link: {href}")


if __name__ == "__main__":
    unittest.main()
