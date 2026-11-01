package `in`.hiddenindia.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.hiddenindia.app.ui.theme.JetBrainsMono

import `in`.hiddenindia.app.ui.theme.LocalHiddenIndiaIsDark

/**
 * Port of web src/components/common/FloatingAIButtons.tsx — the floating Translate +
 * "Ask The Place" pill that hovers above the bottom nav. On web this is scoped to the
 * Discover views ('/' and, before it was collapsed, '/discover'); the caller here should
 * only render this on the equivalent Android routes (Screen.Discover.route == "home", and
 * the literal "discover" search screen) and hide it everywhere else, same as web.
 */
@Composable
fun FloatingAiButtons(
    onOpenTranslator: () -> Unit,
    onOpenAskAi: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalHiddenIndiaIsDark.current
    Column(
        modifier = modifier.padding(end = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Language Assistant & Live Translation circular button
        Box2(
            isDark = isDark,
            onClick = onOpenTranslator,
            content = {
                Icon(
                    Icons.Filled.Translate,
                    contentDescription = "Translate & Language Assistant",
                    tint = if (isDark) Color(0xFFFF8C00) else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        )

        // 2. ASK THE PLACE floating pill button
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary)
                .border(
                    1.dp,
                    if (isDark) Color(0xFFE67E00).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.3f),
                    RoundedCornerShape(50)
                )
                .clickable(onClick = onOpenAskAi)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Filled.Psychology,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Text(
                "ASK THE PLACE",
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun Box2(isDark: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val bg = if (isDark) Color(0xFF141414) else Color(0xFF1B263B)
    val borderCol = if (isDark) Color(0xFFFF8C00).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.2f)
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, borderCol, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
