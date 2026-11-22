package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.DoctorGearConfig
import com.example.data.EnergyLevel
import com.example.data.RehabCatalog
import com.example.data.RehabCheckin
import com.example.data.RehabProfile
import com.example.data.RehabTask
import com.example.data.SpecialCgItem
import com.example.ui.RehabInteractionState
import com.example.ui.UiState
import com.example.ui.components.DoctorInteractionBanner

private data class MemoryDayGroup(
    val dateKey: String,
    val completedTitles: List<String>,
    val checkin: RehabCheckin?
)

private data class RehabMilestone(
    val threshold: Long,
    val title: String,
    val subtitle: String
)

private val MILESTONES = listOf(
    RehabMilestone(1L, "生命跡象確認", "居然真的動了 1 次"),
    RehabMilestone(5L, "懶癌細胞暫退", "累計完成 5 件微療程"),
    RehabMilestone(10L, "臨床康復奇蹟", "累計完成 10 件復健指標"),
    RehabMilestone(25L, "本院終身傳說", "累計完成 25 件（醫師震驚）")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemoriesScreen(
    profileState: UiState<RehabProfile?>,
    tasksState: UiState<List<RehabTask>>,
    checkinsState: UiState<List<RehabCheckin>>,
    interaction: RehabInteractionState,
    doctorGear: DoctorGearConfig = DoctorGearConfig(),
    rehabPoints: Long = 0L,
    unlockedCgIds: Set<String> = emptySet(),
    onUnlockCg: (SpecialCgItem) -> Unit = {},
    onPatDoctor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = (profileState as? UiState.Success)?.data
    val tasks = (tasksState as? UiState.Success)?.data.orEmpty()
    val checkins = (checkinsState as? UiState.Success)?.data.orEmpty()

    val completedTasks = remember(tasks) { tasks.filter { it.completed } }
    val nickname = profile?.feifeiNickname?.ifBlank { "主治醫生" } ?: "主治醫生"

    var previewCg by remember { mutableStateOf<SpecialCgItem?>(null) }
    var isFullscreenCg by remember { mutableStateOf(false) }

    val memoryDays = remember(completedTasks, checkins) {
        val tasksByDate = completedTasks
            .filter { it.completedDate.isNotBlank() }
            .groupBy { it.completedDate }
        val checkinsByDate = checkins.associateBy { it.dateKey }
        val allDates = (tasksByDate.keys + checkinsByDate.keys)
            .distinct()
            .sortedDescending()
        allDates.map { date ->
            val dayCheckin = checkinsByDate[date]
            val dayTitles = (dayCheckin?.completedTaskTitles.orEmpty() + tasksByDate[date].orEmpty().map { it.title })
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
            MemoryDayGroup(
                dateKey = date,
                completedTitles = dayTitles,
                checkin = dayCheckin
            )
        }
    }
    val totalCompleted = maxOf(
        profile?.totalCompleted ?: 0L,
        completedTasks.size.toLong(),
        memoryDays.sumOf { it.completedTitles.size }.toLong()
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "memories_doctor_banner") {
            DoctorInteractionBanner(
                doctorName = nickname,
                pose = interaction.memoryPose,
                quote = interaction.memoryQuote,
                patCounter = interaction.memoryPatCounter,
                doctorGear = doctorGear,
                subtitle = "住院觀察紀錄簿・累計完成 $totalCompleted 項復健・共 ${memoryDays.size} 天存活證明",
                testTagPrefix = "memories",
                onPatDoctor = onPatDoctor
            )
        }

        item(key = "milestone_wall") {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "院內榮譽貼紙牆（只增不減）",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MILESTONES.forEach { milestone ->
                            val unlocked = totalCompleted >= milestone.threshold
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (unlocked) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.7f)
                                },
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (unlocked) {
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (unlocked) Icons.Rounded.Star else Icons.Rounded.LockOpen,
                                        contentDescription = null,
                                        tint = if (unlocked) {
                                            MaterialTheme.colorScheme.secondary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = milestone.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (unlocked) {
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                        Text(
                                            text = milestone.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item(key = "special_cg_gallery") {
            SpecialCgGalleryCard(
                rehabPoints = rehabPoints,
                unlockedCgIds = unlockedCgIds,
                onUnlockCg = onUnlockCg,
                onPreviewCg = {
                    previewCg = it
                    isFullscreenCg = false
                }
            )
        }

        item(key = "memory_postcards_header") {
            Text(
                text = "每日住院觀察病歷",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        if (tasksState is UiState.Loading || checkinsState is UiState.Loading) {
            item(key = "memories_loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (memoryDays.isEmpty()) {
            item(key = "memories_empty") {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "這份病歷夾目前空白得像剛洗完的床單",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "回康復室隨便勾一件喝水或發呆的小事，或去日記寫下今天，本院就會自動幫你蓋章存進這裡。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(memoryDays, key = { it.dateKey }) { day ->
                MemoryPostcard(day = day)
            }
        }
    }

    previewCg?.let { cg ->
        if (!isFullscreenCg) {
            // Stage 1: Compact preview modal card with "+" button to expand to full screen
            Dialog(onDismissRequest = { previewCg = null }) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cg_preview_dialog")
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cg.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = cg.subtitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = { isFullscreenCg = true },
                                    modifier = Modifier.testTag("cg_expand_fullscreen_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ZoomIn,
                                        contentDescription = "全螢幕放大查看",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = { previewCg = null },
                                    modifier = Modifier.testTag("cg_preview_close_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "關閉預覽"
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { isFullscreenCg = true }
                        ) {
                            Image(
                                painter = painterResource(id = cg.drawableRes),
                                contentDescription = cg.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.52f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ZoomIn,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "全螢幕大圖",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Text(
                            text = cg.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Stage 2: Full-screen black zoomable & pannable CG viewer
            var zoomScale by remember(cg.id) { mutableFloatStateOf(1f) }
            var panOffset by remember(cg.id) { mutableStateOf(Offset.Zero) }
            var showHud by remember(cg.id) { mutableStateOf(true) }
            Dialog(
                onDismissRequest = { isFullscreenCg = false },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .pointerInput(cg.id) {
                            detectTapGestures(
                                onTap = { showHud = !showHud },
                                onDoubleTap = {
                                    if (zoomScale > 1.05f) {
                                        zoomScale = 1f
                                        panOffset = Offset.Zero
                                    } else {
                                        zoomScale = 2.4f
                                    }
                                }
                            )
                        }
                        .pointerInput(cg.id) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val nextScale = (zoomScale * zoom).coerceIn(1f, 4f)
                                zoomScale = nextScale
                                panOffset = if (nextScale <= 1.02f) {
                                    Offset.Zero
                                } else {
                                    val maxX = size.width * (nextScale - 1f) * 0.5f
                                    val maxY = size.height * (nextScale - 1f) * 0.5f
                                    Offset(
                                        x = (panOffset.x + pan.x).coerceIn(-maxX, maxX),
                                        y = (panOffset.y + pan.y).coerceIn(-maxY, maxY)
                                    )
                                }
                            }
                        }
                        .testTag("cg_zoom_viewport")
                ) {
                    Image(
                        painter = painterResource(id = cg.drawableRes),
                        contentDescription = cg.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = zoomScale
                                scaleY = zoomScale
                                translationX = panOffset.x
                                translationY = panOffset.y
                            }
                    )

                    AnimatedVisibility(
                        visible = showHud,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.Black.copy(alpha = 0.58f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)) {
                                    Text(
                                        text = cg.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = cg.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFFB5C5)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.62f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.32f))
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (zoomScale > 1.05f) {
                                                zoomScale = 1f
                                                panOffset = Offset.Zero
                                            } else {
                                                zoomScale = 2.4f
                                            }
                                        },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .testTag("cg_zoom_toggle_button")
                                    ) {
                                        Icon(
                                            imageVector = if (zoomScale > 1.05f) Icons.Rounded.ZoomOut else Icons.Rounded.ZoomIn,
                                            contentDescription = if (zoomScale > 1.05f) "還原大小" else "放大查看",
                                            tint = Color.White
                                        )
                                    }
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.62f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.32f))
                                ) {
                                    IconButton(
                                        onClick = { isFullscreenCg = false },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .testTag("cg_close_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "返回小窗預覽",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = showHud,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.68f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = cg.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                                Text(
                                    text = if (zoomScale > 1.05f) {
                                        "雙指縮放或拖曳細節・點一下隱藏文字・點兩下還原"
                                    } else {
                                        "點兩下或雙指縮放畫面・點一下隱藏文字・右上 × 回小窗"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFFC1CE)
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
private fun SpecialCgGalleryCard(
    rehabPoints: Long,
    unlockedCgIds: Set<String>,
    onUnlockCg: (SpecialCgItem) -> Unit,
    onPreviewCg: (SpecialCgItem) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("special_cg_gallery_card")
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
                    Icon(
                        imageVector = Icons.Rounded.PhotoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "特別 CG 紀念圖鑑",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "$rehabPoints 復健點",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "可在「療程 → 復健室」放置領取每小時 60 點數，或陪醫生玩小遊戲累積點數解鎖限定合影。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            RehabCatalog.specialCgItems.forEach { cg ->
                val isUnlocked = cg.id in unlockedCgIds
                val canAfford = rehabPoints >= cg.costPoints

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isUnlocked) {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLowest
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isUnlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .then(if (isUnlocked) Modifier.clickable { onPreviewCg(cg) } else Modifier)
                        .testTag("cg_item_${cg.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(106.dp)
                                .height(68.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = cg.drawableRes),
                                contentDescription = cg.title,
                                contentScale = ContentScale.Crop,
                                alpha = if (isUnlocked) 1f else 0.16f,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (!isUnlocked) {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = "尚未解鎖",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = cg.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isUnlocked) cg.subtitle else "需要 ${cg.costPoints} 復健點數解鎖",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isUnlocked) {
                                Text(
                                    text = "已收藏・點一下開啟預覽",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            } else {
                                Button(
                                    onClick = { onUnlockCg(cg) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.testTag("unlock_cg_button_${cg.id}")
                                ) {
                                    Text(
                                        text = if (canAfford) "花 ${cg.costPoints} 點解鎖" else "還差 ${cg.costPoints - rehabPoints} 點",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryPostcard(
    day: MemoryDayGroup
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = day.dateKey,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                day.checkin?.let { checkinItem ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = checkinItem.formattedEnergyTag(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            day.checkin?.reflection?.takeIf { it.isNotBlank() }?.let { quote ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "「$quote」",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (day.completedTitles.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    day.completedTitles.forEach { taskTitle ->
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
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

