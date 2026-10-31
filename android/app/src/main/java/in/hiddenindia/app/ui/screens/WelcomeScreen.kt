package `in`.hiddenindia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import `in`.hiddenindia.app.data.Destination
import `in`.hiddenindia.app.ui.components.HiddenIndiaLogo
import `in`.hiddenindia.app.ui.components.LogoSize
import `in`.hiddenindia.app.ui.components.ThemedImagePlaceholder
import `in`.hiddenindia.app.ui.theme.JetBrainsMono
import `in`.hiddenindia.app.ui.theme.PlayfairDisplay
import `in`.hiddenindia.app.ui.theme.ThemeController
import `in`.hiddenindia.app.ui.viewmodel.MainViewModel

private data class SpotlightImage(val destinationId: String, val url: String, val state: String, val title: String)

// The exact 4 hero banner images from web WelcomePage.tsx (same Wikimedia URLs, same order).
private val HERO_BANNER = listOf(
    SpotlightImage("loc_0117", "https://upload.wikimedia.org/wikipedia/commons/thumb/f/fb/20191219_Fort_Amber%2C_Amer%2C_Jaipur_0955_9481.jpg/1280px-20191219_Fort_Amber%2C_Amer%2C_Jaipur_0955_9481.jpg", "Rajasthan", "Amber Palace"),
    SpotlightImage("loc_0165", "https://upload.wikimedia.org/wikipedia/commons/thumb/b/be/Darjeeling_Himalayan_Railway%2Ctoy_train_%282%29.jpg/960px-Darjeeling_Himalayan_Railway%2Ctoy_train_%282%29.jpg", "West Bengal", "Darjeeling Railway"),
    SpotlightImage("loc_0166", "https://upload.wikimedia.org/wikipedia/commons/thumb/7/72/Victoria_Memorial_situated_in_Kolkata.jpg/1280px-Victoria_Memorial_situated_in_Kolkata.jpg", "Kolkata", "Victoria Memorial"),
    SpotlightImage("loc_0033", "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/The_White_Desert_in_Kutch%2C_the_great_rann_of_kutch.jpg/960px-The_White_Desert_in_Kutch%2C_the_great_rann_of_kutch.jpg", "Gujarat", "Rann of Kutch")
)

private val SPOTLIGHT_IDS = listOf("loc_0011", "loc_0014", "loc_0047", "loc_0048", "loc_0003", "loc_0010", "loc_0013", "loc_0018", "loc_0006")
private val CATALOG_CATEGORIES = listOf("All", "Heritage", "Nature", "Mountains", "Spiritual", "Offbeat", "Culture")

/**
 * Port of web src/pages/WelcomePage.tsx — the public, unauthenticated landing page shown
 * before Login/Signup. On web this doubles as the root '/' route for a logged-out visitor;
 * here it's the start destination of AuthNavHost (see MainActivity).
 */
@Composable
fun WelcomeScreen(
    viewModel: MainViewModel,
    themeController: ThemeController,
    onLogin: () -> Unit,
    onSignup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allDestinations by viewModel.allDestinations.collectAsState()
    var selectedCategory by remember { mutableStateOf("All") }

    val featuredDestinations = remember(allDestinations, selectedCategory) {
        val spotlight = allDestinations.filter { SPOTLIGHT_IDS.contains(it.id) }
        val remainder = allDestinations.filterNot { SPOTLIGHT_IDS.contains(it.id) }
        val combined = spotlight + remainder
        if (selectedCategory == "All") combined.take(6)
        else combined.filter { it.category.equals(selectedCategory, ignoreCase = true) }.take(6)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            // ---- Public top bar ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HiddenIndiaLogo(size = LogoSize.Md, darkTheme = themeController.isDark)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable { themeController.toggle() }
                            .padding(10.dp)
                    ) {
                        Icon(
                            if (themeController.isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        "LOGIN",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable(onClick = onLogin)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                    Text(
                        "SIGN UP",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable(onClick = onSignup)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }

            // ---- Hero ----
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    Text(
                        "OFFBEAT HERITAGE & AI TRAVEL COMPANION",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Text(
                    "Discover the India You Haven't Seen",
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    lineHeight = 40.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 20.dp)
                )
                Text(
                    "Explore 167+ verified offbeat destinations across India with grounded day-by-day AI itinerary planning, Ask The Place heritage insights, live multilingual translations, and offline travel vaults.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 14.dp)
                )

                Row(
                    modifier = Modifier
                        .padding(top = 22.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onLogin)
                        .padding(horizontal = 28.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "GET STARTED",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(start = 8.dp).size(16.dp))
                }
            }

            // ---- Hero banner image grid (4 verified photos, all visible without scrolling) ----
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HERO_BANNER.take(2).forEach { banner ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(125.dp)
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            AsyncImage(
                                model = banner.url,
                                contentDescription = banner.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            0f to Color.Transparent,
                                            1f to Color.Black.copy(alpha = 0.8f)
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    banner.state,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    banner.title,
                                    fontFamily = PlayfairDisplay,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HERO_BANNER.drop(2).take(2).forEach { banner ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(125.dp)
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            AsyncImage(
                                model = banner.url,
                                contentDescription = banner.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            0f to Color.Transparent,
                                            1f to Color.Black.copy(alpha = 0.8f)
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    banner.state,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    banner.title,
                                    fontFamily = PlayfairDisplay,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // ---- Verified stats strip ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    .padding(vertical = 14.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
            ) {
                StatItem("167+", "Curated Offbeat Gems", Modifier.weight(1f))
                StatItem("28+", "States & Territories", Modifier.weight(1f))
                StatItem("100%", "Grounded Real Places", Modifier.weight(1f))
                StatItem("Offline", "Storage Vault & SOS", Modifier.weight(1f))
            }

            // ---- Explore Hidden Destinations catalog ----
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(
                    "VERIFIED HERITAGE CATALOG",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    "Explore Hidden Destinations",
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    "Preview our authentic travel catalog. Experience rich cultural stepwells, monasteries, river islands, and canyon landscapes.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(CATALOG_CATEGORIES) { cat ->
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
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        items(featuredDestinations, key = { it.id }) { dest ->
            WelcomeCatalogCard(
                destination = dest,
                onClick = onLogin,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }

        item {
            // ---- 6 feature pillars ----
            Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp).padding(horizontal = 16.dp)) {
                Text(
                    "A Complete Heritage Travel Platform",
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )
            }

            val pillars = listOf(
                Triple(Icons.Filled.AutoAwesome, "Grounded AI Trip Planner", "Day-by-day itineraries generated strictly from real, non-hallucinated historical attractions, authentic eateries, local transit, and artisan workshops."),
                Triple(Icons.Filled.Psychology, "Ask The Place AI Heritage Guide", "Chat directly with a destination-aware AI trained on regional folklore, architecture, local etiquette, temple customs, and insider tips."),
                Triple(Icons.Filled.Translate, "Live Regional Language Assistant", "Real-time voice & text translation across Indian regional dialects with phonetic pronunciation guides for respectful local interactions."),
                Triple(Icons.Filled.Map, "OpenStreetMap Cartography", "Live interactive maps pinpointing secret stepwells, mountain viewpoints, and hidden artisan studios with precise coordinates and route directions."),
                Triple(Icons.Filled.SdStorage, "Offline Destination Vault", "Download complete destination packs with guides, emergency contacts, and maps before heading into remote Himalayan valleys or river islands."),
                Triple(Icons.Filled.Emergency, "SOS Safety & Emergency Mode", "One-tap tourist helpline (1363), ambulance, nearest civil hospital directory, and local police contacts accessible anytime without network.")
            )
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                pillars.forEach { (icon, title, desc) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .clickable(onClick = onLogin)
                            .padding(20.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        }
                        Text(title, fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 12.dp))
                        Text(desc, fontSize = 12.sp, lineHeight = 17.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }

            // ---- Responsible Explorer Pledge banner ----
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF10B981).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.LocalFlorist, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(26.dp))
                }
                Text(
                    "The Responsible Explorer Pledge",
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    "We travel to preserve, not consume. Support indigenous artisans, respect sacred spaces, minimize waste, and celebrate India\u2019s rich cultural diversity.",
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .padding(top = 18.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onSignup)
                        .padding(horizontal = 22.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Create Account & Join Community", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(start = 8.dp).size(14.dp))
                }
            }

            // ---- Footer ----
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HiddenIndiaLogo(size = LogoSize.Sm, darkTheme = themeController.isDark)
                Text(
                    "\u00A9 2026 Hidden India. Developed by Royal Bengal Coders",
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontFamily = PlayfairDisplay,
            fontWeight = FontWeight.Bold,
            fontSize = if (value.length > 4) 18.sp else 21.sp,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            text = label.uppercase(),
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.SemiBold,
            fontSize = 8.5.sp,
            letterSpacing = 0.4.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
            lineHeight = 11.sp
        )
    }
}

/**
 * Port of the destination card in the "Explore Hidden Destinations" catalog grid on
 * WelcomePage.tsx (16:10 cover image, state + category badges, tagline, description clamp,
 * spot count + Explore link). Always leads to Login, since Welcome is only ever shown
 * unauthenticated (matches web's `navigate(user ? ... : '/login?redirect=...')` branch).
 */
@Composable
private fun WelcomeCatalogCard(destination: Destination, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1.6f)) {
            var hasError by remember(destination.coverImage) { mutableStateOf(false) }
            if (destination.coverImage.isNotBlank() && !hasError) {
                AsyncImage(model = destination.coverImage, contentDescription = destination.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), onError = { hasError = true })
            } else {
                ThemedImagePlaceholder(
                    query = destination.name,
                    category = destination.category,
                    modifier = Modifier.fillMaxSize(),
                    isShimmering = false
                )
            }
            Row(modifier = Modifier.align(Alignment.TopStart).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    destination.state,
                    fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color.White,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = 10.dp, vertical = 5.dp)
                )
                Text(
                    destination.category,
                    fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary).padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
        Column(modifier = Modifier.padding(18.dp)) {
            Text(destination.name, fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(destination.tagline, fontFamily = JetBrainsMono, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), modifier = Modifier.padding(top = 2.dp))
            Text(
                destination.description,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${destination.places.size} Verified Spots", fontFamily = JetBrainsMono, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Explore", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp).size(12.dp))
                }
            }
        }
    }
}
