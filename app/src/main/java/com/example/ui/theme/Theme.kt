package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalGameTheme = staticCompositionLocalOf { GameThemes.ClassicBlue }
val LocalAppStrings = staticCompositionLocalOf { AppStrings.forLanguage("en") }

@Composable
fun ArrowPuzzleTheme(
    gameTheme: GameThemePalette = GameThemes.ClassicBlue,
    languageCode: String = "en",
    content: @Composable () -> Unit
) {
    val colorScheme = if (gameTheme.isDark) {
        darkColorScheme(
            primary = gameTheme.primaryAccent,
            onPrimary = Color(0xFF081224),
            secondary = gameTheme.secondaryAccent,
            onSecondary = Color(0xFF081224),
            background = gameTheme.bgBottom,
            onBackground = gameTheme.onBackgroundText,
            surface = gameTheme.cardSurface,
            onSurface = gameTheme.onCardText,
            surfaceVariant = gameTheme.boardSurface,
            onSurfaceVariant = gameTheme.mutedCardText,
            outline = gameTheme.cardBorder
        )
    } else {
        lightColorScheme(
            primary = gameTheme.primaryAccent,
            onPrimary = Color.White,
            secondary = gameTheme.secondaryAccent,
            onSecondary = Color.White,
            background = gameTheme.bgTop,
            onBackground = gameTheme.onBackgroundText,
            surface = gameTheme.cardSurface,
            onSurface = gameTheme.onCardText,
            surfaceVariant = gameTheme.boardSurface,
            onSurfaceVariant = gameTheme.mutedCardText,
            outline = gameTheme.cardBorder
        )
    }

    val strings = AppStrings.forLanguage(languageCode)

    CompositionLocalProvider(
        LocalGameTheme provides gameTheme,
        LocalAppStrings provides strings
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
