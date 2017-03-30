package ru.vinyarsky.englishmedia.db;

import android.annotation.TargetApi;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.ReplaySubject;


public final class Podcast {

    private static final String _ID = "_id";
    private static final String CODE = "code";
    private static final String LEVEL = "level";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String IMAGE_PATH = "image_path";
    private static final String RSS_URL = "rss_url";
    private static final String SUBSCRIBED = "subscribed";

    private static String TABLE_NAME = "Podcasts";

    private static final String SQL_SELECT_BY_ID = String.format("select * from %s where %s = ?0", TABLE_NAME, _ID);

    private long id;
    private UUID code;
    private PodcastLevel level;
    private String title;
    private String description;
    private String imagePath;
    private String rssUrl;
    private boolean subscribed;

    public Podcast() {

    }

    @TargetApi(19)
    public static Observable<Podcast> read(DbHelper dbHelper, int id) {
        return Observable.fromCallable(() -> {
            Podcast result = null;
            try (SQLiteDatabase db = dbHelper.getReadableDatabase()) {
                Cursor cursor = db.rawQuery(SQL_SELECT_BY_ID, new String[] { Integer.toString(id) });
                try {
                    cursor.moveToNext();
                    if (cursor.getCount() == 1) {
                        result = new Podcast();
                        // TODO
                    }
                }
                finally {
                    cursor.close();
                }
            }
            return result;
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
    }

    @TargetApi(19)
    public Observable<Void> write(DbHelper dbHelper) {

        class Sub implements ObservableOnSubscribe<Void> {

            private boolean completed = false;

            private boolean failed = false;
            private Throwable error;

            private ObservableEmitter<Void> emitter;

            @Override
            public void subscribe(ObservableEmitter<Void> e) throws Exception {
                if (completed)
                    e.onComplete();
                else if (failed)
                    e.onError(error);
                else
                    this.emitter = e;
            }

            private void onComplete() {
                if (emitter != null) {
                    emitter.onComplete();
                    emitter = null;
                }
                else {
                    this.completed = true;
                }
            }

            private void onError(Throwable error) {
                if (emitter != null) {
                    emitter.onError(error);
                    emitter = null;
                }
                else {
                    this.failed = true;
                    this.error = error;
                }
            }
        }

        Sub sub = new Sub();

        Observable.fromCallable(() -> {
            try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
                ContentValues vals = new ContentValues(7);
                vals.put(_ID, this.getId());
                vals.put(CODE, this.getCode().toString());
                vals.put(LEVEL, this.getLevel().ordinal());
                vals.put(DESCRIPTION, this.getDescription());
                vals.put(IMAGE_PATH, this.getImagePath());
                vals.put(RSS_URL, this.getRssUrl());
                vals.put(SUBSCRIBED, this.isSubscribed());
                try {
                    this.id = db.insertOrThrow(TABLE_NAME, null, vals);
                } catch (android.database.SQLException e) {
                    return e;
                }
            }
            return null;
        })
        .subscribeOn(Schedulers.io())
        .subscribe((error) -> {
            if (error == null)
                sub.onComplete();
            else
                sub.onError(error);
        });

        return ReplaySubject.create(sub)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public long getId() {
        return id;
    }

    private void setId(long id) {
        this.id = id;
    }

    public UUID getCode() {
        return code;
    }

    public void setCode(UUID code) {
        this.code = code;
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

    public enum PodcastLevel { BEGINNER, INTERMEDIATE, ADVANCED }
}
