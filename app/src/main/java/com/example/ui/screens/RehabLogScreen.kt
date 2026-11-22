package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditLocationAlt
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material.icons.rounded.OpenWith
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.CalendarEvent
import com.example.data.DoctorGearConfig
import com.example.data.DoctorPose
import com.example.data.EnergyLevel
import com.example.data.PharmacyItem
import com.example.data.RehabCatalog
import com.example.data.RehabCheckin
import com.example.data.RehabProfile
import com.example.data.RehabTask
import com.example.data.WardDoctorSnapshot
import com.example.data.WardPlacement
import com.example.data.resolvedWardPlacements
import com.example.ui.UiState
import com.example.ui.components.WardRoomView
import com.example.ui.components.WheelTimePicker
import com.example.ui.components.formatHourMinute
import com.example.ui.components.parseHourMinute
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

private enum class DiaryViewMode(val label: String) {
    LIST("列表"),
    CALENDAR("日曆")
}

@Composable
fun RehabLogScreen(
    todayDateKey: String,
    profileState: UiState<RehabProfile?>,
    checkinsState: UiState<List<RehabCheckin>>,
    tasksState: UiState<List<RehabTask>>,
    calendarEvents: List<CalendarEvent> = emptyList(),
    wardDoctorPose: DoctorPose,
    wardDoctorSpeech: String? = null,
    wardSpeechNonce: Int = 0,
    doctorGear: DoctorGearConfig = DoctorGearConfig(),
    isSubmitting: Boolean,
    onWardDoctorPoseChanged: (DoctorPose) -> Unit,
    onWardDoctorSnapshotChanged: (WardDoctorSnapshot) -> Unit = {},
    onToggleWardItem: (PharmacyItem) -> Unit,
    onMoveWardItem: (String, Float, Float) -> Unit,
    onUpsertCalendarEvent: (CalendarEvent) -> Unit = {},
    onDeleteCalendarEvent: (String) -> Unit = {},
    onOpenPharmacy: () -> Unit,
    onSaveCheckin: (EnergyLevel, Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = (profileState as? UiState.Success)?.data
    val doctorNickname = profile?.feifeiNickname?.ifBlank { "主治醫生" } ?: "主治醫生"
    val ownedItemIds = profile?.ownedItemIds.orEmpty()
    val wardPlacements = remember(profile) { profile.resolvedWardPlacements() }

    val checkins = (checkinsState as? UiState.Success)?.data.orEmpty()
    val todayCheckin = checkins.firstOrNull { it.dateKey == todayDateKey }
    val completedTasks = ((tasksState as? UiState.Success)?.data.orEmpty()).filter { it.completed }

    var viewMode by remember { mutableStateOf(DiaryViewMode.LIST) }

    var batteryPercent by remember(todayCheckin?.energyLevel, todayCheckin?.energyPercent) {
        mutableFloatStateOf(
            todayCheckin?.resolvedPercent()?.toFloat()
                ?: energyToDefaultPercent(EnergyLevel.fromWire(todayCheckin?.energyLevel))
        )
    }
    var reflectionText by remember(todayCheckin?.reflection) {
        mutableStateOf(todayCheckin?.reflection.orEmpty())
    }

    LaunchedEffect(todayCheckin) {
        if (todayCheckin != null) {
            batteryPercent = todayCheckin.resolvedPercent().toFloat()
            reflectionText = todayCheckin.reflection
        }
    }

    val currentEnergyLevel = percentToEnergy(batteryPercent)

    // Calendar month state parsed from todayDateKey ("YYYY-MM-DD")
    val initialYearMonth = remember(todayDateKey) {
        val parts = todayDateKey.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 11
        y to m
    }
    var displayYear by remember { mutableIntStateOf(initialYearMonth.first) }
    var displayMonth by remember { mutableIntStateOf(initialYearMonth.second) }
    var selectedCalendarDate by remember { mutableStateOf(todayDateKey) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "view_mode_toggle") {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("diary_mode_toggle")
            ) {
                DiaryViewMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = viewMode == mode,
                        onClick = { viewMode = mode },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = DiaryViewMode.entries.size
                        ),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primary,
                            activeContentColor = MaterialTheme.colorScheme.onPrimary,
                            inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            inactiveContentColor = MaterialTheme.colorScheme.primary,
                            activeBorderColor = MaterialTheme.colorScheme.primary,
                            inactiveBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        ),
                        icon = {
                            Icon(
                                imageVector = if (mode == DiaryViewMode.LIST) {
                                    Icons.Rounded.ViewAgenda
                                } else {
                                    Icons.Rounded.CalendarMonth
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = { Text(mode.label) },
                        modifier = Modifier.testTag("diary_mode_${mode.name.lowercase()}")
                    )
                }
            }
        }

        if (viewMode == DiaryViewMode.LIST) {
            item(key = "ward_decorator_card") {
                WardDecoratorSection(
                    placements = wardPlacements,
                    ownedItemIds = ownedItemIds,
                    doctorNickname = doctorNickname,
                    wardDoctorPose = wardDoctorPose,
                    wardDoctorSpeech = wardDoctorSpeech,
                    wardSpeechNonce = wardSpeechNonce,
                    doctorGear = doctorGear,
                    onMoveWardItem = onMoveWardItem,
                    onToggleWardItem = onToggleWardItem,
                    onWardDoctorPoseChanged = onWardDoctorPoseChanged,
                    onWardDoctorSnapshotChanged = onWardDoctorSnapshotChanged,
                    onOpenPharmacy = onOpenPharmacy
                )
            }

            item(key = "battery_and_blank_diary") {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SlidableBatteryBar(
                            percent = batteryPercent,
                            onPercentChange = { batteryPercent = it }
                        )

                        OutlinedTextField(
                            value = reflectionText,
                            onValueChange = { if (it.length <= 300) reflectionText = it },
                            minLines = 3,
                            maxLines = 6,
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.55f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.45f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("checkin_reflection_input")
                        )

                        Button(
                            onClick = {
                                onSaveCheckin(
                                    currentEnergyLevel,
                                    batteryPercent.roundToInt().coerceIn(0, 100),
                                    reflectionText
                                )
                            },
                            enabled = !isSubmitting,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_checkin_button")
                        ) {
                            Text("儲存")
                        }
                    }
                }
            }

            item(key = "diary_list_header") {
                Text(
                    text = "日記列表",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            when (checkinsState) {
                is UiState.Loading -> {
                    item(key = "loading_checkins") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                is UiState.Error -> {
                    item(key = "error_checkins") {
                        Text(
                            text = "無法讀取日記：${checkinsState.message}",
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                is UiState.Success -> {
                    if (checkins.isEmpty()) {
                        item(key = "empty_checkins") {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = "還沒有記錄，滑動上方電池或留幾行字就能存下今天囉。",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(18.dp)
                                )
                            }
                        }
                    } else {
                        items(checkins, key = { "checkin_${it.id}" }) { entry ->
                            DiaryEntryCard(
                                entry = entry,
                                doctorNickname = doctorNickname,
                                doctorGear = doctorGear,
                                tasksForDay = completedTasks.filter { it.completedDate == entry.dateKey },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // 日曆模式（含 Google Calendar 風格日程）
            item(key = "calendar_card") {
                DiaryCalendarCard(
                    year = displayYear,
                    month = displayMonth,
                    selectedDateKey = selectedCalendarDate,
                    checkins = checkins,
                    completedTasks = completedTasks,
                    calendarEvents = calendarEvents,
                    onPrevMonth = {
                        if (displayMonth == 1) {
                            displayYear -= 1
                            displayMonth = 12
                        } else {
                            displayMonth -= 1
                        }
                    },
                    onNextMonth = {
                        if (displayMonth == 12) {
                            displayYear += 1
                            displayMonth = 1
                        } else {
                            displayMonth += 1
                        }
                    },
                    onSelectDate = { selectedCalendarDate = it },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item(key = "calendar_selected_detail") {
                val dayEntry = checkins.firstOrNull { it.dateKey == selectedCalendarDate }
                val dayTasks = completedTasks.filter { it.completedDate == selectedCalendarDate }
                val dayEvents = calendarEvents.filter { it.dateKey == selectedCalendarDate }
                SelectedCalendarDayDetail(
                    dateKey = selectedCalendarDate,
                    doctorNickname = doctorNickname,
                    doctorGear = doctorGear,
                    entry = dayEntry,
                    tasks = dayTasks,
                    events = dayEvents,
                    onUpsertEvent = onUpsertCalendarEvent,
                    onDeleteEvent = onDeleteCalendarEvent,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun SlidableBatteryBar(
    percent: Float,
    onPercentChange: (Float) -> Unit
) {
    val roundedPercent = percent.roundToInt().coerceIn(0, 100)
    val currentLevel = percentToEnergy(percent)
    val energyLabel = currentLevel.shortTag.substringAfter(' ', currentLevel.shortTag)

    val fillColor = when (currentLevel) {
        EnergyLevel.LEVEL_1, EnergyLevel.LEVEL_2 -> MaterialTheme.colorScheme.secondary
        EnergyLevel.LEVEL_3, EnergyLevel.LEVEL_4, EnergyLevel.LEVEL_5 ->
            MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)
        else -> MaterialTheme.colorScheme.primary
    }
    val trackColor = MaterialTheme.colorScheme.surfaceContainerLow
    val outlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.BatteryChargingFull,
                    contentDescription = null,
                    tint = fillColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "今日電量・$roundedPercent%",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = energyLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // Interactive draggable battery graphic
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val usableWidth = (size.width - 24f).coerceAtLeast(1f)
                        val ratio = ((offset.x - 8f) / usableWidth).coerceIn(0f, 1f)
                        onPercentChange(ratio * 100f)
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        val usableWidth = (size.width - 24f).coerceAtLeast(1f)
                        val ratio = ((change.position.x - 8f) / usableWidth).coerceIn(0f, 1f)
                        onPercentChange(ratio * 100f)
                    }
                }
                .testTag("battery_slider"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val capWidth = 10.dp.toPx()
                val capHeight = 22.dp.toPx()
                val gap = 4.dp.toPx()
                val bodyWidth = size.width - capWidth - gap
                val bodyHeight = size.height - 8.dp.toPx()
                val topY = 4.dp.toPx()
                val radius = 16.dp.toPx()

                // Battery outer background
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(0f, topY),
                    size = Size(bodyWidth, bodyHeight),
                    cornerRadius = CornerRadius(radius, radius)
                )

                // Battery charge fill
                val innerPad = 5.dp.toPx()
                val maxFillWidth = (bodyWidth - innerPad * 2f).coerceAtLeast(0f)
                val fillWidth = maxFillWidth * (roundedPercent / 100f)
                if (fillWidth > 2f) {
                    drawRoundRect(
                        color = fillColor,
                        topLeft = Offset(innerPad, topY + innerPad),
                        size = Size(fillWidth, bodyHeight - innerPad * 2f),
                        cornerRadius = CornerRadius(radius - 4f, radius - 4f)
                    )
                }

                // Battery outline
                drawRoundRect(
                    color = outlineColor,
                    topLeft = Offset(0f, topY),
                    size = Size(bodyWidth, bodyHeight),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Positive terminal nub
                drawRoundRect(
                    color = outlineColor,
                    topLeft = Offset(bodyWidth + gap, (size.height - capHeight) / 2f),
                    size = Size(capWidth, capHeight),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                )
            }
        }

        Text(
            text = "左右滑動電池調整今日電量",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WardDecoratorSection(
    placements: List<WardPlacement>,
    ownedItemIds: List<String>,
    doctorNickname: String,
    wardDoctorPose: DoctorPose,
    wardDoctorSpeech: String?,
    wardSpeechNonce: Int,
    doctorGear: DoctorGearConfig,
    onMoveWardItem: (String, Float, Float) -> Unit,
    onToggleWardItem: (PharmacyItem) -> Unit,
    onWardDoctorPoseChanged: (DoctorPose) -> Unit,
    onWardDoctorSnapshotChanged: (WardDoctorSnapshot) -> Unit,
    onOpenPharmacy: () -> Unit
) {
    val placedIds = remember(placements) { placements.map { it.itemId }.toSet() }
    val ownedItems = remember(ownedItemIds) {
        RehabCatalog.pharmacyItems.filter { it.id in ownedItemIds }
    }
    val itemLookup = remember(ownedItems) { ownedItems.associateBy { it.id } }
    var showAnchors by remember { mutableStateOf(false) }
    var isEditingProps by remember { mutableStateOf(false) }
    var activePlacingItemId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "療癒病房佈置",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${doctorNickname}巡房中・寫日記會同時記錄今天病房的長相",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "已擺 ${placements.size} 件",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (showAnchors) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    border = BorderStroke(
                        1.dp,
                        if (showAnchors) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            showAnchors = !showAnchors
                            if (!showAnchors) activePlacingItemId = null
                        }
                        .testTag("log_ward_anchors_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EditLocationAlt,
                            contentDescription = "顯示擺放錨點",
                            tint = if (showAnchors) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (showAnchors) "隱藏錨點" else "顯示錨點",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (showAnchors) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isEditingProps) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isEditingProps) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            isEditingProps = !isEditingProps
                        }
                        .testTag("log_ward_edit_props_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OpenWith,
                            contentDescription = "切換道具拖曳鎖定",
                            tint = if (isEditingProps) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isEditingProps) "完成編輯道具" else "編輯道具",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isEditingProps) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Edge-to-edge full-screen-width isometric ward view
        WardRoomView(
            placements = placements,
            doctorNickname = doctorNickname,
            isInteractive = true,
            fixedDoctorPose = wardDoctorPose,
            doctorGear = doctorGear,
            isFullBleed = true,
            showAnchors = showAnchors,
            isEditingProps = isEditingProps,
            activePlacingItemId = activePlacingItemId,
            externalSpeechMessage = wardDoctorSpeech,
            externalSpeechNonce = wardSpeechNonce,
            onSelectPlacementAnchor = { anchor ->
                val targetId = activePlacingItemId
                if (targetId != null) {
                    onMoveWardItem(targetId, anchor.xFraction, anchor.yFraction)
                } else {
                    val existing = placements.firstOrNull {
                        RehabCatalog.nearestWardAnchor(it.xFraction, it.yFraction).id == anchor.id
                    }
                    if (existing != null) {
                        activePlacingItemId = existing.itemId
                    } else {
                        val nextUnplaced = ownedItems.firstOrNull { it.id !in placedIds }
                        if (nextUnplaced != null) {
                            onMoveWardItem(nextUnplaced.id, anchor.xFraction, anchor.yFraction)
                            activePlacingItemId = nextUnplaced.id
                        }
                    }
                }
            },
            onMoveItem = onMoveWardItem,
            onDoctorPoseChanged = onWardDoctorPoseChanged,
            onDoctorSnapshotChanged = onWardDoctorSnapshotChanged,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(952f / 840f)
        )

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                if (ownedItems.isEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "還沒有病房擺飾，去藥局領些可愛裝備吧！",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = onOpenPharmacy,
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("ward_go_pharmacy_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocalPharmacy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("免費領道具", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                } else {
                    val selectedName = activePlacingItemId?.let { itemLookup[it]?.shortLabel }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (selectedName != null) {
                                "已選「$selectedName」：點病房任一錨點移動位置，點 ✕ 收回"
                            } else {
                                "病房道具抽屜（點道具選取錨點擺放，點 ✕ 收回）"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ward_item_tray")
                        ) {
                            items(ownedItems, key = { it.id }) { item ->
                                val isPlaced = item.id in placedIds
                                val isSelected = activePlacingItemId == item.id
                                FilterChip(
                                    selected = isPlaced || isSelected,
                                    onClick = {
                                        showAnchors = true
                                        if (!isPlaced) {
                                            val occupiedIds = placements.map {
                                                RehabCatalog.nearestWardAnchor(it.xFraction, it.yFraction).id
                                            }.toSet()
                                            val (dx, dy) = RehabCatalog.defaultWardPosition(item.id)
                                            val defaultAnchor = RehabCatalog.nearestWardAnchor(dx, dy)
                                            val target = if (defaultAnchor.id !in occupiedIds) {
                                                defaultAnchor
                                            } else {
                                                RehabCatalog.wardAnchors.firstOrNull { it.id !in occupiedIds }
                                                    ?: defaultAnchor
                                            }
                                            onMoveWardItem(item.id, target.xFraction, target.yFraction)
                                            activePlacingItemId = item.id
                                        } else {
                                            activePlacingItemId = if (isSelected) null else item.id
                                        }
                                    },
                                    leadingIcon = {
                                        Image(
                                            painter = painterResource(id = item.drawableRes),
                                            contentDescription = null,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = if (isSelected) "📍${item.shortLabel}" else item.shortLabel,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    trailingIcon = if (isPlaced) {
                                        {
                                            IconButton(
                                                onClick = {
                                                    if (activePlacingItemId == item.id) {
                                                        activePlacingItemId = null
                                                    }
                                                    onToggleWardItem(item)
                                                },
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Close,
                                                    contentDescription = "從病房收回 ${item.name}",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        null
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (isSelected) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.primaryContainer
                                        },
                                        selectedLabelColor = if (isSelected) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        }
                                    ),
                                    modifier = Modifier.testTag("ward_tray_chip_${item.id}")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaryEntryCard(
    entry: RehabCheckin,
    doctorNickname: String,
    doctorGear: DoctorGearConfig,
    tasksForDay: List<RehabTask>,
    modifier: Modifier = Modifier
) {
    val level = EnergyLevel.fromWire(entry.energyLevel)
    val snapshotPlacements = remember(entry.wardSnapshot) {
        entry.wardSnapshot.mapNotNull { WardPlacement.decode(it) }
    }
    val hasSnapshot = entry.wardSnapshot.isNotEmpty() || entry.doctorPoseSnapshot.isNotBlank()
    val snapshotDoctor = remember(entry.doctorPoseSnapshot) {
        WardDoctorSnapshot.decode(entry.doctorPoseSnapshot)
    }
    val snapshotPose = remember(entry.doctorPoseSnapshot) {
        DoctorPose.entries.firstOrNull { it.name == entry.doctorPoseSnapshot } ?: DoctorPose.OBSERVE
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.dateKey,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = entry.formattedEnergyTag(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (hasSnapshot) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoCamera,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "當日病房快照",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    WardRoomView(
                        placements = snapshotPlacements,
                        doctorNickname = doctorNickname,
                        isInteractive = false,
                        fixedDoctorPose = snapshotPose,
                        fixedDoctorSnapshot = snapshotDoctor,
                        doctorGear = doctorGear,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(952f / 840f)
                    )
                }
            }

            val completedTitlesForDay = remember(entry.completedTaskTitles, tasksForDay) {
                (entry.completedTaskTitles + tasksForDay.map { it.title })
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
            }

            if (entry.reflection.isNotBlank()) {
                Text(
                    text = entry.reflection,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (completedTitlesForDay.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    completedTitlesForDay.forEach { taskTitle ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = taskTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaryCalendarCard(
    year: Int,
    month: Int,
    selectedDateKey: String,
    checkins: List<RehabCheckin>,
    completedTasks: List<RehabTask>,
    calendarEvents: List<CalendarEvent>,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val checkinDates = remember(checkins) { checkins.map { it.dateKey }.toSet() }
    val taskDates = remember(completedTasks) { completedTasks.map { it.completedDate }.toSet() }
    val eventDates = remember(calendarEvents) { calendarEvents.map { it.dateKey }.toSet() }

    val calendar = remember(year, month) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOffset = calendar.get(Calendar.DAY_OF_WEEK) - 1 // Sunday = 0

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevMonth) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "上個月"
                    )
                }
                Text(
                    text = "${year}年 ${month}月",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onNextMonth) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "下個月"
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("日", "一", "二", "三", "四", "五", "六").forEach { dayName ->
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            val totalCells = firstDayOffset + daysInMonth
            val rows = (totalCells + 6) / 7
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (c in 0 until 7) {
                            val cellIndex = r * 7 + c
                            val dayNum = cellIndex - firstDayOffset + 1
                            if (dayNum in 1..daysInMonth) {
                                val dateKey = String.format(Locale.US, "%04d-%02d-%02d", year, month, dayNum)
                                val isSelected = dateKey == selectedDateKey
                                val hasDiaryOrTask = dateKey in checkinDates || dateKey in taskDates
                                val hasEvent = dateKey in eventDates
                                val hasAny = hasDiaryOrTask || hasEvent

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            when {
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                hasAny -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable { onSelectDate(dateKey) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = dayNum.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected || hasAny) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        if (hasAny) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                if (hasDiaryOrTask) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (isSelected) {
                                                                    MaterialTheme.colorScheme.onPrimary
                                                                } else {
                                                                    MaterialTheme.colorScheme.secondary
                                                                }
                                                            )
                                                    )
                                                }
                                                if (hasEvent) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (isSelected) {
                                                                    MaterialTheme.colorScheme.onPrimary
                                                                } else {
                                                                    MaterialTheme.colorScheme.tertiary
                                                                }
                                                            )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedCalendarDayDetail(
    dateKey: String,
    doctorNickname: String,
    doctorGear: DoctorGearConfig,
    entry: RehabCheckin?,
    tasks: List<RehabTask>,
    events: List<CalendarEvent>,
    onUpsertEvent: (CalendarEvent) -> Unit,
    onDeleteEvent: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val snapshotPlacements = remember(entry?.wardSnapshot) {
        entry?.wardSnapshot.orEmpty().mapNotNull { WardPlacement.decode(it) }
    }
    val hasSnapshot = entry != null &&
        (entry.wardSnapshot.isNotEmpty() || entry.doctorPoseSnapshot.isNotBlank())
    val snapshotDoctor = remember(entry?.doctorPoseSnapshot) {
        WardDoctorSnapshot.decode(entry?.doctorPoseSnapshot)
    }
    val snapshotPose = remember(entry?.doctorPoseSnapshot) {
        DoctorPose.entries.firstOrNull { it.name == entry?.doctorPoseSnapshot } ?: DoctorPose.OBSERVE
    }

    var showEventDialog by remember(dateKey) { mutableStateOf(false) }
    var editingEvent by remember(dateKey) { mutableStateOf<CalendarEvent?>(null) }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = dateKey,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    entry?.let {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = it.formattedEnergyTag(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        editingEvent = null
                        showEventDialog = true
                    },
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("add_calendar_event_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("新增行程", style = MaterialTheme.typography.labelMedium)
                }
            }

            // Google-Calendar-style events for this date
            if (events.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EventNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "行事曆行程（${events.size}）",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    events.forEach { ev ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Text(
                                        text = ev.timeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = ev.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (ev.note.isNotBlank()) {
                                        Text(
                                            text = ev.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        editingEvent = ev
                                        showEventDialog = true
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "編輯行程 ${ev.title}",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteEvent(ev.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "刪除行程 ${ev.title}",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val mergedCompletedTitles = remember(entry?.completedTaskTitles, tasks) {
                (entry?.completedTaskTitles.orEmpty() + tasks.map { it.title })
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
            }

            if (entry == null && mergedCompletedTitles.isEmpty() && events.isEmpty()) {
                Text(
                    text = "這天沒有留下紀錄。需要提醒出門或吃飯的話，可點右上角「新增行程」。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                if (hasSnapshot) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PhotoCamera,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "當日病房快照",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        WardRoomView(
                            placements = snapshotPlacements,
                            doctorNickname = doctorNickname,
                            isInteractive = false,
                            fixedDoctorPose = snapshotPose,
                            fixedDoctorSnapshot = snapshotDoctor,
                            doctorGear = doctorGear,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(952f / 840f)
                        )
                    }
                }

                if (entry != null && entry.reflection.isNotBlank()) {
                    Text(
                        text = entry.reflection,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (mergedCompletedTitles.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        mergedCompletedTitles.forEach { taskTitle ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = taskTitle,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEventDialog) {
        CalendarEventEditorDialog(
            dateKey = dateKey,
            initial = editingEvent,
            onDismiss = { showEventDialog = false },
            onConfirm = { event ->
                onUpsertEvent(event)
                showEventDialog = false
            }
        )
    }
}

@Composable
private fun CalendarEventEditorDialog(
    dateKey: String,
    initial: CalendarEvent?,
    onDismiss: () -> Unit,
    onConfirm: (CalendarEvent) -> Unit
) {
    var isAllDay by remember(initial?.id) { mutableStateOf(initial?.timeText == "全日") }
    val initHm = remember(initial?.id) { parseHourMinute(initial?.timeText ?: "14:00", 14, 0) }
    var eventHour by remember(initial?.id) { mutableIntStateOf(initHm.first) }
    var eventMinute by remember(initial?.id) { mutableIntStateOf(initHm.second) }
    var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var note by remember(initial?.id) { mutableStateOf(initial?.note.orEmpty()) }

    val resolvedTimeText = if (isAllDay) "全日" else formatHourMinute(eventHour, eventMinute)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "$dateKey 新增行程" else "$dateKey 編輯行程",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 60) title = it },
                    label = { Text("行程名稱") },
                    placeholder = { Text("例如：回診領藥、線上會議、取包裹") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calendar_event_title_input")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isAllDay,
                        onClick = { isAllDay = false },
                        label = { Text("指定 ${formatHourMinute(eventHour, eventMinute)}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isAllDay,
                        onClick = { isAllDay = true },
                        label = { Text("全日行程") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
                if (!isAllDay) {
                    WheelTimePicker(
                        hour = eventHour,
                        minute = eventMinute,
                        onTimeChange = { h, m ->
                            eventHour = h
                            eventMinute = m
                        }
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 120) note = it },
                    label = { Text("備忘（選填）") },
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanTitle = title.trim()
                    if (cleanTitle.isNotEmpty()) {
                        onConfirm(
                            CalendarEvent(
                                id = initial?.id ?: UUID.randomUUID().toString(),
                                dateKey = dateKey,
                                timeText = resolvedTimeText,
                                title = cleanTitle,
                                note = note.trim()
                            )
                        )
                    }
                },
                enabled = title.trim().isNotEmpty(),
                modifier = Modifier.testTag("confirm_calendar_event_button")
            ) {
                Text("儲存行程")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun energyToDefaultPercent(level: EnergyLevel): Float = level.percentValue.toFloat()

private fun percentToEnergy(percent: Float): EnergyLevel = when {
    percent <= 10f -> EnergyLevel.LEVEL_1
    percent <= 20f -> EnergyLevel.LEVEL_2
    percent <= 30f -> EnergyLevel.LEVEL_3
    percent <= 40f -> EnergyLevel.LEVEL_4
    percent <= 50f -> EnergyLevel.LEVEL_5
    percent <= 60f -> EnergyLevel.LEVEL_6
    percent <= 70f -> EnergyLevel.LEVEL_7
    percent <= 80f -> EnergyLevel.LEVEL_8
    percent <= 90f -> EnergyLevel.LEVEL_9
    else -> EnergyLevel.LEVEL_10
}
