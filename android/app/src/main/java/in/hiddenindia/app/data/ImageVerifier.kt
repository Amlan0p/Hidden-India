package `in`.hiddenindia.app.data

import java.net.URLDecoder
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * NOTE: no longer used at runtime. The verified image database (ImageRepository) is now the single
 * source of image URLs, and its URLs are used exactly as stored. This file is retained only as
 * offline audit tooling and can be deleted once the database workflow is settled.
 *
 * Former role: central image-validation path.
 *
 * Every image URL stored in destinations.json (destination covers, places, foods, experiences)
 * passes through [sanitize] when the data is loaded. A URL that is clearly wrong for its item is
 * REMOVED (set to blank), so no screen ever displays it and the old runtime resolver (removed)
 * is asked for a verified replacement instead. This works offline: it reasons about the file
 * name, host, the item's name, its destination/state and the content type.
 *
 * It also exposes the shared text helpers and lexicons used by the resolver's candidate scoring.
 */
object ImageVerifier {

    enum class ItemKind { DESTINATION, PLACE, FOOD, EXPERIENCE }

    data class Verdict(val ok: Boolean, val reason: String = "")

    // ------------------------------------------------------------------------------------
    // Text helpers
    // ------------------------------------------------------------------------------------
    fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

    fun words(s: String): List<String> =
        normalize(s).split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }

    /** Name without bracketed alternates, cut at the first list/connector word. */
    fun primaryName(s: String): String =
        s.replace(Regex("\\([^)]*\\)"), " ")
            .split(Regex(",|&| with | at | in "))
            .firstOrNull().orEmpty().trim()

    val GENERIC_WORDS = setOf(
        "the", "and", "view", "viewpoint", "complex", "temple", "fort", "palace", "beach",
        "market", "village", "old", "national", "park", "india", "indian", "island", "lake",
        "hill", "hills", "valley", "falls", "waterfall", "museum", "garden", "gardens",
        "monastery", "gompa", "church", "mosque", "tomb", "bridge", "cave", "caves", "dam",
        "river", "town", "city", "road", "street", "tour", "walk", "trail", "trek", "safari",
        "festival", "food", "dish", "curry", "fry", "rice", "sweet", "special", "style", "local",
        "traditional", "experience", "workshop", "visit", "point", "square", "house", "center",
        "centre", "memorial", "stupa", "ghat", "bazaar", "bazar", "sanctuary", "reserve",
        "wildlife", "range", "area", "heritage", "show", "light", "sound", "cultural", "guided",
        "morning", "evening", "night", "private", "half", "full", "steamed", "fresh", "grilled",
        "fried", "boat", "ride", "class", "session", "demonstration", "homestay", "with", "from",
        "near", "sunset", "sunrise", "main", "view", "photo", "image", "file", "wikipedia",
        "commons", "thumb", "upload", "wikimedia", "jpg", "jpeg", "png", "webp", "front", "side",
        "general", "making", "tasting", "stay", "eco", "great", "grand", "upper", "lower", "little",
        "big", "royal", "ancient", "famous", "popular"
    )

    fun distinctive(name: String): List<String> =
        words(primaryName(name)).filter { it.length >= 4 && it !in GENERIC_WORDS }

    fun editDistance(a: String, b: String): Int {
        if (kotlin.math.abs(a.length - b.length) > 2) return 3
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length) {
            dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
        }
        return dp[a.length][b.length]
    }

    /** Spelling-tolerant presence check (Ranthambore/Ranthambhore, Uparkot/Uperkot...). */
    fun tokenInHay(token: String, hay: Collection<String>): Boolean = hay.any { h ->
        h == token ||
            (token.length >= 5 && h.length >= 4 && editDistance(token, h) <= 1) ||
            (token.length >= 8 && editDistance(token, h) <= 2) ||
            (h.length >= 6 && token.startsWith(h)) ||
            (token.length >= 6 && h.startsWith(token))
    }

    /** Words of a file name, also splitting CamelCase ("PremMandirSideView" -> prem mandir side view). */
    fun fileWordsOf(fileName: String): List<String> =
        words(fileName.replace(Regex("([a-z])([A-Z])"), "\$1 \$2"))

    fun fileName(url: String): String {
        val raw = url.substringBefore("?").substringAfterLast("/")
        val decoded = try { URLDecoder.decode(raw.replace("+", "%2B"), "UTF-8") } catch (_: Exception) { raw }
        return decoded.replace(Regex("^(lossy-page\\d+-)?\\d+px-"), "")
    }

    private fun isWikimediaHost(url: String): Boolean {
        val host = url.substringAfter("://", "").substringBefore("/").lowercase(Locale.ROOT)
        return host == "upload.wikimedia.org" || host == "thumb.wikimedia.org" ||
            host.endsWith(".wikimedia.org") || host.endsWith(".wikipedia.org")
    }

    // ------------------------------------------------------------------------------------
    // Lexicons
    // ------------------------------------------------------------------------------------

    /** Places outside India (and the languages of those places) that must never illustrate an Indian item. */
    val FOREIGN_TOKENS = setOf(
        "vietnam", "halong", "hanoi", "saigon", "nepal", "nepalese", "kathmandu", "pokhara", "lumbini",
        "bhutan", "thimphu", "paro", "punakha", "srilanka", "colombo", "kandy", "galle", "bangladesh",
        "dhaka", "chittagong", "pakistan", "lahore", "karachi", "islamabad", "peshawar", "afghanistan",
        "kabul", "china", "tibet", "lhasa", "beijing", "shanghai", "hongkong", "thailand", "bangkok",
        "chiangmai", "phuket", "myanmar", "burma", "yangon", "bagan", "mandalay", "cambodia", "angkor",
        "laos", "indonesia", "bali", "jakarta", "java", "malaysia", "singapore", "philippines", "manila",
        "japan", "tokyo", "kyoto", "osaka", "korea", "seoul", "russia", "moscow", "turkey", "istanbul",
        "egypt", "cairo", "giza", "iran", "tehran", "iraq", "baghdad", "dubai", "abudhabi", "oman",
        "muscat", "saudi", "mecca", "israel", "jerusalem", "jordan", "petra", "lebanon", "syria", "italy",
        "rome", "venice", "florence", "milan", "france", "paris", "germany", "berlin", "munich", "spain",
        "madrid", "barcelona", "portugal", "lisbon", "england", "london", "scotland", "edinburgh",
        "ireland", "dublin", "netherlands", "amsterdam", "belgium", "brussels", "switzerland", "zurich",
        "austria", "vienna", "greece", "athens", "poland", "prague", "hungary", "budapest", "sweden",
        "norway", "denmark", "finland", "iceland", "usa", "america", "newyork", "california", "texas",
        "florida", "chicago", "washington", "mexico", "bolivia", "peru", "cusco", "brazil", "argentina",
        "chile", "colombia", "venezuela", "cuba", "canada", "toronto", "vancouver", "australia", "sydney",
        "melbourne", "zealand", "africa", "kenya", "nairobi", "tanzania", "morocco", "marrakech",
        "ethiopia", "nigeria", "ghana", "maldives", "mauritius", "fiji", "sierra", "catedral", "iglesia",
        "castillo", "ciudad", "cathedrale", "eglise", "chateau", "kirche", "schloss"
    )

    private val FOREIGN_PHRASE = Regex("(^| )(de la|de los|de las|del|der|die|das)( |$)")

    private val OTHER_STATE_TOKENS = mapOf(
        "kerala" to "kerala", "rajasthan" to "rajasthan", "gujarat" to "gujarat", "punjab" to "punjab",
        "maharashtra" to "maharashtra", "karnataka" to "karnataka", "odisha" to "odisha", "orissa" to "odisha",
        "assam" to "assam", "bihar" to "bihar", "sikkim" to "sikkim", "meghalaya" to "meghalaya",
        "manipur" to "manipur", "nagaland" to "nagaland", "mizoram" to "mizoram", "tripura" to "tripura",
        "arunachal" to "arunachal", "uttarakhand" to "uttarakhand", "himachal" to "himachal",
        "kashmir" to "kashmir", "ladakh" to "ladakh", "andaman" to "andaman", "telangana" to "telangana",
        "andhra" to "andhra", "tamilnadu" to "tamil", "haryana" to "haryana", "jharkhand" to "jharkhand",
        "chhattisgarh" to "chhattisgarh", "uttar" to "uttar"
    )

    private val BAD_FILE = Regex(
        "flag_of|coat_of_arms|map_of|locator|location_map|_map\\.|satellite|nasa|landsat|modis|sentinel|" +
            "astronaut|iss0|from_space|montage|symbol|icon_|logo|emblem|seal_of|diagram|chart|collage|" +
            "portrait|stamp|\\.svg|tsunami"
    )

    private val MONUMENT_WORDS = setOf(
        "fort", "palace", "temple", "mosque", "church", "tomb", "gate", "darwaza", "minar", "gurdwara",
        "gurudwara", "stupa", "monastery", "gompa", "basilica", "cathedral", "mandir", "masjid", "mahal"
    )

    private val CRAFT_OR_FOOD_WORDS = setOf(
        "workshop", "making", "cooking", "tasting", "bhujia", "thali", "tea", "coffee", "sweets", "weaving",
        "pottery", "craft", "crafts", "class", "recipe", "cuisine", "snacks", "street food", "food"
    )

    private val PLACE_WORDS = setOf(
        "monastery", "fort", "temple", "palace", "lake", "beach", "falls", "waterfall", "mosque", "church",
        "stupa", "cave", "caves", "bridge", "ghat", "museum", "garden", "market", "gompa", "dam"
    )

    private val STATE_ALIASES = mapOf("tamil nadu" to "tamil", "tamilnadu" to "tamil")

    // ------------------------------------------------------------------------------------
    // Dataset-derived lexicon (built once from the loaded destinations)
    // ------------------------------------------------------------------------------------
    private val COMMON_ENGLISH = setOf(
        "flowers", "valley", "lake", "hills", "coast", "island", "city", "pradesh", "caves", "falls",
        "river", "beach", "village", "town", "temple", "fort", "bay", "peak", "lakes", "gate", "sea"
    )

    @Volatile private var otherPlaceTokens: Set<String> = emptySet()
    private val allowedByDestination = ConcurrentHashMap<String, Set<String>>()
    private val kindIndex = ConcurrentHashMap<String, ItemKind>()

    private fun indexKey(name: String, area: String?) = normalize(name).trim() + "|" + normalize(area.orEmpty()).trim()

    /** The content type of an item (from the loaded dataset), or null if unknown. */
    fun kindFor(name: String, area: String?): ItemKind? = kindIndex[indexKey(name, area)]

    fun allowedTokensFor(destinationName: String): Set<String> =
        allowedByDestination[normalize(destinationName).trim()] ?: emptySet()

    fun isOtherPlaceToken(token: String): Boolean = token in otherPlaceTokens

    private val DIRECTION_WORDS = setOf(
        "north", "south", "east", "west", "port", "mount", "mountain", "upper", "lower", "great",
        "little", "central", "sri", "new", "old", "rann", "gulf", "cape", "land", "kingdom"
    )

    fun prepare(destinations: List<Destination>) {
        val others = mutableSetOf("kollam", "ashtamudi", "thrissur", "kottayam", "palakkad", "lonavla")
        for (d in destinations) {
            // Every distinctive word of every destination name (including bracketed alternates such
            // as "Aurangabad"), except ordinary English/direction words.
            words(d.name).filter { it.length >= 4 && it !in GENERIC_WORDS && it !in COMMON_ENGLISH && it !in DIRECTION_WORDS }
                .forEach { others.add(it) }
        }
        otherPlaceTokens = others

        kindIndex.clear()
        allowedByDestination.clear()
        for (d in destinations) {
            val area = "${d.name}, ${d.state}"
            val allowed = mutableSetOf<String>()
            allowed.addAll(words(d.name)); allowed.addAll(words(d.state))
            kindIndex[indexKey(d.name, d.state)] = ItemKind.DESTINATION
            d.places.forEach { allowed.addAll(words(it.name)); kindIndex[indexKey(it.name, area)] = ItemKind.PLACE }
            d.localFoods.forEach { allowed.addAll(words(it.name)); kindIndex[indexKey(it.name, area)] = ItemKind.FOOD }
            d.localExperiences.forEach { allowed.addAll(words(it.title)); kindIndex[indexKey(it.title, area)] = ItemKind.EXPERIENCE }
            allowedByDestination[normalize(d.name).trim()] = allowed
        }
    }

    // ------------------------------------------------------------------------------------
    // Verification of an existing URL
    // ------------------------------------------------------------------------------------

    /** Item tokens that identify the item itself, with the destination's own words removed. */
    fun itemTokens(name: String, destinationName: String): List<String> {
        val destWords = words(destinationName).toSet()
        val all = distinctive(name)
        val own = all.filter { it !in destWords }
        return own.ifEmpty { all }
    }

    private fun stateTokens(state: String): Set<String> =
        words(state).map { STATE_ALIASES[it] ?: it }.toSet()

    /** Share of the item's distinctive tokens found in the file name (0..1). */
    fun fileMatchFraction(name: String, destinationName: String, fileWords: List<String>): Double {
        val tokens = itemTokens(name, destinationName)
        if (tokens.isEmpty()) return 0.0
        return tokens.count { tokenInHay(it, fileWords) }.toDouble() / tokens.size
    }

    fun verifyExisting(
        url: String,
        name: String,
        kind: ItemKind,
        destinationName: String,
        state: String,
        coverNames: List<String> = emptyList()
    ): Verdict {
        if (url.isBlank()) return Verdict(true)
        if (!isWikimediaHost(url)) return Verdict(false, "source is not Wikimedia, cannot verify")

        val fn = fileName(url)
        val fnNorm = normalize(fn)
        val fw = fileWordsOf(fn)
        val itemWords = words(name).toSet()
        val allowed = allowedTokensFor(destinationName) + itemWords
        val ownState = stateTokens(state)

        if (BAD_FILE.containsMatchIn(fnNorm)) return Verdict(false, "map, flag, satellite or non-photo file")

        val foreign = fw.firstOrNull { it in FOREIGN_TOKENS && it !in itemWords }
        if (foreign != null) return Verdict(false, "file refers to a place outside India ($foreign)")
        if (FOREIGN_PHRASE.containsMatchIn(fnNorm.replace('_', ' '))) return Verdict(false, "file name is in another language")

        val otherState = fw.firstOrNull { w -> OTHER_STATE_TOKENS[w]?.let { it !in ownState && it !in itemWords } == true }
        if (otherState != null) return Verdict(false, "file refers to another state ($otherState)")

        val otherPlace = fw.firstOrNull { it in otherPlaceTokens && it !in allowed }
        if (otherPlace != null) return Verdict(false, "file refers to another place ($otherPlace)")

        // Content type: a craft/food activity must not be illustrated by a monument, and food must not be a monument.
        val nameWords = words(name)
        val itemIsMonument = nameWords.any { it in MONUMENT_WORDS }
        val fileIsMonument = fw.any { it in MONUMENT_WORDS }
        val itemIsCraftOrFood = kind == ItemKind.FOOD || nameWords.any { it in CRAFT_OR_FOOD_WORDS }
        if (itemIsCraftOrFood && fileIsMonument && !itemIsMonument) {
            return Verdict(false, "monument photo for a food/craft item")
        }

        if ("museum" in nameWords && fileIsMonument && "museum" !in fw) {
            return Verdict(false, "monument photo for a museum")
        }

        return when (kind) {
            ItemKind.DESTINATION -> {
                val names = coverNames + destinationName
                val ok = names.any { n -> distinctive(n).any { tokenInHay(it, fw) } } ||
                    words(destinationName).any { it.length >= 4 && tokenInHay(it, fw) }
                if (ok) Verdict(true) else Verdict(false, "file matches neither the destination nor any of its places")
            }
            ItemKind.PLACE, ItemKind.FOOD -> {
                if (fileMatchFraction(name, destinationName, fw) >= 0.5) Verdict(true)
                else Verdict(false, if (kind == ItemKind.FOOD) "image is not this exact dish" else "file name does not match this place")
            }
            ItemKind.EXPERIENCE -> {
                val tokens = itemTokens(name, destinationName)
                // Otherwise: the file names the destination AND a kind of place the experience is
                // about (e.g. "Tawang Himalayan Monastery ..." with "TawangMonastery.jpg").
                val destInFile = words(destinationName).any { it.length >= 4 && tokenInHay(it, fw) }
                val sharedPlaceWord = nameWords.any { it in PLACE_WORDS && it in fw }
                if (tokens.any { tokenInHay(it, fw) } || (destInFile && sharedPlaceWord)) Verdict(true)
                else Verdict(false, "file matches nothing specific in the experience title")
            }
        }
    }

    // ------------------------------------------------------------------------------------
    // Dataset sanitizer (the single entry point used when destinations are loaded)
    // ------------------------------------------------------------------------------------
    data class Report(var checked: Int = 0, var removed: Int = 0, var duplicatesRemoved: Int = 0)

    private class Rec(val kind: ItemKind, val d: Int, val i: Int, val name: String, var url: String)

    fun sanitize(destinations: List<Destination>, report: Report = Report()): List<Destination> {
        prepare(destinations)
        val recs = mutableListOf<Rec>()
        val coverUrls = destinations.map { it.coverImage }.toMutableList()

        destinations.forEachIndexed { di, d ->
            val coverNames = d.places.map { it.name }
            if (coverUrls[di].isNotBlank()) {
                report.checked++
                if (!verifyExisting(coverUrls[di], d.name, ItemKind.DESTINATION, d.name, d.state, coverNames).ok) {
                    coverUrls[di] = ""; report.removed++
                }
            }
            d.places.forEachIndexed { i, p -> if (p.image.isNotBlank()) recs.add(Rec(ItemKind.PLACE, di, i, p.name, p.image)) }
            d.localFoods.forEachIndexed { i, f -> if (!f.image.isNullOrBlank()) recs.add(Rec(ItemKind.FOOD, di, i, f.name, f.image)) }
            d.localExperiences.forEachIndexed { i, e -> if (!e.image.isNullOrBlank()) recs.add(Rec(ItemKind.EXPERIENCE, di, i, e.title, e.image.orEmpty())) }
        }

        for (r in recs) {
            val d = destinations[r.d]
            report.checked++
            if (!verifyExisting(r.url, r.name, r.kind, d.name, d.state).ok) { r.url = ""; report.removed++ }
        }

        // Suspicious duplicates: unrelated records of the same kind sharing one image keep only the
        // best-matching record. Records naming the exact same subject may legitimately share.
        recs.filter { it.url.isNotBlank() }.groupBy { it.kind to it.url }.values.filter { it.size > 1 }.forEach { group ->
            val sig = group.map { r -> distinctive(r.name).sorted() }.toSet()
            if (sig.size <= 1) return@forEach
            val best = group.maxByOrNull { r ->
                val d = destinations[r.d]
                fileMatchFraction(r.name, d.name, fileWordsOf(fileName(r.url)))
            }
            for (r in group) if (r !== best) { r.url = ""; report.duplicatesRemoved++ }
        }

        val byKey = recs.associateBy { Triple(it.kind, it.d, it.i) }
        return destinations.mapIndexed { di, d ->
            d.copy(
                coverImage = coverUrls[di],
                places = d.places.mapIndexed { i, p -> p.copy(image = byKey[Triple(ItemKind.PLACE, di, i)]?.url ?: p.image) },
                localFoods = d.localFoods.mapIndexed { i, f -> f.copy(image = byKey[Triple(ItemKind.FOOD, di, i)]?.url ?: f.image) },
                localExperiences = d.localExperiences.mapIndexed { i, e -> e.copy(image = byKey[Triple(ItemKind.EXPERIENCE, di, i)]?.url ?: e.image) }
            )
        }
    }
}
