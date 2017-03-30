package ru.vinyarsky.englishmedia;

import android.app.Application;

import ru.vinyarsky.englishmedia.db.DbHelper;

public final class EMApplication extends Application {

    private DbHelper mDbHelper;

    public DbHelper getDbHelper() {
        if (mDbHelper == null)
            mDbHelper = new DbHelper(getApplicationContext());
        return mDbHelper;
    }

    private void closeDbHelper() {
        if (mDbHelper != null) {
            mDbHelper.close();
            mDbHelper = null;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        closeDbHelper();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        closeDbHelper();
    }
}
