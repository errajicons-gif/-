package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val TaounateFestivalColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = RoyalBlueDeep,
    primaryContainer = GoldContainer,
    onPrimaryContainer = GoldBright,
    secondary = GoldBright,
    onSecondary = RoyalBlueDeep,
    secondaryContainer = RoyalBlueElevated,
    onSecondaryContainer = IvoryWhite,
    tertiary = EmeraldSuccess,
    onTertiary = PureWhite,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = PureWhite,
    background = RoyalBlueDeep,
    onBackground = IvoryWhite,
    surface = RoyalBlueSurface,
    onSurface = IvoryWhite,
    surfaceVariant = RoyalBlueMid,
    onSurfaceVariant = SoftSlateText,
    outline = GoldPrimary.copy(alpha = 0.5f),
    outlineVariant = RoyalBlueElevated,
    error = CoralError,
    onError = PureWhite,
    errorContainer = CoralErrorContainer,
    onErrorContainer = IvoryWhite
)

private val FestivalShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun TaounatePhotoBoothTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = TaounateFestivalColorScheme,
            typography = AppTypography,
            shapes = FestivalShapes,
            content = content
        )
    }
}
