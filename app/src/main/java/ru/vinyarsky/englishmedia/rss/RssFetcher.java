package ru.vinyarsky.englishmedia.rss;

import android.content.Context;
import android.database.Cursor;
import android.util.Xml;

import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
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

    private Context appContext;
    private Supplier<DbHelper> dbHelperSupplier;
    private Supplier<ExecutorService> executorSupplier;
    private Supplier<OkHttpClient> httpClientSupplier;

    public RssFetcher(Context appContext, Supplier<DbHelper> dbHelperSupplier, Supplier<ExecutorService> executorSupplier, Supplier<OkHttpClient> httpClientSupplier) {
        this.appContext = appContext;
        this.dbHelperSupplier = dbHelperSupplier;
        this.executorSupplier = executorSupplier;
        this.httpClientSupplier = httpClientSupplier;
    }

    /**
     * [Async] Fetches episodes from rss-feed and writes them to db
     * @param podcastIds
     * @return Total number of fetched episodes
     */
    public Future<Integer> fetchEpisodesAsync(List<Long> podcastIds) {
        return Observable.fromIterable(podcastIds)
                .subscribeOn(Schedulers.from(this.executorSupplier.get()))
                .map((podcastId) -> {
                    Podcast podcast = Podcast.readById(this.dbHelperSupplier.get(), podcastId);
                    // TODO podcast == null
                    Request request = new Request.Builder()
                            .url(podcast.getRssUrl())
                            .build();
                    try (Response response = httpClientSupplier.get().newCall(request).execute()) {
                        if (response.isSuccessful()) {
                            XmlPullParser parser = Xml.newPullParser();
                            parser.setInput(response.body().charStream());
                            List<Episode> episodes = parseRss(parser);
                            for (Episode episode: episodes) {
                                Episode episodeDb = Episode.read(this.dbHelperSupplier.get(), podcast.getCode(), episode.getGuid());
                                if (episodeDb == null) {
                                    episode.setPodcastCode(podcast.getCode());
                                    episode.write(this.dbHelperSupplier.get());
                                }
                                else {
                                    episodeDb.setTitle(episode.getTitle());
                                    // TODO
                                }
                            }
                            parser.setInput(null); // Release internal structures
                            return 1;
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
    private List<Episode> parseRss(XmlPullParser parser) {
        List<Episode> episodes = new ArrayList<>();
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
                if (episode.getGuid() != null && episode.getContentUrl() != null)
                    episodes.add(episode);
            }

        } catch (XmlPullParserException | IOException e) {
            // TODO
        }
        return episodes;
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
                            episode.setGuid(value);
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
