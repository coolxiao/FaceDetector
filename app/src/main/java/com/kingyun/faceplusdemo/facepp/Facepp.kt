package com.kingyun.faceplusdemo.facepp

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object Facepp {
  const val API_KEY = "UVwuENf-Uy1HeJelOW2niyyCVrLsNz93"
  const val API_SECRET = "CB7CQrPseo24N6g64k_gmnH7rX5IA7fW"

  lateinit var retrofit: Retrofit

  fun init() {
    val logging = HttpLoggingInterceptor()
    logging.level = HttpLoggingInterceptor.Level.BODY
    val httpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .readTimeout(30_000, TimeUnit.MILLISECONDS)
        .writeTimeout(30_000, TimeUnit.MILLISECONDS)
        .connectTimeout(30_000, TimeUnit.MILLISECONDS)
        .build()

    val gson = GsonBuilder().create()
    retrofit = Retrofit.Builder()
        .client(httpClient)
        .baseUrl("https://api-cn.faceplusplus.com/facepp/v3/")
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
  }
}