package com.kingyun.faceplusdemo.facepp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object Facepp {
//  const val API_KEY = "UVwuENf-Uy1HeJelOW2niyyCVrLsNz93"
//  const val API_SECRET = "CB7CQrPseo24N6g64k_gmnH7rX5IA7fW"

  const val API_KEY = "_UPk1bWhAldRBsqoDqPoQxxhqVLpqpbG"
  const val API_SECRET = "7-yV_GKRTYmxGI7bPUUu-nZ_JjQnaZDz"
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

  fun portraitBitmap(imageByteArray: ByteArray, rotate: Float = -90F): Bitmap {
    val sourceBmp = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.size)
    return portraitBitmap(sourceBmp, rotate)
  }

  fun portraitBitmap(bitmap: Bitmap, rotate: Float = -90F): Bitmap {
    val matrix = Matrix().apply {
      postRotate(rotate)
      postScale(-1F, 1F)
      postTranslate(bitmap.width.toFloat(), 0F)
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
  }
}