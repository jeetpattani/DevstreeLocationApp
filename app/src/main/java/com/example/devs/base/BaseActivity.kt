package com.example.devs.base

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.example.devs.di.PolylineDrawer
import com.example.devs.utils.ActivityLauncher
import com.example.devs.utils.ActivityLauncher.registerActivityForResult
import javax.inject.Inject

abstract class BaseActivity<VM: BaseViewModel, VB : ViewBinding> : AppCompatActivity() {

    @Inject
    lateinit var polylineDrawer: PolylineDrawer

    protected lateinit var activityLauncher: ActivityLauncher<Intent, ActivityResult>
    protected lateinit var binding: VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = getViewBinding()
        setContentView(binding.root)
        initSetup()
        activityLauncher = registerActivityForResult(this)
    }

    protected abstract fun getViewBinding(): VB
    protected abstract fun initSetup()
}