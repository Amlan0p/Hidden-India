package `in`.hiddenindia.app.ui.theme

/**
 * Exact port of web src/utils/avatarUtils.ts's AVATAR_PRESETS. Same illustrated SVGs
 * (hand-drawn turban/beanie/etc. explorer portraits), rendered via coil-svg so the picker
 * in ProfileScreen looks pixel-identical to web instead of a Compose approximation.
 */
data class AvatarPreset(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val svg: String
)

val AVATAR_PRESETS: List<AvatarPreset> = listOf(
    AvatarPreset(
        id = "preset_developer",
        name = "Cultural Connoisseur",
        category = "Heritage",
        description = "Heritage explorer uncovering ancient stepwells and classical forts.",
        svg = """
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 120">
  <defs>
    <linearGradient id="bg1" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#ea580c" />
      <stop offset="100%" stop-color="#7c2d12" />
    </linearGradient>
  </defs>
  <circle cx="60" cy="60" r="58" fill="url(#bg1)" stroke="#f97316" stroke-width="2"/>
  <path d="M38 48 C38 32 50 24 60 24 C70 24 82 32 82 48 C82 56 76 64 60 66 C44 64 38 56 38 48 Z" fill="#fed7aa"/>
  <path d="M34 40 C34 26 48 18 60 18 C74 18 86 28 86 42 C82 36 72 32 60 32 C48 32 38 36 34 40 Z" fill="#f97316"/>
  <path d="M48 20 C54 14 66 14 72 20 C66 22 54 22 48 20 Z" fill="#fbbf24"/>
  <circle cx="52" cy="46" r="2.5" fill="#431407"/>
  <circle cx="68" cy="46" r="2.5" fill="#431407"/>
  <path d="M50 56 C55 58 58 55 60 54 C62 55 65 58 70 56 C66 54 62 52 60 52 C58 52 54 54 50 56 Z" fill="#431407"/>
  <path d="M26 102 C28 78 44 70 60 70 C76 70 92 78 94 102 Z" fill="#fff7ed"/>
  <path d="M54 70 L60 88 L66 70 Z" fill="#ea580c"/>
</svg>""".trim()
    ),
    AvatarPreset(
        id = "preset_himalayan",
        name = "Himalayan Nomad",
        category = "Mountains",
        description = "High-altitude trekker walking misty pine ridges and passes.",
        svg = """
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 120">
  <defs>
    <linearGradient id="bg2" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#0284c7" />
      <stop offset="100%" stop-color="#0f172a" />
    </linearGradient>
  </defs>
  <circle cx="60" cy="60" r="58" fill="url(#bg2)" stroke="#38bdf8" stroke-width="2"/>
  <path d="M38 46 C38 30 48 22 60 22 C72 22 82 30 82 46 C82 56 76 64 60 66 C44 64 38 56 38 46 Z" fill="#fde047" fill-opacity="0.8"/>
  <path d="M34 44 C34 26 46 16 60 16 C74 16 86 26 86 44 C82 38 72 36 60 36 C48 36 38 38 34 44 Z" fill="#0284c7"/>
  <rect x="44" y="44" width="14" height="9" rx="3" fill="#0f172a"/>
  <rect x="62" y="44" width="14" height="9" rx="3" fill="#0f172a"/>
  <line x1="58" y1="48" x2="62" y2="48" stroke="#0f172a" stroke-width="2"/>
  <path d="M24 104 C26 76 44 68 60 68 C76 68 94 76 96 104 Z" fill="#e0f2fe"/>
  <path d="M52 68 L60 104 L68 68 Z" fill="#0284c7"/>
</svg>""".trim()
    ),
    AvatarPreset(
        id = "preset_priya",
        name = "Heritage Chronicler",
        category = "Culture",
        description = "Documenting folkloric traditions, handloom crafts, and oral histories.",
        svg = """
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 120">
  <defs>
    <linearGradient id="bg3" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#be123c" />
      <stop offset="100%" stop-color="#4c0519" />
    </linearGradient>
  </defs>
  <circle cx="60" cy="60" r="58" fill="url(#bg3)" stroke="#fb7185" stroke-width="2"/>
  <circle cx="60" cy="50" r="22" fill="#fed7aa"/>
  <path d="M36 46 C36 28 46 20 60 20 C74 20 84 28 84 46 C84 56 80 64 80 72 C74 58 72 38 60 38 C48 38 46 58 40 72 C40 64 36 56 36 46 Z" fill="#1e1b4b"/>
  <circle cx="60" cy="42" r="2" fill="#be123c"/>
  <circle cx="52" cy="48" r="2.2" fill="#1e1b4b"/>
  <circle cx="68" cy="48" r="2.2" fill="#1e1b4b"/>
  <path d="M26 104 C28 78 44 68 60 68 C76 68 92 78 94 104 Z" fill="#fdf2f8"/>
  <path d="M44 68 L60 104 L84 76 Z" fill="#e11d48"/>
</svg>""".trim()
    ),
    AvatarPreset(
        id = "preset_wildlife",
        name = "Wildlife Naturalist",
        category = "Wildlife",
        description = "Silent tracker across Western Ghats rainforests and Kaziranga wetlands.",
        svg = """
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 120">
  <defs>
    <linearGradient id="bg4" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#059669" />
      <stop offset="100%" stop-color="#064e3b" />
    </linearGradient>
  </defs>
  <circle cx="60" cy="60" r="58" fill="url(#bg4)" stroke="#34d399" stroke-width="2"/>
  <ellipse cx="60" cy="38" rx="28" ry="8" fill="#a16207"/>
  <path d="M42 38 C42 24 50 18 60 18 C70 18 78 24 78 38 Z" fill="#ca8a04"/>
  <path d="M42 42 C42 38 48 38 60 38 C72 38 78 38 78 42 C78 56 74 64 60 66 C46 64 42 56 42 42 Z" fill="#fed7aa"/>
  <circle cx="52" cy="48" r="2" fill="#1e293b"/>
  <circle cx="68" cy="48" r="2" fill="#1e293b"/>
  <path d="M26 104 C28 78 44 70 60 70 C76 70 92 78 94 104 Z" fill="#fef08a"/>
  <path d="M48 70 L60 104 L72 70 Z" fill="#047857"/>
</svg>""".trim()
    ),
    AvatarPreset(
        id = "preset_pilgrim",
        name = "Spiritual Seeker",
        category = "Spiritual",
        description = "Walking sacred riverbanks, temple sanctums, and peaceful ashrams.",
        svg = """
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 120">
  <defs>
    <linearGradient id="bg5" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#d97706" />
      <stop offset="100%" stop-color="#451a03" />
    </linearGradient>
  </defs>
  <circle cx="60" cy="60" r="58" fill="url(#bg5)" stroke="#fbbf24" stroke-width="2"/>
  <circle cx="60" cy="48" r="20" fill="#fed7aa"/>
  <path d="M58 36 L62 36 L60 46 Z" fill="#dc2626"/>
  <circle cx="60" cy="47" r="1.5" fill="#fbbf24"/>
  <circle cx="52" cy="48" r="2" fill="#1e293b"/>
  <circle cx="68" cy="48" r="2" fill="#1e293b"/>
  <path d="M26 104 C28 78 44 68 60 68 C76 68 92 78 94 104 Z" fill="#fef3c7"/>
  <path d="M38 72 L60 104 L82 72 Z" fill="#f59e0b"/>
</svg>""".trim()
    ),
    AvatarPreset(
        id = "preset_backpacker",
        name = "Coast-to-Coast Wanderer",
        category = "Offbeat",
        description = "Riding regional passenger trains and discovering tucked-away hamlets.",
        svg = """
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 120">
  <defs>
    <linearGradient id="bg6" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#0d9488" />
      <stop offset="100%" stop-color="#134e4a" />
    </linearGradient>
  </defs>
  <circle cx="60" cy="60" r="58" fill="url(#bg6)" stroke="#2dd4bf" stroke-width="2"/>
  <path d="M36 40 C36 24 48 18 60 18 C72 18 84 24 84 40 L92 44 L92 48 L84 46 C82 56 74 64 60 66 C46 64 38 56 36 46 L28 48 L28 44 Z" fill="#fed7aa"/>
  <path d="M34 36 C34 22 46 16 60 16 C74 16 86 22 86 36 Z" fill="#14b8a6"/>
  <rect x="28" y="40" width="8" height="14" rx="3" fill="#0f172a"/>
  <rect x="84" y="40" width="8" height="14" rx="3" fill="#0f172a"/>
  <path d="M26 104 C28 78 44 70 60 70 C76 70 92 78 94 104 Z" fill="#ccfbf1"/>
  <path d="M52 70 L60 104 L68 70 Z" fill="#0f766e"/>
</svg>""".trim()
    )
)

val DEFAULT_AVATAR_SVG: String = AVATAR_PRESETS.first().svg

/**
 * Normalizes an avatar string from backend data URIs or empty values so it matches
 * the predefined SVG format or preserves custom image formats.
 */
fun normalizeAvatarSvg(avatar: String?): String {
    if (avatar.isNullOrBlank()) return DEFAULT_AVATAR_SVG
    val trimmed = avatar.trim()
    if (trimmed.startsWith("<svg", ignoreCase = true)) return trimmed
    if (trimmed.startsWith("data:image/svg+xml", ignoreCase = true)) {
        val commaIndex = trimmed.indexOf(',')
        if (commaIndex != -1) {
            val header = trimmed.substring(0, commaIndex)
            val body = trimmed.substring(commaIndex + 1)
            return if (header.contains("base64", ignoreCase = true)) {
                try {
                    String(android.util.Base64.decode(body, android.util.Base64.DEFAULT), Charsets.UTF_8)
                } catch (e: Exception) {
                    trimmed
                }
            } else {
                try {
                    java.net.URLDecoder.decode(body, "UTF-8")
                } catch (e: Exception) {
                    trimmed
                }
            }
        }
    }
    return trimmed
}

/**
 * Decodes avatar content into a model (ByteArray or String) that Coil can reliably display.
 */
fun resolveAvatarModel(src: String?): Any {
    val normalized = normalizeAvatarSvg(src)
    val trimmed = normalized.trim()
    if (trimmed.startsWith("<svg", ignoreCase = true)) {
        return trimmed.toByteArray(Charsets.UTF_8)
    }
    if (trimmed.startsWith("data:image/", ignoreCase = true)) {
        val commaIndex = trimmed.indexOf(',')
        if (commaIndex != -1) {
            val header = trimmed.substring(0, commaIndex)
            val body = trimmed.substring(commaIndex + 1)
            if (header.contains("base64", ignoreCase = true)) {
                try {
                    return android.util.Base64.decode(body, android.util.Base64.DEFAULT)
                } catch (_: Exception) {}
            }
        }
    }
    return trimmed
}
