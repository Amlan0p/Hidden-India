package `in`.hiddenindia.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import `in`.hiddenindia.app.ui.theme.JetBrainsMono
import `in`.hiddenindia.app.ui.theme.PlayfairDisplay

private const val HERO_IMAGE_URL =
    "https://upload.wikimedia.org/wikipedia/commons/thumb/d/dd/Wide_angle_of_Galigopuram_of_Virupaksha_Temple%2C_Hampi_%2804%29_%28cropped%29.jpg/1280px-Wide_angle_of_Galigopuram_of_Virupaksha_Temple%2C_Hampi_%2804%29_%28cropped%29.jpg"

/**
 * Port of web src/components/common/ExperienceIndiaHero.tsx — the "Experience India's
 * Soul" promo banner shown between the Discover filters and the results grid.
 */
@Composable
fun ExperienceIndiaHero(
    onWatchFilm: () -> Unit,
    onPlanTrip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(224.dp)
            .clip(RoundedCornerShape(20.dp))
    ) {
        AsyncImage(
            model = HERO_IMAGE_URL,
            contentDescription = "Virupaksha Temple, Hampi",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 0.8f),
                        0.55f to Color.Black.copy(alpha = 0.5f),
                        1f to Color.Black.copy(alpha = 0.3f)
                    )
                )
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                Text(
                    text = "Experience India's Soul",
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    lineHeight = 30.sp,
                    color = Color.White
                )
                Text(
                    text = "Cinematic journeys through the unseen. Stepwells, monasteries, and tribal heritage.",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 6.dp)
                )
                Row(
                    modifier = Modifier.padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable(onClick = onWatchFilm)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.height(16.dp))
                        Text("WATCH FILM", fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.5.sp, color = Color.White)
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable(onClick = onPlanTrip)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.height(16.dp))
                        Text("PLAN TRIP", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.5.sp, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}
