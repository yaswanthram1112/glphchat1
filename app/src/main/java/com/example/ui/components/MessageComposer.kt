package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ChatMessage
import com.example.ui.cipher.CipherKeyboard
import com.example.ui.cipher.CipherTextDisplay
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LiquidGlassBackdropState
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LocalMonochromeTokens
import com.example.ui.theme.liquidGlassChrome

private val ComposerActionButtonSize = 44.dp
private val ComposerPillMinHeight = 44.dp
private val ComposerTextFieldMinHeight = 24.dp
private val ComposerTextFieldMaxHeight = 112.dp
private val ComposerGlyphPreviewMaxHeight = 56.dp
private val ComposerCipherPillMaxHeight = 112.dp

/**
 * Floating Monochrome Liquid Glass message composer supporting:
 * - Reply quote banner
 * - Multiline BasicTextField input with a strict fixed max height and vertical scrolling
 *   to prevent layout shifts on long/multiline messages
 * - Live cipher glyph preview & smoothly expanding custom 26-glyph Cipher Keyboard
 */
@Composable
fun MessageComposer(
    composerText: String,
    replyTarget: ChatMessage?,
    showKeyHints: Boolean,
    useStandardKeyboard: Boolean,
    cipherKeyboardExpanded: Boolean,
    backdropState: LiquidGlassBackdropState,
    scrollInvalidationProvider: () -> Int,
    onClearReply: () -> Unit,
    onAppendCipherText: (String) -> Unit,
    onSetComposerText: (String) -> Unit,
    onBackspace: () -> Unit,
    onSend: () -> Unit,
    onToggleKeyHints: () -> Unit,
    onSwitchKeyboardMode: (Boolean) -> Unit,
    onToggleCipherKeyboardExpanded: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalMonochromeTokens.current
    val dockShape = if (!useStandardKeyboard && cipherKeyboardExpanded) {
        RoundedCornerShape(26.dp)
    } else {
        RoundedCornerShape(28.dp)
    }

    LiquidGlassSurface(
        shape = dockShape,
        backdropState = backdropState,
        scrollInvalidationProvider = scrollInvalidationProvider,
        blurRadius = 20.dp,
        fillColor = if (!useStandardKeyboard && cipherKeyboardExpanded) {
            tokens.keyboardTray
        } else {
            tokens.glassFill
        },
        modifier = modifier
            .fillMaxWidth()
            .testTag("message_composer")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ReplyQuoteBanner(
                replyTarget = replyTarget,
                onClearReply = onClearReply,
            )

            if (useStandardKeyboard) {
                StandardMultilineComposer(
                    composerText = composerText,
                    onSetComposerText = onSetComposerText,
                    onSend = onSend,
                    onSwitchToCipherKeyboard = {
                        onSwitchKeyboardMode(false)
                        onToggleCipherKeyboardExpanded(true)
                    },
                )
            } else {
                CipherMultilineComposer(
                    composerText = composerText,
                    showKeyHints = showKeyHints,
                    cipherKeyboardExpanded = cipherKeyboardExpanded,
                    onToggleExpanded = { onToggleCipherKeyboardExpanded(!cipherKeyboardExpanded) },
                    onExpandKeyboard = { onToggleCipherKeyboardExpanded(true) },
                    onSend = onSend,
                )

                AnimatedVisibility(
                    visible = cipherKeyboardExpanded,
                    enter = expandVertically(spring(dampingRatio = 0.85f, stiffness = 420f)) + fadeIn(),
                    exit = shrinkVertically(spring(dampingRatio = 0.9f, stiffness = 460f)) + fadeOut(),
                ) {
                    Column {
                        HorizontalDivider(thickness = 1.dp, color = tokens.hairline)
                        CipherKeyboard(
                            showHints = showKeyHints,
                            onToggleHints = onToggleKeyHints,
                            onKeyPress = onAppendCipherText,
                            onBackspace = onBackspace,
                            onSend = onSend,
                            onSwitchToStandardKeyboard = { onSwitchKeyboardMode(true) },
                            canSend = composerText.isNotBlank(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplyQuoteBanner(
    replyTarget: ChatMessage?,
    onClearReply: () -> Unit,
) {
    val tokens = LocalMonochromeTokens.current
    AnimatedVisibility(
        visible = replyTarget != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        if (replyTarget != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, top = 8.dp)
                    .liquidGlassChrome(
                        shape = RoundedCornerShape(16.dp),
                        fillColor = tokens.glassSoftFill,
                        borderColor = tokens.hairline,
                        topHighlightColor = tokens.specularTop,
                        sheenColor = Color.Transparent,
                    )
                    .padding(start = 12.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(26.dp)
                            .clip(CircleShape)
                            .background(tokens.ink)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Replying to ${replyTarget.senderName}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = replyTarget.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.mutedInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IconButton(
                    onClick = onClearReply,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("clear_reply_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        tint = tokens.ink,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Standard system-keyboard composer row with bottom-anchored circular action buttons
 * and a multiline BasicTextField wrapped in a fixed-max-height verticalScroll viewport
 * so long messages scroll smoothly without causing layout shifts.
 */
@Composable
private fun StandardMultilineComposer(
    composerText: String,
    onSetComposerText: (String) -> Unit,
    onSend: () -> Unit,
    onSwitchToCipherKeyboard: () -> Unit,
) {
    val tokens = LocalMonochromeTokens.current
    val previewScrollState = rememberScrollState()
    val inputScrollState = rememberScrollState()

    LaunchedEffect(composerText) {
        previewScrollState.scrollTo(previewScrollState.maxValue)
        inputScrollState.scrollTo(inputScrollState.maxValue)
    }

    val inputTextStyle = TextStyle(
        fontFamily = InterFontFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = tokens.ink,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Switch back to Cipher Keyboard button (bottom-anchored)
        Box(
            modifier = Modifier
                .size(ComposerActionButtonSize)
                .liquidGlassChrome(
                    shape = CircleShape,
                    fillColor = tokens.glassStrongFill,
                    borderColor = tokens.hairline,
                    topHighlightColor = tokens.specularTop,
                    sheenColor = tokens.specularSheen,
                )
                .clickable(onClick = onSwitchToCipherKeyboard)
                .testTag("switch_cipher_keyboard_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.GridView,
                contentDescription = stringResource(R.string.cd_switch_keyboard),
                tint = tokens.ink,
                modifier = Modifier.size(19.dp)
            )
        }

        // Multiline glass input pill with bounded scrollable glyph preview + scrollable BasicTextField
        Column(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = ComposerPillMinHeight)
                .liquidGlassChrome(
                    shape = RoundedCornerShape(22.dp),
                    fillColor = tokens.glassStrongFill,
                    borderColor = tokens.hairline,
                    topHighlightColor = tokens.specularTop,
                    sheenColor = Color.Transparent,
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
        ) {
            if (composerText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = ComposerGlyphPreviewMaxHeight)
                        .verticalScroll(previewScrollState)
                ) {
                    CipherTextDisplay(
                        text = composerText,
                        glyphSize = 16.dp,
                        color = tokens.ink,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                HorizontalDivider(thickness = 1.dp, color = tokens.hairline)
            }

            // Fixed max-height scrollable viewport hosting full-height multiline BasicTextField
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(
                        min = ComposerTextFieldMinHeight,
                        max = ComposerTextFieldMaxHeight,
                    )
                    .verticalScroll(inputScrollState)
                    .testTag("standard_message_scroll_container"),
                contentAlignment = Alignment.TopStart,
            ) {
                BasicTextField(
                    value = composerText,
                    onValueChange = onSetComposerText,
                    singleLine = false,
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Default,
                    ),
                    textStyle = inputTextStyle,
                    cursorBrush = SolidColor(tokens.ink),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .testTag("standard_message_input"),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.TopStart
                        ) {
                            if (composerText.isEmpty()) {
                                Text(
                                    text = "Message in glyphs...",
                                    style = inputTextStyle.copy(color = tokens.mutedInk),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }

        // Circular Send button (bottom-anchored)
        val canSend = composerText.isNotBlank()
        Box(
            modifier = Modifier
                .size(ComposerActionButtonSize)
                .liquidGlassChrome(
                    shape = CircleShape,
                    fillColor = if (canSend) tokens.ink else tokens.glassSoftFill,
                    borderColor = if (canSend) tokens.ink else tokens.hairline,
                    topHighlightColor = if (canSend) Color.White.copy(alpha = 0.25f) else tokens.specularTop,
                    sheenColor = Color.Transparent,
                )
                .clickable(enabled = canSend, onClick = onSend)
                .testTag("standard_send_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.cd_send),
                tint = if (canSend) tokens.canvas else tokens.mutedInk,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Cipher keyboard composer row with bottom-anchored expand/send pills and a fixed-max-height
 * vertically scrolling preview well for multiline glyph + hint composition.
 */
@Composable
private fun CipherMultilineComposer(
    composerText: String,
    showKeyHints: Boolean,
    cipherKeyboardExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onExpandKeyboard: () -> Unit,
    onSend: () -> Unit,
) {
    val tokens = LocalMonochromeTokens.current
    val cipherPreviewScroll = rememberScrollState()

    LaunchedEffect(composerText) {
        cipherPreviewScroll.scrollTo(cipherPreviewScroll.maxValue)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Expand / Collapse Cipher Keyboard Pill Button
        Box(
            modifier = Modifier
                .size(ComposerActionButtonSize)
                .liquidGlassChrome(
                    shape = CircleShape,
                    fillColor = tokens.glassStrongFill,
                    borderColor = tokens.hairline,
                    topHighlightColor = tokens.specularTop,
                    sheenColor = tokens.specularSheen,
                )
                .clickable(onClick = onToggleExpanded)
                .testTag("toggle_cipher_keyboard_expand"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (cipherKeyboardExpanded) {
                    Icons.Outlined.KeyboardArrowDown
                } else {
                    Icons.Outlined.GridView
                },
                contentDescription = if (cipherKeyboardExpanded) {
                    "Collapse cipher keyboard"
                } else {
                    "Open cipher keyboard"
                },
                tint = tokens.ink,
                modifier = Modifier.size(20.dp)
            )
        }

        // Fixed max-height scrollable multiline cipher preview pill
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = ComposerPillMinHeight, max = ComposerCipherPillMaxHeight)
                .liquidGlassChrome(
                    shape = RoundedCornerShape(22.dp),
                    fillColor = tokens.glassStrongFill,
                    borderColor = tokens.hairline,
                    topHighlightColor = tokens.specularTop,
                    sheenColor = Color.Transparent,
                )
                .then(
                    if (!cipherKeyboardExpanded) {
                        Modifier.clickable(onClick = onExpandKeyboard)
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (composerText.isEmpty()) {
                Text(
                    text = if (cipherKeyboardExpanded) {
                        "Tap glyph keys below..."
                    } else {
                        "Tap to open cipher keyboard..."
                    },
                    style = TextStyle(
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    ),
                    color = tokens.mutedInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(cipherPreviewScroll),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CipherTextDisplay(
                        text = composerText,
                        glyphSize = 18.dp,
                        color = tokens.ink,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (showKeyHints) {
                        Text(
                            text = composerText,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            color = tokens.mutedInk,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("composer_english_preview")
                        )
                    }
                }
            }
        }

        // Compact Circular Send Pill when keyboard is collapsed
        if (!cipherKeyboardExpanded) {
            val canSend = composerText.isNotBlank()
            Box(
                modifier = Modifier
                    .size(ComposerActionButtonSize)
                    .liquidGlassChrome(
                        shape = CircleShape,
                        fillColor = if (canSend) tokens.ink else tokens.glassSoftFill,
                        borderColor = if (canSend) tokens.ink else tokens.hairline,
                        topHighlightColor = if (canSend) Color.White.copy(alpha = 0.25f) else tokens.specularTop,
                        sheenColor = Color.Transparent,
                    )
                    .clickable(enabled = canSend, onClick = onSend)
                    .testTag("collapsed_cipher_send_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.cd_send),
                    tint = if (canSend) tokens.canvas else tokens.mutedInk,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
