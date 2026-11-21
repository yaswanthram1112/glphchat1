package com.example.ui.components

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ChatRoom
import com.example.data.ChatType
import com.example.data.UserAccount
import com.example.data.formatMonospaceTime
import com.example.ui.UiState
import com.example.ui.cipher.CipherTextDisplay
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LocalMonochromeTokens
import com.example.ui.theme.liquidGlassBackdropSource
import com.example.ui.theme.liquidGlassChrome
import com.example.ui.theme.monochromeGlassCanvas
import com.example.ui.theme.rememberLiquidGlassBackdropState

@Composable
fun Sidebar(
    account: UserAccount,
    isDemoMode: Boolean,
    chatsState: UiState<List<ChatRoom>>,
    selectedChatId: String?,
    searchQuery: String,
    isDarkTheme: Boolean,
    globalDecodeActive: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSelectChat: (String) -> Unit,
    onNewDirectClick: () -> Unit,
    onNewGroupClick: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenCipherGuide: () -> Unit,
    onOpenProfile: () -> Unit,
    onRefreshSync: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val tokens = LocalMonochromeTokens.current
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    val backdropState = rememberLiquidGlassBackdropState()

    var headerHeightPx by remember { mutableIntStateOf(0) }
    var footerHeightPx by remember { mutableIntStateOf(0) }
    val topInsetDp = with(density) { headerHeightPx.toDp() }.coerceAtLeast(88.dp)
    val bottomInsetDp = with(density) { footerHeightPx.toDp() }.coerceAtLeast(82.dp)

    val scrollTickProvider: () -> Int = {
        listState.firstVisibleItemIndex * 10_000 +
            listState.firstVisibleItemScrollOffset +
            searchQuery.length * 13
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("sidebar_pane")
    ) {
        // --- 1. FULL-BLEED SCROLLING CONVERSATIONS BACKDROP ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .liquidGlassBackdropSource(backdropState)
                .monochromeGlassCanvas(tokens)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("chat_list"),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = topInsetDp + 8.dp,
                    bottom = bottomInsetDp + 12.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pill-Shaped Frosted Glass Search Input
                item(key = "search_bar") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .liquidGlassChrome(
                                shape = CircleShape,
                                fillColor = tokens.glassFill,
                                borderColor = tokens.hairline,
                                topHighlightColor = tokens.specularTop,
                                sheenColor = tokens.specularSheen,
                            )
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = tokens.mutedInk,
                            modifier = Modifier.size(18.dp)
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.search_chats_placeholder),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = tokens.mutedInk,
                                    maxLines = 1,
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontFamily = InterFontFamily,
                                    fontSize = 14.sp,
                                    color = tokens.ink,
                                ),
                                cursorBrush = SolidColor(tokens.ink),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("sidebar_search_input")
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Clear search",
                                tint = tokens.mutedInk,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onSearchQueryChange("") }
                            )
                        }
                    }
                }

                // Rounded Glass Action Pills: Direct Chat & New Group
                item(key = "quick_actions") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .liquidGlassChrome(
                                    shape = RoundedCornerShape(16.dp),
                                    fillColor = tokens.ink,
                                    borderColor = tokens.ink,
                                    topHighlightColor = if (tokens.isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.22f),
                                    sheenColor = if (tokens.isDark) Color.Transparent else Color.White.copy(alpha = 0.1f),
                                )
                                .clickable(onClick = onNewDirectClick)
                                .testTag("new_direct_chat_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                tint = tokens.canvas,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Direct Chat",
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = tokens.canvas,
                            )
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .liquidGlassChrome(
                                    shape = RoundedCornerShape(16.dp),
                                    fillColor = tokens.glassFill,
                                    borderColor = tokens.hairlineStrong,
                                    topHighlightColor = tokens.specularTop,
                                    sheenColor = tokens.specularSheen,
                                )
                                .clickable(onClick = onNewGroupClick)
                                .testTag("new_group_chat_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.GroupAdd,
                                contentDescription = null,
                                tint = tokens.ink,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "New Group",
                                style = MaterialTheme.typography.labelLarge,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = tokens.ink,
                            )
                        }
                    }
                }

                when (chatsState) {
                    is UiState.Loading -> {
                        item(key = "loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = tokens.ink,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }

                    is UiState.Error -> {
                        item(key = "error") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .liquidGlassChrome(
                                        shape = RoundedCornerShape(22.dp),
                                        fillColor = tokens.glassFill,
                                        borderColor = tokens.hairline,
                                        topHighlightColor = tokens.specularTop,
                                        sheenColor = tokens.specularSheen,
                                    )
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Sync Error",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = tokens.ink,
                                    )
                                    Text(
                                        text = chatsState.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = tokens.mutedInk,
                                    )
                                }
                            }
                        }
                    }

                    is UiState.Success -> {
                        val query = searchQuery.trim().lowercase()
                        val filtered = chatsState.data.filter { room ->
                            query.isEmpty() ||
                                room.displayTitleFor(account.userId).lowercase().contains(query) ||
                                room.subtitleFor(account.userId).lowercase().contains(query) ||
                                room.lastMessageText.lowercase().contains(query)
                        }

                        if (filtered.isEmpty()) {
                            item(key = "empty") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 24.dp)
                                        .liquidGlassChrome(
                                            shape = RoundedCornerShape(24.dp),
                                            fillColor = tokens.glassFill,
                                            borderColor = tokens.hairline,
                                            topHighlightColor = tokens.specularTop,
                                            sheenColor = tokens.specularSheen,
                                        )
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (query.isEmpty()) "No Conversations Yet" else "No Matches",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = tokens.ink,
                                        )
                                        Text(
                                            text = "Start a direct chat by @handle or create a group.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = tokens.mutedInk,
                                        )
                                    }
                                }
                            }
                        } else {
                            items(filtered, key = { it.id }) { room ->
                                ChatListRow(
                                    room = room,
                                    currentUserId = account.userId,
                                    selected = room.id == selectedChatId,
                                    decodePreview = globalDecodeActive,
                                    onClick = { onSelectChat(room.id) },
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 2. FLOATING FROSTED-GLASS TOP HEADER ---
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
                        .height(58.dp)
                        .padding(start = 16.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "GlyphChat",
                            style = MaterialTheme.typography.headlineSmall,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = tokens.ink,
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(tokens.glassSoftFill, CircleShape)
                                .border(1.dp, tokens.hairline, CircleShape)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isDemoMode) stringResource(R.string.mode_local) else stringResource(R.string.mode_cloud),
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.mutedInk,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onOpenCipherGuide,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("open_cipher_guide_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.GridView,
                                contentDescription = stringResource(R.string.cd_cipher_guide),
                                tint = tokens.ink,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        IconButton(
                            onClick = onToggleTheme,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("sidebar_toggle_theme_button")
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                contentDescription = stringResource(R.string.cd_toggle_theme),
                                tint = tokens.ink,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        IconButton(
                            onClick = onRefreshSync,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("sidebar_refresh_sync_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Sync Over The Air",
                                tint = tokens.ink,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        IconButton(
                            onClick = onOpenProfile,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("open_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = stringResource(R.string.cd_profile),
                                tint = tokens.ink,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- 3. FLOATING FROSTED-GLASS IDENTITY FOOTER ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { footerHeightPx = it.height }
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            LiquidGlassSurface(
                shape = RoundedCornerShape(22.dp),
                backdropState = backdropState,
                scrollInvalidationProvider = scrollTickProvider,
                blurRadius = 20.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenProfile)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.size(34.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .liquidGlassChrome(
                                        shape = CircleShape,
                                        fillColor = tokens.ink,
                                        borderColor = tokens.ink,
                                        topHighlightColor = Color.White.copy(alpha = 0.25f),
                                        sheenColor = Color.Transparent,
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = account.displayName.trim().take(2).uppercase().ifBlank { "ME" },
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = tokens.canvas,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(tokens.ink)
                                    .border(1.5.dp, tokens.canvas, CircleShape)
                            )
                        }

                        Column {
                            Text(
                                text = account.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = tokens.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "@${account.handle}#${account.pin}",
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.mutedInk,
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(tokens.glassSoftFill, CircleShape)
                            .border(1.dp, tokens.hairline, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = tokens.ink,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatListRow(
    room: ChatRoom,
    currentUserId: String,
    selected: Boolean,
    decodePreview: Boolean,
    onClick: () -> Unit,
) {
    val tokens = LocalMonochromeTokens.current
    val title = room.displayTitleFor(currentUserId)
    val monogram = title.trim().take(2).uppercase().ifBlank { "GC" }
    val isGroup = room.resolvedType() == ChatType.GROUP
    val isOwnLast = room.lastMessageSenderId == currentUserId

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassChrome(
                shape = RoundedCornerShape(22.dp),
                fillColor = if (selected) tokens.glassStrongFill else tokens.glassFill,
                borderColor = if (selected) tokens.hairlineStrong else tokens.hairline,
                topHighlightColor = tokens.specularTop,
                sheenColor = if (selected) tokens.specularSheen else tokens.specularSheen.copy(alpha = 0.5f),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("chat_row_${room.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Circular Monochrome Monogram Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .liquidGlassChrome(
                    shape = CircleShape,
                    fillColor = if (selected) tokens.ink else tokens.glassSoftFill,
                    borderColor = if (selected) tokens.ink else tokens.hairlineStrong,
                    topHighlightColor = if (selected) Color.White.copy(alpha = 0.28f) else tokens.specularTop,
                    sheenColor = if (selected) Color.Transparent else tokens.specularSheen,
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = monogram,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (selected) tokens.canvas else tokens.ink,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isGroup) {
                        Text(
                            text = "GRP",
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.mutedInk,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(tokens.glassSoftFill, CircleShape)
                                .border(1.dp, tokens.hairline, CircleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = formatMonospaceTime(room.lastMessageAt ?: room.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.mutedInk,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (isOwnLast && room.lastMessageText.isNotBlank()) {
                    val readByOther = room.isLastMessageReadByOther(currentUserId)
                    Icon(
                        imageVector = if (readByOther) Icons.Filled.DoneAll else Icons.Filled.Done,
                        contentDescription = if (readByOther) "Read" else "Sent",
                        tint = if (readByOther) tokens.ink else tokens.mutedInk,
                        modifier = Modifier.size(14.dp)
                    )
                }

                if (room.lastMessageText.isBlank()) {
                    Text(
                        text = "No messages yet — tap to compose",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.mutedInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else if (decodePreview) {
                    Text(
                        text = room.lastMessageText,
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.mutedInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    CipherTextDisplay(
                        text = room.lastMessageText,
                        glyphSize = 13.dp,
                        color = tokens.mutedInk,
                        strokeWidthUnits = 2.0f,
                        maxChars = 20,
                    )
                }
            }
        }
    }
}
