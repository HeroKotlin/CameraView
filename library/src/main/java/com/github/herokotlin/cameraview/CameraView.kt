package com.github.herokotlin.cameraview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.RelativeLayout
import com.github.herokotlin.circleview.CircleView
import com.github.herokotlin.circleview.CircleViewCallback
import com.wonderkiln.camerakit.*
import kotlinx.android.synthetic.main.camera_view.view.*
import android.media.MediaPlayer
import com.github.herokotlin.cameraview.enum.CaptureMode
import com.github.herokotlin.cameraview.enum.VideoQuality


class CameraView: RelativeLayout {

    companion object {
        const val TAG = "CameraView"
    }

    private lateinit var configuration: CameraViewConfiguration
    private lateinit var callback: CameraViewCallback

    private var activeAnimator: ValueAnimator? = null

    private var isVideoRecording = false

    private val chooseViewWidth: Int by lazy {
        val radius = resources.getDimensionPixelSize(R.dimen.camera_view_capture_button_center_radius_normal)
        (radius * 2 * 3.2).toInt()
    }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init()
    }

    private fun init() {

        LayoutInflater.from(context).inflate(R.layout.camera_view, this)

        captureView.addCameraKitListener(object: CameraKitEventListener {
            override fun onVideo(event: CameraKitVideo?) {
                event?.let {

                    val videoPath = it.videoFile.absolutePath

                    val mediaPlayer = MediaPlayer()
                    mediaPlayer.setDataSource(videoPath)
                    mediaPlayer.prepare()

                    val duration = mediaPlayer.duration
                    if (duration >= configuration.videoMinDuration) {
                        showPreviewView()
                        previewView.video = videoPath
                    }
                    else {
//                        callback.onRecordDurationLessThanMinDuration()
                    }

                }
            }

            override fun onEvent(event: CameraKitEvent?) {
                if (event == null) {
                    return
                }
            }

            override fun onImage(event: CameraKitImage?) {
                event?.let {
                    showPreviewView()
                    previewView.image = it.bitmap
                }
            }

            override fun onError(event: CameraKitError?) {
                event?.let {
                    Log.e(TAG, "${it.exception}")
                }
            }
        })

        val circleViewCallback = object: CircleViewCallback {
            override fun onLongPressEnd(circleView: CircleView) {
                if (circleView == captureButton) {
                    stopRecordVideo()
                }
            }

            override fun onLongPressStart(circleView: CircleView) {
                if (circleView == captureButton && configuration.captureMode != CaptureMode.PHOTO) {
                    startRecordVideo()
                }
            }

            override fun onTouchDown(circleView: CircleView) {
                if (circleView == captureButton) {
                    circleView.centerColor =
                        ContextCompat.getColor(context, R.color.camera_view_capture_button_center_color_pressed)
                    circleView.invalidate()
                }
            }

            override fun onTouchEnter(circleView: CircleView) {
                if (circleView == captureButton) {
                    circleView.centerColor =
                        ContextCompat.getColor(context, R.color.camera_view_capture_button_center_color_pressed)
                    circleView.invalidate()
                }
            }

            override fun onTouchLeave(circleView: CircleView) {
                if (circleView == captureButton) {
                    circleView.centerColor =
                        ContextCompat.getColor(context, R.color.camera_view_capture_button_center_color_normal)
                    circleView.invalidate()
                }
            }

            override fun onTouchUp(circleView: CircleView, inside: Boolean, isLongPress: Boolean) {

                if (inside) {
                    if (circleView == captureButton) {
                        circleView.centerColor =
                            ContextCompat.getColor(context, R.color.camera_view_capture_button_center_color_normal)
                        circleView.invalidate()
                    }
                }

                if (!inside || isLongPress) {
                    return
                }

                if (circleView == captureButton) {
                    // 纯视频拍摄需要长按
                    if (configuration.captureMode != CaptureMode.VIDEO) {
                        capturePhoto()
                    }
                }
                else if (circleView == cancelButton) {
                    hidePreviewView()
                }
                else if (circleView == submitButton) {
                    hidePreviewView()
                }

            }
        }

        captureButton.callback = circleViewCallback
        cancelButton.callback = circleViewCallback
        submitButton.callback = circleViewCallback

        exitButton.setOnClickListener {

        }

        flipButton.setOnClickListener {
            captureView.toggleFacing()
        }

        flashButton.setOnClickListener {

            captureView.flash = when (captureView.flash) {
                CameraKit.Constants.FLASH_AUTO -> {
                    CameraKit.Constants.FLASH_ON
                }
                CameraKit.Constants.FLASH_ON -> {
                    CameraKit.Constants.FLASH_OFF
                }
                else -> {
                    CameraKit.Constants.FLASH_AUTO
                }
            }

            flashButton.setImageResource(
                when (captureView.flash) {
                    CameraKit.Constants.FLASH_AUTO -> {
                        R.drawable.camera_view_flash_auto
                    }
                    CameraKit.Constants.FLASH_ON -> {
                        R.drawable.camera_view_flash_on
                    }
                    else -> {
                        R.drawable.camera_view_flash_off
                    }
                }
            )

        }

    }

    fun init(configuration: CameraViewConfiguration) {

        this.configuration = configuration

        if (configuration.guideLabelTitle.isNotEmpty()) {
            guideLabel.visibility = View.VISIBLE
            guideLabel.text = configuration.guideLabelTitle
            if (configuration.guideLabelFadeOutDelay > 0) {
                postDelayed({
                    onGuideLabelFadeOut()
                }, configuration.guideLabelFadeOutDelay * 1000)
            }
        }

        captureView.setVideoQuality(
            when (configuration.videoQuality) {
                VideoQuality.P720 -> {
                    CameraKit.Constants.VIDEO_QUALITY_720P
                }
                VideoQuality.P1080 -> {
                    CameraKit.Constants.VIDEO_QUALITY_1080P
                }
                VideoQuality.P2160 -> {
                    CameraKit.Constants.VIDEO_QUALITY_2160P
                }
                else -> {
                    CameraKit.Constants.VIDEO_QUALITY_480P
                }
            }
        )

    }

    fun start() {
        captureView.start()
    }

    fun stop() {
        captureView.stop()
    }

    fun startRecordVideo() {

        if (isVideoRecording) {
            return
        }

        captureView.captureVideo()

        captureButton.centerRadius = resources.getDimensionPixelSize(R.dimen.camera_view_capture_button_center_radius_recording)
        captureButton.ringWidth = resources.getDimensionPixelSize(R.dimen.camera_view_capture_button_ring_width_recording)
        captureButton.requestLayout()

        startAnimation(
            configuration.videoMaxDuration,
            LinearInterpolator(),
            {
                // 避免结束时还无法到达满圆
                captureButton.trackValue = if (it > 0.99) 1f else it
                captureButton.invalidate()
            },
            {
                stopRecordVideo()
            }
        )

        isVideoRecording = true

    }

    fun stopRecordVideo() {

        if (!isVideoRecording) {
            return
        }

        captureButton.centerRadius = resources.getDimensionPixelSize(R.dimen.camera_view_capture_button_center_radius_normal)
        captureButton.ringWidth = resources.getDimensionPixelSize(R.dimen.camera_view_capture_button_ring_width_normal)
        captureButton.trackValue = 0f
        captureButton.requestLayout()

        captureView.stopVideo()
        activeAnimator?.cancel()

        isVideoRecording = false

    }

    fun capturePhoto() {
        captureView.captureImage()
    }

    private fun onGuideLabelFadeOut() {

        startAnimation(
            1000,
            LinearInterpolator(),
            {
                guideLabel.alpha = 1 - it
            },
            {
                guideLabel.visibility = View.GONE
            }
        )

    }

    private fun showPreviewView() {

        val chooseLayoutParams = chooseView.layoutParams

        startAnimation(
            200,
            LinearInterpolator(),
            {
                val alpha = 1 - it

                chooseLayoutParams.width = (chooseViewWidth * it).toInt()
                chooseView.alpha = it
                chooseView.requestLayout()

                flashButton.alpha = alpha
                flipButton.alpha = alpha
                captureButton.alpha = alpha
                exitButton.alpha = alpha
            },
            {
                flashButton.visibility = View.GONE
                flipButton.visibility = View.GONE
                captureButton.visibility = View.GONE
                exitButton.visibility = View.GONE
            }
        )

        previewView.visibility = View.VISIBLE
        captureView.visibility = View.GONE

    }

    private fun hidePreviewView() {

        val chooseLayoutParams = chooseView.layoutParams

        startAnimation(
            200,
            LinearInterpolator(),
            {
                val alpha = 1 - it

                chooseLayoutParams.width = (chooseViewWidth * alpha).toInt()
                chooseView.alpha = alpha
                chooseView.requestLayout()

                flashButton.alpha = it
                flipButton.alpha = it
                captureButton.alpha = it
                exitButton.alpha = it
            }
        )

        captureView.visibility = View.VISIBLE
        flashButton.visibility = View.VISIBLE
        flipButton.visibility = View.VISIBLE
        captureButton.visibility = View.VISIBLE
        exitButton.visibility = View.VISIBLE

        previewView.visibility = View.GONE

        previewView.image = null
        previewView.video = ""

    }

    private fun startAnimation(duration: Long, interpolator: TimeInterpolator, update: (Float) -> Unit, complete: (() -> Unit)? = null) {

        activeAnimator?.cancel()

        val animator = ValueAnimator.ofFloat(0f, 1f)

        animator.duration = duration
        animator.interpolator = interpolator
        animator.addUpdateListener {
            update(it.animatedValue as Float)
        }
        animator.addListener(object: AnimatorListenerAdapter() {
            // 动画被取消，onAnimationEnd() 也会被调用
            override fun onAnimationEnd(animation: android.animation.Animator?) {
                complete?.invoke()
                if (animation == activeAnimator) {
                    activeAnimator = null
                }
            }
        })
        animator.start()

        activeAnimator = animator

    }

}