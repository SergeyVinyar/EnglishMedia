package ru.vinyarsky.englishmedia.models.domain

import java.util.*

data class Podcast(val code: UUID,
                   val country: Country,
                   val level: PodcastLevel,
                   val title: String,
                   val description: String,
                   val imagePath: String,
                   val rssUrl: String,
                   val subscribed: Boolean)