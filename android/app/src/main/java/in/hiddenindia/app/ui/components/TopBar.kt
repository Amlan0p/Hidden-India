package `in`.hiddenindia.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.hiddenindia.app.ui.theme.HiddenIndiaTextStyles
import `in`.hiddenindia.app.ui.theme.JetBrainsMono
import `in`.hiddenindia.app.ui.theme.LightPrimary

import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import `in`.hiddenindia.app.data.Destination

/**
 * Port of web src/components/layout/Header.tsx (mobile width — the `xl:flex` desktop nav
 * links are hidden on a phone-sized viewport there too), so this mirrors what actually
 * renders on a phone: logo, compact search pill, SOS pill, theme toggle, avatar, and the
 * "Available Offline" status sub-strip.
 */
@Composable
fun HiddenIndiaTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onAvatarClick: () -> Unit,
    onSosClick: () -> Unit,
    onOfflineStripClick: () -> Unit,
    avatarSrc: String?,
    avatarName: String?,
    isOfflineMode: Boolean,
    onLogoClick: () -> Unit = {},
    destinations: List<Destination> = emptyList(),
    onDestinationClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val q = searchQuery.trim().lowercase()

    val suggestions: List<Destination> = remember(q, destinations) {
        if (q.isBlank() || destinations.isEmpty()) emptyList()
        else {
            destinations.filter { dest ->
                dest.name.lowercase().contains(q) ||
                dest.state.lowercase().contains(q) ||
                dest.places.any { it.name.lowercase().contains(q) }
            }.sortedWith(
                compareBy(
                    // 1. Destination name starts with query (e.g. typing 'a' shows 'Abhaneri' first)
                    { if (it.name.lowercase().startsWith(q)) 0 else 1 },
                    // 2. Any word in name starts with query
                    { if (it.name.lowercase().split(" ").any { w -> w.startsWith(q) }) 0 else 1 },
                    // 3. State starts with query
                    { if (it.state.lowercase().startsWith(q)) 0 else 1 },
                    // 4. Any attraction starts with query
                    { if (it.places.any { p -> p.name.lowercase().startsWith(q) }) 0 else 1 },
                    { it.name }
                )
            ).take(5)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.98f))
            .border(width = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onLogoClick)
            ) {
                HiddenIndiaLogo(size = LogoSize.Sm, showText = true, darkTheme = isDarkTheme)
            }

            // Compact interactive search pill
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Box(modifier = Modifier.weight(1f).padding(start = 6.dp)) {
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            focusManager.clearFocus()
                            onSearchSubmit()
                        }),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    )
                    if (searchQuery.isEmpty()) {
                        Text(
                            "Search places...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .clickable {
                                onSearchQueryChange("")
                                focusManager.clearFocus()
                            }
                    )
                }
            }

            // SOS pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFDC2626))
                    .clickable { onSosClick() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Filled.Emergency, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Text("SOS", color = Color.White, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }

            // Theme toggle
            Icon(
                imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                contentDescription = "Toggle theme",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onToggleTheme() }
            )

            // Avatar
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .clickable { onAvatarClick() }
                    .padding(1.dp)
            ) {
                UserAvatar(src = avatarSrc, name = avatarName, size = AvatarSize.Sm)
            }
        }

        // Live interactive suggestions dropdown when typing
        if (suggestions.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "MATCHING DESTINATIONS (${suggestions.size})",
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                suggestions.forEach { dest ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                focusManager.clearFocus()
                                onDestinationClick(dest.id)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = dest.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                            Text(
                                text = " • ${dest.state}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = dest.category,
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }

        // "Available Offline" sub-strip (Adaptive color: lighter orange with white text in light mode, orange with black text in dark mode)
        val bannerBg = if (isDarkTheme) Color(0xFFFF8C00) else LightPrimary
        val bannerFg = if (isDarkTheme) Color.Black else Color.White
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bannerBg)
                .clickable { onOfflineStripClick() }
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Filled.Circle,
                contentDescription = null,
                tint = bannerFg,
                modifier = Modifier.size(8.dp)
            )
            Text(
                text = if (isOfflineMode) "Strict Offline Test Active • Zero Network" else "Available Offline",
                style = HiddenIndiaTextStyles.labelCaps,
                color = bannerFg,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
