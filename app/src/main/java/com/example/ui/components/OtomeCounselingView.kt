package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.example.R
import com.example.data.DoctorQuotes
import com.example.data.GuestRehabStore
import com.example.data.HeartPrescriptionCard
import com.example.data.OtomeClinicCatalog
import com.example.data.OtomeClinicTheme
import com.example.data.OtomeDoctorExpression
import com.example.data.OtomeTimePeriod
import com.example.data.RehabTask
import kotlinx.coroutines.delay
import java.util.Calendar

private enum class ClinicModal {
    NONE,
    SELF_REPORT,
    HEART_RX,
    SECRET_TREEHOLE,
    OBSERVATION_NOTES,
    THEME_PICKER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtomeCounselingView(
    doctorName: String,
    tasks: List<RehabTask>,
    totalCompleted: Long,
    todayDateKey: String,
    clinicProgress: GuestRehabStore.OtomeClinicProgress,
    clinicTheme: OtomeClinicTheme = OtomeClinicTheme.TREEHOUSE,
    onSelectClinicTheme: (OtomeClinicTheme) -> Unit = {},
    onRecordClinicInteraction: () -> Unit,
    onRecordSecretLetterGiven: () -> Unit,
    onSaveDrawnHeartRx: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val completedTasksCount = remember(tasks, todayDateKey) {
        tasks.count { it.completed && (it.completedDate.isBlank() || it.completedDate == todayDateKey) }
    }
    val totalTasksCount = tasks.size
    val progressRatio = if (totalTasksCount > 0) {
        (completedTasksCount.toFloat() / totalTasksCount.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val progressPercent = (progressRatio * 100f).toInt()

    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val timePeriod = remember(currentHour) { OtomeTimePeriod.fromHour(currentHour) }

    val initialPair = remember(timePeriod, completedTasksCount, totalTasksCount) {
        OtomeClinicCatalog.greetingFor(timePeriod, completedTasksCount, totalTasksCount)
    }

    var expression by remember { mutableStateOf(initialPair.first) }
    var dialogueText by remember { mutableStateOf(initialPair.second) }
    var speakerSubtitle by remember { mutableStateOf(initialPair.first.moodTagZh) }
    var isCloseupMode by remember { mutableStateOf(false) }
    var showTouchHotspots by remember { mutableStateOf(true) }
    var activeModal by remember { mutableStateOf(ClinicModal.NONE) }
    var tapNonce by remember { mutableIntStateOf(0) }
    var bouncing by remember { mutableStateOf(false) }

    var drawnHeartRx by remember(clinicProgress.drawnHeartRxId, clinicProgress.drawnHeartRxDate, todayDateKey) {
        mutableStateOf(
            if (clinicProgress.drawnHeartRxDate == todayDateKey && clinicProgress.drawnHeartRxId.isNotBlank()) {
                OtomeClinicCatalog.heartPrescriptions.firstOrNull { it.id == clinicProgress.drawnHeartRxId }
            } else {
                null
            }
        )
    }

    LaunchedEffect(tapNonce) {
        if (tapNonce > 0) {
            bouncing = true
            delay(160L)
            bouncing = false
        }
    }

    fun triggerDialogue(
        nextExpr: OtomeDoctorExpression,
        nextLine: String,
        subtitleOverride: String? = null,
        countVisit: Boolean = true
    ) {
        expression = nextExpr
        dialogueText = nextLine
        speakerSubtitle = subtitleOverride ?: nextExpr.moodTagZh
        tapNonce += 1
        if (countVisit) onRecordClinicInteraction()
    }

    fun cycleTapReaction() {
        val pool = OtomeClinicCatalog.progressTapLines(completedTasksCount, totalTasksCount, timePeriod)
        val filtered = pool.filter { it.second != dialogueText }
        val (nextExpr, nextLine) = filtered.randomOrNull() ?: pool.first()
        triggerDialogue(nextExpr, nextLine)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "otomeClinicAmbient")
    val breatheY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "otomeBreatheY"
    )
    val hotspotPulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hotspotPulse"
    )

    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val stageHeightDp = (maxWidth * 1.38f).coerceIn(470.dp, 580.dp)
        val stageHeightPx = with(density) { stageHeightDp.toPx() }
        val normalShiftYPx = stageHeightPx * 0.05f
        val closeupShiftYPx = stageHeightPx * 0.08f

        val zoomScale by animateFloatAsState(
            targetValue = if (isCloseupMode) 1.86f else 1.44f,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "otomeZoom"
        )
        val zoomShiftY by animateFloatAsState(
            targetValue = if (isCloseupMode) closeupShiftYPx else normalShiftYPx,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "otomeShiftY"
        )
        val closeupPortraitUpPx by animateFloatAsState(
            targetValue = if (isCloseupMode) -250f else 0f,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "otomeCloseupUpPx"
        )
        val tapScale by animateFloatAsState(
            targetValue = if (bouncing) 1.03f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "otomeTapScale"
        )

        val isLateNight = timePeriod == OtomeTimePeriod.LATE_NIGHT

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Clinic Atmospheric Status & Theme Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isLateNight) Color(0xFF4F3E4D) else MaterialTheme.colorScheme.secondaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isLateNight) Icons.Rounded.NightsStay else Icons.Rounded.WbSunny,
                            contentDescription = null,
                            tint = if (isLateNight) Color(0xFFFFDF9E) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = timePeriod.labelZh,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isLateNight) Color(0xFFFFF4F6) else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MedicalServices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "療程 $completedTasksCount/$totalTasksCount ($progressPercent%)・${clinicProgress.clinicVisits}診",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(clinicTheme.accentHex).copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, Color(clinicTheme.accentHex).copy(alpha = 0.55f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { activeModal = ClinicModal.THEME_PICKER }
                        .testTag("otome_theme_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = "切換診療室佈景主題",
                            tint = Color(clinicTheme.accentHex),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = clinicTheme.titleZh,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Main Otome Visual Novel Stage Card
            Card(
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(stageHeightDp)
                    .testTag("otome_counseling_stage")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                ) {
                    // 1. Themeable Clinic Room Illustration Background
                    AnimatedContent(
                        targetState = clinicTheme,
                        transitionSpec = { fadeIn(tween(340)) togetherWith fadeOut(tween(260)) },
                        label = "otomeClinicThemeBg"
                    ) { theme ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                painter = painterResource(id = theme.drawableRes),
                                contentDescription = "心理診療室背景：${theme.titleZh}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (isLateNight) {
                                            Color(0xFF1F1624).copy(alpha = 0.46f)
                                        } else {
                                            Color(theme.ambientOverlayHex)
                                        }
                                    )
                            )
                        }
                    }

                    // 2. Grounded Half-Body Doctor Portrait (proportionally framed across DPIs)
                    Box(
                        contentAlignment = Alignment.BottomCenter,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 52.dp, bottom = 58.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = !isCloseupMode || !showTouchHotspots,
                                onClick = { cycleTapReaction() }
                            )
                            .testTag("otome_doctor_portrait")
                    ) {
                        AnimatedContent(
                            targetState = expression,
                            transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(200)) },
                            contentAlignment = Alignment.BottomCenter,
                            label = "otomePortraitExpression"
                        ) { expr ->
                            Image(
                                painter = painterResource(id = expr.drawableRes),
                                contentDescription = "${doctorName}心理診療立繪（${expr.moodTagZh}）",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxHeight(0.86f)
                                    .graphicsLayer {
                                        transformOrigin = TransformOrigin(0.5f, 0.14f)
                                        scaleX = zoomScale * tapScale
                                        scaleY = zoomScale * tapScale
                                        translationY = breatheY + zoomShiftY - 250f + closeupPortraitUpPx
                                    }
                            )
                        }

                        // 3. Close-up Touch Hotspots (額頭・胸口・白袍口袋・手腕)
                        if (isCloseupMode) {
                            val foreheadSpot = OtomeClinicCatalog.touchSpots.first { it.id == "forehead" }
                            val chestSpot = OtomeClinicCatalog.touchSpots.first { it.id == "chest_heartbeat" }
                            val pocketSpot = OtomeClinicCatalog.touchSpots.first { it.id == "pocket_snacks" }
                            val wristSpot = OtomeClinicCatalog.touchSpots.first { it.id == "wrist_pulse" }

                            val foreheadY = -(stageHeightDp * 0.26f)
                            val chestY = stageHeightDp * 0.12f
                            val pocketY = (chestY + 28.dp).coerceAtMost(stageHeightDp * 0.305f)
                            val wristY = (chestY + 38.dp).coerceAtMost(stageHeightDp * 0.315f)

                            val triggerForehead = {
                                val next = foreheadSpot.reactionReplies
                                    .filter { it.second != dialogueText }
                                    .randomOrNull() ?: foreheadSpot.reactionReplies.first()
                                triggerDialogue(
                                    next.first,
                                    next.second,
                                    subtitleOverride = "腦袋散熱評估中"
                                )
                            }
                            val triggerChest = {
                                val next = chestSpot.reactionReplies
                                    .filter { it.second != dialogueText }
                                    .randomOrNull() ?: chestSpot.reactionReplies.first()
                                triggerDialogue(
                                    next.first,
                                    next.second,
                                    subtitleOverride = "胸口心跳聽診中"
                                )
                            }
                            val triggerPocket = {
                                val next = pocketSpot.reactionReplies
                                    .filter { it.second != dialogueText }
                                    .randomOrNull() ?: pocketSpot.reactionReplies.first()
                                triggerDialogue(
                                    next.first,
                                    next.second,
                                    subtitleOverride = "物資盤點中"
                                )
                            }
                            val triggerWrist = {
                                val next = wristSpot.reactionReplies
                                    .filter { it.second != dialogueText }
                                    .randomOrNull() ?: wristSpot.reactionReplies.first()
                                triggerDialogue(
                                    next.first,
                                    next.second,
                                    subtitleOverride = "懶癌脈象檢測中"
                                )
                            }

                            if (showTouchHotspots) {
                                CloseupTouchChip(
                                    label = foreheadSpot.labelZh,
                                    pulse = hotspotPulse,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .offset(x = (-2).dp, y = foreheadY)
                                        .testTag("touch_spot_forehead"),
                                    onClick = triggerForehead
                                )
                                CloseupTouchChip(
                                    label = chestSpot.labelZh,
                                    pulse = hotspotPulse,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .offset(x = (-2).dp, y = chestY)
                                        .testTag("touch_spot_chest"),
                                    onClick = triggerChest
                                )
                                CloseupTouchChip(
                                    label = pocketSpot.labelZh,
                                    pulse = hotspotPulse,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .offset(x = 76.dp, y = pocketY)
                                        .testTag("touch_spot_pocket"),
                                    onClick = triggerPocket
                                )
                                CloseupTouchChip(
                                    label = wristSpot.labelZh,
                                    pulse = hotspotPulse,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .offset(x = (-66).dp, y = wristY)
                                        .testTag("touch_spot_wrist"),
                                    onClick = triggerWrist
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .offset(x = (-2).dp, y = foreheadY)
                                        .size(width = 110.dp, height = 74.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = triggerForehead
                                        )
                                        .testTag("touch_spot_forehead_hidden")
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .offset(x = (-2).dp, y = chestY)
                                        .size(width = 116.dp, height = 72.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = triggerChest
                                        )
                                        .testTag("touch_spot_chest_hidden")
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .offset(x = 76.dp, y = pocketY)
                                        .size(width = 104.dp, height = 74.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = triggerPocket
                                        )
                                        .testTag("touch_spot_pocket_hidden")
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .offset(x = (-66).dp, y = wristY)
                                        .size(width = 104.dp, height = 72.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = triggerWrist
                                        )
                                        .testTag("touch_spot_wrist_hidden")
                                )
                            }
                        }
                    }

                // Close-up hotspot visibility toggle chip top-left (like WardRoom anchor toggle)
                androidx.compose.animation.AnimatedVisibility(
                    visible = isCloseupMode,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .zIndex(5f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (showTouchHotspots) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color(0xFFFFF8FA).copy(alpha = 0.94f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (showTouchHotspots) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { showTouchHotspots = !showTouchHotspots }
                            .testTag("otome_hotspot_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (showTouchHotspots) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                contentDescription = if (showTouchHotspots) "隱藏感應區" else "顯示感應區",
                                tint = if (showTouchHotspots) Color.White else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (showTouchHotspots) "隱藏感應區" else "顯示感應區",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (showTouchHotspots) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // 4. Right-Side Vertical Bookmark Menu Bar (右側直排乙女選單欄)
                Column(
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 10.dp)
                        .zIndex(6f)
                ) {
                    VerticalOtomeBookmarkButton(
                        label = "病情\n自述",
                        icon = Icons.Rounded.Forum,
                        isHighlighted = activeModal == ClinicModal.SELF_REPORT,
                        testTag = "otome_menu_self_report",
                        onClick = { activeModal = ClinicModal.SELF_REPORT }
                    )
                    VerticalOtomeBookmarkButton(
                        label = if (isCloseupMode) "恢復\n距離" else "專屬\n問診",
                        icon = if (isCloseupMode) Icons.Rounded.ZoomOut else Icons.Rounded.ZoomIn,
                        isHighlighted = isCloseupMode,
                        testTag = "otome_menu_closeup",
                        onClick = {
                            isCloseupMode = !isCloseupMode
                            if (isCloseupMode) {
                                triggerDialogue(
                                    OtomeDoctorExpression.COOL,
                                    DoctorQuotes.otomeCloseupEnterQuote,
                                    subtitleOverride = "近距離檢查模式",
                                    countVisit = false
                                )
                            } else {
                                triggerDialogue(
                                    if (isLateNight) OtomeDoctorExpression.NIGHT else OtomeDoctorExpression.COOL,
                                    DoctorQuotes.otomeCloseupExitQuote,
                                    countVisit = false
                                )
                            }
                        }
                    )
                    VerticalOtomeBookmarkButton(
                        label = "本日\n醫囑",
                        icon = Icons.AutoMirrored.Rounded.Assignment,
                        isHighlighted = activeModal == ClinicModal.HEART_RX,
                        testTag = "otome_menu_daily_order",
                        onClick = {
                            if (drawnHeartRx == null) {
                                val picked = OtomeClinicCatalog.heartPrescriptions.random()
                                drawnHeartRx = picked
                                onSaveDrawnHeartRx(picked.id)
                                onRecordClinicInteraction()
                            }
                            activeModal = ClinicModal.HEART_RX
                        }
                    )
                    VerticalOtomeBookmarkButton(
                        label = "秘密\n樹洞",
                        icon = Icons.Rounded.MailOutline,
                        isHighlighted = activeModal == ClinicModal.SECRET_TREEHOLE,
                        testTag = "otome_menu_treehole",
                        onClick = { activeModal = ClinicModal.SECRET_TREEHOLE }
                    )
                    VerticalOtomeBookmarkButton(
                        label = "觀察\n筆記",
                        icon = Icons.AutoMirrored.Rounded.MenuBook,
                        isHighlighted = activeModal == ClinicModal.OBSERVATION_NOTES,
                        testTag = "otome_menu_notes",
                        onClick = { activeModal = ClinicModal.OBSERVATION_NOTES }
                    )
                }

                // 5. Otome Translucent Dialogue Box (半透明視覺小說對話欄 + 左上角銘牌)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .zIndex(7f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Color(0xFFFFFAFC).copy(alpha = 0.93f),
                        border = BorderStroke(1.5.dp, Color(0xFFE5B4C2)),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(),
                                onClick = { cycleTapReaction() }
                            )
                            .testTag("otome_dialogue_box")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = dialogueText,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    lineHeight = 23.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color(0xFF4A3134)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = speakerSubtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "▾ 點擊繼續對話",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF9C7A7E)
                                )
                            }
                        }
                    }

                    // Top-left Nameplate Badge
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(clinicTheme.accentHex),
                        border = BorderStroke(1.5.dp, Color.White),
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bookmark,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = doctorName,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
    }

    // Modal 1: 病情自述 (Branching Otome Dialogue Choices)
    if (activeModal == ClinicModal.SELF_REPORT) {
        ModalBottomSheet(
            onDismissRequest = { activeModal = ClinicModal.NONE },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "病情自述・今日症狀回報",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "挑一句最符合你現在狀態的話告訴醫師，他會幫你開立最不費力的官方解釋。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OtomeClinicCatalog.selfReportChoices.forEach { choice ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                activeModal = ClinicModal.NONE
                                val pool = DoctorQuotes.selfReportRepliesByChoiceId[choice.id].orEmpty()
                                val picked = pool.filter { it.second != dialogueText }.randomOrNull()
                                    ?: pool.randomOrNull()
                                    ?: (choice.expression to choice.doctorReply)
                                triggerDialogue(picked.first, picked.second)
                            }
                            .testTag("otome_choice_${choice.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MedicalServices,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = choice.patientPrompt,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal 2: 本日醫囑 (Stamped Daily Doctor Order Card)
    if (activeModal == ClinicModal.HEART_RX) {
        val card = drawnHeartRx ?: OtomeClinicCatalog.heartPrescriptions.first()
        Dialog(onDismissRequest = { activeModal = ClinicModal.NONE }) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8FA)),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (card.isSecretAffection) Color(0xFFFFD8E4) else MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Verified,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = card.stampLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        IconButton(
                            onClick = { activeModal = ClinicModal.NONE },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "關閉",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = card.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = card.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val nextPool = OtomeClinicCatalog.heartPrescriptions.filter { it.id != card.id }
                                val nextCard = nextPool.randomOrNull() ?: card
                                drawnHeartRx = nextCard
                                onSaveDrawnHeartRx(nextCard.id)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("重抽一張")
                        }
                        Button(
                            onClick = {
                                activeModal = ClinicModal.NONE
                                val expr = if (card.isSecretAffection) OtomeDoctorExpression.GENTLE else OtomeDoctorExpression.COOL
                                triggerDialogue(
                                    expr,
                                    DoctorQuotes.otomeHeartRxStampedQuote,
                                    countVisit = false
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("收進病歷夾")
                        }
                    }
                }
            }
        }
    }

    // Modal 3: 秘密樹洞 (Shred vs Give to Doctor's Coat Pocket)
    if (activeModal == ClinicModal.SECRET_TREEHOLE) {
        var secretNote by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { activeModal = ClinicModal.NONE }) {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "秘密樹洞・悄悄寫在病歷背面",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "把委屈、焦慮或說不出口的疲倦寫在紙上。可以親手碎掉，也可以交給他收進白袍口袋。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = secretNote,
                        onValueChange = { if (it.length <= 240) secretNote = it },
                        placeholder = { Text("今天其實有一點點累，或者有什麼煩惱…") },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("treehole_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                activeModal = ClinicModal.NONE
                                triggerDialogue(
                                    OtomeDoctorExpression.SIGH,
                                    OtomeClinicCatalog.secretShredReplies.random(),
                                    subtitleOverride = "碎紙機啟動"
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("treehole_shred_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("碎紙機銷毀", maxLines = 1)
                        }

                        Button(
                            onClick = {
                                activeModal = ClinicModal.NONE
                                onRecordSecretLetterGiven()
                                triggerDialogue(
                                    OtomeDoctorExpression.MUG,
                                    OtomeClinicCatalog.secretPocketReplies.random(),
                                    subtitleOverride = "收進白袍口袋檔案",
                                    countVisit = false
                                )
                            },
                            enabled = secretNote.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("treehole_give_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("交給醫師", maxLines = 1)
                        }
                    }
                }
            }
        }
    }

    // Modal 4: 觀察筆記 (Progressive Doctor Diary / Bond Milestones)
    if (activeModal == ClinicModal.OBSERVATION_NOTES) {
        ModalBottomSheet(
            onDismissRequest = { activeModal = ClinicModal.NONE },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${doctorName}的秘密觀察筆記",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "累計診間互動 ${clinicProgress.clinicVisits} 次・累計完成療程 $totalCompleted 件・口袋來信 ${clinicProgress.secretLettersGiven} 封",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(OtomeClinicCatalog.observationNotes, key = { it.tier }) { entry ->
                        val unlocked = clinicProgress.clinicVisits >= entry.requiredVisits &&
                            totalCompleted >= entry.requiredCompleted
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (unlocked) {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (unlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (unlocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = entry.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = if (unlocked) {
                                        entry.diaryText
                                    } else {
                                        "解鎖條件：診間互動達 ${entry.requiredVisits} 次且累計完成 ${entry.requiredCompleted} 項療程"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (unlocked) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal 5: 診療室背景佈置切換 (Saved locally with full theme & background config)
    if (activeModal == ClinicModal.THEME_PICKER) {
        ModalBottomSheet(
            onDismissRequest = { activeModal = ClinicModal.NONE },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "心理診療室・空間佈置主題",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "挑一間今天最讓你想放空的診間氛圍，切換後會自動儲存在本機設定裡。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OtomeClinicTheme.entries.forEach { theme ->
                    val selected = theme == clinicTheme
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (selected) {
                            Color(theme.accentHex).copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        border = BorderStroke(
                            width = if (selected) 1.8.dp else 1.dp,
                            color = if (selected) Color(theme.accentHex) else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                onSelectClinicTheme(theme)
                                activeModal = ClinicModal.NONE
                                triggerDialogue(
                                    nextExpr = if (theme == OtomeClinicTheme.WARM_STUDY) {
                                        OtomeDoctorExpression.MUG
                                    } else {
                                        OtomeDoctorExpression.GENTLE
                                    },
                                    nextLine = theme.switchDoctorReply,
                                    subtitleOverride = "空間切換：${theme.titleZh}",
                                    countVisit = false
                                )
                            }
                            .testTag("otome_theme_option_${theme.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = theme.drawableRes),
                                contentDescription = theme.titleZh,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 64.dp, height = 78.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = theme.titleZh,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = theme.subtitleZh,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "目前使用中",
                                    tint = Color(theme.accentHex),
                                    modifier = Modifier.size(22.dp)
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
private fun VerticalOtomeBookmarkButton(
    label: String,
    icon: ImageVector,
    isHighlighted: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 10.dp, bottomEnd = 10.dp),
        color = if (isHighlighted) {
            MaterialTheme.colorScheme.primary
        } else {
            Color(0xFFFFF8FA).copy(alpha = 0.92f)
        },
        border = BorderStroke(
            width = 1.2.dp,
            color = if (isHighlighted) Color.White else Color(0xFFDDA5B5)
        ),
        shadowElevation = 3.dp,
        modifier = Modifier
            .width(56.dp)
            .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 10.dp, bottomEnd = 10.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            )
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label.replace("\n", ""),
                tint = if (isHighlighted) Color.White else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center,
                color = if (isHighlighted) Color.White else Color(0xFF5A3B40)
            )
        }
    }
}

@Composable
private fun CloseupTouchChip(
    label: String,
    pulse: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = Color(0xFFFFF5F8).copy(alpha = 0.92f),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
        shadowElevation = 4.dp,
        modifier = modifier
            .graphicsLayer {
                scaleX = pulse
                scaleY = pulse
            }
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
