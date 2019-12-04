package ru.vinyarsky.englishmedia.models.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Podcast database entity
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
@Entity(tableName = "Podcasts")
data class PodcastEntity(
        @PrimaryKey
        @ColumnInfo(name = "code")
        var code: String,

        @ColumnInfo(name = "country") var country: String?,
        @ColumnInfo(name = "level") var level: String,
        @ColumnInfo(name = "title") var title: String,
        @ColumnInfo(name = "description") var description: String?,
        @ColumnInfo(name = "image_path") var imagePath: String?,
        @ColumnInfo(name = "rss_url") var rssUrl: String,
        @ColumnInfo(name = "subscribed") var subscribed: Int = 0)
