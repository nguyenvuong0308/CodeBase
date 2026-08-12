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

## Thiết lập experiment

Giữ `ab_testing_ad_place_names` là parameter chung. Chọn `ad_place_ab_<place_name>` làm parameter
của Firebase experiment: control dùng chuỗi rỗng, còn variant dùng JSON object đầy đủ. Vì object
được replace toàn bộ, nên copy object gốc rồi chỉnh các field muốn test để tránh vô tình mất config.
