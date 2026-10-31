package `in`.hiddenindia.app.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import `in`.hiddenindia.app.network.TokenStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data model for a locally persisted AI travel conversation.
 */
data class LocalConversation(
    val id: String,
    val title: String,
    val destinationId: String?,
    val destinationName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messages: List<ChatMessage> = emptyList()
)

/**
 * Manages local conversation persistence on-device.
 *
 * Scoped to the currently authenticated user (via TokenStorage) or "guest"
 * when not logged in. Stored entirely locally using SharedPreferences + Gson;
 * never sent or synced to the backend.
 */
class ChatHistoryRepository(private val context: Context) {

    private val gson = Gson()

    private fun getUserId(): String {
        return TokenStorage(context).getCachedUser()?.id ?: "guest"
    }

    private fun getPrefs(): SharedPreferences {
        val userScope = getUserId()
        return context.applicationContext.getSharedPreferences("chat_history_$userScope", Context.MODE_PRIVATE)
    }

    fun getConversations(): List<LocalConversation> {
        val json = getPrefs().getString(KEY_CONVERSATIONS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<LocalConversation>>() {}.type
            val list: List<LocalConversation> = gson.fromJson(json, type) ?: emptyList()
            list.sortedByDescending { it.updatedAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getConversation(id: String): LocalConversation? {
        return getConversations().find { it.id == id }
    }

    fun saveConversation(conversation: LocalConversation) {
        val current = getConversations().toMutableList()
        val index = current.indexOfFirst { it.id == conversation.id }
        if (index != -1) {
            current[index] = conversation
        } else {
            current.add(0, conversation)
        }
        val sorted = current.sortedByDescending { it.updatedAt }
        val json = gson.toJson(sorted)
        getPrefs().edit().putString(KEY_CONVERSATIONS, json).apply()
    }

    fun deleteConversation(id: String) {
        val current = getConversations().toMutableList()
        current.removeAll { it.id == id }
        val json = gson.toJson(current)
        val editor = getPrefs().edit().putString(KEY_CONVERSATIONS, json)
        if (getActiveConversationId() == id) {
            editor.remove(KEY_ACTIVE_CONV_ID)
        }
        editor.apply()
    }

    fun getActiveConversationId(): String? {
        return getPrefs().getString(KEY_ACTIVE_CONV_ID, null)
    }

    fun setActiveConversationId(id: String?) {
        val editor = getPrefs().edit()
        if (id == null) {
            editor.remove(KEY_ACTIVE_CONV_ID)
        } else {
            editor.putString(KEY_ACTIVE_CONV_ID, id)
        }
        editor.apply()
    }

    companion object {
        private const val KEY_CONVERSATIONS = "saved_conversations"
        private const val KEY_ACTIVE_CONV_ID = "last_active_conv_id"

        /**
         * Automatically generates a clean, concise title from the user's first question.
         * Example: "What are the hidden places to visit in Jaipur?" -> "Hidden places in Jaipur"
         */
        fun generateTitle(question: String): String {
            val cleaned = question.trim()
                .replace(Regex("(?i)^(can you|could you|please|tell me about|what is|what are|where is|where are|how to|how do I)\\s+"), "")
                .replace(Regex("[?!.,;:]+$"), "")
                .trim()

            val words = cleaned.split(Regex("\\s+"))
            val titleCandidate = if (words.size > 6) {
                words.take(6).joinToString(" ")
            } else if (cleaned.isNotBlank()) {
                cleaned
            } else {
                "Trip Discussion"
            }

            return titleCandidate.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
        }

        /**
         * Formats conversation timestamp for UI display (e.g. "Today", "Yesterday", "12 Oct").
         */
        fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            val oneDay = 24 * 60 * 60 * 1000L
            return when {
                diff < 60 * 1000L -> "Just now"
                diff < 60 * 60 * 1000L -> "${diff / (60 * 1000L)}m ago"
                diff < oneDay -> "Today"
                diff < 2 * oneDay -> "Yesterday"
                else -> {
                    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }
}
