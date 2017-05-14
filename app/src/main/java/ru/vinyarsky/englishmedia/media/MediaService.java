package ru.vinyarsky.englishmedia.media;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
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
import com.google.android.exoplayer2.ui.PlaybackControlView;
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
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import io.reactivex.Observable;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import ru.vinyarsky.englishmedia.EMApplication;
import ru.vinyarsky.englishmedia.EMPlaybackControlView;
import ru.vinyarsky.englishmedia.R;
import ru.vinyarsky.englishmedia.db.Episode;
import ru.vinyarsky.englishmedia.db.Podcast;

public class MediaService extends Service implements ExoPlayer.EventListener {

    // TODO Downloading

    // Intent actions for onStartCommand
    private static final String PLAY_PAUSE_TOGGLE_ACTION = "ru.vinyarsky.englishmedia.action.play_pause_toggle";
    private static final String DOWNLOAD_ACTION = "ru.vinyarsky.englishmedia.action.download";

    // Intent actions for broadcast receivers
    public static final String EPISODE_STATUS_CHANGED_BROADCAST_ACTION = "ru.vinyarsky.englishmedia.action.episode_status_changed";
    public static final String NO_NETWORK_BROADCAST_ACTION = "ru.vinyarsky.englishmedia.action.no_network";
    public static final String CONTENT_NOT_FOUND_BROADCAST_ACTION = "ru.vinyarsky.englishmedia.action.content_not_found";

    // Intent extra parameter for PLAY_PAUSE_TOGGLE_ACTION, DOWNLOAD_ACTION and EPISODE_STATUS_CHANGED_BROADCAST_ACTION
    public static final String EPISODE_CODE_EXTRA = "episode_code";

    private SimpleExoPlayer player;
    private ExtractorsFactory extractorsFactory;
    private DataSource.Factory dataSourceFactory;

    private CompositeDisposable compositeDisposable;

    private LocalBroadcastManager broadcastManager;

    private int mountedViewCount = 0;

    private UUID currentEpisodeCode;

    private Handler mainThreadHandler = new Handler();

    public final MediaServiceEventManagerImpl mediaServiceEventManager = new MediaServiceEventManagerImpl();

    private final BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Disconnecting headphones - stop playback
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                if (player != null && player.getPlayWhenReady()) {
                    player.setPlayWhenReady(false);
                    abandonAudioFocus();
                }
            }
        }
    };

    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            // No duck support because we primarily play speech (not music),
            // i.e. we always stop playing
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    player.setPlayWhenReady(true);
                    break;
                default:
                    player.setPlayWhenReady(false);
                    break;
            }
        }
    };

    private final ExtractorMediaSource.EventListener extractorEventListener = new ExtractorMediaSource.EventListener() {
        @Override
        public void onLoadError(IOException error) {
            Throwable cause = error.getCause();
            if (cause instanceof ConnectException || cause instanceof UnknownHostException) {
                reset();
                broadcastEmitNoNetwork();
            }
            else if (cause instanceof HttpDataSource.InvalidResponseCodeException) {
                int responseCode = ((HttpDataSource.InvalidResponseCodeException) cause).responseCode;
                switch (responseCode) {
                    case HttpURLConnection.HTTP_BAD_REQUEST:
                    case HttpURLConnection.HTTP_NOT_FOUND:
                        reset();
                        broadcastEmitContentNotFound();
                        break;
                }
            }
        }

        private void reset() {
            if (player != null && player.getPlayWhenReady()) {
                player.setPlayWhenReady(false);
                abandonAudioFocus();
            }
            mediaServiceEventManager.onEpisodeChanged(null, null);
            currentEpisodeCode = null;
        }
    };

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

        Cache cache = new SimpleCache(new File(this.getCacheDir().getAbsolutePath() + "/exoplayer"), new LeastRecentlyUsedCacheEvictor(1024 * 1024 * 100)); // 100 Mb max
        DataSource.Factory httpDataSourceFactory = new OkHttpDataSourceFactory(app.getHttpClient(), Util.getUserAgent(this, "EnglishMedia"), null);
        this.dataSourceFactory = new CacheDataSourceFactory(cache, httpDataSourceFactory, CacheDataSource.FLAG_BLOCK_ON_CACHE | CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);

        this.extractorsFactory = new DefaultExtractorsFactory();

        this.compositeDisposable = new CompositeDisposable();
        this.broadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
    }

    @Override
    public void onDestroy() {
        releasePlayer();
        this.compositeDisposable.dispose();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        switch (intent.getAction()) {
            case PLAY_PAUSE_TOGGLE_ACTION:
                this.compositeDisposable.add(
                        Observable.just((UUID) intent.getSerializableExtra(EPISODE_CODE_EXTRA))
                                .subscribeOn(Schedulers.io())
                                .map((episodeCode) -> Episode.read(((EMApplication) getApplication()).getDbHelper(), episodeCode))
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe((episode) -> {
                                    initPlayer();
                                    if (episode.getCode().equals(this.currentEpisodeCode)) {
                                        boolean newPlayWhenReady = !this.player.getPlayWhenReady();
                                        if (newPlayWhenReady) {
                                            if (requestAudioFocus())
                                                this.player.setPlayWhenReady(true);
                                        }
                                        else {
                                            this.player.setPlayWhenReady(false);
                                            abandonAudioFocus();
                                        }
                                    } else {
                                        ExtractorMediaSource mediaSource = new ExtractorMediaSource(Uri.parse(episode.getContentUrl()), this.dataSourceFactory, this.extractorsFactory, this.mainThreadHandler, this.extractorEventListener);
                                        this.player.seekTo(episode.getCurrentPosition() * 1000);
                                        this.player.prepare(mediaSource);
                                        this.currentEpisodeCode = episode.getCode();
                                        if (requestAudioFocus())
                                            this.player.setPlayWhenReady(true);
                                        this.compositeDisposable.add(
                                                Observable.just(episode.getPodcastCode())
                                                        .subscribeOn(Schedulers.io())
                                                        .map((podcastCode) -> Podcast.read(((EMApplication) getApplication()).getDbHelper(), podcastCode))
                                                        .observeOn(AndroidSchedulers.mainThread())
                                                        .subscribe((podcast) -> {
                                                            mediaServiceEventManager.onEpisodeChanged(podcast.getTitle(), episode.getTitle());
                                                        }));
                                    }
                                }));
                break;
            case DOWNLOAD_ACTION:
//                if (intent.getData() != null) {
//                    download(intent.getData());
//                }
                break;
        }
        return START_NOT_STICKY;
    }

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
            this.player.setPlayWhenReady(false);
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
        this.mainThreadHandler.removeCallbacks(this::releasePlayer);
        if ((!playWhenReady)
                || (playbackState == ExoPlayer.STATE_IDLE)
                || (playbackState == ExoPlayer.STATE_ENDED))
        {
            saveCurrentPosition(true);
            this.mainThreadHandler.postDelayed(this::releasePlayer, 1000 * 60 * 5); // 5 mins
        }

        if (playWhenReady) {
            registerReceiver(this.becomingNoisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
            Notification notification = new Notification.Builder(this)
                    .setSmallIcon(R.mipmap.ic_notification)
                    .setContentTitle("EnglishMedia playback") // TODO Add to resource
                    .build();
            startForeground(123, notification);
        } else {
            stopForeground(true);
            try {
                unregisterReceiver(this.becomingNoisyReceiver);
            } catch (IllegalArgumentException e) {
                // Do nothing.
                // onPlayerStateChanged is called multiple times with different playbackState,
                // so it can be called with playWhenReady == false more then once and
                // we unregister not registered receiver.
            }
        }
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        // Do nothing
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

        this.compositeDisposable.add(
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
                        }));
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

    private void broadcastEmitNoNetwork() {
        Intent broadcast_intent = new Intent(NO_NETWORK_BROADCAST_ACTION);
        this.broadcastManager.sendBroadcast(broadcast_intent);
    }

    private void broadcastEmitContentNotFound() {
        Intent broadcast_intent = new Intent(CONTENT_NOT_FOUND_BROADCAST_ACTION);
        this.broadcastManager.sendBroadcast(broadcast_intent);
    }

    /**
     * @return true - success
     */
    private boolean requestAudioFocus() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this.audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private void abandonAudioFocus() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(this.audioFocusChangeListener);
    }

    public class MediaServiceBinder extends Binder {

        public void mountPlaybackControlView(EMPlaybackControlView view) {
            initPlayer();
            MediaService.this.mountedViewCount++;
            view.setPlayer(MediaService.this.player);
            view.setMediaServiceEventManager(MediaService.this.mediaServiceEventManager);
            view.setControlDispatcher(new PlaybackControlView.ControlDispatcher() {

                @Override
                public boolean dispatchSetPlayWhenReady(ExoPlayer player, boolean playWhenReady) {
                    if (playWhenReady) {
                        boolean result = requestAudioFocus();
                        if (result)
                            player.setPlayWhenReady(true);
                        return result;
                    }
                    else {
                        abandonAudioFocus();
                        player.setPlayWhenReady(false);
                        return true;
                    }
                }

                @Override
                public boolean dispatchSeekTo(ExoPlayer player, int windowIndex, long positionMs) {
                    player.seekTo(windowIndex, positionMs);
                    return true;
                }
            });
        }

        public void unMountPlaybackControlView(EMPlaybackControlView view) {
            MediaService.this.mountedViewCount--;
            view.setPlayer(null);
            view.setMediaServiceEventManager(null);
            view.setControlDispatcher(null);
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
