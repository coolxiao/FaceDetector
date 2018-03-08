package com.kingyun.faceplusdemo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.kingyun.facedetector.FaceDetector
import com.kingyun.facedetector.FaceServer
import com.kingyun.facedetector.http.createFileForm
import io.fotoapparat.view.CameraView
import kotlinx.android.synthetic.main.activity_detect_face.camera_view
import kotlinx.android.synthetic.main.activity_detect_face.detect_add_face
import kotlinx.android.synthetic.main.activity_detect_face.detect_continue
import kotlinx.android.synthetic.main.activity_detect_face.detect_flip
import java.io.File


class FaceDetectActivity : AppCompatActivity() {

  private var faceDetector: FaceDetector? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_detect_face)

    detect_continue.setOnClickListener {
      faceDetector?.resumeDetect()
    }
    detect_add_face.setOnClickListener {
      faceDetector?.takePicture()?.whenAvailable { file ->
        addFace(file)
      }
    }
    detect_flip.setOnClickListener { faceDetector?.switchToBack() }
  }

  override fun onStart() {
    super.onStart()
    if (faceDetector == null) {
      faceDetector = FaceDetector(this).apply {
        initCamera(camera_view as CameraView) { list, bytes ->
          // TODO: 18-3-8 该回调可以不实现，默认会调用
          defaultFacesDetectAction(list, bytes)
        }
      }
    }
    faceDetector?.requestCameraPermission(this) { granted ->
      when (granted) {
        true -> faceDetector?.start()
        false -> Toast.makeText(this, "未获取到相机权限，请重试", Toast.LENGTH_SHORT).show()
        else -> Toast.makeText(this, "App 需要相机权限才能继续使用", Toast.LENGTH_SHORT).show()
      }
    }
  }

  override fun onStop() {
    super.onStop()
    faceDetector?.stop()
  }

  private fun addFace(file: File?) {
    if (file != null) {
      FaceServer.addFace(createFileForm("image_file", file)) { result ->
        if (result.success) {
          Toast.makeText(this, "人脸添加成功", Toast.LENGTH_SHORT).show()
        } else {
          Toast.makeText(this, result.message ?: "未知错误", Toast.LENGTH_SHORT).show()
        }
      }
    } else {
      Toast.makeText(this, "无法保存图片", Toast.LENGTH_SHORT).show()
    }
  }

}