package ru.vinyarsky.englishmedia;

import android.app.Application;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ru.vinyarsky.englishmedia.db.DbHelper;
import ru.vinyarsky.englishmedia.rss.RssFetcher;

public final class EMApplication extends Application {

    private ExecutorService executor;

    private DbHelper dbHelper;
    private RssFetcher rssFetcher;

    public DbHelper getDbHelper() {
        if (dbHelper == null)
            dbHelper = new DbHelper(getApplicationContext(), this::getExecutor);
        return dbHelper;
    }

    public RssFetcher getRssFetcher() {
        if (rssFetcher == null)
            rssFetcher = new RssFetcher(getApplicationContext(), this::getExecutor);
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

    private ExecutorService getExecutor() {
        if (executor == null)
            executor = Executors.newCachedThreadPool(); // TODO Add some restriction on thread count?
        return executor;
    }

    private synchronized void ShutdownServices() {
        if (dbHelper != null) {
            dbHelper.close();
            dbHelper = null;
        }
        if (rssFetcher != null) {
            rssFetcher.close();
            rssFetcher = null;
        }
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }
}
