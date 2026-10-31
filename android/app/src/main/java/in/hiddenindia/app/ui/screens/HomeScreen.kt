package `in`.hiddenindia.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import `in`.hiddenindia.app.data.Destination
import `in`.hiddenindia.app.ui.components.ExperienceIndiaHero
import `in`.hiddenindia.app.ui.components.ThemedImagePlaceholder
import `in`.hiddenindia.app.ui.components.openYoutubeVideo
import `in`.hiddenindia.app.ui.theme.Gold
import `in`.hiddenindia.app.ui.theme.JetBrainsMono
import `in`.hiddenindia.app.ui.theme.PlayfairDisplay
import `in`.hiddenindia.app.ui.viewmodel.AuthViewModel
import `in`.hiddenindia.app.ui.viewmodel.MainViewModel

/**
 * Port of web's CinematicFilmModal — same single official video
 * (https://youtu.be/1K4fL_P5rQI). Opens it in the YouTube app when installed, falling
 * back to a browser tab otherwise, rather than embedding a video player in-app.
 */
private fun openYoutubeVideo(context: Context, videoId: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    } catch (e: ActivityNotFoundException) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://youtu.be/$videoId")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}

/**
 * Port of web src/pages/HomePage.tsx — the feed shown at the authenticated web app's '/'
 * route, which is what the "Discover" bottom-nav tab actually links to on web (Navbar.tsx:
 * `{ to: '/', label: 'Discover', ... }`). This is a distinct screen from DiscoverScreen.kt
 * (which ports the separate '/discover' search/filter results page, reached from the search
 * bar). The search bar, SOS, avatar, and theme toggle live in the persistent
 * HiddenIndiaTopBar (see MainActivity), same as DiscoverScreen.
 */
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    authViewModel: AuthViewModel,
    onDestinationClick: (String) -> Unit,
    onOpenAiPlanner: () -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allDestinations by viewModel.allDestinations.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val favoriteIds by authViewModel.favoriteIds.collectAsState()
    val pendingFavoriteIds by authViewModel.pendingFavoriteIds.collectAsState()
    val downloadedPackIds by viewModel.downloadedPackIds.collectAsState()
    val downloadingPackIds by viewModel.downloadingPackIds.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    var selectedCategory by remember { mutableStateOf("All") }
    var selectedState by remember { mutableStateOf("All") }
    var visibleLimit by remember { mutableStateOf(12) }
    var stateMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery, selectedCategory, selectedState) {
        visibleLimit = 12
    }

    // Same hardcoded category list as HomePage.tsx (distinct from DiscoverPage.tsx's
    // dynamically-derived list).
    val categories = listOf("All", "Heritage", "Nature", "Food", "Culture", "Spiritual", "Adventure")
    val states = remember(allDestinations) {
        listOf("All") + allDestinations.map { it.state }.distinct().sorted()
    }

    val filteredDestinations = remember(allDestinations, selectedCategory, selectedState, searchQuery) {
        val q = searchQuery.trim().lowercase()
        val filtered = allDestinations.filter { d ->
            val matchesCategory = selectedCategory == "All" || d.category.equals(selectedCategory, ignoreCase = true) ||
                d.secondaryCategories.any { it.equals(selectedCategory, ignoreCase = true) }
            val matchesState = selectedState == "All" || d.state.equals(selectedState, ignoreCase = true)
            val matchesQuery = q.isEmpty() ||
                d.name.lowercase().contains(q) ||
                d.tagline.lowercase().contains(q) ||
                d.state.lowercase().contains(q) ||
                d.region.lowercase().contains(q) ||
                d.description.lowercase().contains(q) ||
                d.longDescription.lowercase().contains(q) ||
                d.category.lowercase().contains(q) ||
                d.secondaryCategories.any { it.lowercase().contains(q) } ||
                d.places.any { it.name.lowercase().contains(q) || it.description.lowercase().contains(q) }

            matchesCategory && matchesState && matchesQuery
        }
        if (q.isEmpty()) {
            filtered
        } else {
            filtered.sortedWith(
                compareBy(
                    // 1. Destination name starts with query (e.g. typing 'a' shows 'Abhaneri' first)
                    { d -> if (d.name.lowercase().startsWith(q)) 0 else 1 },
                    // 2. Any word in name starts with query
                    { d -> if (d.name.lowercase().split(" ").any { it.startsWith(q) }) 0 else 1 },
                    // 3. State starts with query
                    { d -> if (d.state.lowercase().startsWith(q)) 0 else 1 },
                    // 4. Any attraction starts with query
                    { d -> if (d.places.any { it.name.lowercase().startsWith(q) }) 0 else 1 },
                    // 5. Name contains query
                    { d -> if (d.name.lowercase().contains(q)) 0 else 1 },
                    // 6. Alphabetical
                    { d -> d.name }
                )
            )
        }
    }
    val displayedDestinations = filteredDestinations.take(visibleLimit)
    val hasMore = filteredDestinations.size > visibleLimit

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
        item {
            // Category horizontal scroll
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Text(
                        text = cat,
                        fontFamily = JetBrainsMono,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                            .border(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(50))
                            .clickable {
                                selectedCategory = cat
                                visibleLimit = 12
                            }
                            .padding(horizontal = 18.dp, vertical = 9.dp)
                    )
                }
            }

            // Filter bar: State selector, Budget pill, Duration pill, tune icon
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(50))
                                .clickable { stateMenuExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (selectedState == "All") "All States" else selectedState,
                                fontFamily = JetBrainsMono,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = stateMenuExpanded,
                            onDismissRequest = { stateMenuExpanded = false },
                            modifier = Modifier.heightIn(max = 360.dp)
                        ) {
                            states.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(if (s == "All") "All States" else s) },
                                    onClick = { selectedState = s; stateMenuExpanded = false }
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(50))
                            .clickable(onClick = onOpenAiPlanner)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                        Text("Budget", fontFamily = JetBrainsMono, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 6.dp))
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(50))
                            .clickable(onClick = onOpenAiPlanner)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                        Text("Duration", fontFamily = JetBrainsMono, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 6.dp))
                    }
                }

                Icon(
                    Icons.Filled.Tune,
                    contentDescription = "All Filters",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .clickable(onClick = onOpenSearch)
                        .padding(8.dp)
                )
            }

            ExperienceIndiaHero(
                onWatchFilm = { openYoutubeVideo(context, "1K4fL_P5rQI") },
                onPlanTrip = onOpenAiPlanner,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)
            )
        }

        if (isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        } else if (displayedDestinations.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Filled.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No sanctuaries found for \"$searchQuery\"" else "No sanctuaries match your filters",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Try searching by state name, temple, wildlife sanctuary, or broader keywords.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.searchQuery.value = ""
                            selectedCategory = "All"
                            selectedState = "All"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Clear Search & Filters", fontFamily = JetBrainsMono, fontSize = 12.sp)
                    }
                }
            }
        } else {
            if (searchQuery.isNotBlank()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Found ${filteredDestinations.size} result${if (filteredDestinations.size == 1) "" else "s"} for \"$searchQuery\"",
                            fontFamily = JetBrainsMono,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "CLEAR",
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier
                                .clickable { viewModel.searchQuery.value = "" }
                                .padding(4.dp)
                        )
                    }
                }
            }

            items(displayedDestinations, key = { it.id }) { dest ->
                HomeFeedCard(
                    destination = dest,
                    isFavorite = favoriteIds.contains(dest.id),
                    isFavoritePending = pendingFavoriteIds.contains(dest.id),
                    isDownloaded = downloadedPackIds.contains(dest.id),
                    isDownloading = downloadingPackIds.contains(dest.id),
                    onCardClick = { onDestinationClick(dest.id) },
                    onToggleFavorite = { authViewModel.toggleFavorite(dest.id) },
                    onToggleDownload = { viewModel.toggleDownloadPack(dest.id) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            if (hasMore) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(50))
                            .clickable { visibleLimit += 12 }
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                        Text(
                            "SHOW MORE DESTINATIONS (${displayedDestinations.size} OF ${filteredDestinations.size})",
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
}

/**
 * Port of the big feed-card markup in HomePage.tsx (h-72/h-80 hero image, Hidden Gem badge,
 * bookmark, best-time/budget rows, then a download bundle button + explore itinerary
 * button). Distinct from DestinationCard.kt, which ports DiscoverPage.tsx's smaller grid
 * card — these two have different layouts on web and are kept as separate composables here.
 */
@Composable
private fun HomeFeedCard(
    destination: Destination,
    isFavorite: Boolean,
    isFavoritePending: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    onCardClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(288.dp)
                .clickable(onClick = onCardClick)
        ) {
            var hasLoadError by remember(destination.coverImage) { mutableStateOf(false) }
            if (destination.coverImage.isNotBlank() && !hasLoadError) {
                AsyncImage(
                    model = destination.coverImage,
                    contentDescription = destination.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(288.dp),
                    onError = { hasLoadError = true }
                )
            } else {
                ThemedImagePlaceholder(
                    query = destination.name,
                    category = destination.category,
                    modifier = Modifier.fillMaxWidth().height(288.dp),
                    isShimmering = false
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(288.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0f),
                            0.55f to Color.Black.copy(alpha = 0.3f),
                            1f to Color.Black.copy(alpha = 0.8f)
                        )
                    )
            )

            // Hidden Gem badge
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Gold)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Diamond, contentDescription = null, tint = Color(0xFF1B1C1C), modifier = Modifier.size(14.dp))
                Text(
                    "HIDDEN GEM",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    color = Color(0xFF1B1C1C),
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            // Bookmark
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(enabled = !isFavoritePending, onClick = onToggleFavorite),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isFavorite) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = "Bookmark Gem",
                    tint = if (isFavorite) Color(0xFFEF4444) else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Text(
                    text = destination.name,
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    color = Color.White
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                    Text(
                        "${destination.state} • ${destination.region}",
                        fontFamily = JetBrainsMono,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = destination.description,
                fontFamily = PlayfairDisplay,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "Best time to visit",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        val openParen = destination.bestTimeToVisit.indexOf('(')
                        if (openParen != -1) {
                            val months = destination.bestTimeToVisit.substring(0, openParen).trim()
                            val note = destination.bestTimeToVisit.substring(openParen).trim()
                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = months,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    text = note,
                                    fontFamily = JetBrainsMono,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        } else {
                            Text(
                                text = destination.bestTimeToVisit,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.End,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Estimated Budget",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = destination.estimatedBudgetPerDay,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.End,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable(enabled = !isDownloaded && !isDownloading, onClick = onToggleDownload)
                    .padding(vertical = 13.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        if (isDownloaded) Icons.Filled.CloudDone else Icons.Filled.DownloadForOffline,
                        contentDescription = null,
                        tint = if (isDownloaded) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = if (isDownloading) "DOWNLOADING IMAGES & MAP TILES..."
                        else if (isDownloaded) "OFFLINE BUNDLE READY"
                        else "DOWNLOAD OFFLINE BUNDLE (${destination.formattedDownloadSizeMB} MB)",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = if (isDownloaded) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onCardClick)
                    .padding(vertical = 15.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "EXPLORE ITINERARY",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
