package com.orbit.live

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/** Keeps radio / TV audio alive in the background with a lock-screen & notification controller. */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        PlayerHolder.init(this)
        session = MediaSession.Builder(this, PlayerHolder.player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        session?.release()
        session = null
        super.onDestroy()
    }
}
