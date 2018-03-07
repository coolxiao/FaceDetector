package com.kingyun.faceplusdemo.facepp

import android.graphics.Bitmap.CompressFormat.JPEG
import io.fotoapparat.result.Photo
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class SaveBitmapTransform(private val file: File) : (Photo) -> File? {
  override fun invoke(photo: Photo): File? {
    return try {
      FileOutputStream(file).also {
        val bitmap = Facepp.portraitBitmap(photo.encodedImage)
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