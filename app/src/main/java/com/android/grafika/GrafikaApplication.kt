package com.android.grafika

import android.app.Application
import timber.log.Timber

class GrafikaApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}