package ru.vinyarsky.englishmedia.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import ru.vinyarsky.englishmedia.models.data.db.EpisodeEntity

/**
 * Episode database access object
 */
@Dao
interface EpisodeDao {

    /**
     * Returns all episodes of a podcast
     *
     * @param podcastCode Podcast identifier
     * @return Full list of episodes
     */
    @Query("SELECT * FROM Episodes WHERE podcast_code = :podcastCode")
    suspend fun getAllByPodcastCode(podcastCode: String): List<EpisodeEntity>

    /**
     * Returns an episode by identifier
     *
     * @param code Episode identifier
     * @return Episode
     */
    @Query("SELECT * FROM Episodes WHERE code = :code")
    suspend fun get(code: String): EpisodeEntity?

    /**
     * Inserts a new episode
     *
     * @param episodeEntity Episode to insert
     */
    @Insert
    suspend fun insert(episodeEntity: EpisodeEntity)

    /**
     * Updates an existed episode
     *
     * @param episodeEntity Episode to update
     */
    @Update
    suspend fun update(episodeEntity: EpisodeEntity)

    /**
     * Updates a listening position of an episode
     *
     * @param code Episode identifier
     * @param newPositionSec New position (in seconds)
     */
    @Query("UPDATE Episodes SET current_position = :newPositionSec WHERE code = :code")
    suspend fun updatePosition(code: String, newPositionSec: Int)

    /**
     * Updates a listening status of an episode
     *
     * @param code Episode identifier
     * @param newStatus New listening status
     */
    @Query("UPDATE Episodes SET status = :newStatus WHERE code = :code")
    suspend fun updateStatus(code: String, newStatus: String)
}