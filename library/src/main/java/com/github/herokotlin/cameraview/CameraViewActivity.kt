package com.github.herokotlin.cameraview

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import com.github.herokotlin.cameraview.databinding.CameraViewActivityBinding

class CameraViewActivity: AppCompatActivity() {

    companion object {

        lateinit var callback: CameraViewCallback

        lateinit var configuration: CameraViewConfiguration

        fun newInstance(context: Activity) {
            val intent = Intent(context, CameraViewActivity::class.java)
            context.startActivity(intent)
        }

    }

    private lateinit var binding: CameraViewActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        var flags = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            flags = flags or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }

        window.decorView.systemUiVisibility = flags

        binding = CameraViewActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cameraView.init(configuration)

        binding.cameraView.activity = this
        binding.cameraView.onExit = {
            callback.onExit(this)
        }
        binding.cameraView.onCapturePhoto = { photo ->
            callback.onCapturePhoto(this, photo)
        }
        binding.cameraView.onRecordVideo = { video, photo ->
            callback.onRecordVideo(this, video, photo)
        }
        binding.cameraView.onRecordDurationLessThanMinDuration = {
            callback.onRecordDurationLessThanMinDuration(this)
        }

    }

    override fun onResume() {
        super.onResume()
        binding.cameraView.open()
    }

    override fun onPause() {
        super.onPause()
        binding.cameraView.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.cameraView.destroy()
    }

}