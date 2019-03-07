package com.github.herokotlin.cameraview

import android.content.Context
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
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

        camera.addCameraKitListener(object: CameraKitEventListener {
            override fun onVideo(event: CameraKitVideo?) {
                event?.videoFile
            }

            override fun onEvent(event: CameraKitEvent?) {
                if (event == null) {
                    return
                }

            }

            override fun onImage(event: CameraKitImage?) {
                event?.bitmap
            }

            override fun onError(event: CameraKitError?) {
                Log.e(TAG, "${event?.exception}")
            }
        })

        captureButton.callback = object: CircleViewCallback {
            override fun onLongPressEnd(circleView: CircleView) {
                stopRecordVideo()
            }

            override fun onLongPressStart(circleView: CircleView) {
                startRecordVideo()
            }

            override fun onTouchDown(circleView: CircleView) {
                circleView.centerColor = ContextCompat.getColor(context, R.color.camera_view_capture_button_center_color_pressed)
                circleView.invalidate()
            }

            override fun onTouchEnter(circleView: CircleView) {
                circleView.centerColor = ContextCompat.getColor(context, R.color.camera_view_capture_button_center_color_pressed)
                circleView.invalidate()
            }

            override fun onTouchLeave(circleView: CircleView) {
                circleView.centerColor = ContextCompat.getColor(context, R.color.camera_view_capture_button_center_color_normal)
                circleView.invalidate()
            }

            override fun onTouchUp(circleView: CircleView, inside: Boolean, isLongPress: Boolean) {
                if (inside) {
                    circleView.centerColor = ContextCompat.getColor(context, R.color.camera_view_capture_button_center_color_normal)
                    circleView.invalidate()
                }
                if (!inside || isLongPress) {
                    return
                }
                camera.captureImage()
            }
        }

        exitButton.setOnClickListener {

        }

        flipButton.setOnClickListener {
            camera.toggleFacing()
        }

        flashButton.setOnClickListener {

            camera.flash = when (camera.flash) {
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
                when (camera.flash) {
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

    fun start() {
        camera.start()
    }

    fun stop() {
        camera.stop()
    }

    fun startRecordVideo() {
        camera.captureVideo()
    }

    fun stopRecordVideo() {
        camera.stopVideo()
    }

}