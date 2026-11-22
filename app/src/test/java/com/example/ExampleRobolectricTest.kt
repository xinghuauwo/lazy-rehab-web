package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CustomTimetableConfig
import com.example.data.TimetableCellColor
import com.example.data.TimetableCellEntry
import com.example.widget.TimetableWidgetSync
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("懶癌康復室", appName)
        assertEquals("每週課表／班表", context.getString(R.string.timetable_widget_label))
    }

    @Test
    fun `timetable widget renders non empty bitmap`() {
        val config = CustomTimetableConfig(
            tableTitle = "測試班表",
            cornerLabel = "節次",
            cells = listOf(
                TimetableCellEntry(
                    periodId = "p1",
                    dayOfWeek = 1,
                    title = "早班值班",
                    note = "一樓",
                    colorId = TimetableCellColor.SAKURA.id
                )
            )
        )
        val bmp = TimetableWidgetSync.renderTimetableBitmap(
            config = config,
            widthPx = 640,
            heightPx = 640,
            todayDow = 1
        )
        assertEquals(640, bmp.width)
        assertEquals(640, bmp.height)
        assertTrue(!bmp.isRecycled)

        val weekdayOnlyBmp = TimetableWidgetSync.renderTimetableBitmap(
            config = config,
            widthPx = 640,
            heightPx = 640,
            todayDow = 1,
            showWeekends = false
        )
        assertEquals(640, weekdayOnlyBmp.width)
        assertEquals(640, weekdayOnlyBmp.height)
        assertTrue(!weekdayOnlyBmp.isRecycled)
    }

    @Test
    fun `timetable widget persists showWeekends toggle per widget id`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val widgetId = 77
        assertTrue(TimetableWidgetSync.isShowWeekends(context, widgetId))
        TimetableWidgetSync.setWidgetShowWeekends(context, widgetId, false)
        assertTrue(!TimetableWidgetSync.isShowWeekends(context, widgetId))
        TimetableWidgetSync.setWidgetShowWeekends(context, widgetId, true)
        assertTrue(TimetableWidgetSync.isShowWeekends(context, widgetId))
    }
}
