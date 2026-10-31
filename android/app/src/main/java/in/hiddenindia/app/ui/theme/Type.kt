package `in`.hiddenindia.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Material3 role scale, rebuilt against the web app's actual fonts and Tailwind's default
// type scale (the web app never overrides Tailwind's font-size utilities, only font-family
// via .font-serif / h1-h6 -> Playfair Display, and .font-mono -> JetBrains Mono; everything
// else stays on the body sans, Inter).
//
// Material3's 15-role scale doesn't cover every one-off text treatment the web app uses
// (e.g. the uppercase mono nav labels). Screen-level composables should reach for
// HiddenIndiaTextStyles below for anything that isn't a "standard" heading/body/label.

val Typography = Typography(
    displayLarge = TextStyle(fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 48.sp, lineHeight = 52.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp),
    displaySmall = TextStyle(fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),

    headlineLarge = TextStyle(fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = PlayfairDisplay, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
    headlineSmall = TextStyle(fontFamily = PlayfairDisplay, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),

    titleLarge = TextStyle(fontFamily = PlayfairDisplay, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),

    bodyLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),

    labelLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp)
)

// One-off styles for treatments Material3's roles don't name, ported straight from
// specific Tailwind class combos in the web app (e.g. Navbar.tsx's bottom-tab labels).
object HiddenIndiaTextStyles {
    // Navbar.tsx: className="font-mono text-[9px] uppercase tracking-wider"
    val navLabelCaps = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 9.sp,
        letterSpacing = 0.8.sp
    )

    // Generic uppercase mono "eyebrow" label used across cards/section headers
    val labelCaps = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 1.sp
    )

    // .font-display-lg hero headline (WelcomePage / LandingPage style treatments)
    val displayHero = TextStyle(
        fontFamily = PlayfairDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.5).sp
    )
}
