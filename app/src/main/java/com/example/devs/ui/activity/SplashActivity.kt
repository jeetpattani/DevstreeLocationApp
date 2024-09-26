package com.example.devs.ui.activity

import android.os.Handler
import android.os.Looper
import com.example.devs.base.BaseActivity
import com.example.devs.databinding.ActivitySplashBinding
import com.example.devs.ui.viewmodel.LocationViewModel
import com.example.devs.utils.startNewActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashActivity : BaseActivity<LocationViewModel, ActivitySplashBinding>() {

    override fun getViewBinding() = ActivitySplashBinding.inflate(layoutInflater)

    override fun initSetup() {
        Handler(Looper.getMainLooper()).postDelayed({
            startNewActivity(ModifyLocationActivity::class.java,isFinish = true)
        }, 2000)
    }

}