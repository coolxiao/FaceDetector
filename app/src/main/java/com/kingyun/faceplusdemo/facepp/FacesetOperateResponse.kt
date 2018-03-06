package com.kingyun.faceplusdemo.facepp

class FacesetOperateResponse(
    val request_id: String,
    val time_used: Int,
    val error_message: String?,
    val faceset_token: String?,
    val outer_id: String?,
    val face_count: Int,
    val face_added: Int,
    val face_removed: Int,
    val failure_detail: List<FailureDetail>?
)