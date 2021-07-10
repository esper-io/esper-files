package io.esper.files.activity

import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.palette.graphics.Palette
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.jsibbold.zoomage.ZoomageView
import io.esper.files.R
import io.esper.files.constants.Constants.ImageViewerActivityTag

class ImageViewerActivity : AppCompatActivity() {
    private lateinit var imageViewer: ZoomageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)

        imageViewer = findViewById(R.id.imageViewer)

        findViewById<TextView>(R.id.image_name).text = intent.getStringExtra("imageName")
        imageSetter(intent.getStringExtra("imagePath"))
        try {
            imageViewer.setBackgroundColor(
                Palette.from(BitmapFactory.decodeFile(intent.getStringExtra("imagePath")))
                    .generate().vibrantSwatch!!.rgb
            )
        } catch (e: Exception) {
            Log.e(ImageViewerActivityTag, e.toString())
        }
        findViewById<ImageView>(R.id.image_activity_back).setOnClickListener { onBackPressed() }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageSetter(intent.getStringExtra("imagePath"))
        imageViewer.autoCenter = true
        imageViewer.reset(true)
    }

    private fun imageSetter(imgPath: String?) {
        val circularProgressDrawable = CircularProgressDrawable(this)
        circularProgressDrawable.strokeWidth = 10f
        circularProgressDrawable.centerRadius = 70f
        circularProgressDrawable.start()

        Glide.with(this)
            .load(imgPath)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    imageViewer.setImageResource(R.drawable.broken_file)
                    return true
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    imageViewer.reset(true)
                    imageViewer.scaleType = ImageView.ScaleType.FIT_CENTER
                    imageViewer.isZoomable = true
                    imageViewer.isTranslatable = true
                    imageViewer.autoCenter = true
                    imageViewer.doubleTapToZoom = true
                    return false
                }
            })
            .placeholder(circularProgressDrawable).priority(Priority.HIGH)
            .into(imageViewer)
    }
}