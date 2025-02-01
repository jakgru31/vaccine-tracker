package com.example.vaccinetracker.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "No title"
        val message = intent.getStringExtra("message") ?: "No message"
        sendNotification(context, title, message)
    }
}
