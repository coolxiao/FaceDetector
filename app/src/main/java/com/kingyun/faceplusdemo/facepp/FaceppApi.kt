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
  fun searchFaceForm(@Part("api_key") api_key: String, @Part("api_secret") api_secret: String,
      @Part image_file: MultipartBody.Part, @Part("faceset_token")
      faceset_token: String): Call<SearchResponse>

  @POST("search")
  fun searchFace(@Body request: SearchRequest): Call<SearchResponse>

//  @POST("faceset/create")
//  fun createFaceset(@Body request: CreateFaceset): Call<FacesetOperateResponse>

  @POST("faceset/addface")
  fun addFace(@Body request: AddFaceRequest): Call<FacesetOperateResponse>

}