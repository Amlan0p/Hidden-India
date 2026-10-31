package `in`.hiddenindia.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Fort
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TempleHindu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import `in`.hiddenindia.app.data.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import `in`.hiddenindia.app.ui.theme.JetBrainsMono

/**
 * Verified image loader for Indian places, foods, and experiences.
 * The image URL comes from the verified image database (see ImageRepository); there is no
 * runtime web search.
 *
 * If images don't load or cannot be verified with certainty:
 * Displays a clean, themed placeholder. Never injects a default Taj Mahal or random picture.
 */
@Composable
fun WikiVerifiedImage(
    query: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    offlineOnly: Boolean = false,
    area: String? = null,
    category: String? = null,
    recordId: String? = null
) {
    val context = LocalContext.current
    // The image comes from the verified image database by EXACT record ID. Without a record ID, or
    // for a record with no verified image, there is nothing to show: the themed placeholder is used.
    val imageResult by produceState<String?>(initialValue = null, recordId, offlineOnly) {
        value = if (recordId.isNullOrBlank()) null
        else withContext(Dispatchers.IO) { ImageRepository.imageUrlFor(context, recordId) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        val currentImage = imageResult
        var hasLoadError by remember(currentImage) { mutableStateOf(false) }

        if (!currentImage.isNullOrEmpty() && !hasLoadError) {
            val request = remember(currentImage, offlineOnly) {
                val builder = ImageRequest.Builder(context)
                    .data(currentImage)
                    .crossfade(300)
                if (offlineOnly) {
                    builder.networkCachePolicy(coil.request.CachePolicy.DISABLED)
                }
                builder.build()
            }
            AsyncImage(
                model = request,
                contentDescription = query,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
                onError = {
                    hasLoadError = true
                }
            )
        } else {
            // Elegant themed placeholder — never random or Taj Mahal fallback
            ThemedImagePlaceholder(query = query, category = category)
        }
    }
}

@Composable
fun ThemedImagePlaceholder(
    query: String,
    category: String?,
    modifier: Modifier = Modifier,
    isShimmering: Boolean = true
) {
    val alpha = if (isShimmering) {
        val transition = rememberInfiniteTransition(label = "shimmer")
        val animatedAlpha by transition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        animatedAlpha
    } else {
        0.7f
    }

    val icon: ImageVector = remember(category, query) {
        val cat = (category ?: "").lowercase()
        val q = query.lowercase()
        when {
            cat.contains("food") || cat.contains("culinary") || q.contains("thali") ||
                    q.contains("curry") || q.contains("dish") || q.contains("biryani") ||
                    q.contains("fish") || q.contains("snack") || q.contains("coffee") || q.contains("tea") ->
                Icons.Default.Restaurant
            cat.contains("spiritual") || q.contains("temple") || q.contains("darshan") || q.contains("shrine") ->
                Icons.Default.TempleHindu
            cat.contains("heritage") || q.contains("fort") || q.contains("palace") || q.contains("caves") ->
                Icons.Default.Fort
            cat.contains("adventure") || q.contains("trek") || q.contains("safari") ->
                Icons.Default.DirectionsRun
            cat.contains("nature") || q.contains("falls") || q.contains("valley") || q.contains("lake") ->
                Icons.Default.Landscape
            cat.contains("culture") || q.contains("art") || q.contains("craft") ->
                Icons.Default.Palette
            else ->
                Icons.Default.Place
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha),
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = (category ?: "DISCOVER").uppercase(),
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
            )
            Text(
                text = query,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
