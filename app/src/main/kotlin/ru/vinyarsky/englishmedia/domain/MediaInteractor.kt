package ru.vinyarsky.englishmedia.domain

import ru.vinyarsky.englishmedia.domain.gateways.PlayerGateway
import ru.vinyarsky.englishmedia.domain.gateways.MediaServiceGateway
import ru.vinyarsky.englishmedia.domain.repository.PodcastRepository
import ru.vinyarsky.englishmedia.models.domain.Episode
import ru.vinyarsky.englishmedia.models.domain.Podcast

class MediaInteractor(private val mediaServiceGateway: MediaServiceGateway,
                      private val playerGateway: PlayerGateway,
                      private val podcastRepository: PodcastRepository) : PlayerGateway.Listener {

    var playingPodcast: Podcast? = null
        private set

    var playingEpisode: Episode? = null
        private set

    init {
        playerGateway.addListener(this)
    }

    fun play(podcast: Podcast, episode: Episode) {
        mediaServiceGateway.startServiceAsForeground()
        //playerGateway.play(episode.contentUrl, episode.currentPosition)
        // onEpisodeChanged(playingEpisode)
//        Episode.setStatusListeningIfRequired(this@MediaService.dbHelper, url.toString())
  //      this.broadcastEmitEpisodeStatusChanged()

    }

    fun togglePlayStop() {
        playerGateway.togglePlayStop()
    }

    fun addListener(listener: Listener) {

    }

    fun removeListener(listener: Listener) {

    }

    // region PlayerGateway.Listener

    override fun onNoNetwork() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onContentNotFound() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onResetDueError() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPlay() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPositionChanged(positionSec: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onStop(positionSec: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCompleted() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    // endregion PlayerGateway.Listener


    interface Listener {
        fun onPlay()
        fun onPositionChanged(positionSec: Int)
        fun onStop(positionSec: Int)
        fun onCompleted()
        fun onEpisodeChanged(episode: Episode)
        fun onEpisodeStatusChanged(episode: Episode)
    }
}