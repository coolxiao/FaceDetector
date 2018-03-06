package com.kingyun.faceplusdemo.facepp

class Face(val face_token: String, val face_rectangle: FaceRectangle,
    val landmark: Map<String, Coordinate>) {

  data class FaceRectangle(val top: Int, val left: Int, val width: Int, val height: Int)
  data class Coordinate(val x: Int, val y: Int)
}