package com.kingyun.facedetector

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

object FaceServer {
  fun detect(imageFile: FilePart): Call<DetectResponse> {
    return createService(CommonApi::class.java)
        .detectFace(apiKeyPart, apiSecretPart, imageFile)
  }

  fun search(outerId: MultipartBody.Part, imageFile: FilePart): Call<SearchResponse> {
    return createService(CommonApi::class.java)
        .searchFace(apiKeyPart, apiSecretPart, outerId, imageFile)
  }

  fun addFace(imageFile: FilePart) {
    detect(imageFile).enqueue(object : Callback<DetectResponse?> {
      override fun onFailure(call: Call<DetectResponse?>?, t: Throwable?) {
        t?.printStackTrace()
      }

      override fun onResponse(call: Call<DetectResponse?>?, response: Response<DetectResponse?>?) {
        if (response?.errorBody() != null) {
          // TODO: 18-3-7 notify "发生错误: " + response.errorBody()?.string()
          return
        }
        response?.body()?.let {
          if (it.faces.isEmpty()) {
            // TODO: 18-3-7 notify no face
            return@let
          }

          val strBuilder = StringBuilder()
          it.faces.forEach {
            if (strBuilder.isNotEmpty()) {
              strBuilder.append(",")
            }
            strBuilder.append(it.face_token)
          }
          addFaceset(strBuilder.toString())
        }
      }
    })
  }

  fun addFaceset(faceTokens: String) {
    createService(FaceApi::class.java)
        .addFace(apiKeyPart, apiSecretPart, outerIdPart,
            MultipartBody.Part.createFormData("face_tokens", faceTokens))
        .enqueue(object : Callback<FacesetOperateResponse?> {
          override fun onFailure(call: Call<FacesetOperateResponse?>?,
              t: Throwable?) {
            t?.printStackTrace()
          }

          override fun onResponse(call: Call<FacesetOperateResponse?>?,
              response: Response<FacesetOperateResponse?>?) {
            if (response?.errorBody() != null) {
              // TODO: 18-3-7 notify "发生错误: " + response.errorBody()?.string()
              return
            }
            response?.body()?.let {
              if (it.face_added > 0) {
                // TODO: 18-3-7 notify add success
              }
            }
          }
        })
  }
}