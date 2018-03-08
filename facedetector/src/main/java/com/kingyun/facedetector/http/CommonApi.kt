package com.kingyun.facedetector.http

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface CommonApi {
  @Multipart @POST("detect")
  fun detectFace(@Part api_key: ApiKeyPart, @Part api_secret: ApiSecretPart,
      @Part image_file: FilePart): Call<DetectResponse>

  @Multipart @POST("search")
  fun searchFace(@Part api_key: ApiKeyPart, @Part api_secret: ApiSecretPart, @Part
  outer_id: MultipartBody.Part, @Part image_file: FilePart): Call<SearchResponse>
}