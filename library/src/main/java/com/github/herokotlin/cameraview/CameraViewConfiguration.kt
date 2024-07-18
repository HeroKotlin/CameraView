package com.github.herokotlin.cameraview

import com.github.herokotlin.cameraview.enum.CaptureMode

abstract class CameraViewConfiguration {

    var guideLabelTitle = ""

    // 三秒后淡出文字
    var guideLabelFadeOutDelay = 3L

    // 图片是否需要返回 base64
    var photoBase64Enabled = true

    var videoBitRate = 0

    var audioBitRate = 0

    // 视频最短录制时长，单位是毫秒
    var videoMinDuration = 1000L

    // 视频最大录制时长，单位是毫秒
    var videoMaxDuration = 10 * 1000L

    // 拍摄模式
    var captureMode = CaptureMode.PHOTO_VIDEO

}