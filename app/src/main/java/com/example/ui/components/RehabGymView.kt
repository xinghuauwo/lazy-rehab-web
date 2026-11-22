package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SportsMma
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.example.R
import com.example.data.DoctorGearConfig
import com.example.data.DoctorQuotes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

private enum class GymSprite(@DrawableRes val drawableRes: Int) {
    IDLE_A(R.drawable.img_doc_idle_1),
    IDLE_B(R.drawable.img_doc_idle_2),
    WALK_A(R.drawable.img_doc_walk_1),
    WALK_MID(R.drawable.img_doc_walk_mid),
    WALK_OPP(R.drawable.img_doc_walk_opp),
    WALK_PASS(R.drawable.img_doc_walk_pass),
    WALK_B(R.drawable.img_doc_walk_2),
    TURN(R.drawable.img_doc_turn)
}

enum class GymMiniGameType(val titleZh: String) {
    RPS("猜拳對決"),
    HIGH_CARD("撲克牌比大小"),
    MASSAGE("肩頸安撫"),
    FIND_OBJECT("限時尋物")
}

private enum class MiniGameOutcome {
    WIN, LOSE, TIE, COMPLETED
}

private enum class RpsHand(val labelZh: String) {
    ROCK("石頭"),
    SCISSORS("剪刀"),
    PAPER("布")
}

private enum class HiddenClinicProp(
    val labelZh: String,
    @DrawableRes val drawableRes: Int
) {
    BOBA_STRAW("珍奶吸管", R.drawable.img_find_boba_straw),
    STETHOSCOPE("備用聽診器", R.drawable.img_find_stethoscope),
    LAZY_STAMP("拖延許可印章", R.drawable.img_find_lazy_stamp),
    MINT_TIN("薄荷糖罐", R.drawable.img_find_mint_tin),
    CAT_PAW_CLIP("貓爪髮夾", R.drawable.img_find_cat_hairclip),
    SLEEP_MASK("熱敷眼罩", R.drawable.img_find_sleep_mask)
}

private val PinkPrimary = Color(0xFFFF6D8A)
private val PinkSoftRibbon = Color(0xFFFFA8BE)
private val PinkStripBg = Color(0xFFFFB5C7)
private val PinkBannerBg = Color(0xFFFFF4F7)
private val ClinicInk = Color(0xFF3E292E)

@Composable
fun RehabGymView(
    doctorName: String,
    rehabPoints: Long,
    doctorGear: DoctorGearConfig,
    specialChannelUnlocked: Boolean = false,
    hasSeenIntro: Boolean = true,
    onMarkIntroSeen: () -> Unit = {},
    onUnlockSpecialChannel: () -> Unit = {},
    onLockSpecialChannel: () -> Unit = {},
    onAddPoints: (Long) -> Unit,
    onSetRehabPoints: (Long) -> Unit = {},
    onAccrueIdlePoints: () -> Unit,
    topControlsInset: Dp = 12.dp,
    modifier: Modifier = Modifier
) {
    // Periodic idle tick accrual every 30s while in Rehab Gym
    LaunchedEffect(Unit) {
        onAccrueIdlePoints()
        while (true) {
            delay(30_000L)
            onAccrueIdlePoints()
        }
    }

    val scope = rememberCoroutineScope()
    val docBounceScale = remember { Animatable(1f) }

    var docX by remember { mutableFloatStateOf(0.52f) }
    var docY by remember { mutableFloatStateOf(0.76f) }
    var facingRight by remember { mutableStateOf(false) }
    var isWalking by remember { mutableStateOf(false) }
    var isTurning by remember { mutableStateOf(false) }
    var walkDurationMs by remember { mutableIntStateOf(1600) }
    var currentSprite by remember { mutableStateOf(GymSprite.IDLE_A) }
    var speechLine by remember { mutableStateOf(DoctorQuotes.gymWalkLines.first()) }
    var exclamationActive by remember { mutableStateOf(false) }
    var activeGame by remember { mutableStateOf<GymMiniGameType?>(null) }
    val gameBag = remember { mutableStateListOf<GymMiniGameType>() }
    var lastRandomGame by remember { mutableStateOf<GymMiniGameType?>(null) }
    var walkPhase by remember { mutableIntStateOf(0) }
    var showSpecialChannelDialog by remember { mutableStateOf(false) }
    var showIntroDialog by remember(hasSeenIntro) { mutableStateOf(!hasSeenIntro) }

    // Exclamation "!" spawns only every 20~60 seconds while no mini-game is open
    LaunchedEffect(activeGame != null, exclamationActive) {
        if (activeGame == null && !exclamationActive) {
            delay((20_000L..60_000L).random())
            exclamationActive = true
        }
    }

    // Autonomous floor patrol
    LaunchedEffect(activeGame != null) {
        if (activeGame != null) return@LaunchedEffect

        launch {
            var tick = 0
            while (true) {
                if (isTurning) {
                    currentSprite = GymSprite.TURN
                    delay(120L)
                } else if (isWalking) {
                    val pattern = listOf(
                        GymSprite.WALK_A,
                        GymSprite.WALK_MID,
                        GymSprite.WALK_OPP,
                        GymSprite.WALK_PASS
                    )
                    walkPhase = (walkPhase + 1) % pattern.size
                    currentSprite = pattern[walkPhase]
                    delay(135L)
                } else {
                    tick += 1
                    currentSprite = if (tick % 8 == 0) GymSprite.IDLE_B else GymSprite.IDLE_A
                    delay(if (tick % 8 == 0) 400L else 360L)
                }
            }
        }

        val floorSpots = listOf(
            0.24f to 0.78f,
            0.42f to 0.71f,
            0.58f to 0.80f,
            0.74f to 0.73f,
            0.48f to 0.66f,
            0.66f to 0.69f
        )
        var step = 0
        while (true) {
            delay(2800L)
            step += 1
            val candidates = floorSpots.filter { (sx, sy) ->
                abs(sx - docX) + abs(sy - docY) > 0.08f
            }
            val (nextX, nextY) = candidates.randomOrNull() ?: floorSpots[step % floorSpots.size]
            val dx = nextX - docX
            val wantsRight = if (abs(dx) > 0.015f) dx > 0f else facingRight
            if (wantsRight != facingRight) {
                isTurning = true
                delay(220L)
                facingRight = wantsRight
                isTurning = false
            }

            val dist = abs(nextX - docX) + abs(nextY - docY)
            walkDurationMs = (dist * 4500f).roundToInt().coerceIn(1000, 2100)
            isWalking = true
            docX = nextX
            docY = nextY
            delay(walkDurationMs.toLong() + 90L)
            isWalking = false

            if (step % 3 == 0) {
                speechLine = DoctorQuotes.gymWalkLines.filter { it != speechLine }.randomOrNull()
                    ?: DoctorQuotes.gymWalkLines.first()
            }
        }
    }

    val animX by animateFloatAsState(
        targetValue = docX,
        animationSpec = tween(durationMillis = walkDurationMs, easing = LinearEasing),
        label = "gym_doc_x"
    )
    val animY by animateFloatAsState(
        targetValue = docY,
        animationSpec = tween(durationMillis = walkDurationMs, easing = LinearEasing),
        label = "gym_doc_y"
    )

    val gymWalkBobTarget = if (isWalking && walkPhase % 2 == 1) -2.5f else 0f
    val animGymWalkBob by animateFloatAsState(
        targetValue = gymWalkBobTarget,
        animationSpec = tween(durationMillis = 100),
        label = "gym_walk_bob"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "gym_alert")
    val alertBob by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alert_bob"
    )

    fun launchNextGame(specific: GymMiniGameType? = null) {
        val chosen = if (specific != null) {
            specific
        } else {
            if (gameBag.isEmpty()) {
                val refilled = GymMiniGameType.entries.shuffled().toMutableList()
                if (lastRandomGame != null && refilled.size > 1 && refilled.first() == lastRandomGame) {
                    val swapIdx = 1 + Random.nextInt(refilled.size - 1)
                    val first = refilled[0]
                    refilled[0] = refilled[swapIdx]
                    refilled[swapIdx] = first
                }
                gameBag.addAll(refilled)
            }
            val picked = gameBag.removeAt(0)
            lastRandomGame = picked
            picked
        }
        if (specific == null) {
            exclamationActive = false
        }
        activeGame = chosen
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("rehab_gym_stage")
    ) {
        val density = LocalDensity.current
        val roomWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val roomHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

        // Full-screen room illustration
        Image(
            painter = painterResource(id = R.drawable.img_rehab_gym_bg),
            contentDescription = "復健室背景",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Top compact HUD overlay (Points pill + ≡ Special Channel button)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = topControlsInset, start = 14.dp, end = 14.dp)
                .zIndex(15f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFFFFF8FA).copy(alpha = 0.92f),
                border = BorderStroke(1.2.dp, PinkPrimary.copy(alpha = 0.55f)),
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "復健點數 $rehabPoints",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = ClinicInk
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = PinkPrimary.copy(alpha = 0.16f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = PinkPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "+60/hr",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PinkPrimary
                            )
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFF8FA).copy(alpha = 0.92f),
                    border = BorderStroke(1.2.dp, PinkPrimary.copy(alpha = 0.55f)),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showIntroDialog = true }
                        .testTag("rehab_gym_info_button")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = "復健室玩法說明",
                            tint = PinkPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF3E3630).copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showSpecialChannelDialog = true }
                        .testTag("rehab_special_menu_button")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Menu,
                            contentDescription = "特別通道選單",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        val docWidthDp = 64.dp
        val docHeightDp = 110.dp
        val docWidthPx = with(density) { docWidthDp.toPx() }
        val docHeightPx = with(density) { docHeightDp.toPx() }
        val topFloorBound = (with(density) { (topControlsInset + 72.dp).toPx() }).coerceAtLeast(docHeightPx + 12f)
        val centerX = (animX * roomWidthPx).coerceIn(docWidthPx / 2f + 12f, roomWidthPx - docWidthPx / 2f - 12f)
        val footY = (animY * roomHeightPx).coerceIn(topFloorBound, (roomHeightPx - 98f).coerceAtLeast(topFloorBound))
        val offsetX = centerX - docWidthPx / 2f
        val offsetY = footY - docHeightPx

        // Floor shadow scaled cleanly for 0.5x chibi sprite
        Canvas(
            modifier = Modifier
                .zIndex(2f)
                .width(46.dp)
                .height(9.dp)
                .offset {
                    IntOffset(
                        x = (centerX - with(density) { 23.dp.toPx() }).roundToInt(),
                        y = (footY - with(density) { 4.5.dp.toPx() }).roundToInt()
                    )
                }
        ) {
            drawOval(
                color = Color(0xFF432E2B).copy(alpha = 0.26f),
                topLeft = Offset.Zero,
                size = Size(size.width, size.height)
            )
        }

        // Dynamically measured speech bubble above doctor (grows upward cleanly for multi-line quotes)
        if (!exclamationActive) {
            var bubbleW by remember { mutableIntStateOf(0) }
            var bubbleH by remember { mutableIntStateOf(0) }
            val fallbackBubbleWPx = with(density) { 196.dp.toPx() }
            val fallbackBubbleHPx = with(density) { 46.dp.toPx() }
            val measuredW = if (bubbleW > 0) bubbleW.toFloat() else fallbackBubbleWPx
            val measuredH = if (bubbleH > 0) bubbleH.toFloat() else fallbackBubbleHPx
            val topHudClearPx = with(density) { (topControlsInset + 48.dp).toPx() }
            val bubbleGapPx = with(density) { 8.dp.toPx() }
            val bubbleX = (centerX - measuredW / 2f)
                .coerceIn(10f, (roomWidthPx - measuredW - 10f).coerceAtLeast(10f))
            val bubbleY = (offsetY - measuredH - bubbleGapPx)
                .coerceIn(topHudClearPx, (roomHeightPx - measuredH - 12f).coerceAtLeast(topHudClearPx))
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier
                    .zIndex(8f)
                    .widthIn(min = 100.dp, max = 250.dp)
                    .onSizeChanged { size ->
                        bubbleW = size.width
                        bubbleH = size.height
                    }
                    .offset { IntOffset(bubbleX.roundToInt(), bubbleY.roundToInt()) }
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, PinkPrimary.copy(alpha = 0.4f)),
                    shadowElevation = 3.dp
                ) {
                    Text(
                        text = speechLine,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        ),
                        color = ClinicInk,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Pink circular exclamation badge above walking doctor
        if (exclamationActive) {
            val badgeDp = 40.dp
            val badgePx = with(density) { badgeDp.toPx() }
            val badgeX = (centerX - badgePx / 2f).coerceIn(8f, roomWidthPx - badgePx - 8f)
            val badgeY = (offsetY - with(density) { 46.dp.toPx() }).coerceAtLeast(8f)
            Surface(
                shape = CircleShape,
                color = PinkPrimary,
                border = BorderStroke(2.dp, Color.White),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .zIndex(12f)
                    .size(badgeDp)
                    .offset { IntOffset(badgeX.roundToInt(), badgeY.roundToInt()) }
                    .graphicsLayer { translationY = alertBob }
                    .clip(CircleShape)
                    .clickable { launchNextGame() }
                    .testTag("gym_exclamation_bubble")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.PriorityHigh,
                        contentDescription = "復健室驚嘆號事件",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Walking Doctor Avatar (2x enlarged + tactile squash-and-stretch spring bounce on poke)
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier
                .zIndex(5f)
                .width(docWidthDp)
                .height(docHeightDp)
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (exclamationActive) {
                        launchNextGame()
                    } else {
                        speechLine = DoctorQuotes.allDoctorQuotes.random()
                        scope.launch {
                            docBounceScale.snapTo(0.84f)
                            docBounceScale.animateTo(
                                targetValue = 1.09f,
                                animationSpec = spring(dampingRatio = 0.42f, stiffness = 560f)
                            )
                            docBounceScale.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f)
                            )
                        }
                    }
                }
                .testTag("gym_walking_doctor")
        ) {
            val isSide = currentSprite == GymSprite.WALK_A ||
                currentSprite == GymSprite.WALK_MID ||
                currentSprite == GymSprite.WALK_OPP ||
                currentSprite == GymSprite.WALK_PASS ||
                currentSprite == GymSprite.WALK_B ||
                currentSprite == GymSprite.TURN
            val stretchY = docBounceScale.value
            val squashX = 2f - stretchY
            DoctorAvatarWithGear(
                drawableRes = currentSprite.drawableRes,
                gear = doctorGear,
                isSideProfile = isSide,
                contentDescription = "${doctorName}在復健室散步",
                modifier = Modifier
                    .height(docHeightDp)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0.5f, 1f)
                        translationY = with(density) { animGymWalkBob.dp.toPx() }
                        scaleX = (if (facingRight) -1f else 1f) * squashX
                        scaleY = stretchY
                    }
            )
        }
    }

    if (showIntroDialog) {
        AlertDialog(
            onDismissRequest = {
                showIntroDialog = false
                onMarkIntroSeen()
            },
            title = {
                Text(
                    text = DoctorQuotes.gymFirstVisitTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = DoctorQuotes.gymFirstVisitBody,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showIntroDialog = false
                        onMarkIntroSeen()
                    },
                    modifier = Modifier.testTag("rehab_gym_intro_confirm")
                ) {
                    Text("我知道了，開始復健")
                }
            }
        )
    }

    if (showSpecialChannelDialog) {
        SpecialChannelDialog(
            rehabPoints = rehabPoints,
            initialUnlocked = specialChannelUnlocked,
            onUnlockSpecialChannel = onUnlockSpecialChannel,
            onLockSpecialChannel = onLockSpecialChannel,
            onSetRehabPoints = onSetRehabPoints,
            onLaunchMiniGame = { chosen ->
                showSpecialChannelDialog = false
                launchNextGame(chosen)
            },
            onDismiss = { showSpecialChannelDialog = false }
        )
    }

    activeGame?.let { gameType ->
        RehabMiniGameOverlay(
            doctorName = doctorName,
            gameType = gameType,
            onRewardPoints = onAddPoints,
            onDismiss = { activeGame = null }
        )
    }
}

@Composable
private fun SpecialChannelDialog(
    rehabPoints: Long,
    initialUnlocked: Boolean,
    onUnlockSpecialChannel: () -> Unit,
    onLockSpecialChannel: () -> Unit,
    onSetRehabPoints: (Long) -> Unit,
    onLaunchMiniGame: (GymMiniGameType) -> Unit,
    onDismiss: () -> Unit
) {
    var codeInput by remember { mutableStateOf("") }
    var unlocked by remember(initialUnlocked) { mutableStateOf(initialUnlocked) }
    var pointsInput by remember(rehabPoints) { mutableStateOf(rehabPoints.toString()) }
    var statusText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "特別通道",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "目前復健點數：$rehabPoints 點",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!unlocked) {
                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { raw ->
                            val clean = raw.filter { it.isDigit() }.take(8)
                            codeInput = clean
                            if (clean == "9487") {
                                unlocked = true
                                onUnlockSpecialChannel()
                                statusText = "已開啟特別通道，關閉前都不需重複輸入代碼。"
                            }
                        },
                        label = { Text("輸入特別代碼") },
                        placeholder = { Text("輸入代碼即可解鎖（可隨時上鎖關閉）") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cheat_code_input")
                    )
                } else {
                    Text(
                        text = "復健點數直接調整",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    OutlinedTextField(
                        value = pointsInput,
                        onValueChange = { raw ->
                            pointsInput = raw.filter { it.isDigit() }.take(7)
                        },
                        label = { Text("設定復健點數") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cheat_points_input")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val next = (rehabPoints + 500L).coerceAtMost(9_999_999L)
                                pointsInput = next.toString()
                                onSetRehabPoints(next)
                                statusText = "已加發 +500 復健點（目前 $next 點）。"
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+500 點")
                        }
                        Button(
                            onClick = {
                                val parsed = pointsInput.toLongOrNull() ?: rehabPoints
                                onSetRehabPoints(parsed)
                                statusText = "已將復健點數設為 $parsed 點。"
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("cheat_apply_points_button")
                        ) {
                            Text("套用數值")
                        }
                    }

                    HorizontalDivider()

                    Text(
                        text = "指定開啟復健室小遊戲",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        GymMiniGameType.entries.forEach { game ->
                            val icon = when (game) {
                                GymMiniGameType.RPS -> Icons.Rounded.SportsMma
                                GymMiniGameType.HIGH_CARD -> Icons.Rounded.Style
                                GymMiniGameType.MASSAGE -> Icons.Rounded.Favorite
                                GymMiniGameType.FIND_OBJECT -> Icons.Rounded.Search
                            }
                            OutlinedButton(
                                onClick = { onLaunchMiniGame(game) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("special_launch_${game.name.lowercase()}")
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(game.titleZh)
                            }
                        }
                    }

                    HorizontalDivider()

                    OutlinedButton(
                        onClick = {
                            unlocked = false
                            codeInput = ""
                            statusText = "已關閉特別通道，下次開啟需重新輸入代碼。"
                            onLockSpecialChannel()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("special_lock_channel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("關閉／上鎖特別通道")
                    }
                }

                if (statusText.isNotBlank()) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("關閉")
            }
        }
    )
}

@Composable
private fun RehabMiniGameOverlay(
    doctorName: String,
    gameType: GymMiniGameType,
    onRewardPoints: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var settledReward by remember(gameType) { mutableStateOf(false) }
    var isRoundFinished by remember(gameType) { mutableStateOf(false) }
    var roundOutcome by remember(gameType) { mutableStateOf<MiniGameOutcome?>(null) }

    // Find Object: spawn ONLY the single target prop at a widely randomized coordinate in the room
    val targetProp = remember(gameType) { HiddenClinicProp.entries.random() }
    val targetXFraction = remember(gameType) { Random.nextFloat() * 0.72f + 0.14f }
    val targetYFraction = remember(gameType) { Random.nextFloat() * 0.56f + 0.28f }
    var secondsLeft by remember(gameType) { mutableIntStateOf(15) }

    // Pure doctor character dialogue (NO rules or +60 points!)
    var dialogueText by remember(gameType) {
        mutableStateOf(
            when (gameType) {
                GymMiniGameType.RPS -> DoctorQuotes.gymRpsStartQuote
                GymMiniGameType.HIGH_CARD -> DoctorQuotes.gymHighCardStartQuote
                GymMiniGameType.MASSAGE -> DoctorQuotes.gymMassageStartQuote
                GymMiniGameType.FIND_OBJECT -> DoctorQuotes.gymFindObjectStartQuote(targetProp.labelZh)
            }
        )
    }

    fun awardOnce(amount: Long = 60L) {
        if (!settledReward) {
            settledReward = true
            onRewardPoints(amount)
        }
    }

    // RPS state
    var playerRpsChoice by remember(gameType) { mutableStateOf<RpsHand?>(null) }
    var doctorRpsChoice by remember(gameType) { mutableStateOf<RpsHand?>(null) }

    // High Card state: deal 2 distinct random cards upfront so player may tap either Left or Right card
    val dealtPair = remember(gameType) { (1..13).shuffled().take(2) }
    val leftCardRank = dealtPair[0]
    val rightCardRank = dealtPair[1]
    var cardsRevealed by remember(gameType) { mutableStateOf(false) }
    var playerPickedLeft by remember(gameType) { mutableStateOf<Boolean?>(null) }
    var playerCardRank by remember(gameType) { mutableIntStateOf(leftCardRank) }
    var doctorCardRank by remember(gameType) { mutableIntStateOf(rightCardRank) }

    // Massage swipe state (~5s continuous rubbing)
    var rubProgressMs by remember(gameType) { mutableFloatStateOf(0f) }
    var lastDragTimeMs by remember(gameType) { mutableLongStateOf(0L) }
    var lastTouchPos by remember(gameType) { mutableStateOf<Offset?>(null) }
    var isActivelyRubbing by remember(gameType) { mutableStateOf(false) }
    val rubRatio = (rubProgressMs / 5000f).coerceIn(0f, 1f)
    val animatedRubRatio by animateFloatAsState(
        targetValue = rubRatio,
        animationSpec = tween(durationMillis = 80),
        label = "massageProgress"
    )

    @DrawableRes
    val doctorPoseDrawable = when {
        gameType == GymMiniGameType.MASSAGE && isRoundFinished -> R.drawable.img_rehab_doc_look_away
        gameType == GymMiniGameType.MASSAGE && isActivelyRubbing -> R.drawable.img_rehab_doc_eyes_closed
        else -> when (roundOutcome) {
            MiniGameOutcome.WIN -> R.drawable.img_rehab_doc_thumbs_up
            MiniGameOutcome.TIE -> R.drawable.img_rehab_doc_think
            MiniGameOutcome.LOSE -> R.drawable.img_rehab_doc_arms_crossed
            else -> R.drawable.img_rehab_doc_full
        }
    }

    if (gameType == GymMiniGameType.FIND_OBJECT) {
        LaunchedEffect(gameType, isRoundFinished) {
            while (secondsLeft > 0 && !isRoundFinished) {
                delay(1000L)
                secondsLeft -= 1
            }
            if (secondsLeft <= 0 && !isRoundFinished) {
                isRoundFinished = true
                roundOutcome = MiniGameOutcome.LOSE
                dialogueText = DoctorQuotes.findObjLoseQuotes.random()
            }
        }
    }

    // Top system banner text (Rules, countdown, and +60 points ONLY in top system banner)
    val systemBannerText = when (gameType) {
        GymMiniGameType.RPS -> when (roundOutcome) {
            MiniGameOutcome.WIN -> "勝利！復健點數 +60 點已入帳！"
            MiniGameOutcome.TIE -> "平手！本回合不計輸贏（+0 點）"
            MiniGameOutcome.LOSE -> "落敗！一局定勝負（+0 點）"
            else -> "來猜拳吧！一局定勝負（勝 +60 復健點，平手不計）"
        }
        GymMiniGameType.HIGH_CARD -> when (roundOutcome) {
            MiniGameOutcome.WIN -> "你的「${cardRankLabel(playerCardRank)}」大於「${cardRankLabel(doctorCardRank)}」！獲得 +60 點！"
            MiniGameOutcome.LOSE -> "你的「${cardRankLabel(playerCardRank)}」小於「${cardRankLabel(doctorCardRank)}」（+0 點）"
            else -> "撲克比大小！任選左右一張翻牌（勝 +60 復健點）"
        }
        GymMiniGameType.FIND_OBJECT -> when (roundOutcome) {
            MiniGameOutcome.WIN -> "找到了「${targetProp.labelZh}」！復健點數 +60 點！"
            MiniGameOutcome.LOSE -> "時間到！沒能找到「${targetProp.labelZh}」（+0 點）"
            else -> "限時尋物：在房間找出「${targetProp.labelZh}」（剩 ${secondsLeft}s）"
        }
        GymMiniGameType.MASSAGE -> if (isRoundFinished) {
            "安撫完成！復健點數 +60 點已入帳！"
        } else {
            "用手指滑動安撫他！（+60 復健點）"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF281D20))
                .then(
                    if (isRoundFinished) {
                        Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onDismiss() }
                            .testTag("mini_game_finish_dismiss_area")
                    } else if (gameType == GymMiniGameType.MASSAGE) {
                        Modifier.pointerInput(gameType, isRoundFinished) {
                            detectDragGestures(
                                onDragStart = { startPos ->
                                    isActivelyRubbing = true
                                    lastDragTimeMs = System.currentTimeMillis()
                                    lastTouchPos = startPos
                                },
                                onDragEnd = {
                                    isActivelyRubbing = false
                                    lastDragTimeMs = 0L
                                    lastTouchPos = null
                                },
                                onDragCancel = {
                                    isActivelyRubbing = false
                                    lastDragTimeMs = 0L
                                    lastTouchPos = null
                                }
                            ) { change, _ ->
                                val moveDist = change.positionChange().getDistance()
                                change.consume()
                                lastTouchPos = change.position
                                if (moveDist >= 2f && !isRoundFinished) {
                                    val nowMs = System.currentTimeMillis()
                                    val elapsed = if (lastDragTimeMs > 0L) {
                                        (nowMs - lastDragTimeMs).coerceIn(10L, 70L)
                                    } else {
                                        16L
                                    }
                                    lastDragTimeMs = nowMs
                                    val nextMs = (rubProgressMs + elapsed.toFloat()).coerceAtMost(5000f)
                                    rubProgressMs = nextMs
                                    val pct = (nextMs / 5000f)
                                    if (pct in 0.35f..0.40f) {
                                        dialogueText = DoctorQuotes.gymMassageMidEarlyQuote
                                    } else if (pct in 0.70f..0.76f) {
                                        dialogueText = DoctorQuotes.gymMassageMidLateQuote
                                    }
                                    if (nextMs >= 5000f && !isRoundFinished) {
                                        isActivelyRubbing = false
                                        isRoundFinished = true
                                        roundOutcome = MiniGameOutcome.COMPLETED
                                        awardOnce(60L)
                                        dialogueText = DoctorQuotes.massageCompleteQuotes.random()
                                        lastTouchPos = null
                                    }
                                }
                            }
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            val density = LocalDensity.current
            val screenW = with(density) { maxWidth.toPx() }
            val screenH = with(density) { maxHeight.toPx() }

            // 1. Full-bleed Gym Background
            Image(
                painter = painterResource(id = R.drawable.img_rehab_gym_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF311B21).copy(alpha = 0.22f),
                                Color.Transparent,
                                Color(0xFF28161B).copy(alpha = 0.32f)
                            )
                        )
                    )
            )

            // 2. Full-height Standing Doctor Sprite reaching bottom edge (hidden in Find Object!)
            if (gameType != GymMiniGameType.FIND_OBJECT) {
                Crossfade(
                    targetState = doctorPoseDrawable,
                    animationSpec = tween(durationMillis = 220),
                    label = "rehabMiniGamePose",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.84f)
                ) { poseDrawable ->
                    Image(
                        painter = painterResource(id = poseDrawable),
                        contentDescription = "${doctorName}復健室全身立繪",
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.BottomCenter,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 2b. Find Object: ONLY the single target prop appears at a truly random coordinate in the room
            if (gameType == GymMiniGameType.FIND_OBJECT) {
                val propSizeDp = 74.dp
                val propPx = with(density) { propSizeDp.toPx() }
                val px = (targetXFraction * screenW - propPx / 2f).coerceIn(14f, screenW - propPx - 14f)
                val py = (targetYFraction * screenH - propPx / 2f).coerceIn(160f, screenH - propPx - 90f)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .zIndex(10f)
                        .size(propSizeDp)
                        .offset { IntOffset(px.roundToInt(), py.roundToInt()) }
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(enabled = !isRoundFinished) {
                            isRoundFinished = true
                            roundOutcome = MiniGameOutcome.WIN
                            awardOnce(60L)
                            dialogueText = DoctorQuotes.findObjWinQuotes.random()
                        }
                        .testTag("find_prop_${targetProp.name.lowercase()}")
                ) {
                    Image(
                        painter = painterResource(id = targetProp.drawableRes),
                        contentDescription = targetProp.labelZh,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    )
                }
            }

            // Live massage touch pink heart feedback
            if (gameType == GymMiniGameType.MASSAGE && lastTouchPos != null && !isRoundFinished) {
                val pos = lastTouchPos!!
                val dpX = with(density) { pos.x.toDp() }
                val dpY = with(density) { pos.y.toDp() }
                Box(
                    modifier = Modifier
                        .offset(x = dpX - 24.dp, y = dpY - 24.dp)
                        .size(48.dp)
                        .zIndex(14f),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = PinkPrimary.copy(alpha = 0.36f),
                        border = BorderStroke(2.dp, Color.White.copy(alpha = 0.88f)),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 3. Top System Banner (Rules + Points + Close X) styled cleanly in pastel pink/cream
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .zIndex(22f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PinkBannerBg.copy(alpha = 0.93f),
                    border = BorderStroke(1.5.dp, PinkPrimary),
                    shadowElevation = 5.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PinkPrimary,
                            border = BorderStroke(1.5.dp, Color.White),
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.PriorityHigh,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        Text(
                            text = systemBannerText,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = ClinicInk,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF4A3134).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f))
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("mini_game_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "離開小遊戲",
                            tint = Color.White
                        )
                    }
                }
            }

            // 4. Doctor Pure-Dialogue Visual Novel Card (always clear of top system banner)
            val showDialogueBubble = gameType != GymMiniGameType.RPS || isRoundFinished
            if (showDialogueBubble) {
                Box(
                    modifier = Modifier
                        .align(
                            if (gameType == GymMiniGameType.FIND_OBJECT) Alignment.TopCenter else Alignment.TopEnd
                        )
                        .statusBarsPadding()
                        .padding(top = 66.dp, start = 16.dp, end = 16.dp)
                        .widthIn(max = if (gameType == GymMiniGameType.FIND_OBJECT) 340.dp else 245.dp)
                        .zIndex(15f)
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Surface(
                            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                            color = PinkPrimary,
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = doctorName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(
                                topEnd = 14.dp,
                                bottomStart = 14.dp,
                                bottomEnd = 14.dp
                            ),
                            color = Color.White.copy(alpha = 0.96f),
                            border = BorderStroke(1.dp, PinkSoftRibbon),
                            shadowElevation = 6.dp
                        ) {
                            Text(
                                text = dialogueText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = ClinicInk,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                            )
                        }
                    }
                }
            }

            // 5. RPS Mid-Screen Result Overlay (Doctor's square card + Horizontal "YOU WIN!" ribbon matching reference screenshot)
            if (gameType == GymMiniGameType.RPS && isRoundFinished && doctorRpsChoice != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .offset(y = 24.dp)
                        .zIndex(18f),
                    contentAlignment = Alignment.Center
                ) {
                    // Horizontal translucent result ribbon
                    val bannerTitle = when (roundOutcome) {
                        MiniGameOutcome.WIN -> "YOU WIN !"
                        MiniGameOutcome.LOSE -> "YOU LOSE..."
                        else -> "DRAW !"
                    }
                    Surface(
                        color = when (roundOutcome) {
                            MiniGameOutcome.WIN -> PinkPrimary.copy(alpha = 0.72f)
                            MiniGameOutcome.TIE -> Color(0xFFB28B9A).copy(alpha = 0.72f)
                            else -> Color(0xFF5D464C).copy(alpha = 0.74f)
                        },
                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-92).dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFFFF0A8),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = bannerTitle,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.5.sp
                                ),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFFFF0A8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Doctor's chosen hand square card in front of chest (matching reference screenshot 2)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = PinkPrimary,
                        border = BorderStroke(2.5.dp, Color.White),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .offset(y = 14.dp)
                            .size(92.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(14.dp)) {
                            RpsHandArtwork(hand = doctorRpsChoice!!, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }

            // 6. High Card Center Table (2 White Cat Playing Cards — player picks left or right freely)
            if (gameType == GymMiniGameType.HIGH_CARD) {
                fun pickSide(pickLeft: Boolean) {
                    if (cardsRevealed) return
                    val pRank = if (pickLeft) leftCardRank else rightCardRank
                    val dRank = if (pickLeft) rightCardRank else leftCardRank
                    playerPickedLeft = pickLeft
                    playerCardRank = pRank
                    doctorCardRank = dRank
                    cardsRevealed = true
                    isRoundFinished = true
                    if (pRank > dRank) {
                        roundOutcome = MiniGameOutcome.WIN
                        awardOnce(60L)
                        dialogueText = DoctorQuotes.highCardWinQuotes.random()
                    } else {
                        roundOutcome = MiniGameOutcome.LOSE
                        dialogueText = DoctorQuotes.highCardLoseQuotes.random()
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(
                            start = 24.dp,
                            end = 24.dp,
                            bottom = if (isRoundFinished) 132.dp else 90.dp
                        )
                        .zIndex(18f),
                    horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isLeftPlayer = playerPickedLeft == true
                    val isRightPlayer = playerPickedLeft == false

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = when {
                                !cardsRevealed -> PinkPrimary
                                isLeftPlayer -> PinkPrimary
                                else -> Color(0xFF4A3134).copy(alpha = 0.85f)
                            }
                        ) {
                            Text(
                                text = when {
                                    !cardsRevealed -> "選左邊這張"
                                    isLeftPlayer -> "你的手牌"
                                    else -> "${doctorName}的牌"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                        WhiteCatPlayingCard(
                            rank = if (cardsRevealed) leftCardRank else null,
                            faceUp = cardsRevealed,
                            onClick = if (!cardsRevealed) ({ pickSide(true) }) else null,
                            modifier = Modifier
                                .width(126.dp)
                                .aspectRatio(0.68f)
                                .testTag("high_card_left_button")
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = when {
                                !cardsRevealed -> PinkPrimary
                                isRightPlayer -> PinkPrimary
                                else -> Color(0xFF4A3134).copy(alpha = 0.85f)
                            }
                        ) {
                            Text(
                                text = when {
                                    !cardsRevealed -> "選右邊這張"
                                    isRightPlayer -> "你的手牌"
                                    else -> "${doctorName}的牌"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                        WhiteCatPlayingCard(
                            rank = if (cardsRevealed) rightCardRank else null,
                            faceUp = cardsRevealed,
                            onClick = if (!cardsRevealed) ({ pickSide(false) }) else null,
                            modifier = Modifier
                                .width(126.dp)
                                .aspectRatio(0.68f)
                                .testTag("high_card_draw_button")
                        )
                    }
                }
            }

            // 7. Bottom Controls Strip
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 96.dp)
                    .zIndex(20f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (gameType) {
                    GymMiniGameType.RPS -> {
                        // Full-width translucent pink RPS bar matching reference screenshots
                        Surface(
                            color = PinkStripBg.copy(alpha = 0.68f),
                            border = BorderStroke(1.5.dp, PinkPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RpsHand.entries.forEach { hand ->
                                    val isChosen = playerRpsChoice == hand
                                    val alpha = if (isRoundFinished && !isChosen) 0.45f else 1f
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = PinkPrimary,
                                        border = BorderStroke(
                                            width = if (isChosen) 3.dp else 1.5.dp,
                                            color = if (isChosen) Color.White else Color.White.copy(alpha = 0.72f)
                                        ),
                                        shadowElevation = if (isChosen) 8.dp else 4.dp,
                                        modifier = Modifier
                                            .size(84.dp)
                                            .alpha(alpha)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable(enabled = !isRoundFinished) {
                                                val doc = RpsHand.entries.random()
                                                playerRpsChoice = hand
                                                doctorRpsChoice = doc
                                                isRoundFinished = true
                                                val win = (hand == RpsHand.ROCK && doc == RpsHand.SCISSORS) ||
                                                    (hand == RpsHand.SCISSORS && doc == RpsHand.PAPER) ||
                                                    (hand == RpsHand.PAPER && doc == RpsHand.ROCK)
                                                val tie = hand == doc
                                                when {
                                                    win -> {
                                                        roundOutcome = MiniGameOutcome.WIN
                                                        awardOnce(60L)
                                                        dialogueText = DoctorQuotes.rpsWinQuotes.random()
                                                    }
                                                    tie -> {
                                                        roundOutcome = MiniGameOutcome.TIE
                                                        dialogueText = DoctorQuotes.rpsTieQuotes.random()
                                                    }
                                                    else -> {
                                                        roundOutcome = MiniGameOutcome.LOSE
                                                        dialogueText = DoctorQuotes.rpsLoseQuotes.random()
                                                    }
                                                }
                                            }
                                            .testTag("rps_btn_${hand.labelZh}")
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(12.dp)) {
                                            RpsHandArtwork(hand = hand, modifier = Modifier.fillMaxSize())
                                        }
                                    }
                                }
                            }
                        }
                    }

                    GymMiniGameType.MASSAGE -> {
                        AnimatedVisibility(
                            visible = !isRoundFinished,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(horizontal = 16.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = PinkPrimary,
                                border = BorderStroke(2.dp, Color.White),
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PanTool,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "在他身上滑動",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Pink horizontal massage progress bar
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.94f),
                            border = BorderStroke(1.5.dp, PinkPrimary.copy(alpha = 0.75f)),
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = { animatedRubRatio },
                                    color = PinkPrimary,
                                    trackColor = Color(0xFFFFE5EC),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(50))
                                )
                                Text(
                                    text = "${(animatedRubRatio * 100f).roundToInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = ClinicInk
                                )
                            }
                        }
                    }

                    GymMiniGameType.HIGH_CARD, GymMiniGameType.FIND_OBJECT -> Unit
                }

                if (isRoundFinished) {
                    val hintText = when (roundOutcome) {
                        MiniGameOutcome.WIN, MiniGameOutcome.COMPLETED -> "已收下 60 復健點數・點擊任意空白處返回復健室"
                        MiniGameOutcome.TIE -> "本局平手・點擊任意空白處返回復健室"
                        MiniGameOutcome.LOSE, null -> "點擊任意空白處返回復健室"
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = when (roundOutcome) {
                            MiniGameOutcome.WIN, MiniGameOutcome.COMPLETED -> PinkPrimary.copy(alpha = 0.92f)
                            MiniGameOutcome.TIE -> Color(0xFF8C6772).copy(alpha = 0.92f)
                            MiniGameOutcome.LOSE, null -> Color(0xFF4A3134).copy(alpha = 0.90f)
                        },
                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.85f)),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                            .testTag("mini_game_finish_return_button")
                    ) {
                        Text(
                            text = hintText,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

private fun cardRankLabel(rank: Int): String = when (rank) {
    1 -> "A"
    11 -> "J"
    12 -> "Q"
    13 -> "K"
    else -> rank.toString()
}

@Composable
private fun RpsHandArtwork(hand: RpsHand, modifier: Modifier = Modifier) {
    val resId = when (hand) {
        RpsHand.ROCK -> R.drawable.img_rps_rock
        RpsHand.SCISSORS -> R.drawable.img_rps_scissors
        RpsHand.PAPER -> R.drawable.img_rps_paper
    }
    Image(
        painter = painterResource(id = resId),
        contentDescription = hand.labelZh,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

@Composable
private fun WhiteCatPlayingCard(
    rank: Int?,
    faceUp: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        if (!faceUp || rank == null) {
            Image(
                painter = painterResource(id = R.drawable.img_poker_cat_back),
                contentDescription = "白貓撲克牌背面",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val label = cardRankLabel(rank)
            Image(
                painter = painterResource(id = R.drawable.img_poker_cat_front),
                contentDescription = "白貓撲克牌 $label",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color(0xFF7D3448),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 15.dp, top = 11.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color(0xFF7D3448),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 15.dp, bottom = 11.dp)
            )
        }
    }
}

