package ru.vinyarsky.englishmedia.media;

import android.content.Context;

import dagger.Module;
import dagger.Provides;

import okhttp3.OkHttpClient;

@Module
public class MediaModule {

    @Provides
    @MediaScope
    public AudioFocus getAudioFocus(Context context) {
        return new AudioFocusImpl(context);
    }

    @Provides
    @MediaScope
    public OkHttpClient getHttpClient() {
        return new OkHttpClient();
    }

    @Provides
    @MediaScope
    public Player getPlayer(Context context, AudioFocus audioFocus, OkHttpClient httpClient) {
        return new PlayerImpl(context, audioFocus, httpClient);
    }
}
