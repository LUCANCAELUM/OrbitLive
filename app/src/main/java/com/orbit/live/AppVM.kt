package com.orbit.live

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Status { Idle, Loading, Playing, Error }

class AppVM(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    val player get() = PlayerHolder.player

    // ---- catalogue ----
    var tv by mutableStateOf<List<Station>>(emptyList())
    var countries by mutableStateOf<Map<String, Country>>(emptyMap())
    var countryOrder by mutableStateOf<List<String>>(emptyList())
    var loading by mutableStateOf(true)
    var error by mutableStateOf<String?>(null)

    // ---- navigation / filters ----
    var tab by mutableIntStateOf(0)
    var query by mutableStateOf("")
    var country by mutableStateOf<String?>(null)
    var category by mutableStateOf<String?>(null)

    var radio by mutableStateOf<List<Station>>(emptyList())
    var radioLoading by mutableStateOf(false)
    var radioError by mutableStateOf<String?>(null)
    var radioQuery by mutableStateOf("")
    var radioCountry by mutableStateOf<String?>(null)
    var radioTag by mutableStateOf<String?>(null)
    var lastRadioFilterKey by mutableStateOf<String?>(null)
        private set

    var trending by mutableStateOf<List<TrendingStream>>(emptyList())
    var trendingLoading by mutableStateOf(false)
    var trendingError by mutableStateOf<String?>(null)
    var trendingQuery by mutableStateOf("")
    var trendingCategory by mutableStateOf<String?>(null)

    // ---- library ----
    var favs by mutableStateOf(prefs.favs())
    var recents by mutableStateOf(prefs.recents())

    // ---- playback ----
    var current by mutableStateOf<Station?>(null)
    var streamIdx by mutableIntStateOf(0)
    var status by mutableStateOf(Status.Idle)
    var isPlaying by mutableStateOf(false)
    var showPlayer by mutableStateOf(false)
    var inPip by mutableStateOf(false)
    var surf by mutableStateOf(false)
    var sleepMinutes by mutableIntStateOf(0)

    private var queue: List<Station> = emptyList()
    private var qIndex = 0
    private var surfJob: Job? = null
    private var sleepJob: Job? = null
    private var radioJob: Job? = null
    private var trendingJob: Job? = null
    private var trendingSearchJob: Job? = null

    @Suppress("unused")
    private val controller = MediaController.Builder(
        app, SessionToken(app, ComponentName(app, PlaybackService::class.java))
    ).buildAsync() // binds the media session service -> background playback + notification

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> if (current != null) status = Status.Loading
                    Player.STATE_READY -> status = Status.Playing
                }
            }

            override fun onIsPlayingChanged(p: Boolean) {
                isPlaying = p
            }

            override fun onPlayerError(e: PlaybackException) {
                if (e.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                    player.seekToDefaultPosition()
                    player.prepare()
                } else {
                    tryNextSource()
                }
            }
        })
        bootstrap()
    }

    private fun bootstrap() {
        viewModelScope.launch {
            val cachedTvD = async { Repo.loadCachedTv() }
            val cachedTrendingD = async { Repo.loadCachedTrending() }

            val cachedTv = cachedTvD.await()
            if (cachedTv != null) {
                tv = cachedTv.stations
                countries = cachedTv.countries
                countryOrder = cachedTv.countryOrder
                loading = false
            }

            val cachedTrending = cachedTrendingD.await()
            if (cachedTrending != null) {
                trending = cachedTrending
                trendingLoading = false
            }

            if (cachedTv == null || Repo.isTvCacheStale()) {
                loadTv(silent = cachedTv != null)
            }
            loadTrending(silent = cachedTrending != null)
        }
    }

    fun loadTv(silent: Boolean = false) {
        if (!silent) {
            loading = true
            error = null
        }
        viewModelScope.launch {
            try {
                val d = Repo.loadTv()
                tv = d.stations
                countries = d.countries
                countryOrder = d.countryOrder
                error = null
            } catch (e: Exception) {
                if (!silent || tv.isEmpty()) {
                    error = e.message ?: "Network error"
                }
            }
            loading = false
        }
    }

    fun loadRadio(force: Boolean = false) {
        val key = "${radioCountry.orEmpty()}|${radioTag.orEmpty()}|${radioQuery.trim()}"
        if (!force && key == lastRadioFilterKey && radio.isNotEmpty()) return
        radioJob?.cancel()
        radioJob = viewModelScope.launch {
            radioLoading = true
            radioError = null
            try {
                radio = Repo.loadRadio(radioCountry, radioTag, radioQuery.trim())
                lastRadioFilterKey = key
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                radioError = e.message ?: "Network error"
            }
            radioLoading = false
        }
    }

    fun updateTrendingQuery(query: String) {
        if (trendingQuery == query) return
        trendingQuery = query
        trendingSearchJob?.cancel()
        val cached = Repo.getTrendingFromMemory(trendingCategory, query)
        if (cached != null) {
            trending = cached
            trendingLoading = false
            trendingError = null
            return
        }
        trendingSearchJob = viewModelScope.launch {
            if (query.isNotBlank()) delay(300)
            loadTrending()
        }
    }

    fun toggleTrendingCategory(category: String?) {
        val next = if (category == null || trendingCategory == category) null else category
        if (next == trendingCategory && trending.isNotEmpty()) return
        trendingCategory = next
        val cached = Repo.getTrendingFromMemory(next, trendingQuery)
        if (cached != null) {
            trending = cached
            trendingLoading = false
            trendingError = null
            return
        }
        trending = emptyList()
        loadTrending()
    }

    fun loadTrending(silent: Boolean = false) {
        trendingJob?.cancel()
        val selectedCategory = trendingCategory
        val currentQuery = trendingQuery.trim()
        trendingJob = viewModelScope.launch {
            if (!silent) {
                trendingLoading = true
                trendingError = null
            }
            try {
                trending = Repo.loadTrending(selectedCategory, currentQuery)
                trendingError = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!silent || trending.isEmpty()) {
                    trendingError = e.message ?: "Network error"
                }
            }
            trendingLoading = false
        }
    }

    // ---- playback control ----
    fun play(s: Station, list: List<Station> = listOf(s)) {
        queue = list
        qIndex = list.indexOfFirst { it.id == s.id }.coerceAtLeast(0)
        begin(s, 0)
        showPlayer = true
    }

    private fun begin(s: Station, idx: Int) {
        current = s
        streamIdx = idx
        status = Status.Loading
        addRecent(s)
        val url = s.urls.getOrNull(idx) ?: run {
            status = Status.Error
            return
        }
        val seg = url.substringBefore('?').substringAfterLast('/')
        val mime = when {
            url.contains(".mpd") -> MimeTypes.APPLICATION_MPD
            url.contains("m3u8") || (!s.radio && !seg.contains('.')) -> MimeTypes.APPLICATION_M3U8
            else -> null
        }
        val meta = MediaMetadata.Builder()
            .setTitle(s.name)
            .setArtist(countries[s.country]?.name ?: s.country)
            .setArtworkUri(s.logo?.let { Uri.parse(it) })
            .build()
        val item = MediaItem.Builder()
            .setUri(url)
            .setMimeType(mime)
            .setMediaMetadata(meta)
            .build()
        player.setMediaItem(item)
        player.prepare()
        player.playWhenReady = true
    }

    private fun tryNextSource() {
        val s = current ?: return
        if (streamIdx + 1 < s.urls.size) {
            begin(s, streamIdx + 1) // automatic failover
        } else {
            status = Status.Error
            if (surf) zap(1)
        }
    }

    fun nextSource() {
        val s = current ?: return
        if (s.urls.isEmpty()) return
        begin(s, (streamIdx + 1) % s.urls.size)
    }

    fun zap(dir: Int) {
        if (queue.size < 2) return
        qIndex = (qIndex + dir + queue.size) % queue.size
        begin(queue[qIndex], 0)
    }

    fun togglePlay() {
        if (status == Status.Error) {
            current?.let { begin(it, 0) }
        } else if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun stop() {
        player.stop()
        player.clearMediaItems()
        current = null
        showPlayer = false
        status = Status.Idle
    }

    fun surprise() {
        val pool = when (tab) {
            1 -> radio
            2 -> trending.map { it.station }
            else -> tv
        }.ifEmpty { tv }
        if (pool.isEmpty()) return
        val l = pool.shuffled().take(300)
        play(l.first(), l)
    }

    fun toggleSurf() {
        surf = !surf
        surfJob?.cancel()
        if (surf) {
            surfJob = viewModelScope.launch {
                while (true) {
                    delay(25_000)
                    if (status == Status.Playing) zap(1)
                }
            }
        }
    }

    fun setSleep(min: Int) {
        sleepJob?.cancel()
        sleepMinutes = min
        if (min > 0) {
            sleepJob = viewModelScope.launch {
                delay(min * 60_000L)
                player.pause()
                sleepMinutes = 0
            }
        }
    }

    // ---- library ----
    fun isFav(id: String) = favs.any { it.id == id }

    fun toggleFav(s: Station) {
        favs = if (isFav(s.id)) favs.filterNot { it.id == s.id } else listOf(s) + favs
        prefs.saveFavs(favs)
    }

    private fun addRecent(s: Station) {
        recents = (listOf(s) + recents.filterNot { it.id == s.id }).take(20)
        prefs.saveRecents(recents)
    }
}
