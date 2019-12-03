package ru.vinyarsky.englishmedia.data.db

import androidx.room.Dao
import androidx.room.Query
import ru.vinyarsky.englishmedia.models.data.db.PodcastEntity

@Dao
interface PodcastDao {

    @Query("SELECT * FROM Podcasts")
    suspend fun getAllPodcasts(): List<PodcastEntity>


}