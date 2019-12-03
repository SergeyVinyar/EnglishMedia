package ru.vinyarsky.englishmedia.data.converters

import ru.vinyarsky.englishmedia.core.Converter
import ru.vinyarsky.englishmedia.models.data.db.PodcastEntity
import ru.vinyarsky.englishmedia.models.domain.Country
import ru.vinyarsky.englishmedia.models.domain.Podcast
import ru.vinyarsky.englishmedia.models.domain.PodcastLevel
import java.util.*

class PodcastEntityConverter : Converter<PodcastEntity, Podcast> {

    override fun convert(source: PodcastEntity): Podcast? =
            try {
                val sourceCountry = source.country

                Podcast(
                        UUID.fromString(source.code),
                        if (sourceCountry != null) Country.valueOf(sourceCountry) else Country.NONE,
                        PodcastLevel.ADVANCED,
                        "",
                        "",
                        "",
                        "",
                        false)
            } catch (e: IllegalArgumentException) {
                // Parsing failed
                null
            }

    override fun reverse(source: Podcast): PodcastEntity? {
        return null;
    }
}