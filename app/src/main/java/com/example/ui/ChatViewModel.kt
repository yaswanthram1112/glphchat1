package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatMessage
import com.example.data.ChatRepository
import com.example.data.ChatRoom
import com.example.data.ChatType
import com.example.data.HandleDirectoryEntry
import com.example.data.UserAccount
import com.example.data.encodeMemberLabel
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class HandleLookupState(
    val isLoading: Boolean = false,
    val result: HandleDirectoryEntry? = null,
    val error: String? = null,
    val notFound: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    private val repository: ChatRepository,
    val currentUserId: String,
    val suggestedDisplayName: String = "",
) : ViewModel() {

    val isDemoMode: Boolean = repository.isDemoMode
    val demoDirectory: List<HandleDirectoryEntry> = repository.getDemoDirectory()

    init {
        viewModelScope.launch {
            while (true) {
                delay(45_000L)
                val accState = accountState.value
                if (accState is UiState.Success && accState.data != null) {
                    val user = accState.data
                    if (user.handle.isNotBlank()) {
                        repository.setOnlineStatus(currentUserId, user.handle, true)
                    }
                }
            }
        }
    }

    // 1. User Account Stream (null => needs onboarding setup)
    val accountState: StateFlow<UiState<UserAccount?>> = repository.observeUserAccount(currentUserId)
        .map<UserAccount?, UiState<UserAccount?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing account", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load user profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    // 2. Chat List Stream
    val chatsState: StateFlow<UiState<List<ChatRoom>>> = repository.observeChats(currentUserId)
        .map<List<ChatRoom>, UiState<List<ChatRoom>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing chats", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load conversations"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _selectedChatId = MutableStateFlow<String?>(null)
    val selectedChatId: StateFlow<String?> = _selectedChatId.asStateFlow()

    val selectedChat: StateFlow<ChatRoom?> = combine(chatsState, _selectedChatId) { state, id ->
        if (state is UiState.Success && id != null) {
            state.data.firstOrNull { it.id == id }
        } else {
            null
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = null
    )

    // 3. Real-Time Messages Stream for active chat
    val messagesState: StateFlow<UiState<List<ChatMessage>>> = _selectedChatId
        .flatMapLatest { chatId ->
            if (chatId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeMessages(chatId, currentUserId)
                    .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        Log.w(TAG, "Error observing messages", rootError)
                        emit(UiState.Error(rootError.message ?: "Failed to load messages"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList())
        )

    // 4. Live Peer Online/Offline Presence for active Direct Chat
    val activePeerPresence: StateFlow<HandleDirectoryEntry?> = selectedChat
        .flatMapLatest { chat ->
            val peerHandle = if (chat != null && chat.resolvedType() == ChatType.DIRECT) {
                chat.directPeerInfo(currentUserId)?.handle
            } else {
                null
            }
            if (peerHandle.isNullOrBlank()) {
                flowOf(null)
            } else {
                repository.observeHandlePresence(peerHandle)
                    .catch { error ->
                        if (error is CancellationException) throw error
                        emit(null)
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    // UI Interaction States
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _composerText = MutableStateFlow("")
    val composerText: StateFlow<String> = _composerText.asStateFlow()

    private val _replyTarget = MutableStateFlow<ChatMessage?>(null)
    val replyTarget: StateFlow<ChatMessage?> = _replyTarget.asStateFlow()

    private val _globalDecodeActive = MutableStateFlow(false)
    val globalDecodeActive: StateFlow<Boolean> = _globalDecodeActive.asStateFlow()

    private val _decodedMessageIds = MutableStateFlow<Set<String>>(emptySet())
    val decodedMessageIds: StateFlow<Set<String>> = _decodedMessageIds.asStateFlow()

    private val _useStandardKeyboard = MutableStateFlow(false)
    val useStandardKeyboard: StateFlow<Boolean> = _useStandardKeyboard.asStateFlow()

    private val _onboardingBusy = MutableStateFlow(false)
    val onboardingBusy: StateFlow<Boolean> = _onboardingBusy.asStateFlow()

    private val _onboardingError = MutableStateFlow<String?>(null)
    val onboardingError: StateFlow<String?> = _onboardingError.asStateFlow()

    private val _handleLookup = MutableStateFlow(HandleLookupState())
    val handleLookup: StateFlow<HandleLookupState> = _handleLookup.asStateFlow()

    private val _bannerError = MutableStateFlow<String?>(null)
    val bannerError: StateFlow<String?> = _bannerError.asStateFlow()

    fun dismissBannerError() {
        _bannerError.value = null
    }

    fun refreshSync() {
        viewModelScope.launch {
            try {
                val accState = accountState.value
                if (accState is UiState.Success && accState.data != null) {
                    val user = accState.data
                    repository.setOnlineStatus(user.userId, user.handle, true)
                }
                _bannerError.value = "Over-The-Air Sync Complete: Account & real-time streams updated."
            } catch (e: Exception) {
                _bannerError.value = "Sync Status: ${e.localizedMessage}"
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectChat(chatId: String) {
        _selectedChatId.value = chatId
        _replyTarget.value = null
        _decodedMessageIds.value = emptySet()
    }

    fun clearSelection() {
        _selectedChatId.value = null
        _replyTarget.value = null
    }

    fun appendComposerText(text: String) {
        if (_composerText.value.length + text.length <= 2000) {
            _composerText.value += text
        }
    }

    fun setComposerText(text: String) {
        _composerText.value = text.take(2000)
    }

    fun backspaceComposer() {
        val current = _composerText.value
        if (current.isNotEmpty()) {
            _composerText.value = current.dropLast(1)
        }
    }

    fun setReplyTarget(message: ChatMessage?) {
        _replyTarget.value = message
    }

    fun toggleGlobalDecode() {
        _globalDecodeActive.value = !_globalDecodeActive.value
    }

    fun toggleMessageDecode(messageId: String) {
        val current = _decodedMessageIds.value
        _decodedMessageIds.value = if (current.contains(messageId)) {
            current - messageId
        } else {
            current + messageId
        }
    }

    fun setUseStandardKeyboard(useStandard: Boolean) {
        _useStandardKeyboard.value = useStandard
    }

    fun completeOnboarding(displayName: String, handle: String, pin: String) {
        viewModelScope.launch {
            _onboardingBusy.value = true
            _onboardingError.value = null
            val result = repository.setupUserProfile(displayName, handle, pin)
            _onboardingBusy.value = false
            result.onFailure { err ->
                _onboardingError.value = err.message ?: "Unable to save profile."
            }
        }
    }

    fun updatePresence(online: Boolean) {
        val account = (accountState.value as? UiState.Success)?.data ?: return
        if (account.handle.isBlank()) return
        viewModelScope.launch {
            repository.setOnlineStatus(currentUserId, account.handle, online)
        }
    }

    fun markOfflineBeforeSignOut() {
        val account = (accountState.value as? UiState.Success)?.data ?: return
        if (account.handle.isNotBlank()) {
            viewModelScope.launch {
                try {
                    repository.setOnlineStatus(currentUserId, account.handle, false)
                } catch (e: Exception) {
                    Log.w(TAG, "Sign-out presence update warning", e)
                }
            }
        }
    }

    fun lookupHandle(rawHandle: String) {
        viewModelScope.launch {
            _handleLookup.value = HandleLookupState(isLoading = true)
            val result = repository.lookupHandle(rawHandle)
            result.onSuccess { entry ->
                _handleLookup.value = HandleLookupState(
                    isLoading = false,
                    result = entry,
                    notFound = entry == null,
                )
            }.onFailure { err ->
                _handleLookup.value = HandleLookupState(
                    isLoading = false,
                    error = err.message ?: "Lookup failed"
                )
            }
        }
    }

    fun clearHandleLookup() {
        _handleLookup.value = HandleLookupState()
    }

    fun startDirectChat(peer: HandleDirectoryEntry, onOpened: () -> Unit) {
        val account = (accountState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val result = repository.createOrOpenDirectChat(account, peer)
            result.onSuccess { chatId ->
                _selectedChatId.value = chatId
                _handleLookup.value = HandleLookupState()
                onOpened()
            }.onFailure { err ->
                _bannerError.value = err.message ?: "Failed to create chat"
            }
        }
    }

    fun createGroupChat(
        groupName: String,
        members: List<HandleDirectoryEntry>,
        onCreated: () -> Unit
    ) {
        val account = (accountState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val result = repository.createGroupChat(account, groupName, members)
            result.onSuccess { chatId ->
                _selectedChatId.value = chatId
                _handleLookup.value = HandleLookupState()
                onCreated()
            }.onFailure { err ->
                _bannerError.value = err.message ?: "Failed to create group"
            }
        }
    }

    fun renameGroup(chatRoom: ChatRoom, newName: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val result = repository.updateGroupRoom(
                chatRoom = chatRoom,
                newName = newName,
                newMemberUids = chatRoom.memberUids,
                newMemberLabels = chatRoom.memberLabels,
            )
            result.onSuccess { onDone() }
                .onFailure { err -> _bannerError.value = err.message ?: "Failed to rename group" }
        }
    }

    fun addMemberToGroup(chatRoom: ChatRoom, entry: HandleDirectoryEntry) {
        if (chatRoom.memberUids.contains(entry.userId)) return
        viewModelScope.launch {
            val updatedUids = (chatRoom.memberUids + entry.userId).distinct()
            val updatedLabels = chatRoom.memberLabels + (entry.userId to encodeMemberLabel(entry.handle, entry.displayName))
            val result = repository.updateGroupRoom(
                chatRoom = chatRoom,
                newName = chatRoom.name,
                newMemberUids = updatedUids,
                newMemberLabels = updatedLabels,
            )
            result.onFailure { err -> _bannerError.value = err.message ?: "Failed to add member" }
        }
    }

    fun removeMemberFromGroup(chatRoom: ChatRoom, memberUid: String, onLeftGroup: () -> Unit = {}) {
        viewModelScope.launch {
            val updatedUids = chatRoom.memberUids.filter { it != memberUid }
            val updatedLabels = chatRoom.memberLabels.filterKeys { it != memberUid }
            if (updatedUids.isEmpty()) return@launch
            val result = repository.updateGroupRoom(
                chatRoom = chatRoom,
                newName = chatRoom.name,
                newMemberUids = updatedUids,
                newMemberLabels = updatedLabels,
            )
            result.onSuccess {
                if (memberUid == currentUserId) {
                    _selectedChatId.value = null
                    onLeftGroup()
                }
            }.onFailure { err ->
                _bannerError.value = err.message ?: "Failed to update group members"
            }
        }
    }

    fun sendMessage() {
        val chat = selectedChat.value ?: return
        val account = (accountState.value as? UiState.Success)?.data ?: return
        val text = _composerText.value.trim()
        if (text.isEmpty()) return
        val reply = _replyTarget.value

        _composerText.value = ""
        _replyTarget.value = null

        viewModelScope.launch {
            val result = repository.sendMessage(chat, account, text, reply)
            result.onFailure { err ->
                _bannerError.value = err.message ?: "Failed to send message"
            }
        }
    }

    fun markActiveChatRead() {
        val chat = selectedChat.value ?: return
        val msgs = (messagesState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.markChatAndMessagesRead(chat, msgs, currentUserId)
        }
    }

    fun deleteMessage(message: ChatMessage) {
        val chat = selectedChat.value ?: return
        if (message.senderId != currentUserId || message.deleted) return
        viewModelScope.launch {
            val result = repository.deleteMessage(chat.id, message.id)
            result.onFailure { err ->
                _bannerError.value = err.message ?: "Failed to delete message"
            }
        }
    }

    private companion object {
        const val TAG = "ChatViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
