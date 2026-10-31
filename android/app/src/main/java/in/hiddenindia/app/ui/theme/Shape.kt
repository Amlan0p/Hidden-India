package `in`.hiddenindia.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Shapes

// Mirrors the rounded-* Tailwind corner radii used throughout the web app's cards,
// chips and sheets (rounded-lg=8, rounded-xl=12, rounded-2xl=16, rounded-3xl=24).
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
