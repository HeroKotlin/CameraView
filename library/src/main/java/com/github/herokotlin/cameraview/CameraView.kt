package com.github.herokotlin.cameraview

import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.RelativeLayout
import com.github.herokotlin.circleview.CircleView
import com.github.herokotlin.circleview.CircleViewCallback
import com.wonderkiln.camerakit.*
import kotlinx.android.synthetic.main.camera_view.view.*
import android.os.Environment
import com.github.herokotlin.cameraview.enum.CaptureMode
import com.github.herokotlin.cameraview.enum.VideoQuality
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


class CameraView: RelativeLayout {

    companion object {
        const val TAG = "CameraView"
        const val PERMISSION_REQUEST_CODE = 987456
    }

    var onExit: (() -> Unit)? = null

    var onCapturePhoto: ((String, Long, Int, Int) -> Unit)? = null

    var onRecordVideo: ((String, Long, Int, String, Long, Int, Int) -> Unit)? = null

    var onPermissionsGranted: (() -> Unit)? = null

    var onPermissionsDenied: (() -> Unit)? = null

    var onCaptureWithoutPermissions: (() -> Unit)? = null

    var onRecordWithoutExternalStorage: (() -> Unit)? = null

    var onRecordDurationLessThanMinDuration: (() -> Unit)? = null

    private lateinit var configuration: CameraViewConfiguration

    private var activeAnimator: ValueAnimator? = null

    private var isVideoRecording = false

    private val mediaMetadataRetriever = MediaMetadataRetriever()

    private var videoDuration = 0

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

                    mediaMetadataRetriever.setDataSource(videoPath)

                    mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toIntOrNull()?.let {
                        videoDuration = it

                        if (it >= configuration.videoMinDuration) {
                            showPreviewView()
                            previewView.video = videoPath
                        }
                        else {
                            onRecordDurationLessThanMinDuration?.invoke()
                        }
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
                    previewView.photo = it.bitmap
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
                    val photo = previewView.photo
                    val video = previewView.video
                    hidePreviewView()
                    submit(photo, video)
                }

            }
        }

        captureButton.callback = circleViewCallback
        cancelButton.callback = circleViewCallback
        submitButton.callback = circleViewCallback

        exitButton.setOnClickListener {
            onExit?.invoke()
        }

        flipButton.setOnClickListener {
            captureView.toggleFacing()
        }

        flashButton.setOnClickListener {

            captureView.toggleFlash()

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

        captureView.setVideoBitRate(configuration.videoBitRate)

        captureView.setJpegQuality((configuration.photoQuality * 100).toInt())

    }

    fun start() {
        captureView.start()
    }

    fun stop() {
        captureView.stop()
    }

    fun requestPermissions(): Boolean {
        return configuration.requestPermissions(
            listOf(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            PERMISSION_REQUEST_CODE
        )
    }

    fun requestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        if (requestCode != PERMISSION_REQUEST_CODE) {
            return
        }

        for (i in 0 until permissions.size) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                onPermissionsDenied?.invoke()
                return
            }
        }

        onPermissionsGranted?.invoke()

    }

    private fun checkExternalStorageAvailable(): Boolean {
        val state = Environment.getExternalStorageState()
        return state == Environment.MEDIA_MOUNTED
    }

    private fun startRecordVideo() {

        if (isVideoRecording) {
            return
        }

        if (!requestPermissions()) {
            onCaptureWithoutPermissions?.invoke()
            return
        }

        if (!checkExternalStorageAvailable()) {
            onRecordWithoutExternalStorage?.invoke()
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

    private fun stopRecordVideo() {

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

    private fun capturePhoto() {

        if (!requestPermissions()) {
            onCaptureWithoutPermissions?.invoke()
            return
        }

        if (!checkExternalStorageAvailable()) {
            onRecordWithoutExternalStorage?.invoke()
            return
        }

        captureView.captureImage()

    }

    private fun submit(photo: Bitmap?, videoPath: String) {

        if (photo != null) {
            val photoFile = saveToDisk(photo)
            onCapturePhoto?.invoke(photoFile.absolutePath, photoFile.length(), photo.width, photo.height)
        }
        else {
            val firstFrame = mediaMetadataRetriever.frameAtTime
            val videoFile = File(videoPath)
            val photoFile = saveToDisk(firstFrame)
            onRecordVideo?.invoke(
                videoFile.absolutePath, videoFile.length(), videoDuration,
                photoFile.absolutePath, photoFile.length(), firstFrame.width, firstFrame.height
            )
        }
    }

    private fun saveToDisk(bitmap: Bitmap): File {

        val dirname = context.externalCacheDir.absoluteFile
        val filename = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US).format(Date())

        val file = File("$dirname/$filename.jpg")
        val outputStream = FileOutputStream(file)

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()

        return file

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

        previewView.photo = null
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