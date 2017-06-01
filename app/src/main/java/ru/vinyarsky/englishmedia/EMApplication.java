package ru.vinyarsky.englishmedia;

import android.app.Application;

import okhttp3.OkHttpClient;
import ru.vinyarsky.englishmedia.db.DbHelper;
import ru.vinyarsky.englishmedia.media.MediaComponent;
import ru.vinyarsky.englishmedia.media.MediaModule;
import ru.vinyarsky.englishmedia.media.RssFetcher;

public final class EMApplication extends Application {

    private static EMComponent emComponent;

    private static MediaComponent mediaComponent;

    public static EMComponent getEmComponent() {
        assert emComponent != null;
        return emComponent;
    }

    public static MediaComponent getMediaComponent() {
        if (mediaComponent == null)
            mediaComponent = emComponent.createMediaComponent(new MediaModule());
        return mediaComponent;
    }

    private OkHttpClient httpClient;

    private DbHelper dbHelper;
    private RssFetcher rssFetcher;

    public DbHelper getDbHelper() {
        if (dbHelper == null)
            dbHelper = new DbHelper(getApplicationContext());
        return dbHelper;
    }

    public RssFetcher getRssFetcher() {
        if (rssFetcher == null)
            rssFetcher = new RssFetcher(this::getDbHelper, this::getHttpClient);
        return rssFetcher;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        emComponent = DaggerEMComponent.builder()
                .eMModule(new EMModule(this.getApplicationContext()))
                .build();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        ShutdownServices();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        ShutdownServices();
    }

    public OkHttpClient getHttpClient() {
        if (httpClient == null)
            httpClient = new OkHttpClient();
        return httpClient;
    }

    private synchronized void ShutdownServices() {
        if (rssFetcher != null) {
            rssFetcher.close();
            rssFetcher = null;
        }
        if (dbHelper != null) {
            dbHelper.close();
            dbHelper = null;
        }
        if (httpClient != null) {
            httpClient.connectionPool().evictAll();
        }
    }
}
