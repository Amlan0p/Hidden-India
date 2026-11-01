package `in`.hiddenindia.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import `in`.hiddenindia.app.data.Destination
import `in`.hiddenindia.app.ui.components.ThemedImagePlaceholder
import `in`.hiddenindia.app.ui.components.WikiVerifiedImage
import `in`.hiddenindia.app.ui.theme.ForestGreen
import `in`.hiddenindia.app.ui.theme.HeritageGold
import `in`.hiddenindia.app.ui.theme.TerracottaPrimary
import `in`.hiddenindia.app.ui.viewmodel.AuthViewModel
import `in`.hiddenindia.app.ui.viewmodel.MainViewModel

@Composable
fun DestinationDetailScreen(
    destination: Destination,
    viewModel: MainViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onPlanTrip: (Destination) -> Unit,
    onAskAi: (Destination) -> Unit,
    modifier: Modifier = Modifier,
    isOfflineOnly: Boolean = false
) {
    val context = LocalContext.current
    val favoriteIds by authViewModel.favoriteIds.collectAsState()
    val pendingFavoriteIds by authViewModel.pendingFavoriteIds.collectAsState()
    val favoriteError by authViewModel.favoriteError.collectAsState()
    val downloadedPackIds by viewModel.downloadedPackIds.collectAsState()
    val downloadingPackIds by viewModel.downloadingPackIds.collectAsState()
    val isFavorite = favoriteIds.contains(destination.id)
    val isFavoritePending = pendingFavoriteIds.contains(destination.id)
    val isDownloaded = downloadedPackIds.contains(destination.id)
    val isDownloading = downloadingPackIds.contains(destination.id)

    androidx.compose.runtime.LaunchedEffect(favoriteError) {
        favoriteError?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var mapFocusTarget by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    val tabs = listOf("Lore & Heritage", "Key Places", "Hotels", "Local Food", "Experiences", "Emergency")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
        ) {
            // Hero Image Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    val heroModel = remember(destination.coverImage, isOfflineOnly) {
                        val builder = ImageRequest.Builder(context)
                            .data(destination.coverImage.takeIf { it.isNotEmpty() }) // no stock fallback: only real Wikimedia photos
                            .crossfade(true)
                        if (isOfflineOnly) {
                            builder.networkCachePolicy(coil.request.CachePolicy.DISABLED)
                        }
                        builder.build()
                    }
                    var heroLoadFailed by remember(destination.coverImage) { mutableStateOf(false) }
                    if (destination.coverImage.isNotBlank() && !heroLoadFailed) {
                        AsyncImage(
                            model = heroModel,
                            contentDescription = destination.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            onError = { heroLoadFailed = true }
                        )
                    } else {
                        ThemedImagePlaceholder(
                            query = destination.name,
                            category = destination.category,
                            modifier = Modifier.fillMaxSize(),
                            isShimmering = false
                        )
                    }

                    // Dark overlay gradients
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.6f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )

                    // Top Action Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            IconButton(onClick = onBack, modifier = Modifier.testTag("btn_detail_back")) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "Hidden India: ${destination.name}")
                                            putExtra(Intent.EXTRA_TEXT, "${destination.name} — ${destination.tagline}")
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Destination"))
                                    },
                                    modifier = Modifier.testTag("btn_detail_share")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share Destination", tint = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = CircleShape,
                                color = if (isDownloaded) ForestGreen else Color.Black.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.toggleDownloadPack(destination.id) },
                                    enabled = !isDownloading,
                                    modifier = Modifier.testTag("btn_detail_download")
                                ) {
                                    if (isDownloading) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.FileDownload,
                                            contentDescription = "Download",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = CircleShape,
                                color = if (isFavorite) HeritageGold else Color.Black.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                IconButton(
                                    onClick = { authViewModel.toggleFavorite(destination.id) },
                                    enabled = !isFavoritePending,
                                    modifier = Modifier.testTag("btn_detail_fav")
                                ) {
                                    if (isFavoritePending) {
                                        androidx.compose.material3.CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = "Favorite",
                                            tint = if (isFavorite) Color(0xFF3E2723) else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Destination Info
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TerracottaPrimary
                        ) {
                            Text(
                                text = destination.category,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = destination.name,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = HeritageGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${destination.state} • ${destination.region}",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Quick Stats Row
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Best Season", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = destination.bestTimeToVisit.substringBefore("(").trim(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = TerracottaPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Budget / Day", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = destination.estimatedBudgetPerDay,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreen
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable(enabled = !isDownloading) { viewModel.toggleDownloadPack(destination.id) }
                        ) {
                            Text("Offline Pack", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isDownloading) "Syncing..." else if (isDownloaded) "Saved (${destination.formattedDownloadSizeMB} MB)" else "${destination.formattedDownloadSizeMB} MB",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDownloaded) ForestGreen else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Tagline & Description
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    if (destination.tagline.isNotEmpty()) {
                        Text(
                            text = "\"${destination.tagline}\"",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = TerracottaPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        text = destination.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Section Tabs
            item {
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = TerracottaPrimary,
                    edgePadding = 16.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = TerracottaPrimary
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        )
                    }
                }
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> { // Lore & Heritage
                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (!destination.loreTitle.isNullOrEmpty()) {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = TerracottaPrimary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = destination.loreTitle,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (!destination.ancientLegend.isNullOrEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Sacred Legend:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TerracottaPrimary)
                                            Text(destination.ancientLegend, style = MaterialTheme.typography.bodySmall, lineHeight = 20.sp)
                                        }
                                        if (!destination.historicalChronicle.isNullOrEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Historical Chronicle:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TerracottaPrimary)
                                            Text(destination.historicalChronicle, style = MaterialTheme.typography.bodySmall, lineHeight = 20.sp)
                                        }
                                    }
                                }
                            }

                            if (destination.folkloreTales.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Oral Folklore & Tales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                destination.folkloreTales.forEach { tale ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "• $tale",
                                            style = MaterialTheme.typography.bodySmall,
                                            lineHeight = 18.sp,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> { // Key Places
                    item {
                        val resolvedSnap = `in`.hiddenindia.app.ui.components.rememberResolvedImages(destination, isOfflineOnly).toMap()
                        val placeMarkers = remember(destination, resolvedSnap) {
                            val list = mutableListOf<`in`.hiddenindia.app.ui.components.MapMarker>()
                            list.add(
                                `in`.hiddenindia.app.ui.components.MapMarker(
                                    id = destination.id,
                                    lat = destination.coordinates.lat,
                                    lng = destination.coordinates.lng,
                                    name = destination.name,
                                    kind = "center",
                                    isCenter = true,
                                    image = destination.coverImage.ifBlank { null },
                                    description = destination.description.ifBlank { destination.tagline }.ifBlank { null },
                                    category = "${destination.state} • Center"
                                )
                            )
                            destination.places.forEach { p ->
                                p.coordinates?.let { c ->
                                    list.add(
                                        `in`.hiddenindia.app.ui.components.MapMarker(
                                            id = p.id,
                                            lat = c.lat,
                                            lng = c.lng,
                                            name = p.name,
                                            kind = if (p.isSecretGem) "gem" else "place",
                                            image = p.image.ifBlank { resolvedSnap[p.id] ?: "" }.ifBlank { null },
                                            description = p.description.ifBlank { null },
                                            category = p.category.ifBlank { null },
                                            insiderTip = p.insiderTip.ifBlank { null },
                                            bestTimeOfDay = p.bestTimeOfDay.ifBlank { null },
                                            estimatedCost = p.estimatedCost.ifBlank { null }
                                        )
                                    )
                                }
                            }
                            destination.localFoods.forEachIndexed { i, f ->
                                val c = f.coordinates ?: destination.coordinates
                                list.add(
                                    `in`.hiddenindia.app.ui.components.MapMarker(
                                        id = f.id ?: "food_$i",
                                        lat = c.lat,
                                        lng = c.lng,
                                        name = f.name,
                                        kind = "food",
                                        image = f.image?.ifBlank { null } ?: resolvedSnap[f.id ?: "food_$i"],
                                        description = f.description.ifBlank { null },
                                        category = null,
                                        insiderTip = f.whereToTry.ifBlank { null }?.let { "Try it at: $it" },
                                        estimatedCost = null
                                    )
                                )
                            }
                            destination.localExperiences.forEach { e ->
                                val c = e.coordinates ?: destination.coordinates
                                list.add(
                                    `in`.hiddenindia.app.ui.components.MapMarker(
                                        id = e.id,
                                        lat = c.lat,
                                        lng = c.lng,
                                        name = e.title,
                                        kind = "experience",
                                        image = e.image?.ifBlank { null } ?: resolvedSnap[e.id],
                                        description = e.description.ifBlank { null },
                                        category = e.category.ifBlank { null },
                                        insiderTip = e.communityHost.ifBlank { null }?.let { "Hosted by: $it" },
                                        bestTimeOfDay = e.duration.ifBlank { null },
                                        estimatedCost = e.cost.ifBlank { null }
                                    )
                                )
                            }
                            list
                        }
                        if (placeMarkers.isNotEmpty()) {
                            `in`.hiddenindia.app.ui.components.MapViewer(
                                markers = placeMarkers,
                                userLocation = null,
                                onMarkerClick = {},
                                height = 340.dp,
                                focusTarget = mapFocusTarget,
                                offlineOnly = isOfflineOnly,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .testTag("destination_interactive_map")
                            )
                        }
                    }
                    items(destination.places) { place ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column {
                                val placeImg = place.listImage
                                if (placeImg.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                    ) {
                                        var placeLoadFailed by remember(placeImg) { mutableStateOf(false) }
                                        if (!placeLoadFailed) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context)
                                                    .data(placeImg)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = place.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                                onError = { placeLoadFailed = true }
                                            )
                                        } else {
                                            // Stored URL is broken: look for a verified image instead.
                                            WikiVerifiedImage(
                                                query = place.name,
                                                category = place.category,
                                                area = "${destination.name}, ${destination.state}",
                                                offlineOnly = isOfflineOnly,
                                                recordId = place.id,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        if (place.isSecretGem) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = HeritageGold,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(10.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF3E2723), modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text("Secret Gem", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3E2723))
                                                }
                                            }
                                        }
                                        if (place.category.isNotEmpty()) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color.Black.copy(alpha = 0.65f),
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(10.dp)
                                            ) {
                                                Text(
                                                    text = place.category,
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // No hardcoded image for this specific place — try a
                                    // real, verified Wikipedia photo instead of falling
                                    // back to the destination's own cover image (that
                                    // fallback was the actual cause of places within a
                                    // destination visibly repeating the same picture).
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                    ) {
                                        WikiVerifiedImage(
                                            query = place.name,
                                            category = place.category,
                                            area = "${destination.name}, ${destination.state}",
                                            offlineOnly = isOfflineOnly,
                                            modifier = Modifier.fillMaxWidth().height(180.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = place.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(place.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                    if (place.insiderTip.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = TerracottaPrimary, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Tip: ${place.insiderTip}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = TerracottaPrimary)
                                        }
                                    }

                                    place.coordinates?.let { coords ->
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.testTag("btn_view_on_map_${place.id}").clickable {
                                                mapFocusTarget = coords.lat to coords.lng
                                            }
                                        ) {
                                            Icon(Icons.Default.Place, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("View on Map", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = ForestGreen)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> { // Hotels
                    items(destination.hotels) { hotel ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Hotel, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = hotel.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = hotel.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                            .clickable {
                                                try {
                                                    context.startActivity(
                                                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:${hotel.phone}"))
                                                    )
                                                } catch (e: Exception) { /* no dialer app */ }
                                            }
                                            .padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(hotel.phone, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                                            .clickable {
                                                try {
                                                    context.startActivity(
                                                        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${hotel.email}"))
                                                    )
                                                } catch (e: Exception) { /* no email app */ }
                                            }
                                            .padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Email, contentDescription = "Email", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Email", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                    if (destination.hotels.isEmpty()) {
                        item {
                            Text(
                                text = "No verified hotel listings yet for this destination.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }
                }
                3 -> { // Local Food
                    items(destination.localFoods) { food ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column {
                                val foodImg = food.listImage
                                if (!foodImg.isNullOrEmpty()) {
                                    var foodLoadFailed by remember(foodImg) { mutableStateOf(false) }
                                    if (!foodLoadFailed) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(foodImg)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = food.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(150.dp)
                                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                                            onError = { foodLoadFailed = true }
                                        )
                                    } else {
                                        // Stored URL is broken: look for a verified image instead.
                                        WikiVerifiedImage(
                                            query = food.name,
                                            category = "Food",
                                            area = "${destination.name}, ${destination.state}",
                                            offlineOnly = isOfflineOnly,
                                            recordId = food.id,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(150.dp)
                                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                        )
                                    }
                                } else {
                                    WikiVerifiedImage(
                                        query = food.name,
                                        category = "Food",
                                        area = "${destination.name}, ${destination.state}",
                                        offlineOnly = isOfflineOnly,
                                        recordId = food.id,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                    )
                                }
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(food.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(food.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (food.whereToTry.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Try at: ${food.whereToTry}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
                4 -> { // Experiences & Guidelines
                    items(destination.localExperiences) { exp ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column {
                                val expImg = exp.listImage
                                if (!expImg.isNullOrEmpty()) {
                                    var expLoadFailed by remember(expImg) { mutableStateOf(false) }
                                    if (!expLoadFailed) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(expImg)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = exp.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(150.dp)
                                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                                            onError = { expLoadFailed = true }
                                        )
                                    } else {
                                        // Stored URL is broken: look for a verified image instead.
                                        WikiVerifiedImage(
                                            query = exp.title,
                                            category = exp.category,
                                            area = "${destination.name}, ${destination.state}",
                                            offlineOnly = isOfflineOnly,
                                            recordId = exp.id,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(150.dp)
                                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                        )
                                    }
                                } else {
                                    WikiVerifiedImage(
                                        query = exp.title,
                                        category = exp.category,
                                        area = "${destination.name}, ${destination.state}",
                                        offlineOnly = isOfflineOnly,
                                        recordId = exp.id,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                    )
                                }
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(exp.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        if (exp.category.isNotBlank()) {
                                            Surface(shape = RoundedCornerShape(6.dp), color = TerracottaPrimary.copy(alpha = 0.15f)) {
                                                Text(exp.category, color = TerracottaPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(exp.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (exp.communityHost.isNotBlank()) {
                                        Text("Host: ${exp.communityHost}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                    val durationAndCost = listOf(exp.duration, exp.cost).filter { it.isNotBlank() }.joinToString(" • ")
                                    if (durationAndCost.isNotBlank()) {
                                        Text(durationAndCost, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Cultural Etiquette", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            destination.culturalGuidelines.forEach { guide ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Eco, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(guide, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
                5 -> { // Emergency
                    item {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFD32F2F))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Local Emergency Helplines", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Hospital
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Ambulance / medical emergency", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text(destination.emergencyInfo.hospitalNumber, color = TerracottaPrimary, fontSize = 12.sp)
                                        }
                                        IconButton(onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${destination.emergencyInfo.hospitalNumber}"))
                                            context.startActivity(intent)
                                        }) {
                                            Icon(Icons.Default.Call, contentDescription = "Call", tint = ForestGreen)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Police
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Police / all emergencies", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text(destination.emergencyInfo.policeNumber, color = TerracottaPrimary, fontSize = 12.sp)
                                        }
                                        IconButton(onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${destination.emergencyInfo.policeNumber}"))
                                            context.startActivity(intent)
                                        }) {
                                            Icon(Icons.Default.Call, contentDescription = "Call", tint = ForestGreen)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        "National tourist helpline: ${destination.emergencyInfo.touristHelpline}. To find the hospital and police station nearest to you, open the SOS screen.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Action Bar
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars),
            tonalElevation = 12.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onAskAi(destination) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.weight(1f).testTag("btn_ask_ai_destination")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TerracottaPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ask AI", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onPlanTrip(destination) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                    modifier = Modifier.weight(1.3f).testTag("btn_plan_trip_destination")
                ) {
                    Text("Plan AI Trip", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
