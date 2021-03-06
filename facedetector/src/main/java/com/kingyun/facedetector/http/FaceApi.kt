package com.kingyun.facedetector.http

import com.kingyun.facedetector.ApiKeyPart
import com.kingyun.facedetector.ApiSecretPart
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface FaceApi {

  @Multipart @POST("faceset/create")
  fun create(
      @Part api_key: ApiKeyPart,
      @Part api_secret: ApiSecretPart,
      @Part outer_id: MultipartBody.Part,
      @Part face_tokens: MultipartBody.Part,
      @Part force_merge: MultipartBody.Part = MultipartBody.Part.createFormData("force_merge",
          "1")
  ): Call<FacesetOperateResponse>

  @Multipart @POST("faceset/addface")
  fun addFace(
      @Part api_key: ApiKeyPart,
      @Part api_secret: ApiSecretPart,
      @Part outer_id: MultipartBody.Part,
      @Part face_tokens: MultipartBody.Part): Call<FacesetOperateResponse>

  @Multipart @POST("face/setuserid")
  fun setUserId(
      @Part api_key: ApiKeyPart,
      @Part api_secret: ApiSecretPart,
      @Part user_id: MultipartBody.Part,
      @Part face_token: MultipartBody.Part): Call<FacesetOperateResponse>
}