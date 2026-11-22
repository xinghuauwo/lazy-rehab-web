package com.example

import com.example.data.DoctorQuotes
import com.example.data.EnergyLevel
import com.example.data.RehabCheckin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun checkin_preserves_exact_percent_and_falls_back_gracefully() {
        val exactCheckin = RehabCheckin(
            id = "2026-11-21",
            dateKey = "2026-11-21",
            energyLevel = EnergyLevel.LEVEL_7.wireValue,
            energyPercent = 67
        )
        assertEquals(67, exactCheckin.resolvedPercent())
        assertEquals("67% 居然有點電", exactCheckin.formattedEnergyTag())

        val zeroCheckin = RehabCheckin(
            id = "2026-11-20",
            dateKey = "2026-11-20",
            energyLevel = EnergyLevel.LEVEL_1.wireValue,
            energyPercent = 0
        )
        assertEquals(0, zeroCheckin.resolvedPercent())
        assertEquals("0% 完全休眠", zeroCheckin.formattedEnergyTag())

        val legacyCheckin = RehabCheckin(
            id = "2026-11-19",
            dateKey = "2026-11-19",
            energyLevel = EnergyLevel.LEVEL_4.wireValue,
            energyPercent = null
        )
        assertEquals(40, legacyCheckin.resolvedPercent())
        assertEquals("40% 勉強開機", legacyCheckin.formattedEnergyTag())
    }

    @Test
    fun gym_intro_uses_minigame_text_without_daydream_wording() {
        assertTrue(DoctorQuotes.gymFirstVisitBody.contains("簡單的復健用小遊戲"))
        assertFalse(DoctorQuotes.gymFirstVisitBody.contains("發呆"))
        assertFalse(DoctorQuotes.gymFirstVisitTitle.contains("發呆"))
    }

    @Test
    fun custom_split_slots_evaluate_only_enabled_slots() {
        val morningEveningTask = com.example.data.RehabTask(
            id = "t1",
            title = "吃藥",
            timeSlotsEnabled = true,
            morningEnabled = true,
            noonEnabled = false,
            eveningEnabled = true,
            morningDone = true,
            noonDone = false,
            eveningDone = false
        )
        assertEquals(listOf(0, 2), morningEveningTask.activeTimeSlotIndices())
        assertFalse(morningEveningTask.allEnabledTimeSlotsDone())
        assertTrue(morningEveningTask.allEnabledTimeSlotsDone(evening = true))

        val noonEveningTask = morningEveningTask.copy(
            morningEnabled = false,
            noonEnabled = true,
            eveningEnabled = true,
            noonDone = true,
            eveningDone = true
        )
        assertEquals(listOf(1, 2), noonEveningTask.activeTimeSlotIndices())
        assertTrue(noonEveningTask.allEnabledTimeSlotsDone())
    }

    @Test
    fun no_hospital_director_wording_in_quotes_or_styles() {
        val allTextChunks = DoctorQuotes.allDoctorQuotes +
            DoctorQuotes.hospitalBanterQuotes +
            DoctorQuotes.notificationDeadlineQuotes +
            DoctorQuotes.notificationBefore30MinQuotes +
            DoctorQuotes.notificationAtTimeQuotes +
            listOf(DoctorQuotes.gymFirstVisitTitle, DoctorQuotes.gymFirstVisitBody) +
            com.example.data.DoctorStethoscopeStyle.entries.flatMap {
                listOf(it.titleZh, it.badgeZh, it.descriptionZh)
            }
        assertTrue(allTextChunks.none { it.contains("院長") || it.contains("主治醫師") })

        val d = DoctorQuotes.buildDeadlineNotificationMessage("主治醫生", "微積分作業")
        val b = DoctorQuotes.buildBefore30NotificationMessage("主治醫生", "演算法課", "14:30")
        val a = DoctorQuotes.buildAtTimeNotificationMessage("主治醫生", "演算法課", "14:30")
        assertFalse(d.first.contains("院長") || d.second.contains("院長"))
        assertFalse(b.first.contains("院長") || b.second.contains("院長"))
        assertFalse(a.first.contains("院長") || a.second.contains("院長"))
        assertTrue(d.first.contains("主治醫生"))
        assertTrue(d.second.contains("微積分作業"))
        assertTrue(b.second.contains("演算法課") && b.second.contains("14:30"))
        assertTrue(a.second.contains("演算法課") && a.second.contains("14:30"))
    }

    @Test
    fun default_doctor_name_is_zhuzhi_yisheng_without_legacy_branding() {
        val defaultProfile = com.example.data.RehabProfile()
        assertEquals("主治醫生", defaultProfile.feifeiNickname)

        val defaultNotification = DoctorQuotes.buildDeadlineNotificationMessage("", "測試作業")
        assertTrue(defaultNotification.first.startsWith("主治醫生"))

        val legacyForbidden = listOf("主治醫師", "廢廢", "復健所", "廢人小事", "廢物復健", "廢物勳章")
        val catalogText = DoctorQuotes.allDoctorQuotes +
            DoctorQuotes.hospitalBanterQuotes +
            com.example.data.PrescriptionCatalog.allPrescriptions.flatMap { listOf(it.title, it.doctorNote) } +
            com.example.data.RehabCatalog.pharmacyItems.flatMap { listOf(it.name, it.description) }
        legacyForbidden.forEach { term ->
            assertTrue("Unexpected legacy term '$term'", catalogText.none { it.contains(term) })
        }
    }

    @Test
    fun checkin_completedTaskTitles_defaults_empty_and_preserves_snapshots() {
        val emptyCheckin = RehabCheckin(id = "2026-11-22", dateKey = "2026-11-22")
        assertTrue(emptyCheckin.completedTaskTitles.isEmpty())

        val snapshotCheckin = RehabCheckin(
            id = "2026-11-22",
            dateKey = "2026-11-22",
            completedTaskTitles = listOf("喝三口溫水", "做一題微積分")
        )
        assertEquals(listOf("喝三口溫水", "做一題微積分"), snapshotCheckin.completedTaskTitles)
    }

    @Test
    fun doctorNotificationScheduler_schedules_deadlines_calendar_and_timetable_accurately() {
        // Fixed reference time: 2026-11-23 07:00:00 (Monday)
        val calBase = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 23, 7, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val nowMillis = calBase.timeInMillis

        fun expectedMillis(year: Int, monthZeroBased: Int, day: Int, hour: Int, min: Int): Long =
            java.util.Calendar.getInstance().apply {
                set(year, monthZeroBased, day, hour, min, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis

        val uncompletedHomework = com.example.data.RehabTask(
            id = "hw1",
            title = "心理學報告",
            category = com.example.data.TaskCategory.HOMEWORK.wireValue,
            completed = false,
            deadlineDate = "2026-11-23"
        )
        val completedHomework = com.example.data.RehabTask(
            id = "hw2",
            title = "已完成作業",
            category = com.example.data.TaskCategory.HOMEWORK.wireValue,
            completed = true,
            deadlineDate = "2026-11-23"
        )
        val timedEvent = com.example.data.CalendarEvent(
            id = "ev1",
            dateKey = "2026-11-23",
            timeText = "14:30",
            title = "專題討論"
        )
        val allDayEvent = com.example.data.CalendarEvent(
            id = "ev2",
            dateKey = "2026-11-23",
            timeText = "全日",
            title = "校慶停課日"
        )
        val timetable = com.example.data.CustomTimetableConfig(
            periods = listOf(
                com.example.data.TimetablePeriodRow(
                    id = "p_mon",
                    label = "3",
                    startTime = "10:10",
                    endTime = "11:00"
                )
            ),
            cells = listOf(
                com.example.data.TimetableCellEntry(
                    periodId = "p_mon",
                    dayOfWeek = 1, // Monday
                    title = "動畫製作"
                )
            )
        )

        val reminders = com.example.notifications.DoctorNotificationScheduler.computeUpcomingReminders(
            tasks = listOf(uncompletedHomework, completedHomework),
            calendarEvents = listOf(timedEvent, allDayEvent),
            customTimetable = timetable,
            nowMillis = nowMillis
        )

        // 1. Homework deadline at 09:00; completed homework excluded
        val deadlineItems = reminders.filter {
            it.kind == com.example.notifications.DoctorReminderKind.DEADLINE_TODAY
        }
        assertEquals(1, deadlineItems.size)
        assertEquals("心理學報告", deadlineItems.first().itemTitle)
        assertEquals(
            expectedMillis(2026, java.util.Calendar.NOVEMBER, 23, 9, 0),
            deadlineItems.first().triggerMillis
        )

        // 2. Timed CalendarEvent (14:30) -> 14:00 BEFORE_30_MIN & 14:30 AT_TIME
        val timedPre30 = reminders.first { it.itemTitle == "專題討論" && it.kind == com.example.notifications.DoctorReminderKind.BEFORE_30_MIN }
        val timedAt = reminders.first { it.itemTitle == "專題討論" && it.kind == com.example.notifications.DoctorReminderKind.AT_TIME }
        assertEquals(expectedMillis(2026, java.util.Calendar.NOVEMBER, 23, 14, 0), timedPre30.triggerMillis)
        assertEquals(expectedMillis(2026, java.util.Calendar.NOVEMBER, 23, 14, 30), timedAt.triggerMillis)

        // 3. All-day CalendarEvent ("全日") -> 08:00 BEFORE_30_MIN & 08:30 AT_TIME
        val allDayPre30 = reminders.first { it.itemTitle == "校慶停課日" && it.kind == com.example.notifications.DoctorReminderKind.BEFORE_30_MIN }
        val allDayAt = reminders.first { it.itemTitle == "校慶停課日" && it.kind == com.example.notifications.DoctorReminderKind.AT_TIME }
        assertEquals(expectedMillis(2026, java.util.Calendar.NOVEMBER, 23, 8, 0), allDayPre30.triggerMillis)
        assertEquals(expectedMillis(2026, java.util.Calendar.NOVEMBER, 23, 8, 30), allDayAt.triggerMillis)

        // 4. CustomTimetableConfig Monday 10:10 -> 09:40 BEFORE_30_MIN & 10:10 AT_TIME
        val ttPre30 = reminders.first { it.itemTitle == "動畫製作" && it.kind == com.example.notifications.DoctorReminderKind.BEFORE_30_MIN }
        val ttAt = reminders.first { it.itemTitle == "動畫製作" && it.kind == com.example.notifications.DoctorReminderKind.AT_TIME }
        assertEquals(expectedMillis(2026, java.util.Calendar.NOVEMBER, 23, 9, 40), ttPre30.triggerMillis)
        assertEquals(expectedMillis(2026, java.util.Calendar.NOVEMBER, 23, 10, 10), ttAt.triggerMillis)
    }
}
