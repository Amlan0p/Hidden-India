package `in`.hiddenindia.app.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

private const val PREFS_NAME = "hidden_india_theme_prefs"
private const val KEY_THEME = "hidden_india_theme"

/**
 * Port of web src/context/ThemeContext.tsx. Both apps now default to the device's own
 * light/dark setting when there's no saved preference yet, and remember an explicit user
 * toggle after that (SharedPreferences here, localStorage on web).
 */
class ThemeController(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val systemPrefersDark: Boolean = run {
        val nightModeFlags = context.applicationContext.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK
        nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    var isDark by mutableStateOf(
        when (prefs.getString(KEY_THEME, null)) {
            "light" -> false
            "dark" -> true
            else -> systemPrefersDark
        }
    )
        private set

    fun toggle() {
        isDark = !isDark
        prefs.edit().putString(KEY_THEME, if (isDark) "dark" else "light").apply()
    }
}

@Composable
fun rememberThemeController(): ThemeController {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember { ThemeController(context) }
}
