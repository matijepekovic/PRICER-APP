package com.example.pricer.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.*

/**
 * Utility class to manage scheduling and canceling reminders using AlarmManager.
 */
object ReminderManager {
    private const val TAG = "ReminderManager"

    private const val REQUEST_CODE_PREFIX = 1000 // Base request code

    /**
     * Schedules a reminder using AlarmManager.
     *
     * @param context The application context
     * @param prospectId The ID of the prospect for which to schedule the reminder
     * @param customerName The name of the customer (for notification display)
     * @param reminderTime The timestamp when the reminder should trigger
     * @param reminderNote Optional note to display with the reminder
     */
    fun scheduleReminder(
        context: Context,
        prospectId: String,
        customerName: String,
        reminderTime: Long,
        reminderNote: String?
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_PROSPECT_ID, prospectId)
            putExtra(ReminderReceiver.EXTRA_CUSTOMER_NAME, customerName)
            putExtra(ReminderReceiver.EXTRA_REMINDER_NOTE, reminderNote)
        }

        // Using a unique request code for each prospect reminder
        val requestCode = getRequestCodeForProspect(prospectId)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // Cancel any existing alarm first
            alarmManager.cancel(pendingIntent)

            // Check if app can schedule exact alarms on Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.w(TAG, "App doesn't have SCHEDULE_EXACT_ALARM permission")
                    // You might want to show a dialog to the user guiding them to settings
                    // Or use setAndAllowWhileIdle instead which doesn't require the permission
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminderTime,
                        pendingIntent
                    )
                    return
                }
            }

            // Schedule the alarm based on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // For Android 6.0+, use setExactAndAllowWhileIdle for reliable delivery
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            } else {
                // For older versions
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            }

            // Log the scheduled reminder
            val now = System.currentTimeMillis()
            val timeUntilAlarm = reminderTime - now
            val hoursUntil = timeUntilAlarm / (1000 * 60 * 60)
            val minutesUntil = (timeUntilAlarm % (1000 * 60 * 60)) / (1000 * 60)

            Log.i(TAG, "Scheduled reminder for $customerName (ID: $prospectId) in $hoursUntil hours and $minutesUntil minutes")
            Log.d(TAG, "Reminder details - Time: ${Date(reminderTime)}, Note: $reminderNote")

            // Save to persistent storage for recovery after device reboot
            saveReminderToPrefs(context, prospectId, customerName, reminderTime, reminderNote)

        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Failed to schedule exact alarm. Missing permission?", e)
            // Try a non-exact alarm as fallback
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
                Log.i(TAG, "Scheduled non-exact alarm as fallback")
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to schedule even the fallback alarm", e2)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule reminder for prospect $prospectId", e)
        }
    }

    /**
     * Cancels a scheduled reminder.
     *
     * @param context The application context
     * @param prospectId The ID of the prospect whose reminder to cancel
     */
    fun cancelReminder(context: Context, prospectId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val requestCode = getRequestCodeForProspect(prospectId)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // Cancel the alarm
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()

            // Remove from persistent storage
            removeReminderFromPrefs(context, prospectId)

            Log.i(TAG, "Cancelled reminder for prospect ID: $prospectId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel reminder for prospect $prospectId", e)
        }
    }

    /**
     * Reschedules all saved reminders, typically after device reboot.
     *
     * @param context The application context
     */
    fun rescheduleAllReminders(context: Context) {
        val sharedPrefs = context.getSharedPreferences("pricer_reminders", Context.MODE_PRIVATE)
        val reminderIds = sharedPrefs.getStringSet("reminder_ids", setOf()) ?: setOf()

        var restoredCount = 0

        for (prospectId in reminderIds) {
            val reminderJson = sharedPrefs.getString("reminder_$prospectId", null) ?: continue

            try {
                // Parse the JSON string to ReminderData
                val reminderData = ReminderData.fromJson(reminderJson)

                // Skip if the reminder time is in the past
                if (reminderData.reminderTime <= System.currentTimeMillis()) {
                    // Cleanup past reminders
                    removeReminderFromPrefs(context, prospectId)
                    continue
                }

                // Reschedule the reminder
                scheduleReminder(
                    context,
                    prospectId,
                    reminderData.customerName,
                    reminderData.reminderTime,
                    reminderData.reminderNote
                )

                restoredCount++

            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore reminder for $prospectId", e)
            }
        }

        Log.i(TAG, "Rescheduled $restoredCount reminders after device reboot")
    }

    /**
     * Gets a unique request code for a prospect's reminder.
     * This helps ensure we have different PendingIntents for different reminders.
     */
    private fun getRequestCodeForProspect(prospectId: String): Int {
        // Generate a consistent hash code from the prospect ID
        return REQUEST_CODE_PREFIX + (prospectId.hashCode() and 0x0000FFFF)
    }

    /**
     * Saves reminder data to SharedPreferences for persistence across reboots.
     */
    private fun saveReminderToPrefs(
        context: Context,
        prospectId: String,
        customerName: String,
        reminderTime: Long,
        reminderNote: String?
    ) {
        val sharedPrefs = context.getSharedPreferences("pricer_reminders", Context.MODE_PRIVATE)
        val reminderIds = sharedPrefs.getStringSet("reminder_ids", mutableSetOf()) ?: mutableSetOf()

        // Create a new, mutable set with all existing IDs plus the current one
        val updatedIds = reminderIds.toMutableSet().apply { add(prospectId) }

        // Create reminder data object and convert to JSON
        val reminderData = ReminderData(
            prospectId = prospectId,
            customerName = customerName,
            reminderTime = reminderTime,
            reminderNote = reminderNote
        )

        // Save everything to SharedPreferences using the KTX extension function
        with(sharedPrefs.edit()) {
            putStringSet("reminder_ids", updatedIds)
            putString("reminder_$prospectId", reminderData.toJson())
            apply()
        }
    }

    // Fix the RegExp for JSON extraction
    private fun extractJsonValue(json: String, key: String): String? {
        // Change the single character alternation [,}] to a character class
        val pattern = "\"$key\":\\s*\"?(.*?)\"?(?:[,}])".toRegex()
        val matchResult = pattern.find(json)
        return matchResult?.groupValues?.getOrNull(1)
    }
    /**
     * Removes reminder data from SharedPreferences.
     */
    private fun removeReminderFromPrefs(context: Context, prospectId: String) {
        val sharedPrefs = context.getSharedPreferences("pricer_reminders", Context.MODE_PRIVATE)
        val reminderIds = sharedPrefs.getStringSet("reminder_ids", mutableSetOf()) ?: mutableSetOf()

        // Create a new, mutable set with all existing IDs minus the current one
        val updatedIds = reminderIds.toMutableSet().apply { remove(prospectId) }

        // Update SharedPreferences
        sharedPrefs.edit()
            .putStringSet("reminder_ids", updatedIds)
            .remove("reminder_$prospectId")
            .apply()
    }

    /**
     * Data class to store reminder info for persistence.
     */
    private data class ReminderData(
        val prospectId: String,
        val customerName: String,
        val reminderTime: Long,
        val reminderNote: String?
    ) {
        /**
         * Serializes this data to a JSON string.
         */
        fun toJson(): String {
            // Using a simple map for JSON conversion
            val map = mapOf(
                "prospectId" to prospectId,
                "customerName" to customerName,
                "reminderTime" to reminderTime,
                "reminderNote" to reminderNote
            )

            // Convert to JSON using Android's built-in methods
            // This is simplified. In a real app, consider using Gson or similar library
            val sb = StringBuilder("{")
            map.entries.forEachIndexed { index, entry ->
                if (index > 0) sb.append(",")
                sb.append("\"${entry.key}\":")
                when (val value = entry.value) {
                    is String -> sb.append("\"$value\"")
                    is Long -> sb.append(value)
                    null -> sb.append("null")
                    else -> sb.append("\"$value\"")
                }
            }
            sb.append("}")
            return sb.toString()
        }

        companion object {
            /**
             * Creates a ReminderData object from a JSON string.
             */
            fun fromJson(json: String): ReminderData {
                // This is a very simplified JSON parser for demonstration.
                // In a real app, use a proper JSON parser library.

                // Extract values from JSON string
                val prospectId = extractJsonValue(json, "prospectId")
                val customerName = extractJsonValue(json, "customerName")
                val reminderTimeStr = extractJsonValue(json, "reminderTime")
                val reminderNote = extractJsonValue(json, "reminderNote")

                // Parse values
                val reminderTime = reminderTimeStr?.toLongOrNull() ?: 0L

                // Create and return the object
                return ReminderData(
                    prospectId = prospectId ?: "",
                    customerName = customerName ?: "",
                    reminderTime = reminderTime,
                    reminderNote = if (reminderNote == "null") null else reminderNote
                )
            }

            /**
             * Extracts a value from a simple JSON string.
             */
            private fun extractJsonValue(json: String, key: String): String? {
                val pattern = "\"$key\":\\s*\"?(.*?)\"?(?:,|\\})".toRegex()
                val matchResult = pattern.find(json)
                return matchResult?.groupValues?.getOrNull(1)
            }
        }
    }
}