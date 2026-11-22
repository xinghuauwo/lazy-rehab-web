package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DoctorPose
import com.example.data.DoctorQuotes
import com.example.data.EffortLevel
import com.example.data.EnergyLevel
import com.example.data.GUEST_USER_ID
import com.example.data.PharmacyItem
import com.example.data.RehabCatalog
import com.example.data.RehabCheckin
import com.example.data.RehabDataSource
import com.example.data.RehabProfile
import com.example.data.RehabTask
import com.example.data.RxPrescription
import com.example.data.SyncConflictInfo
import com.example.data.TaskCategory
import com.example.data.WardDoctorSnapshot
import com.example.data.currentTodayDateKey
import com.example.data.effectiveCompletionDateKey
import com.example.data.resolvedWardPlacements
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class RehabTab(val navLabelZh: String, val topBarTitleZh: String) {
    ROOM("康復室", "懶癌康復室"),
    PRESCRIPTIONS("療程", "復健療程"),
    PHARMACY("藥局", "院內藥局"),
    MEMORIES("回憶", "康復回憶冊"),
    LOG("日記", "日記")
}

data class RehabInteractionState(
    val selectedTab: RehabTab = RehabTab.ROOM,
    val selectedEnergyFilter: EnergyLevel = EnergyLevel.LEVEL_1,
    val feifeiQuote: String = RehabCatalog.feifeiQuotes.random(),
    val patCounter: Int = 0,
    val stagePose: DoctorPose = DoctorPose.OBSERVE,
    val pharmacyPose: DoctorPose = DoctorPose.PHARMACY,
    val prescriptionPose: DoctorPose = DoctorPose.OBSERVE,
    val prescriptionQuote: String = RehabCatalog.prescriptionDoctorQuotes.first(),
    val prescriptionPatCounter: Int = 0,
    val memoryPose: DoctorPose = DoctorPose.APPROVE,
    val memoryQuote: String = RehabCatalog.memoryDoctorQuotes.first(),
    val memoryPatCounter: Int = 0,
    val wardDoctorPose: DoctorPose = DoctorPose.OBSERVE,
    val wardDoctorSnapshot: WardDoctorSnapshot = WardDoctorSnapshot(),
    val wardDoctorSpeech: String? = null,
    val wardSpeechNonce: Int = 0,
    val pharmacyQuote: String = RehabCatalog.pharmacyQuotes.first(),
    val pharmacyPatCounter: Int = 0,
    val activeRoomCategory: String = TaskCategory.DAILY.wireValue,
    val showAddTaskSheet: Boolean = false,
    val taskToShrink: RehabTask? = null,
    val taskToEdit: RehabTask? = null,
    val showAccountSheet: Boolean = false,
    val showDoctorEquipSheet: Boolean = false,
    val drawnPrescription: RxPrescription? = null,
    val isSubmitting: Boolean = false
)

class RehabViewModel(
    private val repository: RehabDataSource,
    private val currentUserId: String,
    private val initialDisplayName: String
) : ViewModel() {

    val todayDateKey: String = currentTodayDateKey()

    private val _interaction = MutableStateFlow(RehabInteractionState())
    val interaction: StateFlow<RehabInteractionState> = _interaction.asStateFlow()

    private val _syncConflict = MutableStateFlow<SyncConflictInfo?>(null)
    val syncConflict: StateFlow<SyncConflictInfo?> = _syncConflict.asStateFlow()

    private val _isResolvingSyncConflict = MutableStateFlow(false)
    val isResolvingSyncConflict: StateFlow<Boolean> = _isResolvingSyncConflict.asStateFlow()

    private val _syncConflictError = MutableStateFlow<String?>(null)
    val syncConflictError: StateFlow<String?> = _syncConflictError.asStateFlow()

    val profileState: StateFlow<UiState<RehabProfile?>> = repository.observeProfile(currentUserId)
        .map<RehabProfile?, UiState<RehabProfile?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing profile", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "無法讀取康復檔案"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val tasksState: StateFlow<UiState<List<RehabTask>>> = repository.observeTasks(currentUserId)
        .map<List<RehabTask>, UiState<List<RehabTask>>> { list ->
            val sorted = list.sortedWith(
                compareBy<RehabTask> { it.completed }
                    .thenBy { it.sortOrder }
                    .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
            )
            UiState.Success(sorted)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing tasks", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "無法讀取微任務清單"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val checkinsState: StateFlow<UiState<List<RehabCheckin>>> = repository.observeCheckins(currentUserId)
        .map<List<RehabCheckin>, UiState<List<RehabCheckin>>> { list ->
            val sorted = list.sortedByDescending { it.dateKey }
            UiState.Success(sorted)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing checkins", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "無法讀取康復日記"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    init {
        viewModelScope.launch {
            val result = repository.ensureProfileInitialized(
                userId = currentUserId,
                displayName = initialDisplayName,
                todayDate = todayDateKey
            )
            result.onFailure { err ->
                reportDoctorMessage(DoctorQuotes.profileInitErrorQuote(err.localizedMessage))
            }
            repository.resetExpiredTasks(currentUserId, currentTodayDateKey())
            if (currentUserId != GUEST_USER_ID && result.isSuccess) {
                _syncConflict.value = repository.checkLocalCloudConflict(currentUserId)
            }
        }
    }

    fun resolveConflictChooseLocal() {
        if (_isResolvingSyncConflict.value) return
        viewModelScope.launch {
            _isResolvingSyncConflict.value = true
            _syncConflictError.value = null
            val res = repository.resolveConflictChooseLocal(currentUserId)
            _isResolvingSyncConflict.value = false
            res.onSuccess {
                _syncConflict.value = null
                reportRoomDoctorQuote("已保留本機療程紀錄並同步覆寫雲端存檔。", DoctorPose.APPROVE)
            }.onFailure { err ->
                _syncConflictError.value = err.localizedMessage ?: "同步本機存檔至雲端失敗，請稍後再試。"
            }
        }
    }

    fun resolveConflictChooseCloud() {
        if (_isResolvingSyncConflict.value) return
        viewModelScope.launch {
            _isResolvingSyncConflict.value = true
            _syncConflictError.value = null
            val res = repository.resolveConflictChooseCloud(currentUserId)
            _isResolvingSyncConflict.value = false
            res.onSuccess {
                _syncConflict.value = null
                reportRoomDoctorQuote("已套用雲端療程紀錄並更新本機存檔。", DoctorPose.OBSERVE)
            }.onFailure { err ->
                _syncConflictError.value = err.localizedMessage ?: "載入雲端存檔失敗，請稍後再試。"
            }
        }
    }

    fun mirrorCloudBackupIfSynced(
        profile: RehabProfile?,
        tasks: List<RehabTask>,
        checkins: List<RehabCheckin>
    ) {
        if (currentUserId == GUEST_USER_ID || _syncConflict.value != null) return
        viewModelScope.launch {
            repository.mirrorCloudBackupIfSynced(currentUserId, profile, tasks, checkins)
        }
    }

    fun resetExpiredTasksIfNeeded() {
        viewModelScope.launch {
            repository.resetExpiredTasks(currentUserId, currentTodayDateKey())
        }
    }

    fun showFirstWelcomeIfNeeded(hasSeen: Boolean, onMarkSeen: () -> Unit) {
        if (!hasSeen) {
            _interaction.update {
                it.copy(
                    feifeiQuote = DoctorQuotes.firstVisitWelcomeQuote,
                    stagePose = DoctorPose.OBSERVE
                )
            }
            onMarkSeen()
        }
    }

    fun selectTab(tab: RehabTab) {
        _interaction.update { it.copy(selectedTab = tab) }
    }

    fun selectRoomCategory(categoryWire: String) {
        val clean = categoryWire.trim().ifEmpty { TaskCategory.DAILY.wireValue }
        _interaction.update { it.copy(activeRoomCategory = clean) }
    }

    fun reportDoctorMessage(message: String) {
        _interaction.update { state ->
            when (state.selectedTab) {
                RehabTab.ROOM -> state.copy(feifeiQuote = message)
                RehabTab.PRESCRIPTIONS -> state.copy(prescriptionQuote = message)
                RehabTab.PHARMACY -> state.copy(pharmacyQuote = message)
                RehabTab.MEMORIES -> state.copy(memoryQuote = message)
                RehabTab.LOG -> state.copy(
                    wardDoctorSpeech = message,
                    wardSpeechNonce = state.wardSpeechNonce + 1
                )
            }
        }
    }

    fun reportRoomDoctorQuote(quote: String, pose: DoctorPose = DoctorPose.SIPPING) {
        _interaction.update { state ->
            state.copy(
                feifeiQuote = quote,
                stagePose = pose
            )
        }
    }

    fun patFeifei() {
        _interaction.update { state ->
            val nextPat = state.patCounter + 1
            val candidates = RehabCatalog.feifeiQuotes.filter { it != state.feifeiQuote }
            val quote = candidates.randomOrNull() ?: RehabCatalog.feifeiQuotes.random()
            val poses = DoctorPose.entries
            val nextPose = poses[nextPat % poses.size]
            state.copy(
                patCounter = nextPat,
                feifeiQuote = quote,
                stagePose = nextPose
            )
        }
    }

    fun patPharmacyDoctor() {
        _interaction.update { state ->
            val nextPat = state.pharmacyPatCounter + 1
            val poseCandidates = DoctorPose.entries.filter { it != state.pharmacyPose }
            val nextPose = poseCandidates.randomOrNull() ?: DoctorPose.PHARMACY
            val quoteCandidates = RehabCatalog.pharmacyQuotes.filter { it != state.pharmacyQuote }
            val nextQuote = quoteCandidates.randomOrNull() ?: RehabCatalog.pharmacyQuotes.random()
            state.copy(
                pharmacyPatCounter = nextPat,
                pharmacyPose = nextPose,
                pharmacyQuote = nextQuote
            )
        }
    }

    fun patPrescriptionDoctor() {
        _interaction.update { state ->
            val nextPat = state.prescriptionPatCounter + 1
            val poseCandidates = DoctorPose.entries.filter { it != state.prescriptionPose }
            val nextPose = poseCandidates.randomOrNull() ?: DoctorPose.OBSERVE
            val quoteCandidates = RehabCatalog.prescriptionDoctorQuotes.filter { it != state.prescriptionQuote }
            val nextQuote = quoteCandidates.randomOrNull() ?: RehabCatalog.prescriptionDoctorQuotes.random()
            state.copy(
                prescriptionPatCounter = nextPat,
                prescriptionPose = nextPose,
                prescriptionQuote = nextQuote
            )
        }
    }

    fun patMemoryDoctor() {
        _interaction.update { state ->
            val nextPat = state.memoryPatCounter + 1
            val poseCandidates = DoctorPose.entries.filter { it != state.memoryPose }
            val nextPose = poseCandidates.randomOrNull() ?: DoctorPose.APPROVE
            val quoteCandidates = RehabCatalog.memoryDoctorQuotes.filter { it != state.memoryQuote }
            val nextQuote = quoteCandidates.randomOrNull() ?: RehabCatalog.memoryDoctorQuotes.random()
            state.copy(
                memoryPatCounter = nextPat,
                memoryPose = nextPose,
                memoryQuote = nextQuote
            )
        }
    }

    fun setAddTaskSheetVisible(visible: Boolean) {
        _interaction.update { it.copy(showAddTaskSheet = visible) }
    }

    fun setTaskToShrink(task: RehabTask?) {
        _interaction.update { it.copy(taskToShrink = task) }
    }

    fun setTaskToEdit(task: RehabTask?) {
        _interaction.update { it.copy(taskToEdit = task) }
    }

    fun setAccountSheetVisible(visible: Boolean) {
        _interaction.update { it.copy(showAccountSheet = visible) }
    }

    fun setDoctorEquipSheetVisible(visible: Boolean) {
        _interaction.update { it.copy(showDoctorEquipSheet = visible) }
    }

    fun selectEnergyFilter(level: EnergyLevel) {
        _interaction.update { it.copy(selectedEnergyFilter = level, drawnPrescription = null) }
    }

    fun drawRandomPrescription(enabledCategories: Set<TaskCategory> = setOf(TaskCategory.DAILY)) {
        val currentFilter = _interaction.value.selectedEnergyFilter
        val activeCategories = enabledCategories.ifEmpty { setOf(TaskCategory.DAILY) }
        val pool = RehabCatalog.prescriptions.filter {
            it.energyLevel == currentFilter && it.category in activeCategories
        }.ifEmpty {
            RehabCatalog.prescriptions.filter { it.energyLevel == currentFilter }
        }
        val picked = pool.randomOrNull() ?: RehabCatalog.prescriptions.random()
        _interaction.update {
            it.copy(
                drawnPrescription = picked,
                prescriptionPose = DoctorPose.APPROVE,
                prescriptionQuote = DoctorQuotes.drawnPrescriptionQuote(picked.title)
            )
        }
    }

    fun addTask(
        title: String,
        effortLevel: EffortLevel,
        note: String = "",
        categoryWire: String = TaskCategory.DAILY.wireValue,
        resetHour: Long = 0L,
        deadlineDate: String = "",
        timeSlotsEnabled: Boolean = false
    ) {
        val cleanTitle = title.trim()
        val cleanCategory = categoryWire.trim().take(30).ifEmpty { TaskCategory.DAILY.wireValue }
        val categoryLabel = TaskCategory.labelFor(cleanCategory)
        if (cleanTitle.isEmpty()) {
            _interaction.update {
                it.copy(feifeiQuote = DoctorQuotes.emptyTaskTitleQuote)
            }
            return
        }
        viewModelScope.launch {
            _interaction.update { it.copy(isSubmitting = true) }
            val res = repository.addTask(
                userId = currentUserId,
                title = cleanTitle.take(100),
                effortLevel = effortLevel,
                note = note.take(200),
                categoryWire = cleanCategory,
                resetHour = resetHour,
                deadlineDate = deadlineDate,
                timeSlotsEnabled = timeSlotsEnabled
            )
            _interaction.update { state ->
                if (res.isSuccess) {
                    state.copy(
                        isSubmitting = false,
                        showAddTaskSheet = false,
                        activeRoomCategory = cleanCategory,
                        stagePose = DoctorPose.OBSERVE,
                        feifeiQuote = DoctorQuotes.addTaskSuccessQuote(cleanTitle.take(14), categoryLabel)
                    )
                } else {
                    state.copy(
                        isSubmitting = false,
                        feifeiQuote = DoctorQuotes.addTaskErrorQuote(res.exceptionOrNull()?.localizedMessage)
                    )
                }
            }
        }
    }

    fun addPrescriptionToToday(
        prescription: RxPrescription,
        onEnsureCategoryEnabled: (TaskCategory) -> Unit = {}
    ) {
        viewModelScope.launch {
            _interaction.update { it.copy(isSubmitting = true) }
            val res = repository.addTask(
                userId = currentUserId,
                title = prescription.title,
                effortLevel = prescription.effortLevel,
                note = prescription.doctorNote,
                categoryWire = prescription.category.wireValue
            )
            if (res.isSuccess) {
                onEnsureCategoryEnabled(prescription.category)
            }
            _interaction.update { state ->
                if (res.isSuccess) {
                    state.copy(
                        isSubmitting = false,
                        selectedTab = RehabTab.ROOM,
                        activeRoomCategory = prescription.category.wireValue,
                        stagePose = DoctorPose.APPROVE,
                        feifeiQuote = DoctorQuotes.addPrescriptionSuccessQuote(
                            prescription.title,
                            prescription.category.labelZh
                        )
                    )
                } else {
                    state.copy(
                        isSubmitting = false,
                        prescriptionQuote = DoctorQuotes.addPrescriptionErrorQuote(
                            res.exceptionOrNull()?.localizedMessage
                        )
                    )
                }
            }
        }
    }

    fun toggleTask(task: RehabTask) {
        viewModelScope.launch {
            val res = repository.toggleTaskCompletion(currentUserId, task, todayDateKey)
            res.onSuccess {
                if (!task.completed) {
                    val effort = EffortLevel.fromWire(task.effortLevel)
                    _interaction.update { state ->
                        val nextQuote = RehabCatalog.taskCompleteQuote(effort, state.feifeiQuote)
                        state.copy(
                            feifeiQuote = nextQuote,
                            stagePose = DoctorPose.APPROVE
                        )
                    }
                } else {
                    _interaction.update { state ->
                        val nextQuote = RehabCatalog.taskUncheckQuote(task.title, state.feifeiQuote)
                        state.copy(
                            feifeiQuote = nextQuote,
                            stagePose = DoctorPose.SIPPING
                        )
                    }
                }
            }.onFailure { err ->
                _interaction.update {
                    it.copy(feifeiQuote = DoctorQuotes.toggleTaskErrorQuote(err.localizedMessage))
                }
            }
        }
    }

    fun toggleTaskTimeSlot(task: RehabTask, slotIndex: Int) {
        val slotLabel = when (slotIndex) {
            0 -> "早"
            1 -> "中"
            else -> "晚"
        }
        val willBeChecked = when (slotIndex) {
            0 -> !task.morningDone
            1 -> !task.noonDone
            else -> !task.eveningDone
        }
        val nextMorning = if (slotIndex == 0) !task.morningDone else task.morningDone
        val nextNoon = if (slotIndex == 1) !task.noonDone else task.noonDone
        val nextEvening = if (slotIndex == 2) !task.eveningDone else task.eveningDone
        val willCompleteAll = task.allEnabledTimeSlotsDone(nextMorning, nextNoon, nextEvening)

        viewModelScope.launch {
            val res = repository.toggleTaskTimeSlot(currentUserId, task, slotIndex, todayDateKey)
            res.onSuccess {
                _interaction.update { state ->
                    val nextQuote = when {
                        willCompleteAll && !task.completed -> RehabCatalog.taskCompleteQuote(
                            EffortLevel.fromWire(task.effortLevel),
                            state.feifeiQuote
                        )
                        willBeChecked -> DoctorQuotes.toggleTimeSlotQuote(task.title, slotLabel)
                        else -> RehabCatalog.taskUncheckQuote("${task.title}（${slotLabel}）", state.feifeiQuote)
                    }
                    state.copy(
                        feifeiQuote = nextQuote,
                        stagePose = if (willBeChecked) DoctorPose.APPROVE else DoctorPose.SIPPING
                    )
                }
            }.onFailure { err ->
                _interaction.update {
                    it.copy(feifeiQuote = DoctorQuotes.toggleTimeSlotErrorQuote(slotLabel, err.localizedMessage))
                }
            }
        }
    }

    fun toggleTaskSplitMode(
        task: RehabTask,
        enabled: Boolean = !task.timeSlotsEnabled,
        morningEnabled: Boolean = task.morningEnabled,
        noonEnabled: Boolean = task.noonEnabled,
        eveningEnabled: Boolean = task.eveningEnabled
    ) {
        val slotSummary = listOfNotNull(
            if (morningEnabled) "早" else null,
            if (noonEnabled) "中" else null,
            if (eveningEnabled) "晚" else null
        ).joinToString("、").ifEmpty { "早、中、晚" }
        viewModelScope.launch {
            val res = repository.toggleTaskSplitMode(
                userId = currentUserId,
                taskId = task.id,
                enabled = enabled,
                morningEnabled = morningEnabled,
                noonEnabled = noonEnabled,
                eveningEnabled = eveningEnabled
            )
            res.onSuccess {
                _interaction.update {
                    it.copy(
                        stagePose = DoctorPose.OBSERVE,
                        feifeiQuote = DoctorQuotes.toggleSplitModeQuote(task.title, enabled, slotSummary)
                    )
                }
            }.onFailure { err ->
                _interaction.update {
                    it.copy(feifeiQuote = DoctorQuotes.toggleSplitModeErrorQuote(err.localizedMessage))
                }
            }
        }
    }

    fun reorderTasks(orderedTaskIds: List<String>) {
        if (orderedTaskIds.isEmpty()) return
        viewModelScope.launch {
            repository.updateTaskSortOrders(currentUserId, orderedTaskIds)
        }
    }

    fun editTask(
        task: RehabTask,
        newTitle: String,
        newNote: String,
        effortLevel: EffortLevel = EffortLevel.fromWire(task.effortLevel),
        resetHour: Long = task.resetHour,
        deadlineDate: String = task.deadlineDate
    ) {
        val clean = newTitle.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            _interaction.update { it.copy(isSubmitting = true) }
            val res = repository.updateTaskDetails(
                userId = currentUserId,
                taskId = task.id,
                newTitle = clean.take(100),
                newNote = newNote.take(200),
                effortLevel = effortLevel,
                resetHour = resetHour,
                deadlineDate = deadlineDate
            )
            _interaction.update { state ->
                if (res.isSuccess) {
                    state.copy(
                        isSubmitting = false,
                        taskToEdit = null,
                        stagePose = DoctorPose.OBSERVE,
                        feifeiQuote = DoctorQuotes.editTaskSuccessQuote(clean.take(16))
                    )
                } else {
                    state.copy(
                        isSubmitting = false,
                        feifeiQuote = DoctorQuotes.editTaskErrorQuote(res.exceptionOrNull()?.localizedMessage)
                    )
                }
            }
        }
    }

    fun shrinkTask(task: RehabTask, newTitle: String, newNote: String) {
        val clean = newTitle.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            _interaction.update { it.copy(isSubmitting = true) }
            val res = repository.shrinkTask(currentUserId, task.id, clean.take(100), newNote.take(200))
            _interaction.update { state ->
                if (res.isSuccess) {
                    state.copy(
                        isSubmitting = false,
                        taskToShrink = null,
                        stagePose = DoctorPose.APPROVE,
                        feifeiQuote = DoctorQuotes.shrinkTaskSuccessQuote(clean.take(14))
                    )
                } else {
                    state.copy(
                        isSubmitting = false,
                        feifeiQuote = DoctorQuotes.shrinkTaskErrorQuote(res.exceptionOrNull()?.localizedMessage)
                    )
                }
            }
        }
    }

    fun deleteTask(task: RehabTask) {
        viewModelScope.launch {
            val res = repository.deleteTask(currentUserId, task.id)
            res.onSuccess {
                _interaction.update { state ->
                    val nextQuote = RehabCatalog.taskDeleteQuote(task.title, state.feifeiQuote)
                    state.copy(
                        stagePose = DoctorPose.SIPPING,
                        feifeiQuote = nextQuote
                    )
                }
            }.onFailure { err ->
                _interaction.update {
                    it.copy(feifeiQuote = DoctorQuotes.deleteTaskErrorQuote(err.localizedMessage))
                }
            }
        }
    }

    fun updateWardDoctorPose(pose: DoctorPose) {
        _interaction.update { it.copy(wardDoctorPose = pose) }
    }

    fun updateWardDoctorSnapshot(snapshot: WardDoctorSnapshot) {
        _interaction.update { it.copy(wardDoctorSnapshot = snapshot) }
    }

    fun toggleWardItem(item: PharmacyItem) {
        viewModelScope.launch {
            val res = repository.toggleEquipPharmacyItem(currentUserId, item.id)
            res.onSuccess { placed ->
                _interaction.update { state ->
                    state.copy(
                        wardDoctorSpeech = DoctorQuotes.wardToggleItemQuote(item.name, placed),
                        wardSpeechNonce = state.wardSpeechNonce + 1
                    )
                }
            }.onFailure { err ->
                _interaction.update { state ->
                    state.copy(
                        wardDoctorSpeech = DoctorQuotes.wardToggleErrorQuote(err.localizedMessage),
                        wardSpeechNonce = state.wardSpeechNonce + 1
                    )
                }
            }
        }
    }

    fun moveWardItem(itemId: String, xFraction: Float, yFraction: Float) {
        viewModelScope.launch {
            val res = repository.updateWardItemPosition(currentUserId, itemId, xFraction, yFraction)
            res.onFailure { err ->
                _interaction.update { state ->
                    state.copy(
                        wardDoctorSpeech = DoctorQuotes.wardMoveErrorQuote(err.localizedMessage),
                        wardSpeechNonce = state.wardSpeechNonce + 1
                    )
                }
            }
        }
    }

    fun saveTodayCheckin(energyLevel: EnergyLevel, energyPercent: Int, reflection: String) {
        viewModelScope.launch {
            _interaction.update { it.copy(isSubmitting = true) }
            val currentProfile = (profileState.value as? UiState.Success)?.data
            val currentTasks = (tasksState.value as? UiState.Success)?.data.orEmpty()
            val existingTodayCheckin = (checkinsState.value as? UiState.Success)?.data
                ?.firstOrNull { it.dateKey == todayDateKey }
            val liveCompletedTitles = currentTasks
                .filter { task ->
                    if (!task.completed) {
                        false
                    } else if (task.category == TaskCategory.GAME.wireValue) {
                        task.completedDate == todayDateKey ||
                            task.completedDate == effectiveCompletionDateKey(task.category, task.resetHour) ||
                            task.completedDate.isBlank()
                    } else {
                        task.completedDate == todayDateKey || task.completedDate.isBlank()
                    }
                }
                .map { it.title.trim() }
            val mergedCompletedTitles = (existingTodayCheckin?.completedTaskTitles.orEmpty() + liveCompletedTitles)
                .map { it.trim().take(120) }
                .filter { it.isNotEmpty() }
                .distinct()
                .take(100)
            val wardSnapshot = currentProfile.resolvedWardPlacements().map { it.encode() }
            val doctorSnapshotStr = _interaction.value.wardDoctorSnapshot.encode()
            val res = repository.saveCheckin(
                userId = currentUserId,
                dateKey = todayDateKey,
                energyLevel = energyLevel,
                energyPercent = energyPercent,
                reflection = reflection.take(300),
                wardSnapshot = wardSnapshot,
                doctorPoseSnapshot = doctorSnapshotStr,
                completedTaskTitles = mergedCompletedTitles
            )
            _interaction.update { state ->
                if (res.isSuccess) {
                    state.copy(
                        isSubmitting = false,
                        wardDoctorSpeech = DoctorQuotes.saveCheckinSuccessQuote,
                        wardSpeechNonce = state.wardSpeechNonce + 1
                    )
                } else {
                    state.copy(
                        isSubmitting = false,
                        wardDoctorSpeech = DoctorQuotes.saveCheckinErrorQuote(res.exceptionOrNull()?.localizedMessage),
                        wardSpeechNonce = state.wardSpeechNonce + 1
                    )
                }
            }
        }
    }

    fun updateNickname(nickname: String) {
        val clean = nickname.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val res = repository.updateFeifeiNickname(currentUserId, clean.take(30))
            _interaction.update { state ->
                if (res.isSuccess) {
                    val reply = DoctorQuotes.updateNicknameSuccessQuote(clean)
                    state.copy(
                        showAccountSheet = false,
                        feifeiQuote = reply,
                        prescriptionQuote = reply,
                        pharmacyQuote = reply,
                        memoryQuote = reply,
                        wardDoctorSpeech = reply,
                        wardSpeechNonce = state.wardSpeechNonce + 1
                    )
                } else {
                    val err = DoctorQuotes.updateNicknameErrorQuote(res.exceptionOrNull()?.localizedMessage)
                    state.copy(
                        feifeiQuote = err,
                        wardDoctorSpeech = err,
                        wardSpeechNonce = state.wardSpeechNonce + 1
                    )
                }
            }
        }
    }

    fun redeemPharmacyItem(item: PharmacyItem) {
        viewModelScope.launch {
            _interaction.update { it.copy(isSubmitting = true) }
            val res = repository.redeemPharmacyItem(currentUserId, item)
            _interaction.update { state ->
                if (res.isSuccess) {
                    state.copy(
                        isSubmitting = false,
                        pharmacyPose = DoctorPose.APPROVE,
                        pharmacyQuote = DoctorQuotes.redeemPharmacySuccessQuote(item.name)
                    )
                } else {
                    state.copy(
                        isSubmitting = false,
                        pharmacyQuote = DoctorQuotes.redeemPharmacyErrorQuote(res.exceptionOrNull()?.localizedMessage)
                    )
                }
            }
        }
    }

    fun toggleEquipPharmacyItem(item: PharmacyItem) {
        viewModelScope.launch {
            _interaction.update { it.copy(isSubmitting = true) }
            val res = repository.toggleEquipPharmacyItem(currentUserId, item.id)
            _interaction.update { state ->
                if (res.isSuccess) {
                    val equipped = res.getOrDefault(false)
                    state.copy(
                        isSubmitting = false,
                        pharmacyPose = if (equipped) DoctorPose.APPROVE else DoctorPose.PHARMACY,
                        pharmacyQuote = DoctorQuotes.equipPharmacyQuote(item.name, equipped)
                    )
                } else {
                    state.copy(
                        isSubmitting = false,
                        pharmacyQuote = DoctorQuotes.equipPharmacyErrorQuote(res.exceptionOrNull()?.localizedMessage)
                    )
                }
            }
        }
    }

    private companion object {
        const val TAG = "RehabViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
