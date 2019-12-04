package ru.vinyarsky.englishmedia.domain.repository

import ru.vinyarsky.englishmedia.models.domain.Podcast
import java.util.*

/**
 * Podcast's local storage repository
 */
interface PodcastRepository {

    /**
     * Returns all podcasts
     *
     * @return Full list of podcasts
     */
    suspend fun getAll(): List<Podcast>

    /**
     * Returns a podcast by identifier
     *
     * @param code Podcast identifier
     * @return Podcast (null if not found)
     */
    suspend fun get(code: UUID): Podcast?

    /**
     * Saves a podcast
     *
     * @param podcast Podcast to insert or update
     */
    suspend fun save(podcast: Podcast)
}