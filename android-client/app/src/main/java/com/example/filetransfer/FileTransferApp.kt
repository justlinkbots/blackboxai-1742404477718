package com.example.filetransfer

import android.app.Application
import timber.log.Timber

class FileTransferApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}