package com.kingyun.facedetector

import android.os.Handler
import android.os.Looper
import com.kingyun.facedetector.FaceServer.HttpResult
import com.kingyun.facedetector.http.CommonApi
import com.kingyun.facedetector.http.DetectResponse
import com.kingyun.facedetector.http.FaceApi
import com.kingyun.facedetector.http.FacesetOperateResponse
import com.kingyun.facedetector.http.FilePart
import com.kingyun.facedetector.http.SearchResponse
import com.kingyun.facedetector.http.apiKeyPart
import com.kingyun.facedetector.http.apiSecretPart
import com.kingyun.facedetector.http.createService
import com.kingyun.facedetector.http.outerIdPart
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

typealias SimpleHttpResult = HttpResult<Any>

object FaceServer {
  private val handler = Handler(Looper.getMainLooper())

  fun detect(imageFile: FilePart, callback: (HttpResult<DetectResponse>) -> Unit) {
    createService(CommonApi::class.java)
        .detectFace(apiKeyPart, apiSecretPart, imageFile)
        .enqueue(object : Callback<DetectResponse?> {
          override fun onFailure(call: Call<DetectResponse?>?, t: Throwable?) {
            t?.printStackTrace()
            val reason = t?.javaClass?.simpleName
            runOnUiThread { callback(HttpResult(false, "连接服务器失败: " + reason)) }
          }

          override fun onResponse(call: Call<DetectResponse?>?,
              response: Response<DetectResponse?>?) {
            if (response?.errorBody() != null) {
              val errorString = response.errorBody()?.string()
              runOnUiThread { callback(HttpResult(false, "发生错误: " + errorString)) }
              return
            }

            if (response?.body()?.faces?.isEmpty() != false) {
              runOnUiThread { callback(HttpResult(false, "没有找到人脸")) }
              return
            }

            val body = response.body()
            runOnUiThread { callback(HttpResult(true, data = body)) }
          }
        })
  }

  fun search(outerId: MultipartBody.Part, imageFile: FilePart,
      callback: (HttpResult<SearchResponse>) -> Unit) {
    createService(CommonApi::class.java)
        .searchFace(apiKeyPart, apiSecretPart, outerId, imageFile)
        .enqueue(object : Callback<SearchResponse?> {
          override fun onFailure(call: Call<SearchResponse?>?, t: Throwable?) {
            t?.printStackTrace()
            val reason = t?.javaClass?.simpleName
            runOnUiThread { callback(HttpResult(false, "连接服务器失败: " + reason)) }
          }

          override fun onResponse(call: Call<SearchResponse?>?,
              response: Response<SearchResponse?>?) {
            if (response?.errorBody() != null) {
              val errorString = response.errorBody()?.string()
              runOnUiThread { callback(HttpResult(false, "发生错误: " + errorString)) }
              return
            }
            if (response?.body()?.faces?.isEmpty() != false) {
              runOnUiThread { callback(HttpResult(false, "没有找到人脸")) }
              return
            }

            val body = response.body()
            runOnUiThread { callback(HttpResult(true, data = body)) }
          }
        })
  }

  fun addFace(imageFile: FilePart, callback: (SimpleHttpResult) -> Unit) {
    detect(imageFile) {
      if (it.success) {
        val faces = it.data?.faces
        val strBuilder = StringBuilder()
        faces?.forEach {
          if (strBuilder.isNotEmpty()) {
            strBuilder.append(",")
          }
          strBuilder.append(it.face_token)
        }
        if (strBuilder.isEmpty()) {
          callback(SimpleHttpResult(false, "没有找到人脸"))
        } else {
          addFaceset(strBuilder.toString(), callback)
        }
      } else {
        callback(it)
      }
    }
  }

  fun addFaceset(faceTokens: String, callback: (SimpleHttpResult) -> Unit) {
    createService(FaceApi::class.java)
        .addFace(apiKeyPart, apiSecretPart, outerIdPart,
            MultipartBody.Part.createFormData("face_tokens", faceTokens))
        .enqueue(object : Callback<FacesetOperateResponse?> {
          override fun onFailure(call: Call<FacesetOperateResponse?>?,
              t: Throwable?) {
            t?.printStackTrace()
            val reason = t?.javaClass?.simpleName
            runOnUiThread { callback(HttpResult(false, "连接服务器失败: " + reason)) }
          }

          override fun onResponse(call: Call<FacesetOperateResponse?>?,
              response: Response<FacesetOperateResponse?>?) {
            if (response?.errorBody() != null) {
              val errorString = response.errorBody()?.string()
              runOnUiThread { callback(HttpResult(false, "发生错误: " + errorString)) }
              return
            }
            response?.body()?.let {
              if (it.face_added > 0) {
                runOnUiThread { callback(HttpResult(true, "添加人脸成功")) }
              }
            }
          }
        })
  }

  fun runOnUiThread(receiver: () -> Unit) {
    if (Looper.getMainLooper().thread == Thread.currentThread()) {
      receiver()
    } else {
      handler.post { receiver() }
    }
  }

  class HttpResult<out T>(val success: Boolean, val message: String? = null, val data: T? = null)
}