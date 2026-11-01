package `in`.hiddenindia.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.OfflinePin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import `in`.hiddenindia.app.ui.theme.HiddenIndiaTextStyles

// 1:1 port of web src/components/layout/Navbar.tsx — same 6 tabs, same order, same labels.
sealed class Screen(val route: String, val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    // NOTE: route is "home", not "discover" — this mirrors web src/components/layout/
    // Navbar.tsx, where the tab labeled "Discover" links to '/' (HomePage), not '/discover'
    // (the separate search/filter results page, DiscoverScreen.kt, reached via the search
    // bar and matched under this same tab per Navbar's `match: ['/', '/discover']`).
    object Discover : Screen("home", "Discover", Icons.Filled.Explore, Icons.Outlined.Explore)
    object TripPlanner : Screen("planner", "Plan", Icons.Filled.EventNote, Icons.Outlined.EventNote)
    object AskAi : Screen("chat", "Ask AI", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome)
    object MapExplorer : Screen("map", "Map", Icons.Filled.Map, Icons.Outlined.Map)
    object Vault : Screen("vault", "Offline", Icons.Filled.OfflinePin, Icons.Outlined.OfflinePin)
    object Favorites : Screen("favorites", "Saved", Icons.Filled.Bookmark, Icons.Outlined.Bookmark)
}

private val NavItems = listOf(
    Screen.Discover,
    Screen.TripPlanner,
    Screen.AskAi,
    Screen.MapExplorer,
    Screen.Vault,
    Screen.Favorites
)

/**
 * Port of Navbar.tsx: fixed bottom dock, h-16, translucent surface with a hairline top
 * border, active tab in primary color with the mono uppercase label + "active-glow" tint.
 */
@Composable
fun AppBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    val baseRoute = currentRoute.substringBefore("?").substringBefore("/")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.98f))
            .border(width = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .height(64.dp)
            .padding(horizontal = 8.dp)
            .testTag("bottom_nav_bar"),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItems.forEach { screen ->
            val isSelected = baseRoute == screen.route ||
                (screen == Screen.Discover && baseRoute in listOf("home", "discover")) ||
                (screen == Screen.AskAi && baseRoute == "chat")
            val tint = if (isSelected) activeColor else inactiveColor
            Column(
                modifier = Modifier
                    .widthIn(min = 50.dp)
                    .selectable(selected = isSelected, onClick = { onNavigate(screen.route) })
                    .padding(vertical = 4.dp)
                    .testTag("nav_item_${screen.route}"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                    contentDescription = screen.title,
                    tint = tint,
                    modifier = Modifier
                )
                Text(
                    text = screen.title.uppercase(),
                    style = HiddenIndiaTextStyles.navLabelCaps,
                    color = tint
                )
            }
        }
    }
}
