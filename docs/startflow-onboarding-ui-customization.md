# Custom UI màn Onboarding của StartFlow

Module `core:startflow` giữ logic điều hướng, quảng cáo và tracking của onboarding. App có thể thay nội dung và giao diện mà không cần copy hoặc sửa trực tiếp code trong module core thông qua hai extension point:

| API | Dùng để làm gì | Cách hoạt động |
| --- | --- | --- |
| `OnBoardingContentProvider` | Cung cấp số trang, ảnh, title và subtitle | StartFlow chọn một provider; nếu app không đăng ký thì dùng `DefaultOnBoardingContentProvider` |
| `OnBoardingUiCustomizer` | Chỉnh màu, font, text, visibility, background hoặc thuộc tính view theo từng version | StartFlow gọi tất cả customizer đã đăng ký sau khi UI mặc định được bind |

Remote Config `onboarding_config.version` quyết định callback được gọi:

| `version` | Callback |
| --- | --- |
| `1` | `customizeOnBoardingV1(...)` |
| `2` | `customizeOnBoardingV2(...)` |
| `3` | `customizeOnBoardingV3(...)`; trang cuối không có ads dùng `customizeOnBoardingV3EndTab(...)` |

## 1. Custom nội dung onboarding

Tạo provider trong module app:

```kotlin
package com.example.app.startflow

import com.example.app.R
import com.core.startflow.onboarding.OnBoardingContentProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppOnBoardingContentProvider @Inject constructor() : OnBoardingContentProvider {
    override val introPageCount: Int = 3

    override fun getImageResIntro(position: Int): Int = when (position) {
        0 -> R.drawable.intro_1
        1 -> R.drawable.intro_2
        else -> R.drawable.intro_3
    }

    override fun getStringIntro(position: Int): Int = when (position) {
        0 -> R.string.onboarding_title_1
        1 -> R.string.onboarding_title_2
        else -> R.string.onboarding_title_3
    }

    override fun getSubtitleIntro(position: Int): Int? = when (position) {
        0 -> R.string.onboarding_subtitle_1
        1 -> R.string.onboarding_subtitle_2
        2 -> R.string.onboarding_subtitle_3
        else -> null
    }
}
```

`getSubtitleIntro()` hiện được UI V2 sử dụng. Trả về `null` nếu muốn ẩn subtitle. V1 và V3 lấy ảnh/title từ cùng provider nhưng không hiển thị subtitle mặc định.

Nên đăng ký đúng một `OnBoardingContentProvider`. StartFlow lấy phần tử đầu tiên trong `Set`; nếu có nhiều provider thì thứ tự lựa chọn không được đảm bảo.

## 2. Custom giao diện theo version

Tạo một class implement `OnBoardingUiCustomizer`. Chỉ cần override callback của version app đang sử dụng; các hàm đều có implementation mặc định rỗng.

```kotlin
package com.example.app.startflow

import android.graphics.Color
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.app.R
import com.core.config.domain.data.OnBoardingConfig
import com.core.startflow.databinding.CoreFragmentOnboardingBinding
import com.core.startflow.databinding.CoreFragmentOnboardingV2Binding
import com.core.startflow.databinding.FragmentOnboardingV3Binding
import com.core.startflow.databinding.FragmentOnboardingV3EndTabBinding
import com.core.startflow.onboarding.OnBoardingUiCustomizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppOnBoardingUiCustomizer @Inject constructor() : OnBoardingUiCustomizer {

    override fun customizeOnBoardingV1(
        fragment: Fragment,
        binding: CoreFragmentOnboardingBinding,
        position: Int,
        isLastPage: Boolean,
    ) {
        val context = fragment.requireContext()
        binding.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.onboarding_text))
        binding.tvNext.setTextColor(ContextCompat.getColor(context, R.color.onboarding_primary))
        binding.tvNext.text = fragment.getString(
            if (isLastPage) R.string.onboarding_start else R.string.onboarding_next
        )
    }

    override fun customizeOnBoardingV2(
        fragment: Fragment,
        binding: CoreFragmentOnboardingV2Binding,
        position: Int,
        isLastPage: Boolean,
    ) {
        val context = fragment.requireContext()
        binding.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.onboarding_text))
        binding.tvTitle2.setTextColor(ContextCompat.getColor(context, R.color.onboarding_subtitle))
        binding.layoutContent.setBackgroundResource(R.drawable.bg_onboarding_content)

        if (isLastPage) {
            binding.tvNext.setFillGradientEnabled(false)
            binding.tvNext.setBackgroundResource(R.drawable.bg_onboarding_primary_button)
        }
    }

    override fun customizeOnBoardingV3(
        fragment: Fragment,
        binding: FragmentOnboardingV3Binding,
        introductionPosition: Int,
        realPosition: Int,
        isPageEnd: Boolean,
        isShowAd: Boolean,
        onBoardingConfig: OnBoardingConfig,
    ) {
        val context = fragment.requireContext()
        val primaryColor = ContextCompat.getColor(context, R.color.onboarding_primary)

        binding.tvTitle.setTextColor(Color.WHITE)
        binding.tvNextTop.setTextColor(primaryColor)
        binding.tvNextTopV1.setTextColor(primaryColor)
        binding.tvNextBottom.setTextColor(primaryColor)

        if (isPageEnd) {
            binding.tvNextTop.setText(R.string.onboarding_start)
            binding.tvNextTopV1.setText(R.string.onboarding_start)
            binding.tvNextBottom.setText(R.string.onboarding_start)
        }

        // Có thể custom riêng page có ads hoặc theo vị trí action từ Remote Config.
        binding.frameAds.setBackgroundColor(
            if (isShowAd) Color.WHITE else Color.TRANSPARENT
        )
    }

    override fun customizeOnBoardingV3EndTab(
        fragment: Fragment,
        binding: FragmentOnboardingV3EndTabBinding,
        position: Int,
    ) {
        binding.btGetStart.setBackgroundResource(R.drawable.bg_onboarding_primary_button)
        binding.btGetStart.setText(R.string.onboarding_start)
    }
}
```

Customizer được gọi ở cuối `Fragment.initViews()`, sau khi StartFlow đã gán nội dung, trạng thái ads, vị trí action và listener điều hướng mặc định. Vì vậy giá trị gán trong customizer sẽ ghi đè phần UI mặc định.

## 3. Đăng ký với Hilt

Đăng ký provider và customizer bằng multibinding `@IntoSet` trong module app:

```kotlin
import com.core.startflow.onboarding.OnBoardingContentProvider
import com.core.startflow.onboarding.OnBoardingUiCustomizer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StartFlowCustomizationModule {

    @Provides
    @IntoSet
    @Singleton
    fun provideOnBoardingContentProvider(
        provider: AppOnBoardingContentProvider,
    ): OnBoardingContentProvider = provider

    @Provides
    @IntoSet
    @Singleton
    fun provideOnBoardingUiCustomizer(
        customizer: AppOnBoardingUiCustomizer,
    ): OnBoardingUiCustomizer = customizer
}
```

Module `core:startflow` đã khai báo `@Multibinds`, vì vậy app không cần sửa module core. Project mẫu hiện đăng ký hai implementation này trong `app/src/main/java/com/codebasetemplate/required/RequiredModule.kt`.

## Ý nghĩa tham số callback

| Tham số | Ý nghĩa |
| --- | --- |
| `fragment` | Fragment đang hiển thị; dùng `requireContext()`, `getString()` hoặc truy cập lifecycle |
| `binding` | View Binding đúng với layout của version hiện tại |
| `position` | Vị trí content, bắt đầu từ `0` |
| `isLastPage` | Trang content cuối của V1/V2 theo `introPageCount` |
| `introductionPosition` | Vị trí logic trong flow V3; có tính đến vị trí dành cho full-native ad |
| `realPosition` | Vị trí content thực dùng để lấy ảnh/title trong V3 |
| `isPageEnd` | Đây là trang content cuối có thể hoàn tất onboarding |
| `isShowAd` | Trang V3 hiện tại có vùng banner/native ad hay không |
| `onBoardingConfig` | Config đã parse, gồm `version`, `positionNext`, close/swipe và delay |

## Các view thường custom

| Version | View Binding | View chính |
| --- | --- | --- |
| V1 | `CoreFragmentOnboardingBinding` | `ivIntroduction`, `tvTitle`, `dotsIndicator`, `tvNext`, `layoutIndicator` |
| V2 | `CoreFragmentOnboardingV2Binding` | `ivIntroduction`, `layoutContent`, `tvTitle`, `tvTitle2`, `tvNext` |
| V3 | `FragmentOnboardingV3Binding` | `ivIntroduction`, `tvTitle`, `topNext`, `topNextV2`, `bottomNext`, các indicator, `frameAds`, `layoutBannerNative` |
| V3 end-tab | `FragmentOnboardingV3EndTabBinding` | `ivIntroduction`, `tvTitle`, `btGetStart`, `layoutBannerNative` |

## Đồng bộ với Remote Config

Số content page trong provider phải khớp với dữ liệu onboarding đang dùng:

- V1 dùng `application_config.intro_data`.
- V2 dùng `application_config.intro_data_v2`.
- V3 dùng `application_config.intro_data_v3`.
- Nếu mảng tương ứng rỗng, StartFlow tạo danh sách mặc định theo `introPageCount`.
- Giá trị `0` là content không ads, `1` là content có banner/native và `2` là full-native ad.

Ví dụ chọn V3, action ở dưới và ba content page:

```json
{
  "onboarding_config": {
    "version": 3,
    "position_next": "bottom",
    "is_show_close": true,
    "is_show_swipe": true,
    "delay_show_close_swipe_seconds": 1
  },
  "application_config": {
    "intro_data_v3": [0, 1, 0]
  }
}
```

## Lưu ý

- Không giữ `binding` hoặc `fragment` trong singleton customizer; chúng chỉ hợp lệ theo vòng đời view hiện tại.
- Không thay click listener của nút Next/Get Started nếu chỉ đổi giao diện. Listener mặc định còn phụ trách chuyển trang, kết thúc flow, tracking và quảng cáo.
- Nếu đăng ký nhiều `OnBoardingUiCustomizer`, tất cả đều được gọi và thứ tự của `Set` không được đảm bảo. Tránh để nhiều customizer cùng sửa một thuộc tính.
- Dùng resource trong module app cho ảnh, màu, string và drawable để từng app có theme riêng mà không thay đổi `core:startflow`.
- Khi đổi `introPageCount`, cập nhật đủ ảnh/string và các mảng `intro_data*` tương ứng để tránh index ngoài phạm vi hoặc xác định sai trang cuối.
