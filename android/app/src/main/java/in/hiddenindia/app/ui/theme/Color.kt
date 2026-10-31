package `in`.hiddenindia.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 1:1 port of the CSS custom properties in the web app's src/index.css
// (:root for light, .dark for dark). Names mirror the CSS variable names so this can be
// diffed directly against index.css when the web theme changes.

// ---- Light (:root) ----
val LightSurface = Color(0xFFFBF9F8)                 // --color-surface
val LightOnSurface = Color(0xFF1B1C1C)                // --color-on-surface
val LightOnSurfaceVariant = Color(0xFF554336)         // --color-on-surface-variant
val LightPrimary = Color(0xFFFF9933)                  // --color-primary
val LightPrimaryDark = Color(0xFF8F4E00)              // --color-primary-dark
val LightPrimaryContainer = Color(0xFFFF9933)         // --color-primary-container
val LightOnPrimaryContainer = Color(0xFFFFFFFF)       // --color-on-primary-container
val LightTertiary = Color(0xFF31628D)                 // --color-tertiary
val LightTertiaryContainer = Color(0xFF88B6E5)        // --color-tertiary-container
val LightOnTertiaryContainer = Color(0xFF0D4771)      // --color-on-tertiary-container
val LightSurfaceContainerLow = Color(0xFFF6F3F2)      // --color-surface-container-low
val LightSurfaceContainer = Color(0xFFF0EDED)         // --color-surface-container
val LightSurfaceContainerHigh = Color(0xFFEAE8E7)     // --color-surface-container-high
val LightOutlineVariant = Color(0xFFDBC2B0)           // --color-outline-variant
val Gold = Color(0xFFD4AF37)                          // --color-gold (same in both themes)

// ---- Dark (.dark) ----
val DarkSurface = Color(0xFF0A1323)                   // --color-surface
val DarkOnSurface = Color(0xFFDAE2F9)                 // --color-on-surface
val DarkOnSurfaceVariant = Color(0xFF90A0C8)          // --color-on-surface-variant
val DarkPrimary = Color(0xFF20FF8C)                   // --color-primary
val DarkPrimaryDark = Color(0xFF008F47)               // --color-primary-dark
val DarkPrimaryContainer = Color(0xFF20FF8C)          // --color-primary-container
val DarkOnPrimaryContainer = Color(0xFF00391A)        // --color-on-primary-container
val DarkTertiary = Color(0xFF20FF8C)                  // --color-tertiary
val DarkTertiaryContainer = Color(0xFF13243A)         // --color-tertiary-container
val DarkOnTertiaryContainer = Color(0xFF9CCBFB)       // --color-on-tertiary-container
val DarkSurfaceContainerLow = Color(0xFF0F1A2E)       // --color-surface-container-low
val DarkSurfaceContainer = Color(0xFF131C2C)          // --color-surface-container
val DarkSurfaceContainerHigh = Color(0xFF172030)      // --color-surface-container-high
val DarkOutlineVariant = Color(0x1AFFFFFF)            // --color-outline-variant: rgba(255,255,255,0.1)

// Misc raw values used verbatim around the web app (glass panels, glows, leaflet chrome)
val LightGlassBg = Color(0xD9FBF9F8)                  // rgba(251,249,248,0.85)
val DarkGlassBg = Color(0xD90A1323)                   // rgba(10,19,35,0.85)
val LightGlowShadow = Color(0x99FF9933)               // rgba(255,153,51,0.6)
val DarkGlowShadow = Color(0x9920FF8C)                // rgba(32,255,140,0.6)

// ---- Back-compat bridge for existing screen code ----
// Every screen file (DiscoverScreen, ProfileScreen, DestinationDetailScreen, etc.) was
// written against these three names as static top-level colors. They're kept as the same
// names so no call site needs to change, but TerracottaPrimary/TerracottaDark are now
// @Composable properties that read the app's *actual* active theme (LocalHiddenIndiaIsDark,
// driven by ThemeController — see Theme.kt) instead of always returning the light-mode
// value. This makes them flip saffron (#ff9933) -> mint green (#20ff8c) on dark mode exactly
// like MaterialTheme.colorScheme.primary does, matching the web app's --color-primary swap.
val TerracottaPrimary: Color
    @Composable get() = if (LocalHiddenIndiaIsDark.current) DarkPrimary else LightPrimary
val TerracottaDark: Color
    @Composable get() = if (LocalHiddenIndiaIsDark.current) DarkPrimaryDark else LightPrimaryDark
val HeritageGold = Gold                               // was #D4A373; now the real --color-gold #D4AF37 (identical in both themes on web, so this one is correctly static)
val ForestGreen = Color(0xFF059669)                   // eco/success green; web uses ad-hoc Tailwind emerald-600 for this, not a semantic token
