package ru.vinyarsky.englishmedia;

import android.content.Context;

import com.google.firebase.analytics.FirebaseAnalytics;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import ru.vinyarsky.englishmedia.db.DbHelper;
import ru.vinyarsky.englishmedia.media.RssFetcher;

@Module
public class EMModule {

    private Context context;

    public EMModule(Context context) {
        this.context = context;
    }

    @Provides
    @Singleton
    public Context getContext() {
        return this.context;
    }

    @Provides
    @Singleton
    public DbHelper getDbHelper() {
        return new DbHelper(context);
    }

    @Provides
    @Singleton
    public OkHttpClient getHttpClient() {
        return new OkHttpClient();
    }

    @Provides
    @Singleton
    public RssFetcher getRssFetcher(DbHelper dbHelper, OkHttpClient httpClient) {
        return new RssFetcher(dbHelper, httpClient);
    }

    @Provides
    @Singleton
    public FirebaseAnalytics getFirebaseAnalytics() {
        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        firebaseAnalytics.setAnalyticsCollectionEnabled(true);
        return firebaseAnalytics;
    }
}
