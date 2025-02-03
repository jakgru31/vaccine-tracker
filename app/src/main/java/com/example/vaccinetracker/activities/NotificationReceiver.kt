package com.example.vaccinetracker.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * NotificationReceiver is a BroadcastReceiver that listens for broadcasted
 * intents containing notification data and triggers a notification.
 *
 * It extracts the title and message from the received intent and
 * calls the sendNotification function to display the notification.
 */
class NotificationReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "No title"
        val message = intent.getStringExtra("message") ?: "No message"
        sendNotification(context, title, message)
    }
}
