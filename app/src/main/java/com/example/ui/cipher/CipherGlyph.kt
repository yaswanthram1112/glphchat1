package com.example.ui.cipher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMonoFamily
import kotlin.math.min

/**
 * Renders one of the exact 26 geometric vector glyphs (A–Z) in a 24x24 viewBox
 * with strokeWidth=2, round stroke caps/joins, and currentColor tint.
 */
@Composable
fun CipherGlyph(
    letter: Char,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = LocalContentColor.current,
    strokeWidthUnits: Float = 2f,
    compact: Boolean = false,
) {
    val upper = letter.uppercaseChar()
    val bounds = if (compact) glyphHorizontalBounds(upper) else (0f to 24f)
    val contentUnits = (bounds.second - bounds.first).coerceIn(8f, 24f)
    val canvasWidth = if (compact) size * (contentUnits / 24f) else size

    Canvas(
        modifier = modifier
            .size(width = canvasWidth, height = size)
            .semantics { contentDescription = "Cipher glyph $upper" }
    ) {
        val unit = if (compact) {
            this.size.height / 24f
        } else {
            min(this.size.width, this.size.height) / 24f
        }
        val offsetX = if (compact) {
            -bounds.first * unit
        } else {
            (this.size.width - 24f * unit) / 2f
        }
        val offsetY = (this.size.height - 24f * unit) / 2f
        val stroke = Stroke(
            width = strokeWidthUnits * unit,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        drawCipherLetter(
            letter = upper,
            unit = unit,
            ox = offsetX,
            oy = offsetY,
            color = color,
            stroke = stroke,
        )
    }
}

/**
 * Tight horizontal bounds (minX..maxX in the 24x24 viewBox, including stroke half-width
 * and balanced optical side-bearing) so adjacent cipher letters read with natural kerning.
 */
private fun glyphHorizontalBounds(letter: Char): Pair<Float, Float> = when (letter) {
    'A' -> 4.4f to 19.6f
    'B' -> 6.6f to 18.8f
    'C' -> 3.6f to 20.4f
    'D' -> 6.6f to 16.0f
    'E' -> 7.0f to 17.0f
    'F' -> 7.0f to 17.8f
    'G' -> 5.2f to 18.8f
    'H' -> 5.2f to 18.8f
    'I' -> 5.2f to 18.8f
    'J' -> 5.2f to 18.8f
    'K' -> 5.2f to 18.8f
    'L' -> 5.2f to 17.8f
    'M' -> 5.2f to 18.8f
    'N' -> 4.4f to 19.6f
    'O' -> 5.2f to 18.8f
    'P' -> 4.4f to 19.6f
    'Q' -> 4.4f to 20.6f
    'R' -> 4.4f to 19.6f
    'S' -> 3.6f to 20.4f
    'T' -> 5.2f to 18.8f
    'U' -> 3.6f to 20.4f
    'V' -> 5.2f to 16.8f
    'W' -> 7.0f to 17.0f
    'X' -> 4.4f to 19.6f
    'Y' -> 4.4f to 18.8f
    'Z' -> 7.0f to 17.0f
    else -> 4.4f to 19.6f
}

private fun DrawScope.drawCipherLetter(
    letter: Char,
    unit: Float,
    ox: Float,
    oy: Float,
    color: Color,
    stroke: Stroke,
) {
    fun pt(x: Float, y: Float) = Offset(ox + x * unit, oy + y * unit)
    fun dot(cx: Float, cy: Float, r: Float) {
        drawCircle(color = color, radius = r * unit, center = pt(cx, cy))
    }
    fun circleStroke(cx: Float, cy: Float, r: Float) {
        drawCircle(color = color, radius = r * unit, center = pt(cx, cy), style = stroke)
    }
    fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
        drawLine(
            color = color,
            start = pt(x1, y1),
            end = pt(x2, y2),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round
        )
    }
    fun drawSvgPath(build: Path.(sx: (Float) -> Float, sy: (Float) -> Float) -> Unit) {
        val p = Path().apply {
            build({ ox + it * unit }, { oy + it * unit })
        }
        drawPath(path = p, color = color, style = stroke)
    }

    when (letter) {
        // A: M6 8 L12 16 L18 8 + dot(12, 4, 1.5)
        'A' -> {
            drawSvgPath { sx, sy ->
                moveTo(sx(6f), sy(8f))
                lineTo(sx(12f), sy(16f))
                lineTo(sx(18f), sy(8f))
            }
            dot(12f, 4f, 1.5f)
        }

        // B: M13 5 H17 V19 H13 + dots(9, 9, 1.5) & (9, 15, 1.5)
        'B' -> {
            drawSvgPath { sx, sy ->
                moveTo(sx(13f), sy(5f))
                lineTo(sx(17f), sy(5f))
                lineTo(sx(17f), sy(19f))
                lineTo(sx(13f), sy(19f))
            }
            dot(9f, 9f, 1.5f)
            dot(9f, 15f, 1.5f)
        }

        // C: circle(12, 12, 7) + dot(12, 12, 1.5)
        'C' -> {
            circleStroke(12f, 12f, 7f)
            dot(12f, 12f, 1.5f)
        }

        // D: M14 5 V19 + dot(9, 12, 1.5)
        'D' -> {
            line(14f, 5f, 14f, 19f)
            dot(9f, 12f, 1.5f)
        }

        // E: M15 5 H9 V19 H15 + M9 12 H13
        'E' -> {
            drawSvgPath { sx, sy ->
                moveTo(sx(15f), sy(5f))
                lineTo(sx(9f), sy(5f))
                lineTo(sx(9f), sy(19f))
                lineTo(sx(15f), sy(19f))
            }
            line(9f, 12f, 13f, 12f)
        }

        // F: M9 5 V19 + M9 5 H16 + M9 11 H14
        'F' -> {
            line(9f, 5f, 9f, 19f)
            line(9f, 5f, 16f, 5f)
            line(9f, 11f, 14f, 11f)
        }

        // G: M7 11 V7 H17 V11 + dot(12, 15, 1.5)
        'G' -> {
            drawSvgPath { sx, sy ->
                moveTo(sx(7f), sy(11f))
                lineTo(sx(7f), sy(7f))
                lineTo(sx(17f), sy(7f))
                lineTo(sx(17f), sy(11f))
            }
            dot(12f, 15f, 1.5f)
        }

        // H: M7 6 H17 L7 18 H17
        'H' -> {
            drawSvgPath { sx, sy ->
                moveTo(sx(7f), sy(6f))
                lineTo(sx(17f), sy(6f))
                lineTo(sx(7f), sy(18f))
                lineTo(sx(17f), sy(18f))
            }
        }

        // I: M7 6 H17, M12 6 V14, M12 14 L7 19, M12 14 L17 19
        'I' -> {
            line(7f, 6f, 17f, 6f)
            line(12f, 6f, 12f, 14f)
            line(12f, 14f, 7f, 19f)
            line(12f, 14f, 17f, 19f)
        }

        // J: M7 14 H17 + dot(12, 9, 1.5)
        'J' -> {
            line(7f, 14f, 17f, 14f)
            dot(12f, 9f, 1.5f)
        }

        // K: M7 6 L12 12 L17 6, M12 5 V18, M8 18 H16
        'K' -> {
            drawSvgPath { sx, sy ->
                moveTo(sx(7f), sy(6f))
                lineTo(sx(12f), sy(12f))
                lineTo(sx(17f), sy(6f))
            }
            line(12f, 5f, 12f, 18f)
            line(8f, 18f, 16f, 18f)
        }

        // L: M16 6 V18 H7
        'L' -> {
            drawSvgPath { sx, sy ->
                moveTo(sx(16f), sy(6f))
                lineTo(sx(16f), sy(18f))
                lineTo(sx(7f), sy(18f))
            }
        }

        // M: M7 6 L12 12 V19 + M12 12 L17 7
        'M' -> {
            drawSvgPath { sx, sy ->
                moveTo(sx(7f), sy(6f))
                lineTo(sx(12f), sy(12f))
                lineTo(sx(12f), sy(19f))
            }
            line(12f, 12f, 17f, 7f)
        }

        // N: M6 18 L12 6 L18 18
        'N' -> {
            drawSvgPath { sx, sy ->
                moveTo(sx(6f), sy(18f))
                lineTo(sx(12f), sy(6f))
                lineTo(sx(18f), sy(18f))
            }
        }

        // O: M7 18 V11 A5 5 0 0 1 17 11 V18 + dot(12, 13, 1.5)
        'O' -> {
            drawSvgPath { sx, sy ->
                moveTo(sx(7f), sy(18f))
                lineTo(sx(7f), sy(11f))
                arcTo(
                    rect = Rect(sx(7f), sy(6f), sx(17f), sy(16f)),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
                lineTo(sx(17f), sy(18f))
            }
            dot(12f, 13f, 1.5f)
        }

        // P: M6 7 H18, M9 7 V18, M15 7 V18 + dot(12, 13, 1.5)
        'P' -> {
            line(6f, 7f, 18f, 7f)
            line(9f, 7f, 9f, 18f)
            line(15f, 7f, 15f, 18f)
            dot(12f, 13f, 1.5f)
        }

        // Q: M6 15 A4 4 0 0 1 14 15 H19 + dot(10, 16, 1.5)
        'Q' -> {
            drawSvgPath { sx, sy ->
                moveTo(sx(6f), sy(15f))
                arcTo(
                    rect = Rect(sx(6f), sy(11f), sx(14f), sy(19f)),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
                lineTo(sx(19f), sy(15f))
            }
            dot(10f, 16f, 1.5f)
        }

        // R: M7 18 L12 6 L17 18, M9 14 H15, M6 16 L18 8
        'R' -> {
            drawSvgPath { sx, sy ->
                moveTo(sx(7f), sy(18f))
                lineTo(sx(12f), sy(6f))
                lineTo(sx(17f), sy(18f))
            }
            line(9f, 14f, 15f, 14f)
            line(6f, 16f, 18f, 8f)
        }

        // S: M12 12 C9 8, 5 8, 5 12 C5 16, 9 16, 12 12 C15 8, 19 8, 19 12 C19 16, 15 16, 12 12 Z
        // + dots (8.5, 12, 1.2) and (15.5, 12, 1.2)
        'S' -> {
            drawSvgPath { sx, sy ->
                moveTo(sx(12f), sy(12f))
                cubicTo(sx(9f), sy(8f), sx(5f), sy(8f), sx(5f), sy(12f))
                cubicTo(sx(5f), sy(16f), sx(9f), sy(16f), sx(12f), sy(12f))
                cubicTo(sx(15f), sy(8f), sx(19f), sy(8f), sx(19f), sy(12f))
                cubicTo(sx(19f), sy(16f), sx(15f), sy(16f), sx(12f), sy(12f))
                close()
            }
            dot(8.5f, 12f, 1.2f)
            dot(15.5f, 12f, 1.2f)
        }

        // T: M7 7 L12 13 L17 7 + M12 13 L9 19
        'T' -> {
            drawSvgPath { sx, sy ->
                moveTo(sx(7f), sy(7f))
                lineTo(sx(12f), sy(13f))
                lineTo(sx(17f), sy(7f))
            }
            line(12f, 13f, 9f, 19f)
        }

        // U: circle(12, 12, 7) + dot(14, 10, 1.5)
        'U' -> {
            circleStroke(12f, 12f, 7f)
            dot(14f, 10f, 1.5f)
        }

        // V: M7 12 H10, M10 6 V18, M15 6 V18
        'V' -> {
            line(7f, 12f, 10f, 12f)
            line(10f, 6f, 10f, 18f)
            line(15f, 6f, 15f, 18f)
        }

        // W: M9 5 V19, M15 5 V19, M9 10 H15, M9 14 H15
        'W' -> {
            line(9f, 5f, 9f, 19f)
            line(15f, 5f, 15f, 19f)
            line(9f, 10f, 15f, 10f)
            line(9f, 14f, 15f, 14f)
        }

        // X: M6 7 L18 17 V7 L6 17 Z
        'X' -> {
            drawSvgPath { sx, sy ->
                moveTo(sx(6f), sy(7f))
                lineTo(sx(18f), sy(17f))
                lineTo(sx(18f), sy(7f))
                lineTo(sx(6f), sy(17f))
                close()
            }
        }

        // Y: M9 6 V16 H17 Z + M9 16 L6 19
        'Y' -> {
            drawSvgPath { sx, sy ->
                moveTo(sx(9f), sy(6f))
                lineTo(sx(9f), sy(16f))
                lineTo(sx(17f), sy(16f))
                close()
            }
            line(9f, 16f, 6f, 19f)
        }

        // Z: M15 6 H11 V11 H15 V17 H9
        'Z' -> {
            drawSvgPath { sx, sy ->
                moveTo(sx(15f), sy(6f))
                lineTo(sx(11f), sy(6f))
                lineTo(sx(11f), sy(11f))
                lineTo(sx(15f), sy(11f))
                lineTo(sx(15f), sy(17f))
                lineTo(sx(9f), sy(17f))
            }
        }
    }
}

/**
 * Renders an English string word-wrapped as live geometric CipherGlyphs (A–Z)
 * with JetBrains Mono fallback for numbers and punctuation.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CipherTextDisplay(
    text: String,
    modifier: Modifier = Modifier,
    glyphSize: Dp = 20.dp,
    color: Color = LocalContentColor.current,
    strokeWidthUnits: Float = 2f,
    maxChars: Int = Int.MAX_VALUE,
) {
    val clipped = if (text.length > maxChars) text.take(maxChars) + "…" else text
    val lines = clipped.split("\n")
    val fallbackFontSize: TextUnit = (glyphSize.value * 0.72f).sp

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        lines.forEach { line ->
            if (line.isEmpty()) {
                Spacer(modifier = Modifier.height(glyphSize * 0.55f))
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(glyphSize * 0.08f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    line.forEach { ch ->
                        if (ch == ' ') {
                            Spacer(modifier = Modifier.width(glyphSize * 0.42f))
                        } else if (ch in 'A'..'Z' || ch in 'a'..'z') {
                            CipherGlyph(
                                letter = ch,
                                size = glyphSize,
                                color = color,
                                strokeWidthUnits = strokeWidthUnits,
                                compact = true,
                            )
                        } else {
                            Text(
                                text = ch.toString(),
                                color = color,
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = fallbackFontSize,
                                lineHeight = fallbackFontSize,
                            )
                        }
                    }
                }
            }
        }
    }
}
