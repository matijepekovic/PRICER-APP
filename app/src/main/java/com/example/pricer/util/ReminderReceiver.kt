package com.example.pricer.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.pricer.MainActivity
import com.example.pricer.R
import com.example.pricer.util.ReminderManager

/**
 * BroadcastReceiver for handling triggered reminders and boot completed events.
 */
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ReminderReceiver"

        // Notification Channel
        private const val CHANNEL_ID = "pricer_reminders"
        private const val CHANNEL_NAME = "Prospect Reminders"

        // Extra keys for the intent
        const val EXTRA_PROSPECT_ID = "prospect_id"
        const val EXTRA_CUSTOMER_NAME = "customer_name"
        const val EXTRA_REMINDER_NOTE = "reminder_note"

        // Request code for main activity intent
        private const val NOTIFICATION_REQUEST_CODE = 1001
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: ${intent.action}")

        when (intent.action) {
            // When device is rebooted, restore all scheduled reminders
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.i(TAG, "Device rebooted, rescheduling reminders")
                ReminderManager.rescheduleAllReminders(context)
                Log.d(TAG, "onReceive called with action: ${intent.action ?: "null"}")
                Log.d(TAG, "Intent extras: ${intent.extras?.keySet()?.joinToString(", ") { "$it: ${intent.extras?.get(it)}" } ?: "null"}")
            }

            // When a reminder is triggered
            else -> {
                val prospectId = intent.getStringExtra(EXTRA_PROSPECT_ID)
                val customerName = intent.getStringExtra(EXTRA_CUSTOMER_NAME)
                val reminderNote = intent.getStringExtra(EXTRA_REMINDER_NOTE)
                Log.d(TAG, "Extracted from intent - prospectId: $prospectId, customerName: $customerName")

                if (prospectId != null && customerName != null) {
                    Log.i(TAG, "Showing notification for prospect: $customerName")
                    showNotification(context, prospectId, customerName, reminderNote)
                } else {
                    Log.e(TAG, "Missing required data in reminder intent")
                }
            }
        }
    }

    /**
     * Shows a notification for the reminder.
     */
    private fun showNotification(
        context: Context,
        prospectId: String,
        customerName: String,
        reminderNote: String?
    ) {
        try {
            Log.d(TAG, "START showNotification for $customerName")

            // Create notification channel with extra logging
            Log.d(TAG, "Creating notification channel")
            createNotificationChannel(context)

            // Build the main activity intent for when notification is tapped
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("prospect_id", prospectId)
                putExtra("open_prospect_detail", true)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_REQUEST_CODE,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Build notification
            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Reminder: $customerName")
                .setContentText(reminderNote ?: "Follow up with this prospect")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .setColor(Color.BLUE)

            // If the reminder note is longer, add a big text style
            if (!reminderNote.isNullOrBlank() && reminderNote.length > 40) {
                notificationBuilder.setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(reminderNote)
                )
            }

            // Define the notification ID
            val notificationId = prospectId.hashCode() and 0x7FFFFFFF

            // Get notification manager and display notification
            val notificationManager = NotificationManagerCompat.from(context)

            // At the end where you call notify():
            Log.d(TAG, "About to call notify() with ID: $notificationId")
            notificationManager.notify(notificationId, notificationBuilder.build())
            Log.d(TAG, "notify() called successfully")

        } catch (e: SecurityException) {
            Log.e(TAG, "PERMISSION ERROR: No permission to post notifications", e)
        } catch (e: Exception) {
            Log.e(TAG, "ERROR showing notification", e)
        }
    }
    /**
     * Creates a notification channel for Android 8.0+.
     */
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminder notifications for prospects"
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            Log.d(TAG, "Notification channel created")
        }
    }
}