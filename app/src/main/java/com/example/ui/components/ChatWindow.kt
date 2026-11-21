package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ChatMessage
import com.example.data.ChatRoom
import com.example.data.ChatType
import com.example.data.HandleDirectoryEntry
import com.example.data.dayBucketKey
import com.example.data.formatDateSeparator
import com.example.data.formatMonospaceTime
import com.example.ui.UiState
import com.example.ui.cipher.CipherTextDisplay
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LocalMonochromeTokens
import com.example.ui.theme.liquidGlassBackdropSource
import com.example.ui.theme.liquidGlassChrome
import com.example.ui.theme.monochromeGlassCanvas
import com.example.ui.theme.rememberLiquidGlassBackdropState

@Composable
fun ChatWindow(
    chatRoom: ChatRoom,
    currentUserId: String,
    peerPresence: HandleDirectoryEntry?,
    messagesState: UiState<List<ChatMessage>>,
    composerText: String,
    replyTarget: ChatMessage?,
    globalDecodeActive: Boolean,
    decodedMessageIds: Set<String>,
    showKeyHints: Boolean,
    useStandardKeyboard: Boolean,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onToggleGlobalDecode: () -> Unit,
    onToggleMessageDecode: (String) -> Unit,
    onOpenGroupInfo: () -> Unit,
    onReplyMessage: (ChatMessage) -> Unit,
    onClearReply: () -> Unit,
    onDeleteMessage: (ChatMessage) -> Unit,
    onAppendCipherText: (String) -> Unit,
    onSetComposerText: (String) -> Unit,
    onBackspace: () -> Unit,
    onSend: () -> Unit,
    onToggleKeyHints: () -> Unit,
    onSwitchKeyboardMode: (Boolean) -> Unit,
    onMarkRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalMonochromeTokens.current
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    val backdropState = rememberLiquidGlassBackdropState()
    val isGroup = chatRoom.resolvedType() == ChatType.GROUP
    val title = chatRoom.displayTitleFor(currentUserId)
    val monogram = title.trim().take(2).uppercase().ifBlank { "GC" }

    val messageList = (messagesState as? UiState.Success)?.data ?: emptyList()

    // Smoothly expand/collapse the custom Cipher Keyboard underneath the floating glass composer pill
    var cipherKeyboardExpanded by rememberSaveable(chatRoom.id) { mutableStateOf(true) }

    // Dynamic measurements for full-bleed scroll content under floating frosted-glass bars
    var headerHeightPx by remember { mutableIntStateOf(0) }
    var bottomDockHeightPx by remember { mutableIntStateOf(0) }
    val topInsetDp = with(density) { headerHeightPx.toDp() }.coerceAtLeast(88.dp)
    val bottomInsetDp = with(density) { bottomDockHeightPx.toDp() }.coerceAtLeast(92.dp)

    // Draw-phase scroll invalidation provider (zero recompositions on scroll frames)
    val scrollTickProvider: () -> Int = {
        listState.firstVisibleItemIndex * 10_000 +
            listState.firstVisibleItemScrollOffset +
            messageList.size * 31 +
            (if (globalDecodeActive) 1 else 0) +
            decodedMessageIds.size * 7
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(chatRoom.id, messageList.size) {
        if (messageList.isNotEmpty()) {
            listState.animateScrollToItem(messageList.lastIndex)
        }
    }

    // STRICT UNREAD GUARD: Only trigger mark-as-read when there are actual unread items
    val hasUnread = remember(chatRoom.readByUids, messageList, currentUserId) {
        !chatRoom.readByUids.contains(currentUserId) ||
            messageList.any { !it.readByUids.contains(currentUserId) }
    }
    LaunchedEffect(chatRoom.id, hasUnread) {
        if (hasUnread) {
            onMarkRead()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("chat_window")
    ) {
        // --- 1. FULL-BLEED BACKDROP SOURCE: MONOCHROME CANVAS + SCROLLING MESSAGES ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .liquidGlassBackdropSource(backdropState)
                .monochromeGlassCanvas(tokens)
        ) {
            when (messagesState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center),
                        color = tokens.ink,
                        strokeWidth = 2.dp
                    )
                }

                is UiState.Error -> {
                    LiquidGlassSurface(
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Message Stream Error",
                                style = MaterialTheme.typography.titleSmall,
                                color = tokens.ink
                            )
                            Text(
                                text = messagesState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.mutedInk
                            )
                        }
                    }
                }

                is UiState.Success -> {
                    if (messageList.isEmpty()) {
                        LiquidGlassSurface(
                            shape = RoundedCornerShape(26.dp),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 28.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CipherTextDisplay(
                                    text = "HELLO",
                                    glyphSize = 24.dp,
                                    color = tokens.ink,
                                )
                                Text(
                                    text = "No messages yet. Compose with the cipher keyboard below.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = tokens.mutedInk,
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("message_list"),
                            contentPadding = PaddingValues(
                                start = 14.dp,
                                end = 14.dp,
                                top = topInsetDp + 10.dp,
                                bottom = bottomInsetDp + 14.dp,
                            ),
                        ) {
                            itemsIndexed(
                                items = messageList,
                                key = { _, item -> item.id }
                            ) { index, message ->
                                val prev = messageList.getOrNull(index - 1)
                                val next = messageList.getOrNull(index + 1)

                                val prevDay = prev?.let { dayBucketKey(it.createdAt) }
                                val curDay = dayBucketKey(message.createdAt)
                                val nextDay = next?.let { dayBucketKey(it.createdAt) }

                                val showDateHeader = index == 0 || prevDay != curDay
                                if (showDateHeader) {
                                    DateSeparatorPill(label = formatDateSeparator(message.createdAt))
                                }

                                val isOwn = message.senderId == currentUserId
                                val isDecoded = globalDecodeActive || decodedMessageIds.contains(message.id)

                                // Consecutive bubble grouping (WhatsApp & Instagram DM style)
                                val sameAsPrev = !showDateHeader && prev.senderId == message.senderId
                                val sameAsNext = next != null && nextDay == curDay && next.senderId == message.senderId

                                val topSpacer = when {
                                    showDateHeader -> 4.dp
                                    sameAsPrev -> 3.dp
                                    else -> 12.dp
                                }
                                Spacer(modifier = Modifier.height(topSpacer))

                                MessageBubbleRow(
                                    message = message,
                                    isOwn = isOwn,
                                    sameAsPrev = sameAsPrev,
                                    sameAsNext = sameAsNext,
                                    showSenderName = isGroup && !isOwn && !sameAsPrev,
                                    isDecoded = isDecoded,
                                    isReadByOthers = message.isReadByOthers(currentUserId),
                                    onToggleDecode = { onToggleMessageDecode(message.id) },
                                    onReply = { onReplyMessage(message) },
                                    onDelete = { onDeleteMessage(message) },
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 2. FLOATING FROSTED-GLASS TOP HEADER (BLURRED OVER SCROLLING MESSAGES) ---
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .onSizeChanged { headerHeightPx = it.height }
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            LiquidGlassSurface(
                shape = RoundedCornerShape(24.dp),
                backdropState = backdropState,
                scrollInvalidationProvider = scrollTickProvider,
                blurRadius = 20.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(start = if (showBackButton) 4.dp else 12.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (showBackButton) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("chat_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.cd_back),
                                    tint = tokens.ink,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }

                        // Circular Monochrome Contact/Group Avatar with Live Presence Dot
                        val isOnline = !isGroup && peerPresence?.isCurrentlyOnline() == true
                        Box(modifier = Modifier.size(38.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .liquidGlassChrome(
                                        shape = CircleShape,
                                        fillColor = tokens.glassStrongFill,
                                        borderColor = tokens.hairlineStrong,
                                        topHighlightColor = tokens.specularTop,
                                        sheenColor = tokens.specularSheen,
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = monogram,
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = tokens.ink,
                                )
                            }
                            if (!isGroup) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(if (isOnline) tokens.ink else tokens.canvas)
                                        .border(1.5.dp, if (isOnline) tokens.canvas else tokens.mutedInk, CircleShape)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = tokens.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = if (!isGroup) {
                                    if (isOnline) "Online · ${chatRoom.subtitleFor(currentUserId)}"
                                    else "Offline · ${chatRoom.subtitleFor(currentUserId)}"
                                } else {
                                    "${chatRoom.memberUids.size} members · Group"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = tokens.mutedInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Quick Peek Frosted Pill Toggle
                        Row(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(CircleShape)
                                .background(if (globalDecodeActive) tokens.ink else tokens.glassStrongFill, CircleShape)
                                .border(
                                    1.dp,
                                    if (globalDecodeActive) tokens.ink else tokens.hairlineStrong,
                                    CircleShape
                                )
                                .clickable(onClick = onToggleGlobalDecode)
                                .padding(horizontal = 12.dp)
                                .testTag("header_peek_toggle_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (globalDecodeActive) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = stringResource(R.string.cd_peek_all),
                                tint = if (globalDecodeActive) tokens.canvas else tokens.ink,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (globalDecodeActive) "Plain" else "Peek",
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (globalDecodeActive) tokens.canvas else tokens.ink,
                            )
                        }

                        if (isGroup) {
                            IconButton(
                                onClick = onOpenGroupInfo,
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("chat_group_info_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Group,
                                    contentDescription = stringResource(R.string.cd_group_info),
                                    tint = tokens.ink,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 3. FLOATING FROSTED-GLASS BOTTOM COMPOSER & SMOOTHLY EXPANDING CIPHER KEYBOARD ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { bottomDockHeightPx = it.height }
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .navigationBarsPadding()
                .then(if (useStandardKeyboard) Modifier.imePadding() else Modifier)
        ) {
            MessageComposer(
                composerText = composerText,
                replyTarget = replyTarget,
                showKeyHints = showKeyHints,
                useStandardKeyboard = useStandardKeyboard,
                cipherKeyboardExpanded = cipherKeyboardExpanded,
                backdropState = backdropState,
                scrollInvalidationProvider = scrollTickProvider,
                onClearReply = onClearReply,
                onAppendCipherText = onAppendCipherText,
                onSetComposerText = onSetComposerText,
                onBackspace = onBackspace,
                onSend = onSend,
                onToggleKeyHints = onToggleKeyHints,
                onSwitchKeyboardMode = onSwitchKeyboardMode,
                onToggleCipherKeyboardExpanded = { expanded -> cipherKeyboardExpanded = expanded },
            )
        }
    }
}

@Composable
private fun DateSeparatorPill(label: String) {
    val tokens = LocalMonochromeTokens.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .liquidGlassChrome(
                    shape = CircleShape,
                    fillColor = tokens.glassFill,
                    borderColor = tokens.hairline,
                    topHighlightColor = tokens.specularTop,
                    sheenColor = tokens.specularSheen,
                )
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = tokens.mutedInk,
            )
        }
    }
}

@Composable
private fun MessageBubbleRow(
    message: ChatMessage,
    isOwn: Boolean,
    sameAsPrev: Boolean,
    sameAsNext: Boolean,
    showSenderName: Boolean,
    isDecoded: Boolean,
    isReadByOthers: Boolean,
    onToggleDecode: () -> Unit,
    onReply: () -> Unit,
    onDelete: () -> Unit,
) {
    val tokens = LocalMonochromeTokens.current

    // WhatsApp & Instagram DM grouped tail corner radii
    val large = 20.dp
    val tight = 6.dp
    val bubbleShape = if (isOwn) {
        RoundedCornerShape(
            topStart = large,
            topEnd = if (sameAsPrev) tight else large,
            bottomEnd = if (sameAsNext) tight else 5.dp,
            bottomStart = large,
        )
    } else {
        RoundedCornerShape(
            topStart = if (sameAsPrev) tight else large,
            topEnd = large,
            bottomEnd = large,
            bottomStart = if (sameAsNext) tight else 5.dp,
        )
    }

    val bubbleBg = if (isOwn) tokens.outgoingBubble else tokens.incomingBubble
    val bubbleFg = if (isOwn) tokens.canvas else tokens.ink
    val bubbleMuted = if (isOwn) tokens.canvas.copy(alpha = 0.68f) else tokens.mutedInk
    val bubbleBorder = if (isOwn) {
        if (tokens.isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.10f)
    } else {
        tokens.hairline
    }
    val bubbleHighlight = if (isOwn) {
        if (tokens.isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.18f)
    } else {
        tokens.specularTop
    }
    val bubbleSheen = if (isOwn) {
        if (tokens.isDark) Color.Transparent else Color.White.copy(alpha = 0.08f)
    } else {
        tokens.specularSheen
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 128.dp, max = 308.dp)
                .liquidGlassChrome(
                    shape = bubbleShape,
                    fillColor = bubbleBg,
                    borderColor = bubbleBorder,
                    topHighlightColor = bubbleHighlight,
                    sheenColor = bubbleSheen,
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("message_bubble_${message.id}"),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (showSenderName) {
                Text(
                    text = "${message.senderName}  @${message.senderHandle}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = bubbleMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Soft Rounded Reply Quote Well inside bubble
            if (!message.deleted && !message.replyToText.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(bubbleFg.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .border(1.dp, bubbleFg.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(26.dp)
                            .clip(CircleShape)
                            .background(bubbleFg.copy(alpha = 0.7f))
                    )
                    Column {
                        if (!message.replyToSenderName.isNullOrBlank()) {
                            Text(
                                text = message.replyToSenderName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = bubbleFg,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            text = message.replyToText,
                            style = MaterialTheme.typography.bodySmall,
                            color = bubbleMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // Message Body: Deleted vs Plain English Decode vs Live SVG Cipher Glyphs
            if (message.deleted) {
                Text(
                    text = stringResource(R.string.deleted_message_placeholder),
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = bubbleMuted,
                )
            } else {
                AnimatedContent(
                    targetState = isDecoded,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "bubble_decode_${message.id}"
                ) { decoded ->
                    if (decoded) {
                        Text(
                            text = message.text,
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = bubbleFg,
                        )
                    } else {
                        CipherTextDisplay(
                            text = message.text,
                            glyphSize = 20.dp,
                            color = bubbleFg,
                        )
                    }
                }
            }

            // Subtle Inline Meta & Micro-Action Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!message.deleted) {
                        Icon(
                            imageVector = if (isDecoded) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = stringResource(R.string.cd_peek_bubble),
                            tint = bubbleMuted,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable(onClick = onToggleDecode)
                                .testTag("peek_bubble_${message.id}")
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = stringResource(R.string.cd_reply),
                            tint = bubbleMuted,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable(onClick = onReply)
                                .testTag("reply_bubble_${message.id}")
                        )
                        if (isOwn) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = stringResource(R.string.cd_delete),
                                tint = bubbleMuted,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable(onClick = onDelete)
                                    .testTag("delete_bubble_${message.id}")
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formatMonospaceTime(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = bubbleMuted,
                    )
                    if (isOwn) {
                        Icon(
                            imageVector = if (isReadByOthers) Icons.Filled.DoneAll else Icons.Filled.Done,
                            contentDescription = if (isReadByOthers) "Read" else "Sent",
                            tint = if (isReadByOthers) bubbleFg else bubbleMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
