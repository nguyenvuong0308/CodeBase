# Release notes 3.1.1

Phiên bản `3.1.1` tối ưu dung lượng asset mặc định của StartFlow Onboarding.

## Thay đổi

- Chuyển ba ảnh onboarding mặc định `intro_11`, `intro_21` và `intro_31` từ PNG sang WebP.
- Giảm tổng dung lượng các ảnh từ khoảng 3,12 MB xuống 34 KB, tương đương giảm khoảng 98,9%.
- Giữ nguyên resource ID nên code sử dụng `R.drawable.intro_11`, `intro_21` và `intro_31` không cần
  thay đổi.
- Không thay đổi API hoặc Remote Config contract.

## Nâng cấp

```kotlin
dependencies {
    implementation("com.github.nguyenvuong0308:CodeBase:3.1.1")
}
```

**Full changelog:** [`3.1.0...3.1.1`](https://github.com/nguyenvuong0308/CodeBase/compare/3.1.0...3.1.1)
