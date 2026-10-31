package `in`.hiddenindia.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import `in`.hiddenindia.app.data.Destination
import `in`.hiddenindia.app.ui.theme.JetBrainsMono
import `in`.hiddenindia.app.ui.theme.PlayfairDisplay

/**
 * Port of the destination dropdown popover in web src/pages/TripPlannerPage.tsx. A
 * dropdown-anchored popover doesn't translate to a phone, so this uses a bottom sheet —
 * same search box + state/category quick-filter row + scrollable option list content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationPickerSheet(
    destinations: List<Destination>,
    selectedId: String?,
    states: List<String>,
    categories: List<String>,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSelect: (Destination) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var stateFilter by remember { mutableStateOf("All") }
    var categoryFilter by remember { mutableStateOf("All") }

    val filtered = remember(destinations, query, stateFilter, categoryFilter) {
        destinations.filter { d ->
            val q = query.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                d.name.lowercase().contains(q) ||
                d.state.lowercase().contains(q) ||
                d.region.lowercase().contains(q) ||
                d.category.lowercase().contains(q)
            val matchesState = stateFilter == "All" || d.state == stateFilter
            val matchesCategory = categoryFilter == "All" || d.category == categoryFilter
            matchesQuery && matchesState && matchesCategory
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
            // Search box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Box(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    )
                    if (query.isEmpty()) {
                        Text("Type destination, state, or region…", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
            }

            Row(modifier = Modifier.padding(top = 10.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickFilterDropdown(label = "State", value = stateFilter, options = states, onSelect = { stateFilter = it }, modifier = Modifier.weight(1f))
                QuickFilterDropdown(label = "Theme", value = categoryFilter, options = categories, onSelect = { categoryFilter = it }, modifier = Modifier.weight(1f))
            }

            Text(
                "${filtered.size} found",
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                items(filtered, key = { it.id }) { dest ->
                    val isSelected = dest.id == selectedId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.background)
                            .clickable { onSelect(dest) }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            AsyncImage(
                                model = dest.coverImage,
                                contentDescription = dest.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                            )
                            Column(modifier = Modifier.padding(start = 10.dp)) {
                                Text(dest.name, fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("${dest.state} • ${dest.region}", fontFamily = JetBrainsMono, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (isSelected) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickFilterDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$label: $value", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        }
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 360.dp)
        ) {
            options.forEach { opt ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = { onSelect(opt); expanded = false }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
