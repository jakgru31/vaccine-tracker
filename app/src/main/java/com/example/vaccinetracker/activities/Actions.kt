package com.example.vaccinetracker.activities

import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.CalendarContract
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.Calendar

/**
 * Adds an appointment event to the user's calendar.
 *
 * @param context The application context.
 * @param title The title of the appointment.
 * @param description The description of the appointment.
 * @param startTime The start time of the appointment in milliseconds.
 * @param endTime The end time of the appointment in milliseconds.
 */
fun addAppointmentToCalendar(context: Context, title: String, description: String, startTime: Long, endTime: Long) {
    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, title)
        putExtra(CalendarContract.Events.DESCRIPTION, description)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime)
    }
    context.startActivity(intent)
}

/**
 * Sends a notification to the user.
 *
 * @param context The application context.
 * @param title The title of the notification.
 * @param message The message content of the notification.
 */
fun sendNotification(context: Context, title: String, message: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context as Activity, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            return
        }
    }

    val channelId = "VaccineTracker"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "Vaccine Tracker"
        val descriptionText = "Vaccine Tracker Notifications"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()
    val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(1, notification)
}

/**
 * Schedules a notification to be triggered at a specific time.
 *
 * @param context The application context.
 * @param timeInMillis The time at which the notification should be triggered in milliseconds.
 * @param title The title of the notification.
 * @param message The message content of the notification.
 */
fun scheduleNotification(context: Context, timeInMillis: Long, title: String, message: String) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, NotificationReceiver::class.java).apply {
        putExtra("title", title)
        putExtra("message", message)
    }
    val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            context.startActivity(intent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
        }
    } catch (e: SecurityException) {
        e.printStackTrace()
    }
}
