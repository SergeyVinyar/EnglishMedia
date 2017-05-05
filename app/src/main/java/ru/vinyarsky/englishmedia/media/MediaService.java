package ru.vinyarsky.englishmedia.media;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

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
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import io.reactivex.Observable;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import ru.vinyarsky.englishmedia.EMApplication;
import ru.vinyarsky.englishmedia.EMPlaybackControlView;
import ru.vinyarsky.englishmedia.db.Episode;
import ru.vinyarsky.englishmedia.db.Podcast;

public class MediaService extends Service implements ExoPlayer.EventListener {

    // TODO Foreground service
    // TODO Downloading

    // Intent actions for onStartCommand
    private static final String PLAY_PAUSE_TOGGLE_ACTION = "ru.vinyarsky.englishmedia.action.play_pause_toggle";
    private static final String DOWNLOAD_ACTION = "ru.vinyarsky.englishmedia.action.download";

    // Intent action for broadcast receivers
    public static final String EPISODE_STATUS_CHANGED_BROADCAST_ACTION = "ru.vinyarsky.englishmedia.action.episode_status_changed";

    // Intent extra parameter for PLAY_PAUSE_TOGGLE_ACTION, DOWNLOAD_ACTION and EPISODE_STATUS_CHANGED_BROADCAST_ACTION
    public static final String EPISODE_CODE_EXTRA = "episode_code";

    private SimpleExoPlayer player;
    private ExtractorsFactory extractorsFactory;
    private DataSource.Factory dataSourceFactory;

    private LocalBroadcastManager broadcastManager;

    private int mountedViewCount = 0;

    private UUID currentEpisodeCode;

    private Handler releaseDelayedHandler = new Handler();

    public final MediaServiceEventManagerImpl mediaServiceEventManager = new MediaServiceEventManagerImpl();

    public static Intent newPlayPauseToggleIntent(Context appContext, UUID episodeCode) {
        Intent intent = new Intent(PLAY_PAUSE_TOGGLE_ACTION, null, appContext, MediaService.class);
        intent.putExtra(EPISODE_CODE_EXTRA, episodeCode);
        return intent;
    }

    public static Intent newDownloadIntent(Context appContext, UUID episodeCode) {
        Intent intent = new Intent(DOWNLOAD_ACTION, null, appContext, MediaService.class);
        intent.putExtra(EPISODE_CODE_EXTRA, episodeCode);
        return intent;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EMApplication app = (EMApplication) getApplication();

        Cache cache = new SimpleCache(new File(this.getCacheDir().getAbsolutePath() + "/exoplayer"), new LeastRecentlyUsedCacheEvictor(1024 * 1024 * 100));
        DataSource.Factory httpDataSourceFactory = new OkHttpDataSourceFactory(app.getHttpClient(), Util.getUserAgent(this, "EnglishMedia"), null);
        this.dataSourceFactory = new CacheDataSourceFactory(cache, httpDataSourceFactory, CacheDataSource.FLAG_BLOCK_ON_CACHE | CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);

        this.extractorsFactory = new DefaultExtractorsFactory();

        this.broadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
    }

    @Override
    public void onDestroy() {
        releasePlayer();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        switch (intent.getAction()) {
            case PLAY_PAUSE_TOGGLE_ACTION:
                Observable.just((UUID) intent.getSerializableExtra(EPISODE_CODE_EXTRA))
                        .subscribeOn(Schedulers.io())
                        .map((episodeCode) -> Episode.read(((EMApplication) getApplication()).getDbHelper(), episodeCode))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe((episode) -> {
                            initPlayer();
                            if (episode.getCode().equals(this.currentEpisodeCode)) {
                                this.player.setPlayWhenReady(!this.player.getPlayWhenReady());
                            } else {
                                ExtractorMediaSource mediaSource = new ExtractorMediaSource(Uri.parse(episode.getContentUrl()), this.dataSourceFactory, this.extractorsFactory, null, null);
                                this.player.seekTo(episode.getCurrentPosition() * 1000);
                                this.player.prepare(mediaSource);
                                this.player.setPlayWhenReady(true);
                                this.currentEpisodeCode = episode.getCode();
                                Observable.just(episode.getPodcastCode())
                                        .subscribeOn(Schedulers.io())
                                        .map((podcastCode) -> Podcast.read(((EMApplication) getApplication()).getDbHelper(), podcastCode))
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe((podcast) -> {
                                            mediaServiceEventManager.onEpisodeChanged(podcast.getTitle(), episode.getTitle());
                                        });
                            }
                        });
                break;
            case DOWNLOAD_ACTION:
//                if (intent.getData() != null) {
//                    download(intent.getData());
//                }
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
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        // Do nothing
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
        // Do nothing
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        this.releaseDelayedHandler.removeCallbacks(this::releasePlayer);
        if ((!playWhenReady)
                || (playbackState == ExoPlayer.STATE_IDLE)
                || (playbackState == ExoPlayer.STATE_ENDED))
        {
            saveCurrentPosition(true);
            this.releaseDelayedHandler.postDelayed(this::releasePlayer, 1000 * 60 * 5);
        }
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        // Do nothing
    }

    @Override
    public void onPositionDiscontinuity() {
        saveCurrentPosition(false);
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {
        saveCurrentPosition(false);
    }

    /**
     * Save current position of the track
     * @param immediate true - save immediately, false - collect changes for 30 seconds and then save the result
     */
    private void saveCurrentPosition(boolean immediate) {
        if (this.player == null)
            return;
        if (this.currentEpisodeCode == null)
            return;

        if (!immediate && (System.nanoTime() - this.lastSaveCurrentPositionNanoTime < 30L * 1000_000_000)) // 30 seconds
            return;
        this.lastSaveCurrentPositionNanoTime = System.nanoTime();

        Observable.just(new Object[] { this.currentEpisodeCode, this.player.getCurrentPosition() })
                .observeOn(Schedulers.io())
                .map((data) -> {
                    EMApplication app = (EMApplication) getApplication();
                    Episode episode = Episode.read(app.getDbHelper(), (UUID) data[0]);
                    if (episode != null) {
                        Episode.EpisodeStatus oldStatus = episode.getStatus();

                        episode.setCurrentPosition((int)((Long) data[1] / 1000));
                        if (((Long)data[1]).compareTo(episode.getDuration() * 1000L) >= 0) {
                            episode.setStatus(Episode.EpisodeStatus.COMPLETED);
                            episode.setCurrentPosition(0); // Next time we listen from the beginning
                        }
                        else {
                            // No LISTENING for already COMPLETED episodes
                            if(Episode.EpisodeStatus.NEW.equals(episode.getStatus()))
                                episode.setStatus(Episode.EpisodeStatus.LISTENING);
                        }

                        episode.write(app.getDbHelper());

                        if (!episode.getStatus().equals(oldStatus)) {
                            broadcastEmitEpisodeStatusChanged(episode.getCode());
                        }

                        return episode.getStatus();
                    }
                    return Episode.EpisodeStatus.COMPLETED;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((status) -> {
                    if (Episode.EpisodeStatus.COMPLETED.equals(status)) {
                        this.currentEpisodeCode = null;
                        this.mediaServiceEventManager.onEpisodeChanged(null, null);
                    }
                });
    }

    /**
     * System.nanoTime value of the latest saveCurrentPosition saving
     */
    private long lastSaveCurrentPositionNanoTime = 0;

    private void broadcastEmitEpisodeStatusChanged(UUID episodeCode) {
        Intent broadcast_intent = new Intent(EPISODE_STATUS_CHANGED_BROADCAST_ACTION);
        broadcast_intent.putExtra(EPISODE_CODE_EXTRA, episodeCode);
        this.broadcastManager.sendBroadcast(broadcast_intent);
    }

    public class MediaServiceBinder extends Binder {

        public void mountPlaybackControlView(EMPlaybackControlView view) {
            initPlayer();
            MediaService.this.mountedViewCount++;
            view.setPlayer(MediaService.this.player);
            view.setMediaServiceEventManager(MediaService.this.mediaServiceEventManager);
        }

        public void unMountPlaybackControlView(EMPlaybackControlView view) {
            MediaService.this.mountedViewCount--;
            view.setPlayer(null);
            view.setMediaServiceEventManager(null);
        }
    }

    /**
     * Additional events that are absent in ExoPlayer.EventListener
     */
    public interface MediaServiceListener {

        /**
         * Started playback of an episode
         */
        void onEpisodeChanged(String podcastTitle, String episodeTitle);
    }

    public interface MediaServiceEventManager {

        void addListener(MediaServiceListener listener);
        void removeListener(MediaServiceListener listener);

        String getPodcastTitle();
        String getEpisodeTitle();
    }

    private class MediaServiceEventManagerImpl implements MediaServiceEventManager {

        private final CopyOnWriteArraySet<MediaServiceListener> listeners = new CopyOnWriteArraySet<>();
        private String podcastTitle;
        private String episodeTitle;

        @Override
        public void addListener(MediaServiceListener listener) {
            this.listeners.add(listener);
        }

        @Override
        public void removeListener(MediaServiceListener listener) {
            this.listeners.remove(listener);
        }

        private void onEpisodeChanged(String podcastTitle, String episodeTitle) {
            this.podcastTitle = podcastTitle;
            this.episodeTitle = episodeTitle;
            for (MediaServiceListener listener: this.listeners)
                listener.onEpisodeChanged(this.podcastTitle, this.episodeTitle);
        }

        @Override
        public String getPodcastTitle() {
            return this.podcastTitle;
        }

        @Override
        public String getEpisodeTitle() {
            return this.episodeTitle;
        }
    }
}
