package tech.kissmyapps.android

import android.app.Application

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        //tech.kissmyapps.android.

        KissMyAppsSdk.sharedInstance.getConfigurationResult()

    }
}