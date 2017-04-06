package ru.vinyarsky.englishmedia.rss;

import android.util.Xml;

import java.io.IOException;
import java.sql.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.annimon.stream.function.Supplier;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import ru.vinyarsky.englishmedia.db.DbHelper;
import ru.vinyarsky.englishmedia.db.Episode;
import ru.vinyarsky.englishmedia.db.Podcast;

public final class RssFetcher {

    private Supplier<DbHelper> dbHelperSupplier;
    private Supplier<OkHttpClient> httpClientSupplier;

    public RssFetcher(Supplier<DbHelper> dbHelperSupplier, Supplier<OkHttpClient> httpClientSupplier) {
        this.dbHelperSupplier = dbHelperSupplier;
        this.httpClientSupplier = httpClientSupplier;
    }

    /**
     * Fetches episodes from rss-feed and writes them to db
     * @return Total number of fetched episodes
     */
    public Future<Integer> fetchEpisodesAsync(List<UUID> podcastCodes) {
        return Observable.fromIterable(podcastCodes)
                .subscribeOn(Schedulers.io())
                .map((podcastCode) -> {
                    Podcast podcast = Podcast.read(this.dbHelperSupplier.get(), podcastCode);
                    // TODO podcast == null
                    Request request = new Request.Builder()
                            .url(podcast.getRssUrl())
                            .build();
                    try (Response response = httpClientSupplier.get().newCall(request).execute()) {
                        if (response.isSuccessful()) {
                            XmlPullParser parser = Xml.newPullParser();
                            parser.setInput(response.body().charStream());
                            int count = processRss(parser, podcastCode);
                            parser.setInput(null); // Release internal structures
                            return count;
                        }
                    }
                    return 0;
                })
                .reduce(0, (acc, value) -> acc + value)
                .toFuture();
    }

    /**
     * Parses RSS 2.0 xml
     * https://cyber.harvard.edu/rss/rss.html
     * @implNote Must be thread-safe
     */
    private int processRss(XmlPullParser parser, UUID podcastCode) {
        int count = 0;
        try {
            parser.require(XmlPullParser.START_DOCUMENT, null, "rss");
            if (!"2.0".equals(parser.getAttributeValue(null, "version")))
                throw new XmlPullParserException("rss version must be 2.0");
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() != XmlPullParser.START_TAG)
                    continue;
                if (!"item".equals(parser.getName()))
                    continue;
                Episode episode = parseRssItem(parser);
                if (episode.getEpisodeGuid() != null && episode.getContentUrl() != null) {
                    Episode dbEpisode = Episode.readByPodcastCodeAndGuid(this.dbHelperSupplier.get(), podcastCode, episode.getEpisodeGuid());
                    if (dbEpisode == null) {
                        episode.write(this.dbHelperSupplier.get());
                    }
                    else {
                        dbEpisode.setTitle(episode.getTitle());
                        dbEpisode.setDescription(episode.getDescription());
                        dbEpisode.setPageUrl(episode.getPageUrl());
                        dbEpisode.setContentUrl(episode.getContentUrl());
                        dbEpisode.setContentLocalPath(episode.getContentLocalPath());
                        dbEpisode.setDuration(episode.getDuration());
                        dbEpisode.setPubDate(episode.getPubDate());
                        dbEpisode.write(this.dbHelperSupplier.get());
                    }
                    count++;
                }
            }
        } catch (XmlPullParserException | IOException e) {
            // TODO
        }
        return count;
    }

    private Episode parseRssItem(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "item");
        Episode episode = new Episode();
        String currentTag = "";
        while (parser.next() != XmlPullParser.END_TAG) {
            switch (parser.getEventType()) {
                case XmlPullParser.START_TAG:
                    currentTag = parser.getName();
                    if ("enclosure".equals(currentTag)) {
                        episode.setContentUrl(parser.getAttributeValue(null, "url"));
                    }
                    break;
                case XmlPullParser.TEXT:
                    String value = parser.getText();
                    switch (currentTag) {
                        case "title":
                            episode.setTitle(value);
                            break;
                        case "description":
                            episode.setDescription(value);
                            break;
                        case "guid":
                            episode.setEpisodeGuid(value);
                            break;
                        case "pubDate":
                            episode.setPubDate(Date.valueOf(value));
                            break;
                        case "link":
                            episode.setPageUrl(value);
                            break;
                        case "duration": // itunes:duration
                            episode.setDuration(Integer.valueOf(value));
                            break;
                    }
                    break;
            }
        }
        return episode;
    }

    public synchronized void close() {
        // TODO
    }
}
