package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val PastelPinkCreamColorScheme = lightColorScheme(
    primary = StrawberryPinkPrimary,
    onPrimary = Color.White,
    primaryContainer = StrawberryBlushContainer,
    onPrimaryContainer = OnStrawberryContainer,
    secondary = ButterPeachSecondary,
    onSecondary = Color.White,
    secondaryContainer = ButterCreamContainer,
    onSecondaryContainer = OnButterCreamContainer,
    tertiary = MintSageTertiary,
    onTertiary = Color.White,
    tertiaryContainer = MintSageContainer,
    onTertiaryContainer = OnMintSageContainer,
    background = CreamIvoryPaper,
    onBackground = CocoaInk,
    surface = CreamIvoryPaper,
    onSurface = CocoaInk,
    surfaceVariant = CreamButterSurface,
    onSurfaceVariant = MutedCocoaRose,
    surfaceContainerLowest = CrispMilkWhite,
    surfaceContainerLow = BlushPinkSurface,
    surfaceContainer = SoftRoseContainer,
    surfaceContainerHigh = PastelPinkContainer,
    outline = SoftPinkOutline,
    outlineVariant = SoftPinkOutlineVariant
)

val FeifeiShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(30.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun FeifeiRehabTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PastelPinkCreamColorScheme,
        typography = Typography,
        shapes = FeifeiShapes,
        content = content
    )
}
