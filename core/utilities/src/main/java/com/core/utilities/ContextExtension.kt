package com.core.utilities

import android.Manifest
import android.app.Activity
import android.app.LocaleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.telephony.TelephonyManager
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.LifecycleOwner
import com.core.utilities.util.CommonUtil
import com.core.utilities.util.toast.Toasty
import java.io.File
import java.util.Locale


fun Context.isValidGlideContext() = this !is Activity || (!this.isDestroyed && !this.isFinishing)

fun Context.queryCursor(
    uri: Uri,
    projection: Array<String>,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = null,
    showErrors: Boolean = false,
    callback: (cursor: Cursor) -> Unit
) {
    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
        cursor?.use {
            if (cursor.moveToFirst()) {
                do {
                    callback(cursor)
                } while (cursor.moveToNext())
            }
        }
    } catch (e: Exception) {
        if (showErrors) {
            toast(e.message ?: "", Toasty.ERROR)
        }
    }
}


fun Activity.openGallery() {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.type = "image/*"
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    this.startActivity(intent)
}


fun Context.rateApp() {
    val intent = Intent(Intent.ACTION_VIEW)

    intent.data =
        Uri.parse("http://play.google.com/store/apps/details?id=$packageName")
    try {
        startActivity(intent)
    } catch (_: Exception) {}
}

fun Context.shareApp(@StringRes resId: Int) {
    try {
        ShareCompat.IntentBuilder(this)
            .setType("text/plain")
            .setChooserTitle(resId)
            .setText("http://play.google.com/store/apps/details?id=$packageName")
            .startChooser()
    } catch (_: Exception) {}
}

fun Context.shareFiles(listFile: List<String>) {
    val imageUris: ArrayList<Uri> = arrayListOf<Uri>()
    imageUris.addAll(listFile.map {
        FileProvider.getUriForFile(
            this,
            "$packageName.provider",
            File(it)
        )
    })
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND_MULTIPLE
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, imageUris)
        type = "*/*"
    }
    try {
        startActivity(Intent.createChooser(shareIntent, null))
    } catch (_: Exception) {}
}

fun Context.openUrl(url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        toast("No application supported!", Toasty.ERROR)
    }
}

fun Context.isDebug(): Boolean = isAppDebuggable()

fun Context.isAppDebuggable(): Boolean {
    return 0 != (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE)
}

fun Context.openAppNotificationSettings() {
    val intent = Intent().apply {
        action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    }
    startActivity(intent)
}

fun Context.openPhotoByGallery(pathFile: String) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    intent.setDataAndType(Uri.parse("file://$pathFile"), "image/*")
    startActivity(intent)
}

fun Context.openVideoByGallery(pathFile: String) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    intent.setDataAndType(Uri.parse("file://$pathFile"), "video/*")
    startActivity(intent)
}

fun Context.checkStoragePermissions(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        //Android is 11 (R) or above
        Environment.isExternalStorageManager()
    } else {
        //Below android 11
        val write =
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val read =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED
    }
}

fun Context.getCurrentLanguageCode(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSystemService(LocaleManager::class.java).applicationLocales.toLanguageTags()
    } else {
        AppCompatDelegate.getApplicationLocales().toLanguageTags()
    }
}


fun Context.getCurrentLocale(): Locale? {
    return runCatching {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getSystemService(LocaleManager::class.java).applicationLocales.get(0)
        } else {
            AppCompatDelegate.getApplicationLocales().get(0)
        }
    }.getOrNull()
}

fun Context.getCountryCode(): String {
    val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    val countryCode = telephonyManager.run {
        when {
            simCountryIso != null && simCountryIso.length == 2 -> {
                simCountryIso
            }
            phoneType == TelephonyManager.PHONE_TYPE_CDMA -> { // Code Division Multiple Access
                CommonUtil.getCDMACountryIso()
            }
            else -> {
                networkCountryIso
            }
        }
    } ?: ""

    return if (countryCode.length == 2) {
        countryCode
    } else {
        if (CommonUtil.isApi24orHigher()) {
            try {
                resources.configuration.locales[0].country
            } catch (e: Exception) {
                ""
            }
        } else {
            resources.configuration.locale.country
        }
    }.lowercase()

}

fun Context.isRtl(): Boolean {
    return resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
}

fun Context.pxToDP(px: Float): Float {
    return px / resources.displayMetrics.density
}

fun Context.getWidthDisplay(): Int {
    return resources.displayMetrics.widthPixels
}

fun Context.getLifecycleOwner(): LifecycleOwner? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is LifecycleOwner) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

fun Context.getActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}
