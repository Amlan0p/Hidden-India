package `in`.hiddenindia.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.EmergencyShare
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.hiddenindia.app.BuildConfig
import `in`.hiddenindia.app.data.FreshLocationProvider
import java.util.Locale
import androidx.core.content.ContextCompat
import `in`.hiddenindia.app.data.Destination
import `in`.hiddenindia.app.data.EmergencyInfo
import `in`.hiddenindia.app.data.GeoRepository
import `in`.hiddenindia.app.data.GeoUtils
import `in`.hiddenindia.app.network.ApiResult
import `in`.hiddenindia.app.network.NearestEmergencyResponse
import `in`.hiddenindia.app.ui.theme.JetBrainsMono
import `in`.hiddenindia.app.ui.theme.PlayfairDisplay
import `in`.hiddenindia.app.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

private enum class LocationMode { GPS, MANUAL }

private const val SOS_TAG = "EmergencySOS"

/** Debug-only logging. GPS coordinates are rounded to ~100 m so logs don't pin an exact position. */
private fun sosLog(message: String) {
    if (BuildConfig.DEBUG) Log.d(SOS_TAG, message)
}

private fun roundedCoord(value: Double): String = String.format(Locale.US, "%.3f", value)
private val PAN_INDIA_DEFAULTS = EmergencyInfo(
    hospitalName = "Nearest Government Hospital",
    hospitalAddress = "Ask any local resident or hotel staff for directions",
    policeStationName = "Nearest Police Station",
    policeAddress = "Dial 112 to be connected to the nearest control room"
)

/**
 * Port of web src/pages/EmergencySOSPage.tsx — previously just a placeholder stub. All the
 * per-destination emergency data (hospital/police names, numbers, addresses) already
 * existed in Destination.emergencyInfo; this screen was simply never built to use it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencySOSScreen(onBack: () -> Unit, viewModel: MainViewModel? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allDestinations = (viewModel?.allDestinations?.collectAsState()?.value) ?: emptyList()
    val geoRepository = remember { GeoRepository(context.applicationContext) }
    var liveFacilities by remember { mutableStateOf<NearestEmergencyResponse?>(null) }

    var mode by remember { mutableStateOf(LocationMode.GPS) }
    var isLocating by remember { mutableStateOf(false) }
    var geoError by remember { mutableStateOf<String?>(null) }
    var geoLookupFailed by remember { mutableStateOf(false) }
    var nearest by remember { mutableStateOf<Pair<Destination, Double>?>(null) }
    var userCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    // Incremented on every GPS acquisition so a slow, superseded request can never write its result.
    var gpsRequestId by remember { mutableStateOf(0) }

    var manualDest by remember { mutableStateOf<Destination?>(null) }
    var searchFilter by remember { mutableStateOf("") }
    var manualMenuOpen by remember { mutableStateOf(false) }
    var isLookingUpManual by remember { mutableStateOf(false) }
    var manualLookupFailed by remember { mutableStateOf(false) }

    // Manual mode: find the hospital and police station nearest to the CHOSEN place's
    // coordinates (live OpenStreetMap lookup), not a canned per-destination entry.
    LaunchedEffect(manualDest?.id, mode) {
        val d = manualDest
        if (mode != LocationMode.MANUAL || d == null) return@LaunchedEffect
        isLookingUpManual = true
        manualLookupFailed = false
        liveFacilities = null
        // Manual mode always uses the place the user selected, never a GPS fix.
        sosLog("SOS LOCATION:\nmode = manual")
        sosLog("SOS LOCATION:\nlatitude = ${d.coordinates.lat}")
        sosLog("SOS LOCATION:\nlongitude = ${d.coordinates.lng}")
        sosLog("SOS LOCATION:\nsource = manual selection (${d.name})")
        val result = if (viewModel != null) {
            viewModel.getNearestEmergencyFacilities(d.coordinates.lat, d.coordinates.lng)
        } else {
            geoRepository.getNearestEmergencyFacilities(d.coordinates.lat, d.coordinates.lng)
        }
        when (result) {
            is ApiResult.Success -> {
                liveFacilities = result.data
                manualLookupFailed = false
            }
            is ApiResult.Failure -> {
                liveFacilities = null
                manualLookupFailed = true
            }
        }
        isLookingUpManual = false
    }

    fun acquireGps() {
        // Every (re)acquisition starts clean: no coordinates, nearest destination or facility
        // list from an earlier fix (or from manual mode) can be shown or reused.
        gpsRequestId += 1
        val requestId = gpsRequestId
        userCoords = null
        nearest = null
        liveFacilities = null
        geoLookupFailed = false
        geoError = null

        sosLog("SOS LOCATION:\nmode = GPS")
        if (!FreshLocationProvider.hasLocationPermission(context)) {
            sosLog("SOS LOCATION:\nfresh location obtained = false (permission not granted)")
            isLocating = false
            geoError = "Location permission not granted. Please select your current city manually below or use National Emergency Hotlines."
            mode = LocationMode.MANUAL
            return
        }
        isLocating = true
        scope.launch {
            try {
                // A NEW fix is requested from the device. getLastKnownLocation() is not used.
                val fix = FreshLocationProvider.getFreshLocation(context)
                // Superseded by a newer request, or the user switched to manual selection while
                // we were waiting: drop this result so it can't overwrite anything.
                if (requestId != gpsRequestId || mode != LocationMode.GPS) {
                    sosLog("SOS LOCATION:\nfresh location result discarded (superseded or mode changed)")
                    return@launch
                }

                if (fix == null) {
                    sosLog("SOS LOCATION:\nfresh location obtained = false")
                    geoError = "Real-time GPS location could not be detected. Please select your current city manually below or use National Emergency Hotlines."
                    mode = LocationMode.MANUAL
                } else {
                    sosLog("SOS LOCATION:\nfresh location obtained = true")
                    sosLog("SOS LOCATION:\nlatitude = ${roundedCoord(fix.latitude)}")
                    sosLog("SOS LOCATION:\nlongitude = ${roundedCoord(fix.longitude)}")
                    sosLog("SOS LOCATION:\nsource = current GPS (provider=${fix.provider}, age=${fix.ageMs / 1000}s, accuracy=${fix.accuracyMeters?.toInt() ?: -1}m)")

                    userCoords = fix.latitude to fix.longitude
                    if (allDestinations.isNotEmpty()) {
                        nearest = GeoUtils.findNearestDestination(fix.latitude, fix.longitude, allDestinations)
                    }
                    // Real nearest hospital/police lookup from the CURRENT coordinates
                    // (direct OSM first, Render fallback inside GeoRepository, same lat/lng).
                    val result = if (viewModel != null) {
                        viewModel.getNearestEmergencyFacilities(fix.latitude, fix.longitude)
                    } else {
                        geoRepository.getNearestEmergencyFacilities(fix.latitude, fix.longitude)
                    }
                    if (requestId == gpsRequestId && mode == LocationMode.GPS) {
                        when (result) {
                            is ApiResult.Success -> {
                                liveFacilities = result.data
                                geoLookupFailed = false
                            }
                            is ApiResult.Failure -> {
                                liveFacilities = null
                                geoLookupFailed = true
                            }
                        }
                    }
                }
            } catch (e: SecurityException) {
                if (requestId == gpsRequestId) {
                    geoError = "Location permission not granted. Please select your current city manually below."
                    mode = LocationMode.MANUAL
                }
            } finally {
                if (requestId == gpsRequestId) isLocating = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        // Don't override a manual selection the user made while the permission dialog was open.
        if (mode == LocationMode.GPS) acquireGps()
    }
    LaunchedEffect(Unit) { permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }

    val activeDest = if (mode == LocationMode.GPS) nearest?.first else manualDest
    val liveHasLoaded = liveFacilities != null
    val info = when {
        // Live nearest facilities (GPS coordinates, or the manually chosen place's coordinates).
        liveHasLoaded && (mode == LocationMode.GPS || (mode == LocationMode.MANUAL && manualDest != null)) -> {
            val h = liveFacilities?.hospital
            val p = liveFacilities?.police
            EmergencyInfo(
                hospitalName = h?.let { "${it.name} (~${it.distanceKm} km away)" } ?: "No named hospital found nearby within range — dial 108",
                hospitalAddress = h?.address ?: "Address not available for this facility",
                hospitalNumber = h?.phone ?: "108",
                policeStationName = p?.let { "${it.name} (~${it.distanceKm} km away)" } ?: "No named police station found nearby within range — dial 112",
                policeAddress = p?.address ?: "Address not available for this facility",
                policeNumber = p?.phone ?: "112"
            )
        }
        mode == LocationMode.MANUAL && manualDest != null && isLookingUpManual -> EmergencyInfo(
            hospitalName = "Looking up the nearest hospital…",
            hospitalAddress = "Dial 108 for immediate ambulance dispatch",
            policeStationName = "Looking up the nearest police station…",
            policeAddress = "Dial 112 for immediate police dispatch"
        )
        mode == LocationMode.MANUAL && manualDest != null && manualLookupFailed -> EmergencyInfo(
            hospitalName = "Couldn't load nearby hospitals (check your connection) — dial 108",
            hospitalAddress = "Ambulance dispatch is free and works nationwide",
            policeStationName = "Couldn't load nearby police stations — dial 112",
            policeAddress = "National emergency number works nationwide"
        )
        mode == LocationMode.GPS && isLocating -> EmergencyInfo(
            hospitalName = "Looking up the nearest verified hospital…",
            hospitalAddress = "Dial 108 for immediate ambulance dispatch to your GPS location",
            policeStationName = "Looking up the nearest verified police station…",
            policeAddress = "Dial 112 for immediate police dispatch to your GPS location"
        )
        mode == LocationMode.GPS && nearest != null && geoLookupFailed -> EmergencyInfo(
            hospitalName = "Couldn't load nearby hospitals (check your connection) — dial 108",
            hospitalAddress = "Ambulance dispatch is free and works nationwide",
            policeStationName = "Couldn't load nearby police stations — dial 112",
            policeAddress = "National emergency number works nationwide"
        )
        mode == LocationMode.GPS && nearest != null -> EmergencyInfo(
            hospitalName = "Looking up the nearest verified hospital…",
            hospitalAddress = "Dial 108 for immediate ambulance dispatch to your GPS location",
            policeStationName = "Looking up the nearest verified police station…",
            policeAddress = "Dial 112 for immediate police dispatch to your GPS location"
        )
        else -> PAN_INDIA_DEFAULTS
    }
    val locationTitle = activeDest?.let { "${it.name}, ${it.state}" } ?: "National Emergency Services (Pan-India)"
    val sourceLabel = when {
        mode == LocationMode.GPS && nearest != null -> "\uD83D\uDCCD GPS Located"
        mode == LocationMode.MANUAL && manualDest != null -> "\uD83D\uDCDD User Selected"
        else -> "\uD83C\uDDEE\uD83C\uDDF3 Pan-India"
    }

    val filteredDestinations = remember(allDestinations, searchFilter) {
        if (searchFilter.isBlank()) allDestinations
        else allDestinations.filter {
            it.name.contains(searchFilter, true) || it.state.contains(searchFilter, true) || it.region.contains(searchFilter, true)
        }
    }

    fun dial(number: String) {
        if (number.isBlank()) return
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentPadding = PaddingValues(16.dp)) {
        item {
            // Top Navigation Bar with Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("btn_sos_back")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "EMERGENCY SAFETY & SOS",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    color = Color(0xFFDC2626)
                )
            }

            // PRIMARY HIGHLIGHTED IMMEDIATE HELP BUTTON (The main button in SOS tab)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFDC2626), Color(0xFFB91C1C), Color(0xFF991B1B))
                        )
                    )
                    .border(2.dp, Color(0xFFFCA5A5), RoundedCornerShape(20.dp))
                    .clickable {
                        Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show()
                    }
                    .padding(vertical = 18.dp, horizontal = 20.dp)
                    .testTag("btn_immediate_help"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "IMMEDIATE HELP",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 1.5.sp,
                        color = Color.White
                    )
                }
                Text(
                    text = "TAP FOR RAPID EMERGENCY PROTOCOL",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    letterSpacing = 0.8.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // SOS Header Banner
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFDC2626).copy(alpha = 0.08f))
                    .border(1.dp, Color(0xFFDC2626).copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFDC2626).copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.EmergencyShare, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(30.dp))
                }
                Text("24X7 EMERGENCY ASSISTANCE", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp, color = Color(0xFFDC2626), modifier = Modifier.padding(top = 8.dp))
                Text("Tourist Safety & SOS Mode", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 2.dp))
                Text(
                    "Immediate access to emergency services, police, hospital hotlines, and your real-time GPS coordinates.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 6.dp)
                )

                Row(modifier = Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Color(0xFFDC2626)).clickable { dial("1363") }.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("  1363 TOURIST", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                    }
                    Row(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.tertiary).clickable { dial("112") }.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.LocalPolice, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("  112 SOS", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode selector
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(4.dp)
            ) {
                ModeTab("Current Location (GPS)", Icons.Filled.GpsFixed, mode == LocationMode.GPS, Modifier.weight(1f)) {
                    mode = LocationMode.GPS
                    // Always take a fresh fix when returning to GPS mode; never reuse the old one.
                    if (!isLocating) acquireGps()
                }
                ModeTab("Choose Manually", Icons.Filled.EditLocationAlt, mode == LocationMode.MANUAL, Modifier.weight(1f)) {
                    mode = LocationMode.MANUAL
                    liveFacilities = null // never show a GPS-based result under a manual selection
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (mode == LocationMode.GPS) {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp)).padding(16.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.Top, modifier = Modifier.weight(1f)) {
                            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text("REAL-TIME GPS LOCATION", fontFamily = JetBrainsMono, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (isLocating) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                                        Text("  Acquiring GPS satellites...", fontFamily = JetBrainsMono, fontSize = 11.sp, color = Color(0xFFB45309))
                                    }
                                } else if (userCoords != null) {
                                    val (lat, lng) = userCoords!!
                                    Text(activeDest?.let { "${it.name}, ${it.state}" } ?: "Location acquired", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("%.4f\u00B0 N, %.4f\u00B0 E".format(lat, lng), fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.tertiary)
                                    nearest?.let { (dest, dist) ->
                                        Text("Nearest verified heritage hub: ${dest.name} (~${dist.toInt()} km)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                                    }
                                } else {
                                    Text("GPS coordinates not yet acquired.", fontSize = 12.sp, color = Color(0xFFB45309))
                                    Text("Tap refresh to request device location permission.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Box(
                            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable(enabled = !isLocating) { acquireGps() },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Filled.Refresh, contentDescription = "Refresh GPS", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp)) }
                    }

                    geoError?.let { err ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF59E0B).copy(alpha = 0.1f)).padding(10.dp)
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(err, fontSize = 11.sp, color = Color(0xFF92400E))
                                Text(
                                    "Choose your location from the city list \u2192", fontFamily = JetBrainsMono, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp).clickable { mode = LocationMode.MANUAL }
                                )
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp)).padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationCity, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text("  Select Your Current City / District", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        BasicTextField(value = searchFilter, onValueChange = { searchFilter = it }, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface))
                        if (searchFilter.isEmpty()) Text("Search by city, district or state...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box {
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.background)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(12.dp)).clickable { manualMenuOpen = true }.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                manualDest?.let { "${it.name}, ${it.state} (${it.region})" } ?: "-- National Emergency Services (Pan-India) --",
                                fontFamily = JetBrainsMono, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = manualMenuOpen,
                            onDismissRequest = { manualMenuOpen = false },
                            modifier = Modifier.heightIn(max = 360.dp)
                        ) {
                            DropdownMenuItem(text = { Text("-- National Emergency Services (Pan-India) --") }, onClick = { manualDest = null; manualMenuOpen = false })
                            filteredDestinations.forEach { dest ->
                                DropdownMenuItem(text = { Text("${dest.name}, ${dest.state} (${dest.region})") }, onClick = { manualDest = dest; manualMenuOpen = false })
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    Text(
                        if (manualDest != null) "Local emergency contacts for your selected city are loaded below." else "Select a city above for local hospital & police numbers, or use the National Hotlines below.",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Shield, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(15.dp))
                    Text("  RESPONDERS FOR: ", fontFamily = JetBrainsMono, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(locationTitle, fontFamily = PlayfairDisplay, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Text(
                sourceLabel, fontFamily = JetBrainsMono, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 8.dp, vertical = 3.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            EmergencyContactCard(Icons.Filled.LocalHospital, Color(0xFFDC2626), "HOSPITAL / EMERGENCY HEALTHCARE", info.hospitalName, info.hospitalAddress, info.hospitalNumber) { dial(info.hospitalNumber) }
            Spacer(modifier = Modifier.height(10.dp))
            EmergencyContactCard(Icons.Filled.LocalPolice, MaterialTheme.colorScheme.tertiary, "POLICE STATION / CONTROL ROOM", info.policeStationName, info.policeAddress, info.policeNumber) { dial(info.policeNumber) }

            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HotlineTile("National SOS", "112", "All Emergency", Modifier.weight(1f)) { dial("112") }
                HotlineTile("Ambulance", info.ambulanceNumber, "24x7 Medical", Modifier.weight(1f)) { dial(info.ambulanceNumber) }
                HotlineTile("Women Safety", info.womenHelpline, "Helpline", Modifier.weight(1f)) { dial(info.womenHelpline) }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)).padding(14.dp)
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text(
                    "Privacy Protection: Device GPS coordinates are used exclusively on-device to pinpoint local emergency responders. They are never recorded or used to alter your travel browsing context.",
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ModeTab(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Text(
            "  $label", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 10.sp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmergencyContactCard(icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Color, label: String, name: String, address: String, number: String, onCall: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp)).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(13.dp))
                Text("  $label", fontFamily = JetBrainsMono, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = accent)
            }
            Text(name, fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 2.dp))
            Text(address, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(accent.copy(alpha = 0.15f)).clickable(onClick = onCall),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Call, contentDescription = "Call", tint = accent, modifier = Modifier.size(20.dp)) }
    }
}

@Composable
private fun HotlineTile(label: String, number: String, subtitle: String, modifier: Modifier = Modifier, onCall: () -> Unit) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp)).clickable(onClick = onCall).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontFamily = JetBrainsMono, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text(number, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 2.dp))
        Text(subtitle, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
    }
}
