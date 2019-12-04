package ru.vinyarsky.englishmedia.models.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Episode database entity
 *
 * @property code Unique identifier
 * @property podcastCode Unique identifier of a podcast, this episode belongs to
 * @property episodeGuid Episode identifier in an rss feed
 * @property title Title
 * @property description Description
 * @property pageUrl Webpage URL
 * @property contentUrl Audio file URL
 * @property duration Duration (in seconds)
 * @property pubDate Date of publishing
 * @property status Status of listening
 * @property currentPosition Current position (seconds from the beginning)
 */
@Entity(tableName = "Episodes")
data class EpisodeEntity(
        @PrimaryKey
        @ColumnInfo(name = "code")
        var code: String,

        @ColumnInfo(name = "podcast_code") var podcastCode: String,
        @ColumnInfo(name = "episode_guid") var episodeGuid: String,
        @ColumnInfo(name = "title") var title: String?,
        @ColumnInfo(name = "description") var description: String?,
        @ColumnInfo(name = "page_url") var pageUrl: String?,
        @ColumnInfo(name = "content_url") var contentUrl: String,
        @ColumnInfo(name = "duration") var duration: Int,
        @ColumnInfo(name = "pub_date") var pubDate: Long?,
        @ColumnInfo(name = "status") var status: String,
        @ColumnInfo(name = "current_position") var currentPosition: Int = 0)
