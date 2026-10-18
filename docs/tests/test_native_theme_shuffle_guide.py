import json
import struct
import unittest
from pathlib import Path
from html import unescape

ROOT = Path(__file__).resolve().parents[2]
DOCS = ROOT / 'docs/firebase-ads-guide'


class NativeThemeShuffleGuideTest(unittest.TestCase):
    def test_full_remote_configs_and_real_outcome_screenshots(self):
        data = (DOCS / 'native-theme-shuffle-cases.json').read_bytes()
        self.assertEqual(data, (ROOT / 'app/src/androidTest/assets/native-theme-shuffle-cases.json').read_bytes())
        cases = json.loads(data)
        self.assertEqual([4, 5, 6], [c['version'] for c in cases])
        guide = (DOCS / 'firebase_ads_config_guide.html').read_text(encoding='utf-8')
        for case in cases:
            config = case['config']
            self.assertEqual('shuffle', config['native_theme'])
            self.assertEqual({'light', 'dark'}, set(config['native_theme_colors']))
            self.assertIn(json.dumps(config, ensure_ascii=False, indent=2), unescape(guide))
            self.assertNotEqual(config['native_theme_colors']['light']['primary_text_color'], config['native_theme_colors']['dark']['primary_text_color'])
            for orientation, size in [('portrait', (1080, 2400)), ('landscape', (2400, 1080))]:
                for theme in ('light', 'dark'):
                    prefix = f"guide-{case['id']}-{orientation}-{theme}"
                    evidence = json.loads((DOCS / f'native-interstitial-evidence/{prefix}.json').read_text())
                    self.assertEqual(config, evidence['config'])
                    self.assertEqual(theme, evidence['actualTheme'])
                    self.assertEqual(case['id'], evidence['caseId'])
                    self.assertEqual(orientation, evidence['orientation'])
                    self.assertIs(evidence['passed'], True)
                    self.assertEqual('Pixel 7a', evidence['device'])
                    for state in ('waiting', 'ready'):
                        name = f'{prefix}-{state}.png'
                        self.assertIn(f'src="native-interstitial-screenshots/{name}"', guide)
                        png = (DOCS / f'native-interstitial-screenshots/{name}').read_bytes()
                        self.assertEqual(b'\x89PNG\r\n\x1a\n', png[:8])
                        self.assertEqual(size, struct.unpack('>II', png[16:24]))


    def test_template_shuffle_config_and_all_six_outcomes(self):
        data = (DOCS / 'native-template-shuffle-cases.json').read_bytes()
        self.assertEqual(data, (ROOT / 'app/src/androidTest/assets/native-template-shuffle-cases.json').read_bytes())
        case, = json.loads(data)
        config = case['config']
        self.assertEqual('full_interstitial_shuffle', config['native_template_size'])
        self.assertEqual('shuffle', config['native_theme'])
        self.assertEqual('shuffle', config['control_close_position'])
        guide = (DOCS / 'firebase_ads_config_guide.html').read_text(encoding='utf-8')
        self.assertIn(json.dumps(config, ensure_ascii=False, indent=2), unescape(guide))
        for version in (4, 5, 6):
            for theme in ('light', 'dark'):
                for orientation, size in [('portrait', (1080, 2400)), ('landscape', (2400, 1080))]:
                    prefix = f'guide-template-shuffle-{orientation}-v{version}-{theme}'
                    evidence = json.loads((DOCS / f'native-interstitial-evidence/{prefix}.json').read_text())
                    self.assertEqual(config, evidence['config'])
                    self.assertEqual(f'full_interstitial_v{version}', evidence['actualTemplate'])
                    self.assertEqual(theme, evidence['actualTheme'])
                    self.assertEqual(orientation, evidence['orientation'])
                    self.assertIs(evidence['passed'], True)
                    for state in ('waiting', 'ready'):
                        name = f'{prefix}-{state}.png'
                        self.assertIn(f'src="native-interstitial-screenshots/{name}"', guide)
                        png = (DOCS / f'native-interstitial-screenshots/{name}').read_bytes()
                        self.assertEqual(b'\x89PNG\r\n\x1a\n', png[:8])
                        self.assertEqual(size, struct.unpack('>II', png[16:24]))


if __name__ == '__main__':
    unittest.main()
