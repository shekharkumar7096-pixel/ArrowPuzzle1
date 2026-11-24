package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.game.ArrowColor

data class TileColorSet(
    val faceTop: Color,
    val faceBottom: Color,
    val bevelEdge: Color,
    val arrowInk: Color = Color.White
)

data class GameThemePalette(
    val id: String,
    val name: String,
    val tagline: String,
    val priceCoins: Int,
    val isDark: Boolean,
    val bgTop: Color,
    val bgBottom: Color,
    val celebrationTop: Color,
    val celebrationBottom: Color,
    val sunburstRayColor: Color,
    val cardSurface: Color,
    val cardBorder: Color,
    val boardSurface: Color,
    val boardDotColor: Color,
    val primaryAccent: Color,
    val primaryAccentDark: Color,
    val secondaryAccent: Color,
    val onBackgroundText: Color,
    val onCardText: Color,
    val mutedCardText: Color,
    val useUniformPathInk: Boolean,
    val uniformPathInk: Color,
    val blockedPathInk: Color,
    val arrowColors: Map<ArrowColor, TileColorSet>
) {
    fun tileColorsFor(arrowColor: ArrowColor): TileColorSet {
        return arrowColors[arrowColor] ?: DefaultBrightArrows.getValue(ArrowColor.BLUE)
    }

    fun pathColorFor(arrowColor: ArrowColor, isBlockedMarked: Boolean): Color {
        if (isBlockedMarked) return blockedPathInk
        return if (useUniformPathInk) uniformPathInk else tileColorsFor(arrowColor).faceTop
    }
}

private val DefaultBrightArrows: Map<ArrowColor, TileColorSet> = mapOf(
    ArrowColor.NAVY to TileColorSet(Color(0xFF0B132B), Color(0xFF1E293B), Color(0xFF020617)),
    ArrowColor.BLUE to TileColorSet(Color(0xFF38B6FF), Color(0xFF1580E8), Color(0xFF0C58B0)),
    ArrowColor.GREEN to TileColorSet(Color(0xFF3EDB7C), Color(0xFF18B258), Color(0xFF0F7E3D)),
    ArrowColor.YELLOW to TileColorSet(Color(0xFFFFD84D), Color(0xFFF5A615), Color(0xFFC47A00), Color(0xFF3D2500)),
    ArrowColor.RED to TileColorSet(Color(0xFFFF6B6B), Color(0xFFE53946), Color(0xFFA81D2B)),
    ArrowColor.PURPLE to TileColorSet(Color(0xFFC06BFF), Color(0xFF8B3DFF), Color(0xFF5D1CC2))
)

object GameThemes {

    val ClassicBlue = GameThemePalette(
        id = "classic_blue",
        name = "Classic Blue",
        tagline = "Crisp white dotted board & signature navy arrows",
        priceCoins = 0,
        isDark = false,
        bgTop = Color(0xFF1C9BFF),
        bgBottom = Color(0xFF0E76E5),
        celebrationTop = Color(0xFF2196F3),
        celebrationBottom = Color(0xFF0E76E5),
        sunburstRayColor = Color(0x28FFFFFF),
        cardSurface = Color(0xFFFFFFFF),
        cardBorder = Color(0xFFE2E8F0),
        boardSurface = Color(0xFFFFFFFF),
        boardDotColor = Color(0xFFD5DCE8),
        primaryAccent = Color(0xFF4F6BF6),
        primaryAccentDark = Color(0xFF344EDB),
        secondaryAccent = Color(0xFF22C55E),
        onBackgroundText = Color(0xFFFFFFFF),
        onCardText = Color(0xFF0B132B),
        mutedCardText = Color(0xFF64748B),
        useUniformPathInk = true,
        uniformPathInk = Color(0xFF0B132B),
        blockedPathInk = Color(0xFFFF4D6A),
        arrowColors = DefaultBrightArrows
    )

    val ForestGreen = GameThemePalette(
        id = "forest_green",
        name = "Forest Green",
        tagline = "Sunlit emerald meadow & colorful woodland paths",
        priceCoins = 150,
        isDark = false,
        bgTop = Color(0xFF2EB86B),
        bgBottom = Color(0xFF147A45),
        celebrationTop = Color(0xFF2EB86B),
        celebrationBottom = Color(0xFF147A45),
        sunburstRayColor = Color(0x22FFF8B8),
        cardSurface = Color(0xFFF9FFF9),
        cardBorder = Color(0xFFC8EBD3),
        boardSurface = Color(0xFFF5FCF7),
        boardDotColor = Color(0xFFB7DEC3),
        primaryAccent = Color(0xFF1F9D57),
        primaryAccentDark = Color(0xFF126E3B),
        secondaryAccent = Color(0xFFFFB703),
        onBackgroundText = Color(0xFFFFFFFF),
        onCardText = Color(0xFF113320),
        mutedCardText = Color(0xFF4C725B),
        useUniformPathInk = false,
        uniformPathInk = Color(0xFF113320),
        blockedPathInk = Color(0xFFFF4D6A),
        arrowColors = mapOf(
            ArrowColor.NAVY to TileColorSet(Color(0xFF113320), Color(0xFF0A2114), Color(0xFF06140C)),
            ArrowColor.BLUE to TileColorSet(Color(0xFF0284C7), Color(0xFF0369A1), Color(0xFF075985)),
            ArrowColor.GREEN to TileColorSet(Color(0xFF16A34A), Color(0xFF15803D), Color(0xFF166534)),
            ArrowColor.YELLOW to TileColorSet(Color(0xFFD97706), Color(0xFFB45309), Color(0xFF92400E)),
            ArrowColor.RED to TileColorSet(Color(0xFFEA580C), Color(0xFFC2410C), Color(0xFF9A3412)),
            ArrowColor.PURPLE to TileColorSet(Color(0xFF7C3AED), Color(0xFF6D28D9), Color(0xFF5B21B6))
        )
    )

    val CandyWorld = GameThemePalette(
        id = "candy_world",
        name = "Candy World",
        tagline = "Cotton candy skies & vibrant sugar-glazed arrows",
        priceCoins = 200,
        isDark = false,
        bgTop = Color(0xFFFF66B2),
        bgBottom = Color(0xFFAB47BC),
        celebrationTop = Color(0xFFFF66B2),
        celebrationBottom = Color(0xFFAB47BC),
        sunburstRayColor = Color(0x26FFF0F8),
        cardSurface = Color(0xFFFFF8FC),
        cardBorder = Color(0xFFFFD1E8),
        boardSurface = Color(0xFFFFF7FB),
        boardDotColor = Color(0xFFF5BDD9),
        primaryAccent = Color(0xFFEC407A),
        primaryAccentDark = Color(0xFFB71C53),
        secondaryAccent = Color(0xFF26C6DA),
        onBackgroundText = Color(0xFFFFFFFF),
        onCardText = Color(0xFF421536),
        mutedCardText = Color(0xFF8C4F78),
        useUniformPathInk = false,
        uniformPathInk = Color(0xFF421536),
        blockedPathInk = Color(0xFFE11D48),
        arrowColors = mapOf(
            ArrowColor.NAVY to TileColorSet(Color(0xFF4A154B), Color(0xFF310B32), Color(0xFF1E051E)),
            ArrowColor.BLUE to TileColorSet(Color(0xFF00ACC1), Color(0xFF00838F), Color(0xFF006064)),
            ArrowColor.GREEN to TileColorSet(Color(0xFF00C853), Color(0xFF009624), Color(0xFF083D1D)),
            ArrowColor.YELLOW to TileColorSet(Color(0xFFF59E0B), Color(0xFFD97706), Color(0xFFB45309)),
            ArrowColor.RED to TileColorSet(Color(0xFFF50057), Color(0xFFC51162), Color(0xFF880E4F)),
            ArrowColor.PURPLE to TileColorSet(Color(0xFF9C27B0), Color(0xFF7B1FA2), Color(0xFF4A148C))
        )
    )

    val DarkMode = GameThemePalette(
        id = "dark_mode",
        name = "Dark Mode",
        tagline = "Sleek obsidian slate with crisp luminous paths",
        priceCoins = 250,
        isDark = true,
        bgTop = Color(0xFF1A2033),
        bgBottom = Color(0xFF0F1320),
        celebrationTop = Color(0xFF1E293B),
        celebrationBottom = Color(0xFF0F172A),
        sunburstRayColor = Color(0x1278A8FF),
        cardSurface = Color(0xFF1F263B),
        cardBorder = Color(0xFF323E5E),
        boardSurface = Color(0xFF121829),
        boardDotColor = Color(0xFF344161),
        primaryAccent = Color(0xFF4DA3FF),
        primaryAccentDark = Color(0xFF1E6FCC),
        secondaryAccent = Color(0xFF38D989),
        onBackgroundText = Color(0xFFF5F8FF),
        onCardText = Color(0xFFF2F6FF),
        mutedCardText = Color(0xFF9AA8C7),
        useUniformPathInk = true,
        uniformPathInk = Color(0xFFF1F5F9),
        blockedPathInk = Color(0xFFFF526B),
        arrowColors = DefaultBrightArrows
    )

    val NeonNight = GameThemePalette(
        id = "neon_night",
        name = "Neon Night",
        tagline = "Electric synthwave glow & neon laser arrows",
        priceCoins = 300,
        isDark = true,
        bgTop = Color(0xFF1A0F45),
        bgBottom = Color(0xFF09061C),
        celebrationTop = Color(0xFF281266),
        celebrationBottom = Color(0xFF09061C),
        sunburstRayColor = Color(0x1E00F5D4),
        cardSurface = Color(0xFF18123A),
        cardBorder = Color(0xFF4E2A9E),
        boardSurface = Color(0xFF0E0926),
        boardDotColor = Color(0xFF3B2878),
        primaryAccent = Color(0xFF00E5FF),
        primaryAccentDark = Color(0xFF0099B8),
        secondaryAccent = Color(0xFFFF2E93),
        onBackgroundText = Color(0xFFFFFFFF),
        onCardText = Color(0xFFF5F3FF),
        mutedCardText = Color(0xFFB4A7E8),
        useUniformPathInk = false,
        uniformPathInk = Color(0xFF00F0FF),
        blockedPathInk = Color(0xFFFF2E63),
        arrowColors = mapOf(
            ArrowColor.NAVY to TileColorSet(Color(0xFF00F0FF), Color(0xFF00A8E8), Color(0xFF006B9E)),
            ArrowColor.BLUE to TileColorSet(Color(0xFF00F0FF), Color(0xFF00A8E8), Color(0xFF006B9E)),
            ArrowColor.GREEN to TileColorSet(Color(0xFF39FF14), Color(0xFF00C853), Color(0xFF007E33)),
            ArrowColor.YELLOW to TileColorSet(Color(0xFFFFEA00), Color(0xFFFFAB00), Color(0xFFC67C00)),
            ArrowColor.RED to TileColorSet(Color(0xFFFF2E93), Color(0xFFD5006D), Color(0xFF96004B)),
            ArrowColor.PURPLE to TileColorSet(Color(0xFFD55CFF), Color(0xFF9D00FF), Color(0xFF6500B3))
        )
    )

    val OceanWorld = GameThemePalette(
        id = "ocean_world",
        name = "Ocean World",
        tagline = "Tropical turquoise reef & sea-glass paths",
        priceCoins = 350,
        isDark = false,
        bgTop = Color(0xFF00B8D4),
        bgBottom = Color(0xFF006080),
        celebrationTop = Color(0xFF00B8D4),
        celebrationBottom = Color(0xFF006080),
        sunburstRayColor = Color(0x24B2EBF2),
        cardSurface = Color(0xFFF5FEFF),
        cardBorder = Color(0xFFB2EBF2),
        boardSurface = Color(0xFFF2FCFD),
        boardDotColor = Color(0xFFA7DDE6),
        primaryAccent = Color(0xFF0097A7),
        primaryAccentDark = Color(0xFF006978),
        secondaryAccent = Color(0xFFFF7043),
        onBackgroundText = Color(0xFFFFFFFF),
        onCardText = Color(0xFF06323D),
        mutedCardText = Color(0xFF417582),
        useUniformPathInk = false,
        uniformPathInk = Color(0xFF06323D),
        blockedPathInk = Color(0xFFFF4D6A),
        arrowColors = mapOf(
            ArrowColor.NAVY to TileColorSet(Color(0xFF06323D), Color(0xFF042229), Color(0xFF021418)),
            ArrowColor.BLUE to TileColorSet(Color(0xFF0288D1), Color(0xFF0277BD), Color(0xFF01579B)),
            ArrowColor.GREEN to TileColorSet(Color(0xFF00897B), Color(0xFF00695C), Color(0xFF004D40)),
            ArrowColor.YELLOW to TileColorSet(Color(0xFFF59E0B), Color(0xFFD97706), Color(0xFFB45309)),
            ArrowColor.RED to TileColorSet(Color(0xFFF4511E), Color(0xFFD84315), Color(0xFFBF360C)),
            ArrowColor.PURPLE to TileColorSet(Color(0xFF5E35B1), Color(0xFF4527A0), Color(0xFF311B92))
        )
    )

    val Sunset = GameThemePalette(
        id = "sunset",
        name = "Sunset",
        tagline = "Golden hour amber & warm coral dusk",
        priceCoins = 400,
        isDark = false,
        bgTop = Color(0xFFFF8F00),
        bgBottom = Color(0xFFD81B60),
        celebrationTop = Color(0xFFFF8F00),
        celebrationBottom = Color(0xFFD81B60),
        sunburstRayColor = Color(0x28FFE082),
        cardSurface = Color(0xFFFFFAF5),
        cardBorder = Color(0xFFFFD5BA),
        boardSurface = Color(0xFFFFFAF5),
        boardDotColor = Color(0xFFF3C6A8),
        primaryAccent = Color(0xFFF4511E),
        primaryAccentDark = Color(0xFFBF360C),
        secondaryAccent = Color(0xFF8E24AA),
        onBackgroundText = Color(0xFFFFFFFF),
        onCardText = Color(0xFF3E190F),
        mutedCardText = Color(0xFF8A5446),
        useUniformPathInk = false,
        uniformPathInk = Color(0xFF3E190F),
        blockedPathInk = Color(0xFFE11D48),
        arrowColors = DefaultBrightArrows
    )

    val all: List<GameThemePalette> = listOf(
        ClassicBlue,
        ForestGreen,
        CandyWorld,
        DarkMode,
        NeonNight,
        OceanWorld,
        Sunset
    )

    fun byId(id: String): GameThemePalette = all.firstOrNull { it.id == id } ?: ClassicBlue
}
