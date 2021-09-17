package io.esper.videos.strategy.image

import android.content.Context
import android.widget.ImageView
import io.esper.videos.model.FileItem

/**
 * An interface for handling image loading strategies.
 */
interface ImageStrategy {
    /**
     * Set the context.
     *
     * @param context
     */
    fun setContext(context: Context?)

    /**
     * Set the image strategy callback.
     *
     * @param callback
     */
    fun setCallback(callback: ImageStrategyCallback?)

    /**
     * Preloads the image of the file item into a cache.
     *
     * @param item
     */
    fun preload(item: FileItem?)

    /**
     * Loads the image of the file item into the view.
     *
     * @param item
     */
    fun load(item: FileItem?, view: ImageView?)
    interface ImageStrategyCallback {
        /**
         * Queues the next slide using the default duration.
         */
        fun queueSlide()

        /**
         * Queues the next slide using the given duration.
         *
         * @param duration
         */
        fun queueSlide(duration: Int)
    }
}