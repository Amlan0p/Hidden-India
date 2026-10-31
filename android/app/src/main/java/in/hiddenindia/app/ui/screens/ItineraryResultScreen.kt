package `in`.hiddenindia.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import `in`.hiddenindia.app.data.Destination
import `in`.hiddenindia.app.ui.components.MapMarker
import `in`.hiddenindia.app.ui.components.MapViewer
import `in`.hiddenindia.app.ui.theme.ForestGreen
import `in`.hiddenindia.app.ui.theme.HeritageGold
import `in`.hiddenindia.app.ui.theme.JetBrainsMono
import `in`.hiddenindia.app.ui.theme.TerracottaPrimary
import `in`.hiddenindia.app.ui.viewmodel.AuthFormState
import `in`.hiddenindia.app.ui.viewmodel.AuthViewModel
import `in`.hiddenindia.app.ui.viewmodel.MainViewModel

private fun iconForCategory(category: String?): ImageVector = when (category) {
    "Food" -> Icons.Default.LocalCafe
    "Heritage" -> Icons.Default.Castle
    "Nature" -> Icons.Default.Terrain
    "Art" -> Icons.Default.Palette
    else -> Icons.Default.Restaurant
}

/**
 * Port of web src/pages/ItineraryResultPage.tsx. Adds the pieces that were missing from the
 * native version: the Trip Parameters card, a real day-by-day tab switch (native previously
 * stacked every day at once instead of one at a time), Save-to-Offline-Vault, and the
 * "View on Map" modal — coordinates for each timeline item are resolved the same way as web
 * (matching activity/place names against the destination's places/food/experiences).
 */
@Composable
fun ItineraryResultScreen(
    viewModel: MainViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onViewDestination: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val itinerary by viewModel.currentItinerary.collectAsState()
    val context = LocalContext.current
    val savedItineraries by authViewModel.savedItineraries.collectAsState()
    val itineraryActionState by authViewModel.itineraryActionState.collectAsState()
    val allDestinations by viewModel.allDestinations.collectAsState()
    val downloadedPackIds by viewModel.downloadedPackIds.collectAsState()

    val plan = itinerary ?: return
    val isSaved = savedItineraries.any { it.id == plan.id }
    val isSaving = itineraryActionState is AuthFormState.Loading
    val isDownloaded = downloadedPackIds.contains(plan.destinationId)
    val destinationData: Destination? = remember(allDestinations, plan.destinationId) {
        allDestinations.find { it.id == plan.destinationId }
    }

    var selectedDay by remember(plan.id) { mutableStateOf(plan.dayPlans.firstOrNull()?.dayNumber ?: 1) }
    var showMapModal by remember { mutableStateOf(false) }
    val activeDayPlan = plan.dayPlans.find { it.dayNumber == selectedDay } ?: plan.dayPlans.firstOrNull()

    androidx.compose.runtime.LaunchedEffect(itineraryActionState) {
        val state = itineraryActionState
        if (state is AuthFormState.Error) {
            android.widget.Toast.makeText(context, state.message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_itinerary_back")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TerracottaPrimary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text("${plan.destinationName}, ${plan.state}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${plan.days}-Day Curated Journey • ${plan.budgetStyle}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { authViewModel.saveItineraryToAccount(plan) },
                        enabled = !isSaving && !isSaved,
                        modifier = Modifier.testTag("btn_itinerary_save")
                    ) {
                        if (isSaving) {
                            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = TerracottaPrimary)
                        } else {
                            Icon(
                                imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = if (isSaved) "Saved to your trips" else "Save to your trips",
                                tint = if (isSaved) HeritageGold else TerracottaPrimary
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.toggleDownloadPack(plan.destinationId) },
                        enabled = !isDownloaded,
                        modifier = Modifier.testTag("btn_itinerary_offline")
                    ) {
                        Icon(
                            if (isDownloaded) Icons.Default.Check else Icons.Default.OfflinePin,
                            contentDescription = if (isDownloaded) "Saved to Offline Vault" else "Save to Offline Vault",
                            tint = if (isDownloaded) ForestGreen else TerracottaPrimary
                        )
                    }
                    IconButton(onClick = {
                        val shareText = "Hidden India Itinerary for ${plan.destinationName} (${plan.days} Days):\n\n${plan.summary}\n\nTotal Estimated Cost: ${plan.totalCost}"
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Itinerary"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = TerracottaPrimary)
                    }
                }
            }
        }

        val actualDays = plan.dayPlans.size
        val requestedDays = plan.requestedDays

        val resolvedImages = remember(activeDayPlan, destinationData) {
            val usedImages = mutableSetOf<String>()
            val map = mutableMapOf<String, String?>()
            val town = destinationData?.name?.trim()?.lowercase() ?: ""
            activeDayPlan?.activities?.forEach { act ->
                val matchedPlace = destinationData?.places?.firstOrNull { place ->
                    matchesName(place.name, act.placeName, town) &&
                        place.image.isNotBlank() &&
                        !usedImages.contains(place.image)
                }?.image
                val matchedFood = if (matchedPlace == null) {
                    destinationData?.localFoods?.firstOrNull { food ->
                        matchesName(food.name, act.placeName, town) &&
                            !food.image.isNullOrBlank() &&
                            !usedImages.contains(food.image)
                    }?.image
                } else null
                val matchedExp = if (matchedPlace == null && matchedFood == null) {
                    destinationData?.localExperiences?.firstOrNull { exp ->
                        matchesName(exp.title, act.placeName, town) &&
                            !exp.image.isNullOrBlank() &&
                            !usedImages.contains(exp.image)
                    }?.image
                } else null

                val chosen = matchedPlace ?: matchedFood ?: matchedExp
                if (chosen != null) {
                    usedImages.add(chosen)
                    map[act.placeName] = chosen
                } else {
                    map[act.placeName] = null
                }
            }
            map
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Notice if requested days exceeds available content
            if (requestedDays > actualDays) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "${plan.destinationName} has content for nearly $actualDays day${if (actualDays > 1) "s" else ""}, so this screen shows content for only $actualDays day${if (actualDays > 1) "s" else ""}.",
                                fontFamily = JetBrainsMono,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Trip Parameters card (port of web's "Trip Parameters" grid — missing natively before)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = TerracottaPrimary, modifier = Modifier.size(18.dp))
                            Text("  Trip Parameters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TripParam("DESTINATION", "${plan.destinationName}, ${plan.state}", Modifier.weight(1f))
                            TripParam("DURATION", "$actualDays Days, ${if (actualDays > 1) actualDays - 1 else 0} Nights", Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TripParam("BUDGET", plan.budgetStyle, Modifier.weight(1f))
                            TripParam("INTERESTS", plan.interests.joinToString(", "), Modifier.weight(1f))
                        }
                    }
                }
            }

            // Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Surface(shape = RoundedCornerShape(8.dp), color = HeritageGold.copy(alpha = 0.2f)) {
                            Text("AI GENERATED PLAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TerracottaPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                        if (plan.totalCost.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Est: ${plan.totalCost}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(plan.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
                    }
                }
            }

            // Day tabs (web only shows one day at a time; native previously stacked all days)
            if (plan.dayPlans.size > 1) {
                item {
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        plan.dayPlans.forEach { dp ->
                            val isSelected = selectedDay == dp.dayNumber
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) TerracottaPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedDay = dp.dayNumber }
                                    .padding(horizontal = 16.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    "Day ${dp.dayNumber}", fontFamily = JetBrainsMono, fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            activeDayPlan?.let { day ->
                item {
                    Text(
                        "Day ${day.dayNumber}: ${day.theme}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                items(day.activities) { act ->
                    val placeImage = resolvedImages[act.placeName]

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(iconForCategory(null), contentDescription = null, tint = TerracottaPrimary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column {
                                if (!placeImage.isNullOrEmpty()) {
                                    var loadFailed by remember(placeImage) { mutableStateOf(false) }
                                    if (!loadFailed) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(placeImage)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = act.placeName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(120.dp)
                                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                                            onError = { loadFailed = true }
                                        )
                                    }
                                }
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Schedule, contentDescription = null, tint = TerracottaPrimary, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(act.timeSlot, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TerracottaPrimary)
                                        }
                                        Text(act.cost, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = ForestGreen)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(act.placeName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(act.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (act.insiderTip.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = HeritageGold, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(act.insiderTip, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { onViewDestination(plan.destinationId) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Explore ${plan.destinationName} Full Guide", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(TerracottaPrimary)
                        .clickable { showMapModal = true }
                        .padding(vertical = 14.dp)
                        .testTag("btn_view_itinerary_map"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Text("  View on Map", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showMapModal && destinationData != null && activeDayPlan != null) {
        val markers = remember(activeDayPlan, destinationData) {
            val town = destinationData.name.trim().lowercase()
            activeDayPlan.activities.mapIndexedNotNull { idx, act ->
                val coords = destinationData.places.find { matchesName(it.name, act.placeName, town) }?.coordinates
                    ?: destinationData.localFoods.find { matchesName(it.name, act.placeName, town) }?.coordinates
                    ?: destinationData.localExperiences.find { matchesName(it.title, act.placeName, town) }?.coordinates
                    ?: destinationData.coordinates
                MapMarker(id = "act_$idx", lat = coords.lat, lng = coords.lng, name = act.placeName, kind = "place")
            }
        }
        Dialog(onDismissRequest = { showMapModal = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${plan.destinationName} Route Map", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.clickable { showMapModal = false })
                }
                Spacer(modifier = Modifier.height(10.dp))
                MapViewer(markers = markers, userLocation = null, onMarkerClick = {}, height = 360.dp)
            }
        }
    }
}

private fun matchesName(a: String, b: String, town: String = ""): Boolean {
    val an = a.trim().lowercase()
    val bn = b.trim().lowercase()
    if (an == bn) return true
    if (town.isNotEmpty()) {
        if (an == town && bn != town) return false
        if (bn == town && an != town) return false
    }
    val stopWords = setOf("the", "of", "and", "in", "at", "to", "a", "an", "is", town)
    val aWords = an.split(Regex("[\\s,.-]+")).filter { it.length > 2 && it !in stopWords }
    val bWords = bn.split(Regex("[\\s,.-]+")).filter { it.length > 2 && it !in stopWords }
    if (aWords.isEmpty() || bWords.isEmpty()) return an == bn
    val intersection = aWords.intersect(bWords.toSet())
    return intersection.size >= minOf(aWords.size, bWords.size) || (intersection.isNotEmpty() && intersection.size >= 2)
}

@Composable
private fun TripParam(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontFamily = JetBrainsMono, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), letterSpacing = 0.5.sp)
        Text(value, fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.tertiary, maxLines = 1)
    }
}
