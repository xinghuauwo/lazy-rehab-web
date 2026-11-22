package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.RemoteViews
import com.example.R
import com.example.data.CustomTimetableConfig
import com.example.data.TimetableCellColor
import com.example.data.TimetableCellEntry
import com.example.data.TimetablePeriodRow
import com.example.data.currentDayOfWeekMonToSun
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

object TimetableWidgetSync {
    private const val PREFS_NAME = "rehab_timetable_widget_prefs"
    private const val KEY_TIMETABLE_JSON = "custom_timetable_json"
    private const val KEY_DEFAULT_SHOW_WEEKENDS = "default_show_weekends"

    private const val MAX_BITMAP_PIXELS = 900_000f

    private fun keyWidgetShowWeekends(appWidgetId: Int): String = "show_weekends_$appWidgetId"

    fun isShowWeekends(
        context: Context,
        appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    ): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID && prefs.contains(keyWidgetShowWeekends(appWidgetId))) {
            return prefs.getBoolean(keyWidgetShowWeekends(appWidgetId), true)
        }
        return prefs.getBoolean(KEY_DEFAULT_SHOW_WEEKENDS, true)
    }

    fun setWidgetShowWeekends(context: Context, appWidgetId: Int, showWeekends: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(keyWidgetShowWeekends(appWidgetId), showWeekends)
            .putBoolean(KEY_DEFAULT_SHOW_WEEKENDS, showWeekends)
            .apply()
    }

    fun setGlobalShowWeekends(context: Context, showWeekends: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit().putBoolean(KEY_DEFAULT_SHOW_WEEKENDS, showWeekends)
        val manager = runCatching { AppWidgetManager.getInstance(context) }.getOrNull()
        val widgetIds = runCatching {
            val component = ComponentName(context, TimetableAppWidgetProvider::class.java)
            manager?.getAppWidgetIds(component) ?: IntArray(0)
        }.getOrElse { IntArray(0) }
        widgetIds.forEach { widgetId ->
            editor.putBoolean(keyWidgetShowWeekends(widgetId), showWeekends)
        }
        editor.apply()
        updateAllWidgets(context)
    }

    fun onWidgetsDeleted(context: Context, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        appWidgetIds.forEach { widgetId ->
            editor.remove(keyWidgetShowWeekends(widgetId))
        }
        editor.apply()
    }

    fun syncFromApp(context: Context, config: CustomTimetableConfig) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TIMETABLE_JSON, encodeTimetable(config)).apply()
        updateAllWidgets(context)
    }

    fun updateAllWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, TimetableAppWidgetProvider::class.java)
        val widgetIds = manager.getAppWidgetIds(component)
        if (widgetIds.isEmpty()) return
        val config = loadTimetable(context)
        widgetIds.forEach { widgetId ->
            updateWidgetWithConfig(context, manager, widgetId, config)
        }
    }

    fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val config = loadTimetable(context)
        updateWidgetWithConfig(context, appWidgetManager, appWidgetId, config)
    }

    private fun updateWidgetWithConfig(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        config: CustomTimetableConfig
    ) {
        val options = runCatching { appWidgetManager.getAppWidgetOptions(appWidgetId) }
            .getOrElse { Bundle.EMPTY }
        val (targetWidthPx, targetHeightPx) = resolveBitmapDimensionsPx(context, options)
        val showWeekends = isShowWeekends(context, appWidgetId)
        val bitmap = renderTimetableBitmap(
            config = config,
            widthPx = targetWidthPx,
            heightPx = targetHeightPx,
            todayDow = currentDayOfWeekMonToSun(),
            showWeekends = showWeekends
        )
        val views = RemoteViews(context.packageName, R.layout.widget_timetable).apply {
            setImageViewBitmap(R.id.widget_timetable_image, bitmap)
        }
        // Intentionally NO PendingIntent / click handler on timetable cells to prevent accidental launcher taps.
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun resolveBitmapDimensionsPx(context: Context, options: Bundle): Pair<Int, Int> {
        val density = context.resources.displayMetrics.density.coerceIn(1.5f, 4f)
        val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
        val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)

        val widthDp = when {
            minW > 0 && maxW > 0 -> ((minW + maxW) * 0.5f)
            minW > 0 -> minW.toFloat()
            maxW > 0 -> maxW.toFloat()
            else -> 320f
        }.coerceIn(120f, 640f)

        val heightDp = when {
            minH > 0 && maxH > 0 -> ((minH + maxH) * 0.5f)
            minH > 0 -> minH.toFloat()
            maxH > 0 -> maxH.toFloat()
            else -> 320f
        }.coerceIn(120f, 760f)

        var wPx = (widthDp * density).roundToInt().coerceIn(320, 1080)
        var hPx = (heightDp * density).roundToInt().coerceIn(320, 1280)
        val totalPixels = wPx.toFloat() * hPx.toFloat()
        if (totalPixels > MAX_BITMAP_PIXELS) {
            val scale = sqrt(MAX_BITMAP_PIXELS / totalPixels)
            wPx = (wPx * scale).roundToInt().coerceAtLeast(240)
            hPx = (hPx * scale).roundToInt().coerceAtLeast(240)
        }
        return wPx to hPx
    }

    internal fun renderTimetableBitmap(
        config: CustomTimetableConfig,
        widthPx: Int,
        heightPx: Int,
        todayDow: Int,
        showWeekends: Boolean = true
    ): Bitmap {
        val safeW = widthPx.coerceAtLeast(240)
        val safeH = heightPx.coerceAtLeast(240)
        val bitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgCream = Color.parseColor("#FFFDF8")
        val titleBannerBg = Color.parseColor("#FDE4E9")
        val headerBarBg = Color.parseColor("#FFF4F6")
        val todayHeaderBg = Color.parseColor("#FCE0E8")
        val todayColBg = Color.parseColor("#FFF8FA")
        val periodColBg = Color.parseColor("#FFF8F9")
        val inkColor = Color.parseColor("#432E2B")
        val mutedInk = Color.parseColor("#7D635F")
        val todayInk = Color.parseColor("#D45D82")
        val gridLineColor = Color.parseColor("#D5B2BC")
        val cardRadius = min(safeW, safeH) * 0.03f

        canvas.drawColor(Color.TRANSPARENT)

        val outerRect = RectF(1f, 1f, safeW - 1f, safeH - 1f)
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgCream
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(outerRect, cardRadius, cardRadius, cardPaint)

        val clipPath = android.graphics.Path().apply {
            addRoundRect(outerRect, cardRadius, cardRadius, android.graphics.Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clipPath)

        val titleH = (safeH * 0.085f).coerceIn(28f, 68f)
        val headerH = (safeH * 0.075f).coerceIn(24f, 56f)
        val bodyTop = titleH + headerH
        val bodyH = (safeH - bodyTop).coerceAtLeast(40f)

        val periods = config.periods.ifEmpty {
            listOf(TimetablePeriodRow("empty", "1", "09:00", "10:00"))
        }
        val rowCount = periods.size.coerceAtLeast(1)
        val rowH = bodyH / rowCount.toFloat()

        val dayCount = if (showWeekends) 7 else 5
        val periodColW = (safeW * 0.145f).coerceIn(48f, 150f)
        val dayColW = (safeW - periodColW) / dayCount.toFloat()

        // 1. Title banner
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        fillPaint.color = titleBannerBg
        canvas.drawRect(0f, 0f, safeW.toFloat(), titleH, fillPaint)

        val titleTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = (titleH * 0.48f).coerceIn(13f, 30f)
        }
        drawCenteredText(
            canvas = canvas,
            text = config.tableTitle.ifBlank { "我的每週課表／班表" },
            paint = titleTextPaint,
            left = 10f,
            top = 0f,
            right = safeW - 10f,
            bottom = titleH,
            maxLines = 1
        )

        // 2. Day Header Row
        fillPaint.color = headerBarBg
        canvas.drawRect(0f, titleH, safeW.toFloat(), bodyTop, fillPaint)

        val headerTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = min(headerH * 0.46f, dayColW * 0.34f).coerceIn(11f, 24f)
        }
        drawCenteredText(
            canvas = canvas,
            text = config.cornerLabel.ifBlank { "節次" },
            paint = headerTextPaint,
            left = 2f,
            top = titleH,
            right = periodColW - 2f,
            bottom = bodyTop,
            maxLines = 1
        )

        val dayLabels = if (showWeekends) {
            listOf("一", "二", "三", "四", "五", "六", "日")
        } else {
            listOf("一", "二", "三", "四", "五")
        }
        dayLabels.forEachIndexed { idx, shortDay ->
            val dow = idx + 1
            val isToday = dow == todayDow
            val xLeft = periodColW + idx * dayColW
            val xRight = xLeft + dayColW
            if (isToday) {
                fillPaint.color = todayHeaderBg
                canvas.drawRect(xLeft, titleH, xRight, bodyTop, fillPaint)
            }
            headerTextPaint.color = if (isToday) todayInk else inkColor
            val headerStr = if (isToday && dayColW >= 78f) "$shortDay・今" else shortDay
            drawCenteredText(
                canvas = canvas,
                text = headerStr,
                paint = headerTextPaint,
                left = xLeft + 2f,
                top = titleH,
                right = xRight - 2f,
                bottom = bodyTop,
                maxLines = 1
            )
        }

        // 3. Period rows + Course/Shift cells
        val cellLookup = config.cells.associateBy { it.periodId to it.dayOfWeek }
        val periodLabelSize = min(rowH * 0.30f, periodColW * 0.25f).coerceIn(9f, 21f)
        val periodTimeSize = min(rowH * 0.21f, periodColW * 0.17f).coerceIn(7.5f, 15f)
        val cellTitleSize = min(rowH * 0.25f, dayColW * 0.22f).coerceIn(8.5f, 19f)
        val cellNoteSize = min(rowH * 0.19f, dayColW * 0.17f).coerceIn(7.5f, 14f)

        val periodLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = inkColor
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = periodLabelSize
        }
        val periodTimePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedInk
            textSize = periodTimeSize
        }
        val cellTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = cellTitleSize
        }
        val cellNotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = cellNoteSize
        }

        periods.forEachIndexed { rowIdx, period ->
            val yTop = bodyTop + rowIdx * rowH
            val yBottom = yTop + rowH

            // Left period & time cell
            fillPaint.color = periodColBg
            canvas.drawRect(0f, yTop, periodColW, yBottom, fillPaint)

            val showSeparateTimes = periodColW < 84f && rowH >= 48f
            val timeText = if (showSeparateTimes) {
                "${period.startTime}\n${period.endTime}"
            } else {
                "${period.startTime}-${period.endTime}"
            }
            if (rowH >= 38f) {
                val labelTop = yTop + 2f
                val labelBottom = yTop + rowH * 0.56f
                drawCenteredText(
                    canvas = canvas,
                    text = period.label,
                    paint = periodLabelPaint,
                    left = 2f,
                    top = labelTop,
                    right = periodColW - 2f,
                    bottom = labelBottom,
                    maxLines = 2
                )
                drawCenteredText(
                    canvas = canvas,
                    text = timeText,
                    paint = periodTimePaint,
                    left = 2f,
                    top = labelBottom - 1f,
                    right = periodColW - 2f,
                    bottom = yBottom - 2f,
                    maxLines = if (showSeparateTimes) 2 else 1
                )
            } else {
                drawCenteredText(
                    canvas = canvas,
                    text = period.label.replace("\n", " "),
                    paint = periodLabelPaint,
                    left = 2f,
                    top = yTop + 1f,
                    right = periodColW - 2f,
                    bottom = yBottom - 1f,
                    maxLines = 1
                )
            }

            // Day cells (Mon..Sun or Mon..Fri)
            for (dow in 1..dayCount) {
                val xLeft = periodColW + (dow - 1) * dayColW
                val xRight = xLeft + dayColW
                val cell = cellLookup[period.id to dow]
                if (cell != null) {
                    val palette = TimetableCellColor.fromId(cell.colorId)
                    fillPaint.color = palette.bgArgb.toInt()
                    canvas.drawRect(xLeft, yTop, xRight, yBottom, fillPaint)

                    val fgColor = palette.fgArgb.toInt()
                    cellTitlePaint.color = fgColor
                    cellNotePaint.color = Color.argb(
                        215,
                        Color.red(fgColor),
                        Color.green(fgColor),
                        Color.blue(fgColor)
                    )

                    val showNote = cell.note.isNotBlank() && rowH >= 42f && dayColW >= 46f
                    val padX = (dayColW * 0.06f).coerceIn(2f, 6f)
                    if (showNote) {
                        val splitY = yTop + rowH * 0.62f
                        drawCenteredText(
                            canvas = canvas,
                            text = cell.title,
                            paint = cellTitlePaint,
                            left = xLeft + padX,
                            top = yTop + 2f,
                            right = xRight - padX,
                            bottom = splitY,
                            maxLines = if (rowH >= 64f) 3 else 2
                        )
                        drawCenteredText(
                            canvas = canvas,
                            text = cell.note,
                            paint = cellNotePaint,
                            left = xLeft + padX,
                            top = splitY,
                            right = xRight - padX,
                            bottom = yBottom - 2f,
                            maxLines = 1
                        )
                    } else {
                        drawCenteredText(
                            canvas = canvas,
                            text = cell.title,
                            paint = cellTitlePaint,
                            left = xLeft + padX,
                            top = yTop + 2f,
                            right = xRight - padX,
                            bottom = yBottom - 2f,
                            maxLines = if (rowH >= 52f) 3 else 2
                        )
                    }
                } else if (dow == todayDow) {
                    fillPaint.color = todayColBg
                    canvas.drawRect(xLeft, yTop, xRight, yBottom, fillPaint)
                }
            }
        }

        // 4. Crisp grid lines
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gridLineColor
            style = Paint.Style.STROKE
            strokeWidth = (min(safeW, safeH) * 0.0028f).coerceIn(1f, 2.2f)
        }
        canvas.drawLine(0f, titleH, safeW.toFloat(), titleH, linePaint)
        canvas.drawLine(0f, bodyTop, safeW.toFloat(), bodyTop, linePaint)
        for (rowIdx in 1 until rowCount) {
            val y = bodyTop + rowIdx * rowH
            canvas.drawLine(0f, y, safeW.toFloat(), y, linePaint)
        }
        canvas.drawLine(periodColW, titleH, periodColW, safeH.toFloat(), linePaint)
        for (colIdx in 1 until dayCount) {
            val x = periodColW + colIdx * dayColW
            canvas.drawLine(x, titleH, x, safeH.toFloat(), linePaint)
        }

        canvas.restore()

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E0B8C3")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(outerRect, cardRadius, cardRadius, borderPaint)

        return bitmap
    }

    private fun drawCenteredText(
        canvas: Canvas,
        text: String,
        paint: TextPaint,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        maxLines: Int
    ) {
        val availW = (right - left).roundToInt().coerceAtLeast(8)
        val availH = (bottom - top).coerceAtLeast(8f)
        val clean = text.trim()
        if (clean.isEmpty()) return

        val layout = StaticLayout.Builder
            .obtain(clean, 0, clean.length, paint, availW)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .setMaxLines(maxLines.coerceAtLeast(1))
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()

        val textH = layout.height.toFloat()
        val startY = top + ((availH - textH) * 0.5f).coerceAtLeast(0f)

        canvas.save()
        canvas.clipRect(left, top, right, bottom)
        canvas.translate(left, startY)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun loadTimetable(context: Context): CustomTimetableConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_TIMETABLE_JSON, null)
        if (raw.isNullOrBlank()) return CustomTimetableConfig()
        return decodeTimetable(raw)
    }

    private fun encodeTimetable(config: CustomTimetableConfig): String {
        val root = JSONObject()
        root.put("tableTitle", config.tableTitle)
        root.put("cornerLabel", config.cornerLabel)
        val periodsArr = JSONArray()
        config.periods.forEach { p ->
            periodsArr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("label", p.label)
                    put("startTime", p.startTime)
                    put("endTime", p.endTime)
                }
            )
        }
        root.put("periods", periodsArr)
        val cellsArr = JSONArray()
        config.cells.forEach { c ->
            cellsArr.put(
                JSONObject().apply {
                    put("periodId", c.periodId)
                    put("dayOfWeek", c.dayOfWeek)
                    put("title", c.title)
                    put("note", c.note)
                    put("colorId", c.colorId)
                }
            )
        }
        root.put("cells", cellsArr)
        return root.toString()
    }

    private fun decodeTimetable(raw: String): CustomTimetableConfig {
        return try {
            val root = JSONObject(raw)
            val title = root.optString("tableTitle", "我的每週課表／班表").ifBlank { "我的每週課表／班表" }
            val corner = root.optString("cornerLabel", "節次").ifBlank { "節次" }
            val pArr = root.optJSONArray("periods")
            val periods = buildList {
                if (pArr != null) {
                    for (i in 0 until pArr.length()) {
                        val o = pArr.optJSONObject(i) ?: continue
                        val pid = o.optString("id").trim().ifEmpty { continue }
                        val lbl = o.optString("label").ifBlank { "${i + 1}" }
                        val st = o.optString("startTime", "09:00").ifBlank { "09:00" }
                        val et = o.optString("endTime", "10:00").ifBlank { "10:00" }
                        add(TimetablePeriodRow(pid, lbl, st, et))
                    }
                }
            }.ifEmpty { CustomTimetableConfig.defaultPeriods() }
            val cArr = root.optJSONArray("cells")
            val cells = buildList {
                if (cArr != null) {
                    for (i in 0 until cArr.length()) {
                        val o = cArr.optJSONObject(i) ?: continue
                        val pid = o.optString("periodId").trim().ifEmpty { continue }
                        val dow = o.optInt("dayOfWeek", 1).coerceIn(1, 7)
                        val cTitle = o.optString("title").trim().ifEmpty { continue }
                        val cNote = o.optString("note").trim()
                        val cColor = o.optString("colorId", TimetableCellColor.SKY.id)
                        add(TimetableCellEntry(pid, dow, cTitle, cNote, cColor))
                    }
                }
            }
            CustomTimetableConfig(
                tableTitle = title,
                cornerLabel = corner,
                periods = periods,
                cells = cells
            )
        } catch (_: Exception) {
            CustomTimetableConfig()
        }
    }
}
