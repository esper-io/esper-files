package io.esper.files.strategy.image;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.ImageView;

import io.esper.files.model.FileItem;

/**
 * An interface for handling image loading strategies.
 */
public interface ImageStrategy {

    /**
     * Set the context.
     * @param context
     */
    void setContext(Context context);

    /**
     * Set the image strategy callback.
     * @param callback
     */
    void setCallback(ImageStrategyCallback callback);

    /**
     * Preloads the image of the file item into a cache.
     * @param item
     */
    void preload(FileItem item);

    /**
     * Loads the image of the file item into the view.
     * @param item
     */
    void load(FileItem item, ImageView view);

    interface ImageStrategyCallback {

        /**
         * Queues the next slide using the default duration.
         */
        void queueSlide();

        /**
         * Queues the next slide using the given duration.
         * @param duration
         */
        void queueSlide(int duration);
    }

}