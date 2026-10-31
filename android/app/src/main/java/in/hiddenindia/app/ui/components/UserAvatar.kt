package `in`.hiddenindia.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import `in`.hiddenindia.app.ui.theme.DEFAULT_AVATAR_SVG
import `in`.hiddenindia.app.ui.theme.getAvatarGradient
import `in`.hiddenindia.app.ui.theme.getInitials
import `in`.hiddenindia.app.ui.theme.resolveAvatarModel

enum class AvatarSize(val box: Dp, val text: TextUnit) {
    // Ported from UserAvatar.tsx's SIZE_MAP (w-*, text-*)
    Xs(24.dp, 10.sp),
    Sm(32.dp, 12.sp),
    Md(40.dp, 14.sp),
    Lg(64.dp, 20.sp),
    Xl(96.dp, 24.sp),
    Xxl(112.dp, 30.sp)
}

/**
 * Shows the user's avatar image (remote image, local photo, or predefined explorer SVG preset).
 * If no avatar is specified, defaults to the 1st cultural explorer preset.
 */
@Composable
fun UserAvatar(
    src: String?,
    name: String?,
    modifier: Modifier = Modifier,
    size: AvatarSize = AvatarSize.Md,
    bordered: Boolean = false,
    interactive: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val displayName = name ?: "Developer"
    val initials = getInitials(displayName)
    val gradient = getAvatarGradient(displayName)
    val borderColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .size(size.box)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(gradient.from, gradient.to)))
            .then(if (bordered) Modifier.border(2.dp, borderColor, CircleShape) else Modifier)
            .then(if (interactive && onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        val model: Any = resolveAvatarModel(src ?: DEFAULT_AVATAR_SVG)
        AsyncImage(
            model = model,
            contentDescription = displayName,
            modifier = Modifier.size(size.box).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}
