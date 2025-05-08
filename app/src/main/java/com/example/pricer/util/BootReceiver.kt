package com.example.pricer.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.pricer.util.ReminderManager

/**
 * BroadcastReceiver that handles the BOOT_COMPLETED intent to restore scheduled reminders
 * after device restart.
 */
class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "Device boot completed, rescheduling reminders")
            ReminderManager.rescheduleAllReminders(context)
        }
    }
}