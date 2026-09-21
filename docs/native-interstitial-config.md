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
| `native_template_size` | Hỗ trợ các template `full_interstitial_v1`, `full_interstitial_v2`, `full_interstitial_v3`. |
| `is_auto_load_after_dismiss` | Mặc định `true`. Bắt đầu load ad mới sau khi đóng một ad đã hiển thị. |
| `expired_time_second` | TTL của native cache; nếu thiếu thì dùng `native_ad_config.expired_time_second`. Ad hết hạn không được show. |
| `is_ignore_interval` | Mặc định `false`. Khi bật, bỏ qua kiểm tra interval, số lượt trong session và meaningful actions. |
| `close_step_count` | Với V3: `1` để countdown rồi đóng; `2` để qua bước Next rồi Close. Mặc định V3 là `2`. |
| `count_down_timer` | Số giây countdown; ví dụ trên dùng 5 giây. |
| `step_1_count_down_timer`, `step_2_count_down_timer` | Override thời gian từng bước của V3; khi thiếu, fallback về `count_down_timer`. |

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
  mapping, cache, callback, chờ load, timeout, TTL, giới hạn show và reload sau đóng.
- Device test: `AdmobDeviceSmokeTest.test04_actionAppOpenNativeInterstitialLoadShowClose`
  kiểm tra SDK thật, template V3, countdown, nút đóng và callback của `action_app_open`.
  Test yêu cầu APK dev và Google test ad unit; không dùng cho APK production.

```powershell
.\gradlew.bat :core:config:testProdDebugUnitTest :core:ads:testProdDebugUnitTest
.\gradlew.bat :app:connectedDevDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.codebasetemplate.AdmobDeviceSmokeTest#test04_actionAppOpenNativeInterstitialLoadShowClose"
```

Xem thêm [Firebase Ads configuration guide](firebase-ads-guide/firebase_ads_config_guide.html#fullscreen).
