package ru.vinyarsky.englishmedia.db;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.annimon.stream.function.Supplier;

import ru.vinyarsky.englishmedia.R;

public class DbHelper extends SQLiteOpenHelper {

    private static int DB_VERSION = 1;

    private Context appContext;
    private SQLiteDatabase database;

    public DbHelper(Context appContext) {
        super(appContext, "data", null, DB_VERSION);
        this.appContext = appContext;
    }

    @Override
    public synchronized void close() {
        if (this.database != null) {
            this.database.close();
            this.database = null;
        }
        super.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            try {
                // Table creation
                Podcast.onCreate(db);
                Episode.onCreate(db);

                // Populating database from xml/podcasts
                Podcast podcast = null;
                String lastTag = "";
                try (XmlResourceParser parser = appContext.getResources().getXml(R.xml.podcasts)) {
                    int eventType = parser.getEventType();
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        switch (eventType) {
                            case XmlPullParser.START_TAG:
                                lastTag = parser.getName();
                                if (lastTag.equals("podcast")) {
                                    if (podcast != null)
                                        podcast.write(db);
                                    podcast = new Podcast();
                                    podcast.setSubscribed(false);
                                }
                                break;
                            case XmlPullParser.TEXT:
                                String value = parser.getText();
                                switch (lastTag) {
                                    case "code":
// TODO                                        podcast.setCode(UUID.fromString(value));
                                        break;
                                    case "level":
                                        podcast.setLevel(Podcast.PodcastLevel.valueOf(value));
                                        break;
                                    case "title":
                                        podcast.setTitle(value);
                                        break;
                                    case "description":
                                        podcast.setDescription(value);
                                        break;
                                    case "rss_url":
                                        podcast.setRssUrl(value);
                                        break;
                                    case "image_src":
                                        podcast.setImagePath(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + appContext.getPackageName() + "/raw/" + value);
                                        break;
                                }
                                break;
                        }
                        parser.next();
                        eventType = parser.getEventType();
                    }
                }
                if (podcast != null)
                    podcast.write(db);

                db.setTransactionSuccessful();
            } catch (XmlPullParserException | IOException e) {
                Log.e("DbHelper", "onCreate", e);
            }
        }
        finally {
            db.endTransaction();
        }
    }

    /**
     * Use {@link DbHelper#getDatabase()} instead
     */
    @Deprecated
    @Override
    public SQLiteDatabase getReadableDatabase() {
        return super.getReadableDatabase();
    }

    /**
     * Use {@link DbHelper#getDatabase()} instead
     */
    @Deprecated
    @Override
    public SQLiteDatabase getWritableDatabase() {
        return super.getWritableDatabase();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // do nothing
    }

    SQLiteDatabase getDatabase() {
        if (this.database == null)
            this.database = getWritableDatabase();
        return this.database;
    }
}
