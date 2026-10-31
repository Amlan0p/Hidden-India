package `in`.hiddenindia.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExploreOff
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.hiddenindia.app.ui.components.DestinationCard
import `in`.hiddenindia.app.ui.components.ExperienceIndiaHero
import `in`.hiddenindia.app.ui.components.FilterSection
import `in`.hiddenindia.app.ui.components.openYoutubeVideo
import `in`.hiddenindia.app.ui.theme.HiddenIndiaTextStyles
import `in`.hiddenindia.app.ui.theme.JetBrainsMono
import `in`.hiddenindia.app.ui.theme.LightTertiary
import `in`.hiddenindia.app.ui.theme.PlayfairDisplay
import `in`.hiddenindia.app.ui.viewmodel.AuthViewModel
import `in`.hiddenindia.app.ui.viewmodel.MainViewModel

/**
 * Port of web src/pages/DiscoverPage.tsx. The search bar, SOS, avatar, and theme toggle
 * now live in the persistent HiddenIndiaTopBar (see MainActivity), so this screen starts
 * from the "Verified Heritage Archive" eyebrow the way the web page's <div> content does
 * once you scroll past the shared Header.
 */
@Composable
fun DiscoverScreen(
    viewModel: MainViewModel,
    authViewModel: AuthViewModel,
    onDestinationClick: (String) -> Unit,
    onOpenAiPlanner: () -> Unit,
    onOpenAskAi: () -> Unit,
    onOpenTranslator: () -> Unit,
    onOpenMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val destinations by viewModel.filteredDestinations.collectAsState()
    val allDestinations by viewModel.allDestinations.collectAsState()
    val favoriteIds by authViewModel.favoriteIds.collectAsState()
    val pendingFavoriteIds by authViewModel.pendingFavoriteIds.collectAsState()
    val downloadedPackIds by viewModel.downloadedPackIds.collectAsState()
    val downloadingPackIds by viewModel.downloadingPackIds.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedState by viewModel.selectedState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showDownloadedOnly by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val categories = remember(allDestinations) {
        listOf("All") + allDestinations.map { it.category }.distinct().sorted()
    }
    val states = remember(allDestinations) {
        listOf("All") + allDestinations.map { it.state }.distinct().sorted()
    }

    var visibleLimit by remember { mutableStateOf(12) }
    LaunchedEffect(searchQuery, selectedState, selectedCategory) { visibleLimit = 12 }

    val favoriteError by authViewModel.favoriteError.collectAsState()
    LaunchedEffect(favoriteError) {
        favoriteError?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    val baseList = if (showDownloadedOnly) destinations.filter { downloadedPackIds.contains(it.id) } else destinations
    val displayedDestinations = baseList.take(visibleLimit)
    val hasMore = baseList.size > visibleLimit
    val filtersActive = searchQuery.isNotBlank() || selectedState != "All" || selectedCategory != "All"

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                // Eyebrow + H1 + OSM Map View button
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Verified Heritage Archive • ${allDestinations.size} Sanctuaries",
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Discover Hidden India",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenMap)
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 6.dp))
                        Text("Map", fontFamily = JetBrainsMono, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Box(Modifier.padding(top = 16.dp)) {
                    FilterSection(
                        label = "FILTER BY STATE:",
                        items = states,
                        selectedItem = selectedState,
                        activeColor = MaterialTheme.colorScheme.primary,
                        onItemSelected = { viewModel.selectedState.value = it }
                    )
                }
                Box(Modifier.padding(top = 12.dp)) {
                    FilterSection(
                        label = "FILTER BY CATEGORY:",
                        items = categories,
                        selectedItem = selectedCategory,
                        activeColor = LightTertiary,
                        onItemSelected = { viewModel.selectedCategory.value = it }
                    )
                }

                ExperienceIndiaHero(
                    onWatchFilm = { openYoutubeVideo(context, "1K4fL_P5rQI") },
                    onPlanTrip = onOpenAiPlanner,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
                )

                // Results header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Showing ${destinations.size} verified hidden destination${if (destinations.size == 1) "" else "s"}",
                        style = HiddenIndiaTextStyles.labelCaps.copy(letterSpacing = 0.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    if (filtersActive) {
                        Row(
                            modifier = Modifier.clickable {
                                viewModel.searchQuery.value = ""
                                viewModel.selectedState.value = "All"
                                viewModel.selectedCategory.value = "All"
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 4.dp))
                            Text("Reset Filters", fontFamily = JetBrainsMono, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        if (isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        } else if (destinations.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.ExploreOff, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(bottom = 12.dp))
                    Text("No hidden destinations match your search", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Try searching for a destination name, state, landmark, or cuisine.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            items(displayedDestinations, key = { it.id }) { dest ->
                DestinationCard(
                    destination = dest,
                    isFavorite = favoriteIds.contains(dest.id),
                    isDownloaded = downloadedPackIds.contains(dest.id),
                    isDownloading = downloadingPackIds.contains(dest.id),
                    isFavoritePending = pendingFavoriteIds.contains(dest.id),
                    onCardClick = { onDestinationClick(dest.id) },
                    onToggleFavorite = { authViewModel.toggleFavorite(dest.id) },
                    onToggleDownload = { viewModel.toggleDownloadPack(dest.id) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (hasMore) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp)
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
                            "SHOW MORE DESTINATIONS (${displayedDestinations.size} OF ${destinations.size})",
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
