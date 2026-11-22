package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.SyncProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.EffortLevel
import com.example.data.RehabCatalog
import com.example.data.RehabTask
import com.example.data.SyncConflictInfo
import com.example.data.SyncSaveSummary
import com.example.data.TaskCategory
import com.example.data.TaskCategoryOption
import com.example.data.currentTodayDateKey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private fun offsetDateKey(daysOffset: Int): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, daysOffset)
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
}

private fun dateKeyToUtcMillis(dateKey: String): Long? {
    val clean = dateKey.trim()
    if (!Regex("""^\d{4}-\d{2}-\d{2}$""").matches(clean)) return null
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
        isLenient = false
    }
    return runCatching { fmt.parse(clean)?.time }.getOrNull()
}

private fun utcMillisToDateKey(utcMillis: Long): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return fmt.format(Date(utcMillis))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTaskBottomSheet(
    isSubmitting: Boolean,
    availableCategories: List<TaskCategoryOption> = TaskCategory.buildOptions(emptyList()),
    defaultCategoryWire: String = TaskCategory.DAILY.wireValue,
    onDismiss: () -> Unit,
    onConfirmAdd: (
        title: String,
        effort: EffortLevel,
        note: String,
        categoryWire: String,
        resetHour: Long,
        deadlineDate: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedEffort by remember { mutableStateOf(EffortLevel.MICRO) }
    var selectedCategoryWire by remember(defaultCategoryWire) { mutableStateOf(defaultCategoryWire) }
    var resetHour by remember { mutableLongStateOf(4L) }
    var deadlineDate by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "新增一項復健療程",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "越小越好！哪怕只有「打開書本看一秒」也算康復成功喔。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 100) title = it },
                label = { Text("今天想試著做哪件小事？") },
                placeholder = { Text("例如：喝一口水、把桌上的杯子拿去洗") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_task_title_input")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "歸屬於哪個療程區域？",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableCategories.forEach { category ->
                        FilterChip(
                            selected = selectedCategoryWire == category.wireValue,
                            onClick = { selectedCategoryWire = category.wireValue },
                            label = { Text(category.labelZh) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.testTag("add_task_category_${category.wireValue}")
                        )
                    }
                }
            }

            if (selectedCategoryWire == TaskCategory.GAME.wireValue) {
                GameResetHourSelector(
                    resetHour = resetHour,
                    onResetHourChanged = { resetHour = it }
                )
            }

            if (selectedCategoryWire == TaskCategory.HOMEWORK.wireValue) {
                HomeworkDeadlineSelector(
                    deadlineDate = deadlineDate,
                    onDeadlineChanged = { deadlineDate = it }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val activeCategoryLabel = remember(availableCategories, selectedCategoryWire) {
                    availableCategories.firstOrNull { it.wireValue == selectedCategoryWire }?.labelZh
                        ?: TaskCategory.labelFor(selectedCategoryWire)
                }
                val recommendedTasks = remember(selectedCategoryWire, activeCategoryLabel) {
                    RehabCatalog.roomQuickRecommendations(selectedCategoryWire, activeCategoryLabel)
                }
                Text(
                    text = "康復室「$activeCategoryLabel」推薦（點一下直接帶入）",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    recommendedTasks.forEach { quick ->
                        SuggestionChip(
                            onClick = { title = quick },
                            label = { Text(quick) }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "這件事需要多少力氣？",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EffortLevel.entries.forEach { level ->
                        FilterChip(
                            selected = selectedEffort == level,
                            onClick = { selectedEffort = level },
                            label = { Text(level.labelZh) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= 200) note = it },
                label = { Text("給自己的溫柔備註（選填）") },
                placeholder = { Text("做到一半停下來也沒關係喔") },
                maxLines = 2,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("先不用")
                }
                Button(
                    onClick = {
                        val finalReset = if (selectedCategoryWire == TaskCategory.GAME.wireValue) resetHour else 0L
                        val finalDeadline = if (selectedCategoryWire == TaskCategory.HOMEWORK.wireValue) deadlineDate.trim() else ""
                        onConfirmAdd(title, selectedEffort, note, selectedCategoryWire, finalReset, finalDeadline)
                    },
                    enabled = title.trim().isNotEmpty() && !isSubmitting,
                    modifier = Modifier
                        .weight(1.5f)
                        .height(50.dp)
                        .testTag("confirm_add_task_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (isSubmitting) "加入療程中..." else "加入今日復健")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShrinkTaskDialog(
    task: RehabTask,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirmShrink: (newTitle: String, newNote: String) -> Unit
) {
    var shrunkTitle by remember(task.id) {
        mutableStateOf("先做 1 分鐘：${task.title}".take(100))
    }
    var shrunkNote by remember(task.id) {
        mutableStateOf("幫你把大魔王切小塊了，完成這一步就很棒！")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.AutoFixHigh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
        },
        title = {
            Text(
                text = "把大魔王切得像芝麻一樣小",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "原本的「${task.title}」是不是讓人有點喘不過氣？我們把它縮成毫無壓力的第一步：",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = shrunkTitle,
                    onValueChange = { if (it.length <= 100) shrunkTitle = it },
                    label = { Text("縮小後的第一步") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shrink_task_input")
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RehabCatalog.shrinkSuggestions.forEach { preset ->
                        SuggestionChip(
                            onClick = { shrunkTitle = "$preset（${task.title}）".take(100) },
                            label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmShrink(shrunkTitle, shrunkNote) },
                enabled = shrunkTitle.trim().isNotEmpty() && !isSubmitting,
                modifier = Modifier.testTag("confirm_shrink_button")
            ) {
                Text("變成超微事")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun EditTaskDialog(
    task: RehabTask,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirmEdit: (
        newTitle: String,
        newNote: String,
        effortLevel: EffortLevel,
        resetHour: Long,
        deadlineDate: String
    ) -> Unit
) {
    var editedTitle by remember(task.id) { mutableStateOf(task.title) }
    var editedNote by remember(task.id) { mutableStateOf(task.note) }
    var editedEffort by remember(task.id) { mutableStateOf(EffortLevel.fromWire(task.effortLevel)) }
    var editedResetHour by remember(task.id) { mutableLongStateOf(task.resetHour.coerceIn(0L, 23L)) }
    var editedDeadline by remember(task.id) { mutableStateOf(task.deadlineDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "編輯復健項目",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { if (it.length <= 100) editedTitle = it },
                    label = { Text("項目名稱") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_task_title_input")
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "這件事需要多少力氣？",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        EffortLevel.entries.forEach { level ->
                            FilterChip(
                                selected = editedEffort == level,
                                onClick = { editedEffort = level },
                                label = { Text(level.labelZh) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("edit_task_effort_${level.wireValue}")
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = editedNote,
                    onValueChange = { if (it.length <= 200) editedNote = it },
                    label = { Text("溫柔備註（選填）") },
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_task_note_input")
                )
                if (task.category == TaskCategory.GAME.wireValue) {
                    GameResetHourSelector(
                        resetHour = editedResetHour,
                        onResetHourChanged = { editedResetHour = it }
                    )
                }
                if (task.category == TaskCategory.HOMEWORK.wireValue) {
                    HomeworkDeadlineSelector(
                        deadlineDate = editedDeadline,
                        onDeadlineChanged = { editedDeadline = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmEdit(
                        editedTitle,
                        editedNote,
                        editedEffort,
                        editedResetHour,
                        editedDeadline.trim()
                    )
                },
                enabled = editedTitle.trim().isNotEmpty() && !isSubmitting,
                modifier = Modifier.testTag("confirm_edit_task_button")
            ) {
                Text("儲存修改")
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
private fun GameResetHourSelector(
    resetHour: Long,
    onResetHourChanged: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = String.format(Locale.US, "每日換日重置時間：%02d:00", resetHour),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "設定這一款遊戲每天凌晨幾點刷新日常（過該時間點會自動取消打勾）",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(0L, 3L, 4L, 5L, 6L).forEach { hour ->
                FilterChip(
                    selected = resetHour == hour,
                    onClick = { onResetHourChanged(hour) },
                    label = { Text(String.format(Locale.US, "%02d:00", hour)) }
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onResetHourChanged((resetHour + 23L) % 24L) },
                modifier = Modifier.height(36.dp)
            ) {
                Text("-1 小時")
            }
            OutlinedButton(
                onClick = { onResetHourChanged((resetHour + 1L) % 24L) },
                modifier = Modifier.height(36.dp)
            ) {
                Text("+1 小時")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun HomeworkDeadlineSelector(
    deadlineDate: String,
    onDeadlineChanged: (String) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (deadlineDate.isBlank()) "作業死線（選填）" else "作業死線：$deadlineDate",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { showDatePicker = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("homework_deadline_picker_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (deadlineDate.isBlank()) {
                        "從日曆選擇死線日期"
                    } else {
                        "已選死線：$deadlineDate（點此改期）"
                    }
                )
            }
            if (deadlineDate.isNotBlank()) {
                OutlinedButton(
                    onClick = { onDeadlineChanged("") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("homework_deadline_clear_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "清除死線",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SuggestionChip(
                onClick = { onDeadlineChanged(currentTodayDateKey()) },
                label = { Text("今天") }
            )
            SuggestionChip(
                onClick = { onDeadlineChanged(offsetDateKey(1)) },
                label = { Text("明天") }
            )
            SuggestionChip(
                onClick = { onDeadlineChanged(offsetDateKey(3)) },
                label = { Text("3 天後") }
            )
            SuggestionChip(
                onClick = { onDeadlineChanged(offsetDateKey(7)) },
                label = { Text("7 天後") }
            )
        }
    }

    if (showDatePicker) {
        val initialMillis = remember(deadlineDate) {
            dateKeyToUtcMillis(deadlineDate) ?: dateKeyToUtcMillis(currentTodayDateKey())
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDeadlineChanged(utcMillisToDateKey(millis))
                        }
                        showDatePicker = false
                    },
                    modifier = Modifier.testTag("homework_deadline_picker_confirm")
                ) {
                    Text("確定日期")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsSheet(
    userEmail: String,
    isGuestMode: Boolean,
    currentNickname: String,
    isLinkingGoogle: Boolean = false,
    authErrorMessage: String? = null,
    hasNotificationPermission: Boolean = true,
    onDismiss: () -> Unit,
    onSaveNickname: (String) -> Unit,
    onLinkGoogleAccount: () -> Unit,
    onRequestNotificationPermission: () -> Unit = {},
    onSendTestNotification: () -> Unit = {},
    onSignOut: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var nickname by remember(currentNickname) { mutableStateOf(currentNickname) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "掛號病歷與陪伴設定",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = when {
                    isGuestMode -> "目前身分：遊客模式（紀錄保存於本機裝置）"
                    userEmail.isNotBlank() -> "目前登入帳號：$userEmail"
                    else -> "目前登入帳號：已驗證 Google 帳號"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = nickname,
                onValueChange = { if (it.length <= 30) nickname = it },
                label = { Text("幫你的主治醫生取個名字") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { onSaveNickname(nickname) },
                enabled = nickname.trim().isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("儲存陪伴暱稱")
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (hasNotificationPermission) {
                                "醫師定時通知：已開啟（死線當天・行程前 30 分・準點傳訊）"
                            } else {
                                "醫師定時通知：尚未授權通知權限"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (!hasNotificationPermission) {
                        OutlinedButton(
                            onClick = onRequestNotificationPermission,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("enable_notifications_button")
                        ) {
                            Text("允許醫師傳送提醒通知")
                        }
                    } else {
                        OutlinedButton(
                            onClick = onSendTestNotification,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("test_notification_button")
                        ) {
                            Text("請醫師傳一則測試通知")
                        }
                    }
                }
            }

            if (!authErrorMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = authErrorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (isGuestMode) {
                Button(
                    onClick = onLinkGoogleAccount,
                    enabled = !isLinkingGoogle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("link_google_account_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    if (isLinkingGoogle) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("正在連線 Google 帳號...")
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Login,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("登入 Google 帳號（雲端同步）")
                    }
                }
            }

            OutlinedButton(
                onClick = onSignOut,
                enabled = !isLinkingGoogle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(bottom = 8.dp)
                    .testTag("sign_out_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isGuestMode) "切換回登入畫面" else "登出 Google 帳號")
            }
        }
    }
}

@Composable
fun SyncConflictDialog(
    conflict: SyncConflictInfo,
    isResolving: Boolean,
    errorMessage: String?,
    onChooseLocal: () -> Unit,
    onChooseCloud: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Mandatory user resolution before syncing */ },
        shape = RoundedCornerShape(28.dp),
        icon = {
            Icon(
                imageVector = Icons.Rounded.SyncProblem,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "偵測到本機與雲端存檔差異",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "系統偵測到裝置本機與 Google 雲端的療程紀錄不一致，請選擇要以哪一份存檔為主：",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SyncSaveSummaryCard(
                    title = "本機存檔",
                    icon = Icons.Rounded.PhoneAndroid,
                    summary = conflict.localSummary
                )

                SyncSaveSummaryCard(
                    title = "雲端存檔",
                    icon = Icons.Rounded.CloudDone,
                    summary = conflict.cloudSummary
                )

                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onChooseLocal,
                    enabled = !isResolving,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("conflict_choose_local_button")
                ) {
                    if (isResolving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("同步處理中...")
                    } else {
                        Text("以本機為主（覆寫雲端）")
                    }
                }
                OutlinedButton(
                    onClick = onChooseCloud,
                    enabled = !isResolving,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("conflict_choose_cloud_button")
                ) {
                    Text("以雲端為主（更新本機）")
                }
            }
        }
    )
}

@Composable
private fun SyncSaveSummaryCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    summary: SyncSaveSummary
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "累計完成 ${summary.totalCompleted} 次・療程清單 ${summary.taskCount} 項（已勾選 ${summary.completedTaskCount} 項）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "日記紀錄 ${summary.checkinCount} 筆・最近日期 ${summary.latestDateKey.ifBlank { "無" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
