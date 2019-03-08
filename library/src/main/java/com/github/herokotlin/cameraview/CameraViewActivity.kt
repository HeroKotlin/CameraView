package com.github.herokotlin.cameraview

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.camera_view_activity.*

class CameraViewActivity: AppCompatActivity() {

    companion object {

        lateinit var callback: CameraViewCallback

        lateinit var configuration: CameraViewConfiguration

        fun newInstance(context: Activity) {
            val intent = Intent(context, CameraViewActivity::class.java)
            context.startActivity(intent)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        var flags = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            flags = flags or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }

        window.decorView.systemUiVisibility = flags

        setContentView(R.layout.camera_view_activity)

        cameraView.init(configuration)

        cameraView.onExit = {
            callback.onExit(this)
        }
        cameraView.onCapturePhoto = { photoPath, photoSize, photoWidth, photoHeight ->
            callback.onCapturePhoto(this, photoPath, photoSize, photoWidth, photoHeight)
        }
        cameraView.onRecordVideo = { videoPath, videoSize, videoDuration, photoPath, photoSize, photoWidth, photoHeight ->
            callback.onRecordVideo(this, videoPath, videoSize, videoDuration, photoPath, photoSize, photoWidth, photoHeight)
        }
        cameraView.onPermissionsGranted = {
            cameraView.start()
            callback.onPermissionsGranted(this)
        }
        cameraView.onPermissionsDenied = {
            callback.onPermissionsDenied(this)
        }
        cameraView.onRecordWithoutExternalStorage = {
            callback.onCaptureWithoutExternalStorage(this)
        }
        cameraView.onCaptureWithoutPermissions = {
            callback.onCaptureWithoutPermissions(this)
        }
        cameraView.onRecordDurationLessThanMinDuration = {
            callback.onRecordDurationLessThanMinDuration(this)
        }

        if (cameraView.requestPermissions()) {
            cameraView.start()
        }

    }

    override fun onResume() {
        super.onResume()
        cameraView.start()
    }

    override fun onPause() {
        super.onPause()
        cameraView.stop()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraView.requestPermissionsResult(requestCode, permissions, grantResults)
    }

}