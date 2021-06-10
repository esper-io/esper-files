package io.esper.files.bottomsheets

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerFullScreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import io.esper.files.R

var youTubePlayerView: YouTubePlayerView? = null

class YTBottomSheetDialog(videoID: String) : BottomSheetDialogFragment() {

    private var behavior: BottomSheetBehavior<*>? = null
    var videoId: String? = videoID
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(
            R.layout.yt_bottom_sheet_layout,
            container, false
        )
        youTubePlayerView = v.findViewById(R.id.youtube_player_view)
        lifecycle.addObserver(youTubePlayerView!!)

        if (videoId != null)
            youTubePlayerView!!.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                override fun onReady(@NonNull youTubePlayer: YouTubePlayer) {
                    youTubePlayer.loadVideo(videoId!!, 0F)
                    addFullScreenListenerToPlayer()
                }
            })


        return v
    }

    private fun addFullScreenListenerToPlayer() {
        youTubePlayerView!!.addFullScreenListener(object : YouTubePlayerFullScreenListener {
            override fun onYouTubePlayerEnterFullScreen() {
                requireActivity().requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                behavior!!.isDraggable = false
            }

            override fun onYouTubePlayerExitFullScreen() {
                requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                behavior!!.isDraggable = true
            }
        })
    }

    override fun onViewCreated(
        view: View,
        @Nullable savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        view.viewTreeObserver.addOnGlobalLayoutListener {
            //Can be enabled if needed
            //dialog!!.setCanceledOnTouchOutside(false)
            val bottomSheet =
                dialog!!.findViewById(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout
            behavior = BottomSheetBehavior.from<View>(bottomSheet)
            behavior!!.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onPause() {
        super.onPause()
        this.dismiss()
    }

    override fun onStop() {
        super.onStop()
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        youTubePlayerView!!.release()
    }
}