package ru.vinyarsky.englishmedia.db;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.ReplaySubject;

import static android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE;


public final class Podcast {

    public static final String CODE = "code";
    public static final String COUNTRY = "country";
    public static final String LEVEL = "level";
    public static final String TITLE = "title";
    public static final String DESCRIPTION = "description";
    public static final String IMAGE_PATH = "image_path";
    public static final String RSS_URL = "rss_url";
    public static final String SUBSCRIBED = "subscribed";

    private static String TABLE_NAME = "Podcasts";

    private static final String SQL_CREATE_TABLE =
            String.format("create table %s (", TABLE_NAME) +
            String.format(" %s text primary key not null,", CODE) +
            String.format(" %s text,", COUNTRY) +
            String.format(" %s text not null,", LEVEL) +
            String.format(" %s text not null,", TITLE) +
            String.format(" %s text,", DESCRIPTION) +
            String.format(" %s text,", IMAGE_PATH) +
            String.format(" %s text not null,", RSS_URL) +
            String.format(" %s integer not null", SUBSCRIBED) +
            ")";

    private static final String SQL_SELECT_ALL = String.format("select ROWID as _id, * from %s", TABLE_NAME);
    private static final String SQL_SELECT_ALL_BY_PODCAST_LEVEL = String.format("select ROWID as _id, * from %s where %s = ?1", TABLE_NAME, LEVEL);
    private static final String SQL_SELECT_BY_CODE = String.format("select ROWID as _id, * from %s where %s = ?1", TABLE_NAME, CODE);

    private UUID code;
    private Country country;
    private PodcastLevel level;
    private String title;
    private String description;
    private String imagePath;
    private String rssUrl;
    private boolean subscribed;

    /**
     * Create new item
     */
    public Podcast() {
        this.setSubscribed(false);
    }

    /**
     * Instantiate existed item (from current cursor position)
     */
    public Podcast(Cursor cursor) {
        this();
        this.code = UUID.fromString(cursor.getString(cursor.getColumnIndex(CODE)));
        this.setCountry(Country.valueOf(cursor.getString(cursor.getColumnIndex(COUNTRY))));
        this.setLevel(PodcastLevel.valueOf(cursor.getString(cursor.getColumnIndex(LEVEL))));
        this.setTitle(cursor.getString(cursor.getColumnIndex(TITLE)));
        this.setDescription(cursor.getString(cursor.getColumnIndex(DESCRIPTION)));
        this.setImagePath(cursor.getString(cursor.getColumnIndex(IMAGE_PATH)));
        this.setRssUrl(cursor.getString(cursor.getColumnIndex(RSS_URL)));
        this.setSubscribed(cursor.getInt(cursor.getColumnIndex(SUBSCRIBED)) != 0);
    }

    /**
     * Returns Podcast item by Code
     * @return null if not found
     */
    public static Podcast read(DbHelper dbHelper, UUID code) {
        return read(dbHelper.getDatabase(), code);
    }

    /**
     * Returns Podcast item by Code
     * @return null if not found
     */
    /* package */ static Podcast read(SQLiteDatabase db, UUID code) {
        try (Cursor cursor = db.rawQuery(SQL_SELECT_BY_CODE, new String[] { code.toString() })) {
            cursor.moveToNext();
            if (cursor.getCount() > 0)
                return new Podcast(cursor);
            return null;
        }
    }

    /**
     * Returns all items
     */
    public static Cursor readAll(DbHelper dbHelper) {
        return dbHelper.getDatabase().rawQuery(SQL_SELECT_ALL, null);
    }

    /**
     * Returns items with appropriate podcastLevel
     */
    public static Cursor readAllByPodcastLevel(DbHelper dbHelper, PodcastLevel podcastLevel) {
        return dbHelper.getDatabase().rawQuery(SQL_SELECT_ALL_BY_PODCAST_LEVEL, new String[] { podcastLevel.name() });
    }

    public UUID write(DbHelper dbHelper) {
        return this.write(dbHelper.getDatabase());
    }

    /* package */ UUID write(SQLiteDatabase db) {
        if (this.getCode() == null)
            this.code = UUID.randomUUID();

        ContentValues vals = new ContentValues(7);
        vals.put(CODE, this.getCode().toString());
        vals.put(COUNTRY, this.getCountry().name());
        vals.put(LEVEL, this.getLevel().name());
        vals.put(TITLE, this.getTitle());
        vals.put(DESCRIPTION, this.getDescription());
        vals.put(IMAGE_PATH, this.getImagePath());
        vals.put(RSS_URL, this.getRssUrl());
        vals.put(SUBSCRIBED, this.isSubscribed() ? 1 : 0);
        db.insertWithOnConflict(TABLE_NAME, null, vals, CONFLICT_REPLACE);

        return this.getCode();
    }

    static void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    /* package */ void setCode(UUID code) {
        this.code = code;
    }

    public UUID getCode() {
        return code;
    }

    public PodcastLevel getLevel() {
        return level;
    }

    public void setLevel(PodcastLevel level) {
        this.level = level;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getRssUrl() {
        return rssUrl;
    }

    public void setRssUrl(String rssUrl) {
        this.rssUrl = rssUrl;
    }

    public boolean isSubscribed() {
        return subscribed;
    }

    public void setSubscribed(boolean subscribed) {
        this.subscribed = subscribed;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public enum PodcastLevel { BEGINNER, INTERMEDIATE, ADVANCED }

    public enum Country { NONE, UK, US, CZ, DK }
}
