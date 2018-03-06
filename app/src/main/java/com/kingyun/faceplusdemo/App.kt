package com.kingyun.faceplusdemo

import android.app.Application
import com.kingyun.faceplusdemo.facepp.Facepp

/**
 * Created by xifan on 18-3-2.
 */
class App: Application() {
  override fun onCreate() {
    super.onCreate()
    Facepp.init()
  }
}