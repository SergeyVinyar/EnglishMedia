package ru.vinyarsky.englishmedia.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Date;
import java.util.UUID;

import static android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE;


public final class Episode {

    public static final String ID = "_id";
    public static final String CODE = "code";
    public static final String PODCAST_CODE = "podcast_code";
    public static final String EPISODE_GUID = "episode_guid";
    public static final String TITLE = "title";
    public static final String DESCRIPTION = "description";
    public static final String PAGE_URL = "page_url";
    public static final String CONTENT_URL = "content_url";
    public static final String DURATION = "duration";
    public static final String PUB_DATE = "pub_date";
    public static final String STATUS = "status";
    public static final String CURRENT_POSITION = "current_position";

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
            String.format("  %s integer not null,", DURATION) +
            String.format("  %s integer,", PUB_DATE) +
            String.format("  %s text not null,", STATUS) +
            String.format("  %s integer not null", CURRENT_POSITION) +
            ")";

    private static final String SQL_SELECT_ALL_BY_PODCAST_CODE = String.format("select ROWID as _id, * from %s where %s = ? order by %s desc", TABLE_NAME, PODCAST_CODE, PUB_DATE);
    private static final String SQL_EXISTS_BY_PODCAST_CODE_AND_GUID = String.format("select exists(select 1 from %s where %s = ? and %s = ?)", TABLE_NAME, PODCAST_CODE, EPISODE_GUID);
    private static final String SQL_SELECT_BY_CODE = String.format("select ROWID as _id, * from %s where %s = ?", TABLE_NAME, CODE);

    private long dbId;
    private UUID code;
    private UUID podcastCode;
    private String episodeGuid;
    private String title;
    private String description;
    private String pageUrl;
    private String contentUrl;
    private int duration;
    private Date pubDate;
    private EpisodeStatus status;
    private int currentPosition;

    /**
     * Create new item
     */
    public Episode() {
        this.setStatus(EpisodeStatus.NEW);
        this.setCurrentPosition(0);
    }

    /**
     * Instantiate existed item (from current cursor position)
     */
    private Episode(Cursor cursor) {
        this();
        this.dbId = Long.parseLong(cursor.getString(cursor.getColumnIndex(ID)));
        this.code = UUID.fromString(cursor.getString(cursor.getColumnIndex(CODE)));
        this.setPodcastCode(UUID.fromString(cursor.getString(cursor.getColumnIndex(PODCAST_CODE))));
        this.setEpisodeGuid(cursor.getString(cursor.getColumnIndex(EPISODE_GUID)));
        this.setTitle(cursor.getString(cursor.getColumnIndex(TITLE)));
        this.setDescription(cursor.getString(cursor.getColumnIndex(DESCRIPTION)));
        this.setPageUrl(cursor.getString(cursor.getColumnIndex(PAGE_URL)));
        this.setContentUrl(cursor.getString(cursor.getColumnIndex(CONTENT_URL)));
        this.setDuration(Integer.parseInt(cursor.getString(cursor.getColumnIndex(DURATION))));
        this.setPubDate(new Date(cursor.getLong(cursor.getColumnIndex(PUB_DATE))));
        this.setStatus(EpisodeStatus.valueOf(cursor.getString(cursor.getColumnIndex(STATUS))));
        this.setCurrentPosition(Integer.parseInt(cursor.getString(cursor.getColumnIndex(CURRENT_POSITION))));
    }

    public static Episode[] readAllByPodcastCode(DbHelper dbHelper, UUID podcastCode) {
        try (Cursor cursor = dbHelper.getDatabase().rawQuery(SQL_SELECT_ALL_BY_PODCAST_CODE, new String[] { podcastCode.toString() })) {
            return cursorToArray(cursor);
        }
    }

    private static Episode[] cursorToArray(Cursor cursor) {
        Episode[] result = new Episode[cursor.getCount()];
        int index = 0;
        while(cursor.moveToNext())
            result[index++] = new Episode(cursor);
        return result;
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

    public static void setStatusListeningIfRequired(DbHelper dbHelper, String contentUrl) {
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(STATUS, EpisodeStatus.LISTENING.toString());
        String whereClause = String.format("%s != ? and %s = ?", STATUS, CONTENT_URL);
        dbHelper.getDatabase().update(TABLE_NAME, contentValues, whereClause, new String[] { EpisodeStatus.COMPLETED.toString(), contentUrl });
    }

    public static void setStatusCompleted(DbHelper dbHelper, String contentUrl) {
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(STATUS, EpisodeStatus.COMPLETED.toString());
        String whereClause = String.format("%s = ?", CONTENT_URL);
        dbHelper.getDatabase().update(TABLE_NAME, contentValues, whereClause, new String[] { contentUrl });
    }

    public static void updatePosition(DbHelper dbHelper, String contentUrl, int position) {
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(CURRENT_POSITION, position);
        String whereClause = String.format("%s = ?", CONTENT_URL);
        dbHelper.getDatabase().update(TABLE_NAME, contentValues, whereClause, new String[] { contentUrl });
    }

    public UUID write(DbHelper dbHelper) {
        return this.write(dbHelper.getDatabase());
    }

    /* package */ UUID write(SQLiteDatabase db) {
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
        vals.put(DURATION, this.getDuration());
        vals.put(CURRENT_POSITION, this.getCurrentPosition());
        if (this.getPubDate() != null)
            vals.put(PUB_DATE, this.getPubDate().getTime());
        vals.put(STATUS, this.getStatus().toString());
        db.insertWithOnConflict(TABLE_NAME, null, vals, CONFLICT_REPLACE);

        return this.getCode();
    }

    static void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    /** ROWID from SQLite */
    public long getDbId() {
        return dbId;
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

    /**
     * Duration in seconds
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Duration in seconds
     */
    public void setDuration(int duration) {
        this.duration = duration;
    }

    /**
     * Current position in seconds
     */
    public int getCurrentPosition() {
        return currentPosition;
    }

    /**
     * Current position in seconds
     */
    public void setCurrentPosition(int currentPosition) {
        this.currentPosition = currentPosition;
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
