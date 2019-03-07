package com.github.herokotlin.cameraview

import android.animation.Animator
import android.content.Context
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.github.herokotlin.circleview.CircleView
import com.github.herokotlin.circleview.CircleViewCallback
import com.wonderkiln.camerakit.*
import kotlinx.android.synthetic.main.camera_view.view.*


class CameraView: FrameLayout {

    companion object {
        const val TAG = "CameraView"
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
                    showPreviewView()
                    previewView.video = it.videoFile.absolutePath
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
                if (circleView == captureButton) {
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
                    capturePhoto()
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

        if (configuration.guideLabelTitle.isNotEmpty()) {
            guideLabel.visibility = View.VISIBLE
            guideLabel.text = configuration.guideLabelTitle
            if (configuration.guideLabelFadeOutDelay > 0) {
                postDelayed({
                    onGuideLabelFadeOut()
                }, configuration.guideLabelFadeOutDelay * 1000)
            }
        }

    }

    fun start() {
        captureView.start()
    }

    fun stop() {
        captureView.stop()
    }

    fun startRecordVideo() {
        captureView.captureVideo()
    }

    fun stopRecordVideo() {
        captureView.stopVideo()
    }

    fun capturePhoto() {
        captureView.captureImage()
    }

    private fun onGuideLabelFadeOut() {
        guideLabel.animate().alpha(0f).setDuration(1000).setListener(object: Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {
                guideLabel.visibility = View.GONE
            }

            override fun onAnimationCancel(animation: Animator?) {

            }

            override fun onAnimationStart(animation: Animator?) {

            }
        }).start()
    }

    private fun showPreviewView() {

        previewView.visibility = View.VISIBLE

    }

    private fun hidePreviewView() {

        previewView.visibility = View.GONE
        previewView.image = null
        previewView.video = ""

    }

}