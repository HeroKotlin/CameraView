package com.github.herokotlin.cameraview

abstract class CameraViewConfiguration {

    var guideLabelTitle = ""

    var guideLabelFadeOutDelay = 3L

    // 视频最短录制时长
    var videoMinDuration = 1000L

    // 视频最大录制时长
    var videoMaxDuration = 10000L

}