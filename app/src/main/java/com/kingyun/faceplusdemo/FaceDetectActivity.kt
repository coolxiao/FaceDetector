package com.kingyun.faceplusdemo

import android.Manifest
import android.os.Bundle
import android.os.Environment
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
import com.kingyun.faceplusdemo.facepp.AddFaceRequest
import com.kingyun.faceplusdemo.facepp.DetectResponse
import com.kingyun.faceplusdemo.facepp.Facepp
import com.kingyun.faceplusdemo.facepp.FaceppApi
import com.kingyun.faceplusdemo.facepp.FacesetOperateResponse
import com.kingyun.faceplusdemo.facepp.SearchRequest
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.facedetector.Rectangle
import io.fotoapparat.log.logcat
import io.fotoapparat.log.loggers
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.result.WhenDoneListener
import io.fotoapparat.selector.back
import kotlinx.android.synthetic.main.activity_detect_face.camera_view
import kotlinx.android.synthetic.main.activity_detect_face.detect_add_face
import okhttp3.MediaType
import okhttp3.MultipartBody.Part
import okhttp3.RequestBody
import org.jetbrains.anko.ctx
import org.jetbrains.anko.toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import top.zibin.luban.Luban
import top.zibin.luban.OnCompressListener
import java.io.File


class FaceDetectActivity : AppCompatActivity() {

  private lateinit var fotoapparat: Fotoapparat

  override fun onCreate(savedInstanceState: Bundle?) {
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_detect_face)

    detect_add_face.setOnClickListener {
      val file = File(ctx.getExternalFilesDir(null), "newface.jpg")
      fotoapparat.takePicture().saveToFile(file).whenDone(object : WhenDoneListener<Unit> {
        override fun whenDone(it: Unit?) {
          Luban.with(ctx)
              .load(file)
              .setCompressListener(object: OnCompressListener {
                override fun onSuccess(file: File?) {
                  if (file == null) {
                    return
                  }

                  createService(FaceppApi::class.java)
                      .detectFaceForm(Part.createFormData("api_key", Facepp.API_KEY),
                          Part.createFormData("api_secret", Facepp.API_SECRET),
                          Part.createFormData("image_file", "image_file", RequestBody.create(
                              MediaType.parse("image/*"), file)))
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
                            if (!it.error_message.isNullOrBlank()) {
                              toast(it.error_message ?: "")
                              return@let
                            }

                            val strBuilder = StringBuilder()
                            it.faces.forEach {
                              if (strBuilder.isNotEmpty()) {
                                strBuilder.append(",")
                              }
                              strBuilder.append(it.face_token)
                            }
                            createService(FaceppApi::class.java)
                                .addFace(AddFaceRequest(Facepp.API_KEY, Facepp.API_SECRET, "som_set",
                                    strBuilder.toString()))
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
                                      if (!it.error_message.isNullOrBlank()) {
                                        toast(it.error_message ?: "")
                                      }
                                    }
                                  }
                                })

                          }
                        }
                      })
                }

                override fun onError(e: Throwable?) {
                  e?.printStackTrace()
                }

                override fun onStart() {
                }
              })
              .launch()
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
      val faceDetectorProcessor = FaceDetectorProcessorNew.Builder(this).apply {
        listener = object : OnFacesDetectedListener {
          override fun onFacesDetected(
              faces: List<Rectangle>, imageBytes: ByteArray) {
//            createService(FaceppApi::class.java)
//                .searchFace(SearchRequest(
//                    Facepp.API_KEY,
//                    Facepp.API_SECRET,
//                    Base64.encodeToString(imageBytes, Base64.NO_WRAP),
//                    "som_set"
//                ))
          }
        }
      }.build()
      val configuration = CameraConfiguration(frameProcessor = faceDetectorProcessor)

      fotoapparat = Fotoapparat(
          context = this,
          view = camera_view,
          scaleType = ScaleType.CenterCrop,
          lensPosition = back(),
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