package com.github.herokotlin.cameraview.view

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.github.herokotlin.cameraview.databinding.CameraViewPreviewViewBinding
import androidx.core.net.toUri

class PreviewView: FrameLayout {

    private lateinit var binding: CameraViewPreviewViewBinding

    var photo: Bitmap? = null

        set(value) {

            if (field == value) {
                return
            }

            field = value

            if (value == null) {
                binding.imageView.visibility = View.GONE
            }
            else {
                binding.imageView.visibility = View.VISIBLE
                binding.imageView.setImageBitmap(value)
            }

        }

    var video = ""

        set(value) {

            if (field == value) {
                return
            }

            field = value

            binding.videoView.stopPlayback()
            binding.videoView.visibility = View.GONE

            if (value.isNotEmpty()) {
                binding.videoView.visibility = View.VISIBLE
                binding.videoView.setVideoURI(value.toUri())
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
        binding = CameraViewPreviewViewBinding.inflate(LayoutInflater.from(context), this, true)

        binding.videoView.setOnPreparedListener {
            it.isLooping = true
            it.start()
        }
    }

}