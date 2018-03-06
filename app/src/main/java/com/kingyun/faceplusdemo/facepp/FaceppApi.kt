package com.kingyun.faceplusdemo.facepp

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File

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