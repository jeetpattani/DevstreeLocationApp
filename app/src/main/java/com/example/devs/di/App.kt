package com.example.devs.di

import android.app.Application
import com.google.android.libraries.places.api.Places
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Places.initialize(this, "AIzaSyBSNyp6GQnnKlrMr7hD2HGiyF365tFlK5U")
    }

}