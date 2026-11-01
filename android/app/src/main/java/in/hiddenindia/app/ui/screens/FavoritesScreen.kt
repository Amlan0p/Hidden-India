package `in`.hiddenindia.app.ui.screens

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import `in`.hiddenindia.app.ui.components.ThemedImagePlaceholder
import `in`.hiddenindia.app.ui.theme.JetBrainsMono
import `in`.hiddenindia.app.ui.theme.LightTertiary
import `in`.hiddenindia.app.ui.theme.PlayfairDisplay
import `in`.hiddenindia.app.ui.viewmodel.AuthViewModel
import `in`.hiddenindia.app.ui.viewmodel.MainViewModel

private enum class FavoritesTab { DESTINATIONS, ITINERARIES }

/**
 * Port of web src/pages/FavoritesPage.tsx. Both tabs are backed by real account state
 * (AuthViewModel.favoriteIds / .savedItineraries) — nothing here is placeholder data.
 */
@Composable
fun FavoritesScreen(
    viewModel: MainViewModel,
    authViewModel: AuthViewModel,
    onOpenDestination: (String) -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenPlanner: () -> Unit,
    onOpenItinerary: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allDestinations by viewModel.allDestinations.collectAsState()
    val favoriteIds by authViewModel.favoriteIds.collectAsState()
    val savedItineraries by authViewModel.savedItineraries.collectAsState()
    var tab by remember { mutableStateOf(FavoritesTab.DESTINATIONS) }

    val favoriteDestinations = allDestinations.filter { favoriteIds.contains(it.id) }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text("SAVED COLLECTION", fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.primary)
            Text("Saved Gems & Plans", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))

            TabRow(
                selectedTabIndex = if (tab == FavoritesTab.DESTINATIONS) 0 else 1,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        thickness = 1.dp
                    )
                },
                indicator = { tabPositions ->
                    val selectedIndex = if (tab == FavoritesTab.DESTINATIONS) 0 else 1
                    if (selectedIndex in tabPositions.indices) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                            color = MaterialTheme.colorScheme.primary,
                            height = 2.5.dp
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Tab(
                    selected = tab == FavoritesTab.DESTINATIONS,
                    onClick = { tab = FavoritesTab.DESTINATIONS },
                    text = {
                        Text(
                            text = "Favorite Gems (${favoriteDestinations.size})",
                            fontFamily = JetBrainsMono,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp,
                            fontWeight = if (tab == FavoritesTab.DESTINATIONS) FontWeight.Bold else FontWeight.Medium,
                            color = if (tab == FavoritesTab.DESTINATIONS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                Tab(
                    selected = tab == FavoritesTab.ITINERARIES,
                    onClick = { tab = FavoritesTab.ITINERARIES },
                    text = {
                        Text(
                            text = "Saved Itineraries (${savedItineraries.size})",
                            fontFamily = JetBrainsMono,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp,
                            fontWeight = if (tab == FavoritesTab.ITINERARIES) FontWeight.Bold else FontWeight.Medium,
                            color = if (tab == FavoritesTab.ITINERARIES) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        if (tab == FavoritesTab.DESTINATIONS) {
            if (favoriteDestinations.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.BookmarkBorder,
                        title = "No favorite destinations saved yet",
                        subtitle = "Tap the heart icon on any hidden gem card to save it for your upcoming adventures.",
                        ctaLabel = "DISCOVER GEMS",
                        onCta = onOpenDiscover
                    )
                }
            } else {
                items(favoriteDestinations, key = { it.id }) { dest ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(176.dp)
                                .clickable { onOpenDestination(dest.id) }
                        ) {
                            val candidateUrl = dest.listImage
                            var hasError by remember(candidateUrl) { mutableStateOf(false) }
                            if (candidateUrl.isNotBlank() && !hasError) {
                                AsyncImage(model = candidateUrl, contentDescription = dest.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(176.dp), onError = { hasError = true })
                            } else {
                                ThemedImagePlaceholder(
                                    query = dest.name,
                                    category = dest.category,
                                    modifier = Modifier.fillMaxWidth().height(176.dp),
                                    isShimmering = false
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(176.dp)
                                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .clickable { authViewModel.toggleFavorite(dest.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Favorite, contentDescription = "Remove from favorites", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            }
                            Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                                Text(dest.name, fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Color.White)
                                Text(dest.state, fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(dest.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${dest.places.size} Spots", fontFamily = JetBrainsMono, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                Row(modifier = Modifier.clickable { onOpenDestination(dest.id) }, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Explore", fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp).padding(start = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (savedItineraries.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.EventNote,
                        title = "No itineraries created yet",
                        subtitle = "Use our AI Trip Planner to craft a personalized route and save it here.",
                        ctaLabel = "PLAN A TRIP",
                        onCta = onOpenPlanner
                    )
                }
            } else {
                items(savedItineraries, key = { it.id }) { it2 ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${it2.days} Days • ${it2.budgetStyle} Budget", fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, letterSpacing = 0.5.sp, color = LightTertiary)
                            Text("Curated ${it2.destinationName} Journey", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 1.dp))
                            Text(it2.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
                        }
                        Row(modifier = Modifier.padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "VIEW", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable { onOpenItinerary(it2.id) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444).copy(alpha = 0.1f))
                                    .clickable { authViewModel.deleteItineraryFromAccount(it2.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove itinerary", tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    ctaLabel: String,
    onCta: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(44.dp))
        Text(title, fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 10.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text(
            ctaLabel, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.5.sp, color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .padding(top = 14.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onCta)
                .padding(horizontal = 20.dp, vertical = 11.dp)
        )
    }
}
