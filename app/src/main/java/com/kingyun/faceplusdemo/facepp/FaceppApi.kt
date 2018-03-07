package com.kingyun.faceplusdemo.facepp

import com.kingyun.facedetector.http.DetectResponse
import com.kingyun.facedetector.http.FacesetOperateResponse
import com.kingyun.facedetector.http.SearchResponse

interface FaceppApi {
  @Multipart @POST("detect")
  fun detectFaceForm(@Part api_key: MultipartBody.Part, @Part api_secret: MultipartBody.Part,
      @Part image_file: MultipartBody.Part): Call<DetectResponse>

  @Multipart @POST("search")
  fun searchFaceForm(@Part api_key: MultipartBody.Part, @Part api_secret: MultipartBody.Part, @Part
  faceset_token: MultipartBody.Part, @Part image_file: MultipartBody.Part): Call<SearchResponse>

  @Multipart @POST("faceset/addface")
  fun addFace(@Part api_key: MultipartBody.Part, @Part api_secret: MultipartBody.Part, @Part
  outer_id: MultipartBody.Part, @Part face_tokens: MultipartBody.Part): Call<FacesetOperateResponse>

}