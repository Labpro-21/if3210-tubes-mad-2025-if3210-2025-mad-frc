package com.example.purrytify

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class MyApp: Application() {
    companion object{
        const val ACTION_NEXT = "NEXT";
        const val ACTION_PREV = "PREVIOUS";
        const val ACTION_PLAY = "PLAY";
        const val CHANNEL_ID = "Channel_1"
        const val ACTION_SEEK = "ACTION_SEEK"
        const val ACTION_TOGGLE_LOOP = "ACTION_TOGGLE_LOOP"
    }
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel(this)
    }

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Purrytify Music",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

    }

}

