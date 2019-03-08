package com.github.herokotlin.cameraview

import android.app.Activity

interface CameraViewCallback {

    fun onExit(activity: Activity)

    // 拉取文件数据时，发现没权限
    fun onCaptureWithoutPermissions(activity: Activity) {

    }

    // 录制视频时间太短
    fun onRecordDurationLessThanMinDuration() {

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