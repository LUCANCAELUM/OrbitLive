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
import kotlinx.serialization.json.encodeToStream
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

@Serializable
data class Country(val name: String, val flag: String)

@Serializable
data class TvData(
    val stations: List<Station>,
    val countries: Map<String, Country>,
    val countryOrder: List<String> = emptyList(),
    val hostHeaders: Map<String, Map<String, String>> = emptyMap()
)

@Serializable
data class TrendingStream(
    val station: Station,
    val streamerName: String,
    val streamTitle: String,
    val thumbnailUrl: String?,
    val viewerCount: Int,
    val category: String
)

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
    val bitrate: Int = 0,
    val clickcount: Int = 0,
    val clicktrend: Int = 0,
    val votes: Int = 0
)

object Repo {
    private const val TV_CACHE_TTL_MS = 6L * 60 * 60 * 1000
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private lateinit var cacheDirectory: File

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cache(Cache(File(cacheDirectory, "http"), 150L * 1024 * 1024))
            .addInterceptor { c ->
                c.proceed(c.request().newBuilder().header("User-Agent", "OrbitLive/1.0").build())
            }
            // Cache static iptv-org CDN data (6h) and radio-browser responses (5m)
            .addNetworkInterceptor { c ->
                val r = c.proceed(c.request())
                val host = c.request().url.host
                when {
                    host.endsWith("github.io") -> r.newBuilder()
                        .header("Cache-Control", "public, max-age=21600")
                        .removeHeader("Pragma")
                        .build()
                    host.endsWith("radio-browser.info") -> r.newBuilder()
                        .header("Cache-Control", "public, max-age=300")
                        .removeHeader("Pragma")
                        .build()
                    else -> r
                }
            }
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val fastClient: OkHttpClient by lazy {
        client.newBuilder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    /** Per-host headers (Referer / User-Agent) required by some streams. */
    val hostHeaders = ConcurrentHashMap<String, Map<String, String>>()
    private val trendingMemoryCache = ConcurrentHashMap<String, List<TrendingStream>>()

    @Volatile
    private var bestMirrorIndex = 0

    fun init(ctx: Context) {
        cacheDirectory = ctx.applicationContext.cacheDir
    }

    private fun tvSnapshotFile() = File(cacheDirectory, "tv_snapshot_v1.json")
    private fun trendingSnapshotFile() = File(cacheDirectory, "trending_snapshot_v1.json")

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun loadCachedTv(): TvData? = withContext(Dispatchers.IO) {
        val file = tvSnapshotFile()
        if (!file.exists()) return@withContext null
        runCatching {
            file.inputStream().buffered().use { stream ->
                json.decodeFromStream<TvData>(stream).also { data ->
                    if (data.hostHeaders.isNotEmpty()) {
                        hostHeaders.putAll(data.hostHeaders)
                    }
                }
            }
        }.getOrNull()
    }

    fun isTvCacheStale(): Boolean {
        val file = tvSnapshotFile()
        return !file.exists() || (System.currentTimeMillis() - file.lastModified()) > TV_CACHE_TTL_MS
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun saveCachedTv(data: TvData) = withContext(Dispatchers.IO) {
        runCatching {
            val tmp = File(cacheDirectory, "tv_snapshot_v1.tmp")
            tmp.outputStream().buffered().use { stream ->
                json.encodeToStream(TvData.serializer(), data, stream)
            }
            tmp.renameTo(tvSnapshotFile())
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun loadCachedTrending(): List<TrendingStream>? = withContext(Dispatchers.IO) {
        val file = trendingSnapshotFile()
        if (!file.exists()) return@withContext null
        runCatching {
            file.inputStream().buffered().use { stream ->
                json.decodeFromStream(ListSerializer(TrendingStream.serializer()), stream)
            }.also { list ->
                if (list.isNotEmpty()) {
                    trendingMemoryCache[cacheKey(null, null)] = list
                }
            }
        }.getOrNull()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun saveCachedTrending(list: List<TrendingStream>) = withContext(Dispatchers.IO) {
        runCatching {
            val tmp = File(cacheDirectory, "trending_snapshot_v1.tmp")
            tmp.outputStream().buffered().use { stream ->
                json.encodeToStream(ListSerializer(TrendingStream.serializer()), list, stream)
            }
            tmp.renameTo(trendingSnapshotFile())
        }
    }

    private fun cacheKey(category: String?, query: String?): String =
        "${category.orEmpty().lowercase()}|${query.orEmpty().trim().lowercase()}"

    fun getTrendingFromMemory(category: String?, query: String?): List<TrendingStream>? =
        trendingMemoryCache[cacheKey(category, query)]

    @OptIn(ExperimentalSerializationApi::class)
    private suspend inline fun <reified T> get(
        url: String,
        httpClient: OkHttpClient = client
    ): T = withContext(Dispatchers.IO) {
        try {
            httpClient.newCall(Request.Builder().url(url).build()).execute().use { r ->
                if (!r.isSuccessful) error("HTTP ${r.code}")
                val body = r.body ?: error("Empty response")
                json.decodeFromStream<T>(body.byteStream())
            }
        } catch (e: java.io.IOException) {
            // offline fallback: serve stale cache
            httpClient.newCall(
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

        val rawChannels = chD.await()
        val rawStreams = stD.await()
        val rawLogos = lgD.await()
        val rawCountries = coD.await()

        val data = withContext(Dispatchers.Default) {
            val streams = rawStreams.filter { it.channel != null }
            val parsedHeaders = HashMap<String, Map<String, String>>()
            streams.forEach { s ->
                if (s.referrer != null || s.userAgent != null) {
                    runCatching {
                        val host = java.net.URI(s.url).host ?: return@runCatching
                        val m = HashMap<String, String>()
                        s.referrer?.let { m["Referer"] = it }
                        s.userAgent?.let { m["User-Agent"] = it }
                        if (m.isNotEmpty()) {
                            parsedHeaders[host] = m
                            hostHeaders[host] = m
                        }
                    }
                }
            }
            val byCh = streams.groupBy { it.channel!! }
            val logos = HashMap<String, String>(rawLogos.size)
            rawLogos.forEach { l -> l.channel?.let { logos.putIfAbsent(it, l.url) } }

            fun rank(s: StDto): Int {
                val h = s.quality?.filter { it.isDigit() }?.toIntOrNull() ?: 480
                return abs(h - 720) // prefer ~720p: smooth on mobile networks
            }

            val list = rawChannels.asSequence()
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
            val countries = rawCountries.associate { it.code to Country(it.name, it.flag) }
            val countryOrder = list.groupingBy { it.country }.eachCount()
                .entries.sortedByDescending { it.value }.map { it.key }
            TvData(list, countries, countryOrder, parsedHeaders)
        }
        saveCachedTv(data)
        data
    }

    private val mirrors = listOf(
        "https://de1.api.radio-browser.info",
        "https://fi1.api.radio-browser.info",
        "https://nl1.api.radio-browser.info",
        "https://at1.api.radio-browser.info"
    )

    private fun orderedMirrors(): List<Pair<Int, String>> {
        val start = bestMirrorIndex
        return List(mirrors.size) { offset ->
            val idx = (start + offset) % mirrors.size
            idx to mirrors[idx]
        }
    }

    suspend fun loadRadio(country: String?, tag: String?, q: String?): List<Station> {
        var last: Exception? = null
        for ((idx, m) in orderedMirrors()) { // rotate starting from fastest known mirror
            try {
                val b = (m + "/json/stations/search").toHttpUrl().newBuilder()
                    .addQueryParameter("limit", "150")
                    .addQueryParameter("hidebroken", "true")
                    .addQueryParameter("order", "clickcount")
                    .addQueryParameter("reverse", "true")
                if (!country.isNullOrBlank()) b.addQueryParameter("countrycode", country)
                if (!tag.isNullOrBlank()) b.addQueryParameter("tag", tag)
                if (!q.isNullOrBlank()) b.addQueryParameter("name", q)
                val l = get<List<RadioDto>>(b.build().toString(), fastClient)
                bestMirrorIndex = idx
                return withContext(Dispatchers.Default) {
                    l.filter { it.urlResolved.isNotBlank() }.map {
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
                }
            } catch (e: Exception) {
                last = e
            }
        }
        throw last ?: IllegalStateException("No radio mirror reachable")
    }

    suspend fun loadTrending(category: String? = null, query: String? = null): List<TrendingStream> {
        val cleanQuery = query?.trim()?.takeIf { it.isNotEmpty() }
        val tagCandidates = when (category?.lowercase()) {
            null -> listOf(null)
            "gaming" -> listOf("gaming", "game", "chiptune")
            "just chatting" -> listOf("talk", "podcast", "speech")
            "sports" -> listOf("sport", "sports")
            else -> listOf(category.lowercase())
        }

        fun mapDto(dto: RadioDto, categoryOverride: String?): TrendingStream {
            val cleanName = dto.name.trim().ifBlank { "Live Stream" }
            val tagList = dto.tags.split(",")
                .map { t -> t.trim() }
                .filter { t -> t.isNotBlank() }
                .take(3)
            val qualitySub = listOf(
                dto.codec,
                if (dto.bitrate > 0) "${dto.bitrate}k" else ""
            ).filter { s -> s.isNotBlank() }.joinToString(" ")
            val station = Station(
                id = "r:" + dto.stationuuid,
                name = cleanName,
                logo = dto.favicon.ifBlank { null },
                country = dto.countrycode,
                categories = tagList,
                urls = listOf(dto.urlResolved),
                radio = true,
                sub = qualitySub
            )
            val viewers = maxOf(dto.clicktrend, dto.clickcount, dto.votes)
            val primaryCategory = categoryOverride ?: tagList.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: "Live"
            val title = listOf(
                tagList.firstOrNull()?.replaceFirstChar { it.uppercase() } ?: primaryCategory,
                qualitySub
            ).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Live broadcast" }
            return TrendingStream(
                station = station,
                streamerName = cleanName,
                streamTitle = title,
                thumbnailUrl = dto.favicon.ifBlank { null },
                viewerCount = viewers,
                category = primaryCategory
            )
        }

        var last: Exception? = null
        for (tag in tagCandidates) {
            for ((idx, m) in orderedMirrors()) {
                try {
                    val baseBuilder = (m + "/json/stations/search").toHttpUrl().newBuilder()
                        .addQueryParameter("limit", "30")
                        .addQueryParameter("hidebroken", "true")
                        .addQueryParameter("order", if (tag == null && cleanQuery == null) "clicktrend" else "clickcount")
                        .addQueryParameter("reverse", "true")
                    if (tag != null) baseBuilder.addQueryParameter("tag", tag)

                    val mapped = if (cleanQuery != null) {
                        coroutineScope {
                            val byNameD = async {
                                val u = baseBuilder.build().newBuilder().addQueryParameter("name", cleanQuery).build().toString()
                                runCatching { get<List<RadioDto>>(u, fastClient) }.getOrDefault(emptyList())
                            }
                            val byTagD = async {
                                val u = if (tag == null) {
                                    baseBuilder.build().newBuilder().addQueryParameter("tag", cleanQuery.lowercase()).build().toString()
                                } else {
                                    baseBuilder.build().newBuilder().addQueryParameter("tagList", "$tag,${cleanQuery.lowercase()}").build().toString()
                                }
                                runCatching { get<List<RadioDto>>(u, fastClient) }.getOrDefault(emptyList())
                            }
                            val byName = byNameD.await()
                            val byTag = byTagD.await()
                            withContext(Dispatchers.Default) {
                                (byName + byTag)
                                    .distinctBy { it.stationuuid }
                                    .filter { it.urlResolved.isNotBlank() }
                                    .map { mapDto(it, category) }
                                    .sortedByDescending { it.viewerCount }
                            }
                        }
                    } else {
                        val raw = get<List<RadioDto>>(baseBuilder.build().toString(), fastClient)
                        withContext(Dispatchers.Default) {
                            raw.filter { it.urlResolved.isNotBlank() }.map { mapDto(it, category) }
                        }
                    }

                    bestMirrorIndex = idx
                    if (mapped.isNotEmpty() || tag == tagCandidates.last()) {
                        trendingMemoryCache[cacheKey(category, cleanQuery)] = mapped
                        if (category == null && cleanQuery == null && mapped.isNotEmpty()) {
                            saveCachedTrending(mapped)
                        }
                        return mapped
                    }
                    break
                } catch (e: Exception) {
                    last = e
                }
            }
        }
        throw last ?: IllegalStateException("No trending stream mirror reachable")
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
