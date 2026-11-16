package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ThemeMode(val label: String) {
  SYSTEM("Как в системе"),
  LIGHT("Светлая"),
  DARK("Тёмная"),
}

@Immutable
data class ToymoteExtendedColors(
  val streakStart: Color,
  val streakEnd: Color,
  val streakContainer: Color,
  val onStreakContainer: Color,
  val chartLine: Color,
  val chartFillTop: Color,
  val chartFillBottom: Color,
  val badgePositive: Color,
  val badgeCalm: Color,
  val badgeIntense: Color,
)

private val LightExtendedColors =
  ToymoteExtendedColors(
    streakStart = StreakFlameStart,
    streakEnd = StreakFlameEnd,
    streakContainer = Color(0xFFFFEFE8),
    onStreakContainer = Color(0xFF7A2400),
    chartLine = DeepVioletPrimaryLight,
    chartFillTop = Color(0x555B2EFF),
    chartFillBottom = Color(0x055B2EFF),
    badgePositive = PositiveMint,
    badgeCalm = CalmSky,
    badgeIntense = IntenseRose,
  )

private val DarkExtendedColors =
  ToymoteExtendedColors(
    streakStart = Color(0xFFFF9153),
    streakEnd = Color(0xFFFF5C8D),
    streakContainer = Color(0xFF391D29),
    onStreakContainer = Color(0xFFFFD6C9),
    chartLine = LavenderPrimaryDark,
    chartFillTop = Color(0x55CBB6FF),
    chartFillBottom = Color(0x05CBB6FF),
    badgePositive = Color(0xFF4FE0B0),
    badgeCalm = Color(0xFF6EC1F2),
    badgeIntense = Color(0xFFFF7597),
  )

val LocalToymoteColors = staticCompositionLocalOf { LightExtendedColors }

private val DarkColorScheme =
  darkColorScheme(
    primary = LavenderPrimaryDark,
    onPrimary = OnLavenderPrimaryDark,
    primaryContainer = VioletContainerDark,
    onPrimaryContainer = OnVioletContainerDark,
    secondary = SoftPlumSecondaryDark,
    onSecondary = OnSoftPlumDark,
    secondaryContainer = SoftPlumContainerDark,
    onSecondaryContainer = OnSoftPlumContainerDark,
    tertiary = AmethystTertiaryDark,
    onTertiary = OnAmethystDark,
    tertiaryContainer = AmethystContainerDark,
    onTertiaryContainer = OnAmethystContainerDark,
    background = ObsidianBackgroundDark,
    onBackground = CrispLavenderTextDark,
    surface = ObsidianSurfaceDark,
    onSurface = CrispLavenderTextDark,
    surfaceVariant = ObsidianSurfaceContainerDark,
    onSurfaceVariant = MutedLavenderTextDark,
    surfaceContainerLowest = ObsidianBackgroundDark,
    surfaceContainerLow = ObsidianSurfaceContainerLowDark,
    surfaceContainer = ObsidianSurfaceContainerDark,
    surfaceContainerHigh = ObsidianSurfaceContainerHighDark,
    outline = OutlineVioletDark,
    outlineVariant = OutlineVariantDark,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = DeepVioletPrimaryLight,
    onPrimary = OnDeepVioletLight,
    primaryContainer = VioletContainerLight,
    onPrimaryContainer = OnVioletContainerLight,
    secondary = SoftPlumSecondaryLight,
    onSecondary = OnSoftPlumLight,
    secondaryContainer = SoftPlumContainerLight,
    onSecondaryContainer = OnSoftPlumContainerLight,
    tertiary = AmethystTertiaryLight,
    onTertiary = OnAmethystLight,
    tertiaryContainer = AmethystContainerLight,
    onTertiaryContainer = OnAmethystContainerLight,
    background = LavenderBackgroundLight,
    onBackground = DeepInkTextLight,
    surface = LavenderSurfaceLight,
    onSurface = DeepInkTextLight,
    surfaceVariant = LavenderSurfaceContainerLight,
    onSurfaceVariant = MutedVioletTextLight,
    surfaceContainerLowest = LavenderSurfaceLight,
    surfaceContainerLow = LavenderSurfaceContainerLowLight,
    surfaceContainer = LavenderSurfaceContainerLight,
    surfaceContainerHigh = LavenderSurfaceContainerHighLight,
    outline = OutlineVioletLight,
    outlineVariant = OutlineVariantLight,
  )

val ToymoteShapes =
  Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
  )

@Composable
fun ToymoteTheme(
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  content: @Composable () -> Unit,
) {
  val isDark =
    when (themeMode) {
      ThemeMode.SYSTEM -> isSystemInDarkTheme()
      ThemeMode.LIGHT -> false
      ThemeMode.DARK -> true
    }

  val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
  val extendedColors = if (isDark) DarkExtendedColors else LightExtendedColors

  CompositionLocalProvider(LocalToymoteColors provides extendedColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      shapes = ToymoteShapes,
      content = content,
    )
  }
}
