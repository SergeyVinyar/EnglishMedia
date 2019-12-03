package ru.vinyarsky.englishmedia.data.db

import kotlinx.coroutines.*
import ru.vinyarsky.englishmedia.core.Converter
import ru.vinyarsky.englishmedia.domain.repository.PodcastRepository
import ru.vinyarsky.englishmedia.models.data.db.PodcastEntity
import ru.vinyarsky.englishmedia.models.domain.Podcast
import java.util.*

class PodcastRepositoryImpl(private val database: EMDatabase,
                            private val converter: Converter<PodcastEntity, Podcast>) : PodcastRepository {

    override suspend fun getAllPodcasts() = withContext(Dispatchers.Default) {
        database.getPoscastDao().getAllPodcasts()
                .mapNotNull { converter.convert(it) }
    }

    override suspend fun getPodcast(code: UUID): Podcast? {

        return null
    }

    override suspend fun setSubscribed(value: Boolean) {

    }
}