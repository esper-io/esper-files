package io.esper.videos.strategy.image.glide

import android.graphics.Bitmap
import android.util.Log
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.TransformationUtils
import java.security.MessageDigest

class GlideRotateDimenTransformation : BitmapTransformation() {
    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        Log.d(TAG, String.format("Height: %d Width: %d", toTransform.height, toTransform.width))
        if (toTransform.height >= toTransform.width) {
            return toTransform
        }
        return TransformationUtils.rotateImage(toTransform, 90)
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {}

    companion object {
        private val TAG = GlideRotateDimenTransformation::class.java.name
    }
}