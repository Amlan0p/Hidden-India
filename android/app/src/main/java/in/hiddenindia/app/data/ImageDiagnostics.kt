package `in`.hiddenindia.app.data

import android.util.Log
import coil.decode.DataSource
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * Real-time diagnostics for image loading performance, caching, and reliability.
 * Tracks memory/disk cache hits, proxy routing, retries, and failure reasons
 * without logging any secrets, credentials, or personal information.
 */
object ImageDiagnostics {
    private const val TAG = "ImageDiagnostics"

    val totalRequests = AtomicInteger(0)
    val memoryCacheHits = AtomicInteger(0)
    val diskCacheHits = AtomicInteger(0)
    val networkSuccesses = AtomicInteger(0)

    val proxyRequests = AtomicInteger(0)
    val proxySuccesses = AtomicInteger(0)
    val proxyFallbacks = AtomicInteger(0)

    val retryAttempts = AtomicInteger(0)
    val retrySuccesses = AtomicInteger(0)

    val http403Count = AtomicInteger(0)
    val http404Count = AtomicInteger(0)
    val http429Count = AtomicInteger(0)
    val timeoutCount = AtomicInteger(0)
    val otherFailureCount = AtomicInteger(0)

    private val recentEvents = ConcurrentLinkedQueue<String>()
    private const val MAX_EVENTS = 120

    fun recordSuccess(url: String, dataSource: DataSource) {
        totalRequests.incrementAndGet()
        when (dataSource) {
            DataSource.MEMORY, DataSource.MEMORY_CACHE -> memoryCacheHits.incrementAndGet()
            DataSource.DISK -> diskCacheHits.incrementAndGet()
            DataSource.NETWORK -> networkSuccesses.incrementAndGet()
        }
        addEvent("SUCCESS [${dataSource.name}] url=${shortenUrl(url)}")
    }

    fun recordProxySuccess(url: String) {
        proxyRequests.incrementAndGet()
        proxySuccesses.incrementAndGet()
        addEvent("PROXY_HIT url=${shortenUrl(url)}")
    }

    fun recordProxyFallback(url: String, statusCode: Int, reason: String? = null) {
        proxyRequests.incrementAndGet()
        proxyFallbacks.incrementAndGet()
        addEvent("PROXY_FALLBACK status=$statusCode reason=$reason url=${shortenUrl(url)}")
    }

    fun recordFailure(url: String, statusCode: Int, message: String?) {
        totalRequests.incrementAndGet()
        when (statusCode) {
            403 -> http403Count.incrementAndGet()
            404, 410 -> http404Count.incrementAndGet()
            429 -> http429Count.incrementAndGet()
            -1, 408, 504 -> timeoutCount.incrementAndGet()
            else -> otherFailureCount.incrementAndGet()
        }
        Log.w(TAG, "IMAGE_FAIL status=$statusCode reason=$message url=${shortenUrl(url)}")
        addEvent("FAIL status=$statusCode reason=$message url=${shortenUrl(url)}")
    }

    fun recordRetry(url: String, attempt: Int, success: Boolean) {
        retryAttempts.incrementAndGet()
        if (success) retrySuccesses.incrementAndGet()
        addEvent("RETRY attempt=$attempt success=$success url=${shortenUrl(url)}")
    }

    private fun addEvent(event: String) {
        recentEvents.add("[${System.currentTimeMillis() % 1000000}] $event")
        while (recentEvents.size > MAX_EVENTS) {
            recentEvents.poll()
        }
    }

    fun shortenUrl(url: String): String {
        return if (url.length <= 60) url
        else "${url.take(35)}...${url.takeLast(20)}"
    }

    fun getSummary(): String {
        val reqs = totalRequests.get().coerceAtLeast(1)
        val hitRate = ((memoryCacheHits.get() + diskCacheHits.get()).toDouble() / reqs * 100.0)
        return """
            ==================== IMAGE DIAGNOSTICS SUMMARY ====================
            Total Requests: ${totalRequests.get()}
            Memory Cache Hits: ${memoryCacheHits.get()}
            Disk Cache Hits: ${diskCacheHits.get()}
            Combined Cache Hit Rate: ${"%.1f".format(hitRate)}%
            Direct Network Successes: ${networkSuccesses.get()}
            Proxy Requests: ${proxyRequests.get()} (Success: ${proxySuccesses.get()}, Fallbacks: ${proxyFallbacks.get()})
            Automatic Retries: ${retryAttempts.get()} (Success: ${retrySuccesses.get()})
            Errors Encountered:
              • HTTP 403 (Forbidden): ${http403Count.get()}
              • HTTP 404 (Not Found): ${http404Count.get()}
              • HTTP 429 (Rate Limit): ${http429Count.get()}
              • Timeouts / Connection: ${timeoutCount.get()}
              • Other Errors: ${otherFailureCount.get()}
            ===================================================================
        """.trimIndent()
    }

    fun logSummary() {
        Log.i(TAG, getSummary())
    }
}
