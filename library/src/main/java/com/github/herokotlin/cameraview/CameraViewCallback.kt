package com.github.herokotlin.cameraview

import android.app.Activity
import com.github.herokotlin.cameraview.model.Photo
import com.github.herokotlin.cameraview.model.Video

interface CameraViewCallback {

    fun onExit(activity: Activity)

    fun onCapturePhoto(activity: Activity, photo: Photo)

    fun onRecordVideo(activity: Activity, video: Video, photo: Photo)

    // 录制视频时间太短
    fun onRecordDurationLessThanMinDuration(activity: Activity) {

    }

}