package com.github.herokotlin.cameraview

import android.app.Activity

interface CameraViewCallback {

    fun onExit(activity: Activity)

    fun onCapturePhoto(activity: Activity, photoPath: String, photoSize: Long, photoWidth: Int, photoHeight: Int)

    fun onRecordVideo(activity: Activity, videoPath: String, videoSize: Long, videoDuration: Int, photoPath: String, photoSize: Long, photoWidth: Int, photoHeight: Int)

    // 拍摄照片或视频时，发现没权限
    fun onCaptureWithoutPermissions(activity: Activity) {

    }

    // 录制视频时间太短
    fun onRecordDurationLessThanMinDuration(activity: Activity) {

    }

    // 没有外部存储可用
    fun onCaptureWithoutExternalStorage(activity: Activity) {

    }

    // 用户点击同意授权
    fun onPermissionsGranted(activity: Activity) {

    }

    // 用户点击拒绝授权
    fun onPermissionsDenied(activity: Activity) {

    }

}