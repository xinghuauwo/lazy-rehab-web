package com.example.data

import kotlinx.coroutines.flow.Flow

interface RehabDataSource {
    fun observeProfile(userId: String): Flow<RehabProfile?>
    fun observeTasks(userId: String): Flow<List<RehabTask>>
    fun observeCheckins(userId: String): Flow<List<RehabCheckin>>

    suspend fun ensureProfileInitialized(
        userId: String,
        displayName: String,
        todayDate: String
    ): Result<Unit>

    suspend fun addTask(
        userId: String,
        title: String,
        effortLevel: EffortLevel,
        note: String = "",
        categoryWire: String = TaskCategory.DAILY.wireValue,
        resetHour: Long = 0L,
        deadlineDate: String = "",
        timeSlotsEnabled: Boolean = false
    ): Result<Unit>

    suspend fun toggleTaskCompletion(
        userId: String,
        task: RehabTask,
        todayDate: String
    ): Result<Unit>

    suspend fun toggleTaskTimeSlot(
        userId: String,
        task: RehabTask,
        slotIndex: Int,
        todayDate: String
    ): Result<Unit>

    suspend fun toggleTaskSplitMode(
        userId: String,
        taskId: String,
        enabled: Boolean,
        morningEnabled: Boolean = true,
        noonEnabled: Boolean = true,
        eveningEnabled: Boolean = true
    ): Result<Unit>

    suspend fun updateTaskSortOrders(
        userId: String,
        orderedTaskIds: List<String>
    ): Result<Unit>

    suspend fun updateTaskScheduleMeta(
        userId: String,
        taskId: String,
        resetHour: Long,
        deadlineDate: String
    ): Result<Unit>

    suspend fun resetExpiredTasks(
        userId: String,
        todayDate: String
    ): Result<Int>

    suspend fun shrinkTask(
        userId: String,
        taskId: String,
        newTitle: String,
        newNote: String
    ): Result<Unit>

    suspend fun updateTaskDetails(
        userId: String,
        taskId: String,
        newTitle: String,
        newNote: String,
        effortLevel: EffortLevel = EffortLevel.MICRO,
        resetHour: Long = 0L,
        deadlineDate: String = ""
    ): Result<Unit>

    suspend fun deleteTask(userId: String, taskId: String): Result<Unit>

    suspend fun saveCheckin(
        userId: String,
        dateKey: String,
        energyLevel: EnergyLevel,
        energyPercent: Int,
        reflection: String,
        wardSnapshot: List<String>,
        doctorPoseSnapshot: String,
        completedTaskTitles: List<String> = emptyList()
    ): Result<Unit>

    suspend fun updateFeifeiNickname(userId: String, nickname: String): Result<Unit>

    suspend fun redeemPharmacyItem(userId: String, item: PharmacyItem): Result<Unit>

    suspend fun toggleEquipPharmacyItem(userId: String, itemId: String): Result<Boolean>

    suspend fun updateWardItemPosition(
        userId: String,
        itemId: String,
        xFraction: Float,
        yFraction: Float
    ): Result<Unit>

    suspend fun checkLocalCloudConflict(userId: String): SyncConflictInfo? = null

    suspend fun resolveConflictChooseLocal(userId: String): Result<Unit> = Result.success(Unit)

    suspend fun resolveConflictChooseCloud(userId: String): Result<Unit> = Result.success(Unit)

    suspend fun mirrorCloudBackupIfSynced(
        userId: String,
        profile: RehabProfile?,
        tasks: List<RehabTask>,
        checkins: List<RehabCheckin>
    ) {}
}

data class SyncSaveSummary(
    val totalCompleted: Long,
    val todayCount: Long,
    val taskCount: Int,
    val completedTaskCount: Int,
    val checkinCount: Int,
    val latestDateKey: String
)

data class SyncConflictInfo(
    val userId: String,
    val localSummary: SyncSaveSummary,
    val cloudSummary: SyncSaveSummary
)
