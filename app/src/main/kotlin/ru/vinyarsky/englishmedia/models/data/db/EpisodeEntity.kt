package ru.vinyarsky.englishmedia.models.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

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
        @ColumnInfo(name = "pub_date") var pubDate: Int?,
        @ColumnInfo(name = "status") var status: String,
        @ColumnInfo(name = "current_position") var currentPosition: Int = 0)
