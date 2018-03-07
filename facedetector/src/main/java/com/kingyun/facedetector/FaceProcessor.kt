package com.kingyun.facedetector

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Handler
import android.os.Looper
import io.fotoapparat.facedetector.FaceDetector
import io.fotoapparat.facedetector.Rectangle
import io.fotoapparat.preview.Frame
import io.fotoapparat.util.FrameProcessor
import java.io.ByteArrayOutputStream

class FaceProcessor(context: Context) : FrameProcessor {

  var listener: OnFacesDetectedListener? = null
  private var pause: Boolean = false
    @Synchronized get
    @Synchronized set

  private val faceDetector: FaceDetector = FaceDetector.create(context.applicationContext)
  private val handler = Handler(Looper.getMainLooper())

  override fun invoke(frame: Frame) {
    if (pause) {
      return
    }

    val faces = faceDetector.detectFaces(
        frame.image,
        frame.size.width,
        frame.size.height,
        frame.rotation
    ).filter { it.height != 0F && it.width != 0F }
    if (faces.isNotEmpty()) {
      // convert NV21 raw image to bitmap
      val img = YuvImage(frame.image, ImageFormat.NV21, frame.size.width, frame.size.height, null)
      val bytes = ByteArrayOutputStream()
      img.compressToJpeg(Rect(0, 0, img.width, img.height), 85, bytes)
      handler.post { listener?.onFacesDetected(faces, bytes.toByteArray()) }
    }
  }

  fun pause() {
    pause = true
  }

  fun resume() {
    pause = false
  }

  /**
   * Notified when faces are detected.
   */
  interface OnFacesDetectedListener {

    /**
     * Called when faces are detected. Always called on the main thread.
     *
     * @param faces detected faces. If no faces were detected - an empty list.
     */
    fun onFacesDetected(faces: List<Rectangle>, imageBytes: ByteArray)
  }

}