package com.kingyun.facedetector.tramsform

import android.graphics.Bitmap.CompressFormat.JPEG
import com.kingyun.facedetector.FaceDetector
import io.fotoapparat.result.Photo
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

typealias Transform<T> = (Photo) -> T?

class SaveBitmapTransform(private val file: File) : Transform<File?> {
  override fun invoke(photo: Photo): File? {
    return try {
      FileOutputStream(file).also {
        val bitmap = FaceDetector.portraitBitmap(photo.encodedImage)
        bitmap.compress(JPEG, 90, it)
      }
      file
    } catch (e: FileNotFoundException) {
      e.printStackTrace()
      null
    } catch (e: IOException) {
      e.printStackTrace()
      null
    }
  }
}