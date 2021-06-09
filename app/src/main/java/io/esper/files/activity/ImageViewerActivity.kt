package io.esper.files.activity

import android.content.res.Configuration
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.jsibbold.zoomage.ZoomageView
import io.esper.files.R

class ImageViewerActivity : AppCompatActivity() {
    private lateinit var imageViewer: ZoomageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        imageViewer = findViewById(R.id.imageViewer)

        findViewById<TextView>(R.id.image_name).text = intent.getStringExtra("imageName")
        imageSetter(intent.getStringExtra("imagePath"))
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

        Glide.with(this).load(imgPath).listener(object :
                RequestListener<String?, GlideDrawable?> {
            override fun onException(
                    e: Exception?,
                    model: String?,
                    target: Target<GlideDrawable?>?,
                    isFirstResource: Boolean
            ): Boolean {
                imageViewer.setImageResource(R.drawable.broken_photo)
                return true
            }

            override fun onResourceReady(
                    resource: GlideDrawable?,
                    model: String?,
                    target: Target<GlideDrawable?>,
                    isFromMemoryCache: Boolean,
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
        }).placeholder(circularProgressDrawable).priority(Priority.HIGH).into(imageViewer)
    }
}