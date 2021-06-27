@file:Suppress("DEPRECATION")

package io.esper.files.bottomsheets

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.potyvideo.library.AndExoPlayerView
import io.esper.files.R

class VideoBottomSheetDialog(videoPath: String) : BottomSheetDialogFragment() {

    private var fullscreenButton: ImageView? = null
    private var behavior: BottomSheetBehavior<*>? = null
    private lateinit var andExoPlayerView: AndExoPlayerView
    private var path: String? = videoPath
    private var fullscreen: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(
            R.layout.video_bottom_sheet_layout,
            container, false
        )

        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        andExoPlayerView = v.findViewById(R.id.andExoPlayerView)
        andExoPlayerView.setSource(path!!)
        fullscreenButton = v.findViewById(R.id.exo_fullscreen_icon)
        fullscreenButton!!.setOnClickListener {
            if (fullscreen) {
                fullscreenButton!!.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.exo_ic_fullscreen_enter
                    )
                )
                requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                behavior!!.isDraggable = true
                val params =
                    v.layoutParams as FrameLayout.LayoutParams
                params.width = ViewGroup.LayoutParams.MATCH_PARENT
                params.height =
                    (200 * requireContext().applicationContext
                        .resources.displayMetrics.density).toInt()
                andExoPlayerView.layoutParams = params
                fullscreen = false
            } else {
                fullscreenButton!!.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.exo_ic_fullscreen_exit
                    )
                )
                requireActivity().requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                behavior!!.isDraggable = false
                val params =
                    v.layoutParams as FrameLayout.LayoutParams
                params.width = ViewGroup.LayoutParams.MATCH_PARENT
                params.height = ViewGroup.LayoutParams.MATCH_PARENT
                andExoPlayerView.layoutParams = params
                fullscreen = true
            }
        }

        return v
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
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        andExoPlayerView.releasePlayer()
    }
}