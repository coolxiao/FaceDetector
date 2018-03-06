package com.kingyun.faceplusdemo

import android.support.v4.util.LruCache
import okhttp3.OkHttpClient
import retrofit2.Retrofit

/**
 * Created by xifan on 18-3-2.
 */
object Http {
  lateinit var retrofit: Retrofit
  lateinit var httpClient: OkHttpClient

  private val apiCache: LruCache<String, Any> = LruCache(10)

  fun initRetrofit(retrofit: Retrofit) {
    Http.retrofit = retrofit
    apiCache.evictAll()
  }

  fun initClient(okHttpClient: OkHttpClient) {
    httpClient = okHttpClient
  }

  @Suppress("UNCHECKED_CAST")
  fun <T> createRetroService(clazz: Class<T>): T {
    var classT: T = apiCache[clazz.name] as T
    if (classT == null) {
      classT = retrofit.create(clazz)
      apiCache.put(clazz.name, classT)
    }
    return classT
  }

}