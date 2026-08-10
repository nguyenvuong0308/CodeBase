# Custom giao diện Language V1/V2 của StartFlow

Module `core:startflow` cung cấp sẵn giao diện và màu mặc định. App có thể đổi toàn bộ palette của màn Language V1/V2 bằng cách sửa duy nhất file:

`app/src/main/res/values/startflow_language_colors.xml`

Không sửa `core/startflow`, không cần tạo customizer bằng Kotlin và không cần copy layout. Resource trong module `app` có độ ưu tiên cao hơn resource cùng tên trong library nên màu của app sẽ được áp dụng tự động cho mọi nơi mở màn Language, bao gồm Splash và Settings.

## Cấu hình Language V1

| Resource | Thành phần |
| --- | --- |
| `startflow_language_v1_background` | Nền toàn màn hình |
| `startflow_language_v1_toolbar_title` | Màu tiêu đề toolbar |
| `startflow_language_v1_toolbar_action` | Màu chữ action Save |
| `startflow_language_v1_loading_indicator` | Màu vòng loading |
| `startflow_language_v1_loading_text` | Màu chữ khi đang áp dụng ngôn ngữ |
| `startflow_language_v1_ad_background` | Nền vùng quảng cáo |
| `startflow_language_v1_item_selected_background` | Nền item đang chọn |
| `startflow_language_v1_item_unselected_background` | Nền item chưa chọn |
| `startflow_language_v1_item_selected_border` | Viền item đang chọn |
| `startflow_language_v1_item_selected_text` | Chữ item đang chọn |
| `startflow_language_v1_item_unselected_text` | Chữ item chưa chọn |
| `startflow_language_v1_radio_selected` | Radio đang chọn |
| `startflow_language_v1_radio_unselected` | Radio chưa chọn |

## Cấu hình Language V2

| Resource | Thành phần |
| --- | --- |
| `startflow_language_v2_background` | Nền toàn màn hình |
| `startflow_language_v2_back_icon` | Icon Back |
| `startflow_language_v2_title` | Tiêu đề màn hình |
| `startflow_language_v2_apply_icon` | Icon xác nhận/Apply |
| `startflow_language_v2_loading_indicator` | Màu vòng loading |
| `startflow_language_v2_loading_text` | Màu chữ khi đang áp dụng ngôn ngữ |
| `startflow_language_v2_ad_background` | Nền vùng quảng cáo |
| `startflow_language_v2_group_background` | Nền item nhóm ngôn ngữ |
| `startflow_language_v2_group_title` | Tiêu đề nhóm |
| `startflow_language_v2_group_subtitle` | Tên bản địa của nhóm |
| `startflow_language_v2_flag_background` | Nền tròn phía sau emoji cờ |
| `startflow_language_v2_expand_icon` | Icon đóng/mở nhóm |
| `startflow_language_v2_branch` | Đường nối từ nhóm tới các lựa chọn |
| `startflow_language_v2_item_background` | Nền lựa chọn chưa chọn |
| `startflow_language_v2_item_selected_background` | Nền lựa chọn đang chọn |
| `startflow_language_v2_item_text` | Chữ lựa chọn chưa chọn |
| `startflow_language_v2_item_selected_text` | Chữ lựa chọn đang chọn |
| `startflow_language_v2_radio_unselected` | Radio chưa chọn |
| `startflow_language_v2_radio_selected` | Radio đang chọn |

## Ví dụ đổi theo màu thương hiệu

Chỉ thay giá trị, giữ nguyên tên resource:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="startflow_language_v1_background">#FFF8F5</color>
    <color name="startflow_language_v1_item_selected_border">#FF6B35</color>
    <color name="startflow_language_v1_item_selected_text">#FF6B35</color>
    <color name="startflow_language_v1_radio_selected">#FF6B35</color>

    <color name="startflow_language_v2_background">#FFF8F5</color>
    <color name="startflow_language_v2_apply_icon">#FF6B35</color>
    <color name="startflow_language_v2_item_selected_background">#FF6B35</color>
    <color name="startflow_language_v2_loading_indicator">#FF6B35</color>
</resources>
```

File thật của app phải giữ đủ các resource V1/V2 đang có; đoạn trên chỉ minh họa những màu thường cần đổi.

## Dark mode

Nếu app cần palette riêng cho dark mode, tạo file:

`app/src/main/res/values-night/startflow_language_colors.xml`

Khai báo lại cùng tên resource với giá trị dành cho dark mode. Android sẽ tự chọn file phù hợp theo giao diện hệ thống.

## Khi nào phải sửa layout?

Bộ resource trên dành cho thay đổi màu. Chỉ override layout/drawable khi cần thay đổi cấu trúc, kích thước, bo góc hoặc icon. Nếu chỉ đổi màu, luôn ưu tiên sửa `startflow_language_colors.xml` để tránh lệch layout khi module `core:startflow` được cập nhật.
