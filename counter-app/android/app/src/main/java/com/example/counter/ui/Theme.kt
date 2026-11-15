package com.example.counter.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Secondary text must stay readable on both the warm light page and the dark page. */
private val LocalMutedColor = staticCompositionLocalOf { Color(0xFF6E6E76) }

object AppColors {
    val Orange = Color(0xFFF5921E)
    val OrangeDark = Color(0xFFC86A00)
    val OrangeSoft = Color(0xFFFFE9CF)
    val Blue = Color(0xFF2F7BF6)
    val PageLight = Color(0xFFF5F5F2)
    val PageDark = Color(0xFF1B1B1D)
    val CardLight = Color(0xFFFFFFFF)
    val CardDark = Color(0xFF2B2B2E)
    val ChipLight = Color(0xFFEDEDE9)
    val ChipDark = Color(0xFF343437)
    val TextLight = Color(0xFF1C1C1E)
    val TextDark = Color(0xFFF2F2F7)
    val MutedLight = Color(0xFF6E6E76)
    val MutedDark = Color(0xFFB9B9C4)
    val Muted: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalMutedColor.current
    val LineLight = Color(0xFFE5E5E0)
    val LineDark = Color(0xFF3D3D40)
    val Danger = Color(0xFFD6453D)
    val Green = Color(0xFF34C759)

    val PaletteArgb = listOf(
        0xFFF5921E, 0xFF9B9B9B, 0xFF3B82F6, 0xFF34C759,
        0xFFAF52DE, 0xFFFF3B30, 0xFFFF2D55, 0xFF30B0C7
    )
}

@Composable
fun pageBackground(dark: Boolean): Color = if (dark) AppColors.PageDark else AppColors.PageLight

@Composable
fun cardBackground(dark: Boolean): Color = if (dark) AppColors.CardDark else AppColors.CardLight

@Composable
fun textColor(dark: Boolean): Color = if (dark) AppColors.TextDark else AppColors.TextLight

@Composable
fun chipColor(dark: Boolean): Color = if (dark) AppColors.ChipDark else AppColors.ChipLight

@Composable
fun lineColor(dark: Boolean): Color = if (dark) AppColors.LineDark else AppColors.LineLight

private val LightScheme = lightColorScheme(
    inversePrimary = Color(0xFFFFB85C),
    primary = AppColors.OrangeDark,
    onPrimary = Color.White,
    primaryContainer = AppColors.OrangeSoft,
    onPrimaryContainer = Color(0xFF603600),
    secondary = AppColors.OrangeDark,
    secondaryContainer = AppColors.OrangeSoft,
    onSecondaryContainer = Color(0xFF603600),
    surfaceVariant = AppColors.ChipLight,
    onSurfaceVariant = AppColors.MutedLight,
    surfaceTint = AppColors.Orange,
    background = AppColors.PageLight,
    surface = AppColors.CardLight,
    onBackground = AppColors.TextLight,
    onSurface = AppColors.TextLight
)

private val DarkScheme = darkColorScheme(
    inversePrimary = AppColors.OrangeDark,
    primary = Color(0xFFFFB85C),
    onPrimary = Color(0xFF482800),
    primaryContainer = Color(0xFF583B1E),
    onPrimaryContainer = AppColors.OrangeSoft,
    secondary = Color(0xFFFFB85C),
    secondaryContainer = Color(0xFF583B1E),
    onSecondaryContainer = AppColors.OrangeSoft,
    surfaceVariant = AppColors.ChipDark,
    onSurfaceVariant = AppColors.MutedDark,
    surfaceTint = AppColors.Orange,
    background = AppColors.PageDark,
    surface = AppColors.CardDark,
    onBackground = AppColors.TextDark,
    onSurface = AppColors.TextDark
)

@Composable
fun CounterTheme(dark: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalMutedColor provides if (dark) AppColors.MutedDark else AppColors.MutedLight
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = Typography(),
            content = content
        )
    }
}
