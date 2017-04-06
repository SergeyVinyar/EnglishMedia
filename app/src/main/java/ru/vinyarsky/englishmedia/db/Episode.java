package ru.vinyarsky.englishmedia.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Future;


public final class Episode {

    public static final String _ID = "_id";
    public static final String PODCAST_CODE = "podcast_code";
    public static final String GUID = "guid";
    public static final String TITLE = "title";
    public static final String DESCRIPTION = "description";
    public static final String PAGE_URL = "page_url";
    public static final String CONTENT_URL = "content_url";
    public static final String CONTENT_PATH = "content_path";
    public static final String DURATION = "duration";
    public static final String TIME_ELAPSED = "time_elapsed";
    public static final String PUB_DATE = "pub_date";
    public static final String STATUS = "status";

    private static String TABLE_NAME = "Episodes";

    private static final String SQL_CREATE_TABLE =
            String.format("create table %s (", TABLE_NAME) +
            String.format("  %s integer primary key autoincrement not null, ", _ID) +
            String.format("  %s text not null,", PODCAST_CODE) +
            String.format("  %s text not null,", GUID) +
            String.format("  %s text not null,", TITLE) +
            String.format("  %s text,", DESCRIPTION) +
            String.format("  %s text,", PAGE_URL) +
            String.format("  %s text not null,", CONTENT_URL) +
            String.format("  %s text,", CONTENT_PATH) +
            String.format("  %s integer not null,", DURATION) +
            String.format("  %s integer,", TIME_ELAPSED) +
            String.format("  %s integer,", PUB_DATE) +
            String.format("  %s integer not null)", STATUS);
    private static final String SQL_SELECT_ALL = String.format("select * from %s", TABLE_NAME);
    private static final String SQL_SELECT_BY_PODCAST_CODE = String.format("select * from %s where %s = ?0", TABLE_NAME, PODCAST_CODE);
    private static final String SQL_SELECT_BY_PODCAST_CODE_AND_GUID = String.format("select * from %s where %s = ?0 and %s = ?1", TABLE_NAME, PODCAST_CODE, GUID);

    private long id;
    private UUID podcastCode;
    private String guid;
    private String title;
    private String description;
    private String pageUrl;
    private String contentUrl;
    private String contentPath;
    private int duration;
    private int timeElapsed;
    private Date pubDate;
    private EpisodeStatus status;

    /**
     * Create new item
     */
    public Episode() {
    }

    /**
     * Instantiate existed item (from current cursor position)
     */
    private Episode(Cursor cursor) {
        this();
        this.id = cursor.getInt(cursor.getColumnIndex(_ID));
        this.setPodcastCode(UUID.fromString(cursor.getString(cursor.getColumnIndex(PODCAST_CODE))));
        this.setGuid(cursor.getString(cursor.getColumnIndex(GUID)));
        this.setTitle(cursor.getString(cursor.getColumnIndex(TITLE)));
        this.setDescription(cursor.getString(cursor.getColumnIndex(DESCRIPTION)));
        this.setPageUrl(cursor.getString(cursor.getColumnIndex(PAGE_URL)));
        this.setContentUrl(cursor.getString(cursor.getColumnIndex(CONTENT_URL)));
        this.setContentPath(cursor.getString(cursor.getColumnIndex(CONTENT_PATH)));
        this.setDuration(Integer.parseInt(cursor.getString(cursor.getColumnIndex(DURATION))));
        this.setTimeElapsed(Integer.parseInt(cursor.getString(cursor.getColumnIndex(TIME_ELAPSED))));
        this.setPubDate(new Date(cursor.getString(cursor.getColumnIndex(PUB_DATE))));
        this.setStatus(EpisodeStatus.valueOf(cursor.getString(cursor.getColumnIndex(STATUS))));
    }

    public static Future<Cursor> readAllAsync(DbHelper dbHelper) {
        return dbHelper.getExecutorSupplier().get().submit(() -> {
            return dbHelper.getDatabase().rawQuery(SQL_SELECT_ALL, null);
        });
    }

    public static Future<Cursor> readAllByPodcastCodeAsync(DbHelper dbHelper, UUID podcastCode) {
        return dbHelper.getExecutorSupplier().get().submit(() -> {
            return dbHelper.getDatabase().rawQuery(SQL_SELECT_BY_PODCAST_CODE, new String[] { podcastCode.toString() });
        });
    }

    public static Episode read(DbHelper dbHelper, UUID podcastCode, String guid) {
        Cursor cursor = dbHelper.getDatabase().rawQuery(SQL_SELECT_BY_PODCAST_CODE_AND_GUID, new String[] { podcastCode.toString(), guid });
        cursor.moveToNext();
        if (cursor.getCount() > 0)
            return new Episode(cursor);
        else
            return null;
    }

    public long write(DbHelper dbHelper) {
        return this.write(dbHelper.getDatabase());
    }

    long write(SQLiteDatabase db) {
        ContentValues vals = new ContentValues(12);
        if (this.getId() != 0)
            vals.put(_ID, this.getId());
        vals.put(PODCAST_CODE, this.getPodcastCode().toString());
        vals.put(GUID, this.getGuid());
        vals.put(TITLE, this.getTitle());
        vals.put(DESCRIPTION, this.getDescription());
        vals.put(PAGE_URL, this.getPageUrl());
        vals.put(CONTENT_URL, this.getContentUrl());
        vals.put(CONTENT_PATH, this.getContentPath());
        vals.put(DURATION, this.getDuration());
        vals.put(TIME_ELAPSED, this.getTimeElapsed());
        vals.put(PUB_DATE, this.getPubDate().getTime());
        vals.put(STATUS, this.getStatus().ordinal());
        this.id = db.insertOrThrow(TABLE_NAME, null, vals);
        return this.id;
    }

    static void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    public long getId() {
        return id;
    }

    private void setId(long id) {
        this.id = id;
    }

    public UUID getPodcastCode() {
        return podcastCode;
    }

    public void setPodcastCode(UUID podcastCode) {
        this.podcastCode = podcastCode;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
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

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }

    public String getContentPath() {
        return contentPath;
    }

    public void setContentPath(String contentPath) {
        this.contentPath = contentPath;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getTimeElapsed() {
        return timeElapsed;
    }

    public void setTimeElapsed(int timeElapsed) {
        this.timeElapsed = timeElapsed;
    }

    public Date getPubDate() {
        return pubDate;
    }

    public void setPubDate(Date pubDate) {
        this.pubDate = pubDate;
    }

    public EpisodeStatus getStatus() {
        return status;
    }

    public void setStatus(EpisodeStatus status) {
        this.status = status;
    }

    public enum EpisodeStatus { NEW, LISTENING, COMPLETED }
}
