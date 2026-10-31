package `in`.hiddenindia.app.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

fun openYoutubeVideo(context: Context, videoId: String = "1K4fL_P5rQI") {
    val appUri = Uri.parse("vnd.youtube:$videoId")
    val webUri = Uri.parse("https://www.youtube.com/watch?v=$videoId")

    // Attempt 1: Explicitly target YouTube official app
    try {
        val appIntent = Intent(Intent.ACTION_VIEW, appUri).apply {
            setPackage("com.google.android.youtube")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(appIntent)
        return
    } catch (_: Exception) {}

    // Attempt 2: Target YouTube app with web URL
    try {
        val appWebIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
            setPackage("com.google.android.youtube")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(appWebIntent)
        return
    } catch (_: Exception) {}

    // Attempt 3: General vnd.youtube scheme
    try {
        val genericIntent = Intent(Intent.ACTION_VIEW, appUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(genericIntent)
        return
    } catch (_: Exception) {}

    // Attempt 4: Fallback to web browser
    try {
        val browserIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(browserIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open YouTube", Toast.LENGTH_SHORT).show()
    }
}
