# Release notes 3.1.6

Phiên bản `3.1.6` tập trung vào tái cấu trúc luồng Splash để app kế thừa `BaseSplashActivity`
có thể tùy biến cách tải quảng cáo dễ hơn.

## Thay đổi

- Đổi `SplashViewModel` trong `core:startflow` thành `BaseSplashViewModel` và cập nhật
  `BaseSplashActivity.baseViewModel` dùng class mới.
- Mở `BaseSplashActivity.fetchSplashAds()` từ `private` thành `open` để app có thể override khi cần
  logic tải quảng cáo Splash riêng.
- Tách logic tải quảng cáo Splash mặc định thành hai hook `fetchAppOpenAd()` và
  `fetchAppOpenAdTypeInterstitial()`.
- Giữ nguyên hành vi mặc định: core vẫn chọn App Open hoặc Interstitial theo `adTypeFirstOpen` và `adType`
  trong Splash remote config.

## Nâng cấp

```kotlin
dependencies {
    implementation("com.github.nguyenvuong0308:CodeBase:3.1.6")
}
```

## Migration

- Nếu app đang tham chiếu trực tiếp `SplashViewModel` từ `core:startflow`, đổi sang `BaseSplashViewModel`.
- Nếu app chỉ kế thừa `BaseSplashActivity` và không override luồng tải ads, không cần thay đổi code.

**Commit hôm nay:** [`92f3938`](https://github.com/nguyenvuong0308/CodeBase/commit/92f3938f2c93e0ce33489821d1de9efd67814aa6)
