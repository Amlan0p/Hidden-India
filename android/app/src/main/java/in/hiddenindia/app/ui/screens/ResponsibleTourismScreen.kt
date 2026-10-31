package `in`.hiddenindia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.NoDrinks
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.hiddenindia.app.R
import `in`.hiddenindia.app.ui.theme.JetBrainsMono
import `in`.hiddenindia.app.ui.theme.PlayfairDisplay
import `in`.hiddenindia.app.ui.theme.TerracottaPrimary

// icon is a slot lambda (not a plain ImageVector) so the "temple_hindu" principle can reuse
// the same custom drawable Logo.kt uses for that glyph — androidx.compose.material.icons
// has no built-in equivalent of that specific Material Symbol, same reason Logo.kt uses
// painterResource(R.drawable.ic_temple_hindu) instead of an Icons.Filled.* constant.
private data class Principle(val icon: @Composable (Modifier) -> Unit, val title: String, val description: String)

// Same six principles, in the same order, as web src/pages/ResponsibleTourismPage.tsx.
private val PRINCIPLES = listOf(
    Principle(
        { m -> Icon(Icons.Filled.Palette, contentDescription = null, tint = TerracottaPrimary, modifier = m) },
        "Support Independent Local Artisans",
        "Purchase miniature paintings in Bundi, Athangudi tiles in Chettinad, and masks in Majuli directly from community workshops rather than middleman tourist emporiums."
    ),
    Principle(
        { m -> Icon(Icons.Filled.Storefront, contentDescription = null, tint = TerracottaPrimary, modifier = m) },
        "Dine at Family-Run Neighborhood Dhabas",
        "Every meal consumed at local eateries injects revenue directly into the micro-economy and helps preserve generational culinary heritage."
    ),
    Principle(
        { m -> Icon(Icons.Filled.NoDrinks, contentDescription = null, tint = TerracottaPrimary, modifier = m) },
        "Eliminate Single-Use Plastic Water Bottles",
        "Carry a reusable insulated flask. Refill at verified RO filter points and homestays to protect fragile Himalayan valleys and ancient stepwells."
    ),
    Principle(
        { m -> Icon(painterResource(R.drawable.ic_temple_hindu), contentDescription = null, tint = TerracottaPrimary, modifier = m) },
        "Respect Cultural & Sacred Sites",
        "Observe footwear rules at stepwells and Satras, dress with modesty covering knees and shoulders, and maintain quiet reverence during morning prayers."
    ),
    Principle(
        { m -> Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = TerracottaPrimary, modifier = m) },
        "Ask Before Photographing People",
        "Always greet elders with folded hands (\"Khamma Ghani\", \"Nomoskar\", \"Vanakkam\") and seek explicit permission before taking portrait photography."
    ),
    Principle(
        { m -> Icon(Icons.Filled.Hiking, contentDescription = null, tint = TerracottaPrimary, modifier = m) },
        "Stay on Marked Heritage & Nature Paths",
        "Prevent trail erosion and respect private tribal crop terraces in Ziro Valley and Majuli river dikes."
    )
)

/**
 * Port of web src/pages/ResponsibleTourismPage.tsx, reached from Profile ->
 * "Responsible Tourism Guidelines" (Ethics & Community section).
 */
@Composable
fun ResponsibleTourismScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    var pledgeTaken by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.LocalFlorist, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(28.dp))
                }
                Text(
                    "CONSCIOUS EXPLORATION",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    "Responsible Tourism",
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    "\"Discover the India Most Travelers Miss\u2014while leaving every village, stepwell, and sacred grove thriving for generations.\"",
                    fontFamily = JetBrainsMono,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        items(PRINCIPLES) { p ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TerracottaPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    p.icon(Modifier.size(20.dp))
                }
                Text(
                    p.title,
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 10.dp)
                )
                Text(
                    p.description,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, TerracottaPrimary.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = TerracottaPrimary, modifier = Modifier.size(32.dp))
                Text(
                    "The Hidden India Traveler Pledge",
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    "\"I pledge to travel with humility and awareness, to support indigenous craftspeople, to honor local customs, to minimize plastic footprint, and to preserve the spirit of authentic India.\"",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp)
                )

                Row(
                    modifier = Modifier
                        .padding(top = 18.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (pledgeTaken) TerracottaPrimary.copy(alpha = 0.2f) else TerracottaPrimary)
                        .clickable { pledgeTaken = !pledgeTaken }
                        .padding(horizontal = 22.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (pledgeTaken) Icons.Filled.CheckCircle else Icons.Filled.Fingerprint,
                        contentDescription = null,
                        tint = if (pledgeTaken) TerracottaPrimary else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (pledgeTaken) "Pledge Signed & Recorded" else "Sign Traveler Pledge",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp,
                        color = if (pledgeTaken) TerracottaPrimary else Color.White,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                if (pledgeTaken) {
                    Text(
                        "\uD83C\uDF1F Thank you, Explorer! Your commitment helps safeguard India's living cultural heritage.",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = TerracottaPrimary,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}
