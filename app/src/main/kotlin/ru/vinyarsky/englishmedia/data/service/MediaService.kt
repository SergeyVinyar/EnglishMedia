package ru.vinyarsky.englishmedia.data.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import ru.vinyarsky.englishmedia.domain.gateways.PlayerGateway

// TODO Temporary solution. It's gonna be cozy home for MediaSession in the future.
class MediaService : Service() {

    private lateinit var playerGateway: PlayerGateway

    override fun onCreate() {
        super.onCreate()
        // TODO Switch to foreground
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {

        fun getIntent(context: Context) = Intent(context, MediaService::class.java)
    }
}