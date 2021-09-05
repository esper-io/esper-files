@file:Suppress("DEPRECATION")

package io.esper.files.util

import android.app.Activity
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
import com.example.flatdialoglibrary.dialog.FlatDialog
import io.esper.files.R
import java.io.File


object InstallUtil {
    private val TAG = InstallUtil::class.java.simpleName

    fun checkAccessibilitySetting(cxt: Context, service: Class<*>) {
        if (isAccessibilitySettingOpen(service, cxt)) return
//        AlertDialog.Builder(cxt)
//            .setTitle("Accessibility Settings")
//            .setMessage("Find and enable: Files Silent Install Service")
//            .setPositiveButton(
//                "Open"
//            ) { _, _ -> jumpToAccessibilitySetting(cxt) }
//            .show()
        val flatDialog = FlatDialog(cxt)
        flatDialog.setCanceledOnTouchOutside(true)
        flatDialog.setTitle("Accessibility Settings")
                .setIcon(R.drawable.accessibility)
                .setSubtitle("Find and enable: Files Silent Install Service")
                .setFirstButtonText("Open")
                .withFirstButtonListner {
                    jumpToAccessibilitySetting(cxt)
                    flatDialog.dismiss()
                }
                .show()
    }

    fun isAccessibilitySettingOpen(service: Class<*>, cxt: Context): Boolean {
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

    fun jumpToAccessibilitySetting(cxt: Context) {
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

    fun checkUnknownSourcesSetting(cxt: Context) {
        if (isUnknownSourcesSettingOpen(cxt)) return
//        AlertDialog.Builder(cxt)
//            .setTitle("App Installation Settings")
//            .setMessage("Enable install from unknown source settings")
//            .setPositiveButton("Open") { _, _ -> jumpToUnknownSourcesSetting(cxt) }
//            .show()
        val flatDialog = FlatDialog(cxt)
        flatDialog.setCanceledOnTouchOutside(true)
        flatDialog.setTitle("App Installation Settings")
                .setSubtitle("Enable install from unknown source settings")
                .setFirstButtonText("Open")
                .withFirstButtonListner {
                    jumpToUnknownSourcesSetting(cxt)
                    flatDialog.dismiss()
                }
                .show()
    }

    fun isUnknownSourcesSettingOpen(cxt: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            cxt.packageManager.canRequestPackageInstalls() else Settings.Secure.getInt(
                cxt.contentResolver,
                Settings.Secure.INSTALL_NON_MARKET_APPS,
                0
        ) == 1
    }

    private fun jumpToUnknownSourcesSetting(cxt: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            cxt.startActivity(
                    Intent(
                            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:" + cxt.packageName)
                    )
            ) else cxt.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
    }

    fun install(act: Activity, apkFile: File?) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri: Uri
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uri = FileProvider.getUriForFile(
                        act, act.packageName + ".provider",
                        apkFile!!
                )
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                uri = Uri.fromFile(apkFile)
            }
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            act.startActivity(intent)
        } catch (e: Throwable) {
            Toast.makeText(act, "Installation Failed：" + e.message, Toast.LENGTH_LONG).show()
        }
    }
}