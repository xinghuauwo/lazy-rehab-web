package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.Locale

fun parseHourMinute(raw: String, fallbackHour: Int = 9, fallbackMinute: Int = 0): Pair<Int, Int> {
    val parts = raw.trim().split(":")
    val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: fallbackHour.coerceIn(0, 23)
    val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: fallbackMinute.coerceIn(0, 59)
    return h to m
}

fun formatHourMinute(hour: Int, minute: Int): String =
    String.format(Locale.US, "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))

@Composable
fun WheelTimePicker(
    hour: Int,
    minute: Int,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeHour = hour.coerceIn(0, 23)
    val safeMinute = minute.coerceIn(0, 59)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Center highlight band
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                NumericWheelColumn(
                    values = (0..23).toList(),
                    selectedValue = safeHour,
                    onValueSelected = { newH ->
                        if (newH != safeHour) {
                            onTimeChange(newH, safeMinute)
                        }
                    },
                    modifier = Modifier.width(82.dp)
                )

                Text(
                    text = ":",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                NumericWheelColumn(
                    values = (0..59).toList(),
                    selectedValue = safeMinute,
                    onValueSelected = { newM ->
                        if (newM != safeMinute) {
                            onTimeChange(safeHour, newM)
                        }
                    },
                    modifier = Modifier.width(82.dp)
                )
            }
        }
    }
}

@Composable
private fun NumericWheelColumn(
    values: List<Int>,
    selectedValue: Int,
    onValueSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemHeight = 36.dp
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }
    val initialIndex = values.indexOf(selectedValue).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapFling = rememberSnapFlingBehavior(lazyListState = listState)
    val scope = rememberCoroutineScope()

    val centerIndex by remember {
        derivedStateOf {
            val firstIdx = listState.firstVisibleItemIndex
            val offsetPx = listState.firstVisibleItemScrollOffset
            val candidate = if (offsetPx > itemHeightPx * 0.5f) firstIdx + 1 else firstIdx
            candidate.coerceIn(0, values.lastIndex)
        }
    }

    LaunchedEffect(selectedValue) {
        val targetIdx = values.indexOf(selectedValue).coerceAtLeast(0)
        if (!listState.isScrollInProgress && centerIndex != targetIdx) {
            listState.scrollToItem(targetIdx)
        }
    }

    LaunchedEffect(centerIndex, listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val snappedValue = values[centerIndex]
            if (snappedValue != selectedValue) {
                onValueSelected(snappedValue)
            }
        }
    }

    Box(
        modifier = modifier.height(itemHeight * 3)
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = snapFling,
            contentPadding = PaddingValues(vertical = itemHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(values.size, key = { values[it] }) { idx ->
                val value = values[idx]
                val isCenter = idx == centerIndex
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            scope.launch {
                                listState.animateScrollToItem(idx)
                            }
                            onValueSelected(value)
                        }
                ) {
                    Text(
                        text = String.format(Locale.US, "%02d", value),
                        fontSize = if (isCenter) 20.sp else 15.sp,
                        fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCenter) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.alpha(if (isCenter) 1f else 0.52f)
                    )
                }
            }
        }

        // Subtle top/bottom fade gradients
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceContainerLowest,
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    )
                )
        )
    }
}
