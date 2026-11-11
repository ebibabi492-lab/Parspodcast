package com.parspodcast.studio

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ParsPodcastApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize local database, Jalali calendar locale, and LiteRT-LM backend hooks
    }
}
