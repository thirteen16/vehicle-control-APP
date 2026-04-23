package com.example.app

import android.app.Application
import com.example.app.data.local.ServerConfigStore

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        ServerConfigStore(this).getBaseUrl()
    }
}