package com.github.herokotlin.cameraview

import com.github.herokotlin.cameraview.enum.CaptureMode
import com.github.herokotlin.cameraview.enum.VideoQuality

abstract class CameraViewConfiguration {

    var guideLabelTitle = ""

    // 三秒后淡出文字
    var guideLabelFadeOutDelay = 3L

    // 视频质量
    var videoQuality = VideoQuality.P720

    // 视频最短录制时长，单位是毫秒
    var videoMinDuration = 1000L

    // 视频最大录制时长，单位是毫秒
    var videoMaxDuration = 10000L

    // 拍摄模式
    var captureMode = CaptureMode.PHOTO

}