package ru.vinyarsky.englishmedia.domain.repository

import ru.vinyarsky.englishmedia.models.domain.Episode
import ru.vinyarsky.englishmedia.models.domain.EpisodeStatus
import java.util.*

/**
 * Episode's local storage repository
 */
interface EpisodeRepository {

    /**
     * Returns all episodes for a podcast
     *
     * @param podcastCode Podcast identifier
     * @return Full episodes list for a given podcast
     */
    suspend fun getAllByPodcastCode(podcastCode: UUID): List<Episode>

    /**
     * Returns an episode
     *
     * @param code Episode identifier
     * @return Episode (null if not found)
     */
    suspend fun get(code: UUID): Episode?

    /**
     * Saves an episode
     *
     * @param episode Episode to insert or update
     */
    suspend fun save(episode: Episode)

    /**
     * Updates a listening position
     *
     * @param code Episode identifier
     * @param newPositionSec New listening position (in seconds)
     */
    suspend fun updatePosition(code: UUID, newPositionSec: Int)

    /**
     * Updates a listening status (with a bit of logic)
     *
     * @param code Episode identifier
     * @param newStatus New listening status
     * @return true if status was actually changed
     */
    suspend fun updateStatusIfRequired(code: UUID, newStatus: EpisodeStatus): Boolean
}