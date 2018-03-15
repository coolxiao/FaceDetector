package com.kingyun.facedetector.http

import android.util.LruCache
import com.google.gson.GsonBuilder
import com.kingyun.facedetector.http.HttpKt.apiCache
import com.kingyun.facedetector.http.HttpKt.retrofit
import okhttp3.MediaType
import okhttp3.MultipartBody.Part
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

object HttpKt {
  private const val API_URL = "https://api-cn.faceplusplus.com/facepp/v3/"

  internal val apiCache: LruCache<String, Any> = LruCache(10)

  @JvmStatic val retrofit: Retrofit by lazy {
    val logging = HttpLoggingInterceptor()
    logging.level = HttpLoggingInterceptor.Level.BODY
    val httpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .readTimeout(30_000, TimeUnit.MILLISECONDS)
        .writeTimeout(30_000, TimeUnit.MILLISECONDS)
        .connectTimeout(30_000, TimeUnit.MILLISECONDS)
        .build()

    val gson = GsonBuilder().create()
    Retrofit.Builder()
        .client(httpClient)
        .baseUrl(API_URL)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
  }
}

fun createFileForm(name: String, file: File,
    mediaType: MediaType? = MediaType.parse("image/*")): Part {
  return Part.createFormData(name, file.name, RequestBody.create(mediaType, file))
}

fun <T> createService(clazz: Class<T>): T {
  var classT: T = apiCache[clazz.name] as T
  if (classT == null) {
    classT = retrofit.create(clazz)
    apiCache.put(clazz.name, classT)
  }
  return classT
}