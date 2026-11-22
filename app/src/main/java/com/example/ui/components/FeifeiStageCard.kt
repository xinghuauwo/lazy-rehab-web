package com.example.ui.components

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DoctorGearConfig
import com.example.data.DoctorPose
import com.example.data.FeifeiStage
import com.example.data.PharmacyItem
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeifeiStageCard(
    feifeiNickname: String,
    todayCount: Long,
    totalCompleted: Long,
    quote: String,
    patCounter: Int,
    stagePose: DoctorPose,
    doctorGear: DoctorGearConfig,
    equippedItems: List<PharmacyItem>,
    onPatFeifei: () -> Unit,
    onOpenDoctorEquip: () -> Unit,
    onOpenPharmacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stage = FeifeiStage.fromTodayCount(todayCount)
    var squishing by remember { mutableStateOf(false) }

    LaunchedEffect(patCounter) {
        if (patCounter > 0) {
            squishing = true
            delay(170L)
            squishing = false
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "mascotFloat")
    val floatOffsetY by infiniteTransition.animateFloat(
        initialValue = if (stage == FeifeiStage.FLAT) 2f else -5f,
        targetValue = if (stage == FeifeiStage.FLAT) -2f else 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (stage) {
                    FeifeiStage.FLAT -> 2400
                    FeifeiStage.SIT -> 1800
                    FeifeiStage.CHEER -> 950
                    FeifeiStage.GLOW -> 1100
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )
    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloPulse"
    )

    val scaleX by animateFloatAsState(
        targetValue = if (squishing) 1.10f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "mascotScaleX"
    )
    val scaleY by animateFloatAsState(
        targetValue = if (squishing) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "mascotScaleY"
    )
    val tiltAngle by animateFloatAsState(
        targetValue = when {
            squishing -> if (patCounter % 2 == 0) 5f else -5f
            else -> 0f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "mascotTilt"
    )

    val stageHaloColor = MaterialTheme.colorScheme.primaryContainer
    val stagePinkAccent = MaterialTheme.colorScheme.secondaryContainer
    val stageBgTop = MaterialTheme.colorScheme.surfaceContainerLow
    val stageBgBottom = MaterialTheme.colorScheme.surfaceVariant
    val bedOutlineColor = MaterialTheme.colorScheme.outline

    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = when (stage) {
                                FeifeiStage.FLAT -> Icons.Rounded.Bedtime
                                FeifeiStage.SIT -> Icons.Rounded.LocalCafe
                                FeifeiStage.CHEER -> Icons.Rounded.Favorite
                                FeifeiStage.GLOW -> Icons.Rounded.AutoAwesome
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = feifeiNickname,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onOpenPharmacy)
                        .testTag("stage_antibody_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalPharmacy,
                            contentDescription = "前往院內藥局",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "院內藥局",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Interactive doctor & hospital bed stage — tap doctor to open coat & stethoscope menu
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(232.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = {
                            onPatFeifei()
                            onOpenDoctorEquip()
                        }
                    )
                    .testTag("feifei_mascot_card"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(stageBgTop, stageBgBottom)
                        )
                    )
                    val center = Offset(size.width / 2f, size.height * 0.48f)
                    val baseRadius = size.minDimension * 0.42f * haloPulse
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                stageHaloColor.copy(alpha = if (stage == FeifeiStage.GLOW) 0.95f else 0.72f),
                                stagePinkAccent.copy(alpha = 0.28f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = baseRadius * 1.45f
                        ),
                        radius = baseRadius * 1.45f,
                        center = center
                    )

                    val bedWidth = size.width * 0.64f
                    val bedLeft = (size.width - bedWidth) / 2f
                    val bedTop = size.height * 0.74f
                    drawRoundRect(
                        color = stagePinkAccent.copy(alpha = 0.65f),
                        topLeft = Offset(bedLeft, bedTop),
                        size = Size(bedWidth, size.height * 0.16f),
                        cornerRadius = CornerRadius(28f, 28f)
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.78f),
                        topLeft = Offset(bedLeft + bedWidth * 0.14f, bedTop - 10f),
                        size = Size(bedWidth * 0.72f, 22f),
                        cornerRadius = CornerRadius(14f, 14f)
                    )
                    drawRoundRect(
                        color = bedOutlineColor.copy(alpha = 0.4f),
                        topLeft = Offset(bedLeft, bedTop),
                        size = Size(bedWidth, size.height * 0.16f),
                        cornerRadius = CornerRadius(28f, 28f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                AnimatedContent(
                    targetState = stagePose,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "mascotStageTransition"
                ) { currentPose ->
                    DoctorAvatarWithGear(
                        drawableRes = currentPose.drawableRes,
                        gear = doctorGear,
                        contentDescription = "${feifeiNickname}目前穿著${doctorGear.coat.titleZh}、${doctorGear.stethoscope.titleZh}與${doctorGear.headwear.titleZh}",
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .height(174.dp)
                            .graphicsLayer {
                                this.scaleX = scaleX
                                this.scaleY = scaleY
                                this.translationY = floatOffsetY
                                this.rotationZ = tiltAngle
                            }
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Checkroom,
                            contentDescription = "點醫師換配件",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "點醫師換配件",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Speech bubble (tapping directly cycles next doctor quote)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = onPatFeifei
                    )
                    .testTag("room_doctor_quote_card")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "「$quote」",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${stage.subtitleZh}（今日 $todayCount 件・累計 $totalCompleted 件）",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

