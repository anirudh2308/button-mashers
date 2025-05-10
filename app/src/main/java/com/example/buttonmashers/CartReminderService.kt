package com.example.buttonmashers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class CartReminderService : Service() {
    private val CHANNEL_ID = "cart_reminder_channel"
    private val NOTIFICATION_ID = 1

    companion object {
        fun startService(context: Context, itemCount: Int) {
            val startIntent = Intent(context, CartReminderService::class.java)
            startIntent.putExtra("itemCount", itemCount)
            ContextCompat.startForegroundService(context, startIntent)
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, CartReminderService::class.java)
            context.stopService(stopIntent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Get the item count from the intent
        val itemCount = intent?.getIntExtra("itemCount", 0) ?: 0

        // Create notification channel if necessary
        createNotificationChannel()

        // Create and start the notification
        val notification = createNotification(itemCount)
        startForeground(NOTIFICATION_ID, notification)

        return START_NOT_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun createNotification(itemCount: Int): Notification {
        val notificationClickIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, CartActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Don't forget to checkout!")
            .setContentText("You have ${itemCount} items in your cart.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(notificationClickIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cart Reminder Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager!!.createNotificationChannel(channel)
        }
    }
}