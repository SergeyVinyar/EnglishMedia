package ru.vinyarsky.englishmedia.domain.repository

import ru.vinyarsky.englishmedia.models.domain.Episode
import ru.vinyarsky.englishmedia.models.domain.Podcast

interface RssRepository {

    suspend fun fetchEpisodes(podcast: Podcast): Sequence<Episode>
}