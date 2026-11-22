package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.DoctorCoatStyle
import com.example.data.DoctorGearConfig
import com.example.data.DoctorHeadwearStyle
import com.example.data.DoctorQuotes
import com.example.data.DoctorStethoscopeStyle
import com.example.data.RehabCatalog
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

data class WidgetEventItem(
    val title: String,
    val startMinutes: Int? = null,
    val endMinutes: Int? = null,
    val isAllDay: Boolean = false
)

object RehabWidgetSync {
    private const val PREFS_NAME = "rehab_widget_prefs"
    private const val KEY_DOCTOR_NAME = "doctor_name"
    private const val KEY_COMPLETED_TASKS = "completed_tasks"
    private const val KEY_TOTAL_TASKS = "total_tasks"
    private const val KEY_COURSE_REMINDER = "course_reminder"
    private const val KEY_REMINDER_TITLE = "reminder_title"
    private const val KEY_REMINDER_TIME_TEXT = "reminder_time_text"
    private const val KEY_TODAY_EVENTS_JSON = "today_events_json"
    private const val KEY_HOMEWORK_DUE_TODAY_JSON = "homework_due_today_json"
    private const val KEY_COAT_ID = "doctor_coat_id"
    private const val KEY_STETHOSCOPE_ID = "doctor_stethoscope_id"
    private const val KEY_HEADWEAR_ID = "doctor_headwear_id"
    private const val KEY_CURRENT_QUOTE = "current_quote_text"
    private const val KEY_LAST_URGENT_KEY = "last_urgent_event_key"
    private const val KEY_SPRITE_INDEX = "sprite_index"

    private val doctorSpriteDrawables = listOf(
        R.drawable.img_mascot_main,
        R.drawable.img_doctor_approve,
        R.drawable.img_doctor_sipping,
        R.drawable.img_doctor_pharmacy,
        R.drawable.img_doc_idle_2,
        R.drawable.img_doc_interact,
        R.drawable.img_doc_sitting,
        R.drawable.img_doc_picked_up
    )

    internal fun resolveReminderWindow(timeText: String): DoctorQuotes.WidgetReminderWindow {
        val trimmed = timeText.trim()
        if (trimmed.isEmpty() || trimmed.contains("全")) {
            return DoctorQuotes.WidgetReminderWindow.ALL_DAY
        }
        val match = Regex("""(\d{1,2})\s*[:：]\s*(\d{2})""").find(trimmed)
            ?: return DoctorQuotes.WidgetReminderWindow.ALL_DAY
        val hour = match.groupValues[1].toIntOrNull()?.coerceIn(0, 23)
            ?: return DoctorQuotes.WidgetReminderWindow.ALL_DAY
        val minute = match.groupValues[2].toIntOrNull()?.coerceIn(0, 59)
            ?: return DoctorQuotes.WidgetReminderWindow.ALL_DAY
        return windowForMinutes(hour * 60 + minute)
    }

    private fun windowForMinutes(totalMinutes: Int): DoctorQuotes.WidgetReminderWindow = when {
        totalMinutes in 300..600 -> DoctorQuotes.WidgetReminderWindow.MORNING      // 05:00 - 10:00
        totalMinutes in 601..839 -> DoctorQuotes.WidgetReminderWindow.NOON         // 10:01 - 13:59
        totalMinutes in 840..1080 -> DoctorQuotes.WidgetReminderWindow.AFTERNOON   // 14:00 - 18:00
        else -> DoctorQuotes.WidgetReminderWindow.NIGHT                            // 18:01 - 04:59
    }

    private fun encodeEventsJson(events: List<WidgetEventItem>): String {
        val arr = JSONArray()
        events.forEach { ev ->
            val clean = ev.title.trim()
            if (clean.isNotEmpty()) {
                val obj = JSONObject()
                obj.put("title", clean)
                obj.put("startMin", ev.startMinutes ?: -1)
                obj.put("endMin", ev.endMinutes ?: -1)
                obj.put("allDay", ev.isAllDay)
                arr.put(obj)
            }
        }
        return arr.toString()
    }

    private fun decodeEventsJson(raw: String): List<WidgetEventItem> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val title = obj.optString("title").trim()
                    if (title.isEmpty()) continue
                    val startMin = obj.optInt("startMin", -1).takeIf { it in 0..1439 }
                    val endMin = obj.optInt("endMin", -1).takeIf { it in 0..1440 }
                    val allDay = obj.optBoolean("allDay", false)
                    add(WidgetEventItem(title = title, startMinutes = startMin, endMinutes = endMin, isAllDay = allDay))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun currentNowMinutes(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    private fun encodeStringListJson(items: List<String>): String {
        val arr = JSONArray()
        items.forEach { raw ->
            val clean = raw.trim()
            if (clean.isNotEmpty()) arr.put(clean)
        }
        return arr.toString()
    }

    private fun decodeStringListJson(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val v = arr.optString(i).trim()
                    if (v.isNotEmpty()) add(v)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun pickWeightedQuote(
        events: List<WidgetEventItem>,
        dueTodayHomeworkTitles: List<String>,
        fallbackTitle: String,
        fallbackTimeText: String,
        nowMinutes: Int,
        previousQuote: String?
    ): String {
        val effectiveEvents = if (events.isNotEmpty()) {
            events
        } else if (fallbackTitle.isNotBlank()) {
            val w = resolveReminderWindow(fallbackTimeText)
            val fakeStart = when (w) {
                DoctorQuotes.WidgetReminderWindow.MORNING -> 8 * 60
                DoctorQuotes.WidgetReminderWindow.NOON -> 12 * 60
                DoctorQuotes.WidgetReminderWindow.AFTERNOON -> 15 * 60
                DoctorQuotes.WidgetReminderWindow.NIGHT -> 20 * 60
                DoctorQuotes.WidgetReminderWindow.ALL_DAY -> null
            }
            listOf(
                WidgetEventItem(
                    title = fallbackTitle.trim(),
                    startMinutes = fakeStart,
                    endMinutes = fakeStart?.let { it + 60 },
                    isAllDay = fakeStart == null
                )
            )
        } else {
            emptyList()
        }

        val soonQuotes = mutableListOf<String>()
        val ongoingQuotes = mutableListOf<String>()
        val windowQuotes = mutableListOf<String>()
        val homeworkDueTodayQuotes = mutableListOf<String>()

        effectiveEvents.forEach { ev ->
            val pureTitle = "「${ev.title}」"
            val s = ev.startMinutes
            val e = ev.endMinutes ?: s?.let { (it + 60).coerceAtMost(1440) }
            if (s != null && (s - nowMinutes) in 0..30) {
                DoctorQuotes.widgetEventSoonQuotes.forEach { tpl ->
                    soonQuotes += String.format(Locale.US, tpl, pureTitle)
                }
            } else if (s != null && e != null && nowMinutes in s until e) {
                DoctorQuotes.widgetEventOngoingQuotes.forEach { tpl ->
                    ongoingQuotes += String.format(Locale.US, tpl, pureTitle)
                }
            }
            val window = when {
                ev.isAllDay || s == null -> DoctorQuotes.WidgetReminderWindow.ALL_DAY
                else -> windowForMinutes(s)
            }
            DoctorQuotes.widgetScheduleReminderQuotes[window]?.forEach { tpl ->
                windowQuotes += String.format(Locale.US, tpl, pureTitle)
            }
        }

        dueTodayHomeworkTitles.forEach { hwTitle ->
            val clean = hwTitle.trim()
            if (clean.isNotEmpty()) {
                val pureTitle = "「$clean」"
                DoctorQuotes.widgetHomeworkDueTodayQuotes.forEach { tpl ->
                    homeworkDueTodayQuotes += String.format(Locale.US, tpl, pureTitle)
                }
            }
        }

        val nowPhoneWindow = windowForMinutes(nowMinutes)
        if (effectiveEvents.isNotEmpty() && windowQuotes.isEmpty()) {
            val pickTitle = "「${effectiveEvents.first().title}」"
            DoctorQuotes.widgetScheduleReminderQuotes[nowPhoneWindow]?.forEach { tpl ->
                windowQuotes += String.format(Locale.US, tpl, pickTitle)
            }
        }

        val buckets = mutableListOf<Pair<Int, List<String>>>()
        if (soonQuotes.isNotEmpty()) buckets += 55 to soonQuotes
        if (ongoingQuotes.isNotEmpty()) buckets += 32 to ongoingQuotes
        if (homeworkDueTodayQuotes.isNotEmpty()) buckets += 20 to homeworkDueTodayQuotes
        if (windowQuotes.isNotEmpty()) buckets += 24 to windowQuotes
        buckets += 34 to RehabCatalog.feifeiQuotes

        val totalWeight = buckets.sumOf { it.first }.coerceAtLeast(1)
        repeat(4) {
            var roll = Random.nextInt(totalWeight)
            var chosenBucket = buckets.last().second
            for ((w, list) in buckets) {
                if (roll < w) {
                    chosenBucket = list
                    break
                }
                roll -= w
            }
            val candidate = chosenBucket.filter { it != previousQuote }.randomOrNull()
                ?: chosenBucket.randomOrNull()
            if (!candidate.isNullOrBlank()) return candidate
        }
        return RehabCatalog.feifeiQuotes.firstOrNull() ?: DoctorQuotes.widgetFallbackQuote
    }

    private fun urgentSignature(events: List<WidgetEventItem>, nowMinutes: Int): String {
        val soon = events.filter { ev ->
            val s = ev.startMinutes ?: return@filter false
            (s - nowMinutes) in 0..30
        }.joinToString(",") { "soon:${it.title}" }
        val ongoing = events.filter { ev ->
            val s = ev.startMinutes ?: return@filter false
            val e = ev.endMinutes ?: (s + 60)
            nowMinutes in s until e
        }.joinToString(",") { "on:${it.title}" }
        return "$soon|$ongoing"
    }

    fun syncFromApp(
        context: Context,
        doctorName: String,
        completedTasks: Int,
        totalTasks: Int,
        doctorGear: DoctorGearConfig = DoctorGearConfig(),
        todayEvents: List<WidgetEventItem> = emptyList(),
        dueTodayHomeworkTitles: List<String> = emptyList(),
        courseReminder: String = "",
        reminderTitle: String = "",
        reminderTimeText: String = ""
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val nowMin = currentNowMinutes()
        val encodedEvents = encodeEventsJson(todayEvents)
        val encodedHomework = encodeStringListJson(dueTodayHomeworkTitles)
        val urgentKey = urgentSignature(todayEvents, nowMin)
        val prevUrgentKey = prefs.getString(KEY_LAST_URGENT_KEY, "").orEmpty()
        val prevQuote = prefs.getString(KEY_CURRENT_QUOTE, "").orEmpty()

        val editor = prefs.edit()
            .putString(KEY_DOCTOR_NAME, doctorName.ifBlank { "主治醫生" })
            .putInt(KEY_COMPLETED_TASKS, completedTasks.coerceAtLeast(0))
            .putInt(KEY_TOTAL_TASKS, totalTasks.coerceAtLeast(0))
            .putString(KEY_COURSE_REMINDER, courseReminder.trim())
            .putString(KEY_REMINDER_TITLE, reminderTitle.trim())
            .putString(KEY_REMINDER_TIME_TEXT, reminderTimeText.trim())
            .putString(KEY_TODAY_EVENTS_JSON, encodedEvents)
            .putString(KEY_HOMEWORK_DUE_TODAY_JSON, encodedHomework)
            .putString(KEY_COAT_ID, doctorGear.coat.id)
            .putString(KEY_STETHOSCOPE_ID, doctorGear.stethoscope.id)
            .putString(KEY_HEADWEAR_ID, doctorGear.headwear.id)

        if (prevQuote.isBlank() ||
            prevQuote == DoctorQuotes.early8AmCourseQuote ||
            (urgentKey.isNotBlank() && urgentKey != prevUrgentKey)
        ) {
            val nextQuote = pickWeightedQuote(
                events = todayEvents,
                dueTodayHomeworkTitles = dueTodayHomeworkTitles,
                fallbackTitle = reminderTitle,
                fallbackTimeText = reminderTimeText,
                nowMinutes = nowMin,
                previousQuote = prevQuote
            )
            editor.putString(KEY_CURRENT_QUOTE, nextQuote)
        }
        editor.putString(KEY_LAST_URGENT_KEY, urgentKey)

        if (!prefs.contains(KEY_SPRITE_INDEX)) {
            val smartIdx = when {
                completedTasks >= 3 -> 1
                completedTasks >= 1 -> 2
                else -> 0
            }
            editor.putInt(KEY_SPRITE_INDEX, smartIdx)
        }
        editor.apply()
        updateAllWidgets(context)
    }

    fun cycleQuote(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val events = decodeEventsJson(prefs.getString(KEY_TODAY_EVENTS_JSON, "").orEmpty())
        val hwDueToday = decodeStringListJson(prefs.getString(KEY_HOMEWORK_DUE_TODAY_JSON, "").orEmpty())
        val reminderTitle = prefs.getString(KEY_REMINDER_TITLE, "").orEmpty()
        val reminderTimeText = prefs.getString(KEY_REMINDER_TIME_TEXT, "").orEmpty()
        val prevQuote = prefs.getString(KEY_CURRENT_QUOTE, "").orEmpty()
        val nextQuote = pickWeightedQuote(
            events = events,
            dueTodayHomeworkTitles = hwDueToday,
            fallbackTitle = reminderTitle,
            fallbackTimeText = reminderTimeText,
            nowMinutes = currentNowMinutes(),
            previousQuote = prevQuote
        )
        val nextSpriteIndex = (prefs.getInt(KEY_SPRITE_INDEX, 0) + 1) % doctorSpriteDrawables.size
        prefs.edit()
            .putString(KEY_CURRENT_QUOTE, nextQuote)
            .putInt(KEY_SPRITE_INDEX, nextSpriteIndex)
            .apply()
        updateAllWidgets(context)
    }

    fun cycleSprite(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val nextSpriteIndex = (prefs.getInt(KEY_SPRITE_INDEX, 0) + 1) % doctorSpriteDrawables.size
        prefs.edit().putInt(KEY_SPRITE_INDEX, nextSpriteIndex).apply()
        updateAllWidgets(context)
    }

    fun updateAllWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, RehabAppWidgetProvider::class.java)
        val ids = manager.getAppWidgetIds(component)
        if (ids.isEmpty()) return
        val views = buildRemoteViews(context)
        ids.forEach { appWidgetId ->
            manager.updateAppWidget(appWidgetId, views)
        }
    }

    fun buildRemoteViews(context: Context): RemoteViews {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val doctorName = prefs.getString(KEY_DOCTOR_NAME, "主治醫生").orEmpty().ifBlank { "主治醫生" }
        val completedTasks = prefs.getInt(KEY_COMPLETED_TASKS, 0)
        val totalTasks = prefs.getInt(KEY_TOTAL_TASKS, 0)
        val courseReminder = prefs.getString(KEY_COURSE_REMINDER, "").orEmpty().trim()
        val events = decodeEventsJson(prefs.getString(KEY_TODAY_EVENTS_JSON, "").orEmpty())
        val hwDueToday = decodeStringListJson(prefs.getString(KEY_HOMEWORK_DUE_TODAY_JSON, "").orEmpty())
        val reminderTitle = prefs.getString(KEY_REMINDER_TITLE, "").orEmpty().trim()
        val reminderTimeText = prefs.getString(KEY_REMINDER_TIME_TEXT, "").orEmpty().trim()

        val gear = DoctorGearConfig(
            coat = DoctorCoatStyle.fromId(prefs.getString(KEY_COAT_ID, null)),
            stethoscope = DoctorStethoscopeStyle.fromId(prefs.getString(KEY_STETHOSCOPE_ID, null)),
            headwear = DoctorHeadwearStyle.fromId(prefs.getString(KEY_HEADWEAR_ID, null))
        )

        val currentQuote = prefs.getString(KEY_CURRENT_QUOTE, "").orEmpty().ifBlank {
            pickWeightedQuote(
                events = events,
                dueTodayHomeworkTitles = hwDueToday,
                fallbackTitle = reminderTitle,
                fallbackTimeText = reminderTimeText,
                nowMinutes = currentNowMinutes(),
                previousQuote = null
            )
        }

        val spriteIdx = prefs.getInt(KEY_SPRITE_INDEX, 0).let { idx ->
            ((idx % doctorSpriteDrawables.size) + doctorSpriteDrawables.size) % doctorSpriteDrawables.size
        }
        val spriteRes = doctorSpriteDrawables[spriteIdx]
        val compositedBitmap = renderDoctorBitmapWithGear(context, spriteRes, gear)

        val statsText = if (courseReminder.isNotEmpty()) {
            "今日代辦事項 $completedTasks/$totalTasks｜$courseReminder"
        } else {
            "今日代辦事項 $completedTasks/$totalTasks"
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            1001,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextQuoteIntent = Intent(context, RehabAppWidgetProvider::class.java).apply {
            action = RehabAppWidgetProvider.ACTION_NEXT_QUOTE
        }
        val nextQuotePendingIntent = PendingIntent.getBroadcast(
            context,
            1002,
            nextQuoteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextSpriteIntent = Intent(context, RehabAppWidgetProvider::class.java).apply {
            action = RehabAppWidgetProvider.ACTION_NEXT_SPRITE
        }
        val nextSpritePendingIntent = PendingIntent.getBroadcast(
            context,
            1003,
            nextSpriteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return RemoteViews(context.packageName, R.layout.widget_rehab_doctor).apply {
            if (compositedBitmap != null) {
                setImageViewBitmap(R.id.widget_doctor_avatar, compositedBitmap)
            } else {
                setImageViewResource(R.id.widget_doctor_avatar, spriteRes)
            }
            setTextViewText(R.id.widget_doctor_name, doctorName)
            setTextViewText(R.id.widget_stats_text, statsText)
            setTextViewText(R.id.widget_quote_text, "「$currentQuote」")
            setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)
            setOnClickPendingIntent(R.id.widget_quote_container, nextQuotePendingIntent)
            setOnClickPendingIntent(R.id.widget_doctor_avatar, nextSpritePendingIntent)
        }
    }

    private fun renderDoctorBitmapWithGear(
        context: Context,
        spriteRes: Int,
        gear: DoctorGearConfig
    ): Bitmap? {
        val raw = runCatching { BitmapFactory.decodeResource(context.resources, spriteRes) }.getOrNull()
            ?: return null
        val maxH = 380f
        val scale = if (raw.height > maxH) maxH / raw.height.toFloat() else 1f
        val targetW = (raw.width * scale).roundToInt().coerceAtLeast(1)
        val targetH = (raw.height * scale).roundToInt().coerceAtLeast(1)

        val hasCustomGear = gear.coat != DoctorCoatStyle.CLASSIC_WHITE ||
            gear.stethoscope != DoctorStethoscopeStyle.DEFAULT_HEART ||
            gear.headwear != DoctorHeadwearStyle.NONE

        val baseScaled = if (scale < 0.99f) {
            Bitmap.createScaledBitmap(raw, targetW, targetH, true).also {
                if (it !== raw) raw.recycle()
            }
        } else {
            raw
        }

        if (!hasCustomGear) return baseScaled

        val out = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawBitmap(baseScaled, 0f, 0f, null)
        if (baseScaled !== raw) baseScaled.recycle()

        val w = targetW.toFloat()
        val h = targetH.toFloat()
        val outlineW = (min(w, h) * 0.015f).coerceAtLeast(1.1f)
        val outlineBrown = Color.parseColor("#FF4B3532")

        drawCoatAndroid(canvas, gear.coat, w, h, outlineW, outlineBrown)
        drawStethoscopeAndroid(canvas, gear.stethoscope, w, h, outlineW, outlineBrown)
        drawHeadwearAndroid(canvas, gear.headwear, w, h, outlineW, outlineBrown)
        return out
    }

    private fun drawCoatAndroid(
        canvas: Canvas,
        coat: DoctorCoatStyle,
        w: Float,
        h: Float,
        outlineW: Float,
        outlineBrown: Int
    ) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = outlineBrown
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        when (coat) {
            DoctorCoatStyle.CLASSIC_WHITE -> Unit
            DoctorCoatStyle.STAR_DIRECTOR -> {
                drawStarAndroid(canvas, w * 0.355f, h * 0.485f, w * 0.048f, Color.parseColor("#FFFFD54F"), outlineW * 0.9f, outlineBrown)
                strokePaint.color = Color.parseColor("#FF7986CB")
                strokePaint.strokeWidth = outlineW * 2.1f
                canvas.drawLine(w * 0.69f, h * 0.618f, w * 0.72f, h * 0.588f, strokePaint)
                fillPaint.color = Color.parseColor("#FFFFD54F")
                canvas.drawCircle(w * 0.72f, h * 0.588f, w * 0.022f, fillPaint)
                strokePaint.color = outlineBrown
                strokePaint.strokeWidth = outlineW * 0.65f
                canvas.drawCircle(w * 0.72f, h * 0.588f, w * 0.022f, strokePaint)
                fillPaint.color = Color.parseColor("#D990CAF9")
                canvas.drawRoundRect(RectF(w * 0.25f, h * 0.578f, w * 0.34f, h * 0.593f), 6f, 6f, fillPaint)
                canvas.drawRoundRect(RectF(w * 0.67f, h * 0.578f, w * 0.76f, h * 0.593f), 6f, 6f, fillPaint)
            }
            DoctorCoatStyle.CAT_PAW_COAT -> {
                val cx = w * 0.50f
                val cy = h * 0.475f
                val hw = w * 0.062f
                val hh = w * 0.025f
                val bow = Path().apply {
                    moveTo(cx, cy); lineTo(cx - hw, cy - hh); lineTo(cx - hw, cy + hh); close()
                    moveTo(cx, cy); lineTo(cx + hw, cy - hh); lineTo(cx + hw, cy + hh); close()
                }
                fillPaint.color = Color.parseColor("#FFF48FB1")
                strokePaint.strokeWidth = outlineW * 0.85f
                canvas.drawPath(bow, fillPaint)
                canvas.drawPath(bow, strokePaint)
                canvas.drawCircle(cx, cy, hh * 0.55f, fillPaint)
                canvas.drawCircle(cx, cy, hh * 0.55f, strokePaint)
                drawPawAndroid(canvas, w * 0.32f, h * 0.65f, w * 0.036f, Color.parseColor("#FFF8BBD0"), outlineW * 0.75f, outlineBrown)
                drawPawAndroid(canvas, w * 0.68f, h * 0.65f, w * 0.036f, Color.parseColor("#FFF8BBD0"), outlineW * 0.75f, outlineBrown)
            }
            DoctorCoatStyle.PASTEL_MINT_COAT -> {
                strokePaint.color = Color.parseColor("#FF80CBC4")
                strokePaint.strokeWidth = outlineW * 1.9f
                canvas.drawLine(w * 0.365f, h * 0.470f, w * 0.395f, h * 0.625f, strokePaint)
                canvas.drawLine(w * 0.635f, h * 0.470f, w * 0.605f, h * 0.625f, strokePaint)
                val tagRect = RectF(w * 0.635f, h * 0.568f, w * 0.750f, h * 0.598f)
                fillPaint.color = Color.parseColor("#FFE0F2F1")
                canvas.drawRoundRect(tagRect, 6f, 6f, fillPaint)
                strokePaint.color = outlineBrown
                strokePaint.strokeWidth = outlineW * 0.7f
                canvas.drawRoundRect(tagRect, 6f, 6f, strokePaint)
                strokePaint.color = Color.parseColor("#FF00897B")
                strokePaint.strokeWidth = outlineW * 0.9f
                canvas.drawLine(tagRect.left + tagRect.width() * 0.22f, tagRect.centerY(), tagRect.left + tagRect.width() * 0.78f, tagRect.centerY(), strokePaint)
            }
        }
    }

    private fun drawStethoscopeAndroid(
        canvas: Canvas,
        stethoscope: DoctorStethoscopeStyle,
        w: Float,
        h: Float,
        outlineW: Float,
        outlineBrown: Int
    ) {
        if (stethoscope == DoctorStethoscopeStyle.DEFAULT_HEART) return
        val tubeColor = when (stethoscope) {
            DoctorStethoscopeStyle.GOLD_VIP -> Color.parseColor("#FFFFCA28")
            DoctorStethoscopeStyle.CAT_BELL -> Color.parseColor("#FF81D4FA")
            DoctorStethoscopeStyle.BOBA_SCOPE -> Color.parseColor("#FFD7A86E")
            DoctorStethoscopeStyle.DEFAULT_HEART -> Color.GRAY
        }
        val neckPath = Path().apply {
            moveTo(w * 0.375f, h * 0.525f)
            cubicTo(w * 0.385f, h * 0.452f, w * 0.425f, h * 0.448f, w * 0.438f, h * 0.478f)
            moveTo(w * 0.578f, h * 0.460f)
            cubicTo(w * 0.605f, h * 0.458f, w * 0.625f, h * 0.486f, w * 0.625f, h * 0.520f)
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        strokePaint.color = outlineBrown
        strokePaint.strokeWidth = outlineW * 3.1f
        canvas.drawPath(neckPath, strokePaint)
        strokePaint.color = tubeColor
        strokePaint.strokeWidth = outlineW * 1.75f
        canvas.drawPath(neckPath, strokePaint)

        val cx = w * 0.626f
        val cy = h * 0.528f
        val r = w * 0.045f
        when (stethoscope) {
            DoctorStethoscopeStyle.GOLD_VIP -> {
                fillPaint.color = Color.parseColor("#FFFFD54F")
                canvas.drawCircle(cx, cy, r, fillPaint)
                strokePaint.color = outlineBrown
                strokePaint.strokeWidth = outlineW * 0.85f
                canvas.drawCircle(cx, cy, r, strokePaint)
                drawStarAndroid(canvas, cx, cy, r * 0.72f, Color.parseColor("#FFFFF8E1"), outlineW * 0.55f, outlineBrown)
            }
            DoctorStethoscopeStyle.CAT_BELL -> {
                val eo = r * 0.62f
                val leftEar = Path().apply {
                    moveTo(cx - eo * 1.1f, cy - r * 0.4f); lineTo(cx - eo * 1.35f, cy - r * 1.35f); lineTo(cx - eo * 0.25f, cy - r * 0.85f); close()
                }
                val rightEar = Path().apply {
                    moveTo(cx + eo * 1.1f, cy - r * 0.4f); lineTo(cx + eo * 1.35f, cy - r * 1.35f); lineTo(cx + eo * 0.25f, cy - r * 0.85f); close()
                }
                fillPaint.color = Color.parseColor("#FFFFCDD2")
                strokePaint.color = outlineBrown
                strokePaint.strokeWidth = outlineW * 0.75f
                canvas.drawPath(leftEar, fillPaint); canvas.drawPath(leftEar, strokePaint)
                canvas.drawPath(rightEar, fillPaint); canvas.drawPath(rightEar, strokePaint)
                fillPaint.color = Color.parseColor("#FFFFE082")
                canvas.drawCircle(cx, cy, r, fillPaint)
                strokePaint.strokeWidth = outlineW * 0.85f
                canvas.drawCircle(cx, cy, r, strokePaint)
                canvas.drawLine(cx - r * 0.48f, cy + r * 0.22f, cx + r * 0.48f, cy + r * 0.22f, strokePaint)
                fillPaint.color = outlineBrown
                canvas.drawCircle(cx, cy + r * 0.1f, r * 0.18f, fillPaint)
            }
            DoctorStethoscopeStyle.BOBA_SCOPE -> {
                val cupW = r * 1.65f
                val cupH = r * 1.9f
                val cupTop = cy - cupH * 0.45f
                val cupLeft = cx - cupW / 2f
                strokePaint.color = Color.parseColor("#BF9C27B0")
                strokePaint.strokeWidth = outlineW * 1.3f
                canvas.drawLine(cx + cupW * 0.12f, cupTop + cupH * 0.25f, cx + cupW * 0.24f, cupTop - cupH * 0.42f, strokePaint)
                val cupRect = RectF(cupLeft, cupTop, cupLeft + cupW, cupTop + cupH)
                fillPaint.color = Color.parseColor("#FFE6C29F")
                canvas.drawRoundRect(cupRect, 5f, 5f, fillPaint)
                strokePaint.color = outlineBrown
                strokePaint.strokeWidth = outlineW * 0.8f
                canvas.drawRoundRect(cupRect, 5f, 5f, strokePaint)
                fillPaint.color = outlineBrown
                val pr = r * 0.17f
                canvas.drawCircle(cx - cupW * 0.22f, cupTop + cupH * 0.72f, pr, fillPaint)
                canvas.drawCircle(cx, cupTop + cupH * 0.76f, pr, fillPaint)
                canvas.drawCircle(cx + cupW * 0.22f, cupTop + cupH * 0.72f, pr, fillPaint)
            }
            DoctorStethoscopeStyle.DEFAULT_HEART -> Unit
        }
    }

    private fun drawHeadwearAndroid(
        canvas: Canvas,
        headwear: DoctorHeadwearStyle,
        w: Float,
        h: Float,
        outlineW: Float,
        outlineBrown: Int
    ) {
        if (headwear == DoctorHeadwearStyle.NONE) return
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = outlineBrown
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        when (headwear) {
            DoctorHeadwearStyle.NONE -> Unit
            DoctorHeadwearStyle.CAT_EARS -> {
                val ew = w * 0.17f
                val eh = h * 0.088f
                listOf(w * 0.34f to -0.22f, w * 0.66f to 0.22f).forEach { (bx, tilt) ->
                    val by = h * 0.115f
                    val tipX = bx + ew * tilt
                    val tipY = by - eh
                    val outer = Path().apply {
                        moveTo(bx - ew * 0.55f, by); lineTo(tipX, tipY); lineTo(bx + ew * 0.55f, by); close()
                    }
                    val inner = Path().apply {
                        moveTo(bx - ew * 0.28f, by - eh * 0.08f); lineTo(tipX, by - eh * 0.68f); lineTo(bx + ew * 0.28f, by - eh * 0.08f); close()
                    }
                    fillPaint.color = Color.parseColor("#FFF6F5FA")
                    strokePaint.strokeWidth = outlineW * 1.05f
                    canvas.drawPath(outer, fillPaint)
                    canvas.drawPath(outer, strokePaint)
                    fillPaint.color = Color.parseColor("#FFF8BBD0")
                    canvas.drawPath(inner, fillPaint)
                }
            }
            DoctorHeadwearStyle.HEAD_MIRROR -> {
                val bandY = h * 0.138f
                val band = Path().apply {
                    moveTo(w * 0.23f, bandY + h * 0.025f)
                    quadTo(w * 0.50f, bandY - h * 0.02f, w * 0.77f, bandY + h * 0.025f)
                }
                strokePaint.color = outlineBrown
                strokePaint.strokeWidth = outlineW * 2.5f
                canvas.drawPath(band, strokePaint)
                strokePaint.color = Color.parseColor("#FFDCE4EC")
                strokePaint.strokeWidth = outlineW * 1.4f
                canvas.drawPath(band, strokePaint)

                val cx = w * 0.48f
                val cy = h * 0.125f
                val dr = w * 0.092f
                fillPaint.color = Color.parseColor("#FFE3EEF8")
                canvas.drawCircle(cx, cy, dr, fillPaint)
                fillPaint.color = Color.parseColor("#73B0C4DE")
                canvas.drawCircle(cx, cy, dr * 0.74f, fillPaint)
                strokePaint.color = Color.WHITE
                strokePaint.strokeWidth = outlineW * 1.1f
                canvas.drawLine(cx - dr * 0.48f, cy - dr * 0.35f, cx - dr * 0.12f, cy - dr * 0.55f, strokePaint)
                strokePaint.color = outlineBrown
                strokePaint.strokeWidth = outlineW * 0.95f
                canvas.drawCircle(cx, cy, dr, strokePaint)
                fillPaint.color = outlineBrown
                canvas.drawCircle(cx, cy, dr * 0.19f, fillPaint)
            }
            DoctorHeadwearStyle.FLOWER_CROWN -> {
                val crownY = h * 0.105f
                val vine = Path().apply {
                    moveTo(w * 0.26f, crownY + h * 0.032f)
                    quadTo(w * 0.50f, crownY - h * 0.020f, w * 0.74f, crownY + h * 0.032f)
                }
                strokePaint.color = Color.parseColor("#FF81C784")
                strokePaint.strokeWidth = outlineW * 2.0f
                canvas.drawPath(vine, strokePaint)
                val flowers = listOf(
                    Triple(w * 0.30f to crownY + h * 0.020f, Color.parseColor("#FFFFCDD2"), w * 0.044f),
                    Triple(w * 0.40f to crownY + h * 0.004f, Color.parseColor("#FFFFF9C4"), w * 0.048f),
                    Triple(w * 0.50f to crownY - h * 0.004f, Color.WHITE, w * 0.052f),
                    Triple(w * 0.60f to crownY + h * 0.005f, Color.parseColor("#FFBBDEFB"), w * 0.046f),
                    Triple(w * 0.70f to crownY + h * 0.020f, Color.parseColor("#FFFFCDD2"), w * 0.044f)
                )
                flowers.forEach { (pt, petalColor, r) ->
                    for (i in 0 until 5) {
                        val ang = i * (2.0 * PI / 5.0)
                        val px = pt.first + (r * 0.55f * cos(ang)).toFloat()
                        val py = pt.second + (r * 0.55f * sin(ang)).toFloat()
                        fillPaint.color = petalColor
                        canvas.drawCircle(px, py, r * 0.46f, fillPaint)
                        strokePaint.color = Color.argb(153, 75, 53, 50)
                        strokePaint.strokeWidth = outlineW * 0.4f
                        canvas.drawCircle(px, py, r * 0.46f, strokePaint)
                    }
                    fillPaint.color = Color.parseColor("#FFFFD54F")
                    canvas.drawCircle(pt.first, pt.second, r * 0.34f, fillPaint)
                }
            }
            DoctorHeadwearStyle.PAW_CLIP -> {
                val cx = w * 0.675f
                val cy = h * 0.165f
                val rect = RectF(cx - w * 0.065f, cy - h * 0.005f, cx + w * 0.065f, cy + h * 0.009f)
                fillPaint.color = Color.parseColor("#FFE0E0E0")
                canvas.drawRoundRect(rect, 6f, 6f, fillPaint)
                strokePaint.color = outlineBrown
                strokePaint.strokeWidth = outlineW * 0.65f
                canvas.drawRoundRect(rect, 6f, 6f, strokePaint)
                drawPawAndroid(canvas, cx + w * 0.015f, cy - h * 0.004f, w * 0.042f, Color.parseColor("#FFFFABBE"), outlineW * 0.85f, outlineBrown)
            }
        }
    }

    private fun drawStarAndroid(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        outerRadius: Float,
        fillColor: Int,
        outlineW: Float,
        outlineBrown: Int
    ) {
        val innerRadius = outerRadius * 0.45f
        val path = Path()
        for (i in 0 until 10) {
            val angle = -PI / 2.0 + i * (PI / 5.0)
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val px = cx + (r * cos(angle)).toFloat()
            val py = cy + (r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = fillColor }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = outlineBrown
            strokeWidth = outlineW
            strokeJoin = Paint.Join.ROUND
        }
        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, strokePaint)
    }

    private fun drawPawAndroid(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        padRadius: Float,
        fillColor: Int,
        outlineW: Float,
        outlineBrown: Int
    ) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = fillColor }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = outlineBrown
            strokeWidth = outlineW * 0.7f
        }
        val toeR = padRadius * 0.42f
        listOf(
            cx - padRadius * 0.95f to cy - padRadius * 0.85f,
            cx to cy - padRadius * 1.15f,
            cx + padRadius * 0.95f to cy - padRadius * 0.85f
        ).forEach { (tx, ty) ->
            canvas.drawCircle(tx, ty, toeR, fillPaint)
            canvas.drawCircle(tx, ty, toeR, strokePaint)
        }
        canvas.drawCircle(cx, cy, padRadius, fillPaint)
        strokePaint.strokeWidth = outlineW * 0.8f
        canvas.drawCircle(cx, cy, padRadius, strokePaint)
    }
}
