package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.CustomTimetableConfig
import com.example.data.DoctorGearConfig
import com.example.data.DoctorPose
import com.example.data.DoctorQuotes
import com.example.data.GUEST_USER_ID
import com.example.data.GuestRehabRepository
import com.example.data.GuestRehabStore
import com.example.data.OtomeClinicTheme
import com.example.data.RehabRepository
import com.example.data.TaskCategory
import com.example.data.currentDayOfWeekMonToSun
import com.example.data.isEarly8AmStartTime
import com.example.notifications.DoctorNotificationScheduler
import com.example.ui.components.AccountSettingsSheet
import com.example.ui.components.AddTaskBottomSheet
import com.example.ui.components.DoctorEquipSheet
import com.example.ui.components.EditTaskDialog
import com.example.ui.components.ShrinkTaskDialog
import com.example.ui.components.SyncConflictDialog
import com.example.ui.screens.MemoriesScreen
import com.example.ui.screens.PharmacyScreen
import com.example.ui.screens.PrescriptionScreen
import com.example.ui.screens.RehabLogScreen
import com.example.ui.screens.RehabRoomScreen
import com.example.widget.RehabWidgetSync
import com.example.widget.TimetableWidgetSync
import com.example.widget.WidgetEventItem
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val guestStore = remember(context) { GuestRehabStore.getInstance(context) }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val isGuestMode by guestStore.isGuestModeFlow.collectAsStateWithLifecycle(initialValue = null)

    val user = currentUser
    when {
        user != null -> {
            key("user_${user.uid}") {
                MainRehabShell(
                    currentUserId = user.uid,
                    userDisplayName = user.displayName.orEmpty(),
                    userEmail = user.email.orEmpty(),
                    isGuestMode = false,
                    guestStore = guestStore
                )
            }
        }

        isGuestMode == null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        }

        isGuestMode == true -> {
            key("guest_session") {
                MainRehabShell(
                    currentUserId = GUEST_USER_ID,
                    userDisplayName = "旅人患者",
                    userEmail = "",
                    isGuestMode = true,
                    guestStore = guestStore
                )
            }
        }

        else -> {
            AuthScreen(
                onAuthSuccess = {
                    scope.launch { guestStore.setGuestMode(false) }
                },
                onGuestContinue = {
                    scope.launch { guestStore.setGuestMode(true) }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainRehabShell(
    currentUserId: String,
    userDisplayName: String,
    userEmail: String,
    isGuestMode: Boolean,
    guestStore: GuestRehabStore
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    val viewModel: RehabViewModel = viewModel(
        key = "${if (isGuestMode) "guest" else "user"}_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val dataSource = if (isGuestMode) {
                    GuestRehabRepository(guestStore)
                } else {
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    RehabRepository(db = db, guestStore = guestStore)
                }
                RehabViewModel(
                    repository = dataSource,
                    currentUserId = currentUserId,
                    initialDisplayName = userDisplayName
                )
            }
        }
    )

    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val tasksState by viewModel.tasksState.collectAsStateWithLifecycle()
    val checkinsState by viewModel.checkinsState.collectAsStateWithLifecycle()
    val interaction by viewModel.interaction.collectAsStateWithLifecycle()
    val syncConflict by viewModel.syncConflict.collectAsStateWithLifecycle()
    val isResolvingSyncConflict by viewModel.isResolvingSyncConflict.collectAsStateWithLifecycle()
    val syncConflictError by viewModel.syncConflictError.collectAsStateWithLifecycle()
    val doctorGear by guestStore.doctorGearFlow.collectAsStateWithLifecycle(initialValue = DoctorGearConfig())
    val otomeClinicProgress by guestStore.otomeClinicFlow.collectAsStateWithLifecycle(
        initialValue = GuestRehabStore.OtomeClinicProgress()
    )
    val clinicTheme by guestStore.clinicThemeFlow.collectAsStateWithLifecycle(
        initialValue = OtomeClinicTheme.TREEHOUSE
    )
    val hasSeenFirstWelcome by remember(guestStore) {
        guestStore.hasSeenFirstWelcomeFlow.map<Boolean, Boolean?> { it }
    }.collectAsStateWithLifecycle(initialValue = null)
    val customCategories by guestStore.customCategoriesFlow.collectAsStateWithLifecycle(
        initialValue = emptyList()
    )
    val visibleCategoryWires by guestStore.visibleCategoryWiresFlow.collectAsStateWithLifecycle(
        initialValue = setOf(TaskCategory.DAILY.wireValue)
    )
    val customTimetable by guestStore.customTimetableFlow.collectAsStateWithLifecycle(
        initialValue = CustomTimetableConfig()
    )
    val calendarEvents by guestStore.calendarEventsFlow.collectAsStateWithLifecycle(
        initialValue = emptyList()
    )
    val rehabPoints by guestStore.rehabPointsFlow.collectAsStateWithLifecycle(
        initialValue = 0L
    )
    val unlockedCgIds by guestStore.unlockedCgIdsFlow.collectAsStateWithLifecycle(
        initialValue = emptySet()
    )
    val specialChannelUnlocked by guestStore.specialChannelUnlockedFlow.collectAsStateWithLifecycle(
        initialValue = false
    )
    val hasSeenRehabGymIntro by guestStore.hasSeenRehabGymIntroFlow.collectAsStateWithLifecycle(
        initialValue = true
    )

    var showMenuDialog by remember { mutableStateOf(false) }

    val enabledBuiltInCategories = remember(visibleCategoryWires) {
        visibleCategoryWires.mapNotNull { w -> TaskCategory.entries.firstOrNull { it.wireValue == w } }
            .toSet()
            .ifEmpty { setOf(TaskCategory.DAILY) }
    }
    val allCategories = remember(customCategories) {
        TaskCategory.buildOptions(customCategories)
    }

    val profileData = (profileState as? UiState.Success)?.data
    val tasksData = (tasksState as? UiState.Success)?.data.orEmpty()
    val checkinsData = (checkinsState as? UiState.Success)?.data.orEmpty()
    val doctorNickname = profileData?.feifeiNickname?.let {
        if (it.isBlank() || it == "主治醫師") "主治醫生" else it
    } ?: "主治醫生"

    LaunchedEffect(isGuestMode, syncConflict, profileState, tasksState, checkinsState) {
        if (!isGuestMode &&
            syncConflict == null &&
            profileState is UiState.Success &&
            tasksState is UiState.Success &&
            checkinsState is UiState.Success &&
            profileData != null
        ) {
            viewModel.mirrorCloudBackupIfSynced(
                profile = profileData,
                tasks = tasksData,
                checkins = checkinsData
            )
        }
    }

    // Show bespoke first-entry quote only once when not yet seen
    LaunchedEffect(hasSeenFirstWelcome) {
        if (hasSeenFirstWelcome == false) {
            viewModel.showFirstWelcomeIfNeeded(false) {
                scope.launch { guestStore.markFirstWelcomeSeen() }
            }
        }
    }

    var hasNotificationPermission by remember(context) {
        mutableStateOf(DoctorNotificationScheduler.hasNotificationPermission(context))
    }
    var hasRequestedNotificationInSession by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted || DoctorNotificationScheduler.hasNotificationPermission(context)
    }
    val requestNotificationPermissionIfNeeded: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !DoctorNotificationScheduler.hasNotificationPermission(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            hasNotificationPermission = DoctorNotificationScheduler.hasNotificationPermission(context)
        }
    }

    // Request POST_NOTIFICATIONS smoothly once when entering the main shell on Android 13+
    LaunchedEffect(Unit) {
        DoctorNotificationScheduler.ensureNotificationChannel(context)
        if (!hasNotificationPermission && !hasRequestedNotificationInSession) {
            hasRequestedNotificationInSession = true
            requestNotificationPermissionIfNeeded()
        }
    }

    // Reset daily/game tasks cleanly across midnight / custom reset hours and accrue idle points on resume
    LaunchedEffect(currentUserId) {
        viewModel.resetExpiredTasksIfNeeded()
        guestStore.accrueIdleRehabPoints()
    }
    DisposableEffect(lifecycleOwner, currentUserId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = DoctorNotificationScheduler.hasNotificationPermission(context)
                viewModel.resetExpiredTasksIfNeeded()
                scope.launch { guestStore.accrueIdleRehabPoints() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val visibleTasks = remember(tasksData, visibleCategoryWires) {
        tasksData.filter { it.category in visibleCategoryWires }
    }
    val widgetCompletedCount = if (visibleTasks.isNotEmpty()) {
        visibleTasks.count { it.completed }
    } else {
        tasksData.count { it.completed }
    }
    val widgetTotalCount = if (visibleTasks.isNotEmpty()) {
        visibleTasks.size
    } else {
        tasksData.size
    }
    val dueTodayHomeworkTitles = remember(tasksData, viewModel.todayDateKey) {
        tasksData
            .filter {
                it.category == TaskCategory.HOMEWORK.wireValue &&
                    !it.completed &&
                    it.deadlineDate == viewModel.todayDateKey
            }
            .map { it.title }
    }

    val widgetEventsAndBanner = remember(customTimetable, calendarEvents, viewModel.todayDateKey) {
        data class WidgetCandidate(
            val event: WidgetEventItem,
            val timeText: String,
            val bannerText: String,
            val sortMin: Int
        )

        fun parseMinutes(timeText: String): Int? {
            val match = Regex("""(\d{1,2})\s*[:：]\s*(\d{2})""").find(timeText.trim()) ?: return null
            val h = match.groupValues[1].toIntOrNull()?.coerceIn(0, 23) ?: return null
            val m = match.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: return null
            return h * 60 + m
        }

        val todayDow = currentDayOfWeekMonToSun()
        val nowCal = java.util.Calendar.getInstance()
        val nowMinutes = nowCal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + nowCal.get(java.util.Calendar.MINUTE)
        val periodLookup = customTimetable.periods.associateBy { it.id }

        val courseCandidates = customTimetable.cells
            .filter { it.dayOfWeek == todayDow }
            .mapNotNull { cell ->
                val period = periodLookup[cell.periodId] ?: return@mapNotNull null
                val sMin = parseMinutes(period.startTime)
                val eMin = parseMinutes(period.endTime) ?: sMin?.let { (it + 60).coerceAtMost(1440) }
                WidgetCandidate(
                    event = WidgetEventItem(
                        title = cell.title,
                        startMinutes = sMin,
                        endMinutes = eMin,
                        isAllDay = sMin == null
                    ),
                    timeText = period.startTime,
                    bannerText = "今日課班表 ${period.startTime} ${cell.title}",
                    sortMin = sMin ?: 24 * 60
                )
            }
        val calendarCandidates = calendarEvents
            .filter { it.dateKey == viewModel.todayDateKey }
            .map { ev ->
                val bannerTime = if (ev.timeText.isBlank() || ev.timeText == "全日") "全天" else ev.timeText
                val sMin = parseMinutes(ev.timeText)
                val eMin = sMin?.let { (it + 60).coerceAtMost(1440) }
                WidgetCandidate(
                    event = WidgetEventItem(
                        title = ev.title,
                        startMinutes = sMin,
                        endMinutes = eMin,
                        isAllDay = sMin == null
                    ),
                    timeText = ev.timeText,
                    bannerText = "今日行程 $bannerTime ${ev.title}",
                    sortMin = sMin ?: 25 * 60
                )
            }
        val allCandidates = (courseCandidates + calendarCandidates).sortedBy { it.sortMin }
        val upcomingOrActive = allCandidates
            .filter { it.sortMin in 0..1439 && (it.event.endMinutes ?: (it.sortMin + 60)) >= nowMinutes }
            .minByOrNull { it.sortMin }
            ?: allCandidates.minByOrNull { it.sortMin }
        allCandidates.map { it.event } to upcomingOrActive
    }

    // Keep Android home screen desktop widget synchronized with live rehab progress, doctor outfit, & schedule/calendar/homework events
    LaunchedEffect(doctorNickname, widgetCompletedCount, widgetTotalCount, doctorGear, widgetEventsAndBanner, dueTodayHomeworkTitles) {
        val (todayWidgetEvents, topCandidate) = widgetEventsAndBanner
        RehabWidgetSync.syncFromApp(
            context = context,
            doctorName = doctorNickname,
            completedTasks = widgetCompletedCount,
            totalTasks = widgetTotalCount,
            doctorGear = doctorGear,
            todayEvents = todayWidgetEvents,
            dueTodayHomeworkTitles = dueTodayHomeworkTitles,
            courseReminder = topCandidate?.bannerText.orEmpty(),
            reminderTitle = topCandidate?.event?.title.orEmpty(),
            reminderTimeText = topCandidate?.timeText.orEmpty()
        )
    }

    LaunchedEffect(customTimetable) {
        TimetableWidgetSync.syncFromApp(context, customTimetable)
    }

    LaunchedEffect(tasksData, calendarEvents, customTimetable, doctorNickname, hasNotificationPermission) {
        DoctorNotificationScheduler.syncReminders(
            context = context,
            doctorName = doctorNickname,
            tasks = tasksData,
            calendarEvents = calendarEvents,
            customTimetable = customTimetable
        )
    }

    LaunchedEffect(visibleCategoryWires, allCategories) {
        val visibleOrdered = allCategories.filter { it.wireValue in visibleCategoryWires }
        if (visibleOrdered.none { it.wireValue == interaction.activeRoomCategory }) {
            val fallback = visibleOrdered.firstOrNull()?.wireValue ?: TaskCategory.DAILY.wireValue
            viewModel.selectRoomCategory(fallback)
        }
    }

    val toggleCategoryWireVisibility: (String) -> Unit = { wire ->
        val currentlyVisible = wire in visibleCategoryWires
        val next = if (currentlyVisible) {
            if (visibleCategoryWires.size > 1) visibleCategoryWires - wire else visibleCategoryWires
        } else {
            visibleCategoryWires + wire
        }
        scope.launch { guestStore.setVisibleCategoryWires(next) }
        if (!currentlyVisible) {
            viewModel.selectRoomCategory(wire)
        }
    }

    val toggleBuiltInCategory: (TaskCategory) -> Unit = { category ->
        toggleCategoryWireVisibility(category.wireValue)
    }

    // BackHandler returns from secondary tabs to ROOM
    BackHandler(enabled = interaction.selectedTab != RehabTab.ROOM) {
        viewModel.selectTab(RehabTab.ROOM)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = interaction.selectedTab.topBarTitleZh,
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.setAccountSheetVisible(true) },
                            modifier = Modifier.testTag("account_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = "掛號病歷與帳號設定"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            floatingActionButton = {
                if (interaction.selectedTab == RehabTab.ROOM) {
                    ExtendedFloatingActionButton(
                        onClick = { viewModel.setAddTaskSheetVisible(true) },
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null
                            )
                        },
                        text = { Text("新增療程") },
                        shape = RoundedCornerShape(20.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("add_task_fab")
                    )
                }
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ) {
                        RehabTab.entries.forEach { tab ->
                            val selected = interaction.selectedTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.topBarTitleZh
                                    )
                                },
                                label = { Text(tab.navLabelZh, maxLines = 1, softWrap = false) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        RehabTab.entries.forEach { tab ->
                            val selected = interaction.selectedTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.topBarTitleZh
                                    )
                                },
                                label = { Text(tab.navLabelZh, maxLines = 1, softWrap = false) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val contentModifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxWidth()

                    when (interaction.selectedTab) {
                        RehabTab.ROOM -> RehabRoomScreen(
                            profileState = profileState,
                            tasksState = tasksState,
                            interaction = interaction,
                            doctorGear = doctorGear,
                            allCategories = allCategories,
                            visibleCategoryWires = visibleCategoryWires,
                            activeCategoryWire = interaction.activeRoomCategory,
                            todayDateKey = viewModel.todayDateKey,
                            customTimetable = customTimetable,
                            onSelectActiveCategory = viewModel::selectRoomCategory,
                            onToggleCategoryVisibility = toggleCategoryWireVisibility,
                            onAddCustomCategory = { rawName ->
                                scope.launch {
                                    val created = guestStore.addCustomCategory(rawName)
                                    if (created != null) {
                                        viewModel.selectRoomCategory(created)
                                    }
                                }
                            },
                            onRemoveCustomCategory = { wire ->
                                scope.launch {
                                    guestStore.removeCustomCategory(wire)
                                }
                            },
                            onPatFeifei = viewModel::patFeifei,
                            onOpenDoctorEquip = { viewModel.setDoctorEquipSheetVisible(true) },
                            onOpenPharmacy = { viewModel.selectTab(RehabTab.PHARMACY) },
                            onToggleTask = viewModel::toggleTask,
                            onToggleTaskTimeSlot = viewModel::toggleTaskTimeSlot,
                            onToggleTaskSplitMode = { task, enabled, morning, noon, evening ->
                                viewModel.toggleTaskSplitMode(task, enabled, morning, noon, evening)
                            },
                            onReorderTasks = viewModel::reorderTasks,
                            onUpdateTimetableMeta = { title, corner ->
                                scope.launch {
                                    guestStore.updateTimetable { cfg ->
                                        cfg.copy(tableTitle = title, cornerLabel = corner)
                                    }
                                }
                            },
                            onUpsertTimetablePeriod = { period ->
                                requestNotificationPermissionIfNeeded()
                                scope.launch {
                                    var hasCellsOnPeriod = false
                                    guestStore.updateTimetable { cfg ->
                                        hasCellsOnPeriod = cfg.cells.any { it.periodId == period.id }
                                        val nextPeriods = (cfg.periods.filterNot { it.id == period.id } + period)
                                            .sortedBy { it.startTime }
                                        cfg.copy(periods = nextPeriods)
                                    }
                                    if (hasCellsOnPeriod && isEarly8AmStartTime(period.startTime)) {
                                        viewModel.reportRoomDoctorQuote(
                                            quote = DoctorQuotes.early8AmCourseQuote,
                                            pose = DoctorPose.SIPPING
                                        )
                                    }
                                }
                            },
                            onDeleteTimetablePeriod = { periodId ->
                                scope.launch {
                                    guestStore.updateTimetable { cfg ->
                                        cfg.copy(
                                            periods = cfg.periods.filterNot { it.id == periodId },
                                            cells = cfg.cells.filterNot { it.periodId == periodId }
                                        )
                                    }
                                }
                            },
                            onSaveTimetableCell = { cell, periodStartTime ->
                                requestNotificationPermissionIfNeeded()
                                scope.launch {
                                    guestStore.updateTimetable { cfg ->
                                        val nextCells = cfg.cells.filterNot {
                                            it.periodId == cell.periodId && it.dayOfWeek == cell.dayOfWeek
                                        } + cell
                                        cfg.copy(cells = nextCells)
                                    }
                                    if (isEarly8AmStartTime(periodStartTime)) {
                                        viewModel.reportRoomDoctorQuote(
                                            quote = DoctorQuotes.early8AmCourseQuote,
                                            pose = DoctorPose.SIPPING
                                        )
                                    }
                                }
                            },
                            onClearTimetableCell = { periodId, dow ->
                                scope.launch {
                                    guestStore.updateTimetable { cfg ->
                                        cfg.copy(
                                            cells = cfg.cells.filterNot {
                                                it.periodId == periodId && it.dayOfWeek == dow
                                            }
                                        )
                                    }
                                }
                            },
                            onRequestEditTask = viewModel::setTaskToEdit,
                            onRequestShrinkTask = viewModel::setTaskToShrink,
                            onDeleteTask = viewModel::deleteTask,
                            onOpenAddSheet = { viewModel.setAddTaskSheetVisible(true) },
                            modifier = contentModifier
                        )

                        RehabTab.PRESCRIPTIONS -> PrescriptionScreen(
                            doctorName = doctorNickname,
                            interaction = interaction,
                            doctorGear = doctorGear,
                            enabledCategories = enabledBuiltInCategories,
                            rehabPoints = rehabPoints,
                            specialChannelUnlocked = specialChannelUnlocked,
                            hasSeenRehabGymIntro = hasSeenRehabGymIntro,
                            onMarkRehabGymIntroSeen = {
                                scope.launch { guestStore.markRehabGymIntroSeen() }
                            },
                            onUnlockSpecialChannel = {
                                scope.launch { guestStore.setSpecialChannelUnlocked(true) }
                            },
                            onLockSpecialChannel = {
                                scope.launch { guestStore.setSpecialChannelUnlocked(false) }
                            },
                            onAddRehabPoints = { delta ->
                                scope.launch { guestStore.addRehabPoints(delta) }
                            },
                            onSetRehabPoints = { targetPoints ->
                                scope.launch { guestStore.setRehabPoints(targetPoints) }
                            },
                            onAccrueIdlePoints = {
                                scope.launch { guestStore.accrueIdleRehabPoints() }
                            },
                            onToggleCategory = toggleBuiltInCategory,
                            onPatDoctor = viewModel::patPrescriptionDoctor,
                            onSelectEnergy = viewModel::selectEnergyFilter,
                            onDrawRandom = { viewModel.drawRandomPrescription(enabledBuiltInCategories) },
                            onAddPrescription = { rx ->
                                viewModel.addPrescriptionToToday(rx) { cat ->
                                    if (cat.wireValue !in visibleCategoryWires) {
                                        scope.launch {
                                            guestStore.setVisibleCategoryWires(visibleCategoryWires + cat.wireValue)
                                        }
                                    }
                                }
                            },
                            modifier = contentModifier
                        )

                        RehabTab.PHARMACY -> PharmacyScreen(
                            profileState = profileState,
                            tasksState = tasksState,
                            todayDateKey = viewModel.todayDateKey,
                            pharmacyPose = interaction.pharmacyPose,
                            pharmacyQuote = interaction.pharmacyQuote,
                            pharmacyPatCounter = interaction.pharmacyPatCounter,
                            doctorGear = doctorGear,
                            clinicProgress = otomeClinicProgress,
                            clinicTheme = clinicTheme,
                            isSubmitting = interaction.isSubmitting,
                            onPatPharmacyDoctor = viewModel::patPharmacyDoctor,
                            onRedeemItem = viewModel::redeemPharmacyItem,
                            onToggleEquipItem = viewModel::toggleEquipPharmacyItem,
                            onSelectClinicTheme = { theme ->
                                scope.launch { guestStore.setClinicTheme(theme) }
                            },
                            onRecordClinicInteraction = {
                                scope.launch { guestStore.recordClinicInteraction(viewModel.todayDateKey) }
                            },
                            onRecordSecretLetterGiven = {
                                scope.launch { guestStore.recordSecretLetterGiven(viewModel.todayDateKey) }
                            },
                            onSaveDrawnHeartRx = { rxId ->
                                scope.launch { guestStore.saveDrawnHeartRx(rxId, viewModel.todayDateKey) }
                            },
                            modifier = contentModifier
                        )

                        RehabTab.MEMORIES -> MemoriesScreen(
                            profileState = profileState,
                            tasksState = tasksState,
                            checkinsState = checkinsState,
                            interaction = interaction,
                            doctorGear = doctorGear,
                            rehabPoints = rehabPoints,
                            unlockedCgIds = unlockedCgIds,
                            onUnlockCg = { cg ->
                                scope.launch {
                                    guestStore.unlockSpecialCg(cg)
                                        .onSuccess {
                                            viewModel.reportDoctorMessage(DoctorQuotes.cgUnlockSuccessQuote(cg.title))
                                        }
                                        .onFailure { err ->
                                            viewModel.reportDoctorMessage(
                                                err.localizedMessage ?: DoctorQuotes.cgUnlockInsufficientDefaultQuote
                                            )
                                        }
                                }
                            },
                            onPatDoctor = viewModel::patMemoryDoctor,
                            modifier = contentModifier
                        )

                        RehabTab.LOG -> RehabLogScreen(
                            todayDateKey = viewModel.todayDateKey,
                            profileState = profileState,
                            checkinsState = checkinsState,
                            tasksState = tasksState,
                            calendarEvents = calendarEvents,
                            wardDoctorPose = interaction.wardDoctorPose,
                            wardDoctorSpeech = interaction.wardDoctorSpeech,
                            wardSpeechNonce = interaction.wardSpeechNonce,
                            doctorGear = doctorGear,
                            isSubmitting = interaction.isSubmitting,
                            onWardDoctorPoseChanged = viewModel::updateWardDoctorPose,
                            onWardDoctorSnapshotChanged = viewModel::updateWardDoctorSnapshot,
                            onToggleWardItem = viewModel::toggleWardItem,
                            onMoveWardItem = viewModel::moveWardItem,
                            onUpsertCalendarEvent = { event ->
                                requestNotificationPermissionIfNeeded()
                                scope.launch { guestStore.upsertCalendarEvent(event) }
                            },
                            onDeleteCalendarEvent = { eventId ->
                                scope.launch { guestStore.deleteCalendarEvent(eventId) }
                            },
                            onOpenPharmacy = { viewModel.selectTab(RehabTab.PHARMACY) },
                            onSaveCheckin = viewModel::saveTodayCheckin,
                            modifier = contentModifier
                        )
                    }
                }
            }
        }
    }

    if (interaction.showAddTaskSheet) {
        AddTaskBottomSheet(
            isSubmitting = interaction.isSubmitting,
            availableCategories = allCategories,
            defaultCategoryWire = interaction.activeRoomCategory,
            onDismiss = { viewModel.setAddTaskSheetVisible(false) },
            onConfirmAdd = { title, effort, note, categoryWire, resetHour, deadlineDate ->
                if (deadlineDate.isNotBlank()) {
                    requestNotificationPermissionIfNeeded()
                }
                if (categoryWire !in visibleCategoryWires) {
                    scope.launch { guestStore.setVisibleCategoryWires(visibleCategoryWires + categoryWire) }
                }
                viewModel.addTask(
                    title = title,
                    effortLevel = effort,
                    note = note,
                    categoryWire = categoryWire,
                    resetHour = resetHour,
                    deadlineDate = deadlineDate
                )
            }
        )
    }

    interaction.taskToShrink?.let { task ->
        ShrinkTaskDialog(
            task = task,
            isSubmitting = interaction.isSubmitting,
            onDismiss = { viewModel.setTaskToShrink(null) },
            onConfirmShrink = { newTitle, newNote ->
                viewModel.shrinkTask(task, newTitle, newNote)
            }
        )
    }

    interaction.taskToEdit?.let { task ->
        EditTaskDialog(
            task = task,
            isSubmitting = interaction.isSubmitting,
            onDismiss = { viewModel.setTaskToEdit(null) },
            onConfirmEdit = { newTitle, newNote, effortLevel, resetHour, deadlineDate ->
                if (deadlineDate.isNotBlank()) {
                    requestNotificationPermissionIfNeeded()
                }
                viewModel.editTask(
                    task = task,
                    newTitle = newTitle,
                    newNote = newNote,
                    effortLevel = effortLevel,
                    resetHour = resetHour,
                    deadlineDate = deadlineDate
                )
            }
        )
    }

    if (interaction.showAccountSheet) {
        var isLinkingGoogle by remember { mutableStateOf(false) }
        var accountAuthError by remember { mutableStateOf<String?>(null) }

        AccountSettingsSheet(
            userEmail = userEmail,
            isGuestMode = isGuestMode,
            currentNickname = doctorNickname,
            isLinkingGoogle = isLinkingGoogle,
            authErrorMessage = accountAuthError,
            hasNotificationPermission = hasNotificationPermission,
            onDismiss = {
                if (!isLinkingGoogle) {
                    accountAuthError = null
                    viewModel.setAccountSheetVisible(false)
                }
            },
            onSaveNickname = viewModel::updateNickname,
            onLinkGoogleAccount = {
                accountAuthError = null
                isLinkingGoogle = true
                onGoogleSignInClicked(
                    context = context,
                    credentialManager = credentialManager,
                    onAuthSuccess = {
                        isLinkingGoogle = false
                        viewModel.setAccountSheetVisible(false)
                        scope.launch { guestStore.setGuestMode(false) }
                    },
                    onAuthError = { msg ->
                        isLinkingGoogle = false
                        accountAuthError = msg
                        viewModel.reportDoctorMessage(DoctorQuotes.accountLinkSlowQuote(msg))
                    },
                    scope = scope,
                    onAuthCancelled = {
                        isLinkingGoogle = false
                    }
                )
            },
            onRequestNotificationPermission = {
                requestNotificationPermissionIfNeeded()
            },
            onSendTestNotification = {
                DoctorNotificationScheduler.sendTestNotification(context, doctorNickname)
            },
            onSignOut = {
                viewModel.setAccountSheetVisible(false)
                if (isGuestMode) {
                    scope.launch { guestStore.setGuestMode(false) }
                } else {
                    signOutUser(
                        credentialManager = credentialManager,
                        scope = scope
                    )
                }
            }
        )
    }

    if (interaction.showDoctorEquipSheet) {
        DoctorEquipSheet(
            doctorName = doctorNickname,
            currentPose = interaction.stagePose,
            currentQuote = interaction.feifeiQuote,
            patCounter = interaction.patCounter,
            gear = doctorGear,
            onPatDoctor = viewModel::patFeifei,
            onSelectCoat = { coat -> scope.launch { guestStore.setDoctorCoat(coat) } },
            onSelectStethoscope = { scopeType -> scope.launch { guestStore.setDoctorStethoscope(scopeType) } },
            onSelectHeadwear = { headwear -> scope.launch { guestStore.setDoctorHeadwear(headwear) } },
            onDismiss = { viewModel.setDoctorEquipSheetVisible(false) }
        )
    }

    syncConflict?.let { conflict ->
        SyncConflictDialog(
            conflict = conflict,
            isResolving = isResolvingSyncConflict,
            errorMessage = syncConflictError,
            onChooseLocal = viewModel::resolveConflictChooseLocal,
            onChooseCloud = viewModel::resolveConflictChooseCloud
        )
    }
}

private fun tabIcon(tab: RehabTab, selected: Boolean) = when (tab) {
    RehabTab.ROOM -> if (selected) Icons.Rounded.Spa else Icons.Outlined.Spa
    RehabTab.PRESCRIPTIONS -> if (selected) Icons.Rounded.LocalHospital else Icons.Outlined.LocalHospital
    RehabTab.PHARMACY -> if (selected) Icons.Rounded.LocalPharmacy else Icons.Outlined.LocalPharmacy
    RehabTab.MEMORIES -> if (selected) Icons.Rounded.CollectionsBookmark else Icons.Outlined.CollectionsBookmark
    RehabTab.LOG -> if (selected) Icons.Rounded.AutoStories else Icons.Outlined.AutoStories
}

