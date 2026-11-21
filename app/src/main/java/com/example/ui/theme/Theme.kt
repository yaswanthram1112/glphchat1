package com.example.ui.theme

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

@Immutable
data class MonochromeTokens(
    val hairline: Color,
    val hairlineStrong: Color,
    val subtleSurface: Color,
    val glassFill: Color,
    val glassStrongFill: Color,
    val glassSoftFill: Color,
    val specularTop: Color,
    val specularSheen: Color,
    val incomingBubble: Color,
    val outgoingBubble: Color,
    val keySurface: Color,
    val keyboardTray: Color,
    val canvasDepth: Color,
    val mutedInk: Color,
    val ink: Color,
    val canvas: Color,
    val isDark: Boolean,
)

val LocalMonochromeTokens = staticCompositionLocalOf {
    MonochromeTokens(
        hairline = HairlineLight,
        hairlineStrong = HairlineStrongLight,
        subtleSurface = SubtleSurfaceLight,
        glassFill = GlassFillLight,
        glassStrongFill = GlassStrongLight,
        glassSoftFill = GlassSoftLight,
        specularTop = SpecularTopLight,
        specularSheen = SpecularSheenLight,
        incomingBubble = IncomingBubbleLight,
        outgoingBubble = OutgoingBubbleLight,
        keySurface = KeySurfaceLight,
        keyboardTray = KeyboardTrayLight,
        canvasDepth = CanvasDepthLight,
        mutedInk = MutedInkLight,
        ink = PureBlack,
        canvas = CanvasLight,
        isDark = false,
    )
}

private val LightMonochromeScheme = lightColorScheme(
    primary = PureBlack,
    onPrimary = PureWhite,
    primaryContainer = SubtleSurfaceLight,
    onPrimaryContainer = PureBlack,
    secondary = PureBlack,
    onSecondary = PureWhite,
    secondaryContainer = SubtleSurfaceLight,
    onSecondaryContainer = PureBlack,
    background = CanvasLight,
    onBackground = PureBlack,
    surface = PureWhite,
    onSurface = PureBlack,
    surfaceVariant = SubtleSurfaceLight,
    onSurfaceVariant = MutedInkLight,
    outline = HairlineLight,
    outlineVariant = HairlineLight,
    error = PureBlack,
    onError = PureWhite,
)

private val DarkMonochromeScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = PureBlack,
    primaryContainer = SubtleSurfaceDark,
    onPrimaryContainer = PureWhite,
    secondary = PureWhite,
    onSecondary = PureBlack,
    secondaryContainer = SubtleSurfaceDark,
    onSecondaryContainer = PureWhite,
    background = CanvasDark,
    onBackground = PureWhite,
    surface = CanvasDepthDark,
    onSurface = PureWhite,
    surfaceVariant = SubtleSurfaceDark,
    onSurfaceVariant = MutedInkDark,
    outline = HairlineDark,
    outlineVariant = HairlineDark,
    error = PureWhite,
    onError = PureBlack,
)

private val LiquidGlassShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun GlyphChatTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkMonochromeScheme else LightMonochromeScheme
    val tokens = if (darkTheme) {
        MonochromeTokens(
            hairline = HairlineDark,
            hairlineStrong = HairlineStrongDark,
            subtleSurface = SubtleSurfaceDark,
            glassFill = GlassFillDark,
            glassStrongFill = GlassStrongDark,
            glassSoftFill = GlassSoftDark,
            specularTop = SpecularTopDark,
            specularSheen = SpecularSheenDark,
            incomingBubble = IncomingBubbleDark,
            outgoingBubble = OutgoingBubbleDark,
            keySurface = KeySurfaceDark,
            keyboardTray = KeyboardTrayDark,
            canvasDepth = CanvasDepthDark,
            mutedInk = MutedInkDark,
            ink = PureWhite,
            canvas = CanvasDark,
            isDark = true,
        )
    } else {
        MonochromeTokens(
            hairline = HairlineLight,
            hairlineStrong = HairlineStrongLight,
            subtleSurface = SubtleSurfaceLight,
            glassFill = GlassFillLight,
            glassStrongFill = GlassStrongLight,
            glassSoftFill = GlassSoftLight,
            specularTop = SpecularTopLight,
            specularSheen = SpecularSheenLight,
            incomingBubble = IncomingBubbleLight,
            outgoingBubble = OutgoingBubbleLight,
            keySurface = KeySurfaceLight,
            keyboardTray = KeyboardTrayLight,
            canvasDepth = CanvasDepthLight,
            mutedInk = MutedInkLight,
            ink = PureBlack,
            canvas = CanvasLight,
            isDark = false,
        )
    }

    CompositionLocalProvider(LocalMonochromeTokens provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = GlyphChatTypography,
            shapes = LiquidGlassShapes,
            content = content
        )
    }
}
