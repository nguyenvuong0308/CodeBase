package com.codebasetemplate.required.update

import android.app.Activity
import android.app.Application
import android.graphics.Color
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.MainThread
import com.codebasetemplate.R
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cấu hình cho phép hiển thị có bản cập nhật mới ở trong ứng dụng để người dùng có thể cập nhật
 * Cách dùng:
 *
 * private val activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult -> }
 *
 * viewBinding.mainContainer.doOnAttach {
 *             inAppUpdateImpl.checkForUpdate(
 *                 activity = this,
 *                 launcher = activityResultLauncher,
 *                 rootView = it
 *             )
 *         }
 */
@Singleton
class InAppUpdateImpl @Inject constructor(
    app: Application
) {

    companion object {
        /** Chỉ khi staleness >= ngưỡng mới HIỂN THỊ in-app update */
        private const val DAYS_FOR_FLEXIBLE_UPDATE: Int = 7
    }

    /** Safe init: tránh crash nếu Play Services/Store lỗi hoặc không có */
    private val appUpdateManager: AppUpdateManager? = runCatching {
        AppUpdateManagerFactory.create(app)
    }.getOrNull()

    private var installListener: InstallStateUpdatedListener? = null

    /**
     * Gọi từ Activity:
     * - [launcher]: ActivityResultLauncher<IntentSenderRequest> do Activity đăng ký
     * - [rootView]: View (container của Activity) để hiển thị Snackbar (Flexible)
     * - [preferFlexible]: true => ưu tiên Flexible nếu được phép
     */
    @MainThread
    fun checkForUpdate(
        activity: Activity,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        rootView: View? = null,
        preferFlexible: Boolean = true
    ) {
        val manager = appUpdateManager ?: return

        manager.appUpdateInfo.addOnSuccessListener { info ->
            when (info.updateAvailability()) {
                UpdateAvailability.UPDATE_AVAILABLE -> {
                    val days = info.clientVersionStalenessDays() ?: 0

                    // ✅ Chỉ hiển thị sau khi đủ ngày lỗi thời
                    if (days < DAYS_FOR_FLEXIBLE_UPDATE) return@addOnSuccessListener

                    when {
                        preferFlexible && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> startFlexible(activity, launcher, info, rootView)

                        info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> startImmediate(launcher, info)

                        info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> startFlexible(activity, launcher, info, rootView)

                        else -> Unit
                    }
                }

                // Tiếp tục IMMEDIATE nếu đang dở
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    if (info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                        startImmediate(launcher, info)
                    }
                }

                else -> Unit
            }
        }
    }

    /** Gọi ở onResume để tiếp tục Immediate nếu bị gián đoạn. */
    @MainThread
    fun resumeIfNeeded(
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        val manager = appUpdateManager ?: return
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS &&
                info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                startImmediate(launcher, info)
            }
        }
    }

    /** Hủy lắng nghe Flexible khi người dùng bấm “Huỷ”, rời màn, v.v. */
    @MainThread
    fun cancelFlexible() {
        val manager = appUpdateManager ?: return
        installListener?.let { manager.unregisterListener(it) }
        installListener = null
    }

    private fun startImmediate(
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        info: AppUpdateInfo
    ) {
        appUpdateManager?.startUpdateFlowForResult(
            info,
            launcher,
            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
        )
    }

    private fun startFlexible(
        activity: Activity,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        info: AppUpdateInfo,
        rootView: View?
    ) {
        val manager = appUpdateManager ?: return

        // Đăng ký listener chỉ 1 lần
        if (installListener == null) {
            installListener = InstallStateUpdatedListener { state ->
                when (state.installStatus()) {
                    InstallStatus.DOWNLOADED -> {
                        // Hiển thị snackbar khi đã tải xong
                        rootView?.let { showDownloadedSnackbar(activity, it) }
                    }

                    InstallStatus.INSTALLED,
                    InstallStatus.FAILED,
                    InstallStatus.CANCELED -> {
                        // Dọn dẹp listener sớm, tránh leak
                        cancelFlexible()
                    }

                    else -> Unit
                }
            }
            manager.registerListener(installListener!!)
        }

        manager.startUpdateFlowForResult(
            info,
            launcher,
            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
        )
    }

    private fun showDownloadedSnackbar(activity: Activity, rootView: View) {
        // Guard: Activity đã đóng hoặc view chưa gắn vào window -> bỏ qua để tránh IllegalArgumentException
        if (activity.isFinishing || activity.isDestroyed) return
        if (!rootView.isAttachedToWindow) return
        Snackbar.make(
            rootView,
            activity.getString(R.string.msg_update_downloaded),
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction(activity.getString(R.string.text_restart)) { appUpdateManager?.completeUpdate() }
            setActionTextColor(Color.WHITE)
            show()
        }
    }
}