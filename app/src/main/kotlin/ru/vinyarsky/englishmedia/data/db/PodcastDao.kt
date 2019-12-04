package ru.vinyarsky.englishmedia.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import ru.vinyarsky.englishmedia.models.data.db.PodcastEntity

/**
 * Podcast database access object
 */
@Dao
interface PodcastDao {

    /**
     * Returns all podcasts
     *
     * @return Full list of podcasts
     */
    @Query("SELECT * FROM Podcasts")
    suspend fun getAll(): List<PodcastEntity>

    /**
     * Returns a podcast by its identifier
     *
     * @return Podcast
     */
    @Query("SELECT * FROM Podcasts WHERE code = :code")
    suspend fun get(code: String): PodcastEntity?

    /**
     * Inserts a new podcast
     *
     * @param podcast Podcast to insert
     */
    @Insert
    suspend fun insert(podcast: PodcastEntity)

    /**
     * Updates an existed podcast
     *
     * @param podcast Podcast to update
     */
    @Update
    suspend fun update(podcast: PodcastEntity)
}