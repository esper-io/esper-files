package io.esper.files.strategy.image.custom

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.IOException

object CustomRotateDimenTransformation {
    private val TAG = CustomRotateDimenTransformation::class.java.name

    // see https://www.daveperrett.com/articles/2012/07/28/exif-orientation-handling-is-a-ghetto/
    private val EXIF_ORIENTATION_TO_ROTATION = intArrayOf(-1, 0, 0, 180, 180, 90, 90, 270, 270)

    /**
     * Calculates the necessary degrees by which the image needs to be rotated in order to be displayed correctly according to the EXIf information.
     *
     *
     * Note: image flipping is not supported, although part of the same EXIF tag
     *
     * @return the degrees to rotate
     */
    @JvmStatic
    fun getRotationFromExif(filename: String): Int {
        return try {
            val exif = ExifInterface(filename)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
            Log.d(TAG, "File $filename has EXIF orientation $orientation")
            EXIF_ORIENTATION_TO_ROTATION[orientation]
        } catch (e: IOException) {
            Log.e(TAG, "EXIF data for file $filename failed to load.")
            -1
        }
    }

    /**
     * Calculates the necessary degrees by which the image needs to be rotated in order to fill the full screen in landscape mode
     *
     * @return the degrees to rotate
     */
    @JvmStatic
    fun getRotationFromDimensions(image: Bitmap): Int {
        return if (image.width > image.height) {
            90
        } else {
            0
        }
    }

    /**
     * Rotates the given image by the given degrees
     *
     * @return the rotated image
     */
    @JvmStatic
    fun rotate(image: Bitmap, degrees: Int): Bitmap {
        // Rotate the image if it is landscape
        val matrix = Matrix()
        matrix.postRotate(degrees.toFloat())
        return Bitmap.createBitmap(image, 0, 0, image.width, image.height, matrix, true)
    }

    /**
     * Return true if the rotation for the given degeers swaps the coordinates of the image, false otherwise.
     */
    @JvmStatic
    fun isCoordinatesSwapped(degrees: Int): Boolean {
        return degrees == 90 || degrees == 270
    }
}