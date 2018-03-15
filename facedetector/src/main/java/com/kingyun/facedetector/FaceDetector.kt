package com.kingyun.facedetector

import android.Manifest
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat.JPEG
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.widget.Toast
import com.fondesa.kpermissions.extension.listeners
import com.fondesa.kpermissions.extension.permissionsBuilder
import com.kingyun.facedetector.FaceProcessor.OnFacesDetectedListener
import com.kingyun.facedetector.FaceServer.FACESET_OUTER_ID
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
import io.fotoapparat.util.FrameProcessor
import io.fotoapparat.view.CameraView
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
   * It is safe to wrap with [initCamera] to grant permission when it's been used on Marshmallow devices
   * @param receiver true if permission granted, false if denied. When it's returning null which means
   * should show rational.
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

  /**
   * MUST be called before everything.
   * @param configuration customize camera configuration
   * @param faceDetectAction callback when camera detect face
   * @see [pauseDetect] for pause detecting
   * @see [resumeDetect] for resume paused detection
   */
  @JvmOverloads fun initCamera(cameraView: CameraView, configuration: CameraConfiguration? = null,
      faceDetectAction: ((List<Rectangle>, ByteArray) -> Unit)? = null) {
    val ctx = context
    val processor = ctx?.let { FaceProcessor(ctx) }?.apply {
      listener = object : OnFacesDetectedListener {
        override fun onFacesDetected(faces: List<Rectangle>, imageBytes: ByteArray) {
          if (faceDetectAction == null) {
            defaultFacesDetectAction(FACESET_OUTER_ID, faces, imageBytes)
          } else {
            faceDetectAction.invoke(faces, imageBytes)
          }
        }
      }
      faceDetectorProcessor = this
    } ?: throw RuntimeException("")

    fotoapparat = Fotoapparat(
        context = ctx,
        view = cameraView,
        scaleType = ScaleType.CenterCrop,
        lensPosition = front(),
        cameraConfiguration = configuration ?: getDefaultCameraConfiguration(processor),
        logger = loggers(logcat())
    )
  }

  /**
   * start camera and ready to detect faces
   */
  fun start() {
    fotoapparat?.start()
  }

  /**
   * stop camera
   */
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

  fun defaultFacesDetectAction(outerId: String, faces: List<Rectangle>, imageBytes: ByteArray) {
    val ctx = context ?: return
    pauseDetect()

    // convert bitmap
    val bitmapBytes = ByteArrayOutputStream()
        .also { portraitBitmap(imageBytes).compress(JPEG, 90, it) }
        .toByteArray()

    // save file
    val file = File(ctx.getExternalFilesDir(null), "searchface.jpg")
    FileOutputStream(file).buffered().use { it.write(bitmapBytes) }

    FaceServer.search(outerId, file) {
      if (it.success) {
        val result = it.data?.results?.get(0)
        val userId = if (result?.user_id.isNullOrBlank()) "未知" else result?.user_id
        val confidence = result?.confidence

        Toast.makeText(context, "人脸可能是$userId，可信度：$confidence", Toast.LENGTH_SHORT).show()
      } else {
        Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
      }
    }
  }

  private fun getDefaultCameraConfiguration(frameProcessor: FrameProcessor) = CameraConfiguration(
      frameProcessor = frameProcessor,
      jpegQuality = { 85 },
      pictureResolution = { Resolution(1280, 960) },
      focusMode = { Auto }
  )

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