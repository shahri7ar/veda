package com.vibemusic.app.data.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * LRCLIB API service using kotlinx.serialization + HttpURLConnection.
 * No extra library dependency required.
 */
object LrclibApi {

    private const val BASE = "https://lrclib.net/api"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Exact fetch endpoint:
     * GET https://lrclib.net/api/get?artist_name=…&track_name=…&duration=…
     */
    suspend fun getExact(
        artist: String,
        track: String,
        durationSeconds: Long,
    ): Result<LrclibResponse> = runCatching {
        val url = "$BASE/get?" +
                "artist_name=${artist.encode()}" +
                "&track_name=${track.encode()}" +
                "&duration=$durationSeconds"
        fetch(url)
    }

    /**
     * Search endpoint:
     * GET https://lrclib.net/api/search?artist_name=…&track_name=…
     */
    suspend fun search(
        artist: String,
        track: String,
    ): Result<List<LrclibResponse>> = runCatching {
        val url = "$BASE/search?" +
                "artist_name=${artist.encode()}" +
                "&track_name=${track.encode()}"
        val raw = httpGet(url)
        json.decodeFromString<List<LrclibResponse>>(raw)
    }

    /** Fetch a single result by its LRCLIB track id. */
    suspend fun getById(id: Long): Result<LrclibResponse> = runCatching {
        fetch("$BASE/get?id=$id")
    }

    // ---------------------------------------------------------------
    //  Internal
    // ---------------------------------------------------------------

    private suspend fun fetch(url: String): LrclibResponse {
        val raw = httpGet(url)
        return json.decodeFromString(raw)
    }

    private suspend fun httpGet(urlStr: String): String = withContext(Dispatchers.IO) {
        val conn = java.net.URL(urlStr).openConnection() as java.net.HttpURLConnection
        try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 10_000
            conn.readTimeout = 15_000
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "VibeMusic/2.0 (Android)")
            val code = conn.responseCode
            if (code == 404) throw NoSuchElementException("Lyrics not found (404)")
            if (code !in 200..299) throw RuntimeException("HTTP $code")
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun String.encode(): String =
        java.net.URLEncoder.encode(this, "UTF-8")
}
