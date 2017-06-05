package ru.vinyarsky.englishmedia;

import android.content.Context;

import com.google.firebase.analytics.FirebaseAnalytics;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

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
    public FirebaseAnalytics getFirebaseAnalytics() {
        return FirebaseAnalytics.getInstance(context);
    }
}
