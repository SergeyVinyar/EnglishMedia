package ru.vinyarsky.englishmedia.data.converters

import android.net.Uri
import ru.vinyarsky.englishmedia.core.Converter
import ru.vinyarsky.englishmedia.models.data.db.PodcastEntity
import ru.vinyarsky.englishmedia.models.domain.Country
import ru.vinyarsky.englishmedia.models.domain.Podcast
import ru.vinyarsky.englishmedia.models.domain.PodcastLevel
import java.util.*

/**
 * Converter PodcastEntity <-> Podcast
 */
class PodcastEntityConverter : Converter<PodcastEntity, Podcast> {

    override fun convert(source: PodcastEntity): Podcast? =
            try {
                Podcast(
                        UUID.fromString(source.code),
                        source.country?.let { Country.valueOf(it) } ?: Country.NONE,
                        PodcastLevel.valueOf(source.level),
                        source.title,
                        source.description,
                        source.imagePath,
                        Uri.parse(source.rssUrl),
                        source.subscribed != 0)
            } catch (e: IllegalArgumentException) {
                // Parsing failed
                null
            }

    override fun reverse(source: Podcast): PodcastEntity? =
            try {
                PodcastEntity(
                        source.code.toString(),
                        source.country.toString(),
                        source.level.toString(),
                        source.title,
                        source.description,
                        source.imagePath,
                        source.rssUrl.toString(),
                        if (source.subscribed) 1 else 0)
            } catch (e: IllegalArgumentException) {
                // Parsing failed
                null
            }
}