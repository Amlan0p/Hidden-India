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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import `in`.hiddenindia.app.ui.theme.JetBrainsMono
import `in`.hiddenindia.app.ui.theme.LightTertiary
import `in`.hiddenindia.app.ui.theme.PlayfairDisplay
import `in`.hiddenindia.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AMBER = Color(0xFFF59E0B)

/**
 * Port of web src/pages/OfflineVaultPage.tsx. The offline-mode toggle, sync status, and
 * download-timestamp display are now real (see MainViewModel.isOfflineMode/syncAllPacks and
 * DestinationRepository's new download-timestamp tracking) rather than static UI.
 *
 * The web page's "Culture Lexicon" and "Emergency Toolkit" shortcuts route to /language and
 * /emergency; this links to the existing TranslatorScreen and the EmergencySOSScreen stub.
 */
@Composable
fun OfflineVaultScreen(
    viewModel: MainViewModel,
    onOpenDestination: (String) -> Unit,
    onOpenMap: () -> Unit,
    onOpenTranslator: () -> Unit,
    onOpenEmergency: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allDestinations by viewModel.allDestinations.collectAsState()
    val downloadedPackIds by viewModel.downloadedPackIds.collectAsState()
    val downloadingPackIds by viewModel.downloadingPackIds.collectAsState()
    val isOfflineMode by viewModel.isOfflineMode.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val context = LocalContext.current

    val downloadedDestinations = allDestinations.filter { downloadedPackIds.contains(it.id) }
    val totalSizeMB = downloadedDestinations.sumOf { it.downloadSizeMB }
    val dateFmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        item {
            // Header + toggle
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text("EDGE STORAGE", fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.tertiary)
                    Text("Offline Vault", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Row(
                    modifier = Modifier
                        .testTag("btn_offline_mode_toggle")
                        .clip(RoundedCornerShape(50))
                        .background(if (isOfflineMode) AMBER.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
                        .border(if (isOfflineMode) 2.dp else 1.dp, if (isOfflineMode) AMBER.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(50))
                        .clickable { viewModel.setOfflineMode(!isOfflineMode) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(if (isOfflineMode) Icons.Filled.CloudOff else Icons.Filled.Wifi, contentDescription = null, tint = if (isOfflineMode) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                    Text(
                        text = if (isOfflineMode) "  Strict Offline" else "  Online Mode",
                        fontFamily = JetBrainsMono, fontSize = 11.sp,
                        fontWeight = if (isOfflineMode) FontWeight.Bold else FontWeight.Normal,
                        color = if (isOfflineMode) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isOfflineMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AMBER.copy(alpha = 0.12f))
                        .border(1.dp, AMBER.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.SignalWifiOff,
                        contentDescription = null,
                        tint = AMBER,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Offline Mode Active: Internet connectivity is completely blocked in this tab. All content is served strictly from local offline storage.",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = AMBER,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Sync status card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isSyncing) Icons.Filled.Sync else Icons.Filled.CloudDone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Column(modifier = Modifier.padding(start = 10.dp)) {
                        Text(if (isSyncing) "Syncing Vault..." else "Sync Complete", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        val formattedTotal = if (totalSizeMB % 1.0 == 0.0) totalSizeMB.toInt().toString() else String.format(Locale.US, "%.1f", totalSizeMB)
                        Text("${downloadedDestinations.size} Regions Cached • $formattedTotal MB Local", fontFamily = JetBrainsMono, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), CircleShape)
                        .clickable(enabled = !isSyncing) {
                            if (isOfflineMode) {
                                android.widget.Toast.makeText(context, "Offline Mode active in this tab: Internet sync blocked. Local storage verified.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            viewModel.syncAllPacks()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Icon(Icons.Filled.Sync, contentDescription = "Force refresh", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Downloaded regions header
            Text(
                "DOWNLOADED REGIONS (${downloadedDestinations.size})",
                fontFamily = JetBrainsMono, fontSize = 11.sp, letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 22.dp, bottom = 8.dp)
            )
        }

        if (downloadedDestinations.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.DownloadForOffline, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(40.dp))
                    Text("No offline destination packs downloaded yet.", fontFamily = JetBrainsMono, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp, bottom = 14.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable(onClick = onOpenMap)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text("BROWSE DESTINATIONS", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        } else {
            items(downloadedDestinations, key = { it.id }) { dest ->
                val ts = viewModel.getDownloadTimestamp(dest.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isDownloading = downloadingPackIds.contains(dest.id)
                    Row(modifier = Modifier.weight(1f).clickable { onOpenDestination(dest.id) }, verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dest.coverImage.isNotBlank()) {
                                val imageModel: ImageRequest = remember(dest.coverImage, isOfflineMode) {
                                    val builder = ImageRequest.Builder(context)
                                        .data(dest.coverImage)
                                        .crossfade(true)
                                    if (isOfflineMode) {
                                        builder.networkCachePolicy(CachePolicy.DISABLED)
                                    }
                                    builder.build()
                                }
                                AsyncImage(
                                    model = imageModel,
                                    contentDescription = dest.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Filled.FolderZip, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            }
                            if (isDownloading) {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(dest.name, fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                if (isDownloading) "Syncing images & map tiles..."
                                else "${dest.formattedDownloadSizeMB} MB" + (if (ts > 0) " • Saved ${dateFmt.format(Date(ts))}" else ""),
                                fontFamily = JetBrainsMono, fontSize = 10.sp,
                                color = if (isDownloading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onOpenDestination(dest.id) },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Filled.OpenInNew, contentDescription = "Open", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp)) }
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFDC2626).copy(alpha = 0.1f))
                                .clickable { viewModel.toggleDownloadPack(dest.id) },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp)) }
                    }
                }
            }
        }

        item {
            Text(
                "AVAILABLE OFFLINE CAPABILITIES",
                fontFamily = JetBrainsMono, fontSize = 11.sp, letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 20.dp, bottom = 10.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CapabilityCard(
                    icon = Icons.Filled.Terrain, title = "Topography", subtitle = "Leaflet Maps & Verified Pins",
                    accent = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f), onClick = onOpenMap
                )
                CapabilityCard(
                    icon = Icons.Filled.Translate, title = "Culture Lexicon", subtitle = "Local phrases & audio voice",
                    accent = LightTertiary, modifier = Modifier.weight(1f), onClick = onOpenTranslator
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenEmergency)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFDC2626).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Emergency, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text("Emergency Toolkit", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Hospitals, Police, SOS protocols, and offline guide contacts", fontFamily = JetBrainsMono, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CapabilityCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
        }
        Text(title, fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 10.dp))
        Text(subtitle, fontFamily = JetBrainsMono, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
    }
}
