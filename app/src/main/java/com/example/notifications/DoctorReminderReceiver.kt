package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DoctorReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            DoctorNotificationScheduler.ACTION_FIRE_DOCTOR_REMINDER -> {
                val stableKey = intent.getStringExtra(DoctorNotificationScheduler.EXTRA_STABLE_KEY).orEmpty()
                val kindName = intent.getStringExtra(DoctorNotificationScheduler.EXTRA_KIND).orEmpty()
                val kind = runCatching { DoctorReminderKind.valueOf(kindName) }.getOrNull() ?: return
                val dateKey = intent.getStringExtra(DoctorNotificationScheduler.EXTRA_DATE_KEY).orEmpty()
                val title = intent.getStringExtra(DoctorNotificationScheduler.EXTRA_TITLE).orEmpty()
                val timeText = intent.getStringExtra(DoctorNotificationScheduler.EXTRA_TIME_TEXT).orEmpty()
                val doctorName = intent.getStringExtra(DoctorNotificationScheduler.EXTRA_DOCTOR_NAME)
                    ?: "主治醫生"

                DoctorNotificationScheduler.showDoctorNotification(
                    context = context,
                    doctorName = doctorName,
                    kind = kind,
                    itemTitle = title,
                    timeText = timeText,
                    stableKey = stableKey,
                    dateKey = dateKey
                )
            }

            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                DoctorNotificationScheduler.rescheduleStoredReminders(context)
            }
        }
    }
}
