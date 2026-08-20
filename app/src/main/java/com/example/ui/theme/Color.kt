package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

@Immutable
data class AppThemeColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceBorder: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val emeraldGainBg: Color,
    val emeraldGainText: Color,
    val emeraldGainBorder: Color,
    val amberWarningContainer: Color,
    val amberWarningText: Color,
    val amberWarningBorder: Color,
    val bentoBlueContainer: Color,
    val bentoBlueOnContainer: Color,
    val terminalBg: Color,
    val terminalText: Color,
    val terminalPurple: Color,
    val terminalGreen: Color,
    val terminalAmber: Color,
    val cardHighlight: Color
)

// High-Contrast Light Theme Palette (Investor Pro Light)
val LightThemeColors = AppThemeColors(
    isDark = false,
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFF1F5F9),
    surfaceBorder = Color(0xFFE2E8F0),
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF7D5260),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF21005D),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF94A3B8),
    emeraldGainBg = Color(0xFFE8F5E9),
    emeraldGainText = Color(0xFF1B5E20),
    emeraldGainBorder = Color(0xFFA5D6A7),
    amberWarningContainer = Color(0xFFFFF3E0),
    amberWarningText = Color(0xFFE65100),
    amberWarningBorder = Color(0xFFFFCC80),
    bentoBlueContainer = Color(0xFFE1F5FE),
    bentoBlueOnContainer = Color(0xFF01579B),
    terminalBg = Color(0xFF1C1B1F),
    terminalText = Color(0xFFE6E1E5),
    terminalPurple = Color(0xFFD0BCFF),
    terminalGreen = Color(0xFF4ADE80),
    terminalAmber = Color(0xFFFCD34D),
    cardHighlight = Color(0xFFF9F7FA)
)

// High-Contrast Dark Theme Palette (Institutional Cyber/OLED Night Mode)
val DarkThemeColors = AppThemeColors(
    isDark = true,
    background = Color(0xFF0B0F19),
    surface = Color(0xFF131B2E),
    surfaceElevated = Color(0xFF1C2640),
    surfaceBorder = Color(0xFF2D3B59),
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    secondaryContainer = Color(0xFF332D41),
    onSecondaryContainer = Color(0xFFE8DEF8),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFFCBD5E1),
    textMuted = Color(0xFF94A3B8),
    emeraldGainBg = Color(0xFF064E3B),
    emeraldGainText = Color(0xFF4ADE80),
    emeraldGainBorder = Color(0xFF059669),
    amberWarningContainer = Color(0xFF451A03),
    amberWarningText = Color(0xFFFBBF24),
    amberWarningBorder = Color(0xFFD97706),
    bentoBlueContainer = Color(0xFF082F49),
    bentoBlueOnContainer = Color(0xFF38BDF8),
    terminalBg = Color(0xFF05070B),
    terminalText = Color(0xFF38BDF8),
    terminalPurple = Color(0xFFD0BCFF),
    terminalGreen = Color(0xFF4ADE80),
    terminalAmber = Color(0xFFFCD34D),
    cardHighlight = Color(0xFF1C2640)
)

val LocalAppColors = staticCompositionLocalOf { DarkThemeColors }

object AppTheme {
    val colors: AppThemeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}

// Fallback & Shared Color Constants (Ensures full non-composable compatibility)
val DarkSlateBg = Color(0xFFFDF7FF)
val SurfaceCardDark = Color(0xFFFFFFFF)
val SurfaceCardBorder = Color(0xFFCAC4D0)

val BentoPurpleContainer = Color(0xFFEADDFF)
val BentoPurpleOnContainer = Color(0xFF21005D)
val BentoPurpleHeader = Color(0xFFE8DEF8)

val CyanAccent = Color(0xFF6750A4)
val EmeraldGreen = Color(0xFF15803D)
val AmberGold = Color(0xFFB45309)
val RoseRed = Color(0xFFBA1A1A)
val PurpleIndigo = Color(0xFF7D5260)

val TextPrimaryDark = Color(0xFF1D1B20)
val TextSecondaryDark = Color(0xFF49454F)
val TextMutedDark = Color(0xFF79747E)

val TerminalBg = Color(0xFF1C1B1F)
val TerminalText = Color(0xFFE6E1E5)
val TerminalPurple = Color(0xFFD0BCFF)
val TerminalGreen = Color(0xFF4ADE80)
val TerminalAmber = Color(0xFFFCD34D)

val LightBg = Color(0xFFFDF7FF)
val SurfaceCardLight = Color(0xFFFFFFFF)
val TextPrimaryLight = Color(0xFF1D1B20)

val EmeraldGainBg = Color(0xFFE8F5E9)
val EmeraldGainText = Color(0xFF1B5E20)
val EmeraldGainBorder = Color(0xFFA5D6A7)

val AmberWarningContainer = Color(0xFFFFF3E0)
val AmberWarningText = Color(0xFFE65100)
val AmberWarningBorder = Color(0xFFFFCC80)

val BentoBlueContainer = Color(0xFFE1F5FE)
val BentoBlueOnContainer = Color(0xFF01579B)

val BentoCardBgLight = Color(0xFFF9F7FA)
val SurfaceElevatedDark = Color(0xFFF3EDF7)




