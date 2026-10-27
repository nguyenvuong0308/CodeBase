import itertools
import json
import re
import struct
import unittest
from html import unescape
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DOCS = ROOT / 'docs/firebase-ads-guide'


class NativeInterstitialGuideTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.raw = (DOCS / 'native-interstitial-cases.json').read_bytes()
        cls.cases = json.loads(cls.raw)
        cls.html = (DOCS / 'firebase_ads_config_guide.html').read_text(encoding='utf-8')

    def test_complete_matrix_and_identical_device_fixture(self):
        expected = {f'v{v}-{t}-{p}-{s}{suffix}' for v, t, p, s, suffix in itertools.product(
            (4, 5, 6), ('dark', 'light'), ('left', 'right', 'shuffle'), ('countdown', 'close'), ('', '-compact'))}
        self.assertEqual(72, len(self.cases))
        self.assertEqual(expected, {c['id'] for c in self.cases})
        self.assertEqual(self.raw, (ROOT / 'app/src/androidTest/assets/native-interstitial-guide-cases.json').read_bytes())

    def test_full_configs_in_html_match_test_inputs_and_themes(self):
        presets = json.loads((ROOT / 'docs/native-interstitial-theme-presets.json').read_text(encoding='utf-8-sig'))
        for case in self.cases:
            with self.subTest(case=case['id']):
                config = case['config']
                expected = next(p.copy() for p in presets if p['place_name'] == config['place_name'])
                expected.update(control_close_position=case['position'], control_close_style=case['style'],
                                count_down_timer=5, hide_text_count_down=False,
                                hide_progress_count_down=False, is_auto_load_after_dismiss=False)
                self.assertIn(case['readyStyle'], ('default', 'compact'))
                if case['readyStyle'] == 'compact':
                    expected['control_close_ready_style'] = 'compact'
                # Historical capture inputs omit this newer optional field; omission means false.
                # An explicit value still wins, so a changed low-device policy fails this check.
                effective_config = {'is_disable_for_low_device': False, **config}
                self.assertEqual(expected, effective_config)
                card = re.search(r'data-case="' + re.escape(case['id']) + r'"[\s\S]*?<pre><code>([\s\S]*?)</code></pre>', self.html)
                self.assertIsNotNone(card)
                self.assertEqual(config, json.loads(unescape(card.group(1))))

    def test_all_screenshots_and_pass_evidence_match_cases(self):
        image_dir = DOCS / 'native-interstitial-screenshots'
        evidence_dir = DOCS / 'native-interstitial-evidence'
        for case in self.cases:
            for orientation, size in [('portrait', (1080, 2400)), ('landscape', (2400, 1080))]:
                prefix = f"guide-{case['id']}-{orientation}"
                with self.subTest(case=case['id'], orientation=orientation):
                    evidence = json.loads((evidence_dir / f'{prefix}.json').read_text(encoding='utf-8'))
                    self.assertIs(evidence['passed'], True)
                    self.assertEqual('Pixel 7a', evidence['device'])
                    self.assertEqual(36, evidence['api'])
                    self.assertEqual(case['id'], evidence['caseId'])
                    self.assertEqual(orientation, evidence['orientation'])
                    self.assertEqual(case['config'], evidence['config'])
                    allowed = ('left', 'right') if case['position'] == 'shuffle' else (case['position'],)
                    self.assertIn(evidence['actualClosePosition'], allowed)
                    if case['readyStyle'] == 'compact':
                        shape = evidence['readyAppearance']
                        self.assertEqual(int(28 * shape['density'] + 0.5), shape['closeSizePx'])
                        self.assertEqual(int(4 * shape['density'] + 0.5), shape['closePaddingPx'])
                        self.assertEqual(int(48 * shape['density'] + 0.5), shape['touchSizePx'])
                        self.assertIs(shape['progressVisible'], False)
                        self.assertIs(shape['hasBackground'], False)
                    for state in ('waiting', 'ready'):
                        name = f'{prefix}-{state}.png'
                        self.assertIn(f'src="native-interstitial-screenshots/{name}"', self.html)
                        data = (image_dir / name).read_bytes()
                        self.assertEqual(b'\x89PNG\r\n\x1a\n', data[:8])
                        self.assertEqual(size, struct.unpack('>II', data[16:24]))

    def test_close_progress_screenshots_match_configs_and_alpha(self):
        for version, hidden, orientation in itertools.product((4, 5, 6), (False, True), ('portrait', 'landscape')):
            cid = f'v{version}-dark-left-close' + ('-hidden-indicators' if hidden else '')
            with self.subTest(case=cid, orientation=orientation):
                base = next(c for c in self.cases if c['id'] == f'v{version}-dark-left-close')
                expected = dict(base['config'], hide_text_count_down=hidden, hide_progress_count_down=hidden)
                card = re.search(r'data-close-case="' + cid + r'"[\s\S]*?<pre><code>([\s\S]*?)</code></pre>', self.html)
                self.assertIsNotNone(card)
                self.assertEqual(expected, json.loads(unescape(card.group(1))))
                prefix = f'guide-{cid}-{orientation}'
                evidence = json.loads((DOCS / 'native-interstitial-evidence' / f'{prefix}.json').read_text(encoding='utf-8'))
                self.assertEqual(expected, evidence['config'])
                self.assertEqual(cid, evidence['caseId'])
                self.assertEqual(orientation, evidence['orientation'])
                self.assertEqual('Pixel 7a', evidence['device'])
                self.assertIs(evidence['passed'], True)
                waiting = evidence['waitingAppearance']
                self.assertAlmostEqual(0.3 if hidden else 0.65, waiting['closeAlpha'], places=5)
                self.assertIs(waiting['closeEnabled'], False)
                self.assertIs(waiting['progressVisible'], not hidden)
                self.assertIs(waiting['textVisible'], False)
                self.assertEqual(1, evidence['readyAppearance']['closeAlpha'])
                self.assertIs(evidence['readyAppearance']['progressVisible'], not hidden)
                for state in ('waiting', 'ready'):
                    name = f'{prefix}-{state}.png'
                    self.assertIn(f'src="native-interstitial-screenshots/{name}"', self.html)
                    data = (DOCS / 'native-interstitial-screenshots' / name).read_bytes()
                    self.assertEqual(b'\x89PNG\r\n\x1a\n', data[:8])
                    self.assertEqual((1080, 2400) if orientation == 'portrait' else (2400, 1080), struct.unpack('>II', data[16:24]))
        for token in ('30%', '65%', '100%', '@dimen/_25dp', '@dimen/_2dp'):
            self.assertIn(token, self.html)
            self.assertIn(token, (ROOT / 'docs/native-interstitial-config.md').read_text(encoding='utf-8'))

    def test_tab_and_filters_are_available(self):
        self.assertIn('.content [role="tabpanel"] > section', self.html)
        self.assertNotIn('.content > section', self.html)
        for suffix in ('general', 'native'):
            self.assertIn(f'id="guide-{suffix}-tab" role="tab" aria-controls="guide-{suffix}-panel"', self.html)
            self.assertIn(f'id="guide-{suffix}-panel" role="tabpanel" aria-labelledby="guide-{suffix}-tab"', self.html)
        for field in ('version', 'theme', 'position', 'style', 'ready-style', 'orientation'):
            self.assertIn(f'<select id="nf-{field}">', self.html)
        self.assertIn('không publish Firebase', self.html)


if __name__ == '__main__':
    unittest.main()
