package com.github.herokotlin.cameraview.view

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.github.herokotlin.cameraview.R
import kotlinx.android.synthetic.main.camera_view_preview_view.view.*

class PreviewView: FrameLayout {

    var photo: Bitmap? = null

        set(value) {

            if (field == value) {
                return
            }

            field = value

            if (value == null) {
                imageView.visibility = View.GONE
            }
            else {
                imageView.visibility = View.VISIBLE
                imageView.setImageBitmap(value)
            }

        }

    var video = ""

        set(value) {

            if (field == value) {
                return
            }

            field = value

            videoView.stopPlayback()
            videoView.visibility = View.GONE

            if (value.isNotEmpty()) {
                videoView.visibility = View.VISIBLE
                videoView.setVideoURI(Uri.parse(value))
            }

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
        LayoutInflater.from(context).inflate(R.layout.camera_view_preview_view, this)

        videoView.setOnPreparedListener {

            it.isLooping = true
            it.start()

            val ratio = videoView.width.toFloat() / it.videoWidth

            // 按比例缩放
            videoView.layoutParams.height = (it.videoHeight * ratio).toInt()

        }
    }

}