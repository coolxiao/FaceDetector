package com.kingyun.facedetector

import android.Manifest
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat.JPEG
import android.graphics.BitmapFactory
import android.graphics.Matrix
import com.fondesa.kpermissions.extension.listeners
import com.fondesa.kpermissions.extension.permissionsBuilder
import com.kingyun.facedetector.FaceProcessor.OnFacesDetectedListener
import com.kingyun.facedetector.http.CommonApi
import com.kingyun.facedetector.http.SearchResponse
import com.kingyun.facedetector.http.apiKeyPart
import com.kingyun.facedetector.http.apiSecretPart
import com.kingyun.facedetector.http.createFileForm
import com.kingyun.facedetector.http.createService
import com.kingyun.facedetector.http.outerIdPart
import com.kingyun.facedetector.tramsform.ErrorTransform
import com.kingyun.facedetector.tramsform.SaveBitmapTransform
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.configuration.UpdateConfiguration
import io.fotoapparat.facedetector.Rectangle
import io.fotoapparat.log.logcat
import io.fotoapparat.log.loggers
import io.fotoapparat.parameter.FocusMode.Auto
import io.fotoapparat.parameter.Resolution
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.result.PendingResult
import io.fotoapparat.selector.back
import io.fotoapparat.selector.front
import io.fotoapparat.view.CameraView
import okhttp3.MediaType
import org.jetbrains.anko.runOnUiThread
import org.jetbrains.anko.toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class FaceDetector(activity: Activity) {
  private var context: Context? = null

  private var fotoapparat: Fotoapparat? = null
  private var faceDetectorProcessor: FaceProcessor? = null

  init {
    context = activity
  }

  /**
   * @param receiver true if permission granted,
   */
  fun requestCameraPermission(activity: Activity, receiver: (Boolean?) -> Unit) {
    activity.run {
      val permission = permissionsBuilder(
          Manifest.permission.CAMERA
      ).build()
      permission.listeners {
        onAccepted { receiver(true) }

        onDenied { receiver(false) }

        onPermanentlyDenied { receiver(false) }

        onShouldShowRationale { _, _ -> receiver(null) }
      }
      permission.send()
    }
  }

  fun initCamera(cameraView: CameraView,
      callback: ((List<Rectangle>, ByteArray) -> Unit)? = null): Fotoapparat {
    val ctx = context
    val processor = ctx?.let { FaceProcessor(ctx) }?.apply {
      listener = object : OnFacesDetectedListener {
        override fun onFacesDetected(faces: List<Rectangle>, imageBytes: ByteArray) {
          callback?.invoke(faces, imageBytes) ?: defaultFacesDetectAction(faces, imageBytes)
        }
      }
      faceDetectorProcessor = this
    } ?: throw RuntimeException("")

    val configuration = CameraConfiguration(
        frameProcessor = processor,
        jpegQuality = { 85 },
        pictureResolution = { Resolution(1280, 960) },
        focusMode = { Auto }
    )
    return Fotoapparat(
        context = ctx,
        view = cameraView,
        scaleType = ScaleType.CenterCrop,
        lensPosition = front(),
        cameraConfiguration = configuration,
        logger = loggers(logcat())
    )
  }

  fun start() {
    fotoapparat?.start()
  }

  fun stop() {
    fotoapparat?.stop()
  }

  fun takePicture(): PendingResult<File?> {
    val ctx = context
    val file = ctx?.let { File(ctx.getExternalFilesDir(null), "newface.jpg") }
    val transform = if (file == null) ErrorTransform<File>() else SaveBitmapTransform(file)
    return fotoapparat?.takePicture()?.toPendingResult()?.transform(transform)
        ?: throw RuntimeException("Fotoapparat is not initialized!")
  }

  fun switchToBack() {
    fotoapparat?.switchTo(back(), CameraConfiguration.default())
  }

  fun switchToFront() {
    fotoapparat?.switchTo(front(), CameraConfiguration.default())
  }

  fun pauseDetect() {
    faceDetectorProcessor?.pause()
  }

  fun resumeDetect() {
    faceDetectorProcessor?.resume()
  }

  fun updateConfiguration(configuration: UpdateConfiguration) {
    fotoapparat?.updateConfiguration(configuration)
  }

  fun setZoom(zoom: Float) {
    fotoapparat?.setZoom(zoom)
  }

  private fun defaultFacesDetectAction(
      faces: List<Rectangle>,
      imageBytes: ByteArray) {
    val ctx = context ?: return
    faceDetectorProcessor?.pause()

    // convert bitmap
    val bitmapBytes = ByteArrayOutputStream()
        .also { portraitBitmap(imageBytes).compress(JPEG, 90, it) }
        .toByteArray()

    // save file
    val file = File(ctx.getExternalFilesDir(null), "searchface.jpg")
    FileOutputStream(file).buffered().use { it.write(bitmapBytes) }

    val imageType = MediaType.parse("image/*")
    createService(CommonApi::class.java)
        .searchFace(apiKeyPart, apiSecretPart,
            outerIdPart,
            createFileForm("image_file", file, imageType)
        )
        .enqueue(object : Callback<SearchResponse?> {
          override fun onFailure(call: Call<SearchResponse?>?, t: Throwable?) {
            t?.printStackTrace()
            ctx.runOnUiThread {
              toast("cannot connect to service")
            }
          }

          override fun onResponse(call: Call<SearchResponse?>?,
              response: Response<SearchResponse?>?) {
            if (response?.body()?.faces?.isEmpty() == true) {
              ctx.runOnUiThread {
                toast("没有找到人脸")
              }
              return
            }
            if (response?.errorBody() != null) {
              ctx.runOnUiThread {
                toast("发生错误: " + response.errorBody()?.string())
              }
              return
            }

            val confidence = response?.body()?.results?.get(0)?.confidence
            ctx.runOnUiThread {
              toast("人脸可信度: " + confidence)
            }
          }
        })

  }

  companion object {
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
}