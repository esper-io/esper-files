@file:Suppress("DEPRECATION")

package io.esper.files.constants

import android.os.Environment
import java.io.File

object Constants {

    const val storagePermission = 100
    var InternalRootFolder: String = Environment.getExternalStorageDirectory()
        .path + File.separator + "esperfiles" + File.separator
    var InternalCheckerString: String = "/storage/emulated/0/"

    var ExternalRootFolder: String =
        "android/data/io.shoonya.shoonyadpc/cache/esperfiles" + File.separator

    var InternalScreenshotFolderDCIM: String = Environment.getExternalStorageDirectory()
        .path + File.separator + "DCIM" + File.separator + "Screenshots" + File.separator
    var InternalScreenshotFolderPictures: String = Environment.getExternalStorageDirectory()
        .path + File.separator + "Pictures" + File.separator + "Screenshots" + File.separator
    var EsperScreenshotFolder: String = InternalRootFolder + "Screenshots"

    //Tags
    const val MainActivityTag = "MainActivity"
    const val ListItemsFragmentTag = "ListItemsFragment"
    const val FileUtilsTag = "FileUtils"
    const val VideoViewerActivityTag = "VideoViewerActivity"
    const val ImageViewerActivityTag = "ImageViewerActivity"

    // SharedPreference keys
    const val SHARED_LAST_PREFERRED_STORAGE = "LastPrefStorage"
    const val SHARED_EXTERNAL_STORAGE_VALUE = "ExtStorage"
    const val ORIGINAL_SCREENSHOT_STORAGE_PREF_KEY = "OriginalScreenshotFolderKey"
    const val ORIGINAL_SCREENSHOT_STORAGE_VALUE = "OGScreenshotFolder"

    const val SHARED_MANAGED_CONFIG_VALUES = "ManagedConfig"
    const val SHARED_MANAGED_CONFIG_APP_NAME = "app_name"
    const val SHARED_MANAGED_CONFIG_SHOW_SCREENSHOTS = "show_screenshots_folder"
    const val SHARED_MANAGED_CONFIG_DELETION_ALLOWED = "deletion_allowed"
}