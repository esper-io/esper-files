package io.esper.files.activity

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.palette.graphics.Palette
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import io.esper.files.R
import io.esper.files.constants.Constants.VideoViewerActivityTag


open class VideoViewerActivity : AppCompatActivity() {

    private var youTubePlayerView: YouTubePlayerView? = null
    private var playerView: StyledPlayerView? = null

    @Nullable
    protected var player: SimpleExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        setContentView(R.layout.activity_video_viewer)

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)

        val toolbar: Toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        toolbar.setNavigationOnClickListener { onBackPressed() }

        playerView = findViewById(R.id.player_view)
        youTubePlayerView = findViewById(R.id.youtube_player_view)
        if (!intent.getBooleanExtra("isYT", false)) {
            playerView!!.visibility = View.VISIBLE
            youTubePlayerView!!.visibility = View.GONE
            player = SimpleExoPlayer.Builder(this)
                    .build()
            player!!.setAudioAttributes(AudioAttributes.DEFAULT, true)
            player!!.playWhenReady = true
            playerView!!.player = player
            player!!.setMediaItem(
                    MediaItem.fromUri(Uri.parse(intent.getStringExtra("videoPath")!!)),
                    false
            )
            player!!.prepare()
            player!!.play()
            try {
                Glide.with(this).asBitmap().load(intent.getStringExtra("videoPath"))
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onLoadCleared(placeholder: Drawable?) {}
                            override fun onResourceReady(
                                    resource: Bitmap,
                                    transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                            ) {
                                playerView!!.setBackgroundColor(
                                        Palette.from(resource).generate().vibrantSwatch!!.rgb
                                )
                            }
                        })
            } catch (e: Exception) {
                Log.e(VideoViewerActivityTag, e.toString())
            }
        } else {
            playerView!!.visibility = View.GONE
            youTubePlayerView!!.visibility = View.VISIBLE
            lifecycle.addObserver(youTubePlayerView!!)
            youTubePlayerView!!.addYouTubePlayerListener(object :
                    AbstractYouTubePlayerListener() {
                override fun onReady(@NonNull youTubePlayer: YouTubePlayer) {
                    youTubePlayer.loadVideo(intent.getStringExtra("videoPath")!!, 0F)
                }
            })
        }
    }


    override fun onPause() {
        super.onPause()
        finish()
    }

    override fun onStop() {
        super.onStop()
        if (!intent.getBooleanExtra("isYT", false))
            player!!.release()
        else
            youTubePlayerView!!.release()
    }
}