package com.kingyun.faceplusdemo.camera

import android.content.Context
import android.os.Handler
import android.os.Looper
import io.fotoapparat.facedetector.FaceDetector
import io.fotoapparat.facedetector.Rectangle
import io.fotoapparat.facedetector.processor.FaceDetectorProcessor
import io.fotoapparat.preview.Frame
import io.fotoapparat.util.FrameProcessor
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.ExifInterface
import java.io.ByteArrayOutputStream


/**
 * Created by xifan on 18-3-2.
 */
class FaceDetectorProcessorNew(builder: Builder) : FrameProcessor {

  private val handler = Handler(Looper.getMainLooper())

  private val faceDetector: FaceDetector
  private val listener: OnFacesDetectedListener

  var pause: Boolean = false
    @Synchronized get
    @Synchronized set

  init {
    faceDetector = FaceDetector.create(builder.context)
    listener = builder.listener
  }

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
      // convert raw image

      val img = YuvImage(frame.image, ImageFormat.NV21, frame.size.width, frame.size.height, null)
      val bytes = ByteArrayOutputStream()
      img.compressToJpeg(Rect(0, 0, img.width, img.height), 85, bytes)
      handler.post { listener.onFacesDetected(faces, bytes.toByteArray()) }
    }
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

    companion object {

      /**
       * Null-object for [OnFacesDetectedListener].
       */
      val NULL: OnFacesDetectedListener = object : OnFacesDetectedListener {
        override fun onFacesDetected(
            faces: List<Rectangle>, imageBytes: ByteArray) {
          // Do nothing
        }
      }
    }

  }

  /**
   * Builder for [FaceDetectorProcessor].
   */
  class Builder(internal val context: Context) {
    internal var listener = OnFacesDetectedListener.NULL

    /**
     * @param listener which will be notified when faces are detected.
     */
    fun listener(listener: OnFacesDetectedListener?): Builder {
      this.listener = listener ?: OnFacesDetectedListener.NULL

      return this
    }

    fun build(): FaceDetectorProcessorNew {
      return FaceDetectorProcessorNew(this)
    }

  }

}