package com.kingyun.facedetector

import android.os.Handler
import android.os.Looper
import com.kingyun.facedetector.FaceServer.API_KEY
import com.kingyun.facedetector.FaceServer.API_SECRET
import com.kingyun.facedetector.FaceServer.FACESET_OUTER_ID
import com.kingyun.facedetector.FaceServer.HttpResult
import com.kingyun.facedetector.http.CommonApi
import com.kingyun.facedetector.http.DetectResponse
import com.kingyun.facedetector.http.FaceApi
import com.kingyun.facedetector.http.FacesetOperateResponse
import com.kingyun.facedetector.http.SearchResponse
import com.kingyun.facedetector.http.createFileForm
import com.kingyun.facedetector.http.createService
import okhttp3.MultipartBody
import okhttp3.MultipartBody.Part
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

typealias SimpleHttpResult = HttpResult<Any>
typealias ApiKeyPart = Part
typealias ApiSecretPart = Part
typealias FilePart = Part

object FaceServer {
  internal var API_KEY = "_UPk1bWhAldRBsqoDqPoQxxhqVLpqpbG"
  internal var API_SECRET = "7-yV_GKRTYmxGI7bPUUu-nZ_JjQnaZDz"
  // TODO: 18-3-8 temporary outer id
  var FACESET_OUTER_ID = "som_set"
    private set

  private val handler = Handler(Looper.getMainLooper())

  /**
   * set server key and secret, call before [FaceDetector] init
   */
  @JvmStatic fun init(apiKey: String, apiSecret: String) {
    API_KEY = apiKey
    API_SECRET = apiSecret
  }

  /**
   * set server faceset outer id, call before [FaceDetector] init
   */
  @JvmStatic fun setOuterId(outerId: String) {
    FACESET_OUTER_ID = outerId
  }

  @JvmStatic fun detect(imageFile: File, callback: (HttpResult<DetectResponse>) -> Unit) {
    val filePart = createFileForm("image_file", imageFile)
    createService(CommonApi::class.java)
        .detectFace(apiKeyPart, apiSecretPart, filePart)
        .enqueue(object : Callback<DetectResponse?> {
          override fun onFailure(call: Call<DetectResponse?>?, t: Throwable?) {
            t?.printStackTrace()
            val reason = t?.javaClass?.simpleName
            runOnUiThread { callback(HttpResult(false, "连接服务器失败: $reason")) }
          }

          override fun onResponse(call: Call<DetectResponse?>?,
              response: Response<DetectResponse?>?) {
            if (response?.errorBody() != null) {
              val errorString = response.errorBody()?.string()
              runOnUiThread { callback(HttpResult(false, "detect 发生错误: $errorString")) }
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

  @JvmStatic fun search(outerId: String, imageFile: File,
      callback: (HttpResult<SearchResponse>) -> Unit) {
    val filePart = createFileForm("image_file", imageFile)
    val outerIdPart = Part.createFormData("outer_id", outerId)
    createService(CommonApi::class.java)
        .searchFace(apiKeyPart, apiSecretPart, outerIdPart, filePart)
        .enqueue(object : Callback<SearchResponse?> {
          override fun onFailure(call: Call<SearchResponse?>?, t: Throwable?) {
            t?.printStackTrace()
            val reason = t?.javaClass?.simpleName
            runOnUiThread { callback(HttpResult(false, "连接服务器失败: $reason")) }
          }

          override fun onResponse(call: Call<SearchResponse?>?,
              response: Response<SearchResponse?>?) {
            if (response?.errorBody() != null) {
              val errorString = response.errorBody()?.string()
              runOnUiThread { callback(HttpResult(false, "search 发生错误: $errorString")) }
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

  @JvmStatic fun addFace(imageFile: File, userId: String = "", callback: (SimpleHttpResult) -> Unit) {
    detect(imageFile) {
      if (it.success) {
        it.data?.faces?.let {
          if (it.isEmpty()) {
            callback(SimpleHttpResult(false, "没有找到人脸"))
          } else {
            val face = if (it.size > 1) {
              // if have multiple face, use largest one
              it.maxWith(Comparator { o1, o2 ->
                o1.face_rectangle.height * o1.face_rectangle.width - o2.face_rectangle.height * o2.face_rectangle.width
              })!!
            } else it[0]

            val faceToken = face.face_token
            if (userId.isBlank()) {
              addFaceset(faceToken, callback)
            } else {
              setUserId(userId, faceToken) {
                if (it.success) {
                  addFaceset(faceToken, callback)
                } else {
                  callback(it)
                }
              }
            }
          }
        }
      } else {
        callback(it)
      }
    }
  }

  @JvmStatic fun setUserId(userId: String, faceToken: String, callback: (SimpleHttpResult) -> Unit) {
    val userIdPart = Part.createFormData("user_id", userId)
    val faceTokenPart = Part.createFormData("face_token", faceToken)
    createService(FaceApi::class.java)
        .setUserId(apiKeyPart, apiSecretPart, userIdPart, faceTokenPart)
        .enqueue(object : Callback<FacesetOperateResponse?> {
          override fun onFailure(call: Call<FacesetOperateResponse?>?, t: Throwable?) {
            t?.printStackTrace()
            val reason = t?.javaClass?.simpleName
            runOnUiThread { callback(HttpResult(false, "连接服务器失败: $reason")) }
          }

          override fun onResponse(call: Call<FacesetOperateResponse?>?,
              response: Response<FacesetOperateResponse?>?) {
            if (response?.errorBody() != null) {
              val errorString = response.errorBody()?.string()
              runOnUiThread { callback(HttpResult(false, "setUserId 发生错误: $errorString")) }
              return
            }

            runOnUiThread { callback(HttpResult(true)) }
          }
        })
  }

  @JvmStatic fun addFaceset(faceTokens: String, callback: (SimpleHttpResult) -> Unit) {
    createService(FaceApi::class.java)
        .addFace(apiKeyPart, apiSecretPart, outerIdPart,
            MultipartBody.Part.createFormData("face_tokens", faceTokens))
        .enqueue(object : Callback<FacesetOperateResponse?> {
          override fun onFailure(call: Call<FacesetOperateResponse?>?,
              t: Throwable?) {
            t?.printStackTrace()
            val reason = t?.javaClass?.simpleName
            runOnUiThread { callback(HttpResult(false, "连接服务器失败: $reason")) }
          }

          override fun onResponse(call: Call<FacesetOperateResponse?>?,
              response: Response<FacesetOperateResponse?>?) {
            if (response?.errorBody() != null) {
              val errorString = response.errorBody()?.string()
              runOnUiThread { callback(HttpResult(false, "addFaceset 发生错误: $errorString")) }
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

  private fun runOnUiThread(receiver: () -> Unit) {
    if (Looper.getMainLooper().thread == Thread.currentThread()) {
      receiver()
    } else {
      handler.post { receiver() }
    }
  }

  class HttpResult<out T>(val success: Boolean, val message: String? = null, val data: T? = null)
}

val apiKeyPart = Part.createFormData("api_key", API_KEY) as ApiSecretPart
val apiSecretPart = Part.createFormData("api_secret", API_SECRET) as ApiSecretPart
val outerIdPart = Part.createFormData("outer_id", FACESET_OUTER_ID) as Part