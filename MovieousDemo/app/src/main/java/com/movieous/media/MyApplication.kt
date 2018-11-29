package com.movieous.media

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.faceunity.FURenderer
import com.movieous.base.Log
import com.movieous.media.api.sensesdk.utils.STLicenseUtils
import com.movieous.media.utils.DisplayManager
import com.movieous.shortvideo.UShortVideoEnv
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.orhanobut.logger.PrettyFormatStrategy
import kotlin.properties.Delegates

class MyApplication : Application() {

    companion object {
        private val TAG = "MyApplication"
        var context: Context by Delegates.notNull()
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        initLogConfig()
        initShortVideoEnv()
        initSenseTime();
        initFaceunity()
        DisplayManager.init(this)
        registerActivityLifecycleCallbacks(mActivityLifecycleCallbacks)
    }

    private fun initShortVideoEnv() {
        UShortVideoEnv.setLogLevel(Log.I)
        UShortVideoEnv.init(context, Constants.MOVIEOUS_SIGN)
    }

    private fun initSenseTime() {
        if (!STLicenseUtils.checkLicense(this)) {
            Log.i(TAG, "请检查License授权！")
        }
    }

    private fun initFaceunity() {
        FURenderer.initFURenderer(context)
    }

    private fun initLogConfig() {
        val formatStrategy = PrettyFormatStrategy.newBuilder()
            .showThreadInfo(false)
            .methodCount(0)
            .methodOffset(7)
            .tag("Movieous")
            .build()
        Logger.addLogAdapter(object : AndroidLogAdapter(formatStrategy) {
            override fun isLoggable(priority: Int, tag: String?): Boolean {
                return false
            }
        })
    }

    private val mActivityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            Log.i(TAG, "onCreated: " + activity.componentName.className)
        }

        override fun onActivityStarted(activity: Activity) {
            Log.i(TAG, "onStart: " + activity.componentName.className)
        }

        override fun onActivityResumed(activity: Activity) {
        }

        override fun onActivityPaused(activity: Activity) {
        }

        override fun onActivityStopped(activity: Activity) {
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        }

        override fun onActivityDestroyed(activity: Activity) {
            Log.d(TAG, "onDestroy: " + activity.componentName.className)
        }
    }
}
