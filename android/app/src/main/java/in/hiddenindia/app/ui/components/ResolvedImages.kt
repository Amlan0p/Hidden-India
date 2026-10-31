package `in`.hiddenindia.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import `in`.hiddenindia.app.data.Destination
import `in`.hiddenindia.app.data.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Verified photos for the places / foods / experiences of [dest] that have no stored image,
 * keyed by record ID. Each lookup is an exact record-ID read from the verified image database
 * ([ImageRepository.imageUrlFor]); a record without a verified image yields nothing, so its map
 * marker uses the existing placeholder. No name matching and no web search.
 */
@Composable
fun rememberResolvedImages(dest: Destination?, offlineOnly: Boolean = false): Map<String, String> {
    val context = LocalContext.current
    val resolved = remember(dest?.id) { mutableStateMapOf<String, String>() }
    LaunchedEffect(dest?.id, offlineOnly) {
        val d = dest ?: return@LaunchedEffect
        val ids = buildList {
            d.places.forEach { if (it.image.isBlank()) add(it.id) }
            d.localFoods.forEach { f -> val id = f.id; if (f.image.isNullOrBlank() && !id.isNullOrBlank()) add(id) }
            d.localExperiences.forEach { if (it.image.isNullOrBlank()) add(it.id) }
        }
        withContext(Dispatchers.IO) {
            for (id in ids) {
                ImageRepository.imageUrlFor(context, id)?.let { resolved[id] = it }
            }
        }
    }
    return resolved
}
