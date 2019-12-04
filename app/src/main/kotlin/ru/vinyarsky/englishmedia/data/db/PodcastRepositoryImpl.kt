package ru.vinyarsky.englishmedia.data.db

import androidx.room.withTransaction
import kotlinx.coroutines.*
import ru.vinyarsky.englishmedia.core.Converter
import ru.vinyarsky.englishmedia.domain.repository.PodcastRepository
import ru.vinyarsky.englishmedia.models.data.db.PodcastEntity
import ru.vinyarsky.englishmedia.models.domain.Podcast
import java.util.*

/**
 * Podcast's local storage repository
 *
 * @property database Database
 * @property converter Converter from a db-entity to a domain model (and vice versa)
 */
class PodcastRepositoryImpl(private val database: EMDatabase,
                            private val converter: Converter<PodcastEntity, Podcast>) : PodcastRepository {

    private val podcastDao = database.getPoscastDao()

    override suspend fun getAll(): List<Podcast> = withContext(Dispatchers.Default) {
        podcastDao.getAll().mapNotNull { converter.convert(it) }
    }

    override suspend fun get(code: UUID): Podcast? = withContext(Dispatchers.Default) {
        podcastDao.get(code.toString())?.let { converter.convert(it) }
    }

    override suspend fun save(podcast: Podcast) = withContext(Dispatchers.Default) {
        converter.reverse(podcast)?.also {
            database.withTransaction {
                if (podcastDao.get(podcast.code.toString()) != null) {
                    podcastDao.update(it)
                } else {
                    podcastDao.insert(it)
                }
            }
        }
        Unit
    }
}