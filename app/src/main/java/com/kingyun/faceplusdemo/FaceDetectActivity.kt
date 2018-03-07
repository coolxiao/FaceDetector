package com.kingyun.faceplusdemo

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat.JPEG
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.view.Window
import android.widget.Toast
import com.fondesa.kpermissions.extension.listeners
import com.fondesa.kpermissions.extension.permissionsBuilder
import com.kingyun.faceplusdemo.R.string
import com.kingyun.faceplusdemo.camera.FaceDetectorProcessorNew
import com.kingyun.faceplusdemo.camera.FaceDetectorProcessorNew.OnFacesDetectedListener
import com.kingyun.faceplusdemo.facepp.DetectResponse
import com.kingyun.faceplusdemo.facepp.Facepp
import com.kingyun.faceplusdemo.facepp.FaceppApi
import com.kingyun.faceplusdemo.facepp.FacesetOperateResponse
import com.kingyun.faceplusdemo.facepp.SaveBitmapTransform
import com.kingyun.faceplusdemo.facepp.SearchResponse
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.facedetector.Rectangle
import io.fotoapparat.log.logcat
import io.fotoapparat.log.loggers
import io.fotoapparat.parameter.FocusMode.Auto
import io.fotoapparat.parameter.Resolution
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.result.WhenDoneListener
import io.fotoapparat.selector.front
import kotlinx.android.synthetic.main.activity_detect_face.camera_view
import kotlinx.android.synthetic.main.activity_detect_face.detect_add_face
import kotlinx.android.synthetic.main.activity_detect_face.detect_continue
import okhttp3.MediaType
import okhttp3.MultipartBody.Part
import okhttp3.RequestBody
import org.jetbrains.anko.ctx
import org.jetbrains.anko.toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class FaceDetectActivity : AppCompatActivity() {

  private lateinit var fotoapparat: Fotoapparat

  private lateinit var faceDetectorProcessor: FaceDetectorProcessorNew

  override fun onCreate(savedInstanceState: Bundle?) {
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_detect_face)
    detect_continue.setOnClickListener {
      faceDetectorProcessor.pause = false
    }
    detect_add_face.setOnClickListener {
      val file = File(ctx.getExternalFilesDir(null), "newface.jpg")
      fotoapparat.takePicture().toPendingResult()
          .transform(SaveBitmapTransform(file))
          .whenDone(object : WhenDoneListener<File?> {
            override fun whenDone(saveFile: File?) {
              if (saveFile == null) {
                Handler(Looper.getMainLooper()).post {
                  toast("cannot save file")
                }
                return
              }

              val apiKeyPart = Part.createFormData("api_key", Facepp.API_KEY)
              val apiSecretPart = Part.createFormData("api_secret", Facepp.API_SECRET)
              createService(FaceppApi::class.java)
                  .detectFaceForm(apiKeyPart, apiSecretPart,
                      Part.createFormData("image_file", "image_file", RequestBody.create(
                          MediaType.parse("image/*"), saveFile)))
                  .enqueue(object : Callback<DetectResponse?> {
                    override fun onFailure(call: Call<DetectResponse?>?, t: Throwable?) {
                      t?.printStackTrace()
                      Handler(Looper.getMainLooper()).post {
                        toast("detect face failed")
                      }
                    }

                    override fun onResponse(call: Call<DetectResponse?>?,
                        response: Response<DetectResponse?>?) {
                      response?.body()?.let {
                        if (it.faces.isEmpty()) {
                          Handler(Looper.getMainLooper()).post {
                            toast("no face")
                          }
                          return@let
                        }
                        if (response.errorBody() != null) {
                          Handler(Looper.getMainLooper()).post {
                            toast("发生错误: " + response.errorBody()?.string())
                          }
                          return
                        }

                        val strBuilder = StringBuilder()
                        it.faces.forEach {
                          if (strBuilder.isNotEmpty()) {
                            strBuilder.append(",")
                          }
                          strBuilder.append(it.face_token)
                        }
                        createService(FaceppApi::class.java)
                            .addFace(apiKeyPart, apiSecretPart,
                                Part.createFormData("outer_id", "som_set"),
                                Part.createFormData("face_tokens", strBuilder.toString()))
                            .enqueue(object : Callback<FacesetOperateResponse?> {
                              override fun onFailure(call: Call<FacesetOperateResponse?>?,
                                  t: Throwable?) {
                                t?.printStackTrace()
                                Handler(Looper.getMainLooper()).post {
                                  toast("add failed")
                                }
                              }

                              override fun onResponse(call: Call<FacesetOperateResponse?>?,
                                  response: Response<FacesetOperateResponse?>?) {
                                response?.body()?.let {
                                  if (it.face_added > 0) {
                                    Handler(Looper.getMainLooper()).post {
                                      toast("add success")
                                    }
                                  }
                                  if (response.errorBody() != null) {
                                    Handler(Looper.getMainLooper()).post {
                                      toast("发生错误: " + response.errorBody()?.string())
                                    }
                                    return
                                  }
                                }
                              }
                            })

                      }
                    }
                  })
            }
          })
    }
  }

  override fun onStart() {
    super.onStart()
    requireCameraPermission {
      fotoapparat.start()
    }
  }

  override fun onStop() {
    super.onStop()
    if (this::fotoapparat.isInitialized) {
      fotoapparat.stop()
    }
  }

  private fun requireCameraPermission(receiver: () -> Unit) {
    val permission = permissionsBuilder(
        Manifest.permission.CAMERA
    ).build()
    permission.listeners {
      onAccepted {
        initCamera()
        receiver()
      }

      onDenied { onOpenCameraFailed() }

      onPermanentlyDenied { onOpenCameraFailed() }

      onShouldShowRationale { _, _ ->
        Toast.makeText(ctx, string.msg_permission_required, Toast.LENGTH_SHORT).show()
      }
    }
    permission.send()
  }

  private fun initCamera() {
    if (!this::fotoapparat.isInitialized) {
      faceDetectorProcessor = FaceDetectorProcessorNew.Builder(this).apply {
        listener = object : OnFacesDetectedListener {
          override fun onFacesDetected(
              faces: List<Rectangle>, imageBytes: ByteArray) {
            faceDetectorProcessor.pause = true

            // convert bitmap
            val bitmapBytes = ByteArrayOutputStream()
                .also { Facepp.portraitBitmap(imageBytes).compress(JPEG, 90, it) }
                .toByteArray()
            val base64Img = Base64.encodeToString(bitmapBytes, Base64.NO_WRAP)

            // TODO: 18-3-7  debug output
            val file = File(ctx.getExternalFilesDir(null), "searchface.jpg")
            val fileStream = FileOutputStream(file)
            fileStream.write(bitmapBytes)
            fileStream.close()
            // TODO: 18-3-7 debug output end

            val apiKeyPart = Part.createFormData("api_key", Facepp.API_KEY)
            val apiSecretPart = Part.createFormData("api_secret", Facepp.API_SECRET)
            createService(FaceppApi::class.java)
                .searchFaceForm(apiKeyPart, apiSecretPart, Part.createFormData("image_base64",
                    base64Img), Part.createFormData("outer_id", "som_set")
                )
                .enqueue(object : Callback<SearchResponse?> {
                  override fun onFailure(call: Call<SearchResponse?>?, t: Throwable?) {
                    t?.printStackTrace()
                    Handler(Looper.getMainLooper()).post {
                      toast("cannot connect to service")
                    }
                  }

                  override fun onResponse(call: Call<SearchResponse?>?,
                      response: Response<SearchResponse?>?) {
                    if (response?.body()?.faces?.isEmpty() == true) {
                      Handler(Looper.getMainLooper()).post {
                        toast("没有找到人脸")
                      }
                      return
                    }
                    if (response?.errorBody() != null) {
                      Handler(Looper.getMainLooper()).post {
                        toast("发生错误: " + response.errorBody()?.string())
                      }
                      return
                    }

                    val confidence = response?.body()?.results?.get(0)?.confidence
                    Handler(Looper.getMainLooper()).post {
                      toast("人脸可信度: " + confidence)
                    }
                  }
                })

          }
        }
      }.build()
      val configuration = CameraConfiguration(
          frameProcessor = faceDetectorProcessor,
          jpegQuality = { 85 },
          pictureResolution = { Resolution(1280, 960) },
          focusMode = { Auto }
      )

      fotoapparat = Fotoapparat(
          context = this,
          view = camera_view,
          scaleType = ScaleType.CenterCrop,
          lensPosition = front(),
          cameraConfiguration = configuration,
          logger = loggers(logcat())
      )

    }
  }

  private fun onOpenCameraFailed() {
    Toast.makeText(ctx, "failed", Toast.LENGTH_SHORT).show()
  }

  private fun <T> createService(clazz: Class<T>): T {
    return Facepp.retrofit.create(clazz)
  }

}