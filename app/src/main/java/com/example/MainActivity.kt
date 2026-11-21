package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.DemoLocalChatRepository
import com.example.data.FirebaseChatRepository
import com.example.data.PreferencesRepository
import com.example.data.UserAccount
import com.example.data.authStateFlow
import com.example.data.signOutGoogle
import com.example.ui.ChatViewModel
import com.example.ui.UiState
import com.example.ui.cipher.CipherTextDisplay
import com.example.ui.components.ChatWindow
import com.example.ui.components.CipherGuideDialog
import com.example.ui.components.CreateGroupDialog
import com.example.ui.components.GroupInfoDialog
import com.example.ui.components.NewDirectChatDialog
import com.example.ui.components.ProfileDialog
import com.example.ui.components.Sidebar
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.theme.GlyphChatTheme
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.LocalMonochromeTokens
import com.example.ui.theme.liquidGlassChrome
import com.example.ui.theme.monochromeGlassCanvas
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val prefs = remember(context) { PreferencesRepository(context.applicationContext) }
            val isDarkTheme by prefs.darkModeFlow.collectAsStateWithLifecycle(initialValue = false)
            val showKeyHints by prefs.showKeyHintsFlow.collectAsStateWithLifecycle(initialValue = true)
            val scope = rememberCoroutineScope()

            LaunchedEffect(isDarkTheme) {
                val transparent = Color.Transparent.toArgb()
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(transparent)
                    } else {
                        SystemBarStyle.light(transparent, transparent)
                    },
                    navigationBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(transparent)
                    } else {
                        SystemBarStyle.light(transparent, transparent)
                    }
                )
            }

            GlyphChatTheme(darkTheme = isDarkTheme) {
                AppContent(
                    isDarkTheme = isDarkTheme,
                    showKeyHints = showKeyHints,
                    onToggleTheme = {
                        scope.launch { prefs.setDarkMode(!isDarkTheme) }
                    },
                    onToggleKeyHints = {
                        scope.launch { prefs.setShowKeyHints(!showKeyHints) }
                    },
                )
            }
        }
    }
}

@Composable
private fun AppContent(
    isDarkTheme: Boolean,
    showKeyHints: Boolean,
    onToggleTheme: () -> Unit,
    onToggleKeyHints: () -> Unit,
) {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var inDemoMode by rememberSaveable { mutableStateOf(false) }

    val user = currentUser
    when {
        user != null -> {
            // Cloud Authenticated Session — keyed strictly by user.uid
            key(user.uid) {
                val viewModel: ChatViewModel = viewModel(
                    key = "cloud_${user.uid}",
                    factory = viewModelFactory {
                        initializer {
                            val app = checkNotNull(this[APPLICATION_KEY]) {
                                "APPLICATION_KEY missing from CreationExtras"
                            }
                            val dbId = app.getString(R.string.firestore_database_id)
                            val db = if (dbId.isBlank() || dbId == "(default)") {
                                Firebase.firestore
                            } else {
                                FirebaseFirestore.getInstance(dbId)
                            }
                            ChatViewModel(
                                repository = FirebaseChatRepository(db),
                                currentUserId = user.uid,
                                suggestedDisplayName = user.displayName.orEmpty(),
                            )
                        }
                    }
                )
                GlyphChatSession(
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    showKeyHints = showKeyHints,
                    onToggleTheme = onToggleTheme,
                    onToggleKeyHints = onToggleKeyHints,
                    onExitDemoMode = { inDemoMode = false },
                )
            }
        }

        inDemoMode -> {
            // Local Demo Sync Session — strictly local, zero Firestore calls
            key("demo_local_session") {
                val viewModel: ChatViewModel = viewModel(
                    key = "demo_local_session",
                    factory = viewModelFactory {
                        initializer {
                            ChatViewModel(
                                repository = DemoLocalChatRepository(),
                                currentUserId = DemoLocalChatRepository.DEMO_UID,
                                suggestedDisplayName = "Operator Zero",
                            )
                        }
                    }
                )
                GlyphChatSession(
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    showKeyHints = showKeyHints,
                    onToggleTheme = onToggleTheme,
                    onToggleKeyHints = onToggleKeyHints,
                    onExitDemoMode = { inDemoMode = false },
                )
            }
        }

        else -> {
            AuthScreen(
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onAuthSuccess = { inDemoMode = false },
                onEnterDemoMode = { inDemoMode = true },
            )
        }
    }
}

@Composable
private fun GlyphChatSession(
    viewModel: ChatViewModel,
    isDarkTheme: Boolean,
    showKeyHints: Boolean,
    onToggleTheme: () -> Unit,
    onToggleKeyHints: () -> Unit,
    onExitDemoMode: () -> Unit,
) {
    val tokens = LocalMonochromeTokens.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val accountState by viewModel.accountState.collectAsStateWithLifecycle()
    val onboardingBusy by viewModel.onboardingBusy.collectAsStateWithLifecycle()
    val onboardingError by viewModel.onboardingError.collectAsStateWithLifecycle()

    // Lifecycle online/offline presence updates
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, accountState) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.updatePresence(true)
                Lifecycle.Event.ON_STOP -> viewModel.updatePresence(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val performSignOut: () -> Unit = {
        if (viewModel.isDemoMode) {
            onExitDemoMode()
        } else {
            signOutGoogle(
                credentialManager = credentialManager,
                scope = scope,
                onBeforeSignOut = { viewModel.markOfflineBeforeSignOut() },
            )
        }
    }

    when (val state = accountState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .monochromeGlassCanvas(tokens)
                    .safeDrawingPadding(),
                contentAlignment = Alignment.Center
            ) {
                LiquidGlassSurface(shape = RoundedCornerShape(24.dp)) {
                    Column(
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = tokens.ink,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Synchronizing Identity...",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.mutedInk,
                        )
                    }
                }
            }
        }

        is UiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .monochromeGlassCanvas(tokens)
                    .safeDrawingPadding()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                LiquidGlassSurface(shape = RoundedCornerShape(28.dp)) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Sync Error",
                            style = MaterialTheme.typography.titleMedium,
                            color = tokens.ink,
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.mutedInk,
                            textAlign = TextAlign.Center,
                        )
                        Box(
                            modifier = Modifier
                                .liquidGlassChrome(
                                    shape = CircleShape,
                                    fillColor = tokens.ink,
                                    borderColor = tokens.ink,
                                    topHighlightColor = Color.White.copy(alpha = 0.25f),
                                    sheenColor = Color.Transparent,
                                )
                                .clickable(onClick = performSignOut)
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Sign Out",
                                style = MaterialTheme.typography.labelLarge,
                                color = tokens.canvas,
                            )
                        }
                    }
                }
            }
        }

        is UiState.Success -> {
            val account = state.data
            if (account == null || account.handle.isBlank()) {
                OnboardingScreen(
                    suggestedName = viewModel.suggestedDisplayName,
                    isBusy = onboardingBusy,
                    errorMessage = onboardingError,
                    onSubmit = { name, handle, pin ->
                        viewModel.completeOnboarding(name, handle, pin)
                    },
                    onSignOut = performSignOut,
                )
            } else {
                WorkspaceScreen(
                    account = account,
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    showKeyHints = showKeyHints,
                    onToggleTheme = onToggleTheme,
                    onToggleKeyHints = onToggleKeyHints,
                    onSignOut = performSignOut,
                )
            }
        }
    }
}

@Composable
private fun WorkspaceScreen(
    account: UserAccount,
    viewModel: ChatViewModel,
    isDarkTheme: Boolean,
    showKeyHints: Boolean,
    onToggleTheme: () -> Unit,
    onToggleKeyHints: () -> Unit,
    onSignOut: () -> Unit,
) {
    val tokens = LocalMonochromeTokens.current
    val chatsState by viewModel.chatsState.collectAsStateWithLifecycle()
    val selectedChatId by viewModel.selectedChatId.collectAsStateWithLifecycle()
    val selectedChat by viewModel.selectedChat.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val peerPresence by viewModel.activePeerPresence.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val composerText by viewModel.composerText.collectAsStateWithLifecycle()
    val replyTarget by viewModel.replyTarget.collectAsStateWithLifecycle()
    val globalDecodeActive by viewModel.globalDecodeActive.collectAsStateWithLifecycle()
    val decodedMessageIds by viewModel.decodedMessageIds.collectAsStateWithLifecycle()
    val useStandardKeyboard by viewModel.useStandardKeyboard.collectAsStateWithLifecycle()
    val handleLookup by viewModel.handleLookup.collectAsStateWithLifecycle()
    val bannerError by viewModel.bannerError.collectAsStateWithLifecycle()

    var showNewDirectDialog by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showGroupInfoDialog by remember { mutableStateOf(false) }
    var showCipherGuideDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .monochromeGlassCanvas(tokens)
    ) {
        val isSplitPane = maxWidth >= 720.dp

        if (!isSplitPane && selectedChat != null) {
            BackHandler {
                viewModel.clearSelection()
            }
        }

        if (isSplitPane) {
            // Split-Pane Tablet / Desktop Liquid Glass Layout
            Row(modifier = Modifier.fillMaxSize()) {
                Sidebar(
                    account = account,
                    isDemoMode = viewModel.isDemoMode,
                    chatsState = chatsState,
                    selectedChatId = selectedChatId,
                    searchQuery = searchQuery,
                    isDarkTheme = isDarkTheme,
                    globalDecodeActive = globalDecodeActive,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onSelectChat = viewModel::selectChat,
                    onNewDirectClick = {
                        viewModel.clearHandleLookup()
                        showNewDirectDialog = true
                    },
                    onNewGroupClick = {
                        viewModel.clearHandleLookup()
                        showCreateGroupDialog = true
                    },
                    onToggleTheme = onToggleTheme,
                    onOpenCipherGuide = { showCipherGuideDialog = true },
                    onOpenProfile = { showProfileDialog = true },
                    onRefreshSync = viewModel::refreshSync,
                    modifier = Modifier
                        .width(340.dp)
                        .fillMaxHeight()
                )

                VerticalDivider(thickness = 1.dp, color = tokens.hairline)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    val active = selectedChat
                    if (active != null) {
                        ChatWindow(
                            chatRoom = active,
                            currentUserId = account.userId,
                            peerPresence = peerPresence,
                            messagesState = messagesState,
                            composerText = composerText,
                            replyTarget = replyTarget,
                            globalDecodeActive = globalDecodeActive,
                            decodedMessageIds = decodedMessageIds,
                            showKeyHints = showKeyHints,
                            useStandardKeyboard = useStandardKeyboard,
                            showBackButton = false,
                            onBack = viewModel::clearSelection,
                            onToggleGlobalDecode = viewModel::toggleGlobalDecode,
                            onToggleMessageDecode = viewModel::toggleMessageDecode,
                            onOpenGroupInfo = {
                                viewModel.clearHandleLookup()
                                showGroupInfoDialog = true
                            },
                            onReplyMessage = viewModel::setReplyTarget,
                            onClearReply = { viewModel.setReplyTarget(null) },
                            onDeleteMessage = viewModel::deleteMessage,
                            onAppendCipherText = viewModel::appendComposerText,
                            onSetComposerText = viewModel::setComposerText,
                            onBackspace = viewModel::backspaceComposer,
                            onSend = viewModel::sendMessage,
                            onToggleKeyHints = onToggleKeyHints,
                            onSwitchKeyboardMode = viewModel::setUseStandardKeyboard,
                            onMarkRead = viewModel::markActiveChatRead,
                        )
                    } else {
                        EmptySplitPanePlaceholder()
                    }
                }
            }
        } else {
            // Responsive Mobile Single-Pane Messenger Layout
            val active = selectedChat
            if (active == null) {
                Sidebar(
                    account = account,
                    isDemoMode = viewModel.isDemoMode,
                    chatsState = chatsState,
                    selectedChatId = selectedChatId,
                    searchQuery = searchQuery,
                    isDarkTheme = isDarkTheme,
                    globalDecodeActive = globalDecodeActive,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onSelectChat = viewModel::selectChat,
                    onNewDirectClick = {
                        viewModel.clearHandleLookup()
                        showNewDirectDialog = true
                    },
                    onNewGroupClick = {
                        viewModel.clearHandleLookup()
                        showCreateGroupDialog = true
                    },
                    onToggleTheme = onToggleTheme,
                    onOpenCipherGuide = { showCipherGuideDialog = true },
                    onOpenProfile = { showProfileDialog = true },
                    onRefreshSync = viewModel::refreshSync,
                )
            } else {
                ChatWindow(
                    chatRoom = active,
                    currentUserId = account.userId,
                    peerPresence = peerPresence,
                    messagesState = messagesState,
                    composerText = composerText,
                    replyTarget = replyTarget,
                    globalDecodeActive = globalDecodeActive,
                    decodedMessageIds = decodedMessageIds,
                    showKeyHints = showKeyHints,
                    useStandardKeyboard = useStandardKeyboard,
                    showBackButton = true,
                    onBack = viewModel::clearSelection,
                    onToggleGlobalDecode = viewModel::toggleGlobalDecode,
                    onToggleMessageDecode = viewModel::toggleMessageDecode,
                    onOpenGroupInfo = {
                        viewModel.clearHandleLookup()
                        showGroupInfoDialog = true
                    },
                    onReplyMessage = viewModel::setReplyTarget,
                    onClearReply = { viewModel.setReplyTarget(null) },
                    onDeleteMessage = viewModel::deleteMessage,
                    onAppendCipherText = viewModel::appendComposerText,
                    onSetComposerText = viewModel::setComposerText,
                    onBackspace = viewModel::backspaceComposer,
                    onSend = viewModel::sendMessage,
                    onToggleKeyHints = onToggleKeyHints,
                    onSwitchKeyboardMode = viewModel::setUseStandardKeyboard,
                    onMarkRead = viewModel::markActiveChatRead,
                )
            }
        }

        // Floating Glass Toast Banner for Errors
        if (bannerError != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 76.dp)
            ) {
                LiquidGlassSurface(
                    shape = CircleShape,
                    fillColor = tokens.ink,
                    borderColor = tokens.ink,
                    modifier = Modifier.clickable { viewModel.dismissBannerError() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = bannerError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.canvas,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Text(
                            text = "Dismiss",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = tokens.canvas,
                        )
                    }
                }
            }
        }
    }

    // --- MODALS ---
    if (showNewDirectDialog) {
        NewDirectChatDialog(
            lookupState = handleLookup,
            demoDirectory = viewModel.demoDirectory,
            onLookupHandle = viewModel::lookupHandle,
            onSelectPeer = { peer ->
                viewModel.startDirectChat(peer) {
                    showNewDirectDialog = false
                }
            },
            onDismiss = { showNewDirectDialog = false }
        )
    }

    if (showCreateGroupDialog) {
        CreateGroupDialog(
            lookupState = handleLookup,
            demoDirectory = viewModel.demoDirectory,
            onLookupHandle = viewModel::lookupHandle,
            onCreateGroup = { name, members ->
                viewModel.createGroupChat(name, members) {
                    showCreateGroupDialog = false
                }
            },
            onDismiss = { showCreateGroupDialog = false }
        )
    }

    val activeForGroupInfo = selectedChat
    if (showGroupInfoDialog && activeForGroupInfo != null) {
        GroupInfoDialog(
            chatRoom = activeForGroupInfo,
            currentUserId = account.userId,
            lookupState = handleLookup,
            onLookupHandle = viewModel::lookupHandle,
            onRenameGroup = { newName ->
                viewModel.renameGroup(activeForGroupInfo, newName) {}
            },
            onAddMember = { entry ->
                viewModel.addMemberToGroup(activeForGroupInfo, entry)
            },
            onRemoveMember = { memberUid ->
                viewModel.removeMemberFromGroup(activeForGroupInfo, memberUid) {
                    showGroupInfoDialog = false
                }
            },
            onDismiss = { showGroupInfoDialog = false }
        )
    }

    if (showCipherGuideDialog) {
        CipherGuideDialog(onDismiss = { showCipherGuideDialog = false })
    }

    if (showProfileDialog) {
        ProfileDialog(
            account = account,
            isDemoMode = viewModel.isDemoMode,
            onSignOutOrExitDemo = {
                showProfileDialog = false
                onSignOut()
            },
            onRefreshSync = viewModel::refreshSync,
            onDismiss = { showProfileDialog = false }
        )
    }
}

@Composable
private fun EmptySplitPanePlaceholder() {
    val tokens = LocalMonochromeTokens.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .monochromeGlassCanvas(tokens)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassSurface(shape = RoundedCornerShape(28.dp)) {
            Column(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CipherTextDisplay(
                    text = "GLYPHCHAT",
                    glyphSize = 26.dp,
                    color = tokens.ink,
                )
                Text(
                    text = stringResource(R.string.empty_chat_prompt),
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.mutedInk,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
