package io.esper.videos.strategy.image.glide

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import io.esper.videos.R
import io.esper.videos.model.FileItem
import io.esper.videos.strategy.image.ImageStrategy
import io.esper.videos.strategy.image.ImageStrategy.ImageStrategyCallback

class GlideImageStrategy : ImageStrategy {
    private var context: Context? = null
    private var callback: ImageStrategyCallback? = null
    override fun setContext(context: Context?) {
        this.context = context
    }

    override fun setCallback(callback: ImageStrategyCallback?) {
        this.callback = callback
    }

    override fun preload(item: FileItem?) {
        if (item!!.path!!.endsWith(".gif"))
            Glide.with(context!!)
                .asGif()
                .load(item.path)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .priority(Priority.HIGH)
                .preload()
        else
            Glide.with(context!!)
                .asDrawable()
                .load(item.path)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .priority(Priority.HIGH)
                .preload()
    }

    override fun load(item: FileItem?, view: ImageView?) {
        if (item!!.path!!.endsWith(".gif"))
            imageSetterAsGif(item.path!!, view!!)
        else
            imageSetter(item.path!!, view!!)
    }

    private fun imageSetter(imgPath: String, view: ImageView) {

        Glide.with(context!!)
            .asDrawable()
            .transform(GlideRotateDimenTransformation())
            .load(imgPath)
            .transition(withCrossFade())
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    view.setImageResource(R.drawable.broken_file)
                    return true
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    callback!!.queueSlide()
                    return false
                }
            })
            .priority(Priority.HIGH)
            .into(view)
    }

    private fun imageSetterAsGif(imgPath: String, view: ImageView) {
        Glide.with(context!!)
            .asGif()
            .transform(GlideRotateDimenTransformation())
            .load(imgPath)
            .transition(withCrossFade())
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .listener(object : RequestListener<GifDrawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<GifDrawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    view.setImageResource(R.drawable.broken_file)
                    return true
                }

                override fun onResourceReady(
                    resource: GifDrawable,
                    model: Any?,
                    target: Target<GifDrawable>,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    callback!!.queueSlide(250)
                    return false
                }
            })
            .priority(Priority.HIGH)
            .into(view)
    }

    companion object {
        @Suppress("unused")
        private val TAG = GlideImageStrategy::class.java.name
    }
}