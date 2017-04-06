package ru.vinyarsky.englishmedia;

import android.app.Application;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import ru.vinyarsky.englishmedia.db.DbHelper;
import ru.vinyarsky.englishmedia.rss.RssFetcher;

public final class EMApplication extends Application {

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

    private OkHttpClient getHttpClient() {
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
