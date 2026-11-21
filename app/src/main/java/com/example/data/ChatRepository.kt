package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

interface ChatRepository {
    val isDemoMode: Boolean

    fun observeUserAccount(userId: String): Flow<UserAccount?>
    suspend fun setupUserProfile(displayName: String, rawHandle: String, pin: String): Result<UserAccount>
    suspend fun setOnlineStatus(userId: String, handle: String, online: Boolean): Result<Unit>

    suspend fun lookupHandle(rawHandle: String): Result<HandleDirectoryEntry?>
    fun observeHandlePresence(rawHandle: String): Flow<HandleDirectoryEntry?>
    fun getDemoDirectory(): List<HandleDirectoryEntry> = emptyList()

    fun observeChats(userId: String): Flow<List<ChatRoom>>
    fun observeMessages(chatId: String, userId: String): Flow<List<ChatMessage>>

    suspend fun createOrOpenDirectChat(
        currentAccount: UserAccount,
        peer: HandleDirectoryEntry
    ): Result<String>

    suspend fun createGroupChat(
        currentAccount: UserAccount,
        groupName: String,
        members: List<HandleDirectoryEntry>
    ): Result<String>

    suspend fun updateGroupRoom(
        chatRoom: ChatRoom,
        newName: String,
        newMemberUids: List<String>,
        newMemberLabels: Map<String, String>
    ): Result<Unit>

    suspend fun sendMessage(
        chatRoom: ChatRoom,
        sender: UserAccount,
        text: String,
        replyTo: ChatMessage? = null
    ): Result<Unit>

    suspend fun markChatAndMessagesRead(
        chatRoom: ChatRoom,
        messages: List<ChatMessage>,
        userId: String
    ): Result<Unit>

    suspend fun deleteMessage(chatId: String, messageId: String): Result<Unit>
}

class FirebaseChatRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) : ChatRepository {

    constructor(context: Context) : this(
        run {
            val dbId = context.applicationContext.getString(R.string.firestore_database_id)
            if (dbId.isBlank() || dbId == "(default)") {
                Firebase.firestore
            } else {
                FirebaseFirestore.getInstance(dbId)
            }
        }
    )

    override val isDemoMode: Boolean = false

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    override fun observeUserAccount(userId: String): Flow<UserAccount?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user account observation." }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserAccount::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    override suspend fun setupUserProfile(
        displayName: String,
        rawHandle: String,
        pin: String
    ): Result<UserAccount> {
        val cleanName = displayName.trim().take(40)
        val cleanHandle = normalizeHandleInput(rawHandle)
        val cleanPin = pin.filter { it.isDigit() }.take(4)

        if (cleanName.length < 2) {
            return Result.failure(IllegalArgumentException("Display name must be at least 2 characters."))
        }
        if (cleanHandle.length < 3) {
            return Result.failure(IllegalArgumentException("Handle must be 3–24 lowercase letters, digits, or underscores."))
        }
        if (cleanPin.length != 4) {
            return Result.failure(IllegalArgumentException("PIN must be exactly 4 digits."))
        }

        var path = "handles/$cleanHandle"
        return try {
            val uid = requireUserId()
            val handleRef = db.collection("handles").document(cleanHandle)
            val existingHandle = handleRef.get().await()
            if (existingHandle.exists() && existingHandle.getString("userId") != uid) {
                return Result.failure(IllegalStateException("@$cleanHandle is already taken. Choose another handle."))
            }

            val userRef = db.collection("users").document(uid)
            val existingUser = userRef.get().await()

            val batch = db.batch()
            if (existingHandle.exists()) {
                batch.update(
                    handleRef,
                    mapOf(
                        "displayName" to cleanName,
                        "pin" to cleanPin,
                        "online" to true,
                        "lastSeenAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            } else {
                batch.set(
                    handleRef,
                    mapOf(
                        "userId" to uid,
                        "handle" to cleanHandle,
                        "displayName" to cleanName,
                        "pin" to cleanPin,
                        "online" to true,
                        "lastSeenAt" to FieldValue.serverTimestamp(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            }

            path = "users/$uid"
            if (existingUser.exists()) {
                batch.update(
                    userRef,
                    mapOf(
                        "displayName" to cleanName,
                        "handle" to cleanHandle,
                        "pin" to cleanPin,
                        "online" to true,
                        "lastSeenAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            } else {
                batch.set(
                    userRef,
                    mapOf(
                        "userId" to uid,
                        "displayName" to cleanName,
                        "handle" to cleanHandle,
                        "pin" to cleanPin,
                        "online" to true,
                        "lastSeenAt" to FieldValue.serverTimestamp(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            }
            batch.commit().await()

            Result.success(
                UserAccount(
                    id = uid,
                    userId = uid,
                    displayName = cleanName,
                    handle = cleanHandle,
                    pin = cleanPin,
                    online = true,
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun setOnlineStatus(
        userId: String,
        handle: String,
        online: Boolean
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            if (uid != userId || handle.isBlank()) return Result.success(Unit)
            val presenceUpdate = mapOf(
                "online" to online,
                "lastSeenAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            val batch = db.batch()
            batch.update(db.collection("users").document(uid), presenceUpdate)
            batch.update(db.collection("handles").document(normalizeHandleInput(handle)), presenceUpdate)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun lookupHandle(rawHandle: String): Result<HandleDirectoryEntry?> {
        val clean = normalizeHandleInput(rawHandle)
        if (clean.length < 3) {
            return Result.failure(IllegalArgumentException("Enter at least 3 characters for a handle."))
        }
        val path = "handles/$clean"
        return try {
            requireUserId()
            val snap = db.collection("handles").document(clean).get().await()
            val entry = if (snap.exists()) {
                snap.toObject(
                    HandleDirectoryEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else {
                null
            }
            Result.success(entry)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    override fun observeHandlePresence(rawHandle: String): Flow<HandleDirectoryEntry?> = flow {
        val clean = normalizeHandleInput(rawHandle)
        if (clean.length < 3) {
            emit(null)
            return@flow
        }
        val uid = requireUserId()
        val path = "handles/$clean"
        emitAll(
            db.collection("handles").document(clean)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(
                            HandleDirectoryEntry::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    emit(null)
                }
        )
    }

    override fun observeChats(userId: String): Flow<List<ChatRoom>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user chat query." }
        val path = "chats"
        emitAll(
            db.collection("chats")
                .whereArrayContains("memberUids", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatRoom::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.lastMessageAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    override fun observeMessages(chatId: String, userId: String): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user message query." }
        val path = "chats/$chatId/messages"
        emitAll(
            db.collection("chats").document(chatId).collection("messages")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    override suspend fun createOrOpenDirectChat(
        currentAccount: UserAccount,
        peer: HandleDirectoryEntry
    ): Result<String> {
        var path = "chats"
        return try {
            val uid = requireUserId()
            val sorted = listOf(uid, peer.userId).distinct().sorted()
            val chatId = if (sorted.size == 1) {
                "dm_self_${sorted[0]}"
            } else {
                "dm_${sorted[0]}_${sorted[1]}"
            }.take(128)
            path = "chats/$chatId"
            val docRef = db.collection("chats").document(chatId)
            val existing = docRef.get().await()
            if (existing.exists()) {
                return Result.success(chatId)
            }

            val labels = linkedMapOf(
                uid to encodeMemberLabel(currentAccount.handle, currentAccount.displayName),
                peer.userId to encodeMemberLabel(peer.handle, peer.displayName),
            )
            val payload = mapOf(
                "chatType" to ChatType.DIRECT.name,
                "name" to peer.displayName.take(60).ifBlank { peer.handle },
                "creatorId" to uid,
                "memberUids" to sorted,
                "memberLabels" to labels,
                "lastMessageText" to "",
                "lastMessageSenderId" to "",
                "lastMessageAt" to FieldValue.serverTimestamp(),
                "readByUids" to listOf(uid),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            Result.success(chatId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun createGroupChat(
        currentAccount: UserAccount,
        groupName: String,
        members: List<HandleDirectoryEntry>
    ): Result<String> {
        val cleanName = groupName.trim().take(60)
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Group name cannot be empty."))
        }
        var path = "chats"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("chats").document("grp_${UUID.randomUUID().toString().replace("-", "").take(20)}")
            path = docRef.path
            val allMembers = (listOf(uid) + members.map { it.userId }).distinct().take(50)
            val labels = linkedMapOf<String, String>()
            labels[uid] = encodeMemberLabel(currentAccount.handle, currentAccount.displayName)
            members.forEach { m ->
                labels[m.userId] = encodeMemberLabel(m.handle, m.displayName)
            }
            val payload = mapOf(
                "chatType" to ChatType.GROUP.name,
                "name" to cleanName,
                "creatorId" to uid,
                "memberUids" to allMembers,
                "memberLabels" to labels,
                "lastMessageText" to "",
                "lastMessageSenderId" to "",
                "lastMessageAt" to FieldValue.serverTimestamp(),
                "readByUids" to listOf(uid),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun updateGroupRoom(
        chatRoom: ChatRoom,
        newName: String,
        newMemberUids: List<String>,
        newMemberLabels: Map<String, String>
    ): Result<Unit> {
        val cleanName = newName.trim().take(60)
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Group name cannot be empty."))
        }
        val path = "chats/${chatRoom.id}"
        return try {
            requireUserId()
            db.collection("chats").document(chatRoom.id).update(
                mapOf(
                    "name" to cleanName,
                    "memberUids" to newMemberUids.distinct().take(50),
                    "memberLabels" to newMemberLabels,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun sendMessage(
        chatRoom: ChatRoom,
        sender: UserAccount,
        text: String,
        replyTo: ChatMessage?
    ): Result<Unit> {
        val cleanText = text.trim().take(2000)
        if (cleanText.isEmpty()) return Result.success(Unit)
        val path = "chats/${chatRoom.id}/messages"
        return try {
            val uid = requireUserId()
            val msgRef = db.collection("chats").document(chatRoom.id).collection("messages").document()
            val rawMap = linkedMapOf<String, Any?>(
                "chatId" to chatRoom.id,
                "senderId" to uid,
                "senderName" to sender.displayName.take(40).ifBlank { sender.handle },
                "senderHandle" to sender.handle.take(30).ifBlank { "user" },
                "text" to cleanText,
                "memberUids" to chatRoom.memberUids,
                "readByUids" to listOf(uid),
                "deleted" to false,
                "replyToMessageId" to replyTo?.id?.takeIf { it.isNotBlank() }?.take(128),
                "replyToText" to replyTo?.text?.takeIf { it.isNotBlank() }?.take(200),
                "replyToSenderName" to replyTo?.senderName?.takeIf { it.isNotBlank() }?.take(40),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            @Suppress("UNCHECKED_CAST")
            val payload = rawMap.filterValues { it != null } as Map<String, Any>

            val batch = db.batch()
            batch.set(msgRef, payload)
            batch.update(
                db.collection("chats").document(chatRoom.id),
                mapOf(
                    "lastMessageText" to cleanText.take(1000),
                    "lastMessageSenderId" to uid,
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "readByUids" to listOf(uid),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun markChatAndMessagesRead(
        chatRoom: ChatRoom,
        messages: List<ChatMessage>,
        userId: String
    ): Result<Unit> {
        val path = "chats/${chatRoom.id}"
        return try {
            val uid = requireUserId()
            if (uid != userId) return Result.success(Unit)
            val unreadMessages = messages.filter { !it.readByUids.contains(uid) }
            val needsChatRead = !chatRoom.readByUids.contains(uid)
            if (unreadMessages.isEmpty() && !needsChatRead) return Result.success(Unit)

            val batch = db.batch()
            if (needsChatRead) {
                batch.update(
                    db.collection("chats").document(chatRoom.id),
                    mapOf(
                        "readByUids" to (chatRoom.readByUids + uid).distinct().take(50),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            }
            unreadMessages.take(25).forEach { msg ->
                batch.update(
                    db.collection("chats").document(chatRoom.id).collection("messages").document(msg.id),
                    mapOf(
                        "readByUids" to (msg.readByUids + uid).distinct().take(50),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun deleteMessage(chatId: String, messageId: String): Result<Unit> {
        val path = "chats/$chatId/messages/$messageId"
        return try {
            requireUserId()
            db.collection("chats").document(chatId)
                .collection("messages").document(messageId)
                .update(
                    mapOf(
                        "deleted" to true,
                        "text" to "[MESSAGE DELETED]",
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}

/**
 * Immediate local sync repository for zero-friction testing without needing a second device.
 * Strictly local in-memory state — never calls Firestore or Anonymous Auth.
 */
class DemoLocalChatRepository : ChatRepository {
    override val isDemoMode: Boolean = true

    companion object {
        const val DEMO_UID = "demo_local_user"
    }

    private val simScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val elena = HandleDirectoryEntry(
        id = "elena_s",
        userId = "peer_elena",
        handle = "elena_s",
        displayName = "Elena Solis",
        pin = "7402",
        online = true,
        lastSeenAt = Timestamp.now(),
    )
    private val marcus = HandleDirectoryEntry(
        id = "marcus_k",
        userId = "peer_marcus",
        handle = "marcus_k",
        displayName = "Marcus Klein",
        pin = "3190",
        online = true,
        lastSeenAt = Timestamp.now(),
    )
    private val nova = HandleDirectoryEntry(
        id = "nova_ops",
        userId = "peer_nova",
        handle = "nova_ops",
        displayName = "Nova Desk",
        pin = "8821",
        online = false,
        lastSeenAt = Timestamp(Date(System.currentTimeMillis() - 18 * 60_000L)),
    )

    private val directoryFlow = MutableStateFlow(
        linkedMapOf(
            elena.handle to elena,
            marcus.handle to marcus,
            nova.handle to nova,
        )
    )

    private val accountFlow = MutableStateFlow<UserAccount?>(
        UserAccount(
            id = DEMO_UID,
            userId = DEMO_UID,
            displayName = "Operator Zero",
            handle = "zero_one",
            pin = "4829",
            online = true,
            lastSeenAt = Timestamp.now(),
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now(),
        )
    )

    private val chatsFlow: MutableStateFlow<List<ChatRoom>>
    private val messagesFlow: MutableStateFlow<Map<String, List<ChatMessage>>>

    private val autoReplies = listOf(
        "COPY THAT. CIPHER STREAM VERIFIED.",
        "RECEIVED ON SWISS GRID CHANNEL.",
        "HOLD THE EYE ICON TO DECODE ANY BUBBLE.",
        "STANDING BY FOR NEXT COORDINATES.",
        "SIGNAL LOCKED. ZERO NOISE.",
    )
    private var replyIndex = 0

    init {
        val nowMs = System.currentTimeMillis()
        val yesterday = Timestamp(Date(nowMs - 26 * 3600_000L))
        val tenMinAgo = Timestamp(Date(nowMs - 10 * 60_000L))
        val fourMinAgo = Timestamp(Date(nowMs - 4 * 60_000L))
        val twoMinAgo = Timestamp(Date(nowMs - 2 * 60_000L))

        val dmId = "dm_demo_elena"
        val grpId = "grp_swiss_grid"

        val dmRoom = ChatRoom(
            id = dmId,
            chatType = ChatType.DIRECT.name,
            name = elena.displayName,
            creatorId = DEMO_UID,
            memberUids = listOf(DEMO_UID, elena.userId),
            memberLabels = mapOf(
                DEMO_UID to encodeMemberLabel("zero_one", "Operator Zero"),
                elena.userId to encodeMemberLabel(elena.handle, elena.displayName),
            ),
            lastMessageText = "TAP THE EYE ICON TO DECODE THIS SCRIPT",
            lastMessageSenderId = elena.userId,
            lastMessageAt = twoMinAgo,
            readByUids = listOf(elena.userId, DEMO_UID),
            createdAt = yesterday,
            updatedAt = twoMinAgo,
        )

        val grpRoom = ChatRoom(
            id = grpId,
            chatType = ChatType.GROUP.name,
            name = "SWISS GRID OPS",
            creatorId = DEMO_UID,
            memberUids = listOf(DEMO_UID, elena.userId, marcus.userId),
            memberLabels = mapOf(
                DEMO_UID to encodeMemberLabel("zero_one", "Operator Zero"),
                elena.userId to encodeMemberLabel(elena.handle, elena.displayName),
                marcus.userId to encodeMemberLabel(marcus.handle, marcus.displayName),
            ),
            lastMessageText = "ALL NODES SYNCHRONIZED AT 09:00",
            lastMessageSenderId = marcus.userId,
            lastMessageAt = fourMinAgo,
            readByUids = listOf(marcus.userId, elena.userId, DEMO_UID),
            createdAt = yesterday,
            updatedAt = fourMinAgo,
        )

        chatsFlow = MutableStateFlow(listOf(dmRoom, grpRoom))

        val dmMessages = listOf(
            ChatMessage(
                id = "m1",
                chatId = dmId,
                senderId = elena.userId,
                senderName = elena.displayName,
                senderHandle = elena.handle,
                text = "WELCOME TO GLYPHCHAT",
                memberUids = dmRoom.memberUids,
                readByUids = listOf(elena.userId, DEMO_UID),
                deleted = false,
                createdAt = yesterday,
                updatedAt = yesterday,
            ),
            ChatMessage(
                id = "m2",
                chatId = dmId,
                senderId = DEMO_UID,
                senderName = "Operator Zero",
                senderHandle = "zero_one",
                text = "THE QUICK BROWN FOX JUMPS OVER LAZY DOG",
                memberUids = dmRoom.memberUids,
                readByUids = listOf(DEMO_UID, elena.userId),
                deleted = false,
                replyToMessageId = "m1",
                replyToText = "WELCOME TO GLYPHCHAT",
                replyToSenderName = elena.displayName,
                createdAt = tenMinAgo,
                updatedAt = tenMinAgo,
            ),
            ChatMessage(
                id = "m3",
                chatId = dmId,
                senderId = elena.userId,
                senderName = elena.displayName,
                senderHandle = elena.handle,
                text = "TAP THE EYE ICON TO DECODE THIS SCRIPT",
                memberUids = dmRoom.memberUids,
                readByUids = listOf(elena.userId, DEMO_UID),
                deleted = false,
                createdAt = twoMinAgo,
                updatedAt = twoMinAgo,
            ),
        )

        val grpMessages = listOf(
            ChatMessage(
                id = "g1",
                chatId = grpId,
                senderId = elena.userId,
                senderName = elena.displayName,
                senderHandle = elena.handle,
                text = "PROTOCOL V2 IS LIVE",
                memberUids = grpRoom.memberUids,
                readByUids = listOf(elena.userId, marcus.userId, DEMO_UID),
                deleted = false,
                createdAt = yesterday,
                updatedAt = yesterday,
            ),
            ChatMessage(
                id = "g2",
                chatId = grpId,
                senderId = marcus.userId,
                senderName = marcus.displayName,
                senderHandle = marcus.handle,
                text = "ALL NODES SYNCHRONIZED AT 09:00",
                memberUids = grpRoom.memberUids,
                readByUids = listOf(marcus.userId, elena.userId, DEMO_UID),
                deleted = false,
                createdAt = fourMinAgo,
                updatedAt = fourMinAgo,
            ),
        )

        messagesFlow = MutableStateFlow(
            mapOf(
                dmId to dmMessages,
                grpId to grpMessages,
            )
        )
    }

    override fun getDemoDirectory(): List<HandleDirectoryEntry> =
        directoryFlow.value.values.toList()

    override fun observeUserAccount(userId: String): Flow<UserAccount?> = accountFlow

    override suspend fun setupUserProfile(
        displayName: String,
        rawHandle: String,
        pin: String
    ): Result<UserAccount> {
        val cleanName = displayName.trim().take(40)
        val cleanHandle = normalizeHandleInput(rawHandle)
        val cleanPin = pin.filter { it.isDigit() }.take(4)
        if (cleanName.length < 2) {
            return Result.failure(IllegalArgumentException("Display name must be at least 2 characters."))
        }
        if (cleanHandle.length < 3) {
            return Result.failure(IllegalArgumentException("Handle must be 3–24 lowercase letters, digits, or underscores."))
        }
        if (cleanPin.length != 4) {
            return Result.failure(IllegalArgumentException("PIN must be 4 digits."))
        }
        val updated = UserAccount(
            id = DEMO_UID,
            userId = DEMO_UID,
            displayName = cleanName,
            handle = cleanHandle,
            pin = cleanPin,
            online = true,
            lastSeenAt = Timestamp.now(),
            createdAt = accountFlow.value?.createdAt ?: Timestamp.now(),
            updatedAt = Timestamp.now(),
        )
        accountFlow.value = updated
        return Result.success(updated)
    }

    override suspend fun setOnlineStatus(userId: String, handle: String, online: Boolean): Result<Unit> {
        accountFlow.value = accountFlow.value?.copy(online = online, lastSeenAt = Timestamp.now())
        return Result.success(Unit)
    }

    override suspend fun lookupHandle(rawHandle: String): Result<HandleDirectoryEntry?> {
        val clean = normalizeHandleInput(rawHandle)
        if (clean.length < 3) {
            return Result.failure(IllegalArgumentException("Enter at least 3 characters for a handle."))
        }
        return Result.success(directoryFlow.value[clean])
    }

    override fun observeHandlePresence(rawHandle: String): Flow<HandleDirectoryEntry?> {
        val clean = normalizeHandleInput(rawHandle)
        return directoryFlow.map { it[clean] }
    }

    override fun observeChats(userId: String): Flow<List<ChatRoom>> =
        chatsFlow.map { list ->
            list.filter { it.memberUids.contains(userId) }
                .sortedByDescending { it.lastMessageAt ?: it.createdAt ?: Timestamp(0, 0) }
        }

    override fun observeMessages(chatId: String, userId: String): Flow<List<ChatMessage>> =
        messagesFlow.map { map ->
            (map[chatId] ?: emptyList()).sortedBy { it.createdAt ?: Timestamp(0, 0) }
        }

    override suspend fun createOrOpenDirectChat(
        currentAccount: UserAccount,
        peer: HandleDirectoryEntry
    ): Result<String> {
        val existing = chatsFlow.value.firstOrNull {
            it.resolvedType() == ChatType.DIRECT &&
                it.memberUids.contains(peer.userId) &&
                it.memberUids.contains(currentAccount.userId)
        }
        if (existing != null) return Result.success(existing.id)

        val newId = "dm_${peer.handle}_${UUID.randomUUID().toString().take(6)}"
        val now = Timestamp.now()
        val room = ChatRoom(
            id = newId,
            chatType = ChatType.DIRECT.name,
            name = peer.displayName,
            creatorId = currentAccount.userId,
            memberUids = listOf(currentAccount.userId, peer.userId),
            memberLabels = mapOf(
                currentAccount.userId to encodeMemberLabel(currentAccount.handle, currentAccount.displayName),
                peer.userId to encodeMemberLabel(peer.handle, peer.displayName),
            ),
            lastMessageText = "",
            lastMessageSenderId = "",
            lastMessageAt = now,
            readByUids = listOf(currentAccount.userId),
            createdAt = now,
            updatedAt = now,
        )
        chatsFlow.value = listOf(room) + chatsFlow.value
        return Result.success(newId)
    }

    override suspend fun createGroupChat(
        currentAccount: UserAccount,
        groupName: String,
        members: List<HandleDirectoryEntry>
    ): Result<String> {
        val cleanName = groupName.trim().take(60)
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Group name cannot be empty."))
        }
        val newId = "grp_${UUID.randomUUID().toString().take(8)}"
        val now = Timestamp.now()
        val uids = (listOf(currentAccount.userId) + members.map { it.userId }).distinct()
        val labels = linkedMapOf(
            currentAccount.userId to encodeMemberLabel(currentAccount.handle, currentAccount.displayName)
        )
        members.forEach { labels[it.userId] = encodeMemberLabel(it.handle, it.displayName) }
        val room = ChatRoom(
            id = newId,
            chatType = ChatType.GROUP.name,
            name = cleanName,
            creatorId = currentAccount.userId,
            memberUids = uids,
            memberLabels = labels,
            lastMessageText = "",
            lastMessageSenderId = "",
            lastMessageAt = now,
            readByUids = listOf(currentAccount.userId),
            createdAt = now,
            updatedAt = now,
        )
        chatsFlow.value = listOf(room) + chatsFlow.value
        return Result.success(newId)
    }

    override suspend fun updateGroupRoom(
        chatRoom: ChatRoom,
        newName: String,
        newMemberUids: List<String>,
        newMemberLabels: Map<String, String>
    ): Result<Unit> {
        val cleanName = newName.trim().take(60)
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Group name cannot be empty."))
        }
        chatsFlow.value = chatsFlow.value.map { room ->
            if (room.id == chatRoom.id) {
                room.copy(
                    name = cleanName,
                    memberUids = newMemberUids.distinct(),
                    memberLabels = newMemberLabels,
                    updatedAt = Timestamp.now(),
                )
            } else {
                room
            }
        }
        return Result.success(Unit)
    }

    override suspend fun sendMessage(
        chatRoom: ChatRoom,
        sender: UserAccount,
        text: String,
        replyTo: ChatMessage?
    ): Result<Unit> {
        val cleanText = text.trim().take(2000)
        if (cleanText.isEmpty()) return Result.success(Unit)
        val now = Timestamp.now()
        val msgId = "msg_${UUID.randomUUID().toString().take(8)}"
        val message = ChatMessage(
            id = msgId,
            chatId = chatRoom.id,
            senderId = sender.userId,
            senderName = sender.displayName,
            senderHandle = sender.handle,
            text = cleanText,
            memberUids = chatRoom.memberUids,
            readByUids = listOf(sender.userId),
            deleted = false,
            replyToMessageId = replyTo?.id,
            replyToText = replyTo?.text?.take(200),
            replyToSenderName = replyTo?.senderName,
            createdAt = now,
            updatedAt = now,
        )

        val currentList = messagesFlow.value[chatRoom.id] ?: emptyList()
        messagesFlow.value = messagesFlow.value + (chatRoom.id to (currentList + message))
        chatsFlow.value = chatsFlow.value.map { room ->
            if (room.id == chatRoom.id) {
                room.copy(
                    lastMessageText = cleanText,
                    lastMessageSenderId = sender.userId,
                    lastMessageAt = now,
                    readByUids = listOf(sender.userId),
                    updatedAt = now,
                )
            } else {
                room
            }
        }

        // Simulate real-time read receipt transition + peer cipher reply in Demo Mode
        val peerUid = chatRoom.memberUids.firstOrNull { it != sender.userId }
        if (peerUid != null) {
            simScope.launch {
                delay(650)
                // Mark as read (single check -> double check)
                val updatedMsgs = (messagesFlow.value[chatRoom.id] ?: emptyList()).map { m ->
                    if (m.id == msgId) m.copy(readByUids = (m.readByUids + peerUid).distinct()) else m
                }
                messagesFlow.value = messagesFlow.value + (chatRoom.id to updatedMsgs)
                chatsFlow.value = chatsFlow.value.map { r ->
                    if (r.id == chatRoom.id) r.copy(readByUids = (r.readByUids + peerUid).distinct()) else r
                }

                delay(1100)
                val peerInfo = parseMemberLabel(peerUid, chatRoom.memberLabels[peerUid] ?: "$peerUid|Peer")
                val replyText = autoReplies[replyIndex % autoReplies.size]
                replyIndex++
                val replyNow = Timestamp.now()
                val peerMsg = ChatMessage(
                    id = "msg_${UUID.randomUUID().toString().take(8)}",
                    chatId = chatRoom.id,
                    senderId = peerUid,
                    senderName = peerInfo.displayName,
                    senderHandle = peerInfo.handle,
                    text = replyText,
                    memberUids = chatRoom.memberUids,
                    readByUids = listOf(peerUid, sender.userId),
                    deleted = false,
                    createdAt = replyNow,
                    updatedAt = replyNow,
                )
                val afterMsgs = (messagesFlow.value[chatRoom.id] ?: emptyList()) + peerMsg
                messagesFlow.value = messagesFlow.value + (chatRoom.id to afterMsgs)
                chatsFlow.value = chatsFlow.value.map { r ->
                    if (r.id == chatRoom.id) {
                        r.copy(
                            lastMessageText = replyText,
                            lastMessageSenderId = peerUid,
                            lastMessageAt = replyNow,
                            readByUids = listOf(peerUid, sender.userId),
                            updatedAt = replyNow,
                        )
                    } else {
                        r
                    }
                }
            }
        }

        return Result.success(Unit)
    }

    override suspend fun markChatAndMessagesRead(
        chatRoom: ChatRoom,
        messages: List<ChatMessage>,
        userId: String
    ): Result<Unit> = Result.success(Unit)

    override suspend fun deleteMessage(chatId: String, messageId: String): Result<Unit> {
        val updated = (messagesFlow.value[chatId] ?: emptyList()).map { msg ->
            if (msg.id == messageId) {
                msg.copy(deleted = true, text = "[MESSAGE DELETED]", updatedAt = Timestamp.now())
            } else {
                msg
            }
        }
        messagesFlow.value = messagesFlow.value + (chatId to updated)
        return Result.success(Unit)
    }
}
