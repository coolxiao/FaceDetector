package com.kingyun.facedetector.tramsform

import io.fotoapparat.result.Photo

class ErrorTransform<out T> : Transform<T?> {
  override fun invoke(p1: Photo): T? = null
}