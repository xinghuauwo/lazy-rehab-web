package com.example.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.data.DoctorGearConfig
import com.example.data.DoctorPose
import com.example.data.PharmacyItem
import com.example.data.RehabCatalog
import com.example.data.WardAnchor
import com.example.data.WardDoctorSnapshot
import com.example.data.WardPlacement
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class WardDocSprite(@DrawableRes val drawableRes: Int) {
    IDLE_A(R.drawable.img_doc_idle_1),
    IDLE_B(R.drawable.img_doc_idle_2),
    WALK_A(R.drawable.img_doc_walk_1),
    WALK_MID(R.drawable.img_doc_walk_mid),
    WALK_OPP(R.drawable.img_doc_walk_opp),
    WALK_PASS(R.drawable.img_doc_walk_pass),
    WALK_B(R.drawable.img_doc_walk_2),
    TURN(R.drawable.img_doc_turn),
    INTERACT(R.drawable.img_doc_interact),
    POSE_SIP(R.drawable.img_doctor_sipping),
    POSE_APPROVE(R.drawable.img_doctor_approve),
    PICKED_UP(R.drawable.img_doc_picked_up),
    SITTING_CHAIR(R.drawable.img_doc_sitting),
    LYING_BED(R.drawable.img_ward_doc_lying),
    INSPECT_CHAIR(R.drawable.img_doc_interact);

    companion object {
        fun fromSnapshotKey(key: String?): WardDocSprite? =
            entries.firstOrNull { it.name == key }
    }
}

private data class WardWalkTarget(
    val walkX: Float,
    val walkY: Float,
    val interactTargetId: String?,
    val lookAtX: Float
)

/**
 * Keeps autonomous AI patrol strictly inside the open mint-cream checkered tiles,
 * never walking across the hospital bed, cabinet, visitor chair, or outer margins.
 */
private fun clampToWalkableFloor(rawX: Float, rawY: Float): Pair<Float, Float> {
    val x = rawX.coerceIn(0.38f, 0.74f)
    val minY = when {
        x < 0.44f -> 0.76f
        x < 0.58f -> (0.76f - 0.85f * (x - 0.44f)).coerceAtLeast(0.59f)
        x <= 0.65f -> 0.59f
        else -> 0.59f + (x - 0.65f) * 0.65f
    }
    val maxY = (0.88f - 0.52f * abs(x - 0.50f)).coerceAtMost(0.84f)
    val safeMax = maxY.coerceAtLeast(minY + 0.01f)
    val y = rawY.coerceIn(minY, safeMax)
    return x to y
}

/**
 * Projects the doctor's drop shadow strictly onto the ward floor plane,
 * so even when picked up high into the air or dragged near a wall,
 * the shadow stays grounded on the floor underneath him.
 */
private fun clampToFloorShadow(rawX: Float, rawY: Float): Pair<Float, Float> {
    val x = rawX.coerceIn(0.20f, 0.88f)
    val minFloorY = when {
        x < 0.36f -> 0.66f - 0.14f * (x - 0.20f)
        x < 0.62f -> 0.57f
        else -> 0.57f + 0.32f * (x - 0.62f)
    }
    val maxFloorY = (0.89f - 0.48f * abs(x - 0.50f)).coerceAtMost(0.86f)
    val safeMax = maxFloorY.coerceAtLeast(minFloorY + 0.01f)
    val y = rawY.coerceIn(minFloorY, safeMax)
    return x to y
}

private fun isDoctorBehindBed(docX: Float, docY: Float): Boolean {
    if (docX !in 0.22f..0.56f) return false
    val frontEdgeY = if (docX <= 0.43f) {
        0.655f + 0.43f * (docX - 0.22f)
    } else {
        0.745f - 0.75f * (docX - 0.43f)
    }
    return docY in 0.40f..frontEdgeY
}

private fun isDoctorBehindIv(docX: Float, docY: Float): Boolean =
    docX in 0.14f..0.28f && docY < 0.72f

private fun isDoctorBehindChair(docX: Float, docY: Float): Boolean =
    docX in 0.76f..0.95f && docY < 0.71f

private fun normalizedPolygonShape(points: List<Pair<Float, Float>>): Shape =
    GenericShape { size, _ ->
        if (points.isEmpty()) return@GenericShape
        moveTo(points[0].first * size.width, points[0].second * size.height)
        for (i in 1 until points.size) {
            lineTo(points[i].first * size.width, points[i].second * size.height)
        }
        close()
    }

// Hospital bed silhouette ONLY — cleanly separated from nightstand and pink 2-drawer side cabinet
// (Side cabinet & nightstand remain strictly behind the doctor layer so the doctor is never covered by the cabinet)
private val BedOnlyOcclusionShape: Shape = normalizedPolygonShape(
    listOf(
        0.235f to 0.445f,
        0.325f to 0.388f,
        0.368f to 0.415f,
        0.372f to 0.460f,
        0.576f to 0.518f,
        0.576f to 0.618f,
        0.558f to 0.655f,
        0.435f to 0.742f,
        0.238f to 0.652f,
        0.226f to 0.545f
    )
)

private val IvStandOcclusionShape: Shape = normalizedPolygonShape(
    listOf(
        0.17f to 0.37f,
        0.26f to 0.37f,
        0.27f to 0.73f,
        0.16f to 0.73f
    )
)

private val ChairOcclusionShape: Shape = normalizedPolygonShape(
    listOf(
        0.86f to 0.50f,
        0.93f to 0.53f,
        0.91f to 0.69f,
        0.85f to 0.73f,
        0.80f to 0.68f,
        0.79f to 0.58f
    )
)

@Composable
fun WardRoomView(
    placements: List<WardPlacement>,
    doctorNickname: String,
    isInteractive: Boolean,
    fixedDoctorPose: DoctorPose = DoctorPose.OBSERVE,
    fixedDoctorSnapshot: WardDoctorSnapshot? = null,
    doctorGear: DoctorGearConfig = DoctorGearConfig(),
    isFullBleed: Boolean = false,
    showAnchors: Boolean = false,
    isEditingProps: Boolean = false,
    activePlacingItemId: String? = null,
    externalSpeechMessage: String? = null,
    externalSpeechNonce: Int = 0,
    onSelectPlacementAnchor: (WardAnchor) -> Unit = {},
    onMoveItem: (itemId: String, xFraction: Float, yFraction: Float) -> Unit = { _, _, _ -> },
    onDoctorPoseChanged: (DoctorPose) -> Unit = {},
    onDoctorSnapshotChanged: (WardDoctorSnapshot) -> Unit = {},
    onDoctorTapped: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val itemLookup = remember { RehabCatalog.pharmacyItems.associateBy { it.id } }
    val latestPlacements by rememberUpdatedState(placements)
    val latestOnDoctorTapped by rememberUpdatedState(onDoctorTapped)
    val latestOnDoctorSnapshotChanged by rememberUpdatedState(onDoctorSnapshotChanged)
    val haptic = LocalHapticFeedback.current

    var doctorTargetX by remember(isInteractive, fixedDoctorSnapshot) {
        mutableFloatStateOf(
            if (isInteractive) 0.56f else (fixedDoctorSnapshot?.xFraction ?: 0.59f)
        )
    }
    var doctorTargetY by remember(isInteractive, fixedDoctorSnapshot) {
        mutableFloatStateOf(
            if (isInteractive) 0.72f else (fixedDoctorSnapshot?.yFraction ?: 0.73f)
        )
    }
    var doctorFacingRight by remember(isInteractive, fixedDoctorSnapshot) {
        mutableStateOf(if (isInteractive) false else (fixedDoctorSnapshot?.facingRight ?: false))
    }
    var isWalking by remember { mutableStateOf(false) }
    var isTurning by remember { mutableStateOf(false) }
    var isFingerDownOnDoctor by remember { mutableStateOf(false) }
    var isDraggingDoctor by remember { mutableStateOf(false) }
    var dragAnchorX by remember { mutableFloatStateOf(0.56f) }
    var dragAnchorY by remember { mutableFloatStateOf(0.72f) }
    var dragAccumPx by remember { mutableStateOf(Offset.Zero) }
    var isPlayerHoldActive by remember { mutableStateOf(false) }
    var activeWalkDurationMs by remember { mutableIntStateOf(1500) }
    var placedAnchorId by remember(isInteractive, fixedDoctorSnapshot) {
        mutableStateOf(
            if (isInteractive) null else fixedDoctorSnapshot?.anchorId?.takeIf { it.isNotBlank() }
        )
    }

    var draggingPropId by remember { mutableStateOf<String?>(null) }
    var hoveredAnchorId by remember { mutableStateOf<String?>(null) }

    var currentSprite by remember(fixedDoctorPose, fixedDoctorSnapshot, isInteractive) {
        mutableStateOf(
            if (isInteractive) {
                WardDocSprite.IDLE_A
            } else {
                WardDocSprite.fromSnapshotKey(fixedDoctorSnapshot?.spriteKey)
                    ?: when (fixedDoctorPose) {
                        DoctorPose.SIPPING -> WardDocSprite.POSE_SIP
                        DoctorPose.APPROVE -> WardDocSprite.POSE_APPROVE
                        DoctorPose.PHARMACY -> WardDocSprite.INTERACT
                        DoctorPose.OBSERVE -> WardDocSprite.IDLE_A
                    }
            }
        )
    }
    var walkFramePhase by remember { mutableIntStateOf(0) }
    var interactingTargetId by remember { mutableStateOf<String?>(null) }

    var speechLine by remember {
        mutableStateOf(RehabCatalog.wardDoctorWanderLines.first())
    }
    var isTapReacting by remember { mutableStateOf(false) }
    var doctorTapNonce by remember { mutableIntStateOf(0) }
    var doctorIsBouncing by remember { mutableStateOf(false) }
    var dropHoldNonce by remember { mutableIntStateOf(0) }

    LaunchedEffect(externalSpeechNonce) {
        val msg = externalSpeechMessage
        if (isInteractive && externalSpeechNonce > 0 && !msg.isNullOrBlank()) {
            speechLine = msg
            doctorTapNonce += 1
        }
    }

    if (isInteractive) {
        LaunchedEffect(dropHoldNonce) {
            if (dropHoldNonce > 0) {
                delay((5_000L..10_000L).random())
                if (!isDraggingDoctor && !isFingerDownOnDoctor) {
                    isPlayerHoldActive = false
                }
            }
        }
    }

    if (isInteractive) {
        LaunchedEffect(currentSprite, doctorTargetX, doctorTargetY, doctorFacingRight, placedAnchorId) {
            latestOnDoctorSnapshotChanged(
                WardDoctorSnapshot(
                    spriteKey = currentSprite.name,
                    xFraction = doctorTargetX,
                    yFraction = doctorTargetY,
                    facingRight = doctorFacingRight,
                    anchorId = placedAnchorId.orEmpty()
                )
            )
        }
    }

    LaunchedEffect(doctorTapNonce) {
        if (doctorTapNonce > 0) {
            isTapReacting = true
            doctorIsBouncing = true
            delay(170L)
            doctorIsBouncing = false
            delay(2630L)
            isTapReacting = false
        }
    }

    if (isInteractive) {
        LaunchedEffect(Unit) {
            // Sub-loop for walking stride frames & subtle idle blinks
            launch {
                var tick = 0
                while (true) {
                    if (isDraggingDoctor) {
                        currentSprite = WardDocSprite.PICKED_UP
                        delay(120L)
                    } else if (isTurning) {
                        currentSprite = WardDocSprite.TURN
                        delay(110L)
                    } else if (isWalking) {
                        val pattern = listOf(
                            WardDocSprite.WALK_A,
                            WardDocSprite.WALK_MID,
                            WardDocSprite.WALK_OPP,
                            WardDocSprite.WALK_PASS
                        )
                        walkFramePhase = (walkFramePhase + 1) % pattern.size
                        currentSprite = pattern[walkFramePhase]
                        delay(135L)
                    } else if (placedAnchorId == "anchor_chair_side" && !isTapReacting) {
                        currentSprite = WardDocSprite.SITTING_CHAIR
                        delay(200L)
                    } else if (placedAnchorId == "anchor_bed_pillow" && !isTapReacting) {
                        currentSprite = WardDocSprite.LYING_BED
                        delay(200L)
                    } else if (placedAnchorId == "anchor_bed_blanket" && !isTapReacting) {
                        currentSprite = WardDocSprite.INTERACT
                        delay(200L)
                    } else if (interactingTargetId == null && !isTapReacting) {
                        tick += 1
                        currentSprite = if (tick % 9 == 0) WardDocSprite.IDLE_B else WardDocSprite.IDLE_A
                        delay(if (tick % 9 == 0) 420L else 360L)
                    } else {
                        delay(180L)
                    }
                }
            }

            // Main ward patrol & item-interaction state machine (strictly on open checkered floor)
            val builtInSpots = listOf(
                WardWalkTarget(0.39f, 0.77f, "default_iv", 0.22f),
                WardWalkTarget(0.47f, 0.75f, "default_bed", 0.30f),
                WardWalkTarget(0.60f, 0.60f, "default_cabinet", 0.59f),
                WardWalkTarget(0.71f, 0.71f, "default_chair", 0.83f),
                WardWalkTarget(0.56f, 0.73f, null, 0.56f),
                WardWalkTarget(0.65f, 0.65f, null, 0.65f)
            )
            var stepCount = 0

            while (true) {
                delay(2600L)
                stepCount += 1

                while (isTapReacting || isFingerDownOnDoctor || isDraggingDoctor || isPlayerHoldActive) {
                    delay(200L)
                }

                placedAnchorId = null
                interactingTargetId = null

                val placedTargets = latestPlacements.map { placement ->
                    val (floorX, floorY) = clampToWalkableFloor(placement.xFraction, placement.yFraction)
                    WardWalkTarget(
                        walkX = floorX,
                        walkY = floorY,
                        interactTargetId = placement.itemId,
                        lookAtX = placement.xFraction
                    )
                }

                val targetPool = if (placedTargets.isNotEmpty() && stepCount % 2 == 1) {
                    placedTargets
                } else {
                    builtInSpots
                }
                val candidates = targetPool.filter {
                    abs(it.walkX - doctorTargetX) + abs(it.walkY - doctorTargetY) > 0.06f
                }
                val rawChosen = candidates.randomOrNull() ?: targetPool[stepCount % targetPool.size]
                val (safeX, safeY) = clampToWalkableFloor(rawChosen.walkX, rawChosen.walkY)
                val chosen = rawChosen.copy(walkX = safeX, walkY = safeY)

                val dx = chosen.walkX - doctorTargetX
                val wantsFaceRight = if (abs(dx) > 0.015f) dx > 0f else doctorFacingRight

                if (wantsFaceRight != doctorFacingRight) {
                    isTurning = true
                    delay(240L)
                    if (!isFingerDownOnDoctor && !isDraggingDoctor) {
                        doctorFacingRight = wantsFaceRight
                    }
                    isTurning = false
                }

                if (isFingerDownOnDoctor || isDraggingDoctor || isPlayerHoldActive) continue

                val dist = abs(chosen.walkX - doctorTargetX) + abs(chosen.walkY - doctorTargetY)
                activeWalkDurationMs = (dist * 4200f).roundToInt().coerceIn(950, 1950)
                isWalking = true
                doctorTargetX = chosen.walkX
                doctorTargetY = chosen.walkY
                delay(activeWalkDurationMs.toLong() + 80L)
                isWalking = false

                if (isTapReacting || isFingerDownOnDoctor || isDraggingDoctor || isPlayerHoldActive) continue

                val targetId = chosen.interactTargetId
                if (targetId != null) {
                    val lookRight = chosen.lookAtX > doctorTargetX + 0.02f
                    val lookLeft = chosen.lookAtX < doctorTargetX - 0.02f
                    if ((lookRight && !doctorFacingRight) || (lookLeft && doctorFacingRight)) {
                        isTurning = true
                        delay(200L)
                        doctorFacingRight = lookRight
                        isTurning = false
                    }

                    interactingTargetId = targetId
                    val interactSprite = when (targetId) {
                        "boba_iv_drip", "long_straw" -> WardDocSprite.POSE_SIP
                        "miracle_banner", "vip_deed" -> WardDocSprite.POSE_APPROVE
                        else -> WardDocSprite.INTERACT
                    }
                    currentSprite = interactSprite
                    speechLine = RehabCatalog.wardItemInteractionLine(targetId)
                    onDoctorPoseChanged(
                        when (interactSprite) {
                            WardDocSprite.POSE_SIP -> DoctorPose.SIPPING
                            WardDocSprite.POSE_APPROVE -> DoctorPose.APPROVE
                            else -> DoctorPose.OBSERVE
                        }
                    )
                    delay(2800L)
                    interactingTargetId = null
                } else {
                    currentSprite = WardDocSprite.IDLE_A
                    val idleLines = RehabCatalog.wardDoctorWanderLines.filter { it != speechLine }
                    speechLine = idleLines.randomOrNull() ?: RehabCatalog.wardDoctorWanderLines.first()
                    onDoctorPoseChanged(DoctorPose.OBSERVE)
                }
            }
        }
    }

    val animDoctorX by animateFloatAsState(
        targetValue = doctorTargetX,
        animationSpec = tween(
            durationMillis = if (isInteractive && !isDraggingDoctor && !isFingerDownOnDoctor) activeWalkDurationMs else 0,
            easing = LinearEasing
        ),
        label = "doctor_x"
    )
    val animDoctorY by animateFloatAsState(
        targetValue = doctorTargetY,
        animationSpec = tween(
            durationMillis = if (isInteractive && !isDraggingDoctor && !isFingerDownOnDoctor) activeWalkDurationMs else 0,
            easing = LinearEasing
        ),
        label = "doctor_y"
    )
    val walkBobOffsetPx = if (isWalking && walkFramePhase % 2 == 1) -2.5f else 0f
    val animWalkBob by animateFloatAsState(
        targetValue = walkBobOffsetPx,
        animationSpec = tween(durationMillis = 100),
        label = "walk_bob"
    )
    val tapBounceScale by animateFloatAsState(
        targetValue = when {
            isDraggingDoctor -> 1.12f
            doctorIsBouncing -> 1.15f
            else -> 1.0f
        },
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
        label = "tap_bounce"
    )

    val cornerRadius = if (isFullBleed) 0.dp else if (isInteractive) 24.dp else 18.dp
    Surface(
        shape = RoundedCornerShape(cornerRadius),
        border = if (isFullBleed) {
            null
        } else {
            BorderStroke(
                width = if (isInteractive) 1.5.dp else 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        },
        color = Color(0xFFFFFBF4),
        modifier = modifier
            .then(if (isFullBleed) Modifier else Modifier.clip(RoundedCornerShape(cornerRadius)))
            .testTag(if (isInteractive) "ward_room_live" else "ward_room_snapshot")
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFDF6),
                            Color(0xFFFFF3F5)
                        )
                    )
                )
                .padding(
                    vertical = if (isFullBleed) 0.dp else if (isInteractive) 6.dp else 4.dp,
                    horizontal = if (isFullBleed) 0.dp else 4.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            // img_ward_room_bg.png is 952 x 840 (with bedside IV drip included)
            val roomAspect = 952f / 840f
            val availableAspect = (maxWidth / maxHeight).coerceAtLeast(0.5f)
            val fitHeightFirst = !isFullBleed && availableAspect >= roomAspect

            BoxWithConstraints(
                modifier = (if (fitHeightFirst) Modifier.fillMaxHeight() else Modifier.fillMaxWidth())
                    .aspectRatio(roomAspect, matchHeightConstraintsFirst = fitHeightFirst)
            ) {
                val density = LocalDensity.current
                val roomWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
                val roomHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

                // 1. 2.5D Isometric Hospital Ward Base Background
                Image(
                    painter = painterResource(id = R.drawable.img_ward_room_bg),
                    contentDescription = "療癒可愛病房全景（含病床旁點滴）",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(0f)
                )

                // Subtle sparkle indicator over built-in bedside IV when doctor is inspecting it
                if (isInteractive && interactingTargetId == "default_iv") {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.94f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .zIndex(14f)
                            .offset {
                                IntOffset(
                                    x = (0.18f * roomWidthPx).roundToInt(),
                                    y = (0.36f * roomHeightPx).roundToInt()
                                )
                            }
                    ) {
                        Text(
                            text = "💧 點滴巡檢中",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // 2. Placement Anchor Slots (shown when editing/placing, dragging a prop, OR dragging doctor!)
                val anchorsVisible = isInteractive && (
                    showAnchors ||
                        isEditingProps ||
                        draggingPropId != null ||
                        activePlacingItemId != null ||
                        isDraggingDoctor
                    )
                if (anchorsVisible) {
                    val occupiedMap = remember(placements) {
                        placements.associateBy {
                            RehabCatalog.resolveWardDropAnchor(it.xFraction, it.yFraction).id
                        }
                    }
                    RehabCatalog.wardAnchors.forEach { anchor ->
                        val isHovered = hoveredAnchorId == anchor.id
                        val isOccupied = anchor.id in occupiedMap
                        val markerSizeDp = if (isHovered) 34.dp else 26.dp
                        val markerPx = with(density) { markerSizeDp.toPx() }
                        val anchorX = (anchor.xFraction * roomWidthPx - markerPx / 2f)
                            .coerceIn(4f, (roomWidthPx - markerPx - 4f).coerceAtLeast(4f))
                        val anchorY = (anchor.yFraction * roomHeightPx - markerPx / 2f)
                            .coerceIn(4f, (roomHeightPx - markerPx - 4f).coerceAtLeast(4f))

                        val primaryColor = MaterialTheme.colorScheme.primary
                        val secondaryColor = MaterialTheme.colorScheme.secondary

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .zIndex(if (isHovered) 24f else 3f)
                                .offset { IntOffset(anchorX.roundToInt(), anchorY.roundToInt()) }
                                .then(
                                    if (activePlacingItemId != null || showAnchors) {
                                        Modifier.clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            onSelectPlacementAnchor(anchor)
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                                .testTag("ward_anchor_${anchor.id}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(markerSizeDp)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawCircle(
                                        color = when {
                                            isHovered -> primaryColor.copy(alpha = 0.32f)
                                            activePlacingItemId != null -> secondaryColor.copy(alpha = 0.24f)
                                            else -> Color.White.copy(alpha = 0.65f)
                                        }
                                    )
                                    drawCircle(
                                        color = when {
                                            isHovered -> primaryColor
                                            isOccupied -> primaryColor.copy(alpha = 0.7f)
                                            else -> secondaryColor
                                        },
                                        style = Stroke(
                                            width = if (isHovered) 2.5.dp.toPx() else 1.8.dp.toPx(),
                                            pathEffect = if (isHovered) null else PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                                        )
                                    )
                                }
                                if (isHovered || !isOccupied) {
                                    Box(
                                        modifier = Modifier
                                            .size(if (isHovered) 10.dp else 7.dp)
                                            .clip(CircleShape)
                                            .background(if (isHovered) primaryColor else secondaryColor)
                                    )
                                }
                            }
                            if (showAnchors || isHovered || activePlacingItemId != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isHovered) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.White.copy(alpha = 0.90f)
                                    },
                                    border = BorderStroke(
                                        0.8.dp,
                                        if (isHovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    )
                                ) {
                                    Text(
                                        text = anchor.labelZh,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.sp,
                                            fontWeight = if (isHovered) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isHovered) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Placed Pharmacy Props (depth-sorted against live doctor Y position!)
                val liveDoctorX = if (isDraggingDoctor) {
                    (dragAnchorX + dragAccumPx.x / roomWidthPx).coerceIn(0.10f, 0.90f)
                } else {
                    animDoctorX
                }
                val liveDoctorY = if (isDraggingDoctor) {
                    (dragAnchorY + dragAccumPx.y / roomHeightPx).coerceIn(0.28f, 0.90f)
                } else {
                    animDoctorY
                }

                placements.forEachIndexed { index, placement ->
                    val item = itemLookup[placement.itemId] ?: return@forEachIndexed
                    val nearestAnchor = RehabCatalog.nearestWardAnchor(placement.xFraction, placement.yFraction)
                    val isWallProp = nearestAnchor.isWallSurface && placement.yFraction < 0.37f
                    val propZOrder = when {
                        isWallProp -> 2f + index * 0.03f
                        placement.yFraction > liveDoctorY + 0.01f -> 12f + index * 0.03f // In front of doctor
                        else -> 5f + index * 0.03f // Behind doctor
                    }

                    WardPlacedPropSprite(
                        item = item,
                        placement = placement,
                        roomWidthPx = roomWidthPx,
                        roomHeightPx = roomHeightPx,
                        isInteractive = isInteractive,
                        isEditingProps = isInteractive && isEditingProps,
                        isDoctorInteracting = isInteractive && interactingTargetId == placement.itemId,
                        zOrder = propZOrder,
                        onDragStateChanged = { isDragging, currentX, currentY ->
                            if (isDragging) {
                                draggingPropId = item.id
                                hoveredAnchorId = RehabCatalog.resolveWardDropAnchor(currentX, currentY).id
                            } else {
                                if (draggingPropId == item.id) {
                                    draggingPropId = null
                                    hoveredAnchorId = null
                                }
                            }
                        },
                        onCommitMove = { fx, fy ->
                            val snapped = RehabCatalog.resolveWardDropAnchor(fx, fy)
                            onMoveItem(placement.itemId, snapped.xFraction, snapped.yFraction)
                        }
                    )
                }

                // 4. Full-Body Animated White-Haired Doctor (floor shadow decoupled from lifted character!)
                val isLyingPose = currentSprite == WardDocSprite.LYING_BED
                val docWidthDp = if (isFullBleed) 58.dp else if (isInteractive) 50.dp else 34.dp
                val docHeightDp = if (isFullBleed) 102.dp else if (isInteractive) 88.dp else 60.dp
                val renderWidthDp = if (isLyingPose) docHeightDp * 0.88f else docWidthDp
                val renderHeightDp = if (isLyingPose) docWidthDp * 0.96f else docHeightDp
                val docWidthPx = with(density) { renderWidthDp.toPx() }
                val docHeightPx = with(density) { renderHeightDp.toPx() }
                val hitWidthDp = maxOf(renderWidthDp, 64.dp)
                val hitWidthPx = with(density) { hitWidthDp.toPx() }

                val visualCenterX = (liveDoctorX * roomWidthPx)
                    .coerceIn(docWidthPx / 2f + 4f, (roomWidthPx - docWidthPx / 2f - 4f).coerceAtLeast(docWidthPx / 2f + 4f))
                val visualFootY = (liveDoctorY * roomHeightPx)
                    .coerceIn(docHeightPx + 4f, (roomHeightPx - 4f).coerceAtLeast(docHeightPx + 4f))
                val visualOffsetX = visualCenterX - docWidthPx / 2f
                val visualOffsetY = visualFootY - docHeightPx

                // Independent floor-only shadow projection (never climbs onto walls or floats with lifted character)
                val (shadowFracX, shadowFracY) = clampToFloorShadow(liveDoctorX, liveDoctorY)
                val shadowW = if (isDraggingDoctor) docWidthDp * 0.56f else docWidthDp * 0.72f
                val shadowH = if (isInteractive) (if (isDraggingDoctor) 6.dp else 8.dp) else 6.dp
                val shadowWPx = with(density) { shadowW.toPx() }
                val shadowHPx = with(density) { shadowH.toPx() }
                val shadowOffsetX = (shadowFracX * roomWidthPx - shadowWPx / 2f)
                    .coerceIn(4f, (roomWidthPx - shadowWPx - 4f).coerceAtLeast(4f))
                val shadowOffsetY = (shadowFracY * roomHeightPx - shadowHPx / 2f)
                    .coerceIn(4f, (roomHeightPx - shadowHPx - 4f).coerceAtLeast(4f))

                // Stationary touch-hit box anchor during drag so pointerInput coords never jitter!
                val touchCenterX = ((if (isDraggingDoctor) dragAnchorX else animDoctorX) * roomWidthPx)
                    .coerceIn(hitWidthPx / 2f + 4f, (roomWidthPx - hitWidthPx / 2f - 4f).coerceAtLeast(hitWidthPx / 2f + 4f))
                val touchFootY = ((if (isDraggingDoctor) dragAnchorY else animDoctorY) * roomHeightPx)
                    .coerceIn(docHeightPx + 4f, (roomHeightPx - 4f).coerceAtLeast(docHeightPx + 4f))
                val touchOffsetX = touchCenterX - hitWidthPx / 2f
                val touchOffsetY = touchFootY - docHeightPx

                // Independent dynamically-measured floating speech bubble positioned above doctor without clipping long text
                if (isInteractive) {
                    var bubbleW by remember { mutableIntStateOf(0) }
                    var bubbleH by remember { mutableIntStateOf(0) }
                    val fallbackBubbleWPx = with(density) { 180.dp.toPx() }
                    val fallbackBubbleHPx = with(density) { 44.dp.toPx() }
                    val measuredW = (if (bubbleW > 0) bubbleW.toFloat() else fallbackBubbleWPx)
                    val measuredH = (if (bubbleH > 0) bubbleH.toFloat() else fallbackBubbleHPx)
                    val bubbleGapPx = with(density) { (if (isDraggingDoctor) 14.dp else 6.dp).toPx() }
                    val topClearPx = with(density) { 8.dp.toPx() }
                    val bubbleX = (visualCenterX - measuredW / 2f)
                        .coerceIn(8f, (roomWidthPx - measuredW - 8f).coerceAtLeast(8f))
                    val bubbleY = (visualOffsetY - measuredH - bubbleGapPx)
                        .coerceIn(topClearPx, (roomHeightPx - measuredH - 8f).coerceAtLeast(topClearPx))

                    Box(
                        contentAlignment = Alignment.BottomCenter,
                        modifier = Modifier
                            .zIndex(21f)
                            .widthIn(min = 96.dp, max = 236.dp)
                            .onSizeChanged { size ->
                                bubbleW = size.width
                                bubbleH = size.height
                            }
                            .offset { IntOffset(bubbleX.roundToInt(), bubbleY.roundToInt()) }
                    ) {
                        AnimatedVisibility(
                            visible = speechLine.isNotBlank(),
                            enter = fadeIn(tween(180)),
                            exit = fadeOut(tween(180))
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.96f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isTapReacting || isDraggingDoctor) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    }
                                ),
                                shadowElevation = 3.dp
                            ) {
                                Text(
                                    text = speechLine,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // 4a. Floor Shadow Layer (strictly on walkable/floor surface, decoupled from character body!)
                if (!isLyingPose) {
                    Box(
                        modifier = Modifier
                            .zIndex(8f)
                            .width(shadowW)
                            .height(shadowH)
                            .offset {
                                IntOffset(
                                    x = shadowOffsetX.roundToInt(),
                                    y = shadowOffsetY.roundToInt()
                                )
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawOval(
                                color = Color(0xFF432E2B).copy(
                                    alpha = when {
                                        isDraggingDoctor -> 0.14f
                                        placedAnchorId == "anchor_chair_side" -> 0.18f
                                        else -> 0.25f
                                    }
                                ),
                                topLeft = Offset.Zero,
                                size = Size(size.width, size.height)
                            )
                        }
                    }
                }

                // 4b. Visual Doctor Layer (zIndex 9f when walking for 2.5D occlusion; 18f when lifted in mid-air!)
                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier
                        .zIndex(if (isDraggingDoctor) 18f else 9f)
                        .width(renderWidthDp)
                        .height(renderHeightDp)
                        .offset {
                            IntOffset(
                                x = visualOffsetX.roundToInt(),
                                y = visualOffsetY.roundToInt()
                            )
                        }
                ) {
                    val isSideSprite = currentSprite == WardDocSprite.WALK_A ||
                        currentSprite == WardDocSprite.WALK_MID ||
                        currentSprite == WardDocSprite.WALK_OPP ||
                        currentSprite == WardDocSprite.WALK_PASS ||
                        currentSprite == WardDocSprite.WALK_B ||
                        currentSprite == WardDocSprite.TURN
                    val spriteAspect = when (currentSprite) {
                        WardDocSprite.PICKED_UP -> 0.596f
                        WardDocSprite.SITTING_CHAIR -> 0.572f
                        WardDocSprite.LYING_BED -> 1.65f
                        else -> null
                    }

                    DoctorAvatarWithGear(
                        drawableRes = currentSprite.drawableRes,
                        gear = if (isLyingPose) DoctorGearConfig() else doctorGear,
                        isSideProfile = isSideSprite,
                        aspectRatioOverride = spriteAspect,
                        contentDescription = "${doctorNickname}在病房巡房",
                        modifier = Modifier
                            .height(renderHeightDp)
                            .graphicsLayer {
                                val liftPx = if (isDraggingDoctor) -8.dp.toPx() else 0f
                                translationY = with(density) { animWalkBob.dp.toPx() } + liftPx
                                rotationZ = if (isDraggingDoctor) {
                                    if (doctorFacingRight) 4f else -4f
                                } else {
                                    0f
                                }
                                val flipX = if (currentSprite == WardDocSprite.SITTING_CHAIR || isLyingPose) {
                                    1f // Sitting & lying sprites match bed/chair isometric orientation
                                } else if (doctorFacingRight) {
                                    -1f
                                } else {
                                    1f
                                }
                                scaleX = flipX * tapBounceScale
                                scaleY = tapBounceScale
                            }
                    )

                    // Subtle center interaction marker while dragging so player sees exact body-center drop point
                    if (isDraggingDoctor) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
                                .padding(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }

                // Dedicated gesture layer above occlusion overlays (zIndex 16f) with frozen layout offset during drag
                if (isInteractive) {
                    val halfDocHeightFrac = (docHeightPx * 0.5f) / roomHeightPx
                    val dragLiftFrac = with(density) { 8.dp.toPx() } / roomHeightPx

                    Box(
                        modifier = Modifier
                            .zIndex(16f)
                            .width(hitWidthDp)
                            .height(renderHeightDp)
                            .offset {
                                IntOffset(
                                    x = touchOffsetX.roundToInt(),
                                    y = touchOffsetY.roundToInt()
                                )
                            }
                            .pointerInput(roomWidthPx, roomHeightPx) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    isFingerDownOnDoctor = true
                                    isWalking = false
                                    isTurning = false
                                    val snapshotX = animDoctorX
                                    val snapshotY = animDoctorY
                                    doctorTargetX = snapshotX
                                    doctorTargetY = snapshotY
                                    dragAnchorX = snapshotX
                                    dragAnchorY = snapshotY
                                    dragAccumPx = Offset.Zero

                                    val outcome = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                        while (true) {
                                            val event = awaitPointerEvent(PointerEventPass.Main)
                                            val ch = event.changes.firstOrNull { it.id == down.id }
                                                ?: event.changes.firstOrNull()
                                            if (ch == null || !ch.pressed) {
                                                return@withTimeoutOrNull "TAP"
                                            }
                                            if ((ch.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                                return@withTimeoutOrNull "SCROLL_ABORT"
                                            }
                                        }
                                    }

                                    when (outcome) {
                                        "TAP" -> {
                                            isFingerDownOnDoctor = false
                                            doctorTapNonce += 1
                                            interactingTargetId = null
                                            when (placedAnchorId) {
                                                "anchor_chair_side" -> {
                                                    currentSprite = WardDocSprite.SITTING_CHAIR
                                                    speechLine = RehabCatalog.wardDoctorAnchorDropLine(
                                                        anchorId = "anchor_chair_side",
                                                        currentLine = speechLine
                                                    )
                                                    onDoctorPoseChanged(DoctorPose.OBSERVE)
                                                }
                                                "anchor_bed_pillow" -> {
                                                    currentSprite = WardDocSprite.LYING_BED
                                                    speechLine = RehabCatalog.wardDoctorAnchorDropLine(
                                                        anchorId = "anchor_bed_pillow",
                                                        currentLine = speechLine
                                                    )
                                                    onDoctorPoseChanged(DoctorPose.OBSERVE)
                                                }
                                                "anchor_bed_blanket" -> {
                                                    doctorFacingRight = false
                                                    currentSprite = WardDocSprite.INTERACT
                                                    speechLine = RehabCatalog.wardDoctorAnchorDropLine(
                                                        anchorId = "anchor_bed_blanket",
                                                        currentLine = speechLine
                                                    )
                                                    onDoctorPoseChanged(DoctorPose.OBSERVE)
                                                }
                                                else -> {
                                                    val reactionSprites = listOf(
                                                        WardDocSprite.POSE_APPROVE,
                                                        WardDocSprite.INTERACT,
                                                        WardDocSprite.IDLE_B,
                                                        WardDocSprite.POSE_SIP
                                                    )
                                                    val chosenSprite = reactionSprites[doctorTapNonce % reactionSprites.size]
                                                    currentSprite = chosenSprite
                                                    val tapCandidates = RehabCatalog.wardDoctorTapLines.filter { it != speechLine }
                                                    speechLine = tapCandidates.randomOrNull()
                                                        ?: RehabCatalog.wardDoctorTapLines.first()
                                                    onDoctorPoseChanged(
                                                        when (chosenSprite) {
                                                            WardDocSprite.POSE_SIP -> DoctorPose.SIPPING
                                                            WardDocSprite.POSE_APPROVE -> DoctorPose.APPROVE
                                                            else -> DoctorPose.OBSERVE
                                                        }
                                                    )
                                                }
                                            }
                                            latestOnDoctorTapped?.invoke()
                                        }

                                        "SCROLL_ABORT" -> {
                                            isFingerDownOnDoctor = false
                                        }

                                        else -> {
                                            // Long press confirmed — pick up doctor using DOCTOR'S BODY CENTER as interaction point
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            isPlayerHoldActive = true
                                            activeWalkDurationMs = 0
                                            isDraggingDoctor = true
                                            placedAnchorId = null
                                            interactingTargetId = null
                                            currentSprite = WardDocSprite.PICKED_UP

                                            val initCenterY = (snapshotY - halfDocHeightFrac - dragLiftFrac).coerceIn(0.16f, 0.86f)
                                            hoveredAnchorId = RehabCatalog.resolveWardDropAnchorOrNull(snapshotX, initCenterY)?.id
                                            val startCandidates = RehabCatalog.wardDoctorDragStartLines.filter { it != speechLine }
                                            speechLine = startCandidates.randomOrNull()
                                                ?: RehabCatalog.wardDoctorDragStartLines.first()

                                            while (true) {
                                                val event = awaitPointerEvent(PointerEventPass.Main)
                                                val ch = event.changes.firstOrNull { it.id == down.id }
                                                    ?: event.changes.firstOrNull()
                                                if (ch == null || !ch.pressed) break
                                                val delta = ch.positionChange()
                                                ch.consume()
                                                if (abs(delta.x) > 1.5f) {
                                                    doctorFacingRight = delta.x > 0f
                                                }
                                                dragAccumPx += delta
                                                val curFootX = (dragAnchorX + dragAccumPx.x / roomWidthPx).coerceIn(0.10f, 0.90f)
                                                val curFootY = (dragAnchorY + dragAccumPx.y / roomHeightPx).coerceIn(0.28f, 0.90f)
                                                val curCenterY = (curFootY - halfDocHeightFrac - dragLiftFrac).coerceIn(0.16f, 0.86f)
                                                hoveredAnchorId = RehabCatalog.resolveWardDropAnchorOrNull(curFootX, curCenterY)?.id
                                            }

                                            val rawDropFootX = (dragAnchorX + dragAccumPx.x / roomWidthPx).coerceIn(0.10f, 0.90f)
                                            val rawDropFootY = (dragAnchorY + dragAccumPx.y / roomHeightPx).coerceIn(0.28f, 0.90f)
                                            val dropCenterY = (rawDropFootY - halfDocHeightFrac - dragLiftFrac).coerceIn(0.16f, 0.86f)
                                            val targetAnchor = RehabCatalog.resolveWardDropAnchorOrNull(rawDropFootX, dropCenterY)

                                            val (finalX, finalY) = when (targetAnchor?.id) {
                                                "anchor_chair_side" -> 0.845f to 0.675f
                                                "anchor_bed_pillow" -> 0.345f to 0.545f
                                                else -> rawDropFootX to rawDropFootY
                                            }

                                            activeWalkDurationMs = 0
                                            placedAnchorId = targetAnchor?.id
                                            isPlayerHoldActive = true
                                            dragAnchorX = finalX
                                            dragAnchorY = finalY
                                            doctorTargetX = finalX
                                            doctorTargetY = finalY
                                            dragAccumPx = Offset.Zero
                                            hoveredAnchorId = null
                                            isDraggingDoctor = false
                                            isFingerDownOnDoctor = false
                                            dropHoldNonce += 1

                                            when (targetAnchor?.id) {
                                                "anchor_chair_side" -> {
                                                    doctorFacingRight = false
                                                    currentSprite = WardDocSprite.SITTING_CHAIR
                                                }
                                                "anchor_bed_pillow" -> {
                                                    doctorFacingRight = false
                                                    currentSprite = WardDocSprite.LYING_BED
                                                }
                                                "anchor_bed_blanket" -> {
                                                    doctorFacingRight = false
                                                    currentSprite = WardDocSprite.INTERACT
                                                }
                                                else -> {
                                                    currentSprite = WardDocSprite.IDLE_A
                                                }
                                            }
                                            onDoctorPoseChanged(DoctorPose.OBSERVE)
                                            speechLine = if (targetAnchor != null) {
                                                RehabCatalog.wardDoctorAnchorDropLine(
                                                    anchorId = targetAnchor.id,
                                                    currentLine = speechLine
                                                )
                                            } else {
                                                RehabCatalog.wardDoctorRandomDropLine(speechLine)
                                            }
                                        }
                                    }
                                }
                            }
                            .testTag("ward_doctor_sprite")
                    )
                }

                // 5. Separated 2.5D Furniture Layers:
                // Bed and Side Cabinet are strictly separated; Side Cabinet & Nightstand NEVER occlude the doctor!
                val perchedOnBed = isDraggingDoctor ||
                    placedAnchorId == "anchor_bed_pillow" ||
                    placedAnchorId == "anchor_bed_blanket"
                val perchedOnChair = isDraggingDoctor || placedAnchorId == "anchor_chair_side"
                val perchedAtIv = isDraggingDoctor || placedAnchorId == "anchor_bed_iv"

                if (!perchedOnBed && isDoctorBehindBed(liveDoctorX, liveDoctorY)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_ward_room_bg),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(11f)
                            .clip(BedOnlyOcclusionShape)
                    )
                }
                if (!perchedAtIv && isDoctorBehindIv(liveDoctorX, liveDoctorY)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_ward_room_bg),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(11.5f)
                            .clip(IvStandOcclusionShape)
                    )
                }
                if (!perchedOnChair && isDoctorBehindChair(liveDoctorX, liveDoctorY)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_ward_room_bg),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(11f)
                            .clip(ChairOcclusionShape)
                    )
                }
            }

            if (isInteractive) {
                val targetAnchorLabel = hoveredAnchorId?.let { id ->
                    RehabCatalog.anchorById(id)?.labelZh
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(horizontal = if (isFullBleed) 12.dp else 8.dp, vertical = 8.dp)
                        .zIndex(20f)
                ) {
                    Text(
                        text = when {
                            isDraggingDoctor && targetAnchorLabel != null ->
                                "🖐️ 抓起中・放開置於「$targetAnchorLabel」"
                            isDraggingDoctor ->
                                "🖐️ 抓起中：可放到椅子/床，或直接放回空地"
                            activePlacingItemId != null ->
                                "📍 請點選病房內的目標錨點放置道具"
                            isEditingProps ->
                                "🛠️ 道具編輯中：拖曳病房道具即可吸附擺放"
                            placements.isEmpty() ->
                                "✨ 長按抓起醫師換位置・點上方領道具佈置"
                            else ->
                                "✨ 長按抓起醫師換位置・開「編輯道具」可拖擺飾"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WardPlacedPropSprite(
    item: PharmacyItem,
    placement: WardPlacement,
    roomWidthPx: Float,
    roomHeightPx: Float,
    isInteractive: Boolean,
    isEditingProps: Boolean,
    isDoctorInteracting: Boolean,
    zOrder: Float,
    onDragStateChanged: (isDragging: Boolean, currentX: Float, currentY: Float) -> Unit,
    onCommitMove: (xFraction: Float, yFraction: Float) -> Unit
) {
    val density = LocalDensity.current
    var anchorX by remember(placement.itemId, placement.xFraction) {
        mutableFloatStateOf(placement.xFraction)
    }
    var anchorY by remember(placement.itemId, placement.yFraction) {
        mutableFloatStateOf(placement.yFraction)
    }
    var dragAccumPx by remember(placement.itemId) { mutableStateOf(Offset.Zero) }
    var isDragging by remember(placement.itemId) { mutableStateOf(false) }

    val propSizeDp = propDisplaySize(item.id, isInteractive)
    val propSizePx = with(density) { propSizeDp.toPx() }

    val liveX = (anchorX + dragAccumPx.x / roomWidthPx).coerceIn(0.08f, 0.92f)
    val liveY = (anchorY + dragAccumPx.y / roomHeightPx).coerceIn(0.14f, 0.86f)

    // Keep layout offset frozen during drag so pointerInput coords stay rock-steady
    val offsetX = (anchorX * roomWidthPx - propSizePx / 2f)
        .coerceIn(4f, (roomWidthPx - propSizePx - 4f).coerceAtLeast(4f))
    val offsetY = (anchorY * roomHeightPx - propSizePx / 2f)
        .coerceIn(4f, (roomHeightPx - propSizePx - 4f).coerceAtLeast(4f))

    val scale by animateFloatAsState(
        targetValue = when {
            isDragging -> 1.18f
            isDoctorInteracting -> 1.10f
            else -> 1f
        },
        animationSpec = tween(160),
        label = "prop_scale"
    )
    val wiggleDeg by animateFloatAsState(
        targetValue = if (isDoctorInteracting) 4f else 0f,
        animationSpec = tween(180),
        label = "prop_wiggle"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .zIndex(if (isDragging) 25f else zOrder)
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .then(
                if (isInteractive && isEditingProps) {
                    Modifier.pointerInput(placement.itemId, roomWidthPx, roomHeightPx) {
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                                dragAccumPx = Offset.Zero
                                onDragStateChanged(true, anchorX, anchorY)
                            },
                            onDragEnd = {
                                isDragging = false
                                val droppedX = (anchorX + dragAccumPx.x / roomWidthPx).coerceIn(0.08f, 0.92f)
                                val droppedY = (anchorY + dragAccumPx.y / roomHeightPx).coerceIn(0.14f, 0.86f)
                                val snapped = RehabCatalog.resolveWardDropAnchor(droppedX, droppedY)
                                dragAccumPx = Offset.Zero
                                anchorX = snapped.xFraction
                                anchorY = snapped.yFraction
                                onDragStateChanged(false, anchorX, anchorY)
                                onCommitMove(anchorX, anchorY)
                            },
                            onDragCancel = {
                                isDragging = false
                                dragAccumPx = Offset.Zero
                                onDragStateChanged(false, anchorX, anchorY)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragAccumPx += dragAmount
                                val nextX = (anchorX + dragAccumPx.x / roomWidthPx).coerceIn(0.08f, 0.92f)
                                val nextY = (anchorY + dragAccumPx.y / roomHeightPx).coerceIn(0.14f, 0.86f)
                                onDragStateChanged(true, nextX, nextY)
                            }
                        )
                    }
                } else {
                    Modifier
                }
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                translationX = if (isDragging) (liveX - anchorX) * roomWidthPx else 0f
                translationY = if (isDragging) (liveY - anchorY) * roomHeightPx else 0f
            }
        ) {
            if (isDragging || isDoctorInteracting) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.95f),
                    border = BorderStroke(
                        1.dp,
                        if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    ),
                    shadowElevation = 3.dp,
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Text(
                        text = if (isDragging) item.shortLabel else "✨ ${item.shortLabel}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier
                    .size(propSizeDp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationZ = wiggleDeg
                    }
            ) {
                if (item.id !in WALL_HANGING_IDS) {
                    Canvas(
                        modifier = Modifier
                            .width(propSizeDp * 0.72f)
                            .height(if (isInteractive) 6.dp else 4.dp)
                    ) {
                        drawOval(
                            color = Color(0xFF432E2B).copy(alpha = if (isDragging) 0.14f else 0.22f),
                            topLeft = Offset.Zero,
                            size = Size(size.width, size.height)
                        )
                    }
                }

                Image(
                    painter = painterResource(id = item.drawableRes),
                    contentDescription = item.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private val WALL_HANGING_IDS = setOf("miracle_banner", "vip_deed")

private fun propDisplaySize(itemId: String, isInteractive: Boolean): Dp {
    val liveDp = when (itemId) {
        "boba_iv_drip" -> 58.dp
        "auto_turn_arm" -> 46.dp
        "miracle_banner" -> 42.dp
        "vip_deed" -> 42.dp
        "antigravity_pillow" -> 44.dp
        "antisocial_blanket" -> 44.dp
        "long_straw" -> 40.dp
        "procrastination_permit" -> 38.dp
        "silent_alarm" -> 36.dp
        "pill_victory" -> 36.dp
        else -> 42.dp
    }
    return if (isInteractive) liveDp else (liveDp * 0.68f)
}
