// Offline DOM integration test; no browser, navigation, network, or image rendering.
// Install test dependency into ignored build/: npm install --prefix build/native-guide-dom --no-save --package-lock=false linkedom@0.18.12
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { parseHTML } = require('../../build/native-guide-dom/node_modules/linkedom');
const root = path.resolve(__dirname, '../..');
const html = fs.readFileSync(path.join(root, 'docs/firebase-ads-guide/firebase_ads_config_guide.html'), 'utf8');
const cases = JSON.parse(fs.readFileSync(path.join(root, 'docs/firebase-ads-guide/native-interstitial-cases.json'), 'utf8'));
const { document, Event } = parseHTML(html);
const windowEvents = new Map();
const location = { hash: '' };
let copied = '';
// Supply browser services not implemented by this DOM-only parser.
for (const link of document.querySelectorAll('a[href]')) {
  Object.defineProperty(link, 'hash', { get: () => new URL(link.getAttribute('href'), 'https://example.invalid/guide.html').hash });
}
for (const select of document.querySelectorAll('select')) {
  if (![...select.querySelectorAll('option')].some(o => o.selected)) select.querySelector('option').selected = true;
}
const context = {
  document, location, console,
  navigator: { clipboard: { writeText: async text => { copied = text; } } },
  window: { addEventListener: (name, fn) => windowEvents.set(name, fn), setTimeout: () => {} },
  IntersectionObserver: class { observe() {} },
};
vm.runInNewContext(html.match(/<script>([\s\S]*?)<\/script>/)[1], context);
const byId = id => document.getElementById(id);
const activeCases = () => [...document.querySelectorAll('.native-fake-case')].filter(c => !c.hidden);
const selectValue = (id, value) => {
  const select = byId(id);
  for (const option of select.querySelectorAll('option')) option.selected = false;
  const option = [...select.querySelectorAll('option')].find(o => (o.getAttribute('value') ?? o.textContent) === value);
  assert.ok(option, `${id}: ${value}`);
  option.selected = true;
  select.dispatchEvent(new Event('change'));
};

(async () => {
  assert.equal(byId('guide-general-panel').hidden, false);
  assert.equal(byId('guide-native-panel').hidden, true);
  byId('guide-native-tab').click();
  assert.equal(byId('guide-native-tab').getAttribute('aria-selected'), 'true');
  assert.equal(byId('guide-native-panel').hidden, false);
  assert.equal(byId('guide-general-panel').hidden, true);
  assert.equal(location.hash, 'native-fake-interstitial');
  for (const item of cases) {
    selectValue('nf-version', String(item.version));
    selectValue('nf-theme', item.theme);
    selectValue('nf-position', item.position);
    selectValue('nf-style', item.style);
    selectValue('nf-ready-style', item.readyStyle);
    assert.equal(activeCases().length, 1);
    const card = activeCases()[0];
    assert.equal(card.dataset.case, item.id);
    assert.deepEqual(JSON.parse(card.querySelector('pre code').textContent), item.config);
    card.querySelector('.copy-button').click();
    await Promise.resolve();
    assert.deepEqual(JSON.parse(copied), item.config);
    for (const orientation of ['portrait', 'landscape', 'both']) {
      selectValue('nf-orientation', orientation);
      const figures = [...card.querySelectorAll('figure')].filter(f => !f.hidden);
      assert.equal(figures.length, orientation === 'both' ? 4 : 2);
      for (const figure of figures) {
        if (orientation !== 'both') assert.equal(figure.dataset.orientation, orientation);
        assert.ok(figure.querySelector('img').getAttribute('src').includes(`guide-${item.id}-${figure.dataset.orientation}-`));
      }
      assert.ok(byId('nf-result').textContent.includes(item.id));
    }
  }
  byId('guide-general-tab').click();
  assert.equal(byId('guide-native-panel').hidden, true);
  location.hash = '#native-fake-interstitial';
  windowEvents.get('hashchange')();
  assert.equal(byId('guide-native-panel').hidden, false);
  for (const hash of ['#native-theme-shuffle', '#native-template-shuffle']) {
    byId('guide-general-tab').click();
    location.hash = hash;
    windowEvents.get('hashchange')();
    assert.equal(byId('guide-native-panel').hidden, false);
  }
  const key = new Event('keydown', { cancelable: true });
  Object.defineProperty(key, 'key', { value: 'Home' });
  byId('guide-native-tab').dispatchEvent(key);
  assert.equal(byId('guide-general-tab').getAttribute('aria-selected'), 'true');
  assert.equal(byId('guide-native-panel').hidden, true);
  assert.ok(document.querySelector('#key-table').parentElement.classList.contains('table-wrap'));
  console.log(`PASS: tabs/hash/keyboard, ${cases.length} config filters and copy actions, ${cases.length * 3} orientation selections (offline DOM).`);
})().catch(error => { console.error(error); process.exitCode = 1; });
