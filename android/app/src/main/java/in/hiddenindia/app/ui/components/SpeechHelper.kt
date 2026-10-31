package `in`.hiddenindia.app.ui.components

import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Wraps Android's on-device TextToSpeech engine as the native equivalent of web's
 * `window.speechSynthesis` (see LanguageAssistantPage.tsx's speakPhrase). Web had real
 * audio playback for every phrase and the custom translation result; native had none at
 * all until this.
 */
class SpeechController internal constructor(private val tts: TextToSpeech) {
    fun speak(text: String, languageTag: String?, onDone: () -> Unit) {
        languageTag?.let { tag ->
            val locale = Locale.forLanguageTag(tag)
            val result = tts.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.language = Locale.US // graceful fallback if the device has no voice pack for this language
            }
        }
        tts.setSpeechRate(0.85f)
        tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { onDone() }
            override fun onError(utteranceId: String?) { onDone() }
        })
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hidden_india_phrase")
    }

    fun stop() { tts.stop() }
}

@Composable
fun rememberSpeechController(): SpeechController? {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<SpeechController?>(null) }
    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(Unit) {
        val instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // no-op: language is set per-utterance in speak()
            }
        }
        ttsInstance = instance
        controller = SpeechController(instance)
        onDispose {
            instance.stop()
            instance.shutdown()
        }
    }
    return controller
}
