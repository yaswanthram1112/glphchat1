package com.example.ui.cipher

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.example.R
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalMonochromeTokens
import com.example.ui.theme.liquidGlassChrome

private val QwertyRow1 = listOf('Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P')
private val QwertyRow2 = listOf('A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L')
private val QwertyRow3 = listOf('Z', 'X', 'C', 'V', 'B', 'N', 'M')

private val SymbolRow1 = listOf('1', '2', '3', '4', '5', '6', '7', '8', '9', '0')
private val SymbolRow2 = listOf('-', '/', ':', ';', '(', ')', '$', '&', '@', '"')
private val SymbolRow3 = listOf('.', ',', '?', '!', '\'', '#', '+', '=')

private val KeyCapShape = RoundedCornerShape(12.dp)

/**
 * Tactile Monochrome Liquid Glass QWERTY Cipher Keyboard with 26 custom SVG glyphs,
 * optional A–Z corner hints, 123 symbol layer, Space, Backspace, Send, and native
 * standard keyboard switcher — styled as smooth rounded glass keycaps with spring tap feedback.
 */
@Composable
fun CipherKeyboard(
    showHints: Boolean,
    onToggleHints: () -> Unit,
    onKeyPress: (String) -> Unit,
    onBackspace: () -> Unit,
    onSend: () -> Unit,
    onSwitchToStandardKeyboard: () -> Unit,
    canSend: Boolean,
    modifier: Modifier = Modifier,
) {
    var symbolMode by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .testTag("cipher_keyboard"),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        // Floating Pill Utility Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                KeyboardToolbarChip(
                    label = if (showHints) "Hints On" else "Hints Off",
                    icon = if (showHints) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                    contentDesc = stringResource(R.string.cd_toggle_hints),
                    active = showHints,
                    onClick = onToggleHints,
                    testTag = "toggle_hints_button",
                )
                KeyboardToolbarChip(
                    label = if (symbolMode) "Glyphs" else "123",
                    icon = null,
                    contentDesc = if (symbolMode) "Switch to cipher letters" else "Switch to numbers and symbols",
                    active = symbolMode,
                    onClick = { symbolMode = !symbolMode },
                    testTag = "toggle_symbols_button",
                )
            }

            KeyboardToolbarChip(
                label = "System KB",
                icon = Icons.Outlined.Keyboard,
                contentDesc = stringResource(R.string.cd_switch_keyboard),
                active = false,
                onClick = onSwitchToStandardKeyboard,
                testTag = "switch_standard_keyboard_button",
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (!symbolMode) {
                // Row 1: Q W E R T Y U I O P
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    QwertyRow1.forEach { letter ->
                        CipherKeyCap(
                            letter = letter,
                            showHint = showHints,
                            onClick = { onKeyPress(letter.toString()) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Row 2: A S D F G H J K L
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    QwertyRow2.forEach { letter ->
                        CipherKeyCap(
                            letter = letter,
                            showHint = showHints,
                            onClick = { onKeyPress(letter.toString()) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // Row 3: Z X C V B N M + Backspace
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Spacer(modifier = Modifier.width(4.dp))
                    QwertyRow3.forEach { letter ->
                        CipherKeyCap(
                            letter = letter,
                            showHint = showHints,
                            onClick = { onKeyPress(letter.toString()) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    RepeatingActionKeyCap(
                        onBackspace = onBackspace,
                        modifier = Modifier
                            .weight(1.55f)
                            .testTag("cipher_key_backspace")
                    ) { contentColor ->
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = stringResource(R.string.cd_backspace),
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                // Symbols Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    SymbolRow1.forEach { ch ->
                        SymbolKeyCap(
                            symbol = ch.toString(),
                            onClick = { onKeyPress(ch.toString()) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                // Symbols Row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    SymbolRow2.forEach { ch ->
                        SymbolKeyCap(
                            symbol = ch.toString(),
                            onClick = { onKeyPress(ch.toString()) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                // Symbols Row 3 + Backspace
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    SymbolRow3.forEach { ch ->
                        SymbolKeyCap(
                            symbol = ch.toString(),
                            onClick = { onKeyPress(ch.toString()) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    RepeatingActionKeyCap(
                        onBackspace = onBackspace,
                        modifier = Modifier
                            .weight(1.6f)
                            .testTag("cipher_key_backspace_sym")
                    ) { contentColor ->
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = stringResource(R.string.cd_backspace),
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Bottom Row: 123/ABC | SPACE | SEND
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                ActionKeyCap(
                    onClick = { symbolMode = !symbolMode },
                    inverted = false,
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("cipher_key_mode")
                ) { contentColor ->
                    Text(
                        text = if (symbolMode) "ABC" else "123",
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = contentColor
                    )
                }

                ActionKeyCap(
                    onClick = { onKeyPress(" ") },
                    inverted = false,
                    modifier = Modifier
                        .weight(4.4f)
                        .testTag("cipher_key_space")
                ) { contentColor ->
                    Text(
                        text = "space",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.2.sp,
                        color = contentColor.copy(alpha = 0.65f)
                    )
                }

                ActionKeyCap(
                    onClick = { if (canSend) onSend() },
                    inverted = canSend,
                    modifier = Modifier
                        .weight(2.0f)
                        .testTag("cipher_key_send")
                ) { contentColor ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "Send",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.cd_send),
                            tint = contentColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CipherKeyCap(
    letter: Char,
    showHint: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalMonochromeTokens.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 700f),
        label = "keycap_scale"
    )

    val bg = if (pressed) tokens.ink else tokens.keySurface
    val fg = if (pressed) tokens.canvas else tokens.ink
    val borderColor = if (pressed) tokens.ink else tokens.hairline
    val topHighlight = if (pressed) tokens.specularTop.copy(alpha = 0.3f) else tokens.specularTop
    val sheen = if (pressed) Color.Transparent else tokens.specularSheen

    Box(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .liquidGlassChrome(
                shape = KeyCapShape,
                fillColor = bg,
                borderColor = borderColor,
                topHighlightColor = topHighlight,
                sheenColor = sheen,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            )
            .testTag("cipher_key_${letter.lowercaseChar()}"),
    ) {
        if (showHint) {
            Text(
                text = letter.toString(),
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 8.sp,
                lineHeight = 9.sp,
                color = if (pressed) fg.copy(alpha = 0.8f) else tokens.mutedInk.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 3.dp, end = 4.dp)
            )
        }

        CipherGlyph(
            letter = letter,
            size = 22.dp,
            color = fg,
            strokeWidthUnits = 2.1f,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun SymbolKeyCap(
    symbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalMonochromeTokens.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 700f),
        label = "symbol_scale"
    )

    val bg = if (pressed) tokens.ink else tokens.keySurface
    val fg = if (pressed) tokens.canvas else tokens.ink
    val borderColor = if (pressed) tokens.ink else tokens.hairline
    val topHighlight = if (pressed) tokens.specularTop.copy(alpha = 0.3f) else tokens.specularTop
    val sheen = if (pressed) Color.Transparent else tokens.specularSheen

    Box(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .liquidGlassChrome(
                shape = KeyCapShape,
                fillColor = bg,
                borderColor = borderColor,
                topHighlightColor = topHighlight,
                sheenColor = sheen,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = fg
        )
    }
}

@Composable
private fun RepeatingActionKeyCap(
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Color) -> Unit,
) {
    val tokens = LocalMonochromeTokens.current
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 700f),
        label = "backspace_scale"
    )

    LaunchedEffect(isPressed) {
        if (isPressed) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onBackspace()
            delay(350L)
            while (isPressed) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onBackspace()
                delay(45L)
            }
        }
    }

    val bg = if (isPressed) tokens.ink else tokens.glassSoftFill
    val fg = if (isPressed) tokens.canvas else tokens.ink
    val borderColor = if (isPressed) tokens.ink else tokens.hairline

    Box(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .liquidGlassChrome(
                shape = KeyCapShape,
                fillColor = bg,
                borderColor = borderColor,
                topHighlightColor = if (isPressed) Color.White.copy(alpha = 0.35f) else tokens.specularTop,
                sheenColor = if (isPressed) Color.Transparent else tokens.specularSheen,
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        content(fg)
    }
}

@Composable
private fun ActionKeyCap(
    onClick: () -> Unit,
    inverted: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (Color) -> Unit,
) {
    val tokens = LocalMonochromeTokens.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 700f),
        label = "action_scale"
    )

    val isSolid = inverted xor pressed
    val bg = if (isSolid) tokens.ink else tokens.glassSoftFill
    val fg = if (isSolid) tokens.canvas else tokens.ink
    val borderColor = if (isSolid) tokens.ink else tokens.hairline
    val topHighlight = if (isSolid) {
        if (tokens.isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.22f)
    } else {
        tokens.specularTop
    }
    val sheen = if (isSolid) {
        if (tokens.isDark) Color.Transparent else Color.White.copy(alpha = 0.08f)
    } else {
        tokens.specularSheen
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .liquidGlassChrome(
                shape = KeyCapShape,
                fillColor = bg,
                borderColor = borderColor,
                topHighlightColor = topHighlight,
                sheenColor = sheen,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        content(fg)
    }
}

@Composable
private fun KeyboardToolbarChip(
    label: String,
    icon: ImageVector?,
    contentDesc: String,
    active: Boolean,
    onClick: () -> Unit,
    testTag: String,
) {
    val tokens = LocalMonochromeTokens.current
    val bg = if (active) tokens.ink else tokens.glassFill
    val fg = if (active) tokens.canvas else tokens.ink
    val borderColor = if (active) tokens.ink else tokens.hairline

    Row(
        modifier = Modifier
            .height(30.dp)
            .clip(CircleShape)
            .background(bg, CircleShape)
            .border(1.dp, borderColor, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = contentDesc,
                tint = fg,
                modifier = Modifier.size(13.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
