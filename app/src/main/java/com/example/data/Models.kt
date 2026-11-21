package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ChatType {
    DIRECT,
    GROUP;

    companion object {
        fun fromRaw(raw: String): ChatType =
            entries.firstOrNull { it.name == raw } ?: DIRECT
    }
}

@Keep
@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val handle: String = "",
    val pin: String = "0000",
    val online: Boolean = false,
    @ServerTimestamp val lastSeenAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun isCurrentlyOnline(): Boolean {
        if (!online) return false
        val lastSeenMs = lastSeenAt?.toDate()?.time ?: return true
        return (System.currentTimeMillis() - lastSeenMs) <= 120_000L
    }
}

@Keep
@IgnoreExtraProperties
data class HandleDirectoryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val handle: String = "",
    val displayName: String = "",
    val pin: String = "0000",
    val online: Boolean = false,
    @ServerTimestamp val lastSeenAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun isCurrentlyOnline(): Boolean {
        if (!online) return false
        val lastSeenMs = lastSeenAt?.toDate()?.time ?: return true
        return (System.currentTimeMillis() - lastSeenMs) <= 120_000L
    }
}

@Keep
@IgnoreExtraProperties
data class ChatRoom(
    @DocumentId val id: String = "",
    val chatType: String = ChatType.DIRECT.name,
    val name: String = "",
    val creatorId: String = "",
    val memberUids: List<String> = emptyList(),
    val memberLabels: Map<String, String> = emptyMap(),
    val lastMessageText: String = "",
    val lastMessageSenderId: String = "",
    @ServerTimestamp val lastMessageAt: Timestamp? = null,
    val readByUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun resolvedType(): ChatType = ChatType.fromRaw(chatType)

    fun directPeerUid(currentUserId: String): String? =
        if (resolvedType() == ChatType.DIRECT) {
            memberUids.firstOrNull { it != currentUserId } ?: currentUserId
        } else {
            null
        }

    fun directPeerInfo(currentUserId: String): MemberInfo? {
        val peerUid = directPeerUid(currentUserId) ?: return null
        val raw = memberLabels[peerUid] ?: return null
        return parseMemberLabel(peerUid, raw)
    }

    fun displayTitleFor(currentUserId: String): String {
        return if (resolvedType() == ChatType.DIRECT) {
            directPeerInfo(currentUserId)?.displayName ?: name.ifBlank { "Direct Chat" }
        } else {
            name.ifBlank { "Group Chat" }
        }
    }

    fun subtitleFor(currentUserId: String): String {
        return if (resolvedType() == ChatType.DIRECT) {
            val handle = directPeerInfo(currentUserId)?.handle
            if (!handle.isNullOrBlank()) "@$handle" else "DIRECT"
        } else {
            "${memberUids.size} MEMBERS"
        }
    }

    fun membersList(): List<MemberInfo> =
        memberUids.map { uid ->
            val raw = memberLabels[uid] ?: "$uid|Member"
            parseMemberLabel(uid, raw)
        }

    fun isLastMessageReadByOther(currentUserId: String): Boolean =
        readByUids.any { it != currentUserId }
}

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderHandle: String = "",
    val text: String = "",
    val memberUids: List<String> = emptyList(),
    val readByUids: List<String> = emptyList(),
    val deleted: Boolean = false,
    val replyToMessageId: String? = null,
    val replyToText: String? = null,
    val replyToSenderName: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun isReadByOthers(currentUserId: String): Boolean =
        readByUids.any { it != currentUserId }
}

data class MemberInfo(
    val uid: String,
    val handle: String,
    val displayName: String,
)

fun encodeMemberLabel(handle: String, displayName: String): String {
    val cleanHandle = handle.trim().lowercase().replace("|", "")
    val cleanName = displayName.trim().replace("|", "")
    return "$cleanHandle|$cleanName"
}

fun parseMemberLabel(uid: String, raw: String): MemberInfo {
    val idx = raw.indexOf('|')
    return if (idx >= 0) {
        MemberInfo(
            uid = uid,
            handle = raw.substring(0, idx),
            displayName = raw.substring(idx + 1).ifBlank { raw.substring(0, idx) }
        )
    } else {
        MemberInfo(uid = uid, handle = raw, displayName = raw)
    }
}

fun formatMonospaceTime(timestamp: Timestamp?): String {
    val date = timestamp?.toDate() ?: return "--:--"
    return SimpleDateFormat("HH:mm", Locale.US).format(date)
}

fun formatDateSeparator(timestamp: Timestamp?): String {
    val date = timestamp?.toDate() ?: return "TODAY"
    val cal = Calendar.getInstance().apply { time = date }
    val now = Calendar.getInstance()
    return when {
        cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) -> "TODAY"
        cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) - 1 -> "YESTERDAY"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.US).format(date).uppercase(Locale.US)
    }
}

fun dayBucketKey(timestamp: Timestamp?): String {
    val date = timestamp?.toDate() ?: Date()
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
}

fun normalizeHandleInput(raw: String): String =
    raw.trim()
        .removePrefix("@")
        .lowercase(Locale.US)
        .filter { it in 'a'..'z' || it in '0'..'9' || it == '_' }
        .take(24)

fun generateRandomPin(): String =
    (1000..9999).random().toString()
