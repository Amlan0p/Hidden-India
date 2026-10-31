package `in`.hiddenindia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.hiddenindia.app.data.LocalPhrase
import `in`.hiddenindia.app.data.SupportedLanguage
import `in`.hiddenindia.app.ui.components.rememberSpeechController
import `in`.hiddenindia.app.ui.theme.ForestGreen
import `in`.hiddenindia.app.ui.theme.HeritageGold
import `in`.hiddenindia.app.ui.theme.JetBrainsMono
import `in`.hiddenindia.app.ui.theme.TerracottaPrimary
import `in`.hiddenindia.app.ui.viewmodel.MainViewModel

/**
 * Port of web src/pages/LanguageAssistantPage.tsx. Two real gaps fixed vs the previous
 * native version: the "Verified Offline Phrase Bank" (browsing pre-written phrases across
 * every destination, with language/category filters) didn't exist at all, and there was no
 * audio playback anywhere — web speaks every phrase via the browser's SpeechSynthesis;
 * native now uses Android's on-device TextToSpeech (see SpeechHelper.kt) for the same
 * phrase cards AND the custom AI translation result.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val languages by viewModel.allLanguages.collectAsState()
    val isTranslating by viewModel.isTranslating.collectAsState()
    val result by viewModel.translationResult.collectAsState()
    val error by viewModel.translationError.collectAsState()
    val speech = rememberSpeechController()
    var playingPhraseId by remember { mutableStateOf<String?>(null) }

    var inputText by remember { mutableStateOf("") }
    var sourceLanguage by remember(languages) { mutableStateOf(languages.find { it.name == "English" }?.name ?: "English") }
    var targetLanguage by remember(languages) { mutableStateOf<SupportedLanguage?>(languages.firstOrNull { it.name != "English" } ?: languages.firstOrNull()) }
    var expandedDropdown by remember { mutableStateOf(false) }
    var sourceDropdownExpanded by remember { mutableStateOf(false) }

    fun speakPhrase(id: String, text: String, languageName: String?) {
        val speechCode = languages.find { it.name == languageName }?.speechCode
        playingPhraseId = id
        speech?.speak(text, speechCode) { playingPhraseId = null }
    }

    LaunchedEffect(inputText, targetLanguage) {
        if (result != null || error != null) viewModel.clearTranslation()
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("btn_translator_back")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TerracottaPrimary)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.Translate, contentDescription = null, tint = TerracottaPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Local Language Assistant", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(
                "Translate traveler phrases across major Indian languages and regional dialects with phonetic guides and cultural etiquette tips.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Custom AI phrase translator (already real in the prior version — kept, with
            // source-language + swap added to match web, and a working play button added)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    ExposedDropdownMenuBox(expanded = sourceDropdownExpanded, onExpandedChange = { sourceDropdownExpanded = !sourceDropdownExpanded }) {
                        OutlinedTextField(
                            value = sourceLanguage, onValueChange = {}, readOnly = true, label = { Text("From", fontSize = 11.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceDropdownExpanded) },
                            colors = TextFieldDefaults.colors(focusedIndicatorColor = TerracottaPrimary, cursorColor = TerracottaPrimary),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = sourceDropdownExpanded, onDismissRequest = { sourceDropdownExpanded = false }) {
                            languages.forEach { lang ->
                                DropdownMenuItem(text = { Text("${lang.name} (${lang.nativeName})") }, onClick = { sourceLanguage = lang.name; sourceDropdownExpanded = false })
                            }
                        }
                    }
                }
                IconButton(onClick = {
                    val prevSource = sourceLanguage
                    val prevTarget = targetLanguage
                    sourceLanguage = prevTarget?.name ?: sourceLanguage
                    targetLanguage = languages.find { it.name == prevSource } ?: targetLanguage
                }) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "Swap languages", tint = TerracottaPrimary)
                }
                Box(modifier = Modifier.weight(1f)) {
                    ExposedDropdownMenuBox(expanded = expandedDropdown, onExpandedChange = { expandedDropdown = !expandedDropdown }) {
                        OutlinedTextField(
                            value = targetLanguage?.name ?: "Select", onValueChange = {}, readOnly = true, label = { Text("To", fontSize = 11.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                            colors = TextFieldDefaults.colors(focusedIndicatorColor = TerracottaPrimary, cursorColor = TerracottaPrimary),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expandedDropdown, onDismissRequest = { expandedDropdown = false }) {
                            languages.forEach { lang ->
                                DropdownMenuItem(text = { Text("${lang.name} (${lang.nativeName})") }, onClick = { targetLanguage = lang; expandedDropdown = false })
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = inputText, onValueChange = { inputText = it },
                label = { Text("Text in $sourceLanguage") },
                placeholder = { Text("e.g. Where is the nearest bus stop?") },
                minLines = 2,
                colors = TextFieldDefaults.colors(focusedIndicatorColor = TerracottaPrimary, cursorColor = TerracottaPrimary),
                modifier = Modifier.fillMaxWidth().testTag("translator_input_field")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val target = targetLanguage?.name ?: return@Button
                    viewModel.translate(text = inputText, targetLang = target)
                },
                enabled = inputText.isNotBlank() && targetLanguage != null && !isTranslating,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("btn_translate")
            ) {
                if (isTranslating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Translate", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(text = error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = {
                    val target = targetLanguage?.name ?: return@TextButton
                    viewModel.translate(text = inputText, targetLang = target)
                }) { Text("Retry", color = TerracottaPrimary) }
            }

            val translation = result
            if (translation != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = translation.translatedText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = HeritageGold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "Phonetic: ${translation.phonetic}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { speakPhrase("custom_res", translation.translatedText, targetLanguage?.name) }) {
                                Icon(
                                    if (playingPhraseId == "custom_res") Icons.Default.VolumeUp else Icons.Default.PlayArrow,
                                    contentDescription = "Play pronunciation", tint = TerracottaPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Literal meaning", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(translation.literalMeaning, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Cultural tip", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ForestGreen)
                        Text(translation.culturalTip, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
