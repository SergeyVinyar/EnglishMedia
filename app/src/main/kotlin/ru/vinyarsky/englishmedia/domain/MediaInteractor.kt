package ru.vinyarsky.englishmedia.domain

import androidx.annotation.MainThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import ru.vinyarsky.englishmedia.domain.gateways.PlayerGateway
import ru.vinyarsky.englishmedia.domain.gateways.MediaServiceGateway
import ru.vinyarsky.englishmedia.domain.repository.EpisodeRepository
import ru.vinyarsky.englishmedia.models.domain.Episode
import ru.vinyarsky.englishmedia.models.domain.EpisodeStatus
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

class MediaInteractor(private val mediaServiceGateway: MediaServiceGateway,
                      private val playerGateway: PlayerGateway,
                      private val episodeRepository: EpisodeRepository) : PlayerGateway.Listener {

    @Volatile var playingEpisode: Episode? = null
        private set

    private val listeners: CopyOnWriteArrayList<Listener> = CopyOnWriteArrayList()

    init {
        playerGateway.addListener(this)
    }

    @MainThread
    suspend fun play(episodeCode: UUID) {
        episodeRepository.get(episodeCode)?.let {
            try {
                playingEpisode = it
                playerGateway.play(it.contentUrl, it.currentPositionSec)
                if (episodeRepository.updateStatusIfRequired(episodeCode, EpisodeStatus.LISTENING)) {
                    listeners.forEach { listener -> listener.onEpisodeStatusChanged(episodeCode, EpisodeStatus.LISTENING) }
                }
                listeners.forEach { listener -> listener.onEpisodeChanged(it) }
            } catch (e: Exception) {
                playerGateway.stop()
                throw e
            }
        }
    }

    @MainThread
    fun togglePlayStop() {
        playerGateway.togglePlayStop()
    }

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    private fun oopsSomethingGoesWrong() {
        try {
            playerGateway.stop()
        } catch (e: Exception) {
            playingEpisode = null
            throw e
        }
    }

    // region PlayerGateway.Listener

    override fun onNoNetwork() {
        oopsSomethingGoesWrong()
    }

    override fun onContentNotFound() {
        oopsSomethingGoesWrong()
    }

    override fun onResetDueError() {
        oopsSomethingGoesWrong()
    }

    override fun onPlay() {
        playingEpisode?.let {
            mediaServiceGateway.startServiceAsForeground()
            listeners.forEach { listener -> listener.onPlay(it.code) }
        }
    }

    override fun onPositionChanged(positionSec: Int) {
        playingEpisode?.let {
            GlobalScope.launch(Dispatchers.Main) {
                episodeRepository.updatePosition(it.code, positionSec)
                listeners.forEach { listener -> listener.onPositionChanged(it.code, positionSec) }
            }
        }
    }

    override fun onStop(positionSec: Int) {
        playingEpisode?.let {
            GlobalScope.launch(Dispatchers.Main) {
                mediaServiceGateway.stopService()
                episodeRepository.updatePosition(it.code, positionSec)
                listeners.forEach { listener -> listener.onStop(it.code) }
            }
        }
    }

    override fun onCompleted() {
        playingEpisode?.let {
            GlobalScope.launch(Dispatchers.Main) {
                if (episodeRepository.updateStatusIfRequired(it.code, EpisodeStatus.COMPLETED)) {
                    listeners.forEach { listener -> listener.onEpisodeStatusChanged(it.code, EpisodeStatus.COMPLETED) }
                }
                playingEpisode = null
            }
        }
    }

    // endregion PlayerGateway.Listener

    interface Listener {
        fun onPlay(episodeCode: UUID)
        fun onPositionChanged(episodeCode: UUID, positionSec: Int)
        fun onStop(episodeCode: UUID)
        fun onEpisodeChanged(episode: Episode)
        fun onEpisodeStatusChanged(episodeCode: UUID, newStatus: EpisodeStatus)
    }
}