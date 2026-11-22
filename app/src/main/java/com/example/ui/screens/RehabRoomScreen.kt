package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Splitscreen
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.CustomTimetableConfig
import com.example.data.DoctorGearConfig
import com.example.data.RehabCatalog
import com.example.data.RehabProfile
import com.example.data.RehabTask
import com.example.data.TaskCategory
import com.example.data.TaskCategoryOption
import com.example.data.TimetableCellColor
import com.example.data.TimetableCellEntry
import com.example.data.TimetablePeriodRow
import com.example.data.currentDayOfWeekMonToSun
import com.example.data.daysUntilDateKey
import com.example.ui.RehabInteractionState
import com.example.ui.UiState
import com.example.ui.components.FeifeiStageCard
import com.example.ui.components.WheelTimePicker
import com.example.ui.components.formatHourMinute
import com.example.ui.components.parseHourMinute
import com.example.widget.TimetableWidgetSync
import java.util.Locale
import java.util.UUID

@Composable
fun RehabRoomScreen(
    profileState: UiState<RehabProfile?>,
    tasksState: UiState<List<RehabTask>>,
    interaction: RehabInteractionState,
    doctorGear: DoctorGearConfig,
    allCategories: List<TaskCategoryOption> = TaskCategory.buildOptions(emptyList()),
    visibleCategoryWires: Set<String> = setOf(TaskCategory.DAILY.wireValue),
    activeCategoryWire: String = TaskCategory.DAILY.wireValue,
    todayDateKey: String,
    customTimetable: CustomTimetableConfig = CustomTimetableConfig(),
    onSelectActiveCategory: (String) -> Unit = {},
    onToggleCategoryVisibility: (String) -> Unit = {},
    onAddCustomCategory: (String) -> Unit = {},
    onRemoveCustomCategory: (String) -> Unit = {},
    onPatFeifei: () -> Unit,
    onOpenDoctorEquip: () -> Unit,
    onOpenPharmacy: () -> Unit,
    onToggleTask: (RehabTask) -> Unit,
    onToggleTaskTimeSlot: (RehabTask, Int) -> Unit = { _, _ -> },
    onToggleTaskSplitMode: (RehabTask, Boolean, Boolean, Boolean, Boolean) -> Unit = { _, _, _, _, _ -> },
    onReorderTasks: (List<String>) -> Unit = {},
    onUpdateTimetableMeta: (String, String) -> Unit = { _, _ -> },
    onUpsertTimetablePeriod: (TimetablePeriodRow) -> Unit = {},
    onDeleteTimetablePeriod: (String) -> Unit = {},
    onSaveTimetableCell: (TimetableCellEntry, String) -> Unit = { _, _ -> },
    onClearTimetableCell: (String, Int) -> Unit = { _, _ -> },
    onRequestEditTask: (RehabTask) -> Unit,
    onRequestShrinkTask: (RehabTask) -> Unit,
    onDeleteTask: (RehabTask) -> Unit,
    onOpenAddSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = (profileState as? UiState.Success)?.data
    val todayCount = if (profile?.todayDate == todayDateKey) profile.todayCount else 0L
    val totalCompleted = profile?.totalCompleted ?: 0L
    val equippedIds = profile?.equippedItemIds?.toSet().orEmpty()
    val equippedItems = remember(equippedIds) {
        RehabCatalog.pharmacyItems.filter { it.id in equippedIds }
    }
    val nickname = profile?.feifeiNickname?.ifBlank { "主治醫生" } ?: "主治醫生"

    val visibleCategories = remember(allCategories, visibleCategoryWires) {
        allCategories.filter { it.wireValue in visibleCategoryWires }
            .ifEmpty { listOf(allCategories.first()) }
    }
    val effectiveActiveWire = remember(visibleCategories, activeCategoryWire) {
        if (visibleCategories.any { it.wireValue == activeCategoryWire }) {
            activeCategoryWire
        } else {
            visibleCategories.first().wireValue
        }
    }

    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryInput by remember { mutableStateOf("") }
    var splitConfigTask by remember { mutableStateOf<RehabTask?>(null) }

    // Long-press drag reorder state
    val allTasks = (tasksState as? UiState.Success)?.data.orEmpty()
    val filteredFromVm = remember(allTasks, effectiveActiveWire) {
        allTasks.filter { task ->
            val catWire = task.category.trim().ifEmpty { TaskCategory.DAILY.wireValue }
            catWire == effectiveActiveWire
        }
    }
    var draggingTaskId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var localTasks by remember { mutableStateOf(filteredFromVm) }
    val itemHeights = remember { mutableStateMapOf<String, Int>() }
    val latestOnReorderTasks by rememberUpdatedState(onReorderTasks)
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(filteredFromVm, draggingTaskId) {
        if (draggingTaskId == null) {
            localTasks = filteredFromVm
        }
    }

    val homeworkReminder = remember(allTasks, todayDateKey, effectiveActiveWire) {
        val msg = RehabCatalog.homeworkDeadlineReminder(allTasks, todayDateKey)
        if (msg == null) null
        else if (effectiveActiveWire == TaskCategory.HOMEWORK.wireValue) msg
        else {
            val hasImmediate = allTasks.any {
                it.category == TaskCategory.HOMEWORK.wireValue &&
                    !it.completed &&
                    (daysUntilDateKey(it.deadlineDate, todayDateKey) ?: 99) <= 1
            }
            if (hasImmediate) msg else null
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "feifei_stage") {
            FeifeiStageCard(
                feifeiNickname = nickname,
                todayCount = todayCount,
                totalCompleted = totalCompleted,
                quote = interaction.feifeiQuote,
                patCounter = interaction.patCounter,
                stagePose = interaction.stagePose,
                doctorGear = doctorGear,
                equippedItems = equippedItems,
                onPatFeifei = onPatFeifei,
                onOpenDoctorEquip = onOpenDoctorEquip,
                onOpenPharmacy = onOpenPharmacy
            )
        }

        item(key = "tasks_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "今日復健療程",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Box {
                            IconButton(
                                onClick = { categoryMenuExpanded = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("room_category_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "設定療程顯示類別",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            DropdownMenu(
                                expanded = categoryMenuExpanded,
                                onDismissRequest = { categoryMenuExpanded = false }
                            ) {
                                Text(
                                    text = "勾選要顯示的療程類別",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                                allCategories.forEach { category ->
                                    val isChecked = category.wireValue in visibleCategoryWires
                                    DropdownMenuItem(
                                        text = { Text(category.labelZh) },
                                        leadingIcon = {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        trailingIcon = if (!category.isBuiltIn) {
                                            {
                                                IconButton(
                                                    onClick = { onRemoveCustomCategory(category.wireValue) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Close,
                                                        contentDescription = "刪除 ${category.labelZh}",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        } else {
                                            null
                                        },
                                        onClick = { onToggleCategoryVisibility(category.wireValue) },
                                        modifier = Modifier.testTag("room_category_menu_${category.wireValue}")
                                    )
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "新增自訂類別",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Add,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        categoryMenuExpanded = false
                                        newCategoryInput = ""
                                        showAddCategoryDialog = true
                                    },
                                    modifier = Modifier.testTag("room_add_category_menu_item")
                                )
                            }
                        }
                    }

                    Text(
                        text = "長按拖曳排序・點 ⋮ 切早中晚",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Diary-style SingleChoiceSegmentedButtonRow category switcher
                val useHorizontalScroll = visibleCategories.size > 4
                Box(
                    modifier = if (useHorizontalScroll) {
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    } else {
                        Modifier.fillMaxWidth()
                    }
                ) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = (if (useHorizontalScroll) Modifier else Modifier.fillMaxWidth())
                            .testTag("room_category_switcher")
                    ) {
                        visibleCategories.forEachIndexed { index, category ->
                            val selected = category.wireValue == effectiveActiveWire
                            SegmentedButton(
                                selected = selected,
                                onClick = { onSelectActiveCategory(category.wireValue) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = visibleCategories.size
                                ),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = MaterialTheme.colorScheme.primary,
                                    activeContentColor = MaterialTheme.colorScheme.onPrimary,
                                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    inactiveContentColor = MaterialTheme.colorScheme.primary,
                                    activeBorderColor = MaterialTheme.colorScheme.primary,
                                    inactiveBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                ),
                                label = {
                                    Text(
                                        text = category.labelZh,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = (if (useHorizontalScroll) Modifier.widthIn(min = 82.dp) else Modifier)
                                    .testTag("room_category_tab_${category.wireValue}")
                            )
                        }
                    }
                }
            }
        }

        if (homeworkReminder != null) {
            item(key = "homework_deadline_banner") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = homeworkReminder,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        if (effectiveActiveWire == TaskCategory.COURSE.wireValue) {
            item(key = "course_schedule_section") {
                CustomTimetableSection(
                    config = customTimetable,
                    onUpdateMeta = onUpdateTimetableMeta,
                    onUpsertPeriod = onUpsertTimetablePeriod,
                    onDeletePeriod = onDeleteTimetablePeriod,
                    onSaveCell = onSaveTimetableCell,
                    onClearCell = onClearTimetableCell
                )
            }
        }

        when (tasksState) {
            is UiState.Loading -> {
                item(key = "loading_tasks") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            is UiState.Error -> {
                item(key = "error_tasks") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "讀取微任務時遇到問題：${tasksState.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            is UiState.Success -> {
                if (localTasks.isEmpty()) {
                    item(key = "empty_tasks") {
                        EmptyRehabCard(
                            categoryLabel = TaskCategory.labelFor(effectiveActiveWire),
                            onAddClick = onOpenAddSheet
                        )
                    }
                } else {
                    itemsIndexed(localTasks, key = { _, task -> task.id }) { _, task ->
                        val isDraggingThis = draggingTaskId == task.id
                        CompactRehabTaskRow(
                            task = task,
                            todayDateKey = todayDateKey,
                            isDragging = isDraggingThis,
                            dragTranslationY = if (isDraggingThis) dragOffsetY else 0f,
                            onMeasuredHeight = { h -> itemHeights[task.id] = h },
                            modifier = Modifier
                                .zIndex(if (isDraggingThis) 10f else 0f)
                                .pointerInput(task.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            draggingTaskId = task.id
                                            dragOffsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY += dragAmount.y
                                            val currentIdx = localTasks.indexOfFirst { it.id == task.id }
                                            if (currentIdx >= 0) {
                                                val currentHeight = (itemHeights[task.id] ?: 160).coerceAtLeast(80).toFloat()
                                                if (dragOffsetY > currentHeight * 0.62f && currentIdx < localTasks.lastIndex) {
                                                    val mutable = localTasks.toMutableList()
                                                    val moved = mutable.removeAt(currentIdx)
                                                    mutable.add(currentIdx + 1, moved)
                                                    localTasks = mutable
                                                    dragOffsetY -= currentHeight
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                } else if (dragOffsetY < -currentHeight * 0.62f && currentIdx > 0) {
                                                    val mutable = localTasks.toMutableList()
                                                    val moved = mutable.removeAt(currentIdx)
                                                    mutable.add(currentIdx - 1, moved)
                                                    localTasks = mutable
                                                    dragOffsetY += currentHeight
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            draggingTaskId = null
                                            dragOffsetY = 0f
                                            latestOnReorderTasks(localTasks.map { it.id })
                                        },
                                        onDragCancel = {
                                            draggingTaskId = null
                                            dragOffsetY = 0f
                                        }
                                    )
                                },
                            onToggle = { onToggleTask(task) },
                            onToggleTimeSlot = { slotIdx -> onToggleTaskTimeSlot(task, slotIdx) },
                            onToggleSplitMode = { splitConfigTask = task },
                            onEdit = { onRequestEditTask(task) },
                            onShrink = { onRequestShrinkTask(task) },
                            onDelete = { onDeleteTask(task) }
                        )
                    }
                }
            }
        }
    }

    splitConfigTask?.let { targetTask ->
        TimeSlotSplitDialog(
            task = targetTask,
            onDismiss = { splitConfigTask = null },
            onApply = { enabled, morning, noon, evening ->
                onToggleTaskSplitMode(targetTask, enabled, morning, noon, evening)
                splitConfigTask = null
            }
        )
    }

    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("新增療程類別") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "想多開一個專屬分區也沒問題，例如：閱讀、外出、復健運動。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newCategoryInput,
                        onValueChange = { if (it.length <= 20) newCategoryInput = it },
                        label = { Text("類別名稱（最多 20 字）") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_category_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newCategoryInput.trim()
                        if (trimmed.isNotEmpty()) {
                            onAddCustomCategory(trimmed)
                            showAddCategoryDialog = false
                        }
                    },
                    enabled = newCategoryInput.trim().isNotEmpty(),
                    modifier = Modifier.testTag("confirm_add_category_button")
                ) {
                    Text("新增")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompactRehabTaskRow(
    task: RehabTask,
    todayDateKey: String,
    isDragging: Boolean,
    dragTranslationY: Float,
    onMeasuredHeight: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit,
    onToggleTimeSlot: (Int) -> Unit,
    onToggleSplitMode: () -> Unit,
    onEdit: () -> Unit,
    onShrink: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val cardColor by animateColorAsState(
        targetValue = when {
            isDragging -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
            task.completed -> MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.65f)
            else -> MaterialTheme.colorScheme.surfaceContainerLowest
        },
        label = "taskCardColor"
    )

    val isGame = task.category == TaskCategory.GAME.wireValue
    val isHomework = task.category == TaskCategory.HOMEWORK.wireValue
    val daysLeft = remember(task.deadlineDate, todayDateKey) {
        if (isHomework && task.deadlineDate.isNotBlank()) daysUntilDateKey(task.deadlineDate, todayDateKey) else null
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = cardColor,
        shadowElevation = if (isDragging) 8.dp else 0.dp,
        border = BorderStroke(
            width = if (isDragging) 1.8.dp else 1.dp,
            color = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { onMeasuredHeight(it.height) }
            .graphicsLayer {
                translationY = dragTranslationY
                scaleX = if (isDragging) 1.02f else 1f
                scaleY = if (isDragging) 1.02f else 1f
            }
            .clickable(onClick = onToggle)
            .testTag("task_card_${task.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 4.dp, top = 6.dp, bottom = if (task.timeSlotsEnabled) 10.dp else 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DragIndicator,
                    contentDescription = "長按拖曳排序",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.36f),
                    modifier = Modifier.size(16.dp)
                )

                IconButton(
                    onClick = onToggle,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("toggle_task_${task.id}")
                ) {
                    if (task.completed) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "取消完成 ${task.title}",
                                    tint = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = BorderStroke(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                            ),
                            modifier = Modifier.size(24.dp)
                        ) {}
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        color = if (task.completed) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (isGame || (isHomework && task.deadlineDate.isNotBlank())) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isGame) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "每日 %02d:00 重置", task.resetHour.coerceIn(0L, 23L)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (isHomework && task.deadlineDate.isNotBlank()) {
                                val badgeText = when {
                                    daysLeft == null -> "死線 ${task.deadlineDate}"
                                    daysLeft < 0 -> "已逾期 ${-daysLeft} 天 (${task.deadlineDate})"
                                    daysLeft == 0 -> "今天到期 (${task.deadlineDate})"
                                    else -> "死線剩 $daysLeft 天 (${task.deadlineDate})"
                                }
                                val isUrgent = daysLeft != null && daysLeft <= 2 && !task.completed
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isUrgent) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    }
                                ) {
                                    Text(
                                        text = badgeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isUrgent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isUrgent) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("more_task_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "更多操作",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(if (task.timeSlotsEnabled) "調整「早／中／晚」小塊" else "切小塊：自訂早／中／晚")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Splitscreen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onToggleSplitMode()
                            },
                            modifier = Modifier.testTag("split_task_${task.id}")
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    when {
                                        isGame -> "編輯項目／換日時間"
                                        isHomework -> "編輯項目／設定死線"
                                        else -> "編輯這個項目"
                                    }
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            },
                            modifier = Modifier.testTag("edit_task_${task.id}")
                        )
                        if (!task.completed) {
                            DropdownMenuItem(
                                text = { Text("太難了，縮小步驟") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoFixHigh,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onShrink()
                                },
                                modifier = Modifier.testTag("shrink_task_${task.id}")
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("放下這個任務") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                            modifier = Modifier.testTag("delete_task_${task.id}")
                        )
                    }
                }
            }

            if (task.timeSlotsEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 52.dp, end = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val slots = buildList {
                        if (task.morningEnabled) add(Triple(0, "早", task.morningDone))
                        if (task.noonEnabled) add(Triple(1, "中", task.noonDone))
                        if (task.eveningEnabled) add(Triple(2, "晚", task.eveningDone))
                    }.ifEmpty {
                        listOf(
                            Triple(0, "早", task.morningDone),
                            Triple(1, "中", task.noonDone),
                            Triple(2, "晚", task.eveningDone)
                        )
                    }
                    slots.forEach { (slotIndex, label, checked) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (checked) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            border = BorderStroke(
                                1.dp,
                                if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onToggleTimeSlot(slotIndex) }
                                .testTag("slot_${task.id}_$slotIndex")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium,
                                    color = if (checked) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimeSlotSplitDialog(
    task: RehabTask,
    onDismiss: () -> Unit,
    onApply: (enabled: Boolean, morning: Boolean, noon: Boolean, evening: Boolean) -> Unit
) {
    var morning by remember(task.id) { mutableStateOf(if (task.timeSlotsEnabled) task.morningEnabled else true) }
    var noon by remember(task.id) { mutableStateOf(if (task.timeSlotsEnabled) task.noonEnabled else true) }
    var evening by remember(task.id) { mutableStateOf(if (task.timeSlotsEnabled) task.eveningEnabled else true) }
    val hasAnySelected = morning || noon || evening

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Splitscreen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("自訂「早／中／晚」小塊") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "自由勾選這個任務需要的時段小塊（可只選早晚、早中、中晚或三個都要）：",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple("早", morning) { morning = !morning } to "split_opt_morning",
                        Triple("中", noon) { noon = !noon } to "split_opt_noon",
                        Triple("晚", evening) { evening = !evening } to "split_opt_evening"
                    ).forEach { (slot, tag) ->
                        val (label, checked, toggle) = slot
                        FilterChip(
                            selected = checked,
                            onClick = toggle,
                            label = {
                                Text(
                                    text = label,
                                    fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            leadingIcon = if (checked) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                null
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag(tag)
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        "早＋晚" to Triple(true, false, true),
                        "早＋中" to Triple(true, true, false),
                        "中＋晚" to Triple(false, true, true),
                        "早＋中＋晚" to Triple(true, true, true)
                    )
                    presets.forEach { (presetLabel, triple) ->
                        val isMatch = morning == triple.first && noon == triple.second && evening == triple.third
                        OutlinedButton(
                            onClick = {
                                morning = triple.first
                                noon = triple.second
                                evening = triple.third
                            },
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(
                                1.dp,
                                if (isMatch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.heightIn(min = 36.dp)
                        ) {
                            Text(
                                text = presetLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isMatch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (task.timeSlotsEnabled) {
                    TextButton(
                        onClick = { onApply(false, true, true, true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("disable_split_task_button")
                    ) {
                        Text(
                            text = "關閉切割（切回原本單格勾選）",
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(true, morning, noon, evening) },
                enabled = hasAnySelected,
                modifier = Modifier.testTag("confirm_split_task_button")
            ) {
                Text("套用切割")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomTimetableSection(
    config: CustomTimetableConfig,
    onUpdateMeta: (tableTitle: String, cornerLabel: String) -> Unit,
    onUpsertPeriod: (TimetablePeriodRow) -> Unit,
    onDeletePeriod: (String) -> Unit,
    onSaveCell: (TimetableCellEntry, periodStartTime: String) -> Unit,
    onClearCell: (periodId: String, dayOfWeek: Int) -> Unit
) {
    val todayDow = remember { currentDayOfWeekMonToSun() }
    val dayCols = remember {
        listOf(
            1 to "一",
            2 to "二",
            3 to "三",
            4 to "四",
            5 to "五",
            6 to "六",
            7 to "日"
        )
    }

    var showMetaDialog by remember { mutableStateOf(false) }
    var editingPeriod by remember { mutableStateOf<TimetablePeriodRow?>(null) }
    var showAddPeriodDialog by remember { mutableStateOf(false) }
    var editingCellTarget by remember { mutableStateOf<Pair<TimetablePeriodRow, Int>?>(null) }
    var lastChosenColorId by remember { mutableStateOf(TimetableCellColor.SKY.id) }

    val gridLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
    val periodColWidth = 74.dp
    val dayColWidth = 82.dp
    val tableTotalWidth = periodColWidth + dayColWidth * dayCols.size

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("course_schedule_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top customizable title banner (like "動畫一乙", styled in warm game blush-cream)
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showMetaDialog = true }
                    .testTag("timetable_meta_header")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = config.tableTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 28.dp)
                    )
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "自訂課表標題與左上角文字",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(18.dp)
                    )
                }
            }

            HorizontalDivider(thickness = 1.dp, color = gridLineColor)

            // Horizontally scrollable crisp grid: Corner + 一二三四五六日
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .testTag("course_schedule_table")
            ) {
                Column(modifier = Modifier.width(tableTotalWidth)) {
                    // Header Row: Corner label + Mon..Sun
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .width(periodColWidth)
                                .fillMaxHeight()
                                .clickable { showMetaDialog = true }
                                .padding(horizontal = 6.dp, vertical = 10.dp)
                                .testTag("timetable_corner_cell")
                        ) {
                            Text(
                                text = config.cornerLabel,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }

                        dayCols.forEach { (dow, shortDay) ->
                            val isToday = dow == todayDow
                            VerticalDivider(
                                modifier = Modifier.fillMaxHeight(),
                                thickness = 1.dp,
                                color = gridLineColor
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .width(dayColWidth)
                                    .fillMaxHeight()
                                    .background(
                                        if (isToday) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                                        } else {
                                            Color.Transparent
                                        }
                                    )
                                    .padding(vertical = 10.dp)
                                    .testTag("course_day_chip_$dow")
                            ) {
                                Text(
                                    text = if (isToday) "$shortDay・今" else shortDay,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isToday) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    HorizontalDivider(thickness = 1.dp, color = gridLineColor)

                    // Body Period Rows
                    config.periods.forEachIndexed { rowIndex, period ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                                .testTag("timetable_row_${period.id}")
                        ) {
                            // Leftmost Period + Time Cell
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .width(periodColWidth)
                                    .fillMaxHeight()
                                    .heightIn(min = 66.dp)
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.7f))
                                    .clickable {
                                        editingPeriod = period
                                        showAddPeriodDialog = true
                                    }
                                    .padding(horizontal = 4.dp, vertical = 6.dp)
                                    .testTag("timetable_period_${period.id}")
                            ) {
                                Text(
                                    text = period.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 14.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${period.startTime}-${period.endTime}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }

                            // Mon..Sun Course / Shift Cells
                            dayCols.forEach { (dow, _) ->
                                val isToday = dow == todayDow
                                val cell = config.cells.firstOrNull {
                                    it.periodId == period.id && it.dayOfWeek == dow
                                }
                                val cellColor = if (cell != null) {
                                    TimetableCellColor.fromId(cell.colorId)
                                } else {
                                    null
                                }
                                val cellBg = when {
                                    cellColor != null -> Color(cellColor.bgArgb)
                                    isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.14f)
                                    else -> MaterialTheme.colorScheme.surfaceContainerLowest
                                }

                                VerticalDivider(
                                    modifier = Modifier.fillMaxHeight(),
                                    thickness = 1.dp,
                                    color = gridLineColor
                                )

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .width(dayColWidth)
                                        .fillMaxHeight()
                                        .heightIn(min = 66.dp)
                                        .background(cellBg)
                                        .clickable {
                                            editingCellTarget = period to dow
                                        }
                                        .padding(horizontal = 4.dp, vertical = 6.dp)
                                        .testTag("timetable_cell_${period.id}_$dow")
                                ) {
                                    if (cell != null) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = cell.title,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(cellColor!!.fgArgb),
                                                textAlign = TextAlign.Center,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis,
                                                lineHeight = 14.sp
                                            )
                                            if (cell.note.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = cell.note,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = Color(cellColor.fgArgb).copy(alpha = 0.82f),
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    lineHeight = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (rowIndex < config.periods.lastIndex) {
                            HorizontalDivider(thickness = 1.dp, color = gridLineColor)
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = gridLineColor)

            // Bottom footer: quick instructions + Add Period Row button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "點標題、左側節次或任一格子皆可自訂・含週六日",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        editingPeriod = null
                        showAddPeriodDialog = true
                    },
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("add_timetable_period_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("新增節次", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    // 1. Edit Table Title & Corner Label Dialog
    if (showMetaDialog) {
        val context = LocalContext.current
        var titleInput by remember(config.tableTitle) { mutableStateOf(config.tableTitle) }
        var cornerInput by remember(config.cornerLabel) { mutableStateOf(config.cornerLabel) }
        var widgetShowWeekends by remember(showMetaDialog) {
            mutableStateOf(TimetableWidgetSync.isShowWeekends(context))
        }

        AlertDialog(
            onDismissRequest = { showMetaDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("自訂課表標題與欄首") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { if (it.length <= 24) titleInput = it },
                        label = { Text("頂部標題（例：動畫一乙、我的班表）") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cornerInput,
                        onValueChange = { if (it.length <= 10) cornerInput = it },
                        label = { Text("左上角欄名（例：商資、節次、班別）") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { widgetShowWeekends = !widgetShowWeekends }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "桌面小工具顯示週六、週日",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (widgetShowWeekends) "目前顯示週一～週日（7 天）" else "目前僅顯示週一～週五（5 天）",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = widgetShowWeekends,
                                onCheckedChange = { widgetShowWeekends = it },
                                modifier = Modifier.testTag("timetable_meta_weekends_switch")
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val t = titleInput.trim().ifEmpty { "我的每週課表／班表" }
                        val c = cornerInput.trim().ifEmpty { "節次" }
                        onUpdateMeta(t, c)
                        TimetableWidgetSync.setGlobalShowWeekends(context, widgetShowWeekends)
                        showMetaDialog = false
                    }
                ) {
                    Text("儲存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMetaDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 2. Add / Edit Period Row Dialog
    if (showAddPeriodDialog) {
        val current = editingPeriod
        var labelInput by remember(current?.id) {
            mutableStateOf(current?.label ?: "${config.periods.size + 1}")
        }
        val initStart = remember(current?.id) {
            parseHourMinute(current?.startTime ?: "17:10", 17, 10)
        }
        val initEnd = remember(current?.id) {
            parseHourMinute(current?.endTime ?: "18:00", 18, 0)
        }
        var startHour by remember(current?.id) { mutableIntStateOf(initStart.first) }
        var startMinute by remember(current?.id) { mutableIntStateOf(initStart.second) }
        var endHour by remember(current?.id) { mutableIntStateOf(initEnd.first) }
        var endMinute by remember(current?.id) { mutableIntStateOf(initEnd.second) }
        var endManuallyEdited by remember(current?.id) { mutableStateOf(current != null) }
        var editingStartTime by remember(current?.id) { mutableStateOf(true) }

        val startTime = formatHourMinute(startHour, startMinute)
        val endTime = formatHourMinute(endHour, endMinute)

        AlertDialog(
            onDismissRequest = { showAddPeriodDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text(if (current == null) "新增節次／時段列" else "編輯節次／時段") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = labelInput,
                        onValueChange = { if (it.length <= 16) labelInput = it },
                        label = { Text("節次／時段名稱（可用換行）") },
                        placeholder = { Text("例如：1、5 (中午)、早班") },
                        maxLines = 2,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = editingStartTime,
                            onClick = { editingStartTime = true },
                            label = { Text("開始 $startTime") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !editingStartTime,
                            onClick = { editingStartTime = false },
                            label = { Text("結束 $endTime") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    WheelTimePicker(
                        hour = if (editingStartTime) startHour else endHour,
                        minute = if (editingStartTime) startMinute else endMinute,
                        onTimeChange = { h, m ->
                            if (editingStartTime) {
                                startHour = h
                                startMinute = m
                                if (!endManuallyEdited) {
                                    val totalEnd = (h * 60 + m + 50) % (24 * 60)
                                    endHour = totalEnd / 60
                                    endMinute = totalEnd % 60
                                }
                            } else {
                                endManuallyEdited = true
                                endHour = h
                                endMinute = m
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (current != null && config.periods.size > 1) {
                        TextButton(
                            onClick = {
                                onDeletePeriod(current.id)
                                showAddPeriodDialog = false
                            }
                        ) {
                            Text("刪除此列", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Button(
                        onClick = {
                            val lbl = labelInput.trim().ifEmpty { "${config.periods.size + 1}" }
                            onUpsertPeriod(
                                TimetablePeriodRow(
                                    id = current?.id ?: "p_${UUID.randomUUID().toString().take(8)}",
                                    label = lbl,
                                    startTime = startTime,
                                    endTime = endTime
                                )
                            )
                            showAddPeriodDialog = false
                        }
                    ) {
                        Text("儲存")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPeriodDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 3. Edit Grid Cell Dialog
    editingCellTarget?.let { (period, dow) ->
        val existingCell = config.cells.firstOrNull {
            it.periodId == period.id && it.dayOfWeek == dow
        }
        val dayTitle = dayCols.firstOrNull { it.first == dow }?.second ?: "$dow"
        var titleInput by remember(period.id, dow) { mutableStateOf(existingCell?.title.orEmpty()) }
        var noteInput by remember(period.id, dow) { mutableStateOf(existingCell?.note.orEmpty()) }
        var colorId by remember(period.id, dow) {
            mutableStateOf(existingCell?.colorId ?: lastChosenColorId)
        }

        AlertDialog(
            onDismissRequest = { editingCellTarget = null },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text("週$dayTitle ・ ${period.label.replace("\n", " ")} (${period.startTime})")
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { if (it.length <= 36) titleInput = it },
                        label = { Text("課程／班表名稱") },
                        placeholder = { Text("例如：3D建模(二)、早班櫃檯") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("timetable_cell_title_input")
                    )
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { if (it.length <= 36) noteInput = it },
                        label = { Text("教室／備註（選填）") },
                        placeholder = { Text("例如：C405 (每週)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("timetable_cell_note_input")
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "格子底色（可自由配色）",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TimetableCellColor.entries.forEach { palette ->
                                val selected = colorId == palette.id
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(palette.bgArgb))
                                        .border(
                                            width = if (selected) 2.2.dp else 1.dp,
                                            color = if (selected) MaterialTheme.colorScheme.primary else gridLineColor,
                                            shape = CircleShape
                                        )
                                        .clickable { colorId = palette.id }
                                ) {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = palette.labelZh,
                                            tint = Color(palette.fgArgb),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (existingCell != null) {
                        TextButton(
                            onClick = {
                                onClearCell(period.id, dow)
                                editingCellTarget = null
                            }
                        ) {
                            Text("清空此格", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Button(
                        onClick = {
                            val cleanTitle = titleInput.trim()
                            if (cleanTitle.isNotEmpty()) {
                                lastChosenColorId = colorId
                                onSaveCell(
                                    TimetableCellEntry(
                                        periodId = period.id,
                                        dayOfWeek = dow,
                                        title = cleanTitle,
                                        note = noteInput.trim(),
                                        colorId = colorId
                                    ),
                                    period.startTime
                                )
                                editingCellTarget = null
                            }
                        },
                        enabled = titleInput.trim().isNotEmpty(),
                        modifier = Modifier.testTag("confirm_timetable_cell_button")
                    ) {
                        Text("儲存")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCellTarget = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun EmptyRehabCard(
    categoryLabel: String,
    onAddClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Spa,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = "「$categoryLabel」區目前沒有待辦壓力喔！",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "想做點什麼小事的話，可以新增一個超微任務，或是去「療程」隨緣抽一張。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(onClick = onAddClick) {
                Text("新增一件超級小事")
            }
        }
    }
}

