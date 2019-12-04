package ru.vinyarsky.englishmedia.models.domain

import java.util.*

/**
 * Episode domain model
 *
 * @property code Unique identifier
 * @property podcastCode Unique identifier of a podcast, this episode belongs to
 * @property episodeGuid Episode identifier in an rss feed
 * @property title Title
 * @property description Description
 * @property pageUrl Webpage URL
 * @property contentUrl Audio file URL
 * @property durationSec Duration (in seconds)
 * @property pubDate Date of publishing
 * @property status Status of listening
 * @property currentPositionSec Current position (seconds from the beginning)
 */
data class Episode(val code: UUID,
                   val podcastCode: UUID,
                   val episodeGuid: UUID,
                   val title: String?,
                   val description: String?,
                   val pageUrl: String?,
                   val contentUrl: String,
                   val durationSec: Int,
                   val pubDate: Date,
                   val status: EpisodeStatus,
                   val currentPositionSec: Int)
