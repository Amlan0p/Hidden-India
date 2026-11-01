package `in`.hiddenindia.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.hiddenindia.app.R
import `in`.hiddenindia.app.ui.theme.DarkPrimary
import `in`.hiddenindia.app.ui.theme.DarkPrimaryDark
import `in`.hiddenindia.app.ui.theme.DarkBackground
import `in`.hiddenindia.app.ui.theme.JetBrainsMono
import `in`.hiddenindia.app.ui.theme.LightPrimary
import `in`.hiddenindia.app.ui.theme.PlayfairDisplay

enum class LogoSize(val box: androidx.compose.ui.unit.Dp, val corner: androidx.compose.ui.unit.Dp, val icon: androidx.compose.ui.unit.Dp, val title: androidx.compose.ui.unit.TextUnit, val tagline: androidx.compose.ui.unit.TextUnit) {
    // Values below are the mobile (non-`sm:`) breakpoint from Logo.tsx's SIZE_MAP.
    Sm(32.dp, 12.dp, 18.dp, 14.sp, 7.5.sp),
    Md(36.dp, 12.dp, 20.dp, 16.sp, 8.sp),
    Lg(44.dp, 16.dp, 24.dp, 18.sp, 9.sp)
}

/**
 * Port of web src/components/common/Logo.tsx — the brand mark used in the top bar and on
 * Welcome/Login/Signup. Gradient badge with the temple glyph, plus optional wordmark.
 */
@Composable
fun HiddenIndiaLogo(
    modifier: Modifier = Modifier,
    size: LogoSize = LogoSize.Sm,
    showText: Boolean = true,
    iconOnly: Boolean = false,
    darkTheme: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f
) {
    val gradient = if (darkTheme) {
        Brush.linearGradient(listOf(DarkPrimary, DarkPrimaryDark))
    } else {
        Brush.linearGradient(listOf(LightPrimary, Color(0xFFE6851F)))
    }
    val badge: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .size(size.box)
                .clip(RoundedCornerShape(size.corner))
                .background(gradient),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_temple_hindu),
                contentDescription = null,
                modifier = Modifier.size(size.icon),
                colorFilter = ColorFilter.tint(if (darkTheme) Color.Black else Color.White)
            )
        }
    }

    if (iconOnly || !showText) {
        Row(modifier = modifier) { badge() }
        return
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        badge()
        Column {
            Text(
                text = "Hidden India",
                fontFamily = PlayfairDisplay,
                fontWeight = FontWeight.Bold,
                fontSize = size.title,
                color = if (darkTheme) Color.White else Color(0xFF1B1C1C)
            )
            Text(
                text = "TRAVEL PLATFORM",
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = size.tagline,
                letterSpacing = 1.5.sp,
                color = if (darkTheme) DarkPrimary else LightPrimary
            )
        }
    }
}
