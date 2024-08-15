package com.github.herokotlin.cameraview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.AttributeSet
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import com.github.herokotlin.cameraview.databinding.CameraViewBinding
import com.github.herokotlin.cameraview.enum.CaptureMode
import com.github.herokotlin.cameraview.model.Photo
import com.github.herokotlin.cameraview.model.Video
import com.github.herokotlin.circleview.CircleView
import com.github.herokotlin.circleview.CircleViewCallback
import com.otaliastudios.cameraview.CameraException
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.VideoResult
import com.otaliastudios.cameraview.controls.Audio
import com.otaliastudios.cameraview.controls.Flash
import com.otaliastudios.cameraview.controls.Mode
import com.otaliastudios.cameraview.controls.PictureFormat
import com.otaliastudios.cameraview.gesture.Gesture
import com.otaliastudios.cameraview.gesture.GestureAction
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*

class CameraView: RelativeLayout {

    companion object {
        const val TAG = "CameraView"
    }

    private lateinit var binding: CameraViewBinding

    var onExit: (() -> Unit)? = null

    var onCapturePhoto: ((Photo) -> Unit)? = null

    var onRecordVideo: ((Video, Photo) -> Unit)? = null

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

        binding = CameraViewBinding.inflate(LayoutInflater.from(context), this, true)

        binding.captureView.addCameraListener(object: CameraListener() {
            override fun onPictureTaken(result: PictureResult) {
                super.onPictureTaken(result)

                isBusy = false

                result.toBitmap {
                    if (it != null) {
                        showPreviewView()
                        binding.previewView.photo = it
                    }
                }

            }

            override fun onVideoTaken(result: VideoResult) {
                super.onVideoTaken(result)

                isBusy = false
                isVideoRecording = false

                val videoPath = result.file.absolutePath
                mediaMetadataRetriever.setDataSource(videoPath)

                val duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toIntOrNull()
                if (duration != null) {
                    videoDuration = duration

                    if (duration >= configuration.videoMinDuration) {
                        showPreviewView()
                        binding.previewView.video = videoPath
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
                if (circleView != binding.captureButton) {
                    return
                }
                if (configuration.captureMode != CaptureMode.PHOTO) {
                    startRecordVideo()
                }
            }

            override fun onLongPressEnd(circleView: CircleView) {
                if (circleView != binding.captureButton) {
                    return
                }
                stopRecordVideo()
            }

            override fun onTouchDown(circleView: CircleView) {

                if (circleView != binding.captureButton) {
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
                if (circleView != binding.captureButton) {
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
                if (circleView != binding.captureButton) {
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

                if (inside && circleView == binding.captureButton) {
                    circleView.centerColor =
                        ContextCompat.getColor(context, R.color.camera_view_capture_button_center_color_normal)
                    circleView.invalidate()
                }

                if (!inside || isLongPress) {
                    return
                }

                if (circleView == binding.captureButton) {
                    if (isBusy) {
                        return
                    }
                    // 纯视频拍摄需要长按
                    if (configuration.captureMode != CaptureMode.VIDEO) {
                        capturePhoto()
                    }
                }
                else if (circleView == binding.cancelButton) {
                    hidePreviewView()
                }
                else if (circleView == binding.submitButton) {
                    val photo = binding.previewView.photo
                    val video = binding.previewView.video
                    hidePreviewView()
                    submit(photo, video)
                }

            }
        }

        binding.captureButton.callback = circleViewCallback
        binding.cancelButton.callback = circleViewCallback
        binding.submitButton.callback = circleViewCallback

        binding.exitButton.setOnClickListener {
            onExit?.invoke()
        }

        binding.flipButton.setOnClickListener {
            binding.captureView.toggleFacing()
        }

        binding.flashButton.setOnClickListener {

            when (binding.captureView.flash) {
                Flash.AUTO -> {
                    binding.captureView.flash = Flash.ON
                    binding.flashButton.setImageResource(R.drawable.camera_view_flash_auto)
                }
                Flash.ON -> {
                    binding.captureView.flash = Flash.OFF
                    binding.flashButton.setImageResource(R.drawable.camera_view_flash_on)
                }
                else -> {
                    binding.captureView.flash = Flash.AUTO
                    binding.flashButton.setImageResource(R.drawable.camera_view_flash_off)
                }
            }

        }

    }

    fun init(configuration: CameraViewConfiguration) {

        this.configuration = configuration

        if (configuration.guideLabelTitle.isNotEmpty()) {
            binding.guideLabel.visibility = View.VISIBLE
            binding.guideLabel.text = configuration.guideLabelTitle
            if (configuration.guideLabelFadeOutDelay > 0) {
                postDelayed({
                    onGuideLabelFadeOut()
                }, configuration.guideLabelFadeOutDelay * 1000)
            }
        }

        binding.captureView.mapGesture(Gesture.PINCH, GestureAction.ZOOM)
        binding.captureView.mapGesture(Gesture.TAP, GestureAction.AUTO_FOCUS)

        binding.captureView.audio = if (configuration.captureMode == CaptureMode.PHOTO) {
            Audio.OFF
        } else {
            Audio.ON
        }

        binding.captureView.videoBitRate = configuration.videoBitRate
        binding.captureView.audioBitRate = configuration.audioBitRate

        binding.captureView.pictureFormat = PictureFormat.JPEG

    }

    fun open() {
        binding.captureView.open()
    }

    fun close() {
        binding.captureView.close()
    }

    fun destroy() {
        binding.captureView.destroy()
    }

    private fun startRecordVideo() {

        if (isBusy) {
            return
        }

        if (isVideoRecording) {
            return
        }

        binding.captureView.mode = Mode.VIDEO
        // 异步，怕卡住
        post {
            binding.captureView.takeVideoSnapshot(
                File(getFilePath(".mp4"))
            )
        }

        binding.captureButton.centerRadius = resources.getDimensionPixelSize(R.dimen.camera_view_capture_button_center_radius_recording)
        binding.captureButton.ringWidth = resources.getDimensionPixelSize(R.dimen.camera_view_capture_button_ring_width_recording)
        binding.captureButton.requestLayout()

        startAnimation(
            configuration.videoMaxDuration,
            LinearInterpolator(),
            {
                // 避免结束时还无法到达满圆
                binding.captureButton.trackValue = if (it > 0.99) 1f else it
                binding.captureButton.invalidate()
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

        binding.captureButton.centerRadius = resources.getDimensionPixelSize(R.dimen.camera_view_capture_button_center_radius_normal)
        binding.captureButton.ringWidth = resources.getDimensionPixelSize(R.dimen.camera_view_capture_button_ring_width_normal)
        binding.captureButton.trackValue = 0f
        binding.captureButton.requestLayout()

        // 异步，怕卡住
        post {
            binding.captureView.stopVideo()
        }

        activeAnimator?.cancel()

    }

    private fun capturePhoto() {

        if (isBusy) {
            return
        }

        // 此时开始等照片的回调
        isBusy = true

        binding.captureView.mode = Mode.PICTURE

        // 异步，怕卡住
        post {
            binding.captureView.takePictureSnapshot()
        }

    }

    private fun submit(photo: Bitmap?, videoPath: String) {

        if (photo != null) {
            onCapturePhoto?.invoke(saveToDisk(photo, 100))
        }
        else {
            mediaMetadataRetriever.frameAtTime?.let {
                val videoFile = File(videoPath)
                onRecordVideo?.invoke(
                    Video(videoFile.absolutePath, videoFile.length(), videoDuration),
                    saveToDisk(it, 80)
                )
            }
        }
    }

    private fun saveToDisk(bitmap: Bitmap, quality: Int): Photo {

        val file = File(getFilePath(".jpg"))

        val byteOutput = ByteArrayOutputStream()
        val fileOutput = FileOutputStream(file)

        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteOutput)

        var base64 = ""
        if (configuration.photoBase64Enabled) {
            base64 = Base64.encodeToString(byteOutput.toByteArray(), Base64.DEFAULT)
        }

        byteOutput.writeTo(fileOutput)

        fileOutput.flush()
        fileOutput.close()
        byteOutput.close()

        return Photo(file.absolutePath, base64, file.length(), bitmap.width, bitmap.height)

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

        if (isGuideLabelFadingOut || binding.guideLabel.visibility == View.GONE) {
            return
        }

        isGuideLabelFadingOut = true

        startAnimation(
            1000,
            LinearInterpolator(),
            {
                binding.guideLabel.alpha = 1 - it
            },
            {
                binding.guideLabel.visibility = View.GONE
                isGuideLabelFadingOut = false
            }
        )

    }

    private fun showPreviewView() {

        val chooseLayoutParams = binding.chooseView.layoutParams

        startAnimation(
            200,
            LinearInterpolator(),
            {
                val alpha = 1 - it

                chooseLayoutParams.width = (chooseViewWidth * it).toInt()
                binding.chooseView.alpha = it
                binding.chooseView.requestLayout()

                binding.flashButton.alpha = alpha
                binding.flipButton.alpha = alpha
                binding.exitButton.alpha = alpha
                binding.captureButton.alpha = alpha
            },
            {
                binding.flashButton.visibility = View.GONE
                binding.flipButton.visibility = View.GONE
                binding.exitButton.visibility = View.GONE
                binding.captureButton.visibility = View.GONE
            }
        )

        binding.previewView.visibility = View.VISIBLE
        binding.captureView.visibility = View.GONE

    }

    private fun hidePreviewView() {

        val chooseLayoutParams = binding.chooseView.layoutParams

        startAnimation(
            200,
            LinearInterpolator(),
            {
                val alpha = 1 - it

                chooseLayoutParams.width = (chooseViewWidth * alpha).toInt()
                binding.chooseView.alpha = alpha
                binding.chooseView.requestLayout()

                binding.flashButton.alpha = it
                binding.flipButton.alpha = it
                binding.exitButton.alpha = it
                binding.captureButton.alpha = it
            }
        )

        binding.captureView.visibility = View.VISIBLE
        binding.flashButton.visibility = View.VISIBLE
        binding.flipButton.visibility = View.VISIBLE
        binding.exitButton.visibility = View.VISIBLE
        binding.captureButton.visibility = View.VISIBLE

        binding.previewView.visibility = View.GONE

        binding.previewView.photo = null
        binding.previewView.video = ""

    }

    private fun showControls() {
        binding.flipButton.visibility = View.VISIBLE
        binding.flashButton.visibility = View.VISIBLE
        binding.exitButton.visibility = View.VISIBLE
    }

    private fun hideControls() {
        binding.flipButton.visibility = View.GONE
        binding.flashButton.visibility = View.GONE
        binding.exitButton.visibility = View.GONE
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
            override fun onAnimationEnd(animation: Animator) {
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