package com.kingyun.facedetector.http

import com.kingyun.facedetector.model.Face

data class DetectResponse(
    val request_id: String,
    val time_used: Int,
    val error_message: String?,
    val image_id:String?,
    val faces: List<Face>)