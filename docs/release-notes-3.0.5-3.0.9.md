# Release notes 3.0.5–3.0.9

Các phiên bản từ `3.0.5` đến `3.0.9` tập trung vào khả năng tùy biến StartFlow
Onboarding, cấu hình giao diện Native Ads từ Firebase và A/B testing theo từng ad place.

## Nâng cấp

```kotlin
dependencies {
    implementation("com.github.nguyenvuong0308:CodeBase:3.0.9")
}
```

## Điểm nổi bật

- Tùy biến UI Onboarding V1/V2/V3 bằng API typed, không cần truy cập View Binding nội bộ.
- Hỗ trợ thay toàn bộ UI của từng page Onboarding bằng `PageRenderer` trong khi core vẫn quản lý
  navigation, tracking và ads.
- Điều chỉnh kích thước headline, body và CTA của Native Ads qua Firebase Remote Config.
- A/B test từng ad place bằng cơ chế replace toàn bộ object, không yêu cầu thay đổi code phía app.
- Loại bỏ lựa chọn ngôn ngữ Filipino khỏi Language V2.

## Thay đổi theo phiên bản

### 3.0.9 — A/B testing cho ad place

- Thêm parameter `ab_testing_ad_place_names` để khai báo các `place_name` được phép A/B test.
- Mỗi place sử dụng key override theo format `ad_place_ab_<place_name>`.
- Variant hợp lệ thay thế toàn bộ `AdPlaceModel` gốc trước khi map sang domain `AdPlace`; không
  merge từng field.
- Tự động fallback về object gốc khi key không tồn tại, value rỗng, JSON lỗi, thiếu field bắt buộc
  hoặc `place_name` không khớp.
- Chỉ cho phép override place đã tồn tại; không thay đổi thứ tự hoặc số lượng phần tử trong list gốc.
- Giữ nguyên `RemoteConfigRepository`, `AdsManager` và toàn bộ call site phía app.
- Chuyển `BaseActivity.insetsViewModel` từ `private` thành `protected` để activity kế thừa có thể sử
  dụng trạng thái insets.
- Bổ sung unit test và tài liệu cấu hình A/B testing.

Ví dụ:

Giá trị `ab_testing_ad_place_names`:

```json
["anchored_bottom_home"]
```

Giá trị variant của `ad_place_ab_anchored_bottom_home`:

```json
{
  "place_name": "anchored_bottom_home",
  "ad_id": "ca-app-pub-xxx/yyy",
  "ad_type": "native",
  "native_template_size": "medium_cta_bottom",
  "is_enable": true
}
```

Control có thể để chuỗi rỗng. Vì variant replace toàn bộ object, nên copy object gốc rồi chỉnh các
field muốn thử nghiệm.

### 3.0.8 — Custom Onboarding page renderers

- Thêm API tùy biến riêng cho từng version:
  - `OnBoardingV1UiCustomizer`, `OnBoardingV2UiCustomizer`, `OnBoardingV3UiCustomizer` dùng cho
    thay đổi branding an toàn qua `PageState` và `UiSpec` bất biến.
  - `OnBoardingV1PageRenderer`, `OnBoardingV2PageRenderer`, `OnBoardingV3PageRenderer` dùng khi cần
    thay toàn bộ UI của page.
- Hỗ trợ chọn customizer/renderer theo `priority`; renderer có priority cao nhất và thỏa
  `supports(state)` sẽ được sử dụng.
- Core tiếp tục sở hữu navigation, lifecycle, tracking và luồng ads. Renderer V3 có callback để
  chuyển kết quả banner/native ad vào container do app sở hữu.
- Hỗ trợ custom renderer cho cả page chuẩn và `END_TAB` của Onboarding V3.
- `OnBoardingUiCustomizer` cũ vẫn hoạt động như compatibility bridge nhưng các callback đã được
  đánh dấu deprecated.
- Bổ sung Hilt multibinding cho các customizer và renderer mới cùng tài liệu tích hợp chi tiết.

### 3.0.7 — Sửa cách áp dụng text size Native Ads

- Chuẩn hóa `primary_text_size_dp`, `body_text_size_dp` và `cta_text_size_dp` theo hệ responsive
  dimension `@dimen/_Ndp` của project.
- Áp dụng thống nhất cho toàn bộ native template: small, medium, full, interstitial, collapsible,
  Picture-in-Picture và các biến thể CTA/media.
- Giá trị hợp lệ là số nguyên từ `1` đến `35`; thiếu field hoặc ngoài khoảng sẽ giữ kích thước được
  khai báo trong layout.
- Tập trung logic resolve text size trong `BaseNativeTemplateView` để tránh sai khác giữa template.

### 3.0.6 — Cấu hình text size Native Ads từ xa

- Thêm ba field Remote Config cho Native Ads:
  - `primary_text_size_dp`: kích thước headline.
  - `body_text_size_dp`: kích thước body/description.
  - `cta_text_size_dp`: kích thước chữ CTA.
- Truyền các giá trị mới xuyên suốt từ `AdPlaceModel` sang `NativeAdPlace` và phần render template.
- Bổ sung ví dụ vào default config và Firebase Ads configuration guide.

### 3.0.5 — Cập nhật Language V2

- Loại bỏ lựa chọn ngôn ngữ Filipino khỏi màn Language V2 ở cả first-open flow và màn đổi ngôn
  ngữ trong Settings.

## Migration từ 3.0.5 lên 3.0.9

Không có breaking change bắt buộc đối với luồng ads hiện tại. Sau khi nâng dependency, app có thể
tiếp tục dùng API cũ.

Các migration được khuyến nghị:

1. Nếu đang implement `OnBoardingUiCustomizer`, chuyển sang `OnBoardingV1/V2/V3UiCustomizer` cho
   thay đổi style hoặc `OnBoardingV1/V2/V3PageRenderer` cho UI hoàn toàn riêng.
2. Nếu dùng các field text size Native Ads, đảm bảo giá trị nằm trong khoảng `1..35`.
3. Nếu bật A/B testing, cấu hình `ab_testing_ad_place_names` và object variant đầy đủ. Không cần
   thay đổi `AppAdPlaceName` hay call site load/show ads.

## Tài liệu liên quan

- [A/B testing cho Ads](ads-ab-testing-config.md)
- [Firebase Ads configuration guide](firebase-ads-guide/firebase_ads_config_guide.html)
- [Tùy biến StartFlow Onboarding](startflow-onboarding-ui-customization.md)

**Full changelog:** [`3.0.4...3.0.9`](https://github.com/nguyenvuong0308/CodeBase/compare/3.0.4...3.0.9)
