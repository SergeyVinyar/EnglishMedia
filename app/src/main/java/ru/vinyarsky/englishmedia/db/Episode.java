package ru.vinyarsky.englishmedia.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Date;
import java.util.UUID;


public final class Episode {

    public static final String CODE = "code";
    public static final String PODCAST_CODE = "podcast_code";
    public static final String EPISODE_GUID = "episodeGuid";
    public static final String TITLE = "title";
    public static final String DESCRIPTION = "description";
    public static final String PAGE_URL = "page_url";
    public static final String CONTENT_URL = "content_url";
    public static final String CONTENT_LOCAL_PATH = "content_local_path";
    public static final String FILE_SIZE = "file_size";
    public static final String DURATION = "duration";
    public static final String PUB_DATE = "pub_date";
    public static final String STATUS = "status";
    public static final String TIME_ELAPSED = "time_elapsed";

    private static String TABLE_NAME = "Episodes";

    private static final String SQL_CREATE_TABLE =
            String.format("create table %s (", TABLE_NAME) +
            String.format("  %s text primary key not null,", CODE) +
            String.format("  %s text not null,", PODCAST_CODE) +
            String.format("  %s text not null,", EPISODE_GUID) +
            String.format("  %s text,", TITLE) +
            String.format("  %s text,", DESCRIPTION) +
            String.format("  %s text,", PAGE_URL) +
            String.format("  %s text not null,", CONTENT_URL) +
            String.format("  %s text,", CONTENT_LOCAL_PATH) +
            String.format("  %s integer,", FILE_SIZE) +
            String.format("  %s integer not null,", DURATION) +
            String.format("  %s integer,", PUB_DATE) +
            String.format("  %s integer not null,", STATUS) +
            String.format("  %s integer not null", TIME_ELAPSED) +
            ")";

    private static final String SQL_SELECT_ALL = String.format("select ROWID as _id, * from %s", TABLE_NAME);
    private static final String SQL_SELECT_BY_PODCAST_CODE = String.format("select ROWID as _id, * from %s where %s = ?1", TABLE_NAME, PODCAST_CODE);
    private static final String SQL_EXISTS_BY_PODCAST_CODE_AND_GUID = String.format("select exists(select 1 from %s where %s = ?1 and %s = ?2)", TABLE_NAME, PODCAST_CODE, EPISODE_GUID);
    private static final String SQL_SELECT_BY_CODE = String.format("select ROWID as _id, * from %s where %s = ?1", TABLE_NAME, CODE);

    private UUID code;
    private UUID podcastCode;
    private String episodeGuid;
    private String title;
    private String description;
    private String pageUrl;
    private String contentUrl;
    private String contentLocalPath;
    private int fileSize;
    private int duration;
    private Date pubDate;
    private EpisodeStatus status;
    private int timeElapsed;

    /**
     * Create new item
     */
    public Episode() {
        this.setStatus(EpisodeStatus.NEW);
        this.setTimeElapsed(0);
    }

    /**
     * Instantiate existed item (from current cursor position)
     */
    private Episode(Cursor cursor) {
        this();
        this.code = UUID.fromString(cursor.getString(cursor.getColumnIndex(CODE)));
        this.setPodcastCode(UUID.fromString(cursor.getString(cursor.getColumnIndex(PODCAST_CODE))));
        this.setEpisodeGuid(cursor.getString(cursor.getColumnIndex(EPISODE_GUID)));
        this.setTitle(cursor.getString(cursor.getColumnIndex(TITLE)));
        this.setDescription(cursor.getString(cursor.getColumnIndex(DESCRIPTION)));
        this.setPageUrl(cursor.getString(cursor.getColumnIndex(PAGE_URL)));
        this.setContentUrl(cursor.getString(cursor.getColumnIndex(CONTENT_URL)));
        this.setContentLocalPath(cursor.getString(cursor.getColumnIndex(CONTENT_LOCAL_PATH)));
        this.setDuration(Integer.parseInt(cursor.getString(cursor.getColumnIndex(DURATION))));
        this.setPubDate(new Date(cursor.getString(cursor.getColumnIndex(PUB_DATE))));
        this.setStatus(EpisodeStatus.valueOf(cursor.getString(cursor.getColumnIndex(STATUS))));
        this.setTimeElapsed(Integer.parseInt(cursor.getString(cursor.getColumnIndex(TIME_ELAPSED))));
    }

    public static Cursor readAll(DbHelper dbHelper) {
        return dbHelper.getDatabase().rawQuery(SQL_SELECT_ALL, null);
    }

    public static Cursor readAllByPodcastCode(DbHelper dbHelper, UUID podcastCode) {
        return dbHelper.getDatabase().rawQuery(SQL_SELECT_BY_PODCAST_CODE, new String[] { podcastCode.toString() });
    }

    public static Episode read(DbHelper dbHelper, UUID code) {
        try (Cursor cursor = dbHelper.getDatabase().rawQuery(SQL_SELECT_BY_CODE, new String[] { code.toString() })) {
            cursor.moveToNext();
            if (cursor.getCount() > 0)
                return new Episode(cursor);
            else
                return null;
        }
    }

    public static boolean existsByPodcastCodeAndGuid(DbHelper dbHelper, UUID podcastCode, String episodeGuid) {
        try (Cursor cursor = dbHelper.getDatabase().rawQuery(SQL_EXISTS_BY_PODCAST_CODE_AND_GUID, new String[] { podcastCode.toString(), episodeGuid })) {
            cursor.moveToNext();
            return cursor.getCount() > 0 && cursor.getInt(0) == 1;
        }
    }

    public UUID write(DbHelper dbHelper) {
        return this.write(dbHelper.getDatabase());
    }

    UUID write(SQLiteDatabase db) {
        if (this.getCode() == null)
            this.code = UUID.randomUUID();

        ContentValues vals = new ContentValues(11);
        vals.put(CODE, this.getCode().toString());
        vals.put(PODCAST_CODE, this.getPodcastCode().toString());
        vals.put(EPISODE_GUID, this.getEpisodeGuid());
        vals.put(TITLE, this.getTitle());
        vals.put(DESCRIPTION, this.getDescription());
        vals.put(PAGE_URL, this.getPageUrl());
        vals.put(CONTENT_URL, this.getContentUrl());
        vals.put(CONTENT_LOCAL_PATH, this.getContentLocalPath());
        vals.put(DURATION, this.getDuration());
        vals.put(TIME_ELAPSED, this.getTimeElapsed());
        if (this.getPubDate() != null)
            vals.put(PUB_DATE, this.getPubDate().getTime());
        vals.put(STATUS, this.getStatus().ordinal());
        db.insertOrThrow(TABLE_NAME, null, vals);

        return this.getCode();
    }

    static void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    public UUID getCode() {
        return code;
    }

    public UUID getPodcastCode() {
        return podcastCode;
    }

    public void setPodcastCode(UUID podcastCode) {
        this.podcastCode = podcastCode;
    }

    public String getEpisodeGuid() {
        return episodeGuid;
    }

    public void setEpisodeGuid(String episodeGuid) {
        this.episodeGuid = episodeGuid;
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

    public String getContentLocalPath() {
        return contentLocalPath;
    }

    public void setContentLocalPath(String contentLocalPath) {
        this.contentLocalPath = contentLocalPath;
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

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public enum EpisodeStatus { NEW, LISTENING, COMPLETED }
}
