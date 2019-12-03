package ru.vinyarsky.englishmedia.models.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

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
