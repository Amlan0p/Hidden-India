package `in`.hiddenindia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import `in`.hiddenindia.app.data.Destination
import `in`.hiddenindia.app.ui.components.DestinationPickerSheet
import `in`.hiddenindia.app.ui.components.NumberedFieldLabel
import `in`.hiddenindia.app.ui.theme.JetBrainsMono
import `in`.hiddenindia.app.ui.theme.PlayfairDisplay
import `in`.hiddenindia.app.ui.viewmodel.MainViewModel

private val DAY_OPTIONS = listOf(1, 2, 3, 4, 5, 7)
private val BUDGET_OPTIONS = listOf(
    Triple("Budget", "Hostels & Local Dhabas", "₹1,500/day"),
    Triple("Moderate", "Heritage Havelis & Cafes", "₹3,000/day"),
    Triple("Comfortable", "Boutique Palaces & Taxis", "₹5,000/day")
)
private val INTEREST_OPTIONS = listOf("Heritage", "Culture", "Food", "Photography", "Stepwells", "Art & Craft", "Nature & Trails", "Spiritual")
private val TRAVEL_PACE_OPTIONS = listOf("Balanced", "Slow & Immersive", "Active Explorer")

/**
 * Port of web src/pages/TripPlannerPage.tsx. The dropdown popover became a bottom sheet
 * (see DestinationPickerSheet) since an absolutely-positioned web dropdown doesn't
 * translate to a phone; everything else mirrors the web form 1:1, including the numbered
 * step labels and card styling.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun TripPlannerScreen(
    viewModel: MainViewModel,
    preselectedDestination: Destination?,
    onItineraryGenerated: () -> Unit,
    onOpenDestination: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allDestinations by viewModel.allDestinations.collectAsState()
    val isGenerating by viewModel.isGeneratingTrip.collectAsState()
    val itineraryError by viewModel.itineraryError.collectAsState()
    val currentItinerary by viewModel.currentItinerary.collectAsState()
    var hasRequestedGeneration by remember { mutableStateOf(false) }

    LaunchedEffect(isGenerating) {
        if (hasRequestedGeneration && !isGenerating) {
            hasRequestedGeneration = false
            if (currentItinerary != null) onItineraryGenerated()
        }
    }

    var selectedDestination by remember(preselectedDestination, allDestinations) {
        mutableStateOf(preselectedDestination ?: allDestinations.firstOrNull())
    }
    var sheetOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var days by remember { mutableStateOf(3) }
    var budgetStyle by remember { mutableStateOf("Moderate") }
    var travelPace by remember { mutableStateOf("Balanced") } // TODO(backend): not yet sent to AiRepository.planTrip
    val selectedInterests = remember { mutableStateListOf("Heritage", "Culture", "Food") }

    val states = remember(allDestinations) { listOf("All") + allDestinations.map { it.state }.distinct().sorted() }
    val categories = remember(allDestinations) { listOf("All") + allDestinations.map { it.category }.distinct().sorted() }
    val curatedDays = remember(selectedDestination) {
        selectedDestination?.let { dest ->
            val spots = dest.places.size + dest.localFoods.size + dest.localExperiences.size
            maxOf(1, (spots + 1) / 3)
        } ?: 3
    }

    if (sheetOpen) {
        DestinationPickerSheet(
            destinations = allDestinations,
            selectedId = selectedDestination?.id,
            states = states,
            categories = categories,
            sheetState = sheetState,
            onDismiss = { sheetOpen = false },
            onSelect = { selectedDestination = it; sheetOpen = false }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 6.dp)
    ) {
        item {
            // Centered header
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
                Text(
                    text = "GROUNDED AI CURATION",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = "AI Trip Planner",
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = "Generate an authentic day-by-day journey tailored exclusively from verified local places and cultural dhabas across India.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            // Form card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Destination
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        NumberedFieldLabel(1, "Select Destination")
                        Text("${allDestinations.size} Available", fontFamily = JetBrainsMono, fontSize = 10.sp, color = MaterialTheme.colorScheme.tertiary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .clickable { sheetOpen = true }
                            .padding(14.dp)
                            .testTag("dropdown_destination_picker"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            AsyncImage(
                                model = selectedDestination?.listImage,
                                contentDescription = selectedDestination?.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                            )
                            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                                Text(
                                    selectedDestination?.name ?: "Select a destination",
                                    fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface, maxLines = 1
                                )
                                selectedDestination?.let {
                                    Text("${it.state} • ${it.region} India", fontFamily = JetBrainsMono, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Icon(Icons.Filled.ExpandMore, contentDescription = "Change", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    selectedDestination?.let { dest ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Planning for: ${dest.name}", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    dest.tagline.ifBlank { dest.description },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            Row(
                                modifier = Modifier.clickable { onOpenDestination(dest.id) }.padding(start = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Explore Lore", fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.tertiary)
                                Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp).padding(start = 4.dp))
                            }
                        }
                    }
                }

                // 2. Days
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        NumberedFieldLabel(2, "Number of Days")
                        Text("$days Days, ${if (days > 1) days - 1 else 0} Nights", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DAY_OPTIONS.forEach { num ->
                            val isSelected = days == num
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { days = num }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${num}D", fontFamily = JetBrainsMono, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (selectedDestination != null && days > curatedDays) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "${selectedDestination?.name} has verified content for nearly $curatedDays day(s).",
                                    fontFamily = JetBrainsMono,
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 3. Budget
                Column {
                    NumberedFieldLabel(3, "Budget Level")
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BUDGET_OPTIONS.forEach { (key, desc, price) ->
                            val isSelected = budgetStyle == key
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                    .clickable { budgetStyle = key }
                                    .padding(10.dp)
                            ) {
                                Text(key, fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(price, fontFamily = JetBrainsMono, fontSize = 9.sp, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                }

                // 4. Interests
                Column {
                    NumberedFieldLabel(4, "Key Interests")
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        INTEREST_OPTIONS.forEach { interest ->
                            val isSelected = selectedInterests.contains(interest)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(if (isSelected) 1.dp else 0.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (isSelected) 0.6f else 0f), RoundedCornerShape(50))
                                    .clickable {
                                        if (isSelected) {
                                            if (selectedInterests.size > 1) selectedInterests.remove(interest)
                                        } else selectedInterests.add(interest)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(interest, fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // 5. Travel Pace
                Column {
                    NumberedFieldLabel(5, "Travel Pace")
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TRAVEL_PACE_OPTIONS.forEach { pace ->
                            val isSelected = travelPace == pace
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(if (isSelected) 1.dp else 0.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (isSelected) 0.6f else 0f), RoundedCornerShape(12.dp))
                                    .clickable { travelPace = pace }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(pace, fontFamily = JetBrainsMono, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Submit
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(enabled = selectedDestination != null && !isGenerating) {
                            val dest = selectedDestination ?: return@clickable
                            hasRequestedGeneration = true
                            viewModel.generateItinerary(destination = dest, days = days, budget = budgetStyle, interests = selectedInterests.toList())
                        }
                        .padding(vertical = 16.dp)
                        .testTag("btn_generate_itinerary"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("  CURATING ${(selectedDestination?.name ?: "").uppercase()} ITINERARY…", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                        Text("  GENERATE ${(selectedDestination?.name ?: "").uppercase()} JOURNEY", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }

                if (itineraryError != null) {
                    Text(
                        text = itineraryError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag("txt_itinerary_error")
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}
