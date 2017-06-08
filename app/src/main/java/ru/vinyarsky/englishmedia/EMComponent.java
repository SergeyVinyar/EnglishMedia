package ru.vinyarsky.englishmedia;

import android.content.Context;

import com.google.firebase.analytics.FirebaseAnalytics;

import javax.inject.Singleton;

import dagger.Component;

import ru.vinyarsky.englishmedia.db.DbHelper;
import ru.vinyarsky.englishmedia.media.MediaComponent;
import ru.vinyarsky.englishmedia.media.MediaModule;
import ru.vinyarsky.englishmedia.media.RssFetcher;

@Component(modules = {EMModule.class})
@Singleton
public interface EMComponent {

    Context getContext();
    DbHelper getDbHelper();
    RssFetcher getRssFetcher();

    FirebaseAnalytics getFirebaseAnalytics();

    MediaComponent createMediaComponent(MediaModule mediaModule);
}