# Cấu hình Native Collapsible

Native collapsible dùng một native template thông thường làm giao diện thu gọn và hiển thị một
`PopupWindow` riêng khi mở rộng. Vì vậy có thể bật collapsible cho các định dạng native hiện có mà
không cần tạo thêm một native template collapsible tương ứng.

## Các config

| Config | Kiểu | Mặc định | Mô tả |
| --- | --- | --- | --- |
| `is_native_collapsible` | Boolean | `false` | Bật popup mở rộng cho native ad. Đây là config dành riêng cho native, không dùng `is_collapsible` của banner. |
| `native_expand_template` | String | `native_expand_v1` | Chọn giao diện popup mở rộng. Hỗ trợ `native_expand_v1` và `native_expand_v2`. Chỉ có hiệu lực khi `is_native_collapsible` là `true`. |
| `always_hide_inline_native` | Boolean | `false` | Luôn ẩn inline native bằng `GONE` khi `is_native_collapsible=true`, kể cả sau khi thu gọn, refresh hoặc không mở được popup. Popup mở rộng và shimmer loading vẫn hoạt động bình thường. |
| `control_close_position` | String | `right` | Vị trí nút thu gọn. Hỗ trợ `left`/`start` và `right`/`end`. |
| `native_expand_margin_left_dp` | Number | `0` | Khoảng trống bên trái nội dung popup expand, đơn vị dp. Áp dụng cả V1/V2 và nút thu gọn; giá trị âm được đưa về `0`. |
| `native_expand_margin_right_dp` | Number | `0` | Khoảng trống bên phải nội dung popup expand, đơn vị dp. Không ảnh hưởng inline native hoặc shimmer. |
| `collapsible_expand_cooldown_second` | Integer | `0` | Thời gian chờ trước khi native được tự mở rộng lại sau khi người dùng chủ động đóng popup. `0` là không dùng cooldown. |

Nếu `native_expand_template` bị thiếu hoặc có giá trị không hợp lệ, SDK dùng
`native_expand_v1` để tương thích ngược.

Để chỉ hiển thị popup mở rộng, thêm `"always_hide_inline_native": true` vào object placement
trong Firebase Remote Config. Cờ bị thiếu, `null` hoặc `false` giữ hành vi inline hiện tại.
Shimmer vẫn hiển thị trong lúc loading và được thay thế khi ad tải xong; cờ này không giữ
shimmer sau khi đã tải ad.

## Ví dụ Remote Config

Các key dưới đây nằm trong từng object placement của danh sách `banner_native_ad_places`,
không phải các parameter Firebase độc lập. Ví dụ sau giữ inline hiển thị sau khi thu gọn,
với khoảng trống hai bên popup là 16dp:

```json
{
  "place_name": "anchored_bottom_home",
  "ad_id": "ca-app-pub-3940256099942544/2247696110",
  "ad_type": "native",
  "native_template_size": "small_cta_bottom",
  "is_native_collapsible": true,
  "always_hide_inline_native": false,
  "native_expand_template": "native_expand_v2",
  "control_close_position": "left",
  "native_expand_margin_left_dp": 16,
  "native_expand_margin_right_dp": 16,
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
  "always_hide_inline_native": true,
  "native_expand_template": "native_expand_v2",
  "control_close_position": "right",
  "native_expand_margin_left_dp": 16,
  "native_expand_margin_right_dp": 24,
  "collapsible_expand_cooldown_second": 10,
  "is_enable": true
}
```

Activity vẫn ép `is_native_collapsible=true` để tránh cấu hình test vô tình tắt chức năng cần kiểm
tra. Các giá trị template, vị trí nút đóng, cooldown, cờ ẩn inline và margin popup được giữ
nguyên từ Remote Config.

Trong ví dụ này:

- `small_cta_bottom` vẫn chọn template inline và shimmer tương ứng. Với
  `always_hide_inline_native=true`, inline không hiển thị sau khi thu gọn.
- `native_expand_v2` là template của popup mở rộng.
- Popup có khoảng trống trái 16dp, phải 24dp; nút thu gọn nằm cùng vùng nội dung đã thụt vào.
- Đổi `native_expand_template` thành `native_expand_v1` để dùng giao diện V1 mà không thay đổi
  template thu gọn.

## Ẩn inline và chỉnh margin popup

`always_hide_inline_native` chỉ có hiệu lực khi `is_native_collapsible=true`. Khi bật, inline
dùng `GONE`, không chiếm chỗ, kể cả sau khi thu gọn, refresh hoặc không mở được popup. Cờ này
không tự đóng popup đang mở và không ngăn lệnh mở rộng chủ động. Thiếu key, `null` hoặc `false`
sẽ giữ hành vi hiển thị inline hiện tại.

Shimmer vẫn hiển thị khi loading và được thay thế khi ad tải xong. Bật cờ ẩn inline không
làm shimmer biến mất sớm hoặc giữ shimmer sau khi đã tải ad.

Hai key `native_expand_margin_left_dp` và `native_expand_margin_right_dp`:

- Cấu hình độc lập, nhận số nguyên hoặc số thập phân theo dp, ví dụ `12.5`.
- Mỗi bên mặc định `0` khi thiếu hoặc `null`; giá trị âm được đưa về `0`.
- Tạo khoảng trống trong suốt hai bên nội dung popup, áp dụng cho cả V1/V2 và nút thu gọn.
- Luôn chỉ cạnh trái/phải thực tế, không đảo theo ngôn ngữ RTL.
- Không thay đổi margin của inline native hoặc shimmer. Khi bind lại với style mới, popup
  đang hiển thị được cập nhật margin.

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
