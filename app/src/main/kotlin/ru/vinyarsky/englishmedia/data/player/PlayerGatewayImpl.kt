package ru.vinyarsky.englishmedia.data.player

import android.net.Uri
import android.os.Handler
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import ru.vinyarsky.englishmedia.domain.gateways.PlayerGateway
import java.util.concurrent.CopyOnWriteArrayList

class PlayerGatewayImpl(exoPlayerFactory: () -> ExoPlayer,
                        handlerFactory: () -> Handler,
                        private val mediaSourceFactory: ProgressiveMediaSource.Factory) : PlayerGateway {

    override var playingUrl: Uri? = null

    private val exoPlayer: ExoPlayer = exoPlayerFactory()
    private val handler: Handler = handlerFactory()

    private val listeners: CopyOnWriteArrayList<PlayerGateway.Listener> = CopyOnWriteArrayList()

    private val exoPlayerListener = object : Player.EventListener {

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if (exoPlayer.playWhenReady) {
                when (playbackState) {
                    ExoPlayer.STATE_ENDED -> {
                        stop()
                        playingUrl = null
                        listeners.forEach { it.onCompleted() }
                    }
                    ExoPlayer.STATE_READY -> {
                        emitNewPosition.run()
                    }
                }
            }
        }

        override fun onPlayerError(error: ExoPlaybackException?) {
            stop()
            playingUrl = null
            listeners.forEach { it.onResetDueError() }
        }
    }

    private val emitNewPosition = object : Runnable {
        override fun run() {
            handler.removeCallbacks(this)

            val currentPositionSec = (exoPlayer.currentPosition / 1000).toInt()
            listeners.forEach { it.onPositionChanged(currentPositionSec) }

            if (exoPlayer.playWhenReady) {
                handler.postDelayed(this, (30 * 1000).toLong()) // 30 secs
            }
        }
    }

    init {
        exoPlayer.playWhenReady = false
        exoPlayer.addListener(exoPlayerListener)
    }

    override fun release() {
        exoPlayer.removeListener(exoPlayerListener)
        exoPlayer.release()
    }

    override fun play(url: Uri, startFromPositionSec: Int?) {
        if (url != playingUrl) {
            val mediaSource = mediaSourceFactory.createMediaSource(url)
            exoPlayer.prepare(mediaSource)
            playingUrl = url
        }

        if (startFromPositionSec != null) {
            exoPlayer.seekTo(startFromPositionSec * 1000L)
        }

        exoPlayer.playWhenReady = true
        listeners.forEach { it.onPlay() }
    }

    override fun stop() {
        exoPlayer.playWhenReady = false

        val currentPositionSec = (exoPlayer.currentPosition / 1000).toInt()
        listeners.forEach { it.onStop(currentPositionSec) }
        emitNewPosition.run()
    }

    override fun togglePlayStop() {
        playingUrl?.let {
            if (exoPlayer.playWhenReady) {
                stop()
            } else {
                play(it)
            }
        }
    }

    override fun addListener(listener: PlayerGateway.Listener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: PlayerGateway.Listener) {
        listeners.remove(listener)
    }
}