package com.movieous.media.ui.activity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import com.movieous.media.MyApplication
import com.movieous.media.R
import com.movieous.media.api.sensesdk.utils.FileUtils
import com.movieous.media.base.BaseActivity
import com.movieous.media.utils.AppUtils
import kotlinx.android.synthetic.main.activity_splash.*
import pub.devrel.easypermissions.EasyPermissions

/**
 * 启动页
 */
class SplashActivity : BaseActivity() {
    private var textTypeface: Typeface? = null
    private var descTypeFace: Typeface? = null
    private var alphaAnimation: AlphaAnimation? = null

    init {
        textTypeface = Typeface.createFromAsset(MyApplication.context.assets, "fonts/Lobster-1.4.otf")
        descTypeFace = Typeface.createFromAsset(MyApplication.context.assets, "fonts/FZLanTingHeiS-L-GB-Regular.TTF")
    }

    override fun layoutId(): Int = R.layout.activity_splash

    override fun initData() {}

    override fun initView() {
        tv_app_name.typeface = textTypeface
        tv_splash_desc.typeface = descTypeFace
        tv_version_name.text = "v${AppUtils.getVerName(this)}"
        alphaAnimation = AlphaAnimation(0.3f, 1.0f)
        alphaAnimation?.duration = 2000
        alphaAnimation?.setAnimationListener(object : AnimationListener {
            override fun onAnimationEnd(arg0: Animation) {
                startMainActivity()
            }

            override fun onAnimationRepeat(animation: Animation) {}

            override fun onAnimationStart(animation: Animation) {}
        })
        initResource()
        checkPermission()
    }

    override fun start() {}

    fun startMainActivity() {
        startActivity<VideoRecordActivity> { }
    }

    inline fun <reified T : Activity> Activity.startActivity(initializer: Intent.() -> Unit) {
        startActivity(
            Intent(this, T::class.java).apply(initializer)
        )
        finish()
    }

    private fun initResource() {
        object : Thread() {
            override fun run() {
                FileUtils.copyStickerFiles(applicationContext, "sensetime")
            }
        }.start()
    }

    private fun checkPermission() {
        val perms = arrayOf(
            Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA
        )
        EasyPermissions.requestPermissions(this, "应用需要以下权限，请允许", 0, *perms)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        if (requestCode == 0 && perms.isNotEmpty() && alphaAnimation != null && perms.contains(Manifest.permission.READ_PHONE_STATE) && perms.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            iv_web_icon.startAnimation(alphaAnimation)
        }
    }
}