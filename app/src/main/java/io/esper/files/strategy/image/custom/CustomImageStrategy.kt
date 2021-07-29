package io.esper.files.strategy.image.custom

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.ImageView
import io.esper.files.model.FileItem
import io.esper.files.strategy.image.ImageStrategy
import io.esper.files.strategy.image.ImageStrategy.ImageStrategyCallback
import io.esper.files.strategy.image.custom.CustomRotateDimenTransformation.getRotationFromDimensions
import io.esper.files.strategy.image.custom.CustomRotateDimenTransformation.getRotationFromExif
import io.esper.files.strategy.image.custom.CustomRotateDimenTransformation.isCoordinatesSwapped
import io.esper.files.strategy.image.custom.CustomRotateDimenTransformation.rotate
import javax.microedition.khronos.opengles.GL11
import kotlin.math.max

/**
 * A strategy for loading images that was taken from Google's Camera2 app.
 * This was the original implementation before Glide was added.
 */
class CustomImageStrategy : ImageStrategy {
    private var callback: ImageStrategyCallback? = null
    override fun setContext(context: Context?) {
        // Context not needed
    }

    override fun setCallback(callback: ImageStrategyCallback?) {
        this.callback = callback
    }

    override fun preload(item: FileItem?) {
        // This implementation does not support preloading
    }

    override fun load(item: FileItem?, view: ImageView?) {
        var options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(item!!.path, options)
        var sampleSize = 1
        var width = options.outWidth
        var height = options.outHeight

        /*
         * Downscale strategy taken from:
         * https://android.googlesource.com/platform/packages/apps/Camera2/src/com/android/camera/data/FilmstripItemUtils.java
         *
         * For large (> MAXIMUM_TEXTURE_SIZE) high aspect ratio (panorama)
         * Bitmap requests:
         *   Step 1: ask for double size.
         *   Step 2: scale maximum edge down to MAXIMUM_TEXTURE_SIZE.
         *
         * Here's the step 1: double size.
         */if (width > GL11.GL_MAX_TEXTURE_SIZE || height > GL11.GL_MAX_TEXTURE_SIZE) {
            sampleSize = 2
        }
        options = BitmapFactory.Options()
        options.inSampleSize = sampleSize
        /* 32K buffer. */options.inTempStorage = ByteArray(32 * 1024)

        // Load image
        var image = BitmapFactory.decodeFile(item.path, options)
        if (image == null) {
            Log.e(TAG, "Error loading image")
        } else {
            // calculate degrees to rotate
            var degrees = getRotationFromExif(item.path!!)
            if (degrees == -1) {
                @Suppress("LocalVariableName") val AUTO_ROTATE_DIMEN = false
                degrees = if (AUTO_ROTATE_DIMEN) {
                    getRotationFromDimensions(image)
                } else {
                    // no rotation necessary
                    0
                }
            }
            // do the actual rotation if degrees > 0
            if (degrees > 0) {
                image = rotate(image, degrees)
                if (isCoordinatesSwapped(degrees)) {
                    val temp = width
                    width = height
                    height = temp
                }
            }

            /*
             * Step 2: scale maximum edge down to maximum texture size.
             * If Bitmap maximum edge > MAXIMUM_TEXTURE_SIZE, which can happen for panoramas,
             * scale to fit in MAXIMUM_TEXTURE_SIZE.
             */if (image.width > GL11.GL_MAX_TEXTURE_SIZE || image.height > GL11.GL_MAX_TEXTURE_SIZE) {
                // Scale down
                val maxEdge = max(width, height)
                image = Bitmap.createScaledBitmap(image, width * GL11.GL_MAX_TEXTURE_SIZE / maxEdge,
                        height * GL11.GL_MAX_TEXTURE_SIZE / maxEdge, false)
            }
        }
        view!!.setImageBitmap(image)

        // Callback
        callback!!.queueSlide()
    }

    companion object {
        private val TAG = CustomImageStrategy::class.java.name
    }
}