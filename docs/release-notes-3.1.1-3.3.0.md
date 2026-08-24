# Release notes 3.1.1-3.3.0

Tài liệu này tổng hợp các thay đổi từ `3.1.1` đến `3.3.0`, ưu tiên những điểm ảnh hưởng đến app tích hợp core library.

## Nâng cấp

```kotlin
dependencies {
    implementation("com.github.nguyenvuong0308:CodeBase:3.3.0")
}
```

## Điểm nổi bật

- Tối ưu asset mặc định của StartFlow Onboarding, giảm dung lượng ảnh onboarding nhưng giữ nguyên resource ID.
- Mở rộng Splash flow để app kế thừa `BaseSplashActivity` có thể tùy biến cách tải quảng cáo.
- Làm mới Language V2, hỗ trợ nút Apply dạng text, màn applying state, ảnh nền tùy biến và preload native ads đúng version Language.
- Bổ sung cơ chế giới hạn interstitial theo số meaningful action giữa hai lần hiện quảng cáo.
- Thêm native template `small_banner_cta_right`, preview tooling và ảnh minh họa cho Firebase Ads guide.
- Sửa các lỗi ổn định: ANR khi khởi tạo app, crash FCM notification intent, share file lỗi URI, black screen khi đổi ngôn ngữ.

## Thay đổi theo phiên bản

### 3.3.0 - Stability, preload và cấu hình nền language

- Sửa ANR khi khởi tạo app bằng cách bỏ việc inject `AppPreferences` trực tiếp trong `BaseAdmobApplication.onCreate()`.
- `AppPreferences.systemLanguageCode` tự fallback về `Locale.getDefault().language` khi chưa có giá trị lưu sẵn.
- Bật `ReOpenShowCondition` qua `dagger.Lazy` trong app mẫu để tránh khởi tạo sớm các dependency nặng.
- Bắt `SecurityException` trong `MyFirebaseMessagingService.handleIntent()` khi Firebase xử lý notification intent và log analytics `FCM_AUTO_NOTIFY_SECURITY`.
- `shareFiles()` bỏ qua file không tồn tại/không đọc được, cấp `FLAG_GRANT_READ_URI_PERMISSION` và không mở share sheet khi không có URI hợp lệ.
- Splash preload native ads cho Language V2 bằng các placement `ANCHOR_CHANGE_LANGUAGE_V2_NATIVE_1`, `_2`, `_3`; Language V1 vẫn dùng `ANCHOR_CHANGE_LANGUAGE_BOTTOM`.
- Thêm cấu hình ảnh nền cho Language V1/V2:
  - `startflow_language_show_image_background`
  - `startflow_language_image_background`
- Dọn margin thừa trong các layout native/shimmer để template hiển thị sát spec hơn.

### 3.2.0 - Language V2 và Ads controls

- Language V2 có thể đổi nút Apply từ icon sang text bằng `startflow_language_v2_apply_use_text`; text lấy từ `startflow_language_v2_apply_text_value`.
- Bổ sung applying overlay khi đổi ngôn ngữ, ảnh minh họa `start_flow_language_wait`, màu bubble/loading và text đa ngôn ngữ.
- Sửa vị trí list Language V2, sửa black screen khi đổi ngôn ngữ từ setting và thêm manifest test cho `LanguageActivityV2FromSetting`.
- Thêm capping interstitial theo meaningful action:
  - Remote Config field: `meaningful_actions_between_interstitial`.
  - Sau khi interstitial hiển thị, core reset bộ đếm action và chỉ cho phép hiện lần tiếp theo khi đủ số action.
  - `isIgnoreInterval` vẫn bỏ qua rule interval/capping cho placement cần hiện bắt buộc.
- Sửa Native/Banner loading state khi load quảng cáo lỗi, tránh giữ loading UI sau khi hết retry.
- Thêm native template `small_banner_cta_right`, `NativeSmallBannerCtaRightTemplateView`, mapping trong `NativeTemplateSize` và default config mẫu.
- Thêm preview tooling cho native gallery ở flavor `dev`, ảnh minh họa `small_banner_cta_right` và `native_pip`.
- Thêm màn test collapsible native có config/controls riêng để kiểm tra expanded/collapsed state.
- Mở hook setup dialog cho `BaseDialogFragment`, `BaseAdsDialogFragment` và `BaseAdsBottomSheetDialogFragment`.
- Cập nhật Firebase Ads guide và thêm bản HTML step-by-step cho StartFlow Onboarding customization.

### 3.1.6 - Splash flow hooks

- Đổi `SplashViewModel` trong `core:startflow` thành `BaseSplashViewModel`.
- `BaseSplashActivity.baseViewModel` dùng class mới để app kế thừa có điểm override rõ hơn.
- Mở `BaseSplashActivity.fetchSplashAds()` để app override khi cần logic tải ads Splash riêng.
- Tách logic tải ads Splash mặc định thành `fetchAppOpenAd()` và `fetchAppOpenAdTypeInterstitial()`.
- Giữ nguyên behavior mặc định: core vẫn chọn App Open hoặc Interstitial theo `adTypeFirstOpen` và `adType` trong Splash remote config.

### 3.1.1 - Tối ưu asset StartFlow Onboarding

- Chuyển ba ảnh onboarding mặc định `intro_11`, `intro_21`, `intro_31` từ PNG sang WebP.
- Giảm tổng dung lượng ảnh từ khoảng 3.12 MB xuống 34 KB.
- Giữ nguyên resource ID nên app đang dùng `R.drawable.intro_11`, `intro_21`, `intro_31` không cần đổi code.
- Không thay đổi API hoặc Remote Config contract.

## Migration

- Nếu app đang tham chiếu trực tiếp `SplashViewModel`, đổi sang `BaseSplashViewModel`.
- Nếu muốn nút Apply Language V2 dạng text, override `startflow_language_v2_apply_use_text=true` và `startflow_language_v2_apply_text_value`.
- Nếu muốn ảnh nền Language, override `startflow_language_show_image_background=true` và `startflow_language_image_background`.
- Nếu dùng interstitial capping, khai báo `meaningful_actions_between_interstitial` trong `interstitial_ad_config`; giá trị `0` giữ hành vi cũ.
- Nếu cần native dạng banner nhỏ, dùng `native_template_size: "small_banner_cta_right"`.

## Tài liệu liên quan

- [Release notes 3.1.6](release-notes-3.1.6.md)
- [Release notes 3.1.1](release-notes-3.1.1.md)
- [Firebase Ads configuration guide](firebase-ads-guide/firebase_ads_config_guide.html)
- [Native collapsible remote config](native-collapsible-config.md)
- [StartFlow Language UI customization](startflow-language-ui-customization.md)
- [StartFlow Onboarding UI customization](startflow-onboarding-ui-customization.md)

**Full changelog:** [`3.1.1...3.3.0`](https://github.com/nguyenvuong0308/CodeBase/compare/3.1.1...3.3.0)