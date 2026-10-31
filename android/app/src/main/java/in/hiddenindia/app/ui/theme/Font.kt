package `in`.hiddenindia.app.ui.theme

import `in`.hiddenindia.app.R
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight

// Matches web index.css:
//   headings / .font-serif / .font-headline-*  -> 'Playfair Display', Georgia, serif
//   body                                        -> 'Inter', 'Plus Jakarta Sans', system-ui, sans-serif
//   .font-mono / .font-label-caps / .font-label-sm -> 'JetBrains Mono', 'Space Grotesk', monospace
//
// All three are variable-weight fonts on the web. We ship the single variable .ttf for
// each family and pull out static weight instances via FontVariation so Compose gets a
// real weight axis instead of synthetic (faux) bold.

@OptIn(ExperimentalTextApi::class)
private fun variableWeights(resId: Int, style: FontStyle = FontStyle.Normal) = FontFamily(
    Font(resId, FontWeight.Normal, style, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(resId, FontWeight.Medium, style, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(resId, FontWeight.SemiBold, style, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(resId, FontWeight.Bold, style, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(resId, FontWeight.ExtraBold, style, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
)

val PlayfairDisplay: FontFamily = variableWeights(R.font.playfair_display)
val PlayfairDisplayItalic: FontFamily = variableWeights(R.font.playfair_display_italic, FontStyle.Italic)
val Inter: FontFamily = variableWeights(R.font.inter)
val JetBrainsMono: FontFamily = variableWeights(R.font.jetbrains_mono)
