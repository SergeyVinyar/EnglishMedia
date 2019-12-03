package ru.vinyarsky.englishmedia.models.domain

import java.util.*

data class Episode(val code: UUID,
                   val title: String,
                   val description: String,
                   val pageUrl: String,
                   val contentUrl: String,
                   val duration: Int,
                   val publishDate: Date,
                   val status: EpisodeStatus,
                   val currentPosition: Int)
