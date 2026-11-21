package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Holds a recorded GraphicsLayer of the underlying scrollable backdrop so floating
 * frosted-glass headers, composer bars, and overlays can render true blurred backdrop
 * refraction on Android 12+ (API 31+) with clean translucent fallback on older APIs.
 */
@Stable
class LiquidGlassBackdropState(
    val layer: GraphicsLayer
) {
    var containerOffsetInRoot by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberLiquidGlassBackdropState(): LiquidGlassBackdropState {
    val layer = rememberGraphicsLayer()
    return remember(layer) { LiquidGlassBackdropState(layer) }
}

/**
 * Marks a container (such as a scrolling message list + monochrome atmospheric canvas)
 * as the live backdrop source for sibling [LiquidGlassSurface] floating overlays.
 */
fun Modifier.liquidGlassBackdropSource(
    state: LiquidGlassBackdropState
): Modifier = this
    .onGloballyPositioned { coords ->
        if (coords.isAttached) {
            state.containerOffsetInRoot = coords.positionInRoot()
        }
    }
    .drawWithContent {
        if (!state.layer.isReleased) {
            state.layer.record {
                this@drawWithContent.drawContent()
            }
            drawLayer(state.layer)
        } else {
            drawContent()
        }
    }

/**
 * Subtle pure-monochrome tonal atmosphere behind translucent glass surfaces.
 * In dark mode, provides deep obsidian contrast without milky white glare.
 */
fun Modifier.monochromeGlassCanvas(tokens: MonochromeTokens): Modifier = this.drawBehind {
    drawRect(color = tokens.canvas)

    val topGlow = if (tokens.isDark) Color(0xFF141414) else PureWhite
    val bottomTone = if (tokens.isDark) PureBlack else Color(0xFFE7E7E7)

    // Upper-left soft monochrome light field
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(topGlow.copy(alpha = if (tokens.isDark) 0.30f else 0.90f), Color.Transparent),
            center = Offset(size.width * 0.15f, size.height * 0.08f),
            radius = maxOf(size.width, size.height) * 0.72f
        )
    )

    // Lower-right soft neutral depth field
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(bottomTone.copy(alpha = if (tokens.isDark) 0.50f else 0.75f), Color.Transparent),
            center = Offset(size.width * 0.88f, size.height * 0.86f),
            radius = maxOf(size.width, size.height) * 0.68f
        )
    )
}

/**
 * Pure monochrome Liquid Glass / Frosted Glass surface:
 * - Real blurred backdrop capture on API 31+ over scrolling content
 * - Translucent obsidian dark glass fill in dark mode
 * - Subtle 1dp hairline glass border
 */
@Composable
fun LiquidGlassSurface(
    shape: Shape,
    modifier: Modifier = Modifier,
    backdropState: LiquidGlassBackdropState? = null,
    scrollInvalidationProvider: () -> Int = { 0 },
    blurRadius: Dp = 20.dp,
    fillColor: Color? = null,
    borderColor: Color? = null,
    specularStrength: Float = 1f,
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = LocalMonochromeTokens.current
    val density = LocalDensity.current
    val supportsBackdropBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && backdropState != null

    val baseFill = fillColor ?: if (supportsBackdropBlur) {
        tokens.glassFill
    } else if (backdropState != null) {
        tokens.glassStrongFill
    } else {
        tokens.glassFill
    }
    val baseBorder = borderColor ?: tokens.hairline
    val topSpecular = tokens.specularTop.copy(alpha = (tokens.specularTop.alpha * specularStrength).coerceIn(0f, 1f))
    val sheenColor = tokens.specularSheen.copy(alpha = (tokens.specularSheen.alpha * specularStrength).coerceIn(0f, 1f))

    var panelOffsetInRoot by remember { mutableStateOf(Offset.Zero) }
    val blurPx = with(density) { blurRadius.toPx() }

    val borderBrush = remember(topSpecular, baseBorder) {
        Brush.verticalGradient(
            0.0f to topSpecular,
            0.22f to baseBorder,
            1.0f to baseBorder,
        )
    }

    val sheenBrush = remember(sheenColor) {
        Brush.verticalGradient(
            0.0f to sheenColor,
            0.38f to Color.Transparent,
            1.0f to Color.Transparent,
        )
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                if (backdropState != null && coords.isAttached) {
                    panelOffsetInRoot = coords.positionInRoot()
                }
            }
            .clip(shape)
    ) {
        if (backdropState != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Spacer(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        this.clip = true
                        this.shape = shape
                        renderEffect = BlurEffect(
                            radiusX = blurPx,
                            radiusY = blurPx,
                            edgeTreatment = TileMode.Clamp
                        )
                    }
                    .drawBehind {
                        @Suppress("UNUSED_VARIABLE")
                        val tick = scrollInvalidationProvider()
                        val layer = backdropState.layer
                        if (!layer.isReleased && layer.size.width > 0 && layer.size.height > 0) {
                            val dx = backdropState.containerOffsetInRoot.x - panelOffsetInRoot.x
                            val dy = backdropState.containerOffsetInRoot.y - panelOffsetInRoot.y
                            translate(left = dx, top = dy) {
                                drawLayer(layer)
                            }
                        }
                    }
            )
        }

        // Translucent glass fill + specular top sheen + crisp rim border
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(baseFill, shape)
                .background(sheenBrush, shape)
                .border(width = 1.dp, brush = borderBrush, shape = shape)
                .drawWithContent {
                    drawContent()
                    if (topSpecular.alpha > 0.02f) {
                        val strokePx = 1.dp.toPx()
                        drawLine(
                            brush = Brush.horizontalGradient(
                                0.0f to Color.Transparent,
                                0.18f to topSpecular,
                                0.82f to topSpecular,
                                1.0f to Color.Transparent,
                            ),
                            start = Offset(0f, strokePx),
                            end = Offset(size.width, strokePx),
                            strokeWidth = strokePx,
                        )
                    }
                }
        )

        content()
    }
}

/**
 * Lightweight liquid glass modifier for smaller pills, bubbles, inputs, and keycaps.
 */
fun Modifier.liquidGlassChrome(
    shape: Shape,
    fillColor: Color,
    borderColor: Color,
    topHighlightColor: Color,
    sheenColor: Color,
): Modifier = composed {
    val borderBrush = remember(topHighlightColor, borderColor) {
        Brush.verticalGradient(
            0.0f to topHighlightColor,
            0.25f to borderColor,
            1.0f to borderColor,
        )
    }
    val sheenBrush = remember(sheenColor) {
        Brush.verticalGradient(
            0.0f to sheenColor,
            0.42f to Color.Transparent,
            1.0f to Color.Transparent,
        )
    }
    this
        .clip(shape)
        .background(fillColor, shape)
        .background(sheenBrush, shape)
        .border(width = 1.dp, brush = borderBrush, shape = shape)
}
