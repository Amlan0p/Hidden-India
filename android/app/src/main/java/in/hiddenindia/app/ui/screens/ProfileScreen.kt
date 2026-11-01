package `in`.hiddenindia.app.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import `in`.hiddenindia.app.ui.viewmodel.AuthFormState
import `in`.hiddenindia.app.ui.components.AvatarSize
import `in`.hiddenindia.app.ui.components.UserAvatar
import `in`.hiddenindia.app.ui.theme.AVATAR_PRESETS
import `in`.hiddenindia.app.ui.theme.DEFAULT_AVATAR_SVG
import `in`.hiddenindia.app.ui.theme.JetBrainsMono
import `in`.hiddenindia.app.ui.theme.LightTertiary
import `in`.hiddenindia.app.ui.theme.PlayfairDisplay
import `in`.hiddenindia.app.ui.theme.normalizeAvatarSvg
import `in`.hiddenindia.app.ui.theme.ThemeController
import `in`.hiddenindia.app.ui.viewmodel.AuthViewModel
import `in`.hiddenindia.app.ui.viewmodel.MainViewModel
import java.io.ByteArrayOutputStream

/**
 * Port of web src/pages/ProfilePage.tsx. The edit-profile modal now calls a real endpoint
 * (AuthViewModel.updateProfile — the Retrofit call already existed but nothing invoked it),
 * and the avatar preset picker renders the exact same illustrated SVGs as web (see
 * AvatarPresets.kt + coil-svg in HiddenIndiaApplication) rather than an approximation.
 */
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    authViewModel: AuthViewModel,
    themeController: ThemeController,
    onOpenDestination: (String) -> Unit,
    onOpenItinerary: () -> Unit,
    onOpenVault: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenTranslator: () -> Unit,
    onOpenEmergency: () -> Unit,
    onOpenResponsibleTourism: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val favoriteIds by authViewModel.favoriteIds.collectAsState()
    val savedItineraries by authViewModel.savedItineraries.collectAsState()
    val downloadedPackIds by viewModel.downloadedPackIds.collectAsState()
    var showEditModal by remember { mutableStateOf(false) }

    LazyColumn(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentPadding = PaddingValues(16.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("btn_profile_back")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text("Profile & Settings", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFEF4444).copy(alpha = 0.1f))
                        .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.2f), RoundedCornerShape(50))
                        .clickable(onClick = onOpenEmergency)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Emergency, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(15.dp))
                    Text("  SOS Mode", fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color(0xFFDC2626))
                }
            }

            // Hero avatar card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 20.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .clickable { showEditModal = true }
                        .padding(bottom = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .padding(3.dp)
                    ) {
                        UserAvatar(src = currentUser?.avatar, name = currentUser?.name ?: "Explorer", size = AvatarSize.Xxl)
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                    }
                }
                Text((currentUser?.role ?: "Cultural Connoisseur").uppercase(), fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.sp, color = LightTertiary)
                Text(currentUser?.name ?: "Developer", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 2.dp))
                Text(currentUser?.email ?: "developer@hidden_india.in", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "UID: ${currentUser?.id ?: "usr_developer"}",
                        fontFamily = JetBrainsMono, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                    Text(
                        "  EDIT PROFILE", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .clickable { showEditModal = true }
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), shape = RoundedCornerShape(12.dp))
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatColumn(favoriteIds.size, "Gems Saved")
                    StatColumn(savedItineraries.size, "Journeys")
                    StatColumn(downloadedPackIds.size, "Offline Packs")
                }
            }

            SettingsSectionLabel("APPEARANCE")
            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { themeController.toggle() }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (themeController.isDark) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                if (themeController.isDark) "AMOLED Dark Mode" else "Warm Sunlit Light Mode",
                                fontFamily = PlayfairDisplay,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                if (themeController.isDark) "Pure black background with warm orange accents" else "Warm terracotta & sand palette",
                                fontFamily = JetBrainsMono,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = themeController.isDark,
                        onCheckedChange = { themeController.toggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.testTag("switch_theme")
                    )
                }
            }

            SettingsSectionLabel("EXPLORER VAULT & STORAGE")
            SettingsCard {
                SettingsRow(Icons.Filled.SdStorage, LightTertiary, "Offline Destination Vault", "${downloadedPackIds.size} regions stored locally", onOpenVault)
                SettingsDivider()
                SettingsRow(Icons.Filled.SdStorage, MaterialTheme.colorScheme.primary, "Saved Gems & Itineraries", "Your personal travel collection", onOpenFavorites)
            }

            SettingsSectionLabel("ETHICS & COMMUNITY")
            SettingsCard {
                SettingsRow(Icons.Filled.Eco, LightTertiary, "Responsible Tourism Guidelines", "Ethical rules & traveler pledge", onOpenResponsibleTourism)
                SettingsDivider()
                SettingsRow(Icons.Filled.Translate, MaterialTheme.colorScheme.primary, "Local Language Lexicon", "Audio phrases for authentic conversations", onOpenTranslator)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFEF4444).copy(alpha = 0.1f))
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .clickable(onClick = onLogout)
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Logout, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                Text("  Sign Out of Explorer Account", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.5.sp, color = Color(0xFFDC2626))
            }
        }
    }

    if (showEditModal) {
        EditProfileDialog(
            authViewModel = authViewModel,
            currentName = currentUser?.name ?: "",
            currentRole = currentUser?.role ?: "Cultural Connoisseur",
            currentAvatar = currentUser?.avatar ?: "",
            onDismiss = { showEditModal = false }
        )
    }
}

@Composable
private fun StatColumn(value: Int, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            text = "$value",
            fontFamily = PlayfairDisplay,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = LightTertiary,
            lineHeight = 24.sp
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label.uppercase(),
            fontFamily = JetBrainsMono,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 11.sp
        )
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(text, fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    ) { content() }
}

@Composable
private fun SettingsDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)))
}

@Composable
private fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, iconTint: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(title, fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, fontFamily = JetBrainsMono, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun EditProfileDialog(
    authViewModel: AuthViewModel,
    currentName: String,
    currentRole: String,
    currentAvatar: String,
    onDismiss: () -> Unit
) {
    var nameInput by remember { mutableStateOf(currentName) }
    var roleInput by remember { mutableStateOf(currentRole) }
    val initialAvatar = remember(currentAvatar) { normalizeAvatarSvg(currentAvatar) }
    var selectedAvatar by remember { mutableStateOf(initialAvatar) }
    val profileActionState by authViewModel.profileActionState.collectAsState()
    val context = LocalContext.current
    var justSaved by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val input = context.contentResolver.openInputStream(it)
                val bitmap = android.graphics.BitmapFactory.decodeStream(input)
                val maxSize = 256
                val ratio = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height, 1f)
                val scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
                val out = ByteArrayOutputStream()
                scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
                val base64 = android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.NO_WRAP)
                selectedAvatar = "data:image/jpeg;base64,$base64"
            } catch (e: Exception) {
                Toast.makeText(context, "Please choose a valid image file.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(profileActionState) {
        if (profileActionState is AuthFormState.Idle && justSaved) {
            onDismiss()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text("Customize Explorer Profile", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Select an avatar preset, upload a photo, or update your name.", fontFamily = JetBrainsMono, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.clickable(onClick = onDismiss))
            }

            LazyColumn(modifier = Modifier.padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
                item {
                    if (profileActionState is AuthFormState.Error) {
                        Text(
                            (profileActionState as AuthFormState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            fontFamily = JetBrainsMono, fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(64.dp).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape).padding(2.dp)) {
                            UserAvatar(src = selectedAvatar, name = nameInput.ifBlank { "Explorer" }, size = AvatarSize.Lg)
                        }
                        Column(modifier = Modifier.padding(start = 14.dp)) {
                            Text("Profile Picture", fontFamily = PlayfairDisplay, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Choose a preset below or upload your own.", fontFamily = JetBrainsMono, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.background)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .clickable { photoPicker.launch("image/*") }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Upload, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(14.dp))
                                    Text("  Upload Photo", fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                                if (normalizeAvatarSvg(selectedAvatar) != DEFAULT_AVATAR_SVG) {
                                    Text(
                                        "  Reset to Default", fontFamily = JetBrainsMono, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 8.dp).clickable { selectedAvatar = DEFAULT_AVATAR_SVG }
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        "CULTURAL EXPLORER PRESETS", fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }

                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.height(220.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(AVATAR_PRESETS, key = { it.id }) { preset ->
                            val isSelected = normalizeAvatarSvg(selectedAvatar) == preset.svg
                            Column(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .border(if (isSelected) 2.dp else 1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                    .clickable { selectedAvatar = preset.svg }
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(modifier = Modifier.size(44.dp).clip(CircleShape)) {
                                    UserAvatar(src = preset.svg, name = preset.name, size = AvatarSize.Md)
                                }
                                Text(preset.name, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text("DISPLAY NAME", fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                        LabeledTextField(nameInput, { nameInput = it }, "e.g. Developer")
                        Text("EXPLORER TITLE / ROLE", fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                        LabeledTextField(roleInput, { roleInput = it }, "e.g. Cultural Connoisseur")
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            "Cancel", fontFamily = JetBrainsMono, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable(onClick = onDismiss).padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable(enabled = profileActionState !is AuthFormState.Loading) {
                                    justSaved = true
                                    authViewModel.updateProfile(
                                        name = nameInput.trim().ifBlank { currentName.ifBlank { "Explorer" } },
                                        role = roleInput.trim().ifBlank { "Conscious Explorer" },
                                        avatar = selectedAvatar.ifBlank { currentAvatar.ifBlank { DEFAULT_AVATAR_SVG } }
                                    )
                                }
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (profileActionState is AuthFormState.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                Text("  Saving...", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Text("Save Changes", fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledTextField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        BasicTextField(value = value, onValueChange = onValueChange, singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface))
        if (value.isEmpty()) {
            Text(placeholder, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}
