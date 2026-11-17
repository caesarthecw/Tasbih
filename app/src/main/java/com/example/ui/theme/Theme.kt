package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val EmeraldLightScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = EmeraldOnPrimary,
    primaryContainer = EmeraldPrimaryContainer,
    onPrimaryContainer = EmeraldOnPrimaryContainer,
    secondary = EmeraldSecondary,
    onSecondary = EmeraldOnSecondary,
    secondaryContainer = EmeraldSecondaryContainer,
    onSecondaryContainer = EmeraldOnSecondaryContainer,
    background = EmeraldBackground,
    onBackground = EmeraldOnSurface,
    surface = EmeraldSurface,
    onSurface = EmeraldOnSurface,
    surfaceVariant = EmeraldSurfaceVariant,
    onSurfaceVariant = EmeraldOnSurfaceVariant,
    outline = Color(0xFFB5C4BD),
)

private val RoyalNightScheme = darkColorScheme(
    primary = NightPrimary,
    onPrimary = NightOnPrimary,
    primaryContainer = NightPrimaryContainer,
    onPrimaryContainer = NightOnPrimaryContainer,
    secondary = NightSecondary,
    onSecondary = NightOnSecondary,
    secondaryContainer = NightSecondaryContainer,
    onSecondaryContainer = NightOnSecondaryContainer,
    background = NightBackground,
    onBackground = NightOnSurface,
    surface = NightSurface,
    onSurface = NightOnSurface,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightOnSurfaceVariant,
    outline = Color(0xFF334E68),
)

private val SandDunesScheme = lightColorScheme(
    primary = SandPrimary,
    onPrimary = SandOnPrimary,
    primaryContainer = SandPrimaryContainer,
    onPrimaryContainer = SandOnPrimaryContainer,
    secondary = SandSecondary,
    onSecondary = SandOnSecondary,
    secondaryContainer = SandSecondaryContainer,
    onSecondaryContainer = SandOnSecondaryContainer,
    background = SandBackground,
    onBackground = SandOnSurface,
    surface = SandSurface,
    onSurface = SandOnSurface,
    surfaceVariant = SandSurfaceVariant,
    onSurfaceVariant = SandOnSurfaceVariant,
    outline = Color(0xFFCBBBA9),
)

private val TurquoiseScheme = lightColorScheme(
    primary = TurquoisePrimary,
    onPrimary = TurquoiseOnPrimary,
    primaryContainer = TurquoisePrimaryContainer,
    onPrimaryContainer = TurquoiseOnPrimaryContainer,
    secondary = TurquoiseSecondary,
    onSecondary = TurquoiseOnSecondary,
    secondaryContainer = TurquoiseSecondaryContainer,
    onSecondaryContainer = TurquoiseOnSecondaryContainer,
    background = TurquoiseBackground,
    onBackground = TurquoiseOnSurface,
    surface = TurquoiseSurface,
    onSurface = TurquoiseOnSurface,
    surfaceVariant = TurquoiseSurfaceVariant,
    onSurfaceVariant = TurquoiseOnSurfaceVariant,
    outline = Color(0xFFAACFD8),
)

private val JasmineScheme = lightColorScheme(
    primary = JasminePrimary,
    onPrimary = JasmineOnPrimary,
    primaryContainer = JasminePrimaryContainer,
    onPrimaryContainer = JasmineOnPrimaryContainer,
    secondary = JasmineSecondary,
    onSecondary = JasmineOnSecondary,
    secondaryContainer = JasmineSecondaryContainer,
    onSecondaryContainer = JasmineOnSecondaryContainer,
    background = JasmineBackground,
    onBackground = JasmineOnSurface,
    surface = JasmineSurface,
    onSurface = JasmineOnSurface,
    surfaceVariant = JasmineSurfaceVariant,
    onSurfaceVariant = JasmineOnSurfaceVariant,
    outline = Color(0xFFD4BCC6),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

fun resolveColorScheme(themeId: String, systemDark: Boolean): ColorScheme {
    return when (themeId) {
        "theme_royal_night" -> RoyalNightScheme
        "theme_sand_dunes" -> SandDunesScheme
        "theme_turquoise" -> TurquoiseScheme
        "theme_jasmine" -> JasmineScheme
        "theme_emerald" -> if (systemDark) RoyalNightScheme else EmeraldLightScheme
        else -> if (systemDark) RoyalNightScheme else EmeraldLightScheme
    }
}

@Composable
fun GharsMisbahaTheme(
    selectedThemeId: String = "theme_emerald",
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = resolveColorScheme(selectedThemeId, darkTheme)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
