package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.ChatRoom
import com.example.data.HandleDirectoryEntry
import com.example.data.UserAccount
import com.example.data.normalizeHandleInput
import com.example.ui.HandleLookupState
import com.example.ui.cipher.CipherGlyph
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LocalMonochromeTokens
import com.example.ui.theme.liquidGlassChrome

@Composable
private fun LiquidGlassDialogFrame(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val tokens = LocalMonochromeTokens.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        LiquidGlassSurface(
            shape = RoundedCornerShape(28.dp),
            fillColor = tokens.glassStrongFill,
            borderColor = tokens.hairlineStrong,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .widthIn(max = 440.dp)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .padding(start = 20.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.ink,
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(tokens.glassSoftFill, CircleShape)
                            .border(1.dp, tokens.hairline, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.cd_close),
                            tint = tokens.ink,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = tokens.hairline)
                Box(modifier = Modifier.padding(18.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun NewDirectChatDialog(
    lookupState: HandleLookupState,
    demoDirectory: List<HandleDirectoryEntry>,
    onLookupHandle: (String) -> Unit,
    onSelectPeer: (HandleDirectoryEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = LocalMonochromeTokens.current
    var handleInput by remember { mutableStateOf("") }

    LiquidGlassDialogFrame(title = "New Direct Chat", onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .liquidGlassChrome(
                            shape = RoundedCornerShape(16.dp),
                            fillColor = tokens.glassSoftFill,
                            borderColor = tokens.hairline,
                            topHighlightColor = tokens.specularTop,
                            sheenColor = Color.Transparent,
                        )
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "@",
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 14.sp,
                        color = tokens.mutedInk,
                        modifier = Modifier.padding(end = 3.dp)
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (handleInput.isEmpty()) {
                            Text(
                                text = "handle (e.g. elena_s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = tokens.mutedInk,
                            )
                        }
                        BasicTextField(
                            value = handleInput,
                            onValueChange = { handleInput = normalizeHandleInput(it) },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 13.sp,
                                color = tokens.ink,
                            ),
                            cursorBrush = SolidColor(tokens.ink),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("direct_handle_search_input")
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .liquidGlassChrome(
                            shape = RoundedCornerShape(16.dp),
                            fillColor = tokens.ink,
                            borderColor = tokens.ink,
                            topHighlightColor = Color.White.copy(alpha = 0.25f),
                            sheenColor = Color.Transparent,
                        )
                        .clickable { onLookupHandle(handleInput) }
                        .testTag("direct_handle_search_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (lookupState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = tokens.canvas,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search handle",
                            tint = tokens.canvas,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            if (lookupState.error != null) {
                Text(
                    text = lookupState.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.ink,
                )
            } else if (lookupState.notFound) {
                Text(
                    text = "No user found with handle @$handleInput.",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.mutedInk,
                )
            } else if (lookupState.result != null) {
                PeerResultCard(
                    entry = lookupState.result,
                    actionLabel = "Chat",
                    onClick = { onSelectPeer(lookupState.result) }
                )
            }

            if (demoDirectory.isNotEmpty()) {
                HorizontalDivider(thickness = 1.dp, color = tokens.hairline)
                Text(
                    text = "Local Directory",
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.mutedInk,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    demoDirectory.forEach { entry ->
                        PeerResultCard(
                            entry = entry,
                            actionLabel = "Open",
                            onClick = { onSelectPeer(entry) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreateGroupDialog(
    lookupState: HandleLookupState,
    demoDirectory: List<HandleDirectoryEntry>,
    onLookupHandle: (String) -> Unit,
    onCreateGroup: (name: String, members: List<HandleDirectoryEntry>) -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = LocalMonochromeTokens.current
    var groupName by remember { mutableStateOf("") }
    var handleInput by remember { mutableStateOf("") }
    val selectedMembers = remember { mutableStateListOf<HandleDirectoryEntry>() }

    LiquidGlassDialogFrame(title = "Create Group Chat", onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Group Name",
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.mutedInk,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .liquidGlassChrome(
                            shape = RoundedCornerShape(16.dp),
                            fillColor = tokens.glassSoftFill,
                            borderColor = tokens.hairline,
                            topHighlightColor = tokens.specularTop,
                            sheenColor = Color.Transparent,
                        )
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (groupName.isEmpty()) {
                        Text(
                            text = "e.g. Cipher Desk",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.mutedInk,
                        )
                    }
                    BasicTextField(
                        value = groupName,
                        onValueChange = { groupName = it.take(60) },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontFamily = InterFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = tokens.ink,
                        ),
                        cursorBrush = SolidColor(tokens.ink),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("group_name_input")
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Add Members by @handle",
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.mutedInk,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .liquidGlassChrome(
                                shape = RoundedCornerShape(16.dp),
                                fillColor = tokens.glassSoftFill,
                                borderColor = tokens.hairline,
                                topHighlightColor = tokens.specularTop,
                                sheenColor = Color.Transparent,
                            )
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "@",
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 13.sp,
                            color = tokens.mutedInk,
                        )
                        BasicTextField(
                            value = handleInput,
                            onValueChange = { handleInput = normalizeHandleInput(it) },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 13.sp,
                                color = tokens.ink,
                            ),
                            cursorBrush = SolidColor(tokens.ink),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("group_member_handle_input")
                        )
                    }
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .liquidGlassChrome(
                                shape = RoundedCornerShape(16.dp),
                                fillColor = tokens.glassFill,
                                borderColor = tokens.hairlineStrong,
                                topHighlightColor = tokens.specularTop,
                                sheenColor = tokens.specularSheen,
                            )
                            .clickable { onLookupHandle(handleInput) }
                            .padding(horizontal = 14.dp)
                            .testTag("group_lookup_member_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Find",
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 12.sp,
                            color = tokens.ink,
                        )
                    }
                }
            }

            if (lookupState.result != null) {
                val entry = lookupState.result
                val alreadyAdded = selectedMembers.any { it.userId == entry.userId }
                PeerResultCard(
                    entry = entry,
                    actionLabel = if (alreadyAdded) "Added" else "Add",
                    onClick = {
                        if (!alreadyAdded) selectedMembers.add(entry)
                    }
                )
            } else if (lookupState.notFound) {
                Text(
                    text = "Handle @$handleInput not found.",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.mutedInk,
                )
            }

            if (demoDirectory.isNotEmpty()) {
                Text(
                    text = "Quick Select",
                    style = MaterialTheme.typography.labelMedium,
                    color = tokens.mutedInk,
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    demoDirectory.forEach { entry ->
                        val added = selectedMembers.any { it.userId == entry.userId }
                        PeerResultCard(
                            entry = entry,
                            actionLabel = if (added) "Remove" else "Add",
                            onClick = {
                                if (added) selectedMembers.removeAll { it.userId == entry.userId }
                                else selectedMembers.add(entry)
                            }
                        )
                    }
                }
            }

            if (selectedMembers.isNotEmpty()) {
                Text(
                    text = "Selected (${selectedMembers.size}): ${selectedMembers.joinToString(", ") { "@${it.handle}" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.ink,
                )
            }

            val canCreate = groupName.trim().isNotEmpty()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .liquidGlassChrome(
                        shape = RoundedCornerShape(16.dp),
                        fillColor = if (canCreate) tokens.ink else tokens.glassSoftFill,
                        borderColor = if (canCreate) tokens.ink else tokens.hairline,
                        topHighlightColor = if (canCreate) Color.White.copy(alpha = 0.25f) else tokens.specularTop,
                        sheenColor = Color.Transparent,
                    )
                    .clickable(enabled = canCreate) {
                        onCreateGroup(groupName, selectedMembers.toList())
                    }
                    .testTag("confirm_create_group_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Create Group",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (canCreate) tokens.canvas else tokens.mutedInk,
                )
            }
        }
    }
}

@Composable
fun GroupInfoDialog(
    chatRoom: ChatRoom,
    currentUserId: String,
    lookupState: HandleLookupState,
    onLookupHandle: (String) -> Unit,
    onRenameGroup: (String) -> Unit,
    onAddMember: (HandleDirectoryEntry) -> Unit,
    onRemoveMember: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = LocalMonochromeTokens.current
    val isCreator = chatRoom.creatorId == currentUserId
    var nameInput by remember(chatRoom.name) { mutableStateOf(chatRoom.name) }
    var addHandleInput by remember { mutableStateOf("") }

    LiquidGlassDialogFrame(title = "Group Settings", onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isCreator) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .liquidGlassChrome(
                                shape = RoundedCornerShape(16.dp),
                                fillColor = tokens.glassSoftFill,
                                borderColor = tokens.hairline,
                                topHighlightColor = tokens.specularTop,
                                sheenColor = Color.Transparent,
                            )
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it.take(60) },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = InterFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = tokens.ink,
                            ),
                            cursorBrush = SolidColor(tokens.ink),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .liquidGlassChrome(
                                shape = RoundedCornerShape(16.dp),
                                fillColor = tokens.ink,
                                borderColor = tokens.ink,
                                topHighlightColor = Color.White.copy(alpha = 0.25f),
                                sheenColor = Color.Transparent,
                            )
                            .clickable(enabled = nameInput.trim().isNotEmpty() && nameInput.trim() != chatRoom.name) {
                                onRenameGroup(nameInput.trim())
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Save",
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 12.sp,
                            color = tokens.canvas,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .liquidGlassChrome(
                                shape = RoundedCornerShape(16.dp),
                                fillColor = tokens.glassSoftFill,
                                borderColor = tokens.hairline,
                                topHighlightColor = tokens.specularTop,
                                sheenColor = Color.Transparent,
                            )
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "@", fontFamily = JetBrainsMonoFamily, fontSize = 13.sp, color = tokens.mutedInk)
                        BasicTextField(
                            value = addHandleInput,
                            onValueChange = { addHandleInput = normalizeHandleInput(it) },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 13.sp,
                                color = tokens.ink,
                            ),
                            cursorBrush = SolidColor(tokens.ink),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .liquidGlassChrome(
                                shape = RoundedCornerShape(16.dp),
                                fillColor = tokens.glassFill,
                                borderColor = tokens.hairlineStrong,
                                topHighlightColor = tokens.specularTop,
                                sheenColor = tokens.specularSheen,
                            )
                            .clickable { onLookupHandle(addHandleInput) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Find member to add",
                            tint = tokens.ink,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (lookupState.result != null) {
                    val entry = lookupState.result
                    val alreadyIn = chatRoom.memberUids.contains(entry.userId)
                    PeerResultCard(
                        entry = entry,
                        actionLabel = if (alreadyIn) "Member" else "Add",
                        onClick = {
                            if (!alreadyIn) onAddMember(entry)
                        }
                    )
                }
            }

            HorizontalDivider(thickness = 1.dp, color = tokens.hairline)
            Text(
                text = "Members (${chatRoom.memberUids.size})",
                style = MaterialTheme.typography.labelMedium,
                color = tokens.mutedInk,
            )

            LazyColumn(
                modifier = Modifier.heightIn(max = 220.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatRoom.membersList(), key = { it.uid }) { member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlassChrome(
                                shape = RoundedCornerShape(18.dp),
                                fillColor = tokens.glassSoftFill,
                                borderColor = tokens.hairline,
                                topHighlightColor = tokens.specularTop,
                                sheenColor = Color.Transparent,
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = member.displayName + if (member.uid == chatRoom.creatorId) " · Admin" else "",
                                style = MaterialTheme.typography.titleSmall,
                                color = tokens.ink,
                            )
                            Text(
                                text = "@${member.handle}",
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.mutedInk,
                            )
                        }
                        if (isCreator && member.uid != currentUserId) {
                            IconButton(
                                onClick = { onRemoveMember(member.uid) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Remove member",
                                    tint = tokens.ink,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (!isCreator) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .liquidGlassChrome(
                            shape = RoundedCornerShape(16.dp),
                            fillColor = tokens.glassFill,
                            borderColor = tokens.hairlineStrong,
                            topHighlightColor = tokens.specularTop,
                            sheenColor = tokens.specularSheen,
                        )
                        .clickable { onRemoveMember(currentUserId) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Leave Group",
                        style = MaterialTheme.typography.labelLarge,
                        color = tokens.ink,
                    )
                }
            }
        }
    }
}

@Composable
fun CipherGuideDialog(onDismiss: () -> Unit) {
    val tokens = LocalMonochromeTokens.current
    val alphabet = remember { ('A'..'Z').toList() }

    LiquidGlassDialogFrame(title = "26-Glyph Cipher Guide", onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "All messages are stored in English and rendered live through this 1-to-1 geometric vector script.",
                style = MaterialTheme.typography.bodySmall,
                color = tokens.mutedInk,
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(alphabet, key = { it }) { letter ->
                    Column(
                        modifier = Modifier
                            .liquidGlassChrome(
                                shape = RoundedCornerShape(14.dp),
                                fillColor = tokens.glassSoftFill,
                                borderColor = tokens.hairline,
                                topHighlightColor = tokens.specularTop,
                                sheenColor = tokens.specularSheen,
                            )
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CipherGlyph(
                            letter = letter,
                            size = 26.dp,
                            color = tokens.ink,
                        )
                        Text(
                            text = letter.toString(),
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = tokens.mutedInk,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileDialog(
    account: UserAccount,
    isDemoMode: Boolean,
    onSignOutOrExitDemo: () -> Unit,
    onDismiss: () -> Unit,
    onRefreshSync: () -> Unit = {},
) {
    val tokens = LocalMonochromeTokens.current

    LiquidGlassDialogFrame(title = "Account Identity", onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlassChrome(
                        shape = RoundedCornerShape(20.dp),
                        fillColor = tokens.glassSoftFill,
                        borderColor = tokens.hairline,
                        topHighlightColor = tokens.specularTop,
                        sheenColor = tokens.specularSheen,
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = account.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = tokens.ink,
                )
                Text(
                    text = "@${account.handle}#${account.pin}",
                    fontFamily = JetBrainsMonoFamily,
                    fontSize = 14.sp,
                    color = tokens.ink,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isDemoMode) "Sync: Local Demo Engine" else "Sync: Firebase Cloud Firestore",
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.mutedInk,
                )
            }

            Text(
                text = "Share your @${account.handle} handle with peers so they can start an encrypted glyph chat with you.",
                style = MaterialTheme.typography.bodySmall,
                color = tokens.mutedInk,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .liquidGlassChrome(
                        shape = RoundedCornerShape(16.dp),
                        fillColor = tokens.glassStrongFill,
                        borderColor = tokens.hairlineStrong,
                        topHighlightColor = tokens.specularTop,
                        sheenColor = tokens.specularSheen,
                    )
                    .clickable {
                        onRefreshSync()
                        onDismiss()
                    }
                    .testTag("profile_refresh_sync_button")
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Sync Over The Air",
                    tint = tokens.ink,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sync & Refresh Account (OTA)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = tokens.ink,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .liquidGlassChrome(
                        shape = RoundedCornerShape(16.dp),
                        fillColor = tokens.ink,
                        borderColor = tokens.ink,
                        topHighlightColor = Color.White.copy(alpha = 0.25f),
                        sheenColor = Color.Transparent,
                    )
                    .clickable(onClick = onSignOutOrExitDemo)
                    .testTag("profile_sign_out_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isDemoMode) "Exit Demo / Sign in with Google" else "Sign Out",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens.canvas,
                )
            }
        }
    }
}

@Composable
private fun PeerResultCard(
    entry: HandleDirectoryEntry,
    actionLabel: String,
    onClick: () -> Unit,
) {
    val tokens = LocalMonochromeTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlassChrome(
                shape = RoundedCornerShape(18.dp),
                fillColor = tokens.glassSoftFill,
                borderColor = tokens.hairline,
                topHighlightColor = tokens.specularTop,
                sheenColor = Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.size(36.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(tokens.glassFill, CircleShape)
                        .border(1.dp, tokens.hairlineStrong, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = entry.displayName.trim().take(2).uppercase().ifBlank { "GC" },
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = tokens.ink,
                    )
                }
                val isOnline = entry.isCurrentlyOnline()
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(if (isOnline) tokens.ink else tokens.canvas)
                        .border(1.5.dp, if (isOnline) tokens.canvas else tokens.mutedInk, CircleShape)
                )
            }

            Column {
                Text(
                    text = entry.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = tokens.ink,
                )
                Text(
                    text = "@${entry.handle}#${entry.pin}",
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.mutedInk,
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(tokens.glassStrongFill, CircleShape)
                .border(1.dp, tokens.hairlineStrong, CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = tokens.ink,
            )
        }
    }
}
