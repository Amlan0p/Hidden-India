package `in`.hiddenindia.app.ui.theme

import androidx.compose.ui.graphics.Color

// Exact port of web src/utils/avatarUtils.ts's getInitials/getAvatarGradient so a given
// user name always resolves to the same fallback initials + gradient on both platforms.

data class AvatarGradient(val from: Color, val to: Color)

private val AVATAR_GRADIENTS = listOf(
    AvatarGradient(Color(0xFFEA580C), Color(0xFFC2410C)), // Terracotta & Saffron
    AvatarGradient(Color(0xFF0284C7), Color(0xFF0369A1)), // Royal Indigo
    AvatarGradient(Color(0xFF059669), Color(0xFF047857)), // Emerald Forest
    AvatarGradient(Color(0xFFD97706), Color(0xFFB45309)), // Golden Sandstone
    AvatarGradient(Color(0xFF7C3AED), Color(0xFF6D28D9)), // Heritage Amethyst
    AvatarGradient(Color(0xFFE11D48), Color(0xFFBE123C)), // Lotus Carmine
    AvatarGradient(Color(0xFF0D9488), Color(0xFF0F766E)), // Kashmir Jade
    AvatarGradient(Color(0xFF475569), Color(0xFF334155)), // Himalayan Slate
)

fun getInitials(name: String?): String {
    if (name.isNullOrBlank()) return "HI"
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (parts.isEmpty()) return "HI"
    if (parts.size == 1) return parts[0].take(2).uppercase()
    return (parts.first().take(1) + parts.last().take(1)).uppercase()
}

fun getAvatarGradient(seed: String?): AvatarGradient {
    val str = (seed ?: "Explorer").lowercase()
    var hash = 0
    for (c in str) {
        hash = (hash shl 5) - hash + c.code
        hash = hash or 0 // force 32-bit overflow semantics, matching `hash |= 0` in JS
    }
    val index = kotlin.math.abs(hash) % AVATAR_GRADIENTS.size
    return AVATAR_GRADIENTS[index]
}
