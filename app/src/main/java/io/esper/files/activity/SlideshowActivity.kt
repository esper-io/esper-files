@file:Suppress(
        "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS",
        "NAME_SHADOWING",
        "SameParameterValue"
)

package io.esper.files.activity

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.esper.files.R
import io.esper.files.constants.Constants.SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_DELAY
import io.esper.files.constants.Constants.SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_IMAGE_STRATEGY
import io.esper.files.constants.Constants.SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_PATH
import io.esper.files.constants.Constants.SHARED_MANAGED_CONFIG_VALUES
import io.esper.files.constants.Constants.SlideShowActivityTag
import io.esper.files.listener.OnSwipeTouchListener
import io.esper.files.model.FileItem
import io.esper.files.strategy.image.ImageStrategy
import io.esper.files.strategy.image.ImageStrategy.ImageStrategyCallback
import io.esper.files.strategy.image.custom.CustomImageStrategy
import io.esper.files.strategy.image.glide.GlideImageStrategy
import java.io.File
import java.net.URLConnection
import java.util.*
import java.util.Collections.*

class SlideshowActivity : AppCompatActivity(), ImageStrategyCallback {
    private val mSlideshowHandler = Handler()
    private val mHideHandler = Handler()
    private var fileList: List<FileItem?> = ArrayList()
    private var handler = Handler()
    private var runnable: Runnable? = null
    private var delay = 20000
    private var sharedPrefManaged: SharedPreferences? = null

    @Suppress("PrivatePropertyName")
    private var SLIDESHOW_DELAY = 0
    private var blockPreferenceReload = false
    private var imageStrategy: ImageStrategy? = null
    private var imagePosition = 0
    private var isRunning = false
    private var mContentView: ImageView? = null
    private val mHidePart2Runnable = Runnable {
        mContentView!!.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        startSlideshow()
    }
    private var mVisible = false
    private val mHideRunnable = Runnable { hide() }
    private var userInputAllowed = true
    private var currentPath: String? = null
    private val mSlideshowRunnable = Runnable {
        val nextPos = followingImagePosition()
        nextImage(nextPos, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_slideshow)
        mVisible = true
        mContentView = findViewById(R.id.fullscreen_content)
        loadPreferences()
        // Stop resume from reloading the same settings
        blockPreferenceReload = true

        // Gesture / click detection
        mContentView!!.setOnTouchListener(object : OnSwipeTouchListener(this@SlideshowActivity) {
            public override fun onClick() {
//                if (checkUserInputAllowed()) {
//                    toggle();
//                }
            }

            public override fun onDoubleClick() {
                if (!mVisible && checkUserInputAllowed()) {
                    toggleSlideshow()
                    if (isRunning) {
                        Toast.makeText(
                                this@SlideshowActivity,
                                R.string.toast_resumed,
                                Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                                this@SlideshowActivity,
                                R.string.toast_paused,
                                Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            public override fun onLongClick() {
                userInputAllowed = !userInputAllowed
                if (checkUserInputAllowed()) {
                    Toast.makeText(
                            this@SlideshowActivity,
                            R.string.toast_input_allowed,
                            Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                            this@SlideshowActivity,
                            R.string.toast_input_blocked,
                            Toast.LENGTH_SHORT
                    ).show()
                }
            }

            public override fun onSwipeLeft() {
                if (checkUserInputAllowed()) {
                    nextImage(forwards = true, preload = false)
                    startSlideshowIfFullscreen()
                }
            }

            public override fun onSwipeRight() {
                if (checkUserInputAllowed()) {
                    nextImage(forwards = false, preload = false)
                    startSlideshowIfFullscreen()
                }
            }

            override fun onSwipeUp() {
//                if (checkUserInputAllowed()) {
//                    // Swipe up starts and stops the slideshow
//                    toggle();
//                }
            }

            override fun onSwipeDown() {
//                if (checkUserInputAllowed()) {
//                    // Swipe down starts and stops the slideshow
//                    toggle();
//                }
            }
        })
        if (sharedPrefManaged == null) sharedPrefManaged =
                getSharedPreferences(SHARED_MANAGED_CONFIG_VALUES, MODE_PRIVATE)
        currentPath =
                sharedPrefManaged!!.getString(SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_PATH, null)
        SLIDESHOW_DELAY =
                (sharedPrefManaged!!.getInt(SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_DELAY, 3).toString()
                        .toFloat() * 1000).toInt()
        if (currentPath != null)
            fileList =
                    getFileList(currentPath!!, includeDirectories = false, includeSubDirectories = true)
        if (fileList.isEmpty()) {
            // No files to view. Exit
            Log.i(SlideShowActivityTag, "No files in list.")
            Toast.makeText(this, R.string.toast_no_files, Toast.LENGTH_SHORT).show()
            onBackPressed()
            return
        }
        if (RANDOM_ORDER) {
            @Suppress("JavaCollectionsStaticMethodOnImmutableList")
            (shuffle(fileList))
        }
        if (currentPath == null) {
            imagePosition = 0
            nextImage(forwards = true, preload = true)
        } else {
            for (i in fileList.indices) {
                if (currentPath == fileList[i]!!.path) {
                    imagePosition = i
                    break
                }
            }
        }
        Log.v(SlideShowActivityTag, String.format("First item is at index: %s", imagePosition))
        Log.v(SlideShowActivityTag, String.format("File list has size of: %s", fileList.size))

        // Show the first image
        loadImage(imagePosition, false)
    }

    /**
     * Checks if user input is allowed and toasts if not.
     *
     * @return True if allowed, false otherwise
     */
    private fun checkUserInputAllowed(): Boolean {
//        if (!userInputAllowed) {
//            Toast.makeText(SlideshowActivity.this, R.string.toast_input_blocked, Toast.LENGTH_SHORT).show();
//        }
        return userInputAllowed
    }

    override fun onStart() {
        super.onStart()

        // Only reload the settings if not blocked by onCreate
        if (blockPreferenceReload) {
            blockPreferenceReload = false
        } else {
            loadPreferences()
        }
        // Start slideshow if no UI
        startSlideshowIfFullscreen()
    }

    /**
     * Load the relevant preferences.
     */
    private fun loadPreferences() {
        if (sharedPrefManaged == null) sharedPrefManaged =
                getSharedPreferences(SHARED_MANAGED_CONFIG_VALUES, MODE_PRIVATE)
        imageStrategy = if (sharedPrefManaged!!.getInt(
                        SHARED_MANAGED_CONFIG_KIOSK_SLIDESHOW_IMAGE_STRATEGY,
                        1
                ) == 1
        ) {
            Log.d(SlideShowActivityTag, "Image Strategy: Glide")
            try {
                GlideImageStrategy()
            } catch (e: Exception) {
                CustomImageStrategy()
            }
        } else {
            Log.d(SlideShowActivityTag, "Image Strategy: Custom")
            try {
                CustomImageStrategy()
            } catch (e: Exception) {
                GlideImageStrategy()
            }
        }
        imageStrategy!!.setContext(this)
        imageStrategy!!.setCallback(this)
    }

    /**
     * Show the next image.
     */
    private fun nextImage(forwards: Boolean, preload: Boolean) {
        nextImage(nextImagePosition(forwards), preload)
    }

    /**
     * Show the next image.
     */
    private fun nextImage(newPosition: Int, preload: Boolean) {
        if (preload && !PRELOAD_IMAGES) {
            // Stop
            return
        }
        val current = imagePosition
        if (REFRESH_FOLDER && newPosition == 0) { // Time to reload, easy base case
            fileList =
                    getFileList(currentPath!!, includeDirectories = false, includeSubDirectories = true)
            if (RANDOM_ORDER) {
                @Suppress("JavaCollectionsStaticMethodOnImmutableList")
                (shuffle(fileList))
            }
        }
        if (newPosition == current) {
            // Looped. Exit
//            onBackPressed();
            return
        }
        try {
            if (!preload) {
                imagePosition = newPosition
            }
            loadImage(newPosition, preload)
        } catch (e: Exception) {
            Log.e("TAG", e.toString())
        } finally {
        }
    }

    /**
     * Creates a list of fileitem for the given path.
     *
     * @param currentPath           The directory path.
     * @param includeDirectories    Whether or not to include directories.
     * @param includeSubDirectories Whether or not to include sub directories.
     */
    private fun getFileList(
            currentPath: String, includeDirectories: Boolean,
            includeSubDirectories: Boolean
    ): MutableList<FileItem?> {
        Log.d(SlideShowActivityTag, "updateFileList currentPath: $currentPath")

        // Create file list
        val fileList: MutableList<FileItem?> = ArrayList()
        val dir = File(currentPath)
        val files = dir.listFiles()
        if (files != null) {
            // Check hidden file preference
            for (file in files) {
                if (!file.name.startsWith(".")) {
                    // Test directories
                    if (includeDirectories || !file.isDirectory) {
                        fileList.add(createFileItem(file))
                    } else if (includeSubDirectories) {
                        fileList.addAll(
                                getFileList(
                                        file.absolutePath,
                                        includeDirectories = false,
                                        includeSubDirectories = true
                                )
                        )
                    }
                }
            }
        }
        fileList.sortBy { it!!.name }
        return fileList
    }

    /**
     * Create a fileitem from the given file.
     */
    private fun createFileItem(file: File): FileItem {
        val item = FileItem()
        item.name = file.name
        item.path = file.absolutePath
        item.isDirectory = file.isDirectory
        return item
    }

    /**
     * Show the following image.
     * This method handles whether or not the slideshow is in reverse order.
     */
    private fun followingImage() {
        nextImage(!REVERSE_ORDER, true)
    }

    /**
     * Gets the position of the next image.
     */
    private fun nextImagePosition(forwards: Boolean): Int {
        var newPosition = imagePosition
        do {
            newPosition += if (forwards) 1 else -1
            if (newPosition < 0) {
                newPosition = fileList.size - 1
            }
            if (newPosition >= fileList.size) {
                newPosition = 0
            }
        } while (!testPositionIsImage(newPosition) || !testPositionExists(newPosition))
        return newPosition
    }

    /**
     * Gets the position of the following image.
     * This method handles whether or not the slideshow is in reverse order.
     */
    private fun followingImagePosition(): Int {
        return nextImagePosition(!REVERSE_ORDER)
    }

    /**
     * Tests if the current file item is an image.
     *
     * @return True if image, false otherwise.
     */
    private fun testPositionIsImage(position: Int): Boolean {
        return isImage(fileList[position])
    }

    /**
     * Checks the mime-type of the file to see if it is an image.
     */
    private fun isImage(item: FileItem?): Boolean {
        if (item!!.isDirectory!!) {
            return false
        }
        if (item.isImage != null) {
            return item.isImage!!
        }
        val mimeType = getImageMimeType(item)
        item.isImage = mimeType != null && mimeType.startsWith("image")
        return item.isImage!!
    }

    /**
     * Returns the mime type of the given item.
     */
    private fun getImageMimeType(item: FileItem?): String? {
        var mime: String? = ""
        try {
            mime = URLConnection.guessContentTypeFromName(item!!.path)
        } catch (e: StringIndexOutOfBoundsException) {
            // Not sure the cause of this issue but it occurred on production so handling as blank mime.
        }
        if (mime == null || mime.isEmpty()) {
            // Test mime type by loading the image
            val opt = BitmapFactory.Options()
            opt.inJustDecodeBounds = true
            BitmapFactory.decodeFile(item!!.path, opt)
            mime = opt.outMimeType
        }
        return mime
    }

    /**
     * Tests if the current file item still exists.
     *
     * @return True if it's there, false otherwise.
     */
    private fun testPositionExists(position: Int): Boolean {
        return File(fileList[position]!!.path).exists()
    }

    /**
     * Load the image to the screen.
     */
    private fun loadImage(position: Int, preload: Boolean) {
        if (preload && !PRELOAD_IMAGES) {
            // Stop
            return
        }
        try {
            val item = fileList[position]
            if (preload) {
                imageStrategy!!.preload(item)
            } else {
                title = item!!.name
                imageStrategy!!.load(item, mContentView)
            }
        } catch (npe: NullPointerException) {
            Toast.makeText(this, R.string.toast_error_loading_image, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been created, to briefly hint
        // to the user that UI controls are available.
        delayedHide()
    }

    /**
     * Pause or play the slideshow.
     */
    private fun toggleSlideshow() {
        if (isRunning) {
            stopSlideshow()
        } else {
            startSlideshowIfFullscreen()
        }
    }

    /**
     * Stop or start the slideshow.
     */
    //    private void toggle() {
    //        if (mVisible) {
    //            hide();
    //        } else {
    //            show();
    //        }
    //    }
    private fun hide() {
        mVisible = false
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }
    //    private void show() {
    //        stopSlideshow();
    //        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    //                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    //        mVisible = true;
    //        // Schedule a runnable to display UI elements after a delay
    //        mHideHandler.removeCallbacks(mHidePart2Runnable);
    //    }
    /**
     * Schedules a call to hide() in 100 milliseconds, canceling any previously scheduled calls.
     */
    private fun delayedHide() {
        mHideHandler.removeCallbacks(mHideRunnable)
        mHideHandler.postDelayed(mHideRunnable, 100)
    }

    /**
     * Starts or restarts the slideshow if the view is in fullscreen mode.
     */
    private fun startSlideshowIfFullscreen() {
        if (!mVisible) {
            startSlideshow()
        }
    }

    /**
     * Starts or restarts the slideshow
     */
    private fun startSlideshow() {
        isRunning = true
        mSlideshowHandler.removeCallbacks(mSlideshowRunnable)
        queueSlide()
    }

    /**
     * Queue the next slide in the slideshow
     */
    override fun queueSlide() {
        queueSlide(SLIDESHOW_DELAY)
    }

    override fun queueSlide(duration: Int) {
        var delayMillis = duration
        if (delayMillis < SLIDESHOW_DELAY) {
            delayMillis = SLIDESHOW_DELAY
        }
        if (isRunning) {
            // Ensure only one runnable is in the queue
            mSlideshowHandler.removeCallbacks(mSlideshowRunnable)
            mSlideshowHandler.postDelayed(mSlideshowRunnable, delayMillis.toLong())
            // Preload the next image
            followingImage()
        }
    }

    private fun stopSlideshow() {
        isRunning = false
        mSlideshowHandler.removeCallbacks(mSlideshowRunnable)
    }

    override fun onResume() {
        handler.postDelayed(Runnable {
            handler.postDelayed(runnable, delay.toLong())
            hide()
        }.also { runnable = it }, delay.toLong())
        super.onResume()
    }

    override fun onPause() {
        handler.removeCallbacks(runnable)
        super.onPause()
    }

    companion object {
        private const val REVERSE_ORDER = false
        private const val RANDOM_ORDER = false
        private const val REFRESH_FOLDER = true
        private const val PRELOAD_IMAGES = true
        private const val UI_ANIMATION_DELAY = 300
    }
}