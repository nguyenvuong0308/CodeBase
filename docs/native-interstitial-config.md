# Native interstitial

`native_interstitial` tải quảng cáo bằng Native SDK và hiển thị trong dialog toàn màn hình.
Phía app dùng API load/show fullscreen giống interstitial. Template, countdown và màu sắc
vẫn lấy từ các field của native placement.

## Khai báo Remote Config

Thêm placement vào `rewarded_rewardedinter_inter_ad_places` hoặc key fullscreen override
đang được app sử dụng. `ad_id` phải là **native ad unit ID**.

```json
{
  "place_name": "fullscreen_native_interstitial",
  "ad_id": "ca-app-pub-3940256099942544/2247696110",
  "ad_type": "native_interstitial",
  "native_template_size": "full_interstitial_v3",
  "is_enable": true,
  "is_disable_for_low_device": false,
  "is_auto_load_after_dismiss": true,
  "background_full_color": "#FFFFFF",
  "control_close_position": "left",
  "close_step_count": 1,
  "count_down_timer": 5
}
```

ID trong ví dụ là Google test native ad unit. Với placement mới, khai báo tên tương ứng
trong `AppAdPlaceName` và bảo đảm `IAppProviderAdPlaceName` resolve được tên đó.

| Field | Hành vi |
| --- | --- |
| `ad_type` | Dùng `native_interstitial` để load/show và nhận callback qua fullscreen API. |
| `native_template_size` | Nhận `full_interstitial_v1` đến `full_interstitial_v6`, hoặc `full_interstitial_shuffle` để random V4/V5/V6. Thiếu, rỗng hoặc giá trị khác đều fallback về `full_interstitial_v3`; không ảnh hưởng template của `ad_type: "native"`. |
| `is_auto_load_after_dismiss` | Mặc định `true`. Bắt đầu load ad mới sau khi đóng một ad đã hiển thị. |
| `is_disable_for_low_device` | Cờ riêng của placement, mặc định `false` khi thiếu hoặc `null`. Chỉ chặn load/show khi cờ này và `AdmobAdsInitializer.isLowDevice` của app đều `true`; không đặt trong `native_ad_config`. |
| `expired_time_second` | TTL của native cache; nếu thiếu thì dùng `native_ad_config.expired_time_second`. Ad hết hạn không được show. |
| `is_ignore_interval` | Mặc định `false`. Khi bật, bỏ qua kiểm tra interval, số lượt trong session và meaningful actions. |
| `close_step_count` | Với V3: `1` để countdown rồi đóng; `2` để qua bước Next rồi Close. Mặc định V3 là `2`. |
| `count_down_timer` | Số giây countdown; ví dụ trên dùng 5 giây. |
| `step_1_count_down_timer`, `step_2_count_down_timer` | Override thời gian từng bước của V3; khi thiếu, fallback về `count_down_timer`. |

Xem [cách set low device ở app và đọc log](firebase-ads-guide/firebase_ads_config_guide.html#low-device-ads).
Native lồng trong `native_config` có cờ riêng; các placement cùng loại/cùng ad unit không tự
kế thừa cờ của nhau.

## Template V4 / V5 / V6

V1/V2/V3 giữ nguyên hành vi. Ba template mới có XML độc lập trong `layout/` và
`layout-land/`; Android chọn theo orientation của activity.

| Template | Concept | Placement mẫu trong config dev |
| --- | --- | --- |
| `full_interstitial_v4` | Immersive: media lớn, nội dung cùng nền tối | `fullscreen_native_interstitial_v4` |
| `full_interstitial_v5` | Clean: nền sáng, media bo góc, nội dung rõ ràng | `fullscreen_native_interstitial_v5` |
| `full_interstitial_v6` | Spotlight: card media/nội dung, CTA tách phía dưới | `fullscreen_native_interstitial_v6` |

Các màu đều lấy từ native config: `background_full_color` cho nền màn hình,
`background_color` cho card và nền nút đóng, `media_background_color` cho media,
`primary_text_color` cho tiêu đề/nút đóng/countdown, `body_text_color` cho mô tả,
`background_cta` và `cta_text_color` cho CTA. Không tự thay màu theo độ tương phản.
`background_radius` (dp) điều chỉnh bo góc card, `cta_radius_dp` điều chỉnh CTA.
V4/V5/V6 dùng một bước countdown theo `count_down_timer` (mặc định 5 giây), tạm
dừng khi host pause hoặc view detach; `control_close_position` hỗ trợ `left`, `right`,
và `shuffle` cho V4/V5/V6.

```json
"control_close_position": "shuffle"
```

`shuffle` chọn ngẫu nhiên trái/phải (50/50) khi bind mỗi quảng cáo, giữ nguyên trong
suốt countdown, pause/resume và khi áp lại style. Nhãn Ad nằm ở phía đối diện.
Lần hiển thị tiếp theo chọn lại độc lập, có thể trùng bên với lần trước.
Áp dụng cho cả portrait/landscape và cả `control_close_style: "close"`/`"countdown"`.
Giá trị được trim, không phân biệt hoa/thường; `start` tương đương `left`,
`end` tương đương `right`; thiếu/sai giá trị dùng `right`.
Các preset dark/light giữ vị trí `left`; đổi field thành `shuffle` trong placement
Remote Config muốn dùng, sau đó fetch/activate config trước khi hiển thị quảng cáo mới.
Remote Config có thể chọn riêng cách hiển thị nút đóng cho từng placement V4/V5/V6:

```json
{
  "control_close_style": "close",
  "control_close_position": "left",
  "count_down_timer": 5,
  "hide_progress_count_down": false,
  "progress_bar_tint": "#16A34A"
}
```

- `control_close_style: "countdown"`: hiện số đếm, hết thời gian mới hiện X (hành vi cũ).
- `control_close_style: "close"`: hiện X từ đầu cùng vòng tiến trình như V3 ở chế độ
  một bước; X mờ và chưa bấm được cho đến hết `count_down_timer`.
- Thiếu, null, rỗng hoặc giá trị không hợp lệ: fallback về `countdown`.
  Giá trị được trim và không phân biệt hoa/thường.
- `hide_text_count_down` chỉ ẩn chữ số ở chế độ `countdown`;
  `hide_progress_count_down` ẩn vòng tiến trình ở chế độ `close`. Cả hai không cho đóng sớm.
- Vòng tiến trình lấy `progress_bar_tint`, khi thiếu lấy `primary_text_color`.
  Màu X vẫn lấy `primary_text_color`; nền nút lấy `background_color`.

### Progress và alpha nút Close (V4/V5/V6)

Vòng progress dùng cùng kích thước với V3: `@dimen/_25dp`, nét `@dimen/_2dp`.
Vùng chạm Close vẫn là 48×48dp. Với `control_close_style: "close"`:

| Trạng thái | Alpha nút X | Cho đóng |
| --- | --- | --- |
| Đang chờ, `hide_progress_count_down: false` | 65% | Không |
| Đang chờ, `hide_progress_count_down: true` | 30% | Không |
| Hết thời gian chờ | 100% | Có |

Alpha chỉ áp dụng lên X, không làm mờ nền nút. Timer 0/âm cho đóng ngay với alpha
100%; pause/resume giữ thời gian còn lại, bind quảng cáo mới khôi phục trạng thái chờ.

Ví dụ phần cấu hình để ẩn cả số đếm và progress, giữ X mờ trong lúc chờ:

```json
{
  "control_close_style": "close",
  "count_down_timer": 5,
  "hide_text_count_down": true,
  "hide_progress_count_down": true
}
```

`hide_text_count_down` chỉ tác động tới số ở kiểu `countdown`; hai flag không cho
đóng sớm. [Ảnh Pixel 7a mới và config đầy đủ cho v4–v6](firebase-ads-guide/firebase_ads_config_guide.html#native-close-progress)
so sánh cả hai flag `false`/`true`, dọc/ngang và trước/sau khi được đóng.

Config dev của V4/V5/V6 đang chọn `close`. Đổi field này trong placement trên Remote
Config để áp dụng cho quảng cáo được bind sau khi config mới đã fetch/activate;
không cần đổi template hay build lại app đã hỗ trợ field này. Thay đổi file defaults
trong repo không tự publish lên Firebase. Các field 2-step của V3 vẫn không áp dụng
cho V4/V5/V6. V1/V2/V3 không sử dụng `control_close_style`.

### Giao diện Close sau countdown (V4/V5/V6)

Thêm field này vào placement Remote Config để sau khi hết giờ chỉ còn nút X:

```json
{
  "control_close_style": "close",
  "control_close_ready_style": "compact",
  "count_down_timer": 5
}
```

- `control_close_ready_style: "compact"`: hết countdown thì ẩn vòng progress và nền
  bao quanh. View Close có kích thước **28×28dp**, **padding 4dp**; màu X vẫn lấy từ
  `primary_text_color`. View nằm giữa vùng chạm trong suốt 48×48dp để dễ bấm.
- `"default"` hoặc không khai báo field: giữ giao diện hiện tại, Close 48×48dp,
  padding 12dp, nền và progress theo các field style đang dùng.
- Null, rỗng hoặc giá trị sai fallback `default`; bỏ khoảng trắng và không phân biệt
  chữ hoa/thường. Không thay đổi V1/V2/V3.
- Hoạt động với cả `control_close_style: "countdown"` và `"close"`, cả hai orientation,
  cả `left`/`right`/`shuffle`. Giữ nguyên vị trí đã chọn trong lượt quảng cáo.
- Chưa hết giờ: UI đếm giữ nguyên và không thể đóng sớm. Timer 0 hoặc âm: áp dụng
  compact ngay khi bind. Bind quảng cáo mới khôi phục UI đếm; pause/resume không
  làm mất trạng thái hoặc đặt lại thời gian.

Xem [tab Native Fake Interstitial](firebase-ads-guide/firebase_ads_config_guide.html#native-fake-interstitial),
chọn **Sau countdown → Compact · Close 28dp** để xem config đầy đủ và ảnh trước/sau
countdown trên Pixel 7a; chọn **Hiện tại** để so sánh.

## Preset dark / light cho V4–V6

[native-interstitial-theme-presets.json](native-interstitial-theme-presets.json) chứa
6 placement mẫu, cũng có trong `rewarded_rewardedinter_inter_ad_places` của config dev:
`fullscreen_native_interstitial_v4_dark`, `fullscreen_native_interstitial_v4_light`
và các tên tương ứng cho V5/V6. Mỗi preset dùng cùng template, Close trái, kiểu
`close`, chờ 5 giây. Theme được quyết định hoàn toàn bằng các field màu của placement,
không phụ thuộc dark mode của thiết bị.

| Field | Dark | Light |
| --- | --- | --- |
| `background_color` | `#121212` (V6: `#202329`) | `#FFFFFF` |
| `background_full_color` | `#121212` (V6: `#101216`) | `#FFFFFF` (V6: `#EEF0F4`) |
| `media_background_color` | `#121212` | `#FFFFFF` |
| `primary_text_color`, `progress_bar_tint` | `#FFFFFF` | `#111827` |
| `body_text_color` | `#D1D5DB` | `#4B5563` |
| `background_cta` | `#16A34A` | `#15803D` |
| `cta_text_color` | `#FFFFFF` | `#FFFFFF` |

Để đổi một placement đang dùng trên Firebase, copy các field màu của preset vào
placement đó và giữ nguyên `place_name`/`ad_id`. Không thay toàn bộ danh sách ads bằng
file preset. File mẫu dùng Google test ad ID; thay đổi trong repo không tự publish Firebase.

## Cách gọi từ BaseActivity/BaseFragment

Đăng ký preload trong danh sách interstitial:

```kotlin
override fun providerInterAdPlaceName(): List<IAdPlaceName> = listOf(
    AppAdPlaceName.FULLSCREEN_NATIVE_INTERSTITIAL,
)
```

Hoặc chủ động load khi cần, không cần đăng ký preload cho cùng placement:

```kotlin
loadInterstitialAds(
    AppAdPlaceName.FULLSCREEN_NATIVE_INTERSTITIAL,
    oneTimeLoad = true,
)
```

Khi cần hiển thị:

```kotlin
showInterAd(AppAdPlaceName.FULLSCREEN_NATIVE_INTERSTITIAL) { isShown ->
    // Tiếp tục hành động sau quảng cáo, hoặc khi quảng cáo không thể hiển thị.
}
```

`oneTimeLoad` điều khiển đăng ký preload của màn hình; không thay thế
`is_auto_load_after_dismiss`. Không cần đưa placement này vào
`providerPreloadBannerNativeAdPlaceName()` hay gọi `loadBannerNativeAd()`.

## Gọi trực tiếp AdsManager và chờ load

Ví dụ trong FragmentActivity:

```kotlin
val place = AppAdPlaceName.FULLSCREEN_NATIVE_INTERSTITIAL
adsManager.loadFullscreenAd(this, place, identifier = "native-inter")

adsManager.showAd(
    activity = this,
    fragmentManager = supportFragmentManager,
    adPlaceName = place,
    identifier = "native-inter",
    isWaitLoadToShow = true,
)
```

- Nếu có ad hợp lệ trong cache, manager hiển thị ngay khi đủ điều kiện.
- Nếu chưa có ad và không bật chờ load, trả `AdCompleted(isShown = false)` ngay.
- Bật chờ bằng tham số `isWaitLoadToShow = true` hoặc
  `interstitial_ad_config.is_wait_load_to_show = true`. Manager dùng request đang chạy,
  hoặc bắt đầu request mới nếu chưa có.
- Mỗi request native interstitial có timeout 30 giây. Load thất bại hoặc timeout sẽ
  giải phóng trạng thái chờ và trả completed với `isShown = false`.
- Manager kiểm tra Activity, FragmentManager, điều kiện bật ads, TTL và các giới hạn
  interstitial trước khi show. Nếu không hợp lệ, tiếp tục flow với `isShown = false`.

## Shuffle theme và template qua Remote Config

Có thể bật độc lập hoặc kết hợp cả ba loại shuffle:

```json
{
  "native_template_size": "full_interstitial_shuffle",
  "native_theme": "shuffle",
  "control_close_position": "shuffle"
}
```

Đây là các field bổ sung vào placement, không phải config đầy đủ. Xem
[config đầy đủ và ảnh từng kết quả](firebase-ads-guide/firebase_ads_config_guide.html#native-template-shuffle)
để sao chép cả placement và hai palette.

- `native_template_size: "full_interstitial_shuffle"`: chọn đều V4/V5/V6 (mỗi loại 1/3)
  khi tạo view quảng cáo. Template giữ nguyên trong lượt đó; portrait/landscape dùng XML
  riêng của template được chọn. Lượt sau có thể trùng. Dùng `full_interstitial_v4`,
  `full_interstitial_v5` hoặc `full_interstitial_v6` để cố định layout như trước.
- `native_theme: "shuffle"`: chọn light/dark 50/50 mỗi lần bind; `light` hoặc `dark`
  cố định palette tương ứng. Theme giữ nguyên qua countdown, pause/resume và áp lại
  style trong lượt đó. Thiếu/null/rỗng/sai mode giữ màu gốc. Mode trim, không phân biệt
  hoa/thường; không theo chế độ sáng/tối hệ thống. V1/V2/V3 bỏ qua theme mới.
- `native_theme_colors`: object chứa `light` và `dark`, mỗi object chứa các field màu:
  `background_color`, `background_full_color`, `media_background_color`,
  `primary_text_color`, `body_text_color`, `cta_text_color`, `background_cta`,
  `cta_border_color`, `border_color`, `background_color_ads_notify_view`,
  `text_color_ads_notify_view`, `progress_bar_tint`. Tên palette/field viết đúng chữ thường.
  Giá trị là chuỗi màu Android hợp lệ. Palette ghi đè các field màu cùng tên của placement;
  field thiếu/null/sai màu hoặc thiếu palette giữ màu gốc. Riêng tint progress thiếu/sai
  dùng primary_text_color hợp lệ của palette rồi mới dùng tint gốc.
- Palette không đổi radius, kích thước chữ, timer, ad ID hoặc ảnh/video quảng cáo.
  Không có bảng màu light/dark hardcode trong app. Khi shuffle template, cùng palette
  áp dụng cho template được chọn. Shuffle template/theme/Close dùng các lần random độc lập.

Ví dụ palette tối thiểu (nên dùng bộ đầy đủ trong guide để các màu đồng bộ):

```json
{
  "native_theme": "shuffle",
  "native_theme_colors": {
    "light": { "background_color": "#FFFFFF", "primary_text_color": "#111827", "body_text_color": "#4B5563" },
    "dark": { "background_color": "#202329", "primary_text_color": "#F8FAFC", "body_text_color": "#CBD5E1" }
  }
}
```

Đổi config trên Firebase, fetch/activate trước lượt hiển thị mới. Bộ ảnh kiểm thử dùng
Google test ads và config fixture qua production factory/style/render; không publish Firebase.
Các lượt random thật được lặp để thu đủ hai theme và sáu tổ hợp template × theme,
không có đảm bảo đổi theme/template mỗi lần.

Chạy lại và thu ảnh trên device:

```powershell
.\docs\firebase-ads-guide\capture_native_theme_shuffle.ps1 -Serial <SERIAL> -Adb <ADB_PATH> -Python <PYTHON_PATH>
```

## Vòng đời: mỗi NativeAd chỉ hiển thị một lần

Quy tắc này áp dụng cho **instance NativeAd của type `native_interstitial`**, không phải
giới hạn một lần cho cả placement. Placement có thể show nhiều lần bằng các ad mới.

1. Load thành công: cache ad và phát `AdLoaded` qua `adFullScreenFlow`.
2. Dialog hiển thị: đặt `isShowing = true`, phát `AdSucceedToShow`, chạy countdown.
   **Chưa reload ad mới ở thời điểm vừa show.**
3. Người dùng đóng: `holder.reset()` gọi `destroy()` trên ad đã dùng, xóa cache,
   đặt `isShowing = false` và gỡ loader.
4. Cập nhật số lượt/thời điểm show interstitial và phát `AdDismissed`.
5. Nếu `is_auto_load_after_dismiss = true`, bắt đầu load native mới qua `loadFullscreenAd()`.
6. Phát `AdCompleted(isShown = true, isEarnedReward = false)` để tiếp tục hành động.
   **Không chờ lượt reload hoàn tất.**

Nếu tắt auto reload, bên gọi cần load lại trước lượt show tiếp theo, hoặc bật chờ load.
Kết quả load của type mới đi qua `adFullScreenFlow`, không yêu cầu listener banner/native.

## Dùng tại splash: action_app_open

Trong [config dev](../app/src/dev/res/xml/remote_config_defaults.xml), `action_app_open`
đã dùng `native_interstitial`, native test ID, template V3 và một bước đóng sau 5 giây.
Placement này dành cho các lần mở app từ lần thứ hai; lần đầu dùng
`action_app_open_first_open`.

`splash_screen_config.ad_type` hiện vẫn là `interstitial` để chọn nhánh fullscreen của
splash. Type cụ thể được resolve từ placement `action_app_open`. Không cần đổi API splash.

Config dev hiện bật `is_auto_load_after_dismiss = true`. Với placement chỉ dùng ở splash,
có thể đặt `false` để tránh load thêm khi chuẩn bị rời splash; lần mở app sau sẽ load lại.

## Chuyển từ native fullscreen cũ

1. Đổi `ad_type: "native"` thành `"native_interstitial"` cho placement fullscreen độc lập;
   giữ native ad unit ID và các field giao diện.
2. Chuyển đăng ký preload sang `providerInterAdPlaceName()` hoặc dùng `loadInterstitialAds()`.
3. Giữ `showInterAd()` và nhận kết quả load/show qua luồng fullscreen.

Native inline và cấu hình `native` cũ vẫn được hỗ trợ. Luồng interstitial thật rồi show
native nối tiếp vẫn dùng placement cha `ad_type: "interstitial"`,
`is_show_native_after: true` và `native_config.ad_type: "native"`; không cần đổi sang type mới.

## Kiểm thử

- Unit test: `NativeInterstitialAdPlaceMapperTest` và `NativeInterstitialTest` kiểm tra
  mapping (giữ V1–V6, fallback khi thiếu/sai template và giữ nguyên native thường),
  cache, callback, chờ load, timeout, TTL, giới hạn show và reload sau đóng.
- Device test: `NativeInterstitialTemplateDeviceTest` kiểm tra JSON/mapping fallback
  trên Android. `AdmobDeviceSmokeTest.test05_nativeInterstitialLoadShowClose` dùng
  placement dev `fullscreen_native_interstitial` để kiểm tra SDK thật, countdown,
  Next/Close và callback fullscreen; không phụ thuộc type của `action_app_open`.
- Device test: `AdmobDeviceSmokeTest.test04_actionAppOpenNativeInterstitialLoadShowClose`
  kiểm tra SDK thật, template V3, countdown, nút đóng và callback của `action_app_open`.
  Test yêu cầu APK dev và Google test ad unit; không dùng cho APK production.
  Configure `action_app_open` as `native_interstitial` with a Google native test ad unit
  before running this test; it is skipped when the placement uses the default interstitial type.

```powershell
.\gradlew.bat :core:config:testProdDebugUnitTest :core:ads:testProdDebugUnitTest
.\gradlew.bat :core:config:connectedDevDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.core.config.data.mapper.NativeInterstitialTemplateDeviceTest"
.\gradlew.bat :app:connectedDevDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.codebasetemplate.AdmobDeviceSmokeTest#test05_nativeInterstitialLoadShowClose"
.\gradlew.bat :app:connectedDevDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.codebasetemplate.AdmobDeviceSmokeTest#test04_actionAppOpenNativeInterstitialLoadShowClose"
```

Xem thêm [Firebase Ads configuration guide](firebase-ads-guide/firebase_ads_config_guide.html#fullscreen).
