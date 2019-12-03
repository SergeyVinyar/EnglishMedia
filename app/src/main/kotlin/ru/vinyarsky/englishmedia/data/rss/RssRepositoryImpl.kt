package ru.vinyarsky.englishmedia.data.rss

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import ru.vinyarsky.englishmedia.core.ClosableSequence
import ru.vinyarsky.englishmedia.core.asClosable
import ru.vinyarsky.englishmedia.domain.repository.RssRepository
import ru.vinyarsky.englishmedia.models.domain.Episode
import ru.vinyarsky.englishmedia.models.domain.EpisodeStatus
import ru.vinyarsky.englishmedia.models.domain.Podcast
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class RssRepositoryImpl(private val httpClient: OkHttpClient,
                        private val parser: XmlPullParser) : RssRepository {

    override suspend fun fetchEpisodes(podcast: Podcast): ClosableSequence<Episode> = withContext(Dispatchers.Default) {
        val request = Request.Builder()
                .url(podcast.rssUrl)
                .build()

        httpClient.newCall(request).execute().use {
            val body = it.body
            if (it.isSuccessful && body != null) {
                parse(body)
            } else {
                emptySequence()
            }.asClosable { parser.setInput(null) }
        }
    }

    private fun parse(body: ResponseBody): Sequence<Episode> {
        parser.setInput(body.charStream())
        parser.next()

        return sequence {
            try {
                parser.require(XmlPullParser.START_TAG, null, "rss")
                if ("2.0" == parser.getAttributeValue(null, "version")) {
                    while (parser.next() != XmlPullParser.END_DOCUMENT) {
                        if (parser.eventType != XmlPullParser.START_TAG)
                            continue
                        if ("item" != parser.name)
                            continue
                        val episode = parseOneEpisode()
                        if (episode != null) {
                            yield(episode!!)
                        }
                    }
                } else {
                    //FirebaseCrash.report(Exception(String.format("rss version must be 2.0 (podcastCode: %s)", if (podcastCode != null) podcastCode.toString() else "null")))
                }
            } catch (e: XmlPullParserException) {
                //FirebaseCrash.report(e)
            } catch (e: IOException) {
                //FirebaseCrash.report(e)
            }
        }
    }

    private fun parseOneEpisode(): Episode? {
        parser.require(XmlPullParser.START_TAG, null, "item")

        val dateFormatYYYY = SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", Locale.US)
        val dateFormatYY = SimpleDateFormat("E, dd MMM yy HH:mm:ss z", Locale.US)

        var code: UUID? = null
        var title: String? = null
        var description: String? = null
        var pageUrl: String? = null
        var contentUrl: String? = null
        var duration: Int = 0
        var publishDate: Date = Date()

        var currentTag = ""
        while (!(parser.next() == XmlPullParser.END_TAG && "item" == parser.name)) {
            val eventType = parser.eventType
            if (eventType == XmlPullParser.START_TAG) {
                currentTag = parser.name
                if ("enclosure" == currentTag) {
                    contentUrl = parser.getAttributeValue(null, "url")
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                currentTag = ""
            } else if (eventType == XmlPullParser.TEXT && "" != currentTag) {
                val value = parser.text
                when (currentTag) {
                    "title" -> title = value
                    "description" -> description = value
                    "guid" -> {
                        try {
                            code = UUID.fromString(value)
                        } catch (e: IllegalArgumentException) {
                        }
                    }
                    "pubDate" -> {
                        try {
                            publishDate = dateFormatYYYY.parse(value)
                        } catch (e1: ParseException) {
                            try {
                                publishDate = dateFormatYY.parse(value)
                            } catch (e2: ParseException) {
                            }
                        }
                    }
                    "link" -> pageUrl = value
                    "duration" -> {
                        // 1:22:10 or 4930
                        val values = value.split(":")
                        var k = 1
                        for (i in values.indices.reversed()) {
                            duration += Integer.valueOf(values[i]) * k
                            k = k * 60
                        }
                    }
                }
            }
        }

        if (code != null && title != null && description != null && pageUrl != null && contentUrl != null) {
            return Episode(code, title, description, pageUrl, contentUrl, duration, publishDate, EpisodeStatus.NEW, 0)
        } else {
            return null
        }
    }
}