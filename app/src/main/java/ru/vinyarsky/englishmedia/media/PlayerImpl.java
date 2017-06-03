package ru.vinyarsky.englishmedia.media;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;
import java.util.concurrent.CopyOnWriteArraySet;

import okhttp3.OkHttpClient;

/* package */ class PlayerImpl extends SimpleExoPlayer implements Player {

    private Uri playingUrl;

    private AudioFocus audioFocus;

    private final CopyOnWriteArraySet<PlayerListener> listeners = new CopyOnWriteArraySet<>();

    private ExtractorsFactory extractorsFactory;
    private DataSource.Factory dataSourceFactory;

    private Handler handler = new Handler();

    PlayerImpl(Context context, AudioFocus audioFocus, OkHttpClient httpClient) {
        super(new DefaultRenderersFactory(context), new DefaultTrackSelector(), new DefaultLoadControl());
        this.setPlayWhenReady(false);
        this.addListener(this.exoPlayerListener);

        this.audioFocus = audioFocus;
        audioFocus.addListener(this.audioFocusListener);

        Cache cache = new SimpleCache(new File(context.getCacheDir().getAbsolutePath() + "/exoplayer"), new LeastRecentlyUsedCacheEvictor(1024 * 1024 * 100)); // 100 Mb max
        DataSource.Factory httpDataSourceFactory = new OkHttpDataSourceFactory(httpClient, Util.getUserAgent(context, "EnglishMedia"), null);
        this.dataSourceFactory = new CacheDataSourceFactory(cache, httpDataSourceFactory, CacheDataSource.FLAG_BLOCK_ON_CACHE | CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
        this.extractorsFactory = new DefaultExtractorsFactory();
    }

    @Override
    public void play(Uri url, int startFromPositionSec) {
        if (!audioFocus.ensureAudioFocus()) {
            this.playerEventEmitter.onNoAudioFocus();
            return;
        }

        if (!url.equals(this.playingUrl)) {
            ExtractorMediaSource mediaSource = new ExtractorMediaSource(url, this.dataSourceFactory, this.extractorsFactory, this.handler, this.extractorEventListener);
            prepare(mediaSource);
            this.playingUrl = url;
        }

        if (startFromPositionSec != Integer.MAX_VALUE)
            seekTo(startFromPositionSec * 1000);

        this.playerEventEmitter.onPlay();
        setPlayWhenReady(true);
    }

    @Override
    public void stop() {
        setPlayWhenReady(false);
        this.audioFocus.abandonAudioFocus();
        this.playerEventEmitter.onStop((int) getCurrentPosition() / 1000);
        this.emitNewPosition.run();
    }

    @Override
    public void togglePlayStop() {
        if (this.playingUrl != null) {
            if (getPlayWhenReady())
                stop();
            else
                play(this.playingUrl, Integer.MAX_VALUE);
        }
    }

    private void pause() {
        setPlayWhenReady(false);
    }

    private void resume() {
        if (this.playingUrl != null && audioFocus.ensureAudioFocus()) {
            setPlayWhenReady(true);
        }
    }

    @Override
    public Uri getPlayingUrl() {
        return this.playingUrl;
    }

    @Override
    public void release() {
        this.handler.removeCallbacks(this.emitNewPosition);
        audioFocus.removeListener(this.audioFocusListener);
        super.release();
    }

    @Override
    public void addListener(PlayerListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void removeListener(PlayerListener listener) {
        this.listeners.remove(listener);
    }

    @Override
    public ExoPlayer asExoPlayer() {
        return this;
    }

    private Runnable emitNewPosition = new Runnable() {
        @Override
        public void run() {
            PlayerImpl.this.handler.removeCallbacks(this);

            PlayerImpl.this.playerEventEmitter.onPositionChanged((int) (getCurrentPosition() / 1000));

            if (PlayerImpl.this.getPlayWhenReady())
                PlayerImpl.this.handler.postDelayed(this, 30 * 1000); // 30 secs
        }
    };

    private ExoPlayer.EventListener exoPlayerListener = new EventListener() {

        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest) {
            // Do nothing
        }

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            // Do nothing
        }

        @Override
        public void onLoadingChanged(boolean isLoading) {
            // Do nothing
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            if (playWhenReady && playbackState == ExoPlayer.STATE_ENDED) {
                PlayerImpl.this.stop();
                PlayerImpl.this.playerEventEmitter.onCompleted();
                PlayerImpl.this.playingUrl = null;
            }
            else if (playWhenReady && playbackState == ExoPlayer.STATE_READY) {
                PlayerImpl.this.emitNewPosition.run();
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            PlayerImpl.this.stop();
        }

        @Override
        public void onPositionDiscontinuity() {
            // Do nothing
        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            // Do nothing
        }
    };

    private final ExtractorMediaSource.EventListener extractorEventListener = new ExtractorMediaSource.EventListener() {
        @Override
        public void onLoadError(IOException error) {
            Throwable cause = error.getCause() != null ? error.getCause() : error;
            if (cause instanceof ConnectException || cause instanceof UnknownHostException) {
                PlayerImpl.this.playerEventEmitter.onNoNetwork();
            }
            else if (cause instanceof HttpDataSource.InvalidResponseCodeException) {
                int responseCode = ((HttpDataSource.InvalidResponseCodeException) cause).responseCode;
                switch (responseCode) {
                    case HttpURLConnection.HTTP_BAD_REQUEST:
                    case HttpURLConnection.HTTP_NOT_FOUND:
                        PlayerImpl.this.playerEventEmitter.onContentNotFound();
                        break;
                }
            }
        }
    };

    private AudioFocusImpl.AudioFocusListener audioFocusListener = new AudioFocusImpl.AudioFocusListener() {
        @Override
        public void onPause() {
            pause();
        }

        @Override
        public void onResume() {
            resume();
        }
    };

    private PlayerListener playerEventEmitter = new PlayerListener() {
        @Override
        public void onNoNetwork() {
            for (PlayerListener listener: PlayerImpl.this.listeners)
                listener.onNoNetwork();
        }

        @Override
        public void onContentNotFound() {
            for (PlayerListener listener: PlayerImpl.this.listeners)
                listener.onContentNotFound();
        }

        @Override
        public void onNoAudioFocus() {
            for (PlayerListener listener: PlayerImpl.this.listeners)
                listener.onNoAudioFocus();
        }

        @Override
        public void onPlay() {
            for (PlayerListener listener: PlayerImpl.this.listeners)
                listener.onPlay();
        }

        @Override
        public void onPositionChanged(int positionSec) {
            for (PlayerListener listener: PlayerImpl.this.listeners)
                listener.onPositionChanged(positionSec);
        }

        @Override
        public void onStop(int positionSec) {
            for (PlayerListener listener: PlayerImpl.this.listeners)
                listener.onStop(positionSec);
        }

        @Override
        public void onCompleted() {
            for (PlayerListener listener: PlayerImpl.this.listeners)
                listener.onCompleted();
        }
    };
}
