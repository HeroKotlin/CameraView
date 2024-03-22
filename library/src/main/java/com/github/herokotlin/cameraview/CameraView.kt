package com.github.herokotlin.cameraview

import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import com.github.herokotlin.cameraview.enum.CaptureMode
import com.github.herokotlin.circleview.CircleView
import com.github.herokotlin.circleview.CircleViewCallback
import com.otaliastudios.cameraview.CameraException
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.VideoResult
import com.otaliastudios.cameraview.controls.Flash
import com.otaliastudios.cameraview.controls.Mode
import com.otaliastudios.cameraview.controls.PictureFormat
import com.otaliastudios.cameraview.gesture.Gesture
import com.otaliastudios.cameraview.gesture.GestureAction
import kotlinx.android.synthetic.main.camera_view.view.*
import java.io.File
import java.io.FileOutputStream
import java.util.*

class CameraView: RelativeLayout {

    companion object {
        const val TAG = "CameraView"
    }

    var onExit: (() -> Unit)? = null

    var onCapturePhoto: ((String, Long, Int, Int) -> Unit)? = null

    var onRecordVideo: ((String, Long, Int, String, Long, Int, Int) -> Unit)? = null

    var onRecordDurationLessThanMinDuration: (() -> Unit)? = null

    // 用于请求权限
    var activity: Activity? = null

    private lateinit var configuration: CameraViewConfiguration

    private var activeAnimator: ValueAnimator? = null

    private var isGuideLabelFadingOut = false

    private var isVideoRecording = false

    // 是否忙于生成照片或视频
    private var isBusy = false

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

        captureView.addCameraListener(object: CameraListener() {
            override fun onPictureTaken(result: PictureResult) {
                super.onPictureTaken(result)

                isBusy = false

                result.toBitmap {
                    if (it != null) {
                        showPreviewView()
                        previewView.photo = it
                    }
                }

            }

            override fun onVideoTaken(result: VideoResult) {
                super.onVideoTaken(result)

                isBusy = false
                isVideoRecording = false

                val videoPath = result.file.absolutePath
                mediaMetadataRetriever.setDataSource(videoPath)

                val duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toIntOrNull()
                if (duration != null) {
                    videoDuration = duration

                    if (duration >= configuration.videoMinDuration) {
                        showPreviewView()
                        previewView.video = videoPath
                        return
                    }
                    onRecordDurationLessThanMinDuration?.invoke()
                }

                showControls()

            }

            override fun onCameraError(exception: CameraException) {
                super.onCameraError(exception)

                isBusy = false

                if (isVideoRecording) {
                    showControls()
                    isVideoRecording = false
                }

                Log.e(TAG, exception.localizedMessage)

            }
        })

        val circleViewCallback = object: CircleViewCallback {

            override fun onLongPressStart(circleView: CircleView) {
                if (circleView != captureButton) {
                    return
                }
                if (configuration.captureMode != CaptureMode.PHOTO) {
                    startRecordVideo()
                }
            }

            override fun onLongPressEnd(circleView: CircleView) {
                if (circleView != captureButton) {
                    return
                }
                stopRecordVideo()
            }

            override fun onTouchDown(circleView: CircleView) {

                if (circleView != captureButton) {
                    return
                }
                if (isBusy) {
                    return
                }

                circleView.centerColor =
                    ContextCompat.getColor(context, R.color.camera_view_capture_button_center_color_pressed)
                circleView.invalidate()

                // 如果提示文字还在显示，此时应直接淡出
                // 无需等时间到了再开始动画
                if (configuration.guideLabelFadeOutDelay > 0) {
                    onGuideLabelFadeOut()
                }
            }

            override fun onTouchEnter(circleView: CircleView) {
                if (circleView != captureButton) {
                    return
                }
                if (isBusy) {
                    return
                }
                circleView.centerColor =
                    ContextCompat.getColor(context, R.color.camera_view_capture_button_center_color_pressed)
                circleView.invalidate()
            }

            override fun onTouchLeave(circleView: CircleView) {
                if (circleView != captureButton) {
                    return
                }
                if (isBusy) {
                    return
                }
                circleView.centerColor =
                    ContextCompat.getColor(context, R.color.camera_view_capture_button_center_color_normal)
                circleView.invalidate()
            }

            override fun onTouchUp(circleView: CircleView, inside: Boolean, isLongPress: Boolean) {

                if (inside && circleView == captureButton) {
                    circleView.centerColor =
                        ContextCompat.getColor(context, R.color.camera_view_capture_button_center_color_normal)
                    circleView.invalidate()
                }

                if (!inside || isLongPress) {
                    return
                }

                if (circleView == captureButton) {
                    if (isBusy) {
                        return
                    }
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

            when (captureView.flash) {
                Flash.AUTO -> {
                    captureView.flash = Flash.ON
                    flashButton.setImageResource(R.drawable.camera_view_flash_auto)
                }
                Flash.ON -> {
                    captureView.flash = Flash.OFF
                    flashButton.setImageResource(R.drawable.camera_view_flash_on)
                }
                else -> {
                    captureView.flash = Flash.AUTO
                    flashButton.setImageResource(R.drawable.camera_view_flash_off)
                }
            }

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

        captureView.mapGesture(Gesture.PINCH, GestureAction.ZOOM)
        captureView.mapGesture(Gesture.TAP, GestureAction.AUTO_FOCUS)

        captureView.videoBitRate = configuration.videoBitRate
        captureView.audioBitRate = configuration.audioBitRate

        captureView.pictureFormat = PictureFormat.JPEG

    }

    fun open() {
        captureView.open()
    }

    fun close() {
        captureView.close()
    }

    fun destroy() {
        captureView.destroy()
    }

    private fun startRecordVideo() {

        if (isBusy) {
            return
        }

        if (isVideoRecording) {
            return
        }

        captureView.mode = Mode.VIDEO
        // 异步，怕卡住
        post {
            captureView.takeVideoSnapshot(
                File(getFilePath(".mp4"))
            )
        }

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
        hideControls()

    }

    private fun stopRecordVideo() {

        if (isBusy) {
            return
        }

        if (!isVideoRecording) {
            return
        }

        // 此时开始等视频的回调
        isBusy = true

        captureButton.centerRadius = resources.getDimensionPixelSize(R.dimen.camera_view_capture_button_center_radius_normal)
        captureButton.ringWidth = resources.getDimensionPixelSize(R.dimen.camera_view_capture_button_ring_width_normal)
        captureButton.trackValue = 0f
        captureButton.requestLayout()

        // 异步，怕卡住
        post {
            captureView.stopVideo()
        }

        activeAnimator?.cancel()

    }

    private fun capturePhoto() {

        if (isBusy) {
            return
        }

        // 此时开始等照片的回调
        isBusy = true

        captureView.mode = Mode.PICTURE

        // 异步，怕卡住
        post {
            captureView.takePictureSnapshot()
        }

    }

    private fun submit(photo: Bitmap?, videoPath: String) {

        if (photo != null) {
            val photoFile = saveToDisk(photo)
            onCapturePhoto?.invoke(photoFile.absolutePath, photoFile.length(), photo.width, photo.height)
        }
        else {
            mediaMetadataRetriever.frameAtTime?.let {
                val videoFile = File(videoPath)
                val photoFile = saveToDisk(it)
                onRecordVideo?.invoke(
                    videoFile.absolutePath, videoFile.length(), videoDuration,
                    photoFile.absolutePath, photoFile.length(), it.width, it.height
                )
            }
        }
    }

    private fun saveToDisk(bitmap: Bitmap): File {

        val file = File(getFilePath(".jpg"))
        val outputStream = FileOutputStream(file)

        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()

        return file

    }

    private fun getFilePath(extname: String): String {

        val cacheDir = context.externalCacheDir ?: return ""

        var dirname = cacheDir.absolutePath
        if (!dirname.endsWith(File.separator)) {
            dirname += File.separator
        }

        return dirname + UUID.randomUUID().toString() + extname

    }

    private fun onGuideLabelFadeOut() {

        if (isGuideLabelFadingOut || guideLabel.visibility == View.GONE) {
            return
        }

        isGuideLabelFadingOut = true

        startAnimation(
            1000,
            LinearInterpolator(),
            {
                guideLabel.alpha = 1 - it
            },
            {
                guideLabel.visibility = View.GONE
                isGuideLabelFadingOut = false
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
                exitButton.alpha = alpha
                captureButton.alpha = alpha
            },
            {
                flashButton.visibility = View.GONE
                flipButton.visibility = View.GONE
                exitButton.visibility = View.GONE
                captureButton.visibility = View.GONE
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
                exitButton.alpha = it
                captureButton.alpha = it
            }
        )

        captureView.visibility = View.VISIBLE
        flashButton.visibility = View.VISIBLE
        flipButton.visibility = View.VISIBLE
        exitButton.visibility = View.VISIBLE
        captureButton.visibility = View.VISIBLE

        previewView.visibility = View.GONE

        previewView.photo = null
        previewView.video = ""

    }

    private fun showControls() {
        flipButton.visibility = View.VISIBLE
        flashButton.visibility = View.VISIBLE
        exitButton.visibility = View.VISIBLE
    }

    private fun hideControls() {
        flipButton.visibility = View.GONE
        flashButton.visibility = View.GONE
        exitButton.visibility = View.GONE
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