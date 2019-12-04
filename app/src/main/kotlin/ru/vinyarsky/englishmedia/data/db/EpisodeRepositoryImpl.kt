package ru.vinyarsky.englishmedia.data.db

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.vinyarsky.englishmedia.core.Converter
import ru.vinyarsky.englishmedia.domain.repository.EpisodeRepository
import ru.vinyarsky.englishmedia.models.data.db.EpisodeEntity
import ru.vinyarsky.englishmedia.models.domain.Episode
import ru.vinyarsky.englishmedia.models.domain.EpisodeStatus
import java.util.*

/**
 * Episode's local storage repository
 *
 * @property database Database
 * @property converter Converter from a db-entity to a domain model (and vice versa)
 */
class EpisodeRepositoryImpl(private val database: EMDatabase,
                            private val converter: Converter<EpisodeEntity, Episode>) : EpisodeRepository {

    private val episodeDao = database.getEpisodeDao()

    override suspend fun getAllByPodcastCode(podcastCode: UUID): List<Episode> = withContext(Dispatchers.Default) {
        episodeDao.getAllByPodcastCode(podcastCode.toString()).mapNotNull { converter.convert(it) }
    }

    override suspend fun get(code: UUID): Episode? = withContext(Dispatchers.Default) {
        episodeDao.get(code.toString())?.let { converter.convert(it) }
    }

    override suspend fun save(episode: Episode) = withContext(Dispatchers.Default) {
        converter.reverse(episode)?.also {
            database.withTransaction {
                if (episodeDao.get(episode.code.toString()) != null) {
                    episodeDao.update(it)
                } else {
                    episodeDao.insert(it)
                }
            }
        }
        Unit
    }

    override suspend fun updatePosition(code: UUID, newPositionSec: Int) = withContext(Dispatchers.Default) {
        episodeDao.updatePosition(code.toString(), newPositionSec)
    }

    override suspend fun updateStatusIfRequired(code: UUID, newStatus: EpisodeStatus): Boolean = withContext(Dispatchers.Default) {
        // Completed status is a final one - no further changing allowed
        val codeAsString = code.toString()
        val newStatusAsString = newStatus.toString()
        database.withTransaction {
            val currentStatus = episodeDao.getStatus(codeAsString)
            if (newStatusAsString != currentStatus && currentStatus != EpisodeStatus.COMPLETED.toString()) {
                episodeDao.updateStatus(code.toString(), newStatusAsString)
                true
            } else {
                false
            }
        }
    }
}