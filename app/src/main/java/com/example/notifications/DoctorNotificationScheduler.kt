package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.CalendarEvent
import com.example.data.CustomTimetableConfig
import com.example.data.DoctorQuotes
import com.example.data.RehabTask
import com.example.data.TaskCategory
import com.example.data.currentTodayDateKey
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class DoctorReminderKind {
    DEADLINE_TODAY,
    BEFORE_30_MIN,
    AT_TIME
}

data class ScheduledDoctorReminder(
    val stableKey: String,
    val kind: DoctorReminderKind,
    val dateKey: String,
    val triggerMillis: Long,
    val itemTitle: String,
    val timeText: String
) {
    fun requestCode(): Int = (abs(stableKey.hashCode()) % 900_000) + 10_000
}

object DoctorNotificationScheduler {
    private const val TAG = "DoctorNotify"
    const val CHANNEL_ID = "doctor_reminders"
    const val ACTION_FIRE_DOCTOR_REMINDER = "com.example.notifications.ACTION_FIRE_DOCTOR_REMINDER"

    const val EXTRA_STABLE_KEY = "extra_stable_key"
    const val EXTRA_KIND = "extra_kind"
    const val EXTRA_DATE_KEY = "extra_date_key"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_TIME_TEXT = "extra_time_text"
    const val EXTRA_DOCTOR_NAME = "extra_doctor_name"

    private const val PREFS_NAME = "doctor_reminder_prefs"
    private const val KEY_STORED_REMINDERS = "stored_reminders_json"
    private const val KEY_STORED_DOCTOR_NAME = "stored_doctor_name"
    private const val KEY_ACTIVE_REQUEST_CODES = "active_request_codes_json"
    private const val KEY_SENT_PREFIX = "sent_"
    private const val MAX_SCHEDULED_ALARMS = 64

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "主治醫生行程與死線提醒",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "在作業死線當天、課表與行事曆活動前 30 分鐘及準點時由主治醫生傳送提醒通知。"
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun computeUpcomingReminders(
        tasks: List<RehabTask>,
        calendarEvents: List<CalendarEvent>,
        customTimetable: CustomTimetableConfig,
        nowMillis: Long = System.currentTimeMillis(),
        wasSentPredicate: (stableKey: String, dateKey: String) -> Boolean = { _, _ -> false }
    ): List<ScheduledDoctorReminder> {
        val todayKey = currentTodayDateKey(nowMillis)
        val result = mutableListOf<ScheduledDoctorReminder>()

        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val nowHour = nowCal.get(Calendar.HOUR_OF_DAY)

        // 1. 作業死線當天提醒 (09:00, 若當天已過 09:00 且在 22:00 前且尚未送過則補發一次)
        tasks
            .filter {
                it.category == TaskCategory.HOMEWORK.wireValue &&
                    !it.completed &&
                    it.deadlineDate.isNotBlank()
            }
            .forEach { task ->
                val dateParts = parseDateParts(task.deadlineDate) ?: return@forEach
                val triggerCal = Calendar.getInstance().apply {
                    set(dateParts.first, dateParts.second - 1, dateParts.third, 9, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val stableKey = "deadline_${task.id}_${task.deadlineDate}"
                val triggerMillis = when {
                    triggerCal.timeInMillis >= nowMillis -> triggerCal.timeInMillis
                    task.deadlineDate == todayKey && nowHour in 9..21 && !wasSentPredicate(stableKey, todayKey) ->
                        nowMillis + 8_000L
                    else -> null
                }
                if (triggerMillis != null) {
                    result += ScheduledDoctorReminder(
                        stableKey = stableKey,
                        kind = DoctorReminderKind.DEADLINE_TODAY,
                        dateKey = task.deadlineDate,
                        triggerMillis = triggerMillis,
                        itemTitle = task.title,
                        timeText = "今日死線"
                    )
                }
            }

        // 2. 日曆行程：活動前 30 分鐘 + 活動設定時間（全日活動預設早上 08:30，前 30 分為 08:00）
        calendarEvents.forEach { event ->
            val dateParts = parseDateParts(event.dateKey) ?: return@forEach
            val parsedHm = parseHourMinuteOrNull(event.timeText)
            val isAllDay = parsedHm == null || event.timeText.trim() == "全日"
            val targetHour = parsedHm?.first ?: 8
            val targetMin = parsedHm?.second ?: 30
            val displayTime = if (isAllDay) "全日行程" else String.format(Locale.US, "%02d:%02d", targetHour, targetMin)

            val eventCal = Calendar.getInstance().apply {
                set(dateParts.first, dateParts.second - 1, dateParts.third, targetHour, targetMin, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val atTimeMillis = eventCal.timeInMillis
            val before30Millis = atTimeMillis - 30L * 60_000L

            val beforeKey = "cal_pre30_${event.id}_${event.dateKey}_$displayTime"
            if (before30Millis >= nowMillis && !wasSentPredicate(beforeKey, event.dateKey)) {
                result += ScheduledDoctorReminder(
                    stableKey = beforeKey,
                    kind = DoctorReminderKind.BEFORE_30_MIN,
                    dateKey = event.dateKey,
                    triggerMillis = before30Millis,
                    itemTitle = event.title,
                    timeText = displayTime
                )
            }

            val atKey = "cal_at_${event.id}_${event.dateKey}_$displayTime"
            if (atTimeMillis >= nowMillis && !wasSentPredicate(atKey, event.dateKey)) {
                result += ScheduledDoctorReminder(
                    stableKey = atKey,
                    kind = DoctorReminderKind.AT_TIME,
                    dateKey = event.dateKey,
                    triggerMillis = atTimeMillis,
                    itemTitle = event.title,
                    timeText = displayTime
                )
            }
        }

        // 3. 每週課表／班表：未來 7 天內課程開始前 30 分鐘 + 課程開始時間
        val periodLookup = customTimetable.periods.associateBy { it.id }
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        for (dayOffset in 0..6) {
            val dayCal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            val dateKey = dateFormatter.format(dayCal.time)
            val dow = calendarToMonSunDayOfWeek(dayCal.get(Calendar.DAY_OF_WEEK))
            val dayCells = customTimetable.cells.filter { it.dayOfWeek == dow }
            dayCells.forEach { cell ->
                val period = periodLookup[cell.periodId] ?: return@forEach
                val hm = parseHourMinuteOrNull(period.startTime) ?: return@forEach
                val displayTime = String.format(Locale.US, "%02d:%02d", hm.first, hm.second)
                val classCal = (dayCal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, hm.first)
                    set(Calendar.MINUTE, hm.second)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val atTimeMillis = classCal.timeInMillis
                val before30Millis = atTimeMillis - 30L * 60_000L

                val beforeKey = "tt_pre30_${cell.periodId}_${dow}_${dateKey}_$displayTime"
                if (before30Millis >= nowMillis && !wasSentPredicate(beforeKey, dateKey)) {
                    result += ScheduledDoctorReminder(
                        stableKey = beforeKey,
                        kind = DoctorReminderKind.BEFORE_30_MIN,
                        dateKey = dateKey,
                        triggerMillis = before30Millis,
                        itemTitle = cell.title,
                        timeText = displayTime
                    )
                }

                val atKey = "tt_at_${cell.periodId}_${dow}_${dateKey}_$displayTime"
                if (atTimeMillis >= nowMillis && !wasSentPredicate(atKey, dateKey)) {
                    result += ScheduledDoctorReminder(
                        stableKey = atKey,
                        kind = DoctorReminderKind.AT_TIME,
                        dateKey = dateKey,
                        triggerMillis = atTimeMillis,
                        itemTitle = cell.title,
                        timeText = displayTime
                    )
                }
            }
        }

        return result
            .distinctBy { it.stableKey }
            .sortedBy { it.triggerMillis }
            .take(MAX_SCHEDULED_ALARMS)
    }

    fun syncReminders(
        context: Context,
        doctorName: String,
        tasks: List<RehabTask>,
        calendarEvents: List<CalendarEvent>,
        customTimetable: CustomTimetableConfig
    ) {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)

        val upcoming = computeUpcomingReminders(
            tasks = tasks,
            calendarEvents = calendarEvents,
            customTimetable = customTimetable,
            nowMillis = System.currentTimeMillis(),
            wasSentPredicate = { key, dateKey -> isReminderAlreadySent(appContext, key, dateKey) }
        )

        saveStoredReminders(appContext, doctorName, upcoming)
        scheduleRemindersInternal(appContext, doctorName, upcoming)
    }

    fun rescheduleStoredReminders(context: Context) {
        val appContext = context.applicationContext
        ensureNotificationChannel(appContext)
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val doctorName = prefs.getString(KEY_STORED_DOCTOR_NAME, "主治醫生") ?: "主治醫生"
        val nowMillis = System.currentTimeMillis()
        val stored = loadStoredReminders(appContext)
            .filter { it.triggerMillis > nowMillis && !isReminderAlreadySent(appContext, it.stableKey, it.dateKey) }
        scheduleRemindersInternal(appContext, doctorName, stored)
    }

    private fun scheduleRemindersInternal(
        appContext: Context,
        doctorName: String,
        reminders: List<ScheduledDoctorReminder>
    ) {
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Cancel previously scheduled alarms no longer needed
        val oldCodes = decodeIntList(prefs.getString(KEY_ACTIVE_REQUEST_CODES, null))
        oldCodes.forEach { code ->
            val cancelIntent = Intent(appContext, DoctorReminderReceiver::class.java).apply {
                action = ACTION_FIRE_DOCTOR_REMINDER
            }
            val existingPi = PendingIntent.getBroadcast(
                appContext,
                code,
                cancelIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (existingPi != null) {
                alarmManager.cancel(existingPi)
                existingPi.cancel()
            }
        }

        val newCodes = mutableListOf<Int>()
        reminders.forEach { reminder ->
            val code = reminder.requestCode()
            newCodes += code
            val intent = Intent(appContext, DoctorReminderReceiver::class.java).apply {
                action = ACTION_FIRE_DOCTOR_REMINDER
                putExtra(EXTRA_STABLE_KEY, reminder.stableKey)
                putExtra(EXTRA_KIND, reminder.kind.name)
                putExtra(EXTRA_DATE_KEY, reminder.dateKey)
                putExtra(EXTRA_TITLE, reminder.itemTitle)
                putExtra(EXTRA_TIME_TEXT, reminder.timeText)
                putExtra(EXTRA_DOCTOR_NAME, doctorName)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                appContext,
                code,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.triggerMillis,
                    pendingIntent
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to schedule alarm for ${reminder.stableKey}", e)
            }
        }

        prefs.edit()
            .putString(KEY_ACTIVE_REQUEST_CODES, JSONArray(newCodes).toString())
            .apply()
    }

    fun showDoctorNotification(
        context: Context,
        doctorName: String,
        kind: DoctorReminderKind,
        itemTitle: String,
        timeText: String,
        stableKey: String,
        dateKey: String
    ) {
        val appContext = context.applicationContext
        if (!hasNotificationPermission(appContext)) return
        if (stableKey.isNotBlank() && isReminderAlreadySent(appContext, stableKey, dateKey)) return

        ensureNotificationChannel(appContext)
        val (notifTitle, notifBody) = when (kind) {
            DoctorReminderKind.DEADLINE_TODAY -> DoctorQuotes.buildDeadlineNotificationMessage(doctorName, itemTitle)
            DoctorReminderKind.BEFORE_30_MIN -> DoctorQuotes.buildBefore30NotificationMessage(doctorName, itemTitle, timeText)
            DoctorReminderKind.AT_TIME -> DoctorQuotes.buildAtTimeNotificationMessage(doctorName, itemTitle, timeText)
        }

        val launchIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            appContext,
            abs(stableKey.hashCode()) % 100_000 + 2000,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = runCatching {
            BitmapFactory.decodeResource(appContext.resources, R.drawable.img_mascot_main)
        }.getOrNull()

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_doctor_notification)
            .setContentTitle(notifTitle)
            .setContentText(notifBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notifBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        val notifId = if (stableKey.isBlank()) {
            (System.currentTimeMillis() % 100_000).toInt()
        } else {
            (abs(stableKey.hashCode()) % 900_000) + 10_000
        }

        try {
            NotificationManagerCompat.from(appContext).notify(notifId, builder.build())
            if (stableKey.isNotBlank()) {
                markReminderSent(appContext, stableKey, dateKey)
            }
        } catch (se: SecurityException) {
            Log.w(TAG, "Missing notification permission when posting", se)
        }
    }

    fun sendTestNotification(context: Context, doctorName: String) {
        val appContext = context.applicationContext
        if (!hasNotificationPermission(appContext)) return
        ensureNotificationChannel(appContext)
        val safeDoc = doctorName.trim().ifBlank { "主治醫生" }
        val launchIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            appContext,
            9999,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val largeIcon = runCatching {
            BitmapFactory.decodeResource(appContext.resources, R.drawable.img_mascot_main)
        }.getOrNull()

        val body = "測試傳訊成功。之後在作業死線當天，以及課表或日曆行程前 30 分鐘與準點時，我都會發訊息叫你。"
        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_doctor_notification)
            .setContentTitle("$safeDoc ・提醒通道測試")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }
        try {
            NotificationManagerCompat.from(appContext).notify(9999, builder.build())
        } catch (se: SecurityException) {
            Log.w(TAG, "Missing notification permission for test notification", se)
        }
    }

    private fun isReminderAlreadySent(context: Context, stableKey: String, dateKey: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("$KEY_SENT_PREFIX${stableKey}_$dateKey", false)
    }

    private fun markReminderSent(context: Context, stableKey: String, dateKey: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("$KEY_SENT_PREFIX${stableKey}_$dateKey", true)
            .apply()
    }

    private fun saveStoredReminders(
        context: Context,
        doctorName: String,
        reminders: List<ScheduledDoctorReminder>
    ) {
        val arr = JSONArray()
        reminders.forEach { r ->
            arr.put(
                JSONObject().apply {
                    put("stableKey", r.stableKey)
                    put("kind", r.kind.name)
                    put("dateKey", r.dateKey)
                    put("triggerMillis", r.triggerMillis)
                    put("itemTitle", r.itemTitle)
                    put("timeText", r.timeText)
                }
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_STORED_DOCTOR_NAME, doctorName.ifBlank { "主治醫生" })
            .putString(KEY_STORED_REMINDERS, arr.toString())
            .apply()
    }

    private fun loadStoredReminders(context: Context): List<ScheduledDoctorReminder> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_STORED_REMINDERS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val stableKey = obj.optString("stableKey").ifBlank { continue }
                    val kind = runCatching {
                        DoctorReminderKind.valueOf(obj.optString("kind"))
                    }.getOrNull() ?: continue
                    val dateKey = obj.optString("dateKey").ifBlank { continue }
                    val triggerMillis = obj.optLong("triggerMillis", 0L)
                    if (triggerMillis <= 0L) continue
                    add(
                        ScheduledDoctorReminder(
                            stableKey = stableKey,
                            kind = kind,
                            dateKey = dateKey,
                            triggerMillis = triggerMillis,
                            itemTitle = obj.optString("itemTitle"),
                            timeText = obj.optString("timeText")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun decodeIntList(raw: String?): List<Int> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    add(arr.optInt(i))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseDateParts(dateKey: String): Triple<Int, Int, Int>? {
        val parts = dateKey.trim().split("-")
        if (parts.size != 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return null
        val d = parts[2].toIntOrNull()?.takeIf { it in 1..31 } ?: return null
        return Triple(y, m, d)
    }

    internal fun parseHourMinuteOrNull(timeText: String): Pair<Int, Int>? {
        val match = Regex("""(\d{1,2})\s*[:：]\s*(\d{2})""").find(timeText.trim()) ?: return null
        val h = match.groupValues[1].toIntOrNull()?.takeIf { it in 0..23 } ?: return null
        val m = match.groupValues[2].toIntOrNull()?.takeIf { it in 0..59 } ?: return null
        return h to m
    }

    private fun calendarToMonSunDayOfWeek(calDow: Int): Int = when (calDow) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        else -> 7
    }
}
