package com.android.grafika

import android.app.Application
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class GrafikaApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())

//        Executors.newSingleThreadExecutor().execute {
//            val url = URL("http://10.0.2.2:3000")
//
//            val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
//            conn.requestMethod = "GET"
//
//            val responseCode = conn.responseCode
//            if (responseCode == HttpURLConnection.HTTP_OK) {
//                val inputStream = conn.inputStream
//                val response = inputStream.bufferedReader().use { it.readText() }
//                Timber.i("Response: $response")
//            } else {
//                Timber.i("Response code: $responseCode")
//            }
//        }
    }
}