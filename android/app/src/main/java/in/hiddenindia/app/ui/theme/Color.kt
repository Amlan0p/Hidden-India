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

// ---- Dark: AMOLED Black + Orange Accent Palette ----
val DarkBackground = Color(0xFF000000)                // Main background: pure AMOLED black (#000000)
val DarkSecondaryBackground = Color(0xFF080808)       // Secondary background (#080808)
val DarkCard = Color(0xFF101010)                      // Card background (#101010)
val DarkElevatedSurface = Color(0xFF181818)           // Elevated surface (#181818)
val DarkInputBackground = Color(0xFF141414)           // Input background (#141414)

val DarkPrimary = Color(0xFFFF8C00)                   // Primary orange (#FF8C00)
val DarkPrimaryDark = Color(0xFFE67E00)               // Pressed orange (#E67E00)
val DarkSecondaryOrange = Color(0xFFFFA726)           // Secondary orange (#FFA726)
val DarkPrimaryContainer = Color(0xFFFF8C00)          // Primary container (#FF8C00)
val DarkOnPrimary = Color(0xFF000000)                 // Black text for high contrast on orange
val DarkOnPrimaryContainer = Color(0xFF000000)        // Black text for high contrast on orange container

val DarkTertiary = Color(0xFFFFA726)                  // Warm orange (#FFA726)
val DarkTertiaryContainer = Color(0xFF181818)         // Elevated surface container (#181818)
val DarkOnTertiaryContainer = Color(0xFFFFA726)       // Warm orange text

val DarkSurface = Color(0xFF101010)                   // Cards and surfaces: near-black #101010
val DarkOnSurface = Color(0xFFFFFFFF)                 // Primary text: White (#FFFFFF)
val DarkOnSurfaceVariant = Color(0xFFB3B3B3)          // Secondary text: Soft grey (#B3B3B3)
val DarkMutedText = Color(0xFF777777)                 // Muted text: #777777

val DarkSurfaceContainerLowest = Color(0xFF000000)    // Pure AMOLED black (#000000)
val DarkSurfaceContainerLow = Color(0xFF080808)       // Secondary background (#080808)
val DarkSurfaceContainer = Color(0xFF101010)          // Card background (#101010)
val DarkSurfaceContainerHigh = Color(0xFF181818)      // Elevated surface (#181818)
val DarkSurfaceContainerHighest = Color(0xFF222222)   // #222222
val DarkOutlineVariant = Color(0xFF292929)            // Divider and borders (#292929)
val DarkOutline = Color(0xFF777777)                   // Muted border / outline (#777777)
val DarkError = Color(0xFFFF5252)                     // Error (#FF5252)
val DarkSuccess = Color(0xFF4CAF50)                   // Success (#4CAF50)

// Misc raw values used verbatim around the web app (glass panels, glows, leaflet chrome)
val LightGlassBg = Color(0xD9FBF9F8)                  // rgba(251,249,248,0.85)
val DarkGlassBg = Color(0xEB000000)                   // AMOLED translucent black
val LightGlowShadow = Color(0x99FF9933)               // rgba(255,153,51,0.6)
val DarkGlowShadow = Color(0x33FF8C00)                // Subtle warm orange shadow (no neon glow)

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
