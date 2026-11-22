package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.data.DoctorCoatStyle
import com.example.data.DoctorGearConfig
import com.example.data.DoctorHeadwearStyle
import com.example.data.DoctorStethoscopeStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val SpriteOutlineBrown = Color(0xFF4B3532)

/**
 * Renders the white-haired doctor sprite with crisp Canvas-drawn white coat & stethoscope accessories
 * aligned to the 285x512 (0.556f aspect) sprite proportions.
 */
@Composable
fun DoctorAvatarWithGear(
    @DrawableRes drawableRes: Int,
    gear: DoctorGearConfig,
    contentDescription: String?,
    isSideProfile: Boolean = false,
    aspectRatioOverride: Float? = null,
    modifier: Modifier = Modifier
) {
    val aspect = aspectRatioOverride ?: if (isSideProfile) 0.50f else 0.556f
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.aspectRatio(aspect, matchHeightConstraintsFirst = true)
    ) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        if (gear.coat != DoctorCoatStyle.CLASSIC_WHITE ||
            gear.stethoscope != DoctorStethoscopeStyle.DEFAULT_HEART ||
            gear.headwear != DoctorHeadwearStyle.NONE
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val outlineW = (size.minDimension * 0.015f).coerceAtLeast(1.1f)
                drawCoatAccessory(
                    coat = gear.coat,
                    isSideProfile = isSideProfile,
                    outlineW = outlineW
                )
                drawStethoscopeAccessory(
                    stethoscope = gear.stethoscope,
                    isSideProfile = isSideProfile,
                    outlineW = outlineW
                )
                drawHeadwearAccessory(
                    headwear = gear.headwear,
                    isSideProfile = isSideProfile,
                    outlineW = outlineW
                )
            }
        }
    }
}

private fun DrawScope.drawCoatAccessory(
    coat: DoctorCoatStyle,
    isSideProfile: Boolean,
    outlineW: Float
) {
    val w = size.width
    val h = size.height
    when (coat) {
        DoctorCoatStyle.CLASSIC_WHITE -> Unit

        DoctorCoatStyle.STAR_DIRECTOR -> {
            val collarPinX = if (isSideProfile) w * 0.62f else w * 0.355f
            val collarPinY = h * 0.485f
            val pinR = w * 0.048f
            drawStarBadge(
                center = Offset(collarPinX, collarPinY),
                outerRadius = pinR,
                fill = Color(0xFFFFD54F),
                outlineW = outlineW * 0.9f
            )

            if (!isSideProfile) {
                // Pocket pen with tiny star topper peeking out of right coat pocket
                val penStart = Offset(w * 0.69f, h * 0.618f)
                val penEnd = Offset(w * 0.72f, h * 0.588f)
                drawLine(
                    color = Color(0xFF7986CB),
                    start = penStart,
                    end = penEnd,
                    strokeWidth = outlineW * 2.1f,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = Color(0xFFFFD54F),
                    radius = w * 0.022f,
                    center = penEnd
                )
                drawCircle(
                    color = SpriteOutlineBrown,
                    radius = w * 0.022f,
                    center = penEnd,
                    style = Stroke(width = outlineW * 0.65f)
                )

                // Soft pastel sky-blue cuff trim on both coat sleeves
                drawRoundRect(
                    color = Color(0xFF90CAF9).copy(alpha = 0.85f),
                    topLeft = Offset(w * 0.25f, h * 0.578f),
                    size = Size(w * 0.09f, h * 0.015f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = Color(0xFF90CAF9).copy(alpha = 0.85f),
                    topLeft = Offset(w * 0.67f, h * 0.578f),
                    size = Size(w * 0.09f, h * 0.015f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }

        DoctorCoatStyle.CAT_PAW_COAT -> {
            // Cute pink bow tie under chin
            if (!isSideProfile) {
                drawBowTie(
                    center = Offset(w * 0.50f, h * 0.475f),
                    halfWidth = w * 0.062f,
                    halfHeight = w * 0.025f,
                    fill = Color(0xFFF48FB1),
                    outlineW = outlineW * 0.85f
                )
                // Embroidered paw pads on left & right pockets
                drawPawPrint(Offset(w * 0.32f, h * 0.65f), w * 0.036f, Color(0xFFF8BBD0), outlineW * 0.75f)
                drawPawPrint(Offset(w * 0.68f, h * 0.65f), w * 0.036f, Color(0xFFF8BBD0), outlineW * 0.75f)
            } else {
                drawPawPrint(Offset(w * 0.59f, h * 0.635f), w * 0.042f, Color(0xFFF8BBD0), outlineW * 0.75f)
            }
        }

        DoctorCoatStyle.PASTEL_MINT_COAT -> {
            // Mint-green clinic trim & "休診" name tag on coat chest
            val mint = Color(0xFF80CBC4)
            if (!isSideProfile) {
                drawLine(
                    color = mint,
                    start = Offset(w * 0.365f, h * 0.470f),
                    end = Offset(w * 0.395f, h * 0.625f),
                    strokeWidth = outlineW * 1.9f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = mint,
                    start = Offset(w * 0.635f, h * 0.470f),
                    end = Offset(w * 0.605f, h * 0.625f),
                    strokeWidth = outlineW * 1.9f,
                    cap = StrokeCap.Round
                )
                // Name tag badge
                val tagLeft = w * 0.635f
                val tagTop = h * 0.568f
                val tagSize = Size(w * 0.115f, h * 0.030f)
                drawRoundRect(
                    color = Color(0xFFE0F2F1),
                    topLeft = Offset(tagLeft, tagTop),
                    size = tagSize,
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = SpriteOutlineBrown,
                    topLeft = Offset(tagLeft, tagTop),
                    size = tagSize,
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(width = outlineW * 0.7f)
                )
                drawLine(
                    color = Color(0xFF00897B),
                    start = Offset(tagLeft + tagSize.width * 0.22f, tagTop + tagSize.height * 0.5f),
                    end = Offset(tagLeft + tagSize.width * 0.78f, tagTop + tagSize.height * 0.5f),
                    strokeWidth = outlineW * 0.9f,
                    cap = StrokeCap.Round
                )
            } else {
                drawRoundRect(
                    color = mint,
                    topLeft = Offset(w * 0.55f, h * 0.565f),
                    size = Size(w * 0.12f, h * 0.025f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }
    }
}

private fun DrawScope.drawStethoscopeAccessory(
    stethoscope: DoctorStethoscopeStyle,
    isSideProfile: Boolean,
    outlineW: Float
) {
    val w = size.width
    val h = size.height
    if (stethoscope == DoctorStethoscopeStyle.DEFAULT_HEART) return

    val tubeColor = when (stethoscope) {
        DoctorStethoscopeStyle.GOLD_VIP -> Color(0xFFFFCA28)
        DoctorStethoscopeStyle.CAT_BELL -> Color(0xFF81D4FA)
        DoctorStethoscopeStyle.BOBA_SCOPE -> Color(0xFFD7A86E)
        DoctorStethoscopeStyle.DEFAULT_HEART -> Color.Gray
    }

    // Repaint neck tube path cleanly over base gray stethoscope
    val neckPath = Path().apply {
        if (!isSideProfile) {
            moveTo(w * 0.375f, h * 0.525f)
            cubicTo(
                w * 0.385f, h * 0.452f,
                w * 0.425f, h * 0.448f,
                w * 0.438f, h * 0.478f
            )
            moveTo(w * 0.578f, h * 0.460f)
            cubicTo(
                w * 0.605f, h * 0.458f,
                w * 0.625f, h * 0.486f,
                w * 0.625f, h * 0.520f
            )
        } else {
            moveTo(w * 0.495f, h * 0.460f)
            cubicTo(
                w * 0.555f, h * 0.465f,
                w * 0.600f, h * 0.492f,
                w * 0.608f, h * 0.525f
            )
        }
    }
    drawPath(
        path = neckPath,
        color = SpriteOutlineBrown,
        style = Stroke(width = outlineW * 3.1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    drawPath(
        path = neckPath,
        color = tubeColor,
        style = Stroke(width = outlineW * 1.75f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    val discCenter = if (isSideProfile) Offset(w * 0.610f, h * 0.528f) else Offset(w * 0.626f, h * 0.528f)
    val discRadius = w * 0.045f

    when (stethoscope) {
        DoctorStethoscopeStyle.GOLD_VIP -> {
            drawCircle(color = Color(0xFFFFD54F), radius = discRadius, center = discCenter)
            drawCircle(
                color = SpriteOutlineBrown,
                radius = discRadius,
                center = discCenter,
                style = Stroke(width = outlineW * 0.85f)
            )
            drawStarBadge(
                center = discCenter,
                outerRadius = discRadius * 0.72f,
                fill = Color(0xFFFFF8E1),
                outlineW = outlineW * 0.55f
            )
        }

        DoctorStethoscopeStyle.CAT_BELL -> {
            // Two little cat ears above the bell chest piece
            val earOffset = discRadius * 0.62f
            val leftEar = Path().apply {
                moveTo(discCenter.x - earOffset * 1.1f, discCenter.y - discRadius * 0.4f)
                lineTo(discCenter.x - earOffset * 1.35f, discCenter.y - discRadius * 1.35f)
                lineTo(discCenter.x - earOffset * 0.25f, discCenter.y - discRadius * 0.85f)
                close()
            }
            val rightEar = Path().apply {
                moveTo(discCenter.x + earOffset * 1.1f, discCenter.y - discRadius * 0.4f)
                lineTo(discCenter.x + earOffset * 1.35f, discCenter.y - discRadius * 1.35f)
                lineTo(discCenter.x + earOffset * 0.25f, discCenter.y - discRadius * 0.85f)
                close()
            }
            drawPath(leftEar, color = Color(0xFFFFCDD2))
            drawPath(leftEar, color = SpriteOutlineBrown, style = Stroke(width = outlineW * 0.75f))
            drawPath(rightEar, color = Color(0xFFFFCDD2))
            drawPath(rightEar, color = SpriteOutlineBrown, style = Stroke(width = outlineW * 0.75f))

            // Golden jingle bell body
            drawCircle(color = Color(0xFFFFE082), radius = discRadius, center = discCenter)
            drawCircle(
                color = SpriteOutlineBrown,
                radius = discRadius,
                center = discCenter,
                style = Stroke(width = outlineW * 0.85f)
            )
            // Bell slit
            drawLine(
                color = SpriteOutlineBrown,
                start = Offset(discCenter.x - discRadius * 0.48f, discCenter.y + discRadius * 0.22f),
                end = Offset(discCenter.x + discRadius * 0.48f, discCenter.y + discRadius * 0.22f),
                strokeWidth = outlineW * 0.85f,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = SpriteOutlineBrown,
                radius = discRadius * 0.18f,
                center = Offset(discCenter.x, discCenter.y + discRadius * 0.1f)
            )
        }

        DoctorStethoscopeStyle.BOBA_SCOPE -> {
            // Mini milk tea cup chest piece with straw & boba pearls
            val cupW = discRadius * 1.65f
            val cupH = discRadius * 1.9f
            val cupTop = discCenter.y - cupH * 0.45f
            val cupLeft = discCenter.x - cupW / 2f

            // Straw
            drawLine(
                color = Color(0xFF9C27B0).copy(alpha = 0.75f),
                start = Offset(discCenter.x + cupW * 0.12f, cupTop + cupH * 0.25f),
                end = Offset(discCenter.x + cupW * 0.24f, cupTop - cupH * 0.42f),
                strokeWidth = outlineW * 1.3f,
                cap = StrokeCap.Round
            )
            // Cup body
            drawRoundRect(
                color = Color(0xFFE6C29F),
                topLeft = Offset(cupLeft, cupTop),
                size = Size(cupW, cupH),
                cornerRadius = CornerRadius(5f, 5f)
            )
            drawRoundRect(
                color = SpriteOutlineBrown,
                topLeft = Offset(cupLeft, cupTop),
                size = Size(cupW, cupH),
                cornerRadius = CornerRadius(5f, 5f),
                style = Stroke(width = outlineW * 0.8f)
            )
            // Boba pearls
            val pearlR = discRadius * 0.17f
            drawCircle(SpriteOutlineBrown, pearlR, Offset(discCenter.x - cupW * 0.22f, cupTop + cupH * 0.72f))
            drawCircle(SpriteOutlineBrown, pearlR, Offset(discCenter.x, cupTop + cupH * 0.76f))
            drawCircle(SpriteOutlineBrown, pearlR, Offset(discCenter.x + cupW * 0.22f, cupTop + cupH * 0.72f))
        }

        DoctorStethoscopeStyle.DEFAULT_HEART -> Unit
    }
}

private fun DrawScope.drawStarBadge(
    center: Offset,
    outerRadius: Float,
    fill: Color,
    outlineW: Float
) {
    val innerRadius = outerRadius * 0.45f
    val path = Path()
    for (i in 0 until 10) {
        val angle = -PI / 2.0 + i * (PI / 5.0)
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val px = center.x + (r * cos(angle)).toFloat()
        val py = center.y + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, color = fill)
    drawPath(path, color = SpriteOutlineBrown, style = Stroke(width = outlineW, join = StrokeJoin.Round))
}

private fun DrawScope.drawBowTie(
    center: Offset,
    halfWidth: Float,
    halfHeight: Float,
    fill: Color,
    outlineW: Float
) {
    val path = Path().apply {
        moveTo(center.x, center.y)
        lineTo(center.x - halfWidth, center.y - halfHeight)
        lineTo(center.x - halfWidth, center.y + halfHeight)
        close()
        moveTo(center.x, center.y)
        lineTo(center.x + halfWidth, center.y - halfHeight)
        lineTo(center.x + halfWidth, center.y + halfHeight)
        close()
    }
    drawPath(path, color = fill)
    drawPath(path, color = SpriteOutlineBrown, style = Stroke(width = outlineW, join = StrokeJoin.Round))
    drawCircle(color = fill, radius = halfHeight * 0.55f, center = center)
    drawCircle(color = SpriteOutlineBrown, radius = halfHeight * 0.55f, center = center, style = Stroke(width = outlineW * 0.85f))
}

private fun DrawScope.drawPawPrint(
    center: Offset,
    padRadius: Float,
    fill: Color,
    outlineW: Float
) {
    val toeR = padRadius * 0.42f
    val toes = listOf(
        Offset(center.x - padRadius * 0.95f, center.y - padRadius * 0.85f),
        Offset(center.x, center.y - padRadius * 1.15f),
        Offset(center.x + padRadius * 0.95f, center.y - padRadius * 0.85f)
    )
    toes.forEach { t ->
        drawCircle(color = fill, radius = toeR, center = t)
        drawCircle(color = SpriteOutlineBrown, radius = toeR, center = t, style = Stroke(width = outlineW * 0.7f))
    }
    drawCircle(color = fill, radius = padRadius, center = center)
    drawCircle(color = SpriteOutlineBrown, radius = padRadius, center = center, style = Stroke(width = outlineW * 0.8f))
}

private fun DrawScope.drawHeadwearAccessory(
    headwear: DoctorHeadwearStyle,
    isSideProfile: Boolean,
    outlineW: Float
) {
    if (headwear == DoctorHeadwearStyle.NONE) return
    val w = size.width
    val h = size.height
    when (headwear) {
        DoctorHeadwearStyle.NONE -> Unit

        DoctorHeadwearStyle.CAT_EARS -> {
            val earWidth = w * 0.17f
            val earHeight = h * 0.088f
            val centers = if (!isSideProfile) {
                listOf(Offset(w * 0.34f, h * 0.115f), Offset(w * 0.66f, h * 0.115f))
            } else {
                listOf(Offset(w * 0.44f, h * 0.112f), Offset(w * 0.61f, h * 0.120f))
            }
            centers.forEachIndexed { idx, base ->
                val tilt = if (!isSideProfile) (if (idx == 0) -0.22f else 0.22f) else (if (idx == 0) -0.15f else 0.18f)
                val tipX = base.x + earWidth * tilt
                val tipY = base.y - earHeight
                val outer = Path().apply {
                    moveTo(base.x - earWidth * 0.55f, base.y)
                    lineTo(tipX, tipY)
                    lineTo(base.x + earWidth * 0.55f, base.y)
                    close()
                }
                val inner = Path().apply {
                    moveTo(base.x - earWidth * 0.28f, base.y - earHeight * 0.08f)
                    lineTo(tipX, base.y - earHeight * 0.68f)
                    lineTo(base.x + earWidth * 0.28f, base.y - earHeight * 0.08f)
                    close()
                }
                drawPath(outer, color = Color(0xFFF6F5FA))
                drawPath(outer, color = SpriteOutlineBrown, style = Stroke(width = outlineW * 1.05f, join = StrokeJoin.Round))
                drawPath(inner, color = Color(0xFFF8BBD0))
            }
        }

        DoctorHeadwearStyle.HEAD_MIRROR -> {
            // Silver headband across upper forehead + iconic reflective doctor head-mirror disc
            val bandY = h * 0.138f
            val bandPath = Path().apply {
                if (!isSideProfile) {
                    moveTo(w * 0.23f, bandY + h * 0.025f)
                    quadraticTo(w * 0.50f, bandY - h * 0.02f, w * 0.77f, bandY + h * 0.025f)
                } else {
                    moveTo(w * 0.26f, bandY + h * 0.02f)
                    quadraticTo(w * 0.52f, bandY - h * 0.015f, w * 0.74f, bandY + h * 0.022f)
                }
            }
            drawPath(
                path = bandPath,
                color = SpriteOutlineBrown,
                style = Stroke(width = outlineW * 2.5f, cap = StrokeCap.Round)
            )
            drawPath(
                path = bandPath,
                color = Color(0xFFDCE4EC),
                style = Stroke(width = outlineW * 1.4f, cap = StrokeCap.Round)
            )

            val discCenter = if (!isSideProfile) Offset(w * 0.48f, h * 0.125f) else Offset(w * 0.62f, h * 0.128f)
            val discR = w * 0.092f
            drawCircle(color = Color(0xFFE3EEF8), radius = discR, center = discCenter)
            drawCircle(color = Color(0xFFB0C4DE).copy(alpha = 0.45f), radius = discR * 0.74f, center = discCenter)
            // Specular highlight arc
            drawLine(
                color = Color.White,
                start = Offset(discCenter.x - discR * 0.48f, discCenter.y - discR * 0.35f),
                end = Offset(discCenter.x - discR * 0.12f, discCenter.y - discR * 0.55f),
                strokeWidth = outlineW * 1.1f,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = SpriteOutlineBrown,
                radius = discR,
                center = discCenter,
                style = Stroke(width = outlineW * 0.95f)
            )
            // Center peephole
            drawCircle(color = SpriteOutlineBrown, radius = discR * 0.19f, center = discCenter)
        }

        DoctorHeadwearStyle.FLOWER_CROWN -> {
            val crownY = h * 0.105f
            val vinePath = Path().apply {
                moveTo(w * 0.26f, crownY + h * 0.032f)
                quadraticTo(w * 0.50f, crownY - h * 0.020f, w * 0.74f, crownY + h * 0.032f)
            }
            drawPath(
                path = vinePath,
                color = Color(0xFF81C784),
                style = Stroke(width = outlineW * 2.0f, cap = StrokeCap.Round)
            )
            val flowers = if (!isSideProfile) {
                listOf(
                    Triple(Offset(w * 0.30f, crownY + h * 0.020f), Color(0xFFFFCDD2), w * 0.044f),
                    Triple(Offset(w * 0.40f, crownY + h * 0.004f), Color(0xFFFFF9C4), w * 0.048f),
                    Triple(Offset(w * 0.50f, crownY - h * 0.004f), Color(0xFFFFFFFF), w * 0.052f),
                    Triple(Offset(w * 0.60f, crownY + h * 0.005f), Color(0xFFBBDEFB), w * 0.046f),
                    Triple(Offset(w * 0.70f, crownY + h * 0.020f), Color(0xFFFFCDD2), w * 0.044f)
                )
            } else {
                listOf(
                    Triple(Offset(w * 0.36f, crownY + h * 0.015f), Color(0xFFFFCDD2), w * 0.046f),
                    Triple(Offset(w * 0.51f, crownY + h * 0.002f), Color(0xFFFFFFFF), w * 0.052f),
                    Triple(Offset(w * 0.66f, crownY + h * 0.016f), Color(0xFFBBDEFB), w * 0.046f)
                )
            }
            flowers.forEach { (center, petalColor, r) ->
                for (i in 0 until 5) {
                    val angle = i * (2.0 * PI / 5.0)
                    val px = center.x + (r * 0.55f * cos(angle)).toFloat()
                    val py = center.y + (r * 0.55f * sin(angle)).toFloat()
                    drawCircle(color = petalColor, radius = r * 0.46f, center = Offset(px, py))
                    drawCircle(
                        color = SpriteOutlineBrown.copy(alpha = 0.6f),
                        radius = r * 0.46f,
                        center = Offset(px, py),
                        style = Stroke(width = outlineW * 0.4f)
                    )
                }
                drawCircle(color = Color(0xFFFFD54F), radius = r * 0.34f, center = center)
            }
        }

        DoctorHeadwearStyle.PAW_CLIP -> {
            val clipCenter = if (!isSideProfile) Offset(w * 0.675f, h * 0.165f) else Offset(w * 0.585f, h * 0.165f)
            // Hairpin bar
            drawRoundRect(
                color = Color(0xFFE0E0E0),
                topLeft = Offset(clipCenter.x - w * 0.065f, clipCenter.y - h * 0.005f),
                size = Size(w * 0.13f, h * 0.014f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = SpriteOutlineBrown,
                topLeft = Offset(clipCenter.x - w * 0.065f, clipCenter.y - h * 0.005f),
                size = Size(w * 0.13f, h * 0.014f),
                cornerRadius = CornerRadius(6f, 6f),
                style = Stroke(width = outlineW * 0.65f)
            )
            drawPawPrint(
                center = Offset(clipCenter.x + w * 0.015f, clipCenter.y - h * 0.004f),
                padRadius = w * 0.042f,
                fill = Color(0xFFFFABBE),
                outlineW = outlineW * 0.85f
            )
        }
    }
}

