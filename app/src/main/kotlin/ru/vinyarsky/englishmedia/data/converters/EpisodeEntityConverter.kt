package ru.vinyarsky.englishmedia.data.converters

import android.net.Uri
import ru.vinyarsky.englishmedia.core.Converter
import ru.vinyarsky.englishmedia.models.data.db.EpisodeEntity
import ru.vinyarsky.englishmedia.models.domain.*
import java.util.*

/**
 * Converter EpisodeEntity <-> Episode
 */
class EpisodeEntityConverter : Converter<EpisodeEntity, Episode> {

    override fun convert(source: EpisodeEntity): Episode? =
            try {
                Episode(
                        UUID.fromString(source.code),
                        UUID.fromString(source.podcastCode),
                        UUID.fromString(source.episodeGuid),
                        source.title,
                        source.description,
                        source.pageUrl?.let { Uri.parse(it) },
                        Uri.parse(source.contentUrl),
                        source.duration,
                        source.pubDate?.let { Date(it) } ?: Date(),
                        EpisodeStatus.valueOf(source.status),
                        source.currentPosition)
            } catch (e: IllegalArgumentException) {
                // Parsing failed
                null
            }

    override fun reverse(source: Episode): EpisodeEntity? =
            try {
                EpisodeEntity(
                        source.code.toString(),
                        source.podcastCode.toString(),
                        source.episodeGuid.toString(),
                        source.title,
                        source.description,
                        source.pageUrl?.toString(),
                        source.contentUrl.toString(),
                        source.durationSec,
                        source.pubDate.time,
                        source.status.toString(),
                        source.currentPositionSec)
            } catch (e: IllegalArgumentException) {
                // Parsing failed
                null
            }
}