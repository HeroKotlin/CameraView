package com.github.herokotlin.cameraview.model

data class Photo(
    val path: String,
    val base64: String,
    val size: Long,
    val width: Int,
    val height: Int,
)