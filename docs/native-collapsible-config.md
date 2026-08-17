# Cấu hình Native Collapsible

Native collapsible dùng một native template thông thường làm giao diện thu gọn và hiển thị một
`PopupWindow` riêng khi mở rộng. Vì vậy có thể bật collapsible cho các định dạng native hiện có mà
không cần tạo thêm một native template collapsible tương ứng.

## Các config

| Config | Kiểu | Mặc định | Mô tả |
| --- | --- | --- | --- |
| `is_native_collapsible` | Boolean | `false` | Bật popup mở rộng cho native ad. Đây là config dành riêng cho native, không dùng `is_collapsible` của banner. |
| `native_expand_template` | String | `native_expand_v1` | Chọn giao diện popup mở rộng. Hỗ trợ `native_expand_v1` và `native_expand_v2`. Chỉ có hiệu lực khi `is_native_collapsible` là `true`. |
| `control_close_position` | String | `right` | Vị trí nút thu gọn. Hỗ trợ `left`/`start` và `right`/`end`. |
| `collapsible_expand_cooldown_second` | Integer | `0` | Thời gian chờ trước khi native được tự mở rộng lại sau khi người dùng chủ động đóng popup. `0` là không dùng cooldown. |

Nếu `native_expand_template` bị thiếu hoặc có giá trị không hợp lệ, SDK dùng
`native_expand_v1` để tương thích ngược.

## Ví dụ Remote Config

```json
{
  "place_name": "anchored_bottom_home",
  "ad_id": "ca-app-pub-3940256099942544/2247696110",
  "ad_type": "native",
  "native_template_size": "small_cta_bottom",
  "is_native_collapsible": true,
  "native_expand_template": "native_expand_v2",
  "control_close_position": "left",
  "collapsible_expand_cooldown_second": 10,
  "is_enable": true
}
```

Màn `Native Collapsible Test` trong app dùng ad place riêng
`anchored_native_collapsible_test`. Thêm object sau vào danh sách `banner_native_ad_places` trên
Firebase Remote Config để chạy màn test độc lập:

```json
{
  "place_name": "anchored_native_collapsible_test",
  "ad_id": "ca-app-pub-3940256099942544/2247696110",
  "ad_type": "native",
  "native_template_size": "small_cta_bottom",
  "is_native_collapsible": true,
  "native_expand_template": "native_expand_v2",
  "control_close_position": "right",
  "collapsible_expand_cooldown_second": 10,
  "is_enable": true
}
```

Activity vẫn ép `is_native_collapsible=true` để tránh cấu hình test vô tình tắt chức năng cần kiểm
tra. Các giá trị template, vị trí nút đóng và cooldown được giữ nguyên từ Remote Config.

Trong ví dụ này:

- `small_cta_bottom` là template hiển thị sau khi thu gọn.
- `native_expand_v2` là template của popup mở rộng.
- Đổi `native_expand_template` thành `native_expand_v1` để dùng giao diện V1 mà không thay đổi
  template thu gọn.

## Quy tắc lựa chọn giao diện

| `native_template_size` | `is_native_collapsible` | `native_expand_template` | Kết quả |
| --- | --- | --- | --- |
| Template thông thường | `false` hoặc không khai báo | Bất kỳ | Chỉ hiển thị native template thông thường. |
| Template thông thường | `true` | `native_expand_v1` | Template thông thường được bọc bởi host collapsible; popup dùng `NativeExpandView`. |
| Template thông thường | `true` | `native_expand_v2` | Template thông thường được bọc bởi host collapsible; popup dùng `NativeExpandViewV2`. |
| Template thông thường | `true` | Thiếu hoặc không hợp lệ | Fallback về `NativeExpandView` V1. |

Các giá trị cũ `medium_collapsible_cta_bottom` và `medium_collapsible_banner` đã bị loại bỏ. Cần đổi
sang một `native_template_size` thông thường kết hợp với `is_native_collapsible=true`.

## Lưu ý vòng đời

- Popup bị đóng khi host bị detach khỏi cửa sổ, ví dụ khi chuyển sang màn hình khác. Việc này tránh
  `WindowLeaked` và popup giữ token của Activity cũ.
- Cooldown chỉ được ghi nhận khi người dùng bấm nút thu gọn. Popup bị đóng do vòng đời không kích
  hoạt cooldown.
- Native ad chỉ được bind với một `NativeAdView` tại một thời điểm: popup được huỷ trước khi ad được
  bind lại vào template thu gọn.
