package `in`.hiddenindia.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import `in`.hiddenindia.app.data.Destination
import `in`.hiddenindia.app.data.GeoUtils
import `in`.hiddenindia.app.ui.components.MapMarker
import `in`.hiddenindia.app.ui.components.MapViewer
import `in`.hiddenindia.app.ui.theme.JetBrainsMono
import `in`.hiddenindia.app.ui.theme.PlayfairDisplay
import `in`.hiddenindia.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

private enum class MapSection { ALL, PLACES, FOOD, EXPERIENCES }

/**
 * Port of web src/pages/InteractiveMapPage.tsx. The map itself is a real Leaflet+OSM map
 * rendered in a WebView (see MapViewer.kt) rather than a static list, matching the web
 * app's actual mapping technology. GPS uses the plain Android LocationManager (no Play
 * Services dependency was already in this project) instead of the web Geolocation API.
 *
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveMapScreen(
    viewModel: MainViewModel,
    onDestinationSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    offlineOnly: Boolean = false
) {
    val allDestinations by viewModel.allDestinations.collectAsState()
    val downloadedPackIds by viewModel.downloadedPackIds.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedDestId by remember { mutableStateOf("MY_LOCATION") }
    var regionMenuOpen by remember { mutableStateOf(false) }
    var section by remember { mutableStateOf(MapSection.ALL) }
    var focusTarget by remember { mutableStateOf<Pair<Double, Double>?>(null) }

    var userCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var isLocating by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var nearest by remember { mutableStateOf<Pair<Destination, Double>?>(null) }

    fun locate() {
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) {
            locationError = "Location permission not granted."
            isLocating = false
            return
        }
        isLocating = true
        locationError = null
        scope.launch {
            try {
                val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
                val providers = lm.getProviders(true)
                val loc = providers.mapNotNull { lm.getLastKnownLocation(it) }.maxByOrNull { it.time }
                if (loc == null) {
                    locationError = "Unable to detect GPS position. Please check location permissions."
                } else {
                    userCoords = loc.latitude to loc.longitude
                    if (allDestinations.isNotEmpty()) {
                        nearest = GeoUtils.findNearestDestination(loc.latitude, loc.longitude, allDestinations)
                    }
                }
            } catch (e: SecurityException) {
                locationError = "Location permission not granted."
            } finally {
                isLocating = false
            }
        }
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { locate() }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    val isMyLocation = selectedDestId == "MY_LOCATION"
    val currentDest: Destination? = if (isMyLocation) {
        nearest?.first ?: allDestinations.firstOrNull()
    } else {
        allDestinations.find { it.id == selectedDestId } ?: allDestinations.firstOrNull()
    }

    val resolvedImages = `in`.hiddenindia.app.ui.components.rememberResolvedImages(currentDest)
    val resolvedSnapshot = resolvedImages.toMap()
    val areaLabel = currentDest?.let { "${it.name}, ${it.state}" }

    val markers = remember(currentDest, section, resolvedSnapshot) {
        val list = mutableListOf<MapMarker>()
        currentDest?.let { dest ->
            list.add(
                MapMarker(
                    id = dest.id,
                    lat = dest.coordinates.lat,
                    lng = dest.coordinates.lng,
                    name = dest.name,
                    kind = "center",
                    isCenter = true,
                    image = dest.coverImage.ifBlank { null },
                    description = dest.description.ifBlank { dest.tagline }.ifBlank { null },
                    category = "${dest.state} • Center",
                    insiderTip = dest.tagline.ifBlank { null },
                    bestTimeOfDay = dest.bestTimeToVisit.ifBlank { null },
                    estimatedCost = dest.estimatedBudgetPerDay.ifBlank { null }
                )
            )
            val addPlaces = {
                dest.places.forEach { p ->
                    p.coordinates?.let {
                        list.add(
                            MapMarker(
                                id = p.id, lat = it.lat, lng = it.lng, name = p.name,
                                kind = if (p.isSecretGem) "gem" else "place",
                                image = p.image.ifBlank { resolvedSnapshot[p.id] ?: "" }.ifBlank { null },
                                description = p.description.ifBlank { p.insiderTip }.ifBlank { dest.tagline }.ifBlank { null },
                                category = p.category,
                                insiderTip = p.insiderTip.ifBlank { null },
                                bestTimeOfDay = p.bestTimeOfDay.ifBlank { null },
                                estimatedCost = p.estimatedCost.ifBlank { null }
                            )
                        )
                    }
                }
            }
            val addFoods = {
                dest.localFoods.forEachIndexed { i, f ->
                    val c = f.coordinates ?: dest.coordinates
                    list.add(
                        MapMarker(
                            id = f.id ?: "food_$i", lat = c.lat, lng = c.lng, name = f.name, kind = "food",
                            image = (f.image?.ifBlank { null } ?: resolvedSnapshot[f.id ?: "food_$i"]),
                            description = f.description.ifBlank { f.whereToTry?.let { "Famous culinary specialty. Best enjoyed at $it." } ?: "Traditional regional delicacy in ${dest.name}." },
                            category = null,
                            insiderTip = f.whereToTry.ifBlank { null }?.let { "Try it at: $it" },
                            estimatedCost = null
                        )
                    )
                }
            }
            val addExperiences = {
                dest.localExperiences.forEach { e ->
                    val c = e.coordinates ?: dest.coordinates
                    list.add(
                        MapMarker(
                            id = e.id, lat = c.lat, lng = c.lng, name = e.title, kind = "experience",
                            image = (e.image?.ifBlank { null } ?: resolvedSnapshot[e.id]),
                            description = e.description.ifBlank { "Authentic cultural experience in ${dest.name}." },
                            category = e.category.ifBlank { "Cultural Experience" },
                            insiderTip = e.communityHost.ifBlank { null }?.let { "Hosted by: $it" },
                            bestTimeOfDay = e.duration.ifBlank { null },
                            estimatedCost = e.cost.ifBlank { null }
                        )
                    )
                }
            }
            when (section) {
                MapSection.ALL -> {
                    addPlaces()
                    addFoods()
                    addExperiences()
                }
                MapSection.PLACES -> addPlaces()
                MapSection.FOOD -> addFoods()
                MapSection.EXPERIENCES -> addExperiences()
            }
        }
        list
    }

    LazyColumn(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header + region selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            Text("  OPENSTREETMAP CARTOGRAPHY", fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Text("Explore All Hidden Spots", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 2.dp))
                    }
                }

                Box(modifier = Modifier.padding(top = 10.dp)) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable { regionMenuOpen = true }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isMyLocation) "\uD83D\uDCCD MY LOCATION" else "${currentDest?.name}, ${currentDest?.state}",
                            fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 11.sp,
                            color = if (isMyLocation) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                        )
                    }
                    DropdownMenu(
                        expanded = regionMenuOpen,
                        onDismissRequest = { regionMenuOpen = false },
                        modifier = Modifier.heightIn(max = 360.dp)
                    ) {
                        DropdownMenuItem(text = { Text("\uD83D\uDCCD MY LOCATION") }, onClick = {
                            selectedDestId = "MY_LOCATION"; regionMenuOpen = false; locate()
                        })
                        allDestinations.forEach { dest ->
                            DropdownMenuItem(text = { Text("${dest.name}, ${dest.state}") }, onClick = {
                                selectedDestId = dest.id; regionMenuOpen = false
                            })
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Strict Offline Map Banner (shown only when opened with offline isolation)
                if (offlineOnly) {
                    val destDownloaded = !isMyLocation && currentDest != null && downloadedPackIds.contains(currentDest.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (destDownloaded) androidx.compose.ui.graphics.Color(0xFF10B981).copy(alpha = 0.1f)
                                else androidx.compose.ui.graphics.Color(0xFFF59E0B).copy(alpha = 0.1f)
                            )
                            .border(
                                1.dp,
                                if (destDownloaded) androidx.compose.ui.graphics.Color(0xFF10B981).copy(alpha = 0.3f) else androidx.compose.ui.graphics.Color(0xFFF59E0B).copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                if (destDownloaded) Icons.Filled.MyLocation else Icons.Filled.LocationOff,
                                contentDescription = null,
                                tint = if (destDownloaded) androidx.compose.ui.graphics.Color(0xFF059669) else androidx.compose.ui.graphics.Color(0xFFB45309),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (destDownloaded) "Pre-Cached Offline Map: rendering saved OpenStreetMap tiles for ${currentDest?.name}." else "Live map tiles are unavailable offline for this region. Download this destination's pack for cached tiles.",
                                fontFamily = JetBrainsMono, fontSize = 10.sp,
                                color = if (destDownloaded) androidx.compose.ui.graphics.Color(0xFF047857) else androidx.compose.ui.graphics.Color(0xFF92400E),
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                        Text(
                            text = if (destDownloaded) "CACHED" else "LIVE ONLY",
                            fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 9.sp,
                            color = if (destDownloaded) androidx.compose.ui.graphics.Color(0xFF047857) else androidx.compose.ui.graphics.Color(0xFF92400E),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (destDownloaded) androidx.compose.ui.graphics.Color(0xFF10B981).copy(alpha = 0.2f) else androidx.compose.ui.graphics.Color(0xFFF59E0B).copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // GPS status banner
                if (isMyLocation) {
                    Box(modifier = Modifier.padding(top = 12.dp)) {
                        when {
                            isLocating -> StatusBanner(
                                icon = Icons.Filled.Sync,
                                text = "Acquiring your live GPS coordinates...",
                                bg = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                fg = MaterialTheme.colorScheme.primary
                            )
                            locationError != null -> Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(androidx.compose.ui.graphics.Color(0xFFEF4444).copy(alpha = 0.1f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.LocationOff, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFFDC2626), modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        locationError ?: "GPS not available",
                                        fontFamily = JetBrainsMono,
                                        fontSize = 11.sp,
                                        color = androidx.compose.ui.graphics.Color(0xFFDC2626),
                                        maxLines = 2
                                    )
                                }
                                Text(
                                    "RETRY GPS", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                                    color = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(androidx.compose.ui.graphics.Color(0xFFDC2626))
                                        .clickable { locate() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            userCoords != null -> Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(androidx.compose.ui.graphics.Color(0xFF10B981).copy(alpha = 0.1f))
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.MyLocation, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF10B981), modifier = Modifier.size(15.dp))
                                    val (lat, lng) = userCoords!!
                                    Text(
                                        "  Your GPS: %.4f\u00B0N, %.4f\u00B0E".format(lat, lng),
                                        fontFamily = JetBrainsMono, fontSize = 11.sp, color = androidx.compose.ui.graphics.Color(0xFF047857)
                                    )
                                }
                                nearest?.let { (dest, dist) ->
                                    Text(
                                        "Nearest Gem: ${dest.name} (~${dist.toInt()} km)",
                                        fontFamily = JetBrainsMono, fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Map
                MapViewer(
                    markers = markers,
                    userLocation = if (isMyLocation) userCoords else null,
                    onMarkerClick = { },
                    height = 340.dp,
                    focusTarget = focusTarget,
                    offlineOnly = offlineOnly,
                    modifier = Modifier.padding(top = 16.dp)
                )

                // Sub-category tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val allCount = (currentDest?.places?.size ?: 0) + (currentDest?.localFoods?.size ?: 0) + (currentDest?.localExperiences?.size ?: 0)
                    SectionTab("All", allCount, section == MapSection.ALL, MaterialTheme.colorScheme.tertiary) { section = MapSection.ALL }
                    SectionTab("Places & Gems", currentDest?.places?.size ?: 0, section == MapSection.PLACES, MaterialTheme.colorScheme.primary) { section = MapSection.PLACES }
                    SectionTab("Local Food", currentDest?.localFoods?.size ?: 0, section == MapSection.FOOD, androidx.compose.ui.graphics.Color(0xFFF59E0B)) { section = MapSection.FOOD }
                    SectionTab("Experiences", currentDest?.localExperiences?.size ?: 0, section == MapSection.EXPERIENCES, androidx.compose.ui.graphics.Color(0xFFA855F7)) { section = MapSection.EXPERIENCES }
                }
                Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp).clip(RoundedCornerShape(1.dp)).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))) {
                    Box(Modifier.fillMaxWidth().padding(1.dp))
                }

                currentDest?.let { dest ->
                    Row(
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
                            .clickable { onDestinationSelected(dest.id) }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Guide to ${dest.name}", fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp).padding(start = 4.dp))
                    }
                }
            }
        }

        val dest = currentDest
        if (dest != null) {
            when (section) {
                MapSection.ALL -> {
                    items(dest.places, key = { "all_place_${it.id}" }) { p ->
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            PlaceListCard(
                                area = areaLabel,
                                name = p.name,
                                badge = if (p.isSecretGem) "GEM" else p.category,
                                description = p.description,
                                meta = p.estimatedCost,
                                accent = MaterialTheme.colorScheme.primary,
                                imageUrl = p.image,
                                onFocusMap = p.coordinates?.let { c -> { focusTarget = c.lat to c.lng } }
                            )
                        }
                    }
                    items(dest.localFoods, key = { "all_food_${it.id ?: it.name}" }) { f ->
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            PlaceListCard(
                                area = areaLabel,
                                name = f.name,
                                badge = null,
                                description = f.description,
                                meta = null,
                                accent = androidx.compose.ui.graphics.Color(0xFFB45309),
                                imageUrl = f.image,
                                onFocusMap = (f.coordinates ?: dest.coordinates).let { c -> { focusTarget = c.lat to c.lng } }
                            )
                        }
                    }
                    items(dest.localExperiences, key = { "all_exp_${it.id}" }) { e ->
                        Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            PlaceListCard(
                                area = areaLabel,
                                name = e.title,
                                badge = e.category,
                                description = e.description,
                                meta = e.cost,
                                accent = androidx.compose.ui.graphics.Color(0xFF7E22CE),
                                imageUrl = e.image,
                                onFocusMap = (e.coordinates ?: dest.coordinates).let { c -> { focusTarget = c.lat to c.lng } }
                            )
                        }
                    }
                }
                MapSection.PLACES -> items(dest.places, key = { it.id }) { p ->
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        PlaceListCard(
                            area = areaLabel,
                            name = p.name,
                            badge = if (p.isSecretGem) "GEM" else p.category,
                            description = p.description,
                            meta = p.estimatedCost,
                            accent = MaterialTheme.colorScheme.primary,
                            imageUrl = p.image,
                            onFocusMap = p.coordinates?.let { c -> { focusTarget = c.lat to c.lng } }
                        )
                    }
                }
                MapSection.FOOD -> items(dest.localFoods, key = { it.id ?: it.name }) { f ->
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        PlaceListCard(
                            area = areaLabel,
                            name = f.name,
                            badge = null,
                            description = f.description,
                            meta = null,
                            accent = androidx.compose.ui.graphics.Color(0xFFB45309),
                            imageUrl = f.image,
                            onFocusMap = (f.coordinates ?: dest.coordinates).let { c -> { focusTarget = c.lat to c.lng } }
                        )
                    }
                }
                MapSection.EXPERIENCES -> items(dest.localExperiences, key = { it.id }) { e ->
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        PlaceListCard(
                            area = areaLabel,
                            name = e.title,
                            badge = e.category,
                            description = e.description,
                            meta = e.cost,
                            accent = androidx.compose.ui.graphics.Color(0xFF7E22CE),
                            imageUrl = e.image,
                            onFocusMap = (e.coordinates ?: dest.coordinates).let { c -> { focusTarget = c.lat to c.lng } }
                        )
                    }
                }
            }
        }

        item { Box(Modifier.padding(bottom = 24.dp)) }
    }
}

@Composable
private fun StatusBanner(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = fg)
        Text("  $text", fontFamily = JetBrainsMono, fontSize = 11.sp, color = fg)
    }
}

@Composable
private fun SectionTab(label: String, count: Int, selected: Boolean, accent: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) accent.copy(alpha = 0.15f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            " $count", fontSize = 10.sp, fontWeight = FontWeight.Bold,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 4.dp)
                .clip(RoundedCornerShape(50))
                .background(if (selected) accent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 6.dp, vertical = 1.dp)
        )
    }
}

@Composable
private fun PlaceListCard(
    area: String? = null,
    name: String,
    badge: String? = null,
    description: String,
    meta: String? = null,
    accent: androidx.compose.ui.graphics.Color,
    imageUrl: String? = null,
    onFocusMap: (() -> Unit)? = null
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    ) {
        if (!imageUrl.isNullOrEmpty()) {
            var loadFailed by remember(imageUrl) { mutableStateOf(false) }
            if (!loadFailed) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    onError = { loadFailed = true }
                )
            } else {
                `in`.hiddenindia.app.ui.components.ThemedImagePlaceholder(
                    query = name,
                    category = badge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    isShimmering = false
                )
            }
        } else {
            // No hardcoded image — try a real, verified Wikipedia photo instead of a
            // generic fallback (this list previously fell back to the destination's own
            // cover photo for places lacking one, which is why they repeated).
            `in`.hiddenindia.app.ui.components.WikiVerifiedImage(
                query = name,
                category = badge,
                area = area,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            )
        }
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(name, fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = accent, maxLines = 1, modifier = Modifier.weight(1f))
                if (!badge.isNullOrBlank()) {
                    Text(
                        badge, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 9.sp,
                        color = accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(accent.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, modifier = Modifier.padding(top = 4.dp))
            Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp).clip(RoundedCornerShape(1.dp)).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))) {
                Box(Modifier.fillMaxWidth().padding(0.5.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!meta.isNullOrBlank()) {
                    Text(
                        text = meta,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accent.copy(alpha = 0.12f))
                        .clickable(enabled = onFocusMap != null) { onFocusMap?.invoke() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = accent, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Focus Map",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = accent,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
