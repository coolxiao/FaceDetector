package com.kingyun.faceplusdemo.facepp

data class DetectResponse(
    val request_id: String,
    val time_used: Int,
    val error_message: String?,
    val image_id:String?,
    val faces: List<Face>
    ) {


}