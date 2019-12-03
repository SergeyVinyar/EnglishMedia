package ru.vinyarsky.englishmedia.domain.repository

import ru.vinyarsky.englishmedia.models.domain.Podcast
import java.util.*

interface PodcastRepository {

    suspend fun getAllPodcasts(): List<Podcast>

    suspend fun getPodcast(code: UUID): Podcast?

    suspend fun setSubscribed(value: Boolean)
}