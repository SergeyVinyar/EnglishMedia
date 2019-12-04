package ru.vinyarsky.englishmedia.models.domain

import java.util.*

/**
 * Episode domain model
 *
 * @property code Unique identifier
 * @property country Origin country of a podcast
 * @property level Level of difficulty
 * @property title Title
 * @property description Description
 * @property imagePath Logo image
 * @property rssUrl RSS feed URL
 * @property subscribed Flag of subscription (not implemented yet)
 */
data class Podcast(val code: UUID,
                   val country: Country,
                   val level: PodcastLevel,
                   val title: String,
                   val description: String?,
                   val imagePath: String?,
                   val rssUrl: String,
                   val subscribed: Boolean)