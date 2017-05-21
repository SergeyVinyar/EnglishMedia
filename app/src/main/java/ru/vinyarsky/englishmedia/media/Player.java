package ru.vinyarsky.englishmedia.media;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;

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

import okhttp3.OkHttpClient;

/* package */ class Player extends SimpleExoPlayer  {

    private Uri playingUrl;

    private AudioFocus audioFocus;
    private PlayerListener playerListener;

    private ExtractorsFactory extractorsFactory;
    private DataSource.Factory dataSourceFactory;

    private Handler handler = new Handler();

    /* package */ Player(Context context, OkHttpClient httpClient, PlayerListener playerListener) {
        super(new DefaultRenderersFactory(context), new DefaultTrackSelector(), new DefaultLoadControl());
        this.setPlayWhenReady(false);
        this.addListener(this.exoPlayerListener);
        this.audioFocus = new AudioFocus(context, this.audioFocusListener);
        this.playerListener = playerListener;

        Cache cache = new SimpleCache(new File(context.getCacheDir().getAbsolutePath() + "/exoplayer"), new LeastRecentlyUsedCacheEvictor(1024 * 1024 * 100)); // 100 Mb max
        DataSource.Factory httpDataSourceFactory = new OkHttpDataSourceFactory(httpClient, Util.getUserAgent(context, "EnglishMedia"), null);
        this.dataSourceFactory = new CacheDataSourceFactory(cache, httpDataSourceFactory, CacheDataSource.FLAG_BLOCK_ON_CACHE | CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);
        this.extractorsFactory = new DefaultExtractorsFactory();
    }

    public void play(Uri url, int startFromPositionSec) {
        if (!audioFocus.ensureAudioFocus()) {
            this.playerListener.onNoAudioFocus();
            return;
        }

        if (!url.equals(this.playingUrl)) {
            ExtractorMediaSource mediaSource = new ExtractorMediaSource(url, this.dataSourceFactory, this.extractorsFactory, this.handler, this.extractorEventListener);
            prepare(mediaSource);
            this.playingUrl = url;
        }

        if (startFromPositionSec != Integer.MAX_VALUE)
            seekTo(startFromPositionSec * 1000);

        this.playerListener.onPlay();
        setPlayWhenReady(true);
        this.emitNewPosition.run();
    }

    public void stop() {
        setPlayWhenReady(false);
        this.audioFocus.abandonAudioFocus();
        this.playerListener.onStop((int) getCurrentPosition() / 1000);
        this.emitNewPosition.run();
    }

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
        this.emitNewPosition.run();
    }

    private void resume() {
        if (this.playingUrl != null && audioFocus.ensureAudioFocus()) {
            setPlayWhenReady(true);
            this.emitNewPosition.run();
        }
    }

    private void reset() {
        this.audioFocus.abandonAudioFocus();
        setPlayWhenReady(false);
        this.playingUrl = null;
    }

    @Nullable
    public Uri getPlayingUrl() {
        return this.playingUrl;
    }

    private final ExtractorMediaSource.EventListener extractorEventListener = new ExtractorMediaSource.EventListener() {
        @Override
        public void onLoadError(IOException error) {
            Throwable cause = error.getCause();
            if (cause instanceof ConnectException || cause instanceof UnknownHostException) {
                reset();
                Player.this.playerListener.onNoNetwork();
            }
            else if (cause instanceof HttpDataSource.InvalidResponseCodeException) {
                int responseCode = ((HttpDataSource.InvalidResponseCodeException) cause).responseCode;
                switch (responseCode) {
                    case HttpURLConnection.HTTP_BAD_REQUEST:
                    case HttpURLConnection.HTTP_NOT_FOUND:
                        reset();
                        Player.this.playerListener.onContentNotFound();
                        break;
                }
            }
        }
    };

    @Override
    public void release() {
        this.handler.removeCallbacks(this.emitNewPosition);
        super.release();
    }

    private Runnable emitNewPosition = new Runnable() {
        @Override
        public void run() {
            Player.this.handler.removeCallbacks(this);

            Player.this.playerListener.onPositionChanged((int) (getCurrentPosition() / 1000));

            if (Player.this.getPlayWhenReady())
                Player.this.handler.postDelayed(this, 30 * 1000); // 30 secs
        }
    };

    private ExoPlayer.EventListener exoPlayerListener = new EventListener() {

        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest) {
            Player.this.emitNewPosition.run();
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
            if (playbackState == ExoPlayer.STATE_ENDED) {
                Player.this.stop();
                Player.this.playerListener.onCompleted();
                reset();
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            reset();
        }

        @Override
        public void onPositionDiscontinuity() {
            Player.this.emitNewPosition.run();
        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            // Do nothing
        }
    };

    private AudioFocus.AudioFocusListener audioFocusListener = new AudioFocus.AudioFocusListener() {
        @Override
        public void onPause() {
            pause();
        }

        @Override
        public void onResume() {
            resume();
        }
    };

    /* package */ interface PlayerListener {

        void onNoNetwork();
        void onContentNotFound();
        void onNoAudioFocus();

        void onPlay();
        void onPositionChanged(int positionSec);
        void onStop(int positionSec);
        void onCompleted();
    }
}
