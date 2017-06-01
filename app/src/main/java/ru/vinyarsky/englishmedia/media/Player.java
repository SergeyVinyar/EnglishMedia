package ru.vinyarsky.englishmedia.media;

import android.net.Uri;
import android.support.annotation.Nullable;

import com.google.android.exoplayer2.ExoPlayer;

public interface Player {

    void play(Uri url, int startFromPositionSec);
    void stop();
    void togglePlayStop();

    @Nullable
    Uri getPlayingUrl();

    void release();

    void addListener(PlayerListener listener);
    void removeListener(PlayerListener listener);

    /**
     * PlaybackControlView requires, hope to remove it later
     */
    ExoPlayer asExoPlayer();

    interface PlayerListener {

        void onNoNetwork();
        void onContentNotFound();
        void onNoAudioFocus();

        void onPlay();
        void onPositionChanged(int positionSec);
        void onStop(int positionSec);
        void onCompleted();
    }
}
