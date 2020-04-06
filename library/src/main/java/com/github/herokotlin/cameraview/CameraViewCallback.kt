package com.github.herokotlin.cameraview

import android.app.Activity

interface CameraViewCallback {

    fun onExit(activity: Activity)

    fun onCapturePhoto(activity: Activity, photoPath: String, photoSize: Long, photoWidth: Int, photoHeight: Int)

    fun onRecordVideo(activity: Activity, videoPath: String, videoSize: Long, videoDuration: Int, photoPath: String, photoSize: Long, photoWidth: Int, photoHeight: Int)

    // 录制视频时间太短
    fun onRecordDurationLessThanMinDuration(activity: Activity) {

    }

}