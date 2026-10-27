# Cấu hình A/B testing cho Ads

Cơ chế A/B testing thay thế toàn bộ object trên các list ad place hiện tại. Code phía app tiếp tục
dùng các API `RemoteConfigRepository` và `AdsManager` hiện có, không cần thay đổi implementation.

## Các parameter Remote Config

Tạo `ab_testing_ad_place_names` là JSON array chứa các placement hiện có được phép nhận override:

```json
[
  "anchored_bottom_home",
  "fullscreen_test"
]
```

Với mỗi tên, tạo một parameter Remote Config theo quy ước:

```text
ad_place_ab_<place_name>
```

Ví dụ, key override của `anchored_bottom_home` là `ad_place_ab_anchored_bottom_home`.

Giá trị control có thể để rỗng. Variant phải chứa object ad place đầy đủ, tối thiểu gồm
`place_name`, `ad_id`, `ad_type` và `is_enable`:

```json
{
  "place_name": "anchored_bottom_home",
  "ad_id": "ca-app-pub-xxx/yyy",
  "ad_type": "native",
  "native_template_size": "medium_cta_bottom",
  "is_enable": true,
  "is_disable_for_low_device": true,
  "background_color": "#FFFFFF",
  "background_cta": "#5E56F5"
}
```

## Quy tắc resolve

1. Core resolve các list ad place versioned/fallback hiện có trước.
2. Danh sách tên được trim, loại trùng và giới hạn ở các placement đã có trong list gốc.
3. Override khác rỗng được parse và validate.
4. Override hợp lệ thay thế toàn bộ object gốc; các field không được merge.
5. Override không tồn tại, rỗng, sai JSON, thiếu field hoặc sai `place_name` sẽ bị bỏ qua.
6. Override không thể thêm `place_name` mới.

Do replace diễn ra trước khi map sang domain `AdPlace`, các call site phía app không cần thay đổi.

## Low device theo từng placement

`is_disable_for_low_device` nằm trong object có `place_name`, không đặt ở config cấp ad type.
Chỉ placement có cờ `true` bị chặn khi app đã set `AdmobAdsInitializer.isLowDevice = true`
trên instance được inject, trước luồng tải/hiển thị ads. Biến ở app mặc định `false`;
cờ placement thiếu hoặc `null` cũng mặc định `false`.

Hai placement cùng loại hoặc cùng ad unit có thể có cờ khác nhau. `native_config` lồng
trong interstitial có cờ riêng. Vì A/B thay toàn bộ object, variant phải giữ
`is_disable_for_low_device: true` nếu muốn tiếp tục chặn placement đó trên low device;
bỏ field sẽ trả về `false`, không kế thừa giá trị từ control.

Xem [hướng dẫn setup app và log low device](firebase-ads-guide/firebase_ads_config_guide.html#low-device-ads).

## Thiết lập experiment

Giữ `ab_testing_ad_place_names` là parameter chung. Chọn `ad_place_ab_<place_name>` làm parameter
của Firebase experiment: control dùng chuỗi rỗng, còn variant dùng JSON object đầy đủ. Vì object
được replace toàn bộ, nên copy object gốc rồi chỉnh các field muốn test để tránh vô tình mất config.
