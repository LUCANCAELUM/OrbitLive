package com.orbit.live

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@Serializable
data class Station(
    val id: String,
    val name: String,
    val logo: String? = null,
    val country: String = "",
    val categories: List<String> = emptyList(),
    val urls: List<String> = emptyList(),
    val radio: Boolean = false,
    val sub: String = ""
)

data class Country(val name: String, val flag: String)
data class TvData(val stations: List<Station>, val countries: Map<String, Country>)

@Serializable
private data class ChDto(
    val id: String,
    val name: String,
    val country: String = "",
    val categories: List<String> = emptyList(),
    @SerialName("is_nsfw") val isNsfw: Boolean = false,
    val closed: String? = null,
    val logo: String? = null
)

@Serializable
private data class StDto(
    val channel: String? = null,
    val url: String,
    val quality: String? = null,
    @SerialName("user_agent") val userAgent: String? = null,
    val referrer: String? = null
)

@Serializable
private data class LogoDto(
    val channel: String? = null,
    val url: String
)

@Serializable
private data class CountryDto(
    val name: String,
    val code: String,
    val flag: String = ""
)

@Serializable
private data class RadioDto(
    val stationuuid: String,
    val name: String = "",
    @SerialName("url_resolved") val urlResolved: String = "",
    val favicon: String = "",
    val countrycode: String = "",
    val tags: String = "",
    val codec: String = "",
    val bitrate: Int = 0
)

object Repo {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    lateinit var client: OkHttpClient

    /** Per-host headers (Referer / User-Agent) required by some streams. */
    val hostHeaders = ConcurrentHashMap<String, Map<String, String>>()

    fun init(ctx: Context) {
        client = OkHttpClient.Builder()
            .cache(Cache(File(ctx.cacheDir, "http"), 150L * 1024 * 1024))
            .addInterceptor { c ->
                c.proceed(c.request().newBuilder().header("User-Agent", "OrbitLive/1.0").build())
            }
            // iptv-org data is static JSON on a CDN -> cache aggressively (6h).
            .addNetworkInterceptor { c ->
                val r = c.proceed(c.request())
                if (c.request().url.host.endsWith("github.io")) {
                    r.newBuilder()
                        .header("Cache-Control", "public, max-age=21600")
                        .removeHeader("Pragma")
                        .build()
                } else {
                    r
                }
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend inline fun <reified T> get(url: String): T = withContext(Dispatchers.IO) {
        try {
            client.newCall(Request.Builder().url(url).build()).execute().use { r ->
                if (!r.isSuccessful) error("HTTP ${r.code}")
                val body = r.body ?: error("Empty response")
                json.decodeFromStream<T>(body.byteStream())
            }
        } catch (e: java.io.IOException) {
            // offline fallback: serve stale cache
            client.newCall(
                Request.Builder().url(url).cacheControl(CacheControl.FORCE_CACHE).build()
            ).execute().use { r ->
                if (!r.isSuccessful) throw e
                val body = r.body ?: throw e
                json.decodeFromStream<T>(body.byteStream())
            }
        }
    }

    suspend fun loadTv(): TvData = coroutineScope {
        val base = "https://iptv-org.github.io/api/"
        val chD = async { get<List<ChDto>>(base + "channels.json") }
        val stD = async { get<List<StDto>>(base + "streams.json") }
        val lgD = async { runCatching { get<List<LogoDto>>(base + "logos.json") }.getOrDefault(emptyList()) }
        val coD = async { runCatching { get<List<CountryDto>>(base + "countries.json") }.getOrDefault(emptyList()) }

        val streams = stD.await().filter { it.channel != null }
        streams.forEach { s ->
            if (s.referrer != null || s.userAgent != null) {
                runCatching {
                    val host = java.net.URI(s.url).host ?: return@runCatching
                    val m = HashMap<String, String>()
                    s.referrer?.let { m["Referer"] = it }
                    s.userAgent?.let { m["User-Agent"] = it }
                    hostHeaders[host] = m
                }
            }
        }
        val byCh = streams.groupBy { it.channel!! }
        val logos = HashMap<String, String>()
        lgD.await().forEach { l -> l.channel?.let { logos.putIfAbsent(it, l.url) } }

        fun rank(s: StDto): Int {
            val h = s.quality?.filter { it.isDigit() }?.toIntOrNull() ?: 480
            return abs(h - 720) // prefer ~720p: smooth on mobile networks
        }

        val list = chD.await().asSequence()
            .filter { !it.isNsfw && it.closed == null && "xxx" !in it.categories && byCh.containsKey(it.id) }
            .map { c ->
                Station(
                    id = c.id,
                    name = c.name,
                    logo = c.logo ?: logos[c.id],
                    country = c.country,
                    categories = c.categories,
                    urls = byCh[c.id]!!.sortedBy { rank(it) }.map { it.url }.distinct()
                )
            }
            .sortedBy { it.name.lowercase() }
            .toList()
        val countries = coD.await().associate { it.code to Country(it.name, it.flag) }
        TvData(list, countries)
    }

    private val mirrors = listOf(
        "https://de1.api.radio-browser.info",
        "https://fi1.api.radio-browser.info",
        "https://nl1.api.radio-browser.info",
        "https://at1.api.radio-browser.info"
    )

    suspend fun loadRadio(country: String?, tag: String?, q: String?): List<Station> {
        var last: Exception? = null
        for (m in mirrors) { // client-side load balancing across community mirrors
            try {
                val b = (m + "/json/stations/search").toHttpUrl().newBuilder()
                    .addQueryParameter("limit", "300")
                    .addQueryParameter("hidebroken", "true")
                    .addQueryParameter("order", "clickcount")
                    .addQueryParameter("reverse", "true")
                if (!country.isNullOrBlank()) b.addQueryParameter("countrycode", country)
                if (!tag.isNullOrBlank()) b.addQueryParameter("tag", tag)
                if (!q.isNullOrBlank()) b.addQueryParameter("name", q)
                val l = get<List<RadioDto>>(b.build().toString())
                return l.filter { it.urlResolved.isNotBlank() }.map {
                    Station(
                        id = "r:" + it.stationuuid,
                        name = it.name.trim().ifBlank { "Radio" },
                        logo = it.favicon.ifBlank { null },
                        country = it.countrycode,
                        categories = it.tags.split(",").map { t -> t.trim() }.filter { t -> t.isNotBlank() }.take(3),
                        urls = listOf(it.urlResolved),
                        radio = true,
                        sub = listOf(
                            it.codec,
                            if (it.bitrate > 0) "${it.bitrate}k" else ""
                        ).filter { s -> s.isNotBlank() }.joinToString(" ")
                    )
                }
            } catch (e: Exception) {
                last = e
            }
        }
        throw last ?: IllegalStateException("No radio mirror reachable")
    }
}

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("orbit", Context.MODE_PRIVATE)
    private val ser = ListSerializer(Station.serializer())
    private val json = Json { ignoreUnknownKeys = true }

    private fun read(k: String): List<Station> =
        runCatching { json.decodeFromString(ser, sp.getString(k, "[]")!!) }.getOrDefault(emptyList())

    private fun write(k: String, l: List<Station>) =
        sp.edit().putString(k, json.encodeToString(ser, l)).apply()

    fun favs() = read("favs")
    fun saveFavs(l: List<Station>) = write("favs", l)
    fun recents() = read("recents")
    fun saveRecents(l: List<Station>) = write("recents", l)
}
