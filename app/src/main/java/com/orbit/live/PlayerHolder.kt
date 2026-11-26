package com.orbit.live

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

/** One process-wide player shared by the UI and the background PlaybackService. */
object PlayerHolder {
    lateinit var player: ExoPlayer

    @OptIn(UnstableApi::class)
    @Synchronized
    fun init(ctx: Context) {
        if (::player.isInitialized) return
        val http = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(10_000)
            .setUserAgent("Mozilla/5.0 (Linux; Android 14) OrbitLive/1.0")
        val ds = ResolvingDataSource.Factory(http) { spec ->
            val h = Repo.hostHeaders[spec.uri.host ?: ""]
            if (h != null) spec.withAdditionalHeaders(h) else spec
        }
        val load = DefaultLoadControl.Builder()
            .setBufferDurationsMs(8_000, 30_000, 1_500, 3_000) // fast channel start, low memory
            .build()
        player = ExoPlayer.Builder(ctx.applicationContext)
            .setMediaSourceFactory(DefaultMediaSourceFactory(ds))
            .setLoadControl(load)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
    }
}
