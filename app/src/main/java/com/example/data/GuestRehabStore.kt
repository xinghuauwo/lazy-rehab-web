package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.Timestamp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date
import java.util.UUID

const val GUEST_USER_ID = "local_guest_user"

private val Context.guestRehabDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "feifei_guest_rehab"
)

class GuestRehabStore private constructor(private val context: Context) {

    private val isGuestKey = booleanPreferencesKey("is_guest_mode")
    private val snapshotJsonKey = stringPreferencesKey("guest_snapshot_json")
    private val doctorCoatIdKey = stringPreferencesKey("doctor_coat_id")
    private val doctorStethoscopeIdKey = stringPreferencesKey("doctor_stethoscope_id")
    private val doctorHeadwearIdKey = stringPreferencesKey("doctor_headwear_id")
    private val clinicVisitsKey = intPreferencesKey("otome_clinic_visits")
    private val lastClinicDateKey = stringPreferencesKey("otome_last_clinic_date")
    private val secretLettersCountKey = intPreferencesKey("otome_secret_letters_count")
    private val drawnHeartRxIdKey = stringPreferencesKey("otome_drawn_heart_rx_id")
    private val drawnHeartRxDateKey = stringPreferencesKey("otome_drawn_heart_rx_date")
    private val enabledTaskCategoriesKey = stringPreferencesKey("enabled_task_categories")
    private val customTaskCategoriesKey = stringPreferencesKey("custom_task_categories_json")
    private val otomeClinicThemeIdKey = stringPreferencesKey("otome_clinic_theme_id")
    private val otomeClinicThemeConfigKey = stringPreferencesKey("otome_clinic_theme_config_json")
    private val hasSeenFirstWelcomeKey = booleanPreferencesKey("has_seen_first_welcome")
    private val hasSeenRehabGymIntroKey = booleanPreferencesKey("has_seen_rehab_gym_intro")
    private val courseScheduleJsonKey = stringPreferencesKey("course_schedule_json")
    private val customTimetableJsonKey = stringPreferencesKey("custom_timetable_json")
    private val calendarEventsJsonKey = stringPreferencesKey("calendar_events_json")
    private val rehabPointsKey = longPreferencesKey("rehab_game_points")
    private val lastRehabTickKey = longPreferencesKey("rehab_last_tick_millis")
    private val unlockedCgIdsKey = stringPreferencesKey("unlocked_cg_ids_json")
    private val specialChannelUnlockedKey = booleanPreferencesKey("special_channel_unlocked")
    private val syncedCloudUidKey = stringPreferencesKey("synced_cloud_uid")

    val clinicThemeFlow: Flow<OtomeClinicTheme> = context.guestRehabDataStore.data
        .map { prefs ->
            val rawConfig = prefs[otomeClinicThemeConfigKey]
            val idFromConfig = if (!rawConfig.isNullOrBlank()) {
                try {
                    JSONObject(rawConfig).optString("themeId")
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }
            OtomeClinicTheme.fromId(idFromConfig?.ifBlank { null } ?: prefs[otomeClinicThemeIdKey])
        }
        .distinctUntilChanged()

    val hasSeenFirstWelcomeFlow: Flow<Boolean> = context.guestRehabDataStore.data
        .map { prefs -> prefs[hasSeenFirstWelcomeKey] ?: false }
        .distinctUntilChanged()

    val hasSeenRehabGymIntroFlow: Flow<Boolean> = context.guestRehabDataStore.data
        .map { prefs -> prefs[hasSeenRehabGymIntroKey] ?: false }
        .distinctUntilChanged()

    val customCategoriesFlow: Flow<List<String>> = context.guestRehabDataStore.data
        .map { prefs -> decodeStringList(prefs[customTaskCategoriesKey]) }
        .distinctUntilChanged()

    val visibleCategoryWiresFlow: Flow<Set<String>> = context.guestRehabDataStore.data
        .map { prefs ->
            val raw = prefs[enabledTaskCategoriesKey]
            val parsed = decodeCategoryWires(raw)
            parsed.ifEmpty { setOf(TaskCategory.DAILY.wireValue) }
        }
        .distinctUntilChanged()

    val enabledCategoriesFlow: Flow<Set<TaskCategory>> = visibleCategoryWiresFlow
        .map { wires ->
            wires.mapNotNull { w -> TaskCategory.entries.firstOrNull { it.wireValue == w } }
                .toSet()
                .ifEmpty { setOf(TaskCategory.DAILY) }
        }
        .distinctUntilChanged()

    val isGuestModeFlow: Flow<Boolean> = context.guestRehabDataStore.data
        .map { prefs -> prefs[isGuestKey] ?: false }
        .distinctUntilChanged()

    val doctorGearFlow: Flow<DoctorGearConfig> = context.guestRehabDataStore.data
        .map { prefs ->
            DoctorGearConfig(
                coat = DoctorCoatStyle.fromId(prefs[doctorCoatIdKey]),
                stethoscope = DoctorStethoscopeStyle.fromId(prefs[doctorStethoscopeIdKey]),
                headwear = DoctorHeadwearStyle.fromId(prefs[doctorHeadwearIdKey])
            )
        }
        .distinctUntilChanged()

    val courseScheduleFlow: Flow<List<CourseScheduleEntry>> = context.guestRehabDataStore.data
        .map { prefs ->
            decodeStringList(prefs[courseScheduleJsonKey])
                .mapNotNull { CourseScheduleEntry.decode(it) }
                .sortedWith(compareBy<CourseScheduleEntry> { it.dayOfWeek }.thenBy { it.startTime })
        }
        .distinctUntilChanged()

    val customTimetableFlow: Flow<CustomTimetableConfig> = context.guestRehabDataStore.data
        .map { prefs ->
            val raw = prefs[customTimetableJsonKey]
            if (!raw.isNullOrBlank()) {
                decodeCustomTimetable(raw)
            } else {
                val legacy = decodeStringList(prefs[courseScheduleJsonKey])
                    .mapNotNull { CourseScheduleEntry.decode(it) }
                migrateLegacyToTimetable(legacy)
            }
        }
        .distinctUntilChanged()

    val calendarEventsFlow: Flow<List<CalendarEvent>> = context.guestRehabDataStore.data
        .map { prefs ->
            decodeStringList(prefs[calendarEventsJsonKey])
                .mapNotNull { CalendarEvent.decode(it) }
                .sortedWith(compareBy<CalendarEvent> { it.dateKey }.thenBy { it.timeText })
        }
        .distinctUntilChanged()

    val rehabPointsFlow: Flow<Long> = context.guestRehabDataStore.data
        .map { prefs -> (prefs[rehabPointsKey] ?: 0L).coerceAtLeast(0L) }
        .distinctUntilChanged()

    val unlockedCgIdsFlow: Flow<Set<String>> = context.guestRehabDataStore.data
        .map { prefs -> decodeStringList(prefs[unlockedCgIdsKey]).toSet() }
        .distinctUntilChanged()

    val specialChannelUnlockedFlow: Flow<Boolean> = context.guestRehabDataStore.data
        .map { prefs -> prefs[specialChannelUnlockedKey] ?: false }
        .distinctUntilChanged()

    data class OtomeClinicProgress(
        val clinicVisits: Int = 0,
        val lastClinicDate: String = "",
        val secretLettersGiven: Int = 0,
        val drawnHeartRxId: String = "",
        val drawnHeartRxDate: String = ""
    )

    val otomeClinicFlow: Flow<OtomeClinicProgress> = context.guestRehabDataStore.data
        .map { prefs ->
            OtomeClinicProgress(
                clinicVisits = prefs[clinicVisitsKey] ?: 0,
                lastClinicDate = prefs[lastClinicDateKey] ?: "",
                secretLettersGiven = prefs[secretLettersCountKey] ?: 0,
                drawnHeartRxId = prefs[drawnHeartRxIdKey] ?: "",
                drawnHeartRxDate = prefs[drawnHeartRxDateKey] ?: ""
            )
        }
        .distinctUntilChanged()

    private val stateFlow: Flow<GuestLocalState> = context.guestRehabDataStore.data
        .map { prefs -> decodeState(prefs[snapshotJsonKey]) }

    val profileFlow: Flow<RehabProfile?> = stateFlow
        .map { it.profile }
        .distinctUntilChanged()

    val tasksFlow: Flow<List<RehabTask>> = stateFlow
        .map { it.tasks }
        .distinctUntilChanged()

    val checkinsFlow: Flow<List<RehabCheckin>> = stateFlow
        .map { it.checkins }
        .distinctUntilChanged()

    suspend fun setGuestMode(enabled: Boolean) {
        context.guestRehabDataStore.edit { prefs ->
            prefs[isGuestKey] = enabled
        }
    }

    suspend fun setClinicTheme(theme: OtomeClinicTheme) {
        context.guestRehabDataStore.edit { prefs ->
            prefs[otomeClinicThemeIdKey] = theme.id
            prefs[otomeClinicThemeConfigKey] = theme.toConfigJson()
        }
    }

    suspend fun markFirstWelcomeSeen() {
        context.guestRehabDataStore.edit { prefs ->
            prefs[hasSeenFirstWelcomeKey] = true
        }
    }

    suspend fun markRehabGymIntroSeen() {
        context.guestRehabDataStore.edit { prefs ->
            prefs[hasSeenRehabGymIntroKey] = true
        }
    }

    suspend fun setSpecialChannelUnlocked(unlocked: Boolean) {
        context.guestRehabDataStore.edit { prefs ->
            prefs[specialChannelUnlockedKey] = unlocked
        }
    }

    suspend fun setVisibleCategoryWires(wires: Set<String>) {
        val safe = wires.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            .ifEmpty { setOf(TaskCategory.DAILY.wireValue) }
        context.guestRehabDataStore.edit { prefs ->
            prefs[enabledTaskCategoriesKey] = JSONArray(safe.toList()).toString()
        }
    }

    suspend fun setEnabledCategories(categories: Set<TaskCategory>) {
        setVisibleCategoryWires(categories.map { it.wireValue }.toSet())
    }

    suspend fun addCustomCategory(rawName: String): String? {
        val clean = rawName.trim().take(20)
        if (clean.isEmpty()) return null
        val matchedBuiltIn = TaskCategory.entries.firstOrNull {
            it.labelZh == clean || it.wireValue.equals(clean, ignoreCase = true)
        }
        if (matchedBuiltIn != null) {
            context.guestRehabDataStore.edit { prefs ->
                val currentVisible = decodeCategoryWires(prefs[enabledTaskCategoriesKey])
                    .ifEmpty { setOf(TaskCategory.DAILY.wireValue) }
                prefs[enabledTaskCategoriesKey] = JSONArray((currentVisible + matchedBuiltIn.wireValue).toList()).toString()
            }
            return matchedBuiltIn.wireValue
        }
        context.guestRehabDataStore.edit { prefs ->
            val currentCustom = decodeStringList(prefs[customTaskCategoriesKey])
            val nextCustom = if (clean in currentCustom) currentCustom else currentCustom + clean
            prefs[customTaskCategoriesKey] = JSONArray(nextCustom).toString()

            val currentVisible = decodeCategoryWires(prefs[enabledTaskCategoriesKey])
                .ifEmpty { setOf(TaskCategory.DAILY.wireValue) }
            prefs[enabledTaskCategoriesKey] = JSONArray((currentVisible + clean).toList()).toString()
        }
        return clean
    }

    suspend fun removeCustomCategory(wire: String) {
        if (TaskCategory.entries.any { it.wireValue == wire }) return
        context.guestRehabDataStore.edit { prefs ->
            val nextCustom = decodeStringList(prefs[customTaskCategoriesKey]).filterNot { it == wire }
            prefs[customTaskCategoriesKey] = JSONArray(nextCustom).toString()

            val nextVisible = (decodeCategoryWires(prefs[enabledTaskCategoriesKey]) - wire)
                .ifEmpty { setOf(TaskCategory.DAILY.wireValue) }
            prefs[enabledTaskCategoriesKey] = JSONArray(nextVisible.toList()).toString()
        }
    }

    suspend fun setDoctorCoat(coat: DoctorCoatStyle) {
        context.guestRehabDataStore.edit { prefs ->
            prefs[doctorCoatIdKey] = coat.id
        }
    }

    suspend fun setDoctorStethoscope(stethoscope: DoctorStethoscopeStyle) {
        context.guestRehabDataStore.edit { prefs ->
            prefs[doctorStethoscopeIdKey] = stethoscope.id
        }
    }

    suspend fun setDoctorHeadwear(headwear: DoctorHeadwearStyle) {
        context.guestRehabDataStore.edit { prefs ->
            prefs[doctorHeadwearIdKey] = headwear.id
        }
    }

    suspend fun upsertCourseScheduleEntry(entry: CourseScheduleEntry) {
        context.guestRehabDataStore.edit { prefs ->
            val current = decodeStringList(prefs[courseScheduleJsonKey])
                .mapNotNull { CourseScheduleEntry.decode(it) }
            val next = current.filterNot { it.id == entry.id } + entry
            prefs[courseScheduleJsonKey] = JSONArray(next.map { it.encode() }).toString()
        }
    }

    suspend fun deleteCourseScheduleEntry(entryId: String) {
        context.guestRehabDataStore.edit { prefs ->
            val current = decodeStringList(prefs[courseScheduleJsonKey])
                .mapNotNull { CourseScheduleEntry.decode(it) }
            val next = current.filterNot { it.id == entryId }
            prefs[courseScheduleJsonKey] = JSONArray(next.map { it.encode() }).toString()
        }
    }

    suspend fun updateTimetable(transform: (CustomTimetableConfig) -> CustomTimetableConfig) {
        context.guestRehabDataStore.edit { prefs ->
            val raw = prefs[customTimetableJsonKey]
            val current = if (!raw.isNullOrBlank()) {
                decodeCustomTimetable(raw)
            } else {
                val legacy = decodeStringList(prefs[courseScheduleJsonKey])
                    .mapNotNull { CourseScheduleEntry.decode(it) }
                migrateLegacyToTimetable(legacy)
            }
            val next = transform(current)
            prefs[customTimetableJsonKey] = encodeCustomTimetable(next)
        }
    }

    suspend fun upsertCalendarEvent(event: CalendarEvent) {
        context.guestRehabDataStore.edit { prefs ->
            val current = decodeStringList(prefs[calendarEventsJsonKey])
                .mapNotNull { CalendarEvent.decode(it) }
            val next = current.filterNot { it.id == event.id } + event
            prefs[calendarEventsJsonKey] = JSONArray(next.map { it.encode() }).toString()
        }
    }

    suspend fun deleteCalendarEvent(eventId: String) {
        context.guestRehabDataStore.edit { prefs ->
            val current = decodeStringList(prefs[calendarEventsJsonKey])
                .mapNotNull { CalendarEvent.decode(it) }
            val next = current.filterNot { it.id == eventId }
            prefs[calendarEventsJsonKey] = JSONArray(next.map { it.encode() }).toString()
        }
    }

    /**
     * Idle accrual for 復健室: 60 復健點數 / 小時 = 1 點 / 分鐘.
     */
    suspend fun accrueIdleRehabPoints(nowMillis: Long = System.currentTimeMillis()): Long {
        var accrued = 0L
        context.guestRehabDataStore.edit { prefs ->
            val lastTick = prefs[lastRehabTickKey] ?: 0L
            if (lastTick <= 0L) {
                prefs[lastRehabTickKey] = nowMillis
            } else {
                val elapsed = (nowMillis - lastTick).coerceAtLeast(0L)
                val earned = (elapsed / 60_000L).coerceIn(0L, 1440L)
                if (earned > 0L) {
                    val currentPoints = (prefs[rehabPointsKey] ?: 0L).coerceAtLeast(0L)
                    prefs[rehabPointsKey] = (currentPoints + earned).coerceAtMost(9_999_999L)
                    prefs[lastRehabTickKey] = lastTick + earned * 60_000L
                    accrued = earned
                }
            }
        }
        return accrued
    }

    suspend fun addRehabPoints(delta: Long) {
        if (delta == 0L) return
        context.guestRehabDataStore.edit { prefs ->
            val current = (prefs[rehabPointsKey] ?: 0L).coerceAtLeast(0L)
            prefs[rehabPointsKey] = (current + delta).coerceIn(0L, 9_999_999L)
        }
    }

    suspend fun setRehabPoints(exactPoints: Long) {
        context.guestRehabDataStore.edit { prefs ->
            prefs[rehabPointsKey] = exactPoints.coerceIn(0L, 9_999_999L)
        }
    }

    suspend fun unlockSpecialCg(cg: SpecialCgItem): Result<Unit> {
        return try {
            context.guestRehabDataStore.edit { prefs ->
                val unlocked = decodeStringList(prefs[unlockedCgIdsKey]).toMutableSet()
                if (cg.id in unlocked) {
                    throw IllegalStateException("「${cg.title}」已經解鎖過囉！")
                }
                val currentPoints = (prefs[rehabPointsKey] ?: 0L).coerceAtLeast(0L)
                if (currentPoints < cg.costPoints) {
                    throw IllegalStateException("復健點數還差 ${cg.costPoints - currentPoints} 點，去復健室陪醫生散步或玩小遊戲吧！")
                }
                prefs[rehabPointsKey] = currentPoints - cg.costPoints
                unlocked.add(cg.id)
                prefs[unlockedCgIdsKey] = JSONArray(unlocked.toList()).toString()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordClinicInteraction(todayDate: String) {
        context.guestRehabDataStore.edit { prefs ->
            val current = prefs[clinicVisitsKey] ?: 0
            prefs[clinicVisitsKey] = current + 1
            prefs[lastClinicDateKey] = todayDate
        }
    }

    suspend fun recordSecretLetterGiven(todayDate: String) {
        context.guestRehabDataStore.edit { prefs ->
            val currentVisits = prefs[clinicVisitsKey] ?: 0
            val currentLetters = prefs[secretLettersCountKey] ?: 0
            prefs[clinicVisitsKey] = currentVisits + 1
            prefs[secretLettersCountKey] = currentLetters + 1
            prefs[lastClinicDateKey] = todayDate
        }
    }

    suspend fun saveDrawnHeartRx(rxId: String, todayDate: String) {
        context.guestRehabDataStore.edit { prefs ->
            prefs[drawnHeartRxIdKey] = rxId
            prefs[drawnHeartRxDateKey] = todayDate
        }
    }

    private fun decodeStringList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val value = arr.optString(i).trim()
                    if (value.isNotEmpty() && value !in this) add(value)
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encodeCustomTimetable(config: CustomTimetableConfig): String {
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

    private fun decodeCustomTimetable(raw: String): CustomTimetableConfig {
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

    private fun migrateLegacyToTimetable(legacy: List<CourseScheduleEntry>): CustomTimetableConfig {
        val defaultPeriods = CustomTimetableConfig.defaultPeriods()
        if (legacy.isEmpty()) return CustomTimetableConfig(periods = defaultPeriods)
        val extraPeriods = mutableListOf<TimetablePeriodRow>()
        val cells = mutableListOf<TimetableCellEntry>()
        legacy.forEach { entry ->
            val matchedPeriod = defaultPeriods.firstOrNull { p ->
                p.startTime == entry.startTime ||
                    p.startTime.take(2) == entry.startTime.take(2)
            }
            val periodId = if (matchedPeriod != null) {
                matchedPeriod.id
            } else {
                val existingExtra = extraPeriods.firstOrNull {
                    it.startTime == entry.startTime && it.endTime == entry.endTime
                }
                if (existingExtra != null) {
                    existingExtra.id
                } else {
                    val newId = "legacy_${extraPeriods.size + 1}"
                    extraPeriods += TimetablePeriodRow(
                        id = newId,
                        label = "${defaultPeriods.size + extraPeriods.size + 1}",
                        startTime = entry.startTime,
                        endTime = entry.endTime
                    )
                    newId
                }
            }
            cells.removeAll { it.periodId == periodId && it.dayOfWeek == entry.dayOfWeek }
            cells += TimetableCellEntry(
                periodId = periodId,
                dayOfWeek = entry.dayOfWeek,
                title = entry.title,
                note = entry.location,
                colorId = TimetableCellColor.SKY.id
            )
        }
        val allPeriods = (defaultPeriods + extraPeriods).sortedBy { it.startTime }
        return CustomTimetableConfig(periods = allPeriods, cells = cells)
    }

    private fun decodeCategoryWires(raw: String?): Set<String> {
        if (raw.isNullOrBlank()) return emptySet()
        val trimmed = raw.trim()
        return if (trimmed.startsWith("[")) {
            decodeStringList(trimmed).toSet()
        } else {
            trimmed.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        }
    }

    suspend fun updateState(
        markLocalDirty: Boolean = true,
        transform: (GuestLocalState) -> GuestLocalState
    ) {
        context.guestRehabDataStore.edit { prefs ->
            val current = decodeState(prefs[snapshotJsonKey])
            val next = transform(current)
            prefs[snapshotJsonKey] = encodeState(next)
            if (markLocalDirty) {
                prefs[syncedCloudUidKey] = ""
            }
        }
    }

    suspend fun getSyncedCloudUid(): String =
        context.guestRehabDataStore.data.map { it[syncedCloudUidKey].orEmpty() }.first()

    suspend fun setSyncedCloudUid(uid: String) {
        context.guestRehabDataStore.edit { prefs ->
            prefs[syncedCloudUidKey] = uid
        }
    }

    suspend fun overwriteLocalFromCloud(
        uid: String,
        profile: RehabProfile?,
        tasks: List<RehabTask>,
        checkins: List<RehabCheckin>
    ) {
        val normalizedProfile = profile?.let {
            val cleanNick = it.feifeiNickname.trim().let { nick ->
                if (nick.isBlank() || nick == "主治醫師") "主治醫生" else nick
            }
            it.copy(
                id = GUEST_USER_ID,
                userId = GUEST_USER_ID,
                feifeiNickname = cleanNick
            )
        }
        val normalizedTasks = tasks.map { it.copy(userId = GUEST_USER_ID) }
        val normalizedCheckins = checkins.map { it.copy(userId = GUEST_USER_ID) }
        context.guestRehabDataStore.edit { prefs ->
            prefs[snapshotJsonKey] = encodeState(
                GuestLocalState(
                    profile = normalizedProfile,
                    tasks = normalizedTasks,
                    checkins = normalizedCheckins
                )
            )
            prefs[syncedCloudUidKey] = uid
        }
    }

    suspend fun readLocalStateSnapshot(): GuestLocalState = stateFlow.first()

    data class GuestLocalState(
        val profile: RehabProfile? = null,
        val tasks: List<RehabTask> = emptyList(),
        val checkins: List<RehabCheckin> = emptyList()
    )

    private fun encodeState(state: GuestLocalState): String {
        val root = JSONObject()
        state.profile?.let { profile ->
            val pObj = JSONObject().apply {
                put("userId", profile.userId)
                put("displayName", profile.displayName)
                put("feifeiNickname", profile.feifeiNickname)
                put("todayDate", profile.todayDate)
                put("todayCount", profile.todayCount)
                put("totalCompleted", profile.totalCompleted)
                put("ownedItemIds", JSONArray(profile.ownedItemIds))
                put("equippedItemIds", JSONArray(profile.equippedItemIds))
                put("wardPlacements", JSONArray(profile.wardPlacements))
            }
            root.put("profile", pObj)
        }

        val tasksArr = JSONArray()
        state.tasks.forEach { task ->
            tasksArr.put(
                JSONObject().apply {
                    put("id", task.id)
                    put("userId", task.userId)
                    put("title", task.title)
                    put("effortLevel", task.effortLevel)
                    put("category", task.category)
                    put("completed", task.completed)
                    put("completedDate", task.completedDate)
                    put("note", task.note)
                    put("sortOrder", task.sortOrder)
                    put("timeSlotsEnabled", task.timeSlotsEnabled)
                    put("morningDone", task.morningDone)
                    put("noonDone", task.noonDone)
                    put("eveningDone", task.eveningDone)
                    put("morningEnabled", task.morningEnabled)
                    put("noonEnabled", task.noonEnabled)
                    put("eveningEnabled", task.eveningEnabled)
                    put("resetHour", task.resetHour)
                    put("deadlineDate", task.deadlineDate)
                    put("createdAtMillis", task.createdAt?.toDate()?.time ?: 0L)
                    put("updatedAtMillis", task.updatedAt?.toDate()?.time ?: 0L)
                }
            )
        }
        root.put("tasks", tasksArr)

        val checkinsArr = JSONArray()
        state.checkins.forEach { checkin ->
            checkinsArr.put(
                JSONObject().apply {
                    put("id", checkin.id)
                    put("userId", checkin.userId)
                    put("dateKey", checkin.dateKey)
                    put("energyLevel", checkin.energyLevel)
                    checkin.energyPercent?.let { put("energyPercent", it) }
                    put("reflection", checkin.reflection)
                    put("wardSnapshot", JSONArray(checkin.wardSnapshot))
                    put("doctorPoseSnapshot", checkin.doctorPoseSnapshot)
                    put("completedTaskTitles", JSONArray(checkin.completedTaskTitles))
                    put("createdAtMillis", checkin.createdAt?.toDate()?.time ?: 0L)
                    put("updatedAtMillis", checkin.updatedAt?.toDate()?.time ?: 0L)
                }
            )
        }
        root.put("checkins", checkinsArr)
        return root.toString()
    }

    private fun decodeState(raw: String?): GuestLocalState {
        if (raw.isNullOrBlank()) return GuestLocalState()
        return try {
            val root = JSONObject(raw)
            val profile = root.optJSONObject("profile")?.let { pObj ->
                RehabProfile(
                    id = GUEST_USER_ID,
                    userId = pObj.optString("userId", GUEST_USER_ID),
                    displayName = pObj.optString("displayName", "旅人患者"),
                    feifeiNickname = pObj.optString("feifeiNickname", "主治醫生").let {
                        if (it.isBlank() || it == "主治醫師") "主治醫生" else it
                    },
                    todayDate = pObj.optString("todayDate", ""),
                    todayCount = pObj.optLong("todayCount", 0L),
                    totalCompleted = pObj.optLong("totalCompleted", 0L),
                    antibodyBalance = 0L,
                    ownedItemIds = pObj.optJSONArray("ownedItemIds").toStringList(),
                    equippedItemIds = pObj.optJSONArray("equippedItemIds").toStringList(),
                    wardPlacements = pObj.optJSONArray("wardPlacements").toStringList()
                )
            }

            val tasksArr = root.optJSONArray("tasks")
            val tasks = buildList {
                if (tasksArr != null) {
                    for (i in 0 until tasksArr.length()) {
                        val obj = tasksArr.optJSONObject(i) ?: continue
                        val createdMillis = obj.optLong("createdAtMillis", 0L)
                        val updatedMillis = obj.optLong("updatedAtMillis", 0L)
                        add(
                            RehabTask(
                                id = obj.optString("id"),
                                userId = obj.optString("userId", GUEST_USER_ID),
                                title = obj.optString("title"),
                                effortLevel = obj.optString("effortLevel", EffortLevel.MICRO.wireValue),
                                category = obj.optString("category", TaskCategory.DAILY.wireValue),
                                completed = obj.optBoolean("completed", false),
                                completedDate = obj.optString("completedDate", ""),
                                note = obj.optString("note", ""),
                                sortOrder = obj.optLong("sortOrder", 0L),
                                timeSlotsEnabled = obj.optBoolean("timeSlotsEnabled", false),
                                morningDone = obj.optBoolean("morningDone", false),
                                noonDone = obj.optBoolean("noonDone", false),
                                eveningDone = obj.optBoolean("eveningDone", false),
                                morningEnabled = obj.optBoolean("morningEnabled", true),
                                noonEnabled = obj.optBoolean("noonEnabled", true),
                                eveningEnabled = obj.optBoolean("eveningEnabled", true),
                                resetHour = obj.optLong("resetHour", 0L).coerceIn(0L, 23L),
                                deadlineDate = obj.optString("deadlineDate", ""),
                                createdAt = createdMillis.takeIf { it > 0L }?.let { Timestamp(Date(it)) },
                                updatedAt = updatedMillis.takeIf { it > 0L }?.let { Timestamp(Date(it)) }
                            )
                        )
                    }
                }
            }

            val checkinsArr = root.optJSONArray("checkins")
            val checkins = buildList {
                if (checkinsArr != null) {
                    for (i in 0 until checkinsArr.length()) {
                        val obj = checkinsArr.optJSONObject(i) ?: continue
                        val createdMillis = obj.optLong("createdAtMillis", 0L)
                        val updatedMillis = obj.optLong("updatedAtMillis", 0L)
                        val dateKey = obj.optString("dateKey")
                        val exactPercent = if (obj.has("energyPercent") && !obj.isNull("energyPercent")) {
                            obj.optInt("energyPercent", -1).takeIf { it in 0..100 }
                        } else {
                            null
                        }
                        add(
                            RehabCheckin(
                                id = obj.optString("id", dateKey),
                                userId = obj.optString("userId", GUEST_USER_ID),
                                dateKey = dateKey,
                                energyLevel = obj.optString("energyLevel", EnergyLevel.LEVEL_1.wireValue),
                                energyPercent = exactPercent,
                                reflection = obj.optString("reflection", ""),
                                wardSnapshot = obj.optJSONArray("wardSnapshot").toStringList(),
                                doctorPoseSnapshot = obj.optString("doctorPoseSnapshot", ""),
                                completedTaskTitles = obj.optJSONArray("completedTaskTitles").toStringList(),
                                createdAt = createdMillis.takeIf { it > 0L }?.let { Timestamp(Date(it)) },
                                updatedAt = updatedMillis.takeIf { it > 0L }?.let { Timestamp(Date(it)) }
                            )
                        )
                    }
                }
            }
            GuestLocalState(profile = profile, tasks = tasks, checkins = checkins)
        } catch (_: Exception) {
            GuestLocalState()
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) {
                val value = optString(i)
                if (value.isNotEmpty()) add(value)
            }
        }
    }

    companion object {
        @Volatile
        private var instance: GuestRehabStore? = null

        fun getInstance(context: Context): GuestRehabStore {
            return instance ?: synchronized(this) {
                instance ?: GuestRehabStore(context.applicationContext).also { instance = it }
            }
        }
    }
}

internal fun resetExpiredTasksOnList(
    tasks: List<RehabTask>,
    now: Timestamp
): Pair<List<RehabTask>, Int> {
    var resetCount = 0
    val nowMillis = now.toDate().time
    val updated = tasks.map { task ->
        if (task.category == TaskCategory.HOMEWORK.wireValue) {
            task
        } else {
            val hasAnyCheck = task.completed || task.morningDone || task.noonDone || task.eveningDone
            val updatedMillis = task.updatedAt?.toDate()?.time ?: 0L
            if (hasAnyCheck && shouldResetTask(task.category, task.resetHour, task.completedDate, updatedMillis, nowMillis)) {
                resetCount++
                task.copy(
                    completed = false,
                    completedDate = "",
                    morningDone = false,
                    noonDone = false,
                    eveningDone = false,
                    updatedAt = now
                )
            } else {
                task
            }
        }
    }
    return updated to resetCount
}

class GuestRehabRepository(
    private val store: GuestRehabStore
) : RehabDataSource {

    override fun observeProfile(userId: String): Flow<RehabProfile?> = store.profileFlow

    override fun observeTasks(userId: String): Flow<List<RehabTask>> = store.tasksFlow

    override fun observeCheckins(userId: String): Flow<List<RehabCheckin>> = store.checkinsFlow

    override suspend fun ensureProfileInitialized(
        userId: String,
        displayName: String,
        todayDate: String
    ): Result<Unit> = runLocalMutation {
        val now = Timestamp.now()
        store.updateState { current ->
            val existingProfile = current.profile
            if (existingProfile == null) {
                val newProfile = RehabProfile(
                    id = GUEST_USER_ID,
                    userId = GUEST_USER_ID,
                    displayName = displayName.trim().ifBlank { "旅人患者" }.take(60),
                    feifeiNickname = "主治醫生",
                    todayDate = todayDate,
                    todayCount = 0L,
                    totalCompleted = 0L
                )
                val starterTasks = RehabCatalog.starterTasks.mapIndexed { index, (title, effort, note) ->
                    RehabTask(
                        id = "guest_starter_$index",
                        userId = GUEST_USER_ID,
                        title = title.take(100),
                        effortLevel = effort.wireValue,
                        completed = false,
                        completedDate = "",
                        note = note.take(200),
                        sortOrder = index.toLong(),
                        createdAt = Timestamp(Date(now.toDate().time - index * 1000L)),
                        updatedAt = now
                    )
                }
                current.copy(profile = newProfile, tasks = starterTasks)
            } else {
                val fixedNick = if (existingProfile.feifeiNickname.isBlank() || existingProfile.feifeiNickname == "主治醫師") {
                    "主治醫生"
                } else {
                    existingProfile.feifeiNickname
                }
                val nextProfile = if (existingProfile.todayDate != todayDate) {
                    existingProfile.copy(todayDate = todayDate, todayCount = 0L, feifeiNickname = fixedNick)
                } else if (fixedNick != existingProfile.feifeiNickname) {
                    existingProfile.copy(feifeiNickname = fixedNick)
                } else {
                    existingProfile
                }
                val (nextTasks, _) = resetExpiredTasksOnList(current.tasks, now)
                current.copy(profile = nextProfile, tasks = nextTasks)
            }
        }
    }

    override suspend fun resetExpiredTasks(userId: String, todayDate: String): Result<Int> {
        return try {
            var count = 0
            val now = Timestamp.now()
            store.updateState { current ->
                val nextProfile = current.profile?.let { p ->
                    if (p.todayDate != todayDate) p.copy(todayDate = todayDate, todayCount = 0L) else p
                }
                val (nextTasks, resetCount) = resetExpiredTasksOnList(current.tasks, now)
                count = resetCount
                current.copy(profile = nextProfile, tasks = nextTasks)
            }
            Result.success(count)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addTask(
        userId: String,
        title: String,
        effortLevel: EffortLevel,
        note: String,
        categoryWire: String,
        resetHour: Long,
        deadlineDate: String,
        timeSlotsEnabled: Boolean
    ): Result<Unit> = runLocalMutation {
        val trimmedTitle = title.trim()
        val trimmedNote = note.trim()
        val cleanCategory = categoryWire.trim().take(30).ifEmpty { TaskCategory.DAILY.wireValue }
        val safeResetHour = if (cleanCategory == TaskCategory.GAME.wireValue) resetHour.coerceIn(0L, 23L) else 0L
        val safeDeadline = if (cleanCategory == TaskCategory.HOMEWORK.wireValue) deadlineDate.trim().take(10) else ""
        require(trimmedTitle.isNotEmpty() && trimmedTitle.length <= 100) {
            "微任務名稱需為 1 到 100 個字"
        }
        require(trimmedNote.length <= 200) { "備註最多 200 個字" }
        val now = Timestamp.now()
        store.updateState { current ->
            val minOrder = current.tasks.minOfOrNull { it.sortOrder } ?: 0L
            val newTask = RehabTask(
                id = UUID.randomUUID().toString(),
                userId = GUEST_USER_ID,
                title = trimmedTitle,
                effortLevel = effortLevel.wireValue,
                category = cleanCategory,
                completed = false,
                completedDate = "",
                note = trimmedNote,
                sortOrder = minOrder - 1L,
                timeSlotsEnabled = timeSlotsEnabled,
                morningDone = false,
                noonDone = false,
                eveningDone = false,
                resetHour = safeResetHour,
                deadlineDate = safeDeadline,
                createdAt = now,
                updatedAt = now
            )
            current.copy(tasks = listOf(newTask) + current.tasks)
        }
    }

    override suspend fun toggleTaskCompletion(
        userId: String,
        task: RehabTask,
        todayDate: String
    ): Result<Unit> = runLocalMutation {
        require(task.id.isNotBlank()) { "任務 ID 不可為空" }
        val now = Timestamp.now()
        val effectiveTaskDate = effectiveCompletionDateKey(task.category, task.resetHour, now.toDate().time)
        store.updateState { current ->
            val newCompleted = !task.completed
            val newCompletedDate = if (newCompleted) effectiveTaskDate else ""
            val updatedTasks = current.tasks.map { existing ->
                if (existing.id == task.id) {
                    existing.copy(
                        completed = newCompleted,
                        completedDate = newCompletedDate,
                        morningDone = if (existing.timeSlotsEnabled) (newCompleted && existing.morningEnabled) else existing.morningDone,
                        noonDone = if (existing.timeSlotsEnabled) (newCompleted && existing.noonEnabled) else existing.noonDone,
                        eveningDone = if (existing.timeSlotsEnabled) (newCompleted && existing.eveningEnabled) else existing.eveningDone,
                        updatedAt = now
                    )
                } else {
                    existing
                }
            }
            val profile = current.profile
            val updatedProfile = profile?.let { p ->
                val baseTodayCount = if (p.todayDate == todayDate) p.todayCount else 0L
                val wasCountedToday = task.completedDate == effectiveTaskDate || task.completedDate == todayDate
                val nextTodayCount = when {
                    newCompleted -> (baseTodayCount + 1L).coerceIn(0L, 999L)
                    wasCountedToday -> (baseTodayCount - 1L).coerceIn(0L, 999L)
                    else -> baseTodayCount
                }
                val nextTotal = if (newCompleted) {
                    p.totalCompleted + 1L
                } else {
                    (p.totalCompleted - 1L).coerceAtLeast(0L)
                }
                p.copy(
                    todayDate = todayDate,
                    todayCount = nextTodayCount,
                    totalCompleted = nextTotal
                )
            }
            current.copy(profile = updatedProfile, tasks = updatedTasks)
        }
    }

    override suspend fun toggleTaskTimeSlot(
        userId: String,
        task: RehabTask,
        slotIndex: Int,
        todayDate: String
    ): Result<Unit> = runLocalMutation {
        require(task.id.isNotBlank()) { "任務 ID 不可為空" }
        val now = Timestamp.now()
        store.updateState { current ->
            val existing = current.tasks.firstOrNull { it.id == task.id } ?: task
            val effectiveTaskDate = effectiveCompletionDateKey(existing.category, existing.resetHour, now.toDate().time)
            val nextMorning = if (slotIndex == 0) !existing.morningDone else existing.morningDone
            val nextNoon = if (slotIndex == 1) !existing.noonDone else existing.noonDone
            val nextEvening = if (slotIndex == 2) !existing.eveningDone else existing.eveningDone
            val allDone = existing.allEnabledTimeSlotsDone(nextMorning, nextNoon, nextEvening)
            val anyDone = (existing.morningEnabled && nextMorning) ||
                (existing.noonEnabled && nextNoon) ||
                (existing.eveningEnabled && nextEvening)
            val wasCompleted = existing.completed
            val nextCompleted = allDone
            val nextCompletedDate = if (anyDone || nextCompleted) effectiveTaskDate else ""

            val updatedTasks = current.tasks.map { t ->
                if (t.id == task.id) {
                    t.copy(
                        morningDone = nextMorning,
                        noonDone = nextNoon,
                        eveningDone = nextEvening,
                        completed = nextCompleted,
                        completedDate = nextCompletedDate,
                        updatedAt = now
                    )
                } else {
                    t
                }
            }

            val updatedProfile = current.profile?.let { p ->
                val baseTodayCount = if (p.todayDate == todayDate) p.todayCount else 0L
                val nextTodayCount = when {
                    !wasCompleted && nextCompleted -> (baseTodayCount + 1L).coerceIn(0L, 999L)
                    wasCompleted && !nextCompleted -> (baseTodayCount - 1L).coerceIn(0L, 999L)
                    else -> baseTodayCount
                }
                val nextTotal = when {
                    !wasCompleted && nextCompleted -> p.totalCompleted + 1L
                    wasCompleted && !nextCompleted -> (p.totalCompleted - 1L).coerceAtLeast(0L)
                    else -> p.totalCompleted
                }
                p.copy(
                    todayDate = todayDate,
                    todayCount = nextTodayCount,
                    totalCompleted = nextTotal
                )
            }
            current.copy(profile = updatedProfile, tasks = updatedTasks)
        }
    }

    override suspend fun toggleTaskSplitMode(
        userId: String,
        taskId: String,
        enabled: Boolean,
        morningEnabled: Boolean,
        noonEnabled: Boolean,
        eveningEnabled: Boolean
    ): Result<Unit> = runLocalMutation {
        require(taskId.isNotBlank()) { "任務 ID 不可為空" }
        val hasAny = morningEnabled || noonEnabled || eveningEnabled
        val safeMorning = if (hasAny) morningEnabled else true
        val safeNoon = if (hasAny) noonEnabled else true
        val safeEvening = if (hasAny) eveningEnabled else true
        val now = Timestamp.now()
        store.updateState { current ->
            current.copy(
                tasks = current.tasks.map { existing ->
                    if (existing.id == taskId) {
                        val nextMorning = if (!enabled) false else if (existing.completed) safeMorning else (existing.morningDone && safeMorning)
                        val nextNoon = if (!enabled) false else if (existing.completed) safeNoon else (existing.noonDone && safeNoon)
                        val nextEvening = if (!enabled) false else if (existing.completed) safeEvening else (existing.eveningDone && safeEvening)
                        val nextCompleted = if (!enabled) {
                            existing.completed
                        } else {
                            (!safeMorning || nextMorning) && (!safeNoon || nextNoon) && (!safeEvening || nextEvening)
                        }
                        existing.copy(
                            timeSlotsEnabled = enabled,
                            morningEnabled = safeMorning,
                            noonEnabled = safeNoon,
                            eveningEnabled = safeEvening,
                            morningDone = nextMorning,
                            noonDone = nextNoon,
                            eveningDone = nextEvening,
                            completed = nextCompleted,
                            updatedAt = now
                        )
                    } else {
                        existing
                    }
                }
            )
        }
    }

    override suspend fun updateTaskSortOrders(
        userId: String,
        orderedTaskIds: List<String>
    ): Result<Unit> = runLocalMutation {
        val now = Timestamp.now()
        val indexMap = orderedTaskIds.withIndex().associate { it.value to it.index.toLong() }
        store.updateState { current ->
            current.copy(
                tasks = current.tasks.map { existing ->
                    val nextOrder = indexMap[existing.id]
                    if (nextOrder != null) {
                        existing.copy(sortOrder = nextOrder, updatedAt = now)
                    } else {
                        existing
                    }
                }
            )
        }
    }

    override suspend fun updateTaskScheduleMeta(
        userId: String,
        taskId: String,
        resetHour: Long,
        deadlineDate: String
    ): Result<Unit> = runLocalMutation {
        require(taskId.isNotBlank()) { "任務 ID 不可為空" }
        val now = Timestamp.now()
        val safeHour = resetHour.coerceIn(0L, 23L)
        val safeDeadline = deadlineDate.trim().take(10)
        store.updateState { current ->
            current.copy(
                tasks = current.tasks.map { existing ->
                    if (existing.id == taskId) {
                        existing.copy(
                            resetHour = safeHour,
                            deadlineDate = safeDeadline,
                            updatedAt = now
                        )
                    } else {
                        existing
                    }
                }
            )
        }
    }

    override suspend fun shrinkTask(
        userId: String,
        taskId: String,
        newTitle: String,
        newNote: String
    ): Result<Unit> = runLocalMutation {
        val trimmedTitle = newTitle.trim()
        val trimmedNote = newNote.trim()
        require(taskId.isNotBlank()) { "任務 ID 不可為空" }
        require(trimmedTitle.isNotEmpty() && trimmedTitle.length <= 100) {
            "微任務名稱需為 1 到 100 個字"
        }
        require(trimmedNote.length <= 200) { "備註最多 200 個字" }
        val now = Timestamp.now()
        store.updateState { current ->
            current.copy(
                tasks = current.tasks.map { existing ->
                    if (existing.id == taskId) {
                        existing.copy(
                            title = trimmedTitle,
                            effortLevel = EffortLevel.MICRO.wireValue,
                            note = trimmedNote,
                            updatedAt = now
                        )
                    } else {
                        existing
                    }
                }
            )
        }
    }

    override suspend fun updateTaskDetails(
        userId: String,
        taskId: String,
        newTitle: String,
        newNote: String,
        effortLevel: EffortLevel,
        resetHour: Long,
        deadlineDate: String
    ): Result<Unit> = runLocalMutation {
        val trimmedTitle = newTitle.trim()
        val trimmedNote = newNote.trim()
        require(taskId.isNotBlank()) { "任務 ID 不可為空" }
        require(trimmedTitle.isNotEmpty() && trimmedTitle.length <= 100) {
            "微任務名稱需為 1 到 100 個字"
        }
        require(trimmedNote.length <= 200) { "備註最多 200 個字" }
        val now = Timestamp.now()
        val safeHour = resetHour.coerceIn(0L, 23L)
        val safeDeadline = deadlineDate.trim().take(10)
        store.updateState { current ->
            current.copy(
                tasks = current.tasks.map { existing ->
                    if (existing.id == taskId) {
                        existing.copy(
                            title = trimmedTitle,
                            note = trimmedNote,
                            effortLevel = effortLevel.wireValue,
                            resetHour = if (existing.category == TaskCategory.GAME.wireValue) safeHour else existing.resetHour,
                            deadlineDate = if (existing.category == TaskCategory.HOMEWORK.wireValue) safeDeadline else existing.deadlineDate,
                            updatedAt = now
                        )
                    } else {
                        existing
                    }
                }
            )
        }
    }

    override suspend fun deleteTask(userId: String, taskId: String): Result<Unit> = runLocalMutation {
        require(taskId.isNotBlank()) { "任務 ID 不可為空" }
        store.updateState { current ->
            current.copy(tasks = current.tasks.filterNot { it.id == taskId })
        }
    }

    override suspend fun saveCheckin(
        userId: String,
        dateKey: String,
        energyLevel: EnergyLevel,
        energyPercent: Int,
        reflection: String,
        wardSnapshot: List<String>,
        doctorPoseSnapshot: String,
        completedTaskTitles: List<String>
    ): Result<Unit> = runLocalMutation {
        val trimmedReflection = reflection.trim()
        val safePercent = energyPercent.coerceIn(0, 100)
        val cleanSnapshot = wardSnapshot.take(20)
        val cleanCompletedTitles = completedTaskTitles
            .map { it.trim().take(120) }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(100)
        require(dateKey.length == 10) { "日期格式需為 YYYY-MM-DD" }
        require(trimmedReflection.length <= 300) { "心情筆記最多 300 個字" }
        val now = Timestamp.now()
        store.updateState { current ->
            val existing = current.checkins.firstOrNull { it.dateKey == dateKey }
            val mergedTitles = (existing?.completedTaskTitles.orEmpty() + cleanCompletedTitles)
                .map { it.trim().take(120) }
                .filter { it.isNotEmpty() }
                .distinct()
                .take(100)
            val updated = RehabCheckin(
                id = dateKey,
                userId = GUEST_USER_ID,
                dateKey = dateKey,
                energyLevel = energyLevel.wireValue,
                energyPercent = safePercent,
                reflection = trimmedReflection,
                wardSnapshot = cleanSnapshot,
                doctorPoseSnapshot = doctorPoseSnapshot.take(128),
                completedTaskTitles = mergedTitles,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
            current.copy(
                checkins = listOf(updated) + current.checkins.filterNot { it.dateKey == dateKey }
            )
        }
    }

    override suspend fun updateFeifeiNickname(userId: String, nickname: String): Result<Unit> = runLocalMutation {
        val trimmed = nickname.trim()
        require(trimmed.isNotEmpty() && trimmed.length <= 30) { "暱稱需為 1 到 30 個字" }
        store.updateState { current ->
            current.copy(profile = current.profile?.copy(feifeiNickname = trimmed))
        }
    }

    override suspend fun redeemPharmacyItem(userId: String, item: PharmacyItem): Result<Unit> = runLocalMutation {
        store.updateState { current ->
            val profile = current.profile ?: return@updateState current
            if (item.id in profile.ownedItemIds) {
                throw IllegalStateException("你已經領取過「${item.name}」囉！")
            }
            val nextOwned = (profile.ownedItemIds + item.id).distinct().take(20)
            val nextEquipped = (profile.equippedItemIds + item.id).distinct().take(20)
            val existingPlacements = profile.resolvedWardPlacements()
            val (dx, dy) = RehabCatalog.defaultWardPosition(item.id)
            val nextPlacements = (existingPlacements.filterNot { it.itemId == item.id } +
                WardPlacement(item.id, dx, dy)).take(20).map { it.encode() }
            current.copy(
                profile = profile.copy(
                    ownedItemIds = nextOwned,
                    equippedItemIds = nextEquipped,
                    wardPlacements = nextPlacements
                )
            )
        }
    }

    override suspend fun toggleEquipPharmacyItem(userId: String, itemId: String): Result<Boolean> {
        return try {
            var nowEquipped = false
            store.updateState { current ->
                val profile = current.profile ?: return@updateState current
                if (itemId !in profile.ownedItemIds) {
                    throw IllegalStateException("需要先在院內藥局領取此道具喔！")
                }
                val existingPlacements = profile.resolvedWardPlacements()
                val isCurrentlyInWard = itemId in profile.equippedItemIds || existingPlacements.any { it.itemId == itemId }
                val nextEquipped: List<String>
                val nextPlacements: List<String>
                if (isCurrentlyInWard) {
                    nowEquipped = false
                    nextEquipped = profile.equippedItemIds.filterNot { it == itemId }
                    nextPlacements = existingPlacements.filterNot { it.itemId == itemId }.map { it.encode() }
                } else {
                    nowEquipped = true
                    nextEquipped = (profile.equippedItemIds + itemId).distinct().take(20)
                    val (dx, dy) = RehabCatalog.defaultWardPosition(itemId)
                    nextPlacements = (existingPlacements.filterNot { it.itemId == itemId } +
                        WardPlacement(itemId, dx, dy)).take(20).map { it.encode() }
                }
                current.copy(
                    profile = profile.copy(
                        equippedItemIds = nextEquipped,
                        wardPlacements = nextPlacements
                    )
                )
            }
            Result.success(nowEquipped)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateWardItemPosition(
        userId: String,
        itemId: String,
        xFraction: Float,
        yFraction: Float
    ): Result<Unit> = runLocalMutation {
        store.updateState { current ->
            val profile = current.profile ?: return@updateState current
            if (itemId !in profile.ownedItemIds) return@updateState current
            val existingPlacements = profile.resolvedWardPlacements()
            val updated = WardPlacement(itemId, xFraction, yFraction)
            val nextPlacements = (existingPlacements.filterNot { it.itemId == itemId } + updated)
                .take(20)
                .map { it.encode() }
            val nextEquipped = (profile.equippedItemIds + itemId).distinct().take(20)
            current.copy(
                profile = profile.copy(
                    equippedItemIds = nextEquipped,
                    wardPlacements = nextPlacements
                )
            )
        }
    }

    private suspend fun runLocalMutation(block: suspend () -> Unit): Result<Unit> {
        return try {
            block()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
