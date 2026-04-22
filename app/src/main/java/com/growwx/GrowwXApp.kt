package com.growwx

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class — annotated with @HiltAndroidApp to trigger
 * Hilt's code generation and set up the application-level component.
 */
@HiltAndroidApp
class GrowwXApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Future: initialise Crashlytics, Firebase, analytics SDKs here
    }
}
