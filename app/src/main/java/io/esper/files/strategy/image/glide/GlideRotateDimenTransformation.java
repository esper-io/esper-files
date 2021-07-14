package io.esper.files.strategy.image.glide;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.load.resource.bitmap.TransformationUtils;

import org.jetbrains.annotations.NotNull;

import java.security.MessageDigest;

public class GlideRotateDimenTransformation extends BitmapTransformation {

    private static final String TAG = GlideRotateDimenTransformation.class.getName();

    @Override
    protected Bitmap transform(@NotNull BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
        Log.d(TAG, String.format("Height: %d Width: %d", toTransform.getHeight(), toTransform.getWidth()));
        if (toTransform.getHeight() >= toTransform.getWidth()) {
            // Perform fit center here on un-rotated image.
            toTransform = TransformationUtils.fitCenter(pool, toTransform, outWidth, outHeight);
            return toTransform;
        }
        // Fit center using largest side (width) for both to reduce computation for rotate
        //noinspection SuspiciousNameCombination
        toTransform = TransformationUtils.fitCenter(pool, toTransform, outWidth, outWidth);
        return TransformationUtils.rotateImage(toTransform, 90);
    }

    @Override
    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {

    }
}
