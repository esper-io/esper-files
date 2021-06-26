package io.esper.files.util

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.text.TextUtils.SimpleStringSplitter
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object AccessibilityUtil {
    private val TAG = AccessibilityUtil::class.java.simpleName

    fun checkSetting(cxt: Context, service: Class<*>) {
        if (isSettingOpen(service, cxt)) return
        AlertDialog.Builder(cxt)
            .setTitle("Accessibility Settings")
            .setMessage("Find and enable: Files Silent Install Service")
            .setPositiveButton(
                "Open"
            ) { _, _ -> jumpToSetting(cxt) }
            .show()
    }

    fun isSettingOpen(service: Class<*>, cxt: Context): Boolean {
        try {
            val enable = Settings.Secure.getInt(
                cxt.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED,
                0
            )
            if (enable != 1) return false
            val services = Settings.Secure.getString(
                cxt.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (!TextUtils.isEmpty(services)) {
                val split = SimpleStringSplitter(':')
                split.setString(services)
                while (split.hasNext()) {
                    if (split.next()
                            .equals(cxt.packageName + "/" + service.name, ignoreCase = true)
                    ) return true
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "isSettingOpen: " + e.message)
        }
        return false
    }

    fun jumpToSetting(cxt: Context) {
        try {
            cxt.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (e: Throwable) {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                cxt.startActivity(intent)
            } catch (e2: Throwable) {
                Log.e(TAG, "jumpToSetting: " + e2.message)
            }
        }
    }

    fun install(cxt: Context, apkFile: File?) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri: Uri
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uri = FileProvider.getUriForFile(
                    cxt, cxt.packageName + ".provider",
                    apkFile!!
                )
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                uri = Uri.fromFile(apkFile)
            }
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            cxt.startActivity(intent)
        } catch (e: Throwable) {
            Toast.makeText(cxt, "Installation Failed：" + e.message, Toast.LENGTH_LONG).show()
        }
    }
}