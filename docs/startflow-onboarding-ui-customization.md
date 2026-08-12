# Custom UI màn Onboarding của StartFlow

Module `core:startflow` giữ logic điều hướng, quảng cáo và tracking của onboarding. App có thể thay nội dung và giao diện mà không cần copy hoặc sửa trực tiếp code trong module core thông qua các extension point:

| API | Dùng để làm gì | Cách hoạt động |
| --- | --- | --- |
| `OnBoardingContentProvider` | Cung cấp số trang, ảnh, title và subtitle | StartFlow chọn một provider; nếu app không đăng ký thì dùng `DefaultOnBoardingContentProvider` |
| `OnBoardingV1/V2/V3UiCustomizer` | Đổi branding bằng `state`/`UiSpec` typed, không truy cập View Binding của core | Các customizer chạy theo `priority` từ thấp đến cao |
| `OnBoardingV1/V2/V3PageRenderer` | Thay toàn bộ UI của một số hoặc tất cả page | Renderer có priority cao nhất thỏa `supports(state)` được chọn; core vẫn giữ navigation và tracking |
| `OnBoardingUiCustomizer` | API View Binding cũ | Callback của cả V1/V2/V3 đã deprecated và chỉ còn là bridge tương thích |

Remote Config `onboarding_config.version` quyết định callback được gọi:

| `version` | Callback |
| --- | --- |
| `1` | `OnBoardingV1UiCustomizer`; UI riêng dùng `OnBoardingV1PageRenderer` |
| `2` | `OnBoardingV2UiCustomizer`; UI riêng dùng `OnBoardingV2PageRenderer` |
| `3` | `OnBoardingV3UiCustomizer`; nếu cần UI hoàn toàn riêng dùng `OnBoardingV3PageRenderer` |

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

`getSubtitleIntro()` được UI V2 và card `top_v2` của V3 sử dụng. Trả về `null` nếu muốn ẩn subtitle.

Nên đăng ký đúng một `OnBoardingContentProvider`. StartFlow lấy phần tử đầu tiên trong `Set`; nếu có nhiều provider thì thứ tự lựa chọn không được đảm bảo.

## 2. Custom branding V1/V2/V3 bằng UiSpec

Đây là API nên dùng cho phần lớn app. App nhận state bất biến và trả về một bản `UiSpec` mới; core vẫn sở hữu layout, click listener, ads và tracking. Mỗi version có contract riêng để chỉ expose đúng khả năng của layout đó:

| Version | State | UiSpec | Customizer |
| --- | --- | --- | --- |
| V1 | `OnBoardingV1PageState` | `OnBoardingV1UiSpec` | `OnBoardingV1UiCustomizer` |
| V2 | `OnBoardingV2PageState` | `OnBoardingV2UiSpec` | `OnBoardingV2UiCustomizer` |
| V3 | `OnBoardingV3PageState` | `OnBoardingV3UiSpec` | `OnBoardingV3UiCustomizer` |

Ví dụ V1:

```kotlin
class AppOnBoardingV1UiCustomizer @Inject constructor() : OnBoardingV1UiCustomizer {
    override fun customize(
        context: Context,
        state: OnBoardingV1PageState,
        current: OnBoardingV1UiSpec,
    ) = current.copy(
        actionTextColor = ContextCompat.getColor(context, R.color.onboarding_primary),
        actionBackgroundRes = R.drawable.bg_onboarding_action,
    )
}
```

Ví dụ V2; đặt `isActionFillGradientEnabled = false` nếu app muốn dùng drawable CTA riêng:

```kotlin
class AppOnBoardingV2UiCustomizer @Inject constructor() : OnBoardingV2UiCustomizer {
    override fun customize(
        context: Context,
        state: OnBoardingV2PageState,
        current: OnBoardingV2UiSpec,
    ) = current.copy(
        contentBackgroundRes = R.drawable.bg_onboarding_content,
        actionBackgroundRes = R.drawable.bg_onboarding_action,
        isActionFillGradientEnabled = false,
    )
}
```

Ví dụ V3:

```kotlin
@Singleton
class AppOnBoardingV3UiCustomizer @Inject constructor() : OnBoardingV3UiCustomizer {
    override val priority: Int = 100

    override fun customize(
        context: Context,
        state: OnBoardingV3PageState,
        current: OnBoardingV3UiSpec,
    ): OnBoardingV3UiSpec {
        return current.copy(
            titleTextColor = ContextCompat.getColor(context, R.color.onboarding_text),
            actionTextColor = ContextCompat.getColor(context, R.color.onboarding_primary),
            actionText = context.getString(
                if (state.isPageEnd) R.string.onboarding_start else R.string.onboarding_next
            ),
            actionBackgroundRes = R.drawable.bg_onboarding_primary_button,
        )
    }
}
```

`OnBoardingV3PageState` cung cấp loại page (`STANDARD`/`END_TAB`), vị trí logic và vị trí content, số page, ảnh/title/subtitle đã resolve, trạng thái page cuối, ads, vị trí action và `OnBoardingConfig` đầy đủ.

## 3. Thay toàn bộ UI cho app đặc thù

Không override XML của core bằng resource cùng tên. Hãy tạo layout trong app và dùng renderer tương ứng:

| Version | Renderer | Render scope | Kết quả |
| --- | --- | --- | --- |
| V1 | `OnBoardingV1PageRenderer` | `OnBoardingV1RenderScope` | `OnBoardingV1RenderedPage` |
| V2 | `OnBoardingV2PageRenderer` | `OnBoardingV2RenderScope` | `OnBoardingV2RenderedPage` |
| V3 | `OnBoardingV3PageRenderer` | `OnBoardingV3RenderScope` | `OnBoardingV3RenderedPage` |

Ví dụ V3:

```kotlin
@Singleton
class SpecialOnBoardingV3Renderer @Inject constructor() : OnBoardingV3PageRenderer {
    override val priority: Int = 100

    override fun supports(state: OnBoardingV3PageState): Boolean {
        // Có thể thay tất cả page hoặc chỉ một loại/vị trí cụ thể.
        return state.pageType == OnBoardingV3PageType.STANDARD
    }

    override fun render(scope: OnBoardingV3RenderScope): OnBoardingV3RenderedPage {
        val binding = AppOnboardingSpecialBinding.inflate(
            scope.inflater,
            scope.parent,
            false,
        )
        binding.image.setImageResource(scope.state.imageRes)
        binding.title.text = scope.state.title
        binding.subtitle.text = scope.state.subtitle
        binding.action.setOnClickListener { scope.actions.onPrimaryAction() }

        return OnBoardingV3RenderedPage(
            view = binding.root,
            onBannerNativeResult = { resource, placeName ->
                binding.layoutBannerNative.processAdResource(resource, placeName)
            },
            onDispose = {
                // Hủy animation/listener thuộc riêng custom view nếu có.
            },
        )
    }
}
```

View trả về không được có parent. Dùng `scope.lifecycleOwner` cho observer/animation theo lifecycle. Gọi `scope.actions.onPrimaryAction()`, `onNext()` hoặc `onFinish()` để core tiếp tục đảm bảo tracking và flow. V1/V2 đặt ads ở Activity bên ngoài page; V3 đặt ads trong page nên renderer V3 cần forward callback vào ad container như ví dụ trên.

## 4. API View Binding cũ (deprecated)

Tạo một class implement `OnBoardingUiCustomizer`. Chỉ cần override callback của version app đang sử dụng; các hàm đều có implementation mặc định rỗng.

```kotlin
package com.example.app.startflow

import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.app.R
import com.core.startflow.databinding.StartflowFragmentOnboardingBinding
import com.core.startflow.databinding.StartflowFragmentOnboardingV2Binding
import com.core.startflow.onboarding.OnBoardingUiCustomizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppOnBoardingUiCustomizer @Inject constructor() : OnBoardingUiCustomizer {

    override fun customizeOnBoardingV1(
        fragment: Fragment,
        binding: StartflowFragmentOnboardingBinding,
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
        binding: StartflowFragmentOnboardingV2Binding,
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

}
```

Các callback View Binding cũ vẫn được gọi để tương thích source, nhưng toàn bộ V1/V2/V3 đã deprecated. Code mới cần migrate sang `UiCustomizer` hoặc `PageRenderer` đúng version.

## 5. Đăng ký với Hilt

Đăng ký provider và customizer bằng multibinding `@IntoSet` trong module app:

```kotlin
import com.core.startflow.onboarding.OnBoardingContentProvider
import com.core.startflow.onboarding.v1.OnBoardingV1UiCustomizer
import com.core.startflow.onboarding.v2.OnBoardingV2UiCustomizer
import com.core.startflow.onboarding.v3.OnBoardingV3PageRenderer
import com.core.startflow.onboarding.v3.OnBoardingV3UiCustomizer
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
    fun provideOnBoardingV1UiCustomizer(
        customizer: AppOnBoardingV1UiCustomizer,
    ): OnBoardingV1UiCustomizer = customizer

    @Provides
    @IntoSet
    @Singleton
    fun provideOnBoardingV2UiCustomizer(
        customizer: AppOnBoardingV2UiCustomizer,
    ): OnBoardingV2UiCustomizer = customizer

    @Provides
    @IntoSet
    @Singleton
    fun provideOnBoardingV3UiCustomizer(
        customizer: AppOnBoardingV3UiCustomizer,
    ): OnBoardingV3UiCustomizer = customizer

    // Chỉ đăng ký renderer này ở những app cần thay toàn bộ UI.
    @Provides
    @IntoSet
    @Singleton
    fun provideOnBoardingV3PageRenderer(
        renderer: SpecialOnBoardingV3Renderer,
    ): OnBoardingV3PageRenderer = renderer
}
```

Module `core:startflow` đã khai báo `@Multibinds`, vì vậy app không cần sửa module core. App chỉ cần đăng ký customizer của những version đang dùng; renderer là tùy chọn.

## Ý nghĩa state/callback

| Tham số | Ý nghĩa |
| --- | --- |
| `fragment`, `binding` | Chỉ thuộc bridge cũ đã deprecated |
| `position` | Vị trí content, bắt đầu từ `0` |
| `isLastPage` | Trang content cuối thực tế của V1/V2 sau khi parse Remote Config |
| `introductionPosition` | Vị trí logic trong flow V3; có tính đến vị trí dành cho full-native ad |
| `realPosition` | Vị trí content thực dùng để lấy ảnh/title trong V3 |
| `isPageEnd` | Đây là trang content cuối có thể hoàn tất onboarding |
| `isShowAd` | Page content hiện tại được cấu hình hiển thị ads hay không; V1/V2 render ads ở Activity, V3 render trong page |
| `config` | Config đã parse, gồm `version`, `positionNext`, close/swipe và delay |
| `pageType` | `STANDARD` hoặc `END_TAB`; dùng để renderer chọn đúng loại page |
| `actions` | Callback `onPrimaryAction()`, `onNext()` và `onFinish()` do core xử lý |

## Các view thường custom

| Version | API mặc định | Khả năng chính |
| --- | --- | --- |
| V1 | `OnBoardingV1UiSpec` | Title/action, visibility indicator, text appearance và background |
| V2 | `OnBoardingV2UiSpec` | Title/subtitle/action, content background và fill-gradient CTA |
| V3 mặc định | `OnBoardingV3UiSpec` | Text/color/text appearance/background/visibility/indicator |
| App đặc thù | `OnBoardingV1/V2/V3PageRenderer` | Toàn bộ view của page, action callback, lifecycle; V3 có thêm callback ads |

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

- Không giữ `binding`, `fragment`, `RenderScope` hoặc view đã render trong singleton; chúng chỉ hợp lệ theo vòng đời view hiện tại.
- Không thay click listener của nút Next/Get Started nếu chỉ đổi giao diện. Listener mặc định còn phụ trách chuyển trang, kết thúc flow, tracking và quảng cáo.
- `OnBoardingUiCustomizer` chỉ là bridge cũ. Nếu vẫn đăng ký nhiều bridge, chúng chạy theo tên class để kết quả xác định nhưng không nên thêm code mới vào API này.
- `OnBoardingV1/V2/V3UiCustomizer` chạy từ priority thấp đến cao. Mỗi version chỉ chọn `PageRenderer` hỗ trợ page có priority cao nhất; tên class là tie-breaker.
- Không override XML của core bằng resource cùng tên; binding của core vẫn yêu cầu đầy đủ ID/type và có thể crash khi inflate.
- Dùng resource trong module app cho ảnh, màu, string và drawable để từng app có theme riêng mà không thay đổi `core:startflow`.
- Khi đổi `introPageCount`, cập nhật đủ ảnh/string và các mảng `intro_data*` tương ứng để tránh index ngoài phạm vi hoặc xác định sai trang cuối.
