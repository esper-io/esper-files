package io.esper.files.activity

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.potyvideo.library.AndExoPlayerView
import io.esper.files.R

class VideoViewerActivity : AppCompatActivity() {

    private var youTubePlayerView: YouTubePlayerView? = null
    private var andExoPlayerView: AndExoPlayerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_viewer)

        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)

        andExoPlayerView = findViewById(R.id.andExoPlayerView)
        youTubePlayerView = findViewById(R.id.youtube_player_view)
        findViewById<ImageView>(R.id.video_activity_back).setOnClickListener { onBackPressed() }

        if (!intent.getBooleanExtra("isYT", false)) {
            andExoPlayerView!!.visibility = View.VISIBLE
            youTubePlayerView!!.visibility = View.GONE
            andExoPlayerView!!.setSource(intent.getStringExtra("videoPath")!!)
        } else {
            andExoPlayerView!!.visibility = View.GONE
            youTubePlayerView!!.visibility = View.VISIBLE
            lifecycle.addObserver(youTubePlayerView!!)
            youTubePlayerView!!.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
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
            andExoPlayerView!!.releasePlayer()
        else
            youTubePlayerView!!.release()
    }
}