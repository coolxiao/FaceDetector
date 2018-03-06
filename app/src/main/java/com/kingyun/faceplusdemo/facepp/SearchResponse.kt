package com.kingyun.faceplusdemo.facepp


data class SearchResponse(
    val request_id: String,
    val time_used: Int,
    val error_message: String?,
    val results: List<Result>?,
    val thresholds: Thresholds?,
    val image_id:String?,
    val faces: List<Face>
) {
  data class Result(val confidence: Double, val user_id: String, val face_token: String)
  data class Thresholds(val `1e-3`: Double, val `1e-4`: Double, val `1e-5`: Double)
}