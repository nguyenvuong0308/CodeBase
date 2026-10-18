"""Generate shared device-test cases and the offline native interstitial guide panel."""
import html
import itertools
import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent
GUIDE = HERE / 'firebase_ads_config_guide.html'
START = '<!-- NATIVE_FAKE_INTERSTITIAL_START -->'
END = '<!-- NATIVE_FAKE_INTERSTITIAL_END -->'


def build_shuffle(presets, assets):
    keys = ('background_color', 'background_full_color', 'media_background_color',
            'primary_text_color', 'body_text_color', 'cta_text_color', 'background_cta',
            'cta_border_color', 'border_color', 'background_color_ads_notify_view',
            'text_color_ads_notify_view', 'progress_bar_tint')
    cases, cards = [], []
    for version in (4, 5, 6):
        palettes = {theme: {k: p[k] for k in keys if k in p}
                    for theme in ('dark', 'light')
                    for p in presets if p['place_name'] == f'fullscreen_native_interstitial_v{version}_{theme}'}
        config = next(p.copy() for p in presets if p['place_name'] == f'fullscreen_native_interstitial_v{version}_dark')
        config.update(native_theme='shuffle', native_theme_colors=palettes,
                      control_close_position='shuffle', control_close_style='close',
                      control_close_ready_style='compact', count_down_timer=5,
                      hide_text_count_down=False, hide_progress_count_down=False,
                      is_auto_load_after_dismiss=False)
        cid = f'v{version}-theme-shuffle'
        cases.append(dict(id=cid, version=version, theme='dark', style='close', config=config))
        photos=[]
        for outcome, orientation, state in itertools.product(('light', 'dark'), ('portrait', 'landscape'), ('waiting', 'ready')):
            path=f'native-interstitial-screenshots/guide-{cid}-{orientation}-{outcome}-{state}.png'
            photos.append(f'<figure><a href="{path}" target="_blank" rel="noopener"><img src="{path}" alt="{cid} {outcome} {orientation} {state}" loading="lazy"></a><figcaption>Random → {outcome} · {orientation} · {state}</figcaption></figure>')
        cards.append(f'<h3>V{version} · một config shuffle, hai kết quả</h3><div class="native-fake-photos">{"".join(photos)}</div><details open><summary>Config placement đầy đủ</summary><pre><code>{html.escape(json.dumps(config, ensure_ascii=False, indent=2))}</code></pre></details>')
    data=json.dumps(cases, ensure_ascii=False, indent=2)+'\n'
    (HERE / 'native-theme-shuffle-cases.json').write_text(data, encoding='utf-8')
    (assets / 'native-theme-shuffle-cases.json').write_text(data, encoding='utf-8')
    theme_panel = f'''<section id="native-theme-shuffle"><h2>Shuffle theme · V4/V5/V6</h2>
<p><code>native_theme: "shuffle"</code> chọn light hoặc dark với xác suất 50/50 khi bind quảng cáo mới. Giữ nguyên trong countdown, pause/resume và cập nhật style của lượt đó; lượt sau có thể trùng theme. Shuffle theme độc lập với shuffle vị trí Close, không theo theme hệ thống.</p>
<p><code>native_theme_colors</code> chứa object <code>light</code> và <code>dark</code>. Các field màu trong palette ghi đè màu gốc của placement; thiếu/null/sai màu hoặc thiếu palette thì giữ màu gốc. Riêng <code>progress_bar_tint</code> thiếu/sai dùng primary_text_color hợp lệ trong palette, rồi mới dùng tint gốc. Chỉ nhận các field màu xuất hiện trong JSON bên dưới và <code>cta_border_color</code>; không đổi kích thước, timer hoặc nội dung ảnh/video. Không có màu light/dark hardcode trong app.</p>
<p><code>native_theme</code> cũng nhận <code>light</code>/<code>dark</code> để cố định một palette; thiếu/null/rỗng/sai giữ toàn bộ màu gốc. Mode chấp nhận chữ hoa và khoảng trắng; tên palette và field màu dùng chữ thường đúng như ví dụ. V1/V2/V3 bỏ qua các field mới.</p>
<p>3 config dưới đây có ảnh thật của cả hai kết quả random, mỗi kết quả có portrait/landscape và waiting/ready: 24 ảnh. Test lặp các lượt random thật đến khi thấy đủ hai kết quả, không ép theme. <a href="native-interstitial-evidence/results-theme-shuffle.txt">Kết quả và lệnh kiểm thử shuffle</a>. Các config là phương án thay thế cùng placement, không thêm trùng place_name.</p>
{''.join(cards)}</section>'''
    config = cases[0]['config'].copy()
    config['native_template_size'] = 'full_interstitial_shuffle'
    case = dict(id='template-shuffle', version=4, theme='dark', style='close', config=config)
    data=json.dumps([case], ensure_ascii=False, indent=2)+'\n'
    (HERE / 'native-template-shuffle-cases.json').write_text(data, encoding='utf-8')
    (assets / 'native-template-shuffle-cases.json').write_text(data, encoding='utf-8')
    photos=[]
    for version, theme, orientation, state in itertools.product((4,5,6), ('light','dark'), ('portrait','landscape'), ('waiting','ready')):
        path=f'native-interstitial-screenshots/guide-template-shuffle-{orientation}-v{version}-{theme}-{state}.png'
        photos.append(f'<figure><a href="{path}" target="_blank" rel="noopener"><img src="{path}" alt="shuffle V{version} {theme} {orientation} {state}" loading="lazy"></a><figcaption>Random → V{version} · {theme} · {orientation} · {state}</figcaption></figure>')
    return theme_panel + f'''<section id="native-template-shuffle"><h2>Shuffle template V4/V5/V6</h2>
<p><code>native_template_size: "full_interstitial_shuffle"</code> chọn ngẫu nhiên V4, V5 hoặc V6 với xác suất 1/3 khi tạo view quảng cáo. Giữ nguyên template trong suốt lượt hiển thị; lượt sau có thể trùng. Cả portrait và landscape dùng XML tương ứng của template được chọn. Thiếu/sai template vẫn fallback V3.</p>
<p>Hoạt động độc lập với <code>native_theme: "shuffle"</code> và <code>control_close_position: "shuffle"</code>. Có thể bật riêng từng loại shuffle. Khi bật cả ba, layout, theme và vị trí Close được chọn độc lập. Hai palette màu được dùng chung cho template được chọn; mọi màu vẫn lấy từ remote config.</p>
<p>Config đầy đủ dưới đây bật cả ba shuffle và Close compact. 24 ảnh thật bao phủ 6 kết quả template × theme ở cả hai orientation, trạng thái waiting/ready. Test chạy random thật đến khi đủ kết quả; không ép template/theme. <a href="native-interstitial-evidence/results-theme-shuffle.txt">Kết quả và lệnh kiểm thử</a>.</p>
<details open><summary>Config placement đầy đủ · shuffle cả template, theme và Close</summary><pre><code>{html.escape(json.dumps(config, ensure_ascii=False, indent=2))}</code></pre></details>
<div class="native-fake-photos">{''.join(photos)}</div></section>'''



def build():
    presets = json.loads((HERE.parent / 'native-interstitial-theme-presets.json').read_text(encoding='utf-8-sig'))
    cases = []
    for version, theme, position, style, ready_style in itertools.product((4, 5, 6), ('dark', 'light'), ('left', 'right', 'shuffle'), ('countdown', 'close'), ('default', 'compact')):
        config = next(p.copy() for p in presets if p['place_name'] == f'fullscreen_native_interstitial_v{version}_{theme}')
        config.update(control_close_position=position, control_close_style=style, count_down_timer=5,
                      hide_text_count_down=False, hide_progress_count_down=False,
                      is_auto_load_after_dismiss=False)
        suffix = '' if ready_style == 'default' else '-compact'
        # Keep the original configs/screenshots as the missing-field/default examples.
        if ready_style == 'compact':
            config['control_close_ready_style'] = 'compact'
        cases.append(dict(id=f'v{version}-{theme}-{position}-{style}{suffix}', version=version,
                          theme=theme, position=position, style=style, readyStyle=ready_style, config=config))
    data = json.dumps(cases, ensure_ascii=False, indent=2) + '\n'
    (HERE / 'native-interstitial-cases.json').write_text(data, encoding='utf-8')
    assets = ROOT / 'app/src/androidTest/assets'
    assets.mkdir(parents=True, exist_ok=True)
    (assets / 'native-interstitial-guide-cases.json').write_text(data, encoding='utf-8')
    shuffle_panel = build_shuffle(presets, assets)
    cards = []
    for case in cases:
        cid = case['id']
        photos = []
        for orientation, state in itertools.product(('portrait', 'landscape'), ('waiting', 'ready')):
            filename = f'guide-{cid}-{orientation}-{state}.png'
            path = f'native-interstitial-screenshots/{filename}'
            label = 'Đang đếm · chưa được đóng' if state == 'waiting' else 'Hết thời gian · đã cho đóng'
            photos.append(f'<figure data-orientation="{orientation}"><a href="{path}" target="_blank" rel="noopener"><img src="{path}" alt="{cid} {orientation} {state}" loading="lazy"></a><figcaption>{orientation} · {label}</figcaption></figure>')
        cards.append(f'''<div class="native-fake-case" data-case="{cid}" hidden>
<h3>V{case['version']} · {case['theme']} · Close {case['position']} · {'Số đếm' if case['style'] == 'countdown' else 'X + progress'} · {'Close 28dp' if case['readyStyle'] == 'compact' else 'Close hiện tại'}</h3>
<p><code>{cid}</code> · Pixel 7a · Android 16 · 5 giây. Bấm ảnh để mở PNG gốc.</p>
<div class="native-fake-photos">{''.join(photos)}</div>
<details open><summary>Config placement đầy đủ · Sao chép JSON bên dưới</summary><pre><code>{html.escape(json.dumps(case['config'], ensure_ascii=False, indent=2))}</code></pre></details>
</div>''')
    panel = f'''{START}
<div id="guide-native-panel" role="tabpanel" aria-labelledby="guide-native-tab" hidden>
<section id="native-fake-interstitial">
<p class="section-kicker">Native SDK · Fullscreen dialog</p><h2>Native Fake Interstitial</h2>
<div class="config-keys" data-config-key="rewarded_rewardedinter_inter_ad_places"><code>rewarded_rewardedinter_inter_ad_places</code></div>
<p><a href="#native-theme-shuffle">Mới: Shuffle light/dark theme — config và ảnh cả hai kết quả</a> · <a href="#native-template-shuffle">Shuffle template V4/V5/V6</a></p>
<p>72 config cho V4/V5/V6 × dark/light × trái/phải/shuffle × số/progress × Close hiện tại/compact; mỗi config có ảnh portrait và landscape trên Pixel 7a. 144 ca hiển thị, 288 ảnh chụp ở hai trạng thái. Mỗi bộ ảnh bên dưới đi kèm đúng JSON đã dùng trong ca đó.</p>
<p><a href="native-interstitial-evidence/results.txt">Kết quả kiểm thử và lệnh chạy lại</a> · <a href="capture_native_interstitial_guide.ps1" download>Script chạy test và thu thập ảnh</a></p>
<div class="note"><strong>Cách dùng:</strong> chọn một case, sao chép JSON và thay placement tương ứng trong danh sách fullscreen; giữ <code>place_name</code> đã đăng ký và thay <code>ad_id</code> bằng native ad unit của bạn. Các ví dụ dùng Google test native ID. Không dán cả 72 case vào một danh sách vì chúng là các phương án thay thế cho cùng placement. Publish rồi fetch/activate Remote Config trước lần hiển thị mới. Bộ ảnh này dùng config fixture trong kiểm thử, không publish Firebase.</div>
<table><thead><tr><th>Field</th><th>Hành vi</th></tr></thead><tbody>
<tr><td><code>native_template_size</code></td><td><code>full_interstitial_v4</code>, <code>full_interstitial_v5</code>, <code>full_interstitial_v6</code>. XML riêng cho portrait/landscape. Type <code>native_interstitial</code> nhận V1–V6 hoặc <code>full_interstitial_shuffle</code> để random V4/V5/V6; thiếu/sai template fallback V3.</td></tr>
<tr><td><code>control_close_position</code></td><td><code>left</code>, <code>right</code>, <code>shuffle</code>. Shuffle chọn trái/phải 50/50 mỗi lần bind, giữ nguyên khi countdown/pause/resume; nhãn Ad ở phía đối diện. Các lượt độc lập có thể trùng bên. Thiếu/sai dùng right; start/end là alias.</td></tr>
<tr><td><code>control_close_style</code></td><td><code>countdown</code>: số rồi X. <code>close</code>: X mờ với vòng progress, hết thời gian mới bấm được. Thiếu/sai dùng countdown. Hai kiểu này dành cho V4–V6.</td></tr>
<tr><td><code>control_close_ready_style</code></td><td><code>default</code>: giữ giao diện hiện tại sau countdown. <code>compact</code>: chỉ khi hết giờ, ẩn vòng progress và nền bao quanh; nút Close 28×28dp, padding 4dp, nằm giữa vùng chạm trong suốt 48×48dp. Màu X vẫn lấy <code>primary_text_color</code>, vị trí trái/phải/shuffle không đổi. Áp dụng cho cả số và progress, portrait/landscape. Thiếu/null/rỗng/sai dùng default; chấp nhận chữ hoa và khoảng trắng. Timer 0/âm chuyển compact ngay khi bind.</td></tr>
<tr><td><code>count_down_timer</code></td><td>Case dùng 5 giây, mặc định 5. Giá trị 0 hoặc âm cho đóng ngay ở V4–V6. Không đổi thời gian chờ khi chỉ đổi kiểu hiển thị.</td></tr>
<tr><td><code>hide_text_count_down</code>, <code>hide_progress_count_down</code></td><td>Ẩn số hoặc vòng progress tương ứng, không cho đóng sớm. Ma trận này để false để nhìn rõ hai kiểu.</td></tr>
<tr><td><code>primary_text_color</code>, <code>body_text_color</code>, <code>progress_bar_tint</code></td><td>Màu title/X/số, mô tả, vòng progress. Progress thiếu tint dùng primary_text_color.</td></tr>
<tr><td>Dark / light</td><td>Do các field màu nền, media, chữ và CTA trong JSON quyết định, không phụ thuộc chế độ hệ thống. Ảnh/video của quảng cáo không được đổi màu theo theme.</td></tr>
</tbody></table>
<p>Ví dụ bật: <code>"control_close_ready_style": "compact"</code>. Để giữ giao diện cũ: <code>"control_close_ready_style": "default"</code> hoặc bỏ field. Các case “Hiện tại” bên dưới giữ config cũ không có field này; các case “Compact” có field rõ ràng. Đổi chế độ chỉ thay hình thức sau countdown, không rút ngắn thời gian chờ. Khi bind quảng cáo mới, UI đếm giờ được khôi phục.</p>
<p>V1/V2/V3 giữ hành vi cũ; V3 có <code>close_step_count</code> 1/2 và timer từng bước. Ma trận số/progress và shuffle trong tab này áp dụng cho V4/V5/V6. Xem <a href="../native-interstitial-config.md">hướng dẫn API và vòng đời</a> hoặc <a href="native-interstitial-cases.json" download>tải toàn bộ case JSON</a>.</p>
<div class="native-fake-filters">
<label>Template<select id="nf-version"><option value="4">V4 · Immersive</option><option value="5">V5 · Clean</option><option value="6">V6 · Spotlight</option></select></label>
<label>Theme<select id="nf-theme"><option value="dark">dark</option><option value="light">light</option></select></label>
<label>Vị trí Close<select id="nf-position"><option value="left">Trái</option><option value="right">Phải</option><option value="shuffle">Shuffle</option></select></label>
<label>Countdown<select id="nf-style"><option value="countdown">Số đếm</option><option value="close">X + progress</option></select></label>
<label>Sau countdown<select id="nf-ready-style"><option value="default">Hiện tại</option><option value="compact">Compact · Close 28dp</option></select></label>
<label>Orientation<select id="nf-orientation"><option value="portrait">Portrait</option><option value="landscape">Landscape</option><option value="both">Cả hai</option></select></label>
</div><p id="nf-result" aria-live="polite"></p>
{''.join(cards)}
</section>{shuffle_panel}</div>
{END}'''
    source = GUIDE.read_text(encoding='utf-8')
    if START in source:
        source = source[:source.index(START)] + panel + source[source.index(END) + len(END):]
    else:
        source = source.replace('    </article>\n  </main>', '    </div>\n' + panel + '\n    </article>\n  </main>')
        source = source.replace('<article class="content">', '''<article class="content">
<div class="guide-tabs" role="tablist" aria-label="Nội dung hướng dẫn">
<button type="button" id="guide-general-tab" role="tab" aria-controls="guide-general-panel" aria-selected="true">Cấu hình chung</button>
<button type="button" id="guide-native-tab" role="tab" aria-controls="guide-native-panel" aria-selected="false" tabindex="-1">Native Fake Interstitial</button>
</div><div id="guide-general-panel" role="tabpanel" aria-labelledby="guide-general-tab">''')
        source = source.replace('<li><a href="#gallery">Native UI gallery</a></li>', '<li><a href="#gallery">Native UI gallery</a></li>\n<li><a href="#native-fake-interstitial">Native Fake Interstitial</a></li>')
    GUIDE.write_text(source, encoding='utf-8')


if __name__ == '__main__':
    build()
