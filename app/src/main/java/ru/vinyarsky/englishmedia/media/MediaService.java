package ru.vinyarsky.englishmedia.media;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;

import java.io.File;

import ru.vinyarsky.englishmedia.EMApplication;

public class MediaService extends Service implements ExoPlayer.EventListener {

    // TODO Add current position remembering
    // TODO Foreground service
    // TODO Downloading

    public static final String PLAY_ACTION = "ru.vinyarsky.englishmedia.action.play";
    public static final String DOWNLOAD_ACTION = "ru.vinyarsky.englishmedia.action.download";

    private SimpleExoPlayer player;
    private ExtractorsFactory extractorsFactory;
    private DataSource.Factory dataSourceFactory;

    private int mountedViewCount = 0;

    private Handler releaseDelayedHandler = new Handler();

    public static Intent newPlayIntent(Context appContext, Uri uri) {
        return new Intent(PLAY_ACTION, uri, appContext, MediaService.class);
    }

    public static Intent newDownloadIntent(Context appContext, Uri uri) {
        return new Intent(DOWNLOAD_ACTION, uri, appContext, MediaService.class);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EMApplication app = (EMApplication) getApplication();

        Cache cache = new SimpleCache(new File(this.getCacheDir().getAbsolutePath() + "/exoplayer"), new LeastRecentlyUsedCacheEvictor(1024 * 1024 * 100));
        DataSource.Factory httpDataSourceFactory = new OkHttpDataSourceFactory(app.getHttpClient(), Util.getUserAgent(this, "EnglishMedia"), null);
        this.dataSourceFactory = new CacheDataSourceFactory(cache, httpDataSourceFactory, CacheDataSource.FLAG_BLOCK_ON_CACHE | CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);

        this.extractorsFactory = new DefaultExtractorsFactory();
    }

    @Override
    public void onDestroy() {
        releasePlayer();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        switch (intent.getAction()) {
            case PLAY_ACTION:
                initPlayer();
                if (intent.getData() != null) {
                    ExtractorMediaSource mediaSource = new ExtractorMediaSource(intent.getData(), this.dataSourceFactory, this.extractorsFactory, null, null);
                    this.player.prepare(mediaSource);
                    this.player.setPlayWhenReady(true);
                }
                else {
                    // Just play already prepared media
                    this.player.setPlayWhenReady(true);
                }
                break;
            case DOWNLOAD_ACTION:
                if (intent.getData() != null) {
                    download(intent.getData());
                }
                break;
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MediaServiceBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (!this.player.getPlayWhenReady()
                || this.player.getPlaybackState() == ExoPlayer.STATE_IDLE
                || this.player.getPlaybackState() == ExoPlayer.STATE_ENDED)
        {
            releasePlayer();
        }
        return false;
    }

    private synchronized void initPlayer() {
        if (this.player == null) {
            this.player = ExoPlayerFactory.newSimpleInstance(this, new DefaultTrackSelector(), new DefaultLoadControl());
            this.player.setPlayWhenReady(true);
            this.player.addListener(this);
        }
    }

    private synchronized void releasePlayer() {
        if (this.player != null && this.mountedViewCount <= 0) {
            this.player.removeListener(this);
            this.player.release();
            this.player = null;
        }
    }

    private void download(Uri uri) {
//        Thread t = new Thread(() -> {
//            try {
//                DataSource ds = dataSourceFactory.createDataSource();
//
//                ds.open(new DataSpec(Uri.parse("http://open.live.bbc.co.uk/mediaselector/5/redir/version/2.0/mediaset/audio-nondrm-download-low/proto/http/vpid/p04z6zdy.mp3"),
//                        DataSpec.FLAG_ALLOW_GZIP
//                ));
//
//                byte[] buffer = new byte[1024 * 1024];
//                int readCount = 0;
//                while (readCount != C.RESULT_END_OF_INPUT) {
//                    readCount = ds.read(buffer, 0, buffer.length);
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
//
//
//
//        try {
//            t.start();
//            t.join();
//        } catch (Exception e) {
//
//        }
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        this.releaseDelayedHandler.removeCallbacks(this::releasePlayer);
        if ((!playWhenReady)
                || (playbackState == ExoPlayer.STATE_IDLE)
                || (playbackState == ExoPlayer.STATE_ENDED))
        {
            this.releaseDelayedHandler.postDelayed(this::releasePlayer, 1000 * 60 * 5);
        }
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {

    }

    @Override
    public void onPositionDiscontinuity() {

    }

    public class MediaServiceBinder extends Binder {

        public void mountPlaybackControlView(PlaybackControlView view) {
            initPlayer();
            MediaService.this.mountedViewCount++;
            view.setPlayer(MediaService.this.player);
        }

        public void unMountPlaybackControlView(PlaybackControlView view) {
            MediaService.this.mountedViewCount--;
            view.setPlayer(null);
        }
    }
}
