package `in`.hiddenindia.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.hiddenindia.app.ui.theme.HiddenIndiaTextStyles
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.sp

/**
 * Port of the "Filter by State:" / "Filter by Category:" blocks in web src/pages/
 * DiscoverPage.tsx — a small mono uppercase label above a horizontally-scrolling row of
 * rounded-full chips, active chip filled with [activeColor].
 */
@Composable
fun FilterSection(
    label: String,
    items: List<String>,
    selectedItem: String,
    activeColor: Color,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = HiddenIndiaTextStyles.labelCaps,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                val isSelected = item.equals(selectedItem, ignoreCase = true)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .then(
                            if (isSelected) Modifier.background(activeColor)
                            else Modifier
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(50))
                        )
                        .clickable { onItemSelected(item) }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    Text(
                        text = item,
                        style = HiddenIndiaTextStyles.labelCaps.copy(letterSpacing = 0.sp),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) (if (isDarkTheme) Color.Black else Color.White) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
