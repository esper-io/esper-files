@file:Suppress("DEPRECATION")

package io.esper.files.constants

import android.os.Environment
import java.io.File

object Constants {

    const val storagePermission = 100
    var InternalRootFolder: String = Environment.getExternalStorageDirectory()
        .path + File.separator + "esperfiles" + File.separator
    var InternalCheckerString: String = "/storage/emulated/0/"

    var ExternalRootFolder: String = "esperfiles" + File.separator

    var InternalScreenshotFolderDCIM: String = Environment.getExternalStorageDirectory()
        .path + File.separator + "DCIM" + File.separator + "Screenshots" + File.separator
    var InternalScreenshotFolderPictures: String = Environment.getExternalStorageDirectory()
        .path + File.separator + "Pictures" + File.separator + "Screenshots" + File.separator

    @Suppress("SpellCheckingInspection")
    var EsperScreenshotFolder: String = InternalRootFolder + "Screenshots"

    val videoAudioFileFormats = arrayListOf(
        "mp4",
        "mov",
        "mkv",
        "mp3",
        "aac",
        "3gp",
        "m4a",
        "mpeg4",
        "wav",
        "ogg",
        "ts",
        "webm"
    )
    val imageFileFormats = arrayListOf(
        "jpeg",
        "jpg",
        "png",
        "gif",
        "bmp",
        "tiff",
        "tif",
        "svg",
        "webp",
        "heif",
        "heic",
        "ico",
        "raw"
    )
    val otherFileFormats = arrayListOf(
        "pdf",
        "zip",
        "xls",
        "xlsx",
        "ppt",
        "pptx",
        "doc",
        "docx",
        "csv",
        "vcf",
        "crt",
        "json",
        "txt",
        "apk",
        "xapk",
        "obb",
        ".7z",
        "log",
        "html",
        "xhtml",
        "htm"
    )

    //Tags
    const val MainActivityTag = "MainActivity"
    const val ListItemsFragmentTag = "ListItemsFragment"
    const val FileUtilsTag = "FileUtils"
    const val VideoViewerActivityTag = "VideoViewerActivity"
    const val ImageViewerActivityTag = "ImageViewerActivity"
    const val SlideShowActivityTag = "SlideShowActivity"

    // SharedPreference keys
    const val SHARED_LAST_PREFERRED_STORAGE = "LastPrefStorage"
    const val SHARED_EXTERNAL_STORAGE_VALUE = "ExtStorage"
    const val ORIGINAL_SCREENSHOT_STORAGE_PREF_KEY = "OriginalScreenshotFolderKey"
    const val ORIGINAL_SCREENSHOT_STORAGE_VALUE = "OGScreenshotFolder"

    const val SHARED_MANAGED_CONFIG_VALUES = "ManagedConfig"
    const val SHARED_MANAGED_CONFIG_APP_NAME = "app_name"
    const val SHARED_MANAGED_CONFIG_SHOW_SCREENSHOTS = "show_screenshots_folder"
    const val SHARED_MANAGED_CONFIG_DELETION_ALLOWED = "deletion_allowed"
    const val SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW = "kiosk_slideshow"
    const val SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_PATH = "kiosk_slideshow_path"
    const val SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_DELAY = "kiosk_slideshow_delay"
    const val SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_IMAGE_STRATEGY =
        "kiosk_slideshow_image_strategy"
    const val SHARED_MANAGED_CONFIG_FILE_FORMATS_AUDIO_VIDEO = "audio_video"
    const val SHARED_MANAGED_CONFIG_FILE_FORMATS_IMAGE = "image"
    const val SHARED_MANAGED_CONFIG_FILE_FORMATS_OTHER = "other"
    const val SHARED_MANAGED_CONFIG_USE_INBUILT_PDF = "inbuilt_pdf"
    const val SHARED_MANAGED_CONFIG_USE_INBUILT_AUDIO_VIDEO = "inbuilt_audio_video"
    const val SHARED_MANAGED_CONFIG_USE_INBUILT_IMAGE = "inbuilt_image"
}