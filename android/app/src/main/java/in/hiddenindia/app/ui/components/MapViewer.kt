package `in`.hiddenindia.app.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.RequestDisallowInterceptTouchEvent
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import `in`.hiddenindia.app.data.OfflineTileCache
import `in`.hiddenindia.app.ui.theme.JetBrainsMono
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileInputStream

data class MapMarker(
    val id: String,
    val lat: Double,
    val lng: Double,
    val name: String,
    val kind: String, // "place" | "gem" | "food" | "experience"
    val isCenter: Boolean = false,
    val image: String? = null,
    val description: String? = null,
    val category: String? = null,
    val insiderTip: String? = null,
    val bestTimeOfDay: String? = null,
    val estimatedCost: String? = null
)

private var cachedLeafletTemplate: String? = null

private fun getLeafletHtml(context: Context, isDark: Boolean): String {
    if (cachedLeafletTemplate == null) {
        val css = context.assets.open("leaflet.css").bufferedReader().use { it.readText() }
        val js = context.assets.open("leaflet.js").bufferedReader().use { it.readText() }
        val baseHtml = context.assets.open("leaflet_map.html").bufferedReader().use { it.readText() }
        cachedLeafletTemplate = baseHtml
            .replace("/*LEAFLET_CSS*/", css)
            .replace("/*LEAFLET_JS*/", js)
    }
    return cachedLeafletTemplate!!.replace("%%IS_DARK%%", if (isDark) "true" else "false")
}

/**
 * Custom WebView that prevents parent scrollable containers (like Compose LazyColumn or ScrollView)
 * from stealing touch events while the user is interacting with the map.
 */
@SuppressLint("ClickableViewAccessibility")
private class MapWebView(context: Context) : WebView(context) {

    private fun disallowParentIntercept(disallow: Boolean) {
        var p = parent
        while (p != null) {
            p.requestDisallowInterceptTouchEvent(disallow)
            p = p.parent
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                disallowParentIntercept(true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                disallowParentIntercept(false)
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                disallowParentIntercept(true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                disallowParentIntercept(false)
            }
        }
        return super.onTouchEvent(event)
    }

    override fun overScrollBy(
        deltaX: Int, deltaY: Int,
        scrollX: Int, scrollY: Int,
        scrollRangeX: Int, scrollRangeY: Int,
        maxOverScrollX: Int, maxOverScrollY: Int,
        isTouchEvent: Boolean
    ): Boolean {
        return false
    }
}

/**
 * Port of web src/components/common/MapViewer.tsx. Wraps a WebView around Leaflet + OSM tiles.
 * Assets (leaflet.css and leaflet.js) are inlined directly into the document so there is
 * zero network delay or missing script issues.
 *
 * Implements robust software rendering guards and renderer process crash recovery to prevent
 * aw_browser_terminator watchdog terminates on Mesa emulator devices lacking rendernodes.
 */
@OptIn(ExperimentalComposeUiApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapViewer(
    markers: List<MapMarker>,
    userLocation: Pair<Double, Double>?,
    onMarkerClick: (String) -> Unit,
    height: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    focusTarget: Pair<Double, Double>? = null,
    offlineOnly: Boolean = false
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val disallowIntercept = remember { RequestDisallowInterceptTouchEvent() }

    var isRendererCrashed by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }

    if (isRendererCrashed) {
        // Fallback UI when WebView renderer process is terminated by the OS emulator watchdog
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Interactive Cartography",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${markers.size} points of interest mapped for this region",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        isRendererCrashed = false
                        refreshKey++
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reload Map", fontSize = 12.sp)
                }
            }
        }
    } else {
        key(refreshKey) {
            AndroidView(
                modifier = modifier
                    .fillMaxWidth()
                    .height(height)
                    .clip(shape)
                    .pointerInteropFilter(requestDisallowInterceptTouchEvent = disallowIntercept) { motionEvent ->
                        when (motionEvent.actionMasked) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                disallowIntercept(true)
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                disallowIntercept(false)
                            }
                        }
                        false
                    },
                factory = {
                    MapWebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // Software layer type prevents crashes on emulator/headless devices lacking DRM GPU rendernodes
                        setLayerType(View.LAYER_TYPE_SOFTWARE, null)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = false
                            allowFileAccess = true
                            allowContentAccess = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = "HiddenIndiaAndroid/1.0 (contact@hiddenindia.in) " + userAgentString
                        }

                        setTag(tagIsPageLoaded, false)
                        setTag(tagOfflineOnly, offlineOnly)

                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                android.util.Log.d("MapViewer", "[Leaflet JS] ${consoleMessage?.message()} (${consoleMessage?.lineNumber()})")
                                return true
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            private val tileUrlRegex = Regex("""/(\d+)/(-?\d+)/(-?\d+)(?:@[0-9a-zA-Z]+)?\.png""")

                            override fun onRenderProcessGone(
                                view: WebView?,
                                detail: RenderProcessGoneDetail?
                            ): Boolean {
                                android.util.Log.w(
                                    "MapViewer",
                                    "WebView render process gone (crashed: ${detail?.didCrash()}). Handled safely."
                                )
                                isRendererCrashed = true
                                view?.let { wv ->
                                    try {
                                        (wv.parent as? ViewGroup)?.removeView(wv)
                                        wv.destroy()
                                    } catch (_: Exception) {}
                                }
                                return true
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val uri = request?.url
                                if (uri != null && (uri.scheme == "http" || uri.scheme == "https")) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) { }
                                }
                                return true
                            }

                            @Deprecated("Deprecated in Java")
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) { }
                                }
                                return true
                            }

                            override fun shouldInterceptRequest(
                                view: WebView,
                                request: WebResourceRequest
                            ): WebResourceResponse? {
                                val url = request.url.toString()
                                val match = tileUrlRegex.find(url)
                                if (match != null) {
                                    val (z, x, y) = match.destructured
                                    val file = OfflineTileCache.tileFile(context, z.toInt(), x.toInt(), y.toInt())
                                    if (file.exists() && file.length() > 0) {
                                        return WebResourceResponse("image/png", null, FileInputStream(file))
                                    }
                                }
                                // Intercept cached destination images for map popups
                                if (url.startsWith("http://") || url.startsWith("https://")) {
                                    val imgFile = `in`.hiddenindia.app.data.OfflineImageCache.getImageFile(context, url)
                                    if (imgFile.exists() && imgFile.length() > 0) {
                                        val mimeType = when {
                                            url.contains(".png", ignoreCase = true) -> "image/png"
                                            url.contains(".webp", ignoreCase = true) -> "image/webp"
                                            url.contains(".svg", ignoreCase = true) -> "image/svg+xml"
                                            else -> "image/jpeg"
                                        }
                                        return WebResourceResponse(mimeType, null, FileInputStream(imgFile))
                                    }
                                }
                                val isOffline = (view.getTag(tagOfflineOnly) as? Boolean) == true
                                if (isOffline) {
                                    // When in offline mode, block all non-cached network requests
                                    return WebResourceResponse("text/plain", "UTF-8", java.io.ByteArrayInputStream(ByteArray(0)))
                                }
                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (url != null && !url.startsWith("https://hiddenindia.app")) {
                                    val html = getLeafletHtml(context, isDark)
                                    view?.loadDataWithBaseURL("https://hiddenindia.app/", html, "text/html", "UTF-8", null)
                                    return
                                }
                                setTag(tagIsPageLoaded, true)
                                view?.evaluateJavascript("resizeMap()", null)
                                val markersJson = getTag(tagMarkersJson) as? String
                                if (!markersJson.isNullOrEmpty()) {
                                    view?.evaluateJavascript("setMarkers($markersJson)", null)
                                }
                                val loc = getTag(tagUserLoc) as? Pair<*, *>
                                if (loc != null && loc.first is Double && loc.second is Double) {
                                    view?.evaluateJavascript("setUserLocation(${loc.first}, ${loc.second}, null)", null)
                                }
                                val target = getTag(tagFocusTarget) as? Pair<*, *>
                                if (target != null && target.first is Double && target.second is Double) {
                                    view?.evaluateJavascript("focusOn(${target.first}, ${target.second}, 15)", null)
                                }
                            }
                        }

                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onMarkerClick(id: String) { onMarkerClick(id) }
                        }, "Android")

                        val html = getLeafletHtml(context, isDark)
                        loadDataWithBaseURL("https://hiddenindia.app/", html, "text/html", "UTF-8", null)
                    }
                },
                update = { webView ->
                    if (isRendererCrashed) return@AndroidView

                    try {
                        val currentUrl = webView.url
                        if (currentUrl != null && !currentUrl.startsWith("https://hiddenindia.app")) {
                            val html = getLeafletHtml(context, isDark)
                            webView.loadDataWithBaseURL("https://hiddenindia.app/", html, "text/html", "UTF-8", null)
                        }

                        val lastDark = webView.getTag(tagIsDark) as? Boolean
                        if (lastDark != null && lastDark != isDark) {
                            webView.setTag(tagIsDark, isDark)
                            webView.evaluateJavascript("if (window.setTheme) { setTheme($isDark); }", null)
                        } else if (lastDark == null) {
                            webView.setTag(tagIsDark, isDark)
                        }

                        val json = JSONArray().apply {
                            markers.forEach { m ->
                                put(JSONObject().apply {
                                    put("id", m.id); put("lat", m.lat); put("lng", m.lng)
                                    put("name", m.name); put("kind", m.kind); put("isCenter", m.isCenter)
                                    put("image", m.image ?: JSONObject.NULL)
                                    put("description", m.description ?: JSONObject.NULL)
                                    put("category", m.category ?: JSONObject.NULL)
                                    put("insiderTip", m.insiderTip ?: JSONObject.NULL)
                                    put("bestTimeOfDay", m.bestTimeOfDay ?: JSONObject.NULL)
                                    put("estimatedCost", m.estimatedCost ?: JSONObject.NULL)
                                })
                            }
                        }.toString()

                        webView.setTag(tagMarkersJson, json)
                        webView.setTag(tagUserLoc, userLocation)
                        webView.setTag(tagFocusTarget, focusTarget)
                        webView.setTag(tagOfflineOnly, offlineOnly)

                        val isPageLoaded = webView.getTag(tagIsPageLoaded) as? Boolean ?: false
                        if (isPageLoaded && (currentUrl == null || currentUrl.startsWith("https://hiddenindia.app"))) {
                            webView.evaluateJavascript("setMarkers($json)", null)
                            userLocation?.let { (lat, lng) ->
                                webView.evaluateJavascript("setUserLocation($lat, $lng, null)", null)
                            }
                            focusTarget?.let { (lat, lng) ->
                                webView.evaluateJavascript("focusOn($lat, $lng, 15)", null)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MapViewer", "Error updating map view: ${e.message}")
                    }
                }
            )
        }
    }
}

private const val tagIsPageLoaded = 0x7f090001
private const val tagMarkersJson = 0x7f090002
private const val tagUserLoc = 0x7f090003
private const val tagFocusTarget = 0x7f090004
private const val tagIsDark = 0x7f090005
private const val tagOfflineOnly = 0x7f090006
