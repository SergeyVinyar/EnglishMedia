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
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ui.PlaybackControlView;

import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import ru.vinyarsky.englishmedia.EMApplication;
import ru.vinyarsky.englishmedia.EMPlaybackControlView;
import ru.vinyarsky.englishmedia.R;
import ru.vinyarsky.englishmedia.db.DbHelper;
import ru.vinyarsky.englishmedia.db.Episode;
import ru.vinyarsky.englishmedia.db.Podcast;

public class MediaService extends Service {

    // TODO Implement explicit downloading by a user

    private static final String PLAY_PAUSE_TOGGLE_ACTION = "ru.vinyarsky.englishmedia.action.play_pause_toggle";
    private static final String DOWNLOAD_ACTION = "ru.vinyarsky.englishmedia.action.download";

    public static final String EPISODE_STATUS_CHANGED_BROADCAST_ACTION = "ru.vinyarsky.englishmedia.action.episode_status_changed";
    public static final String NO_NETWORK_BROADCAST_ACTION = "ru.vinyarsky.englishmedia.action.no_network";
    public static final String CONTENT_NOT_FOUND_BROADCAST_ACTION = "ru.vinyarsky.englishmedia.action.content_not_found";

    public static final String EPISODE_CODE_EXTRA = "episode_code";
    public static final String EPISODE_URL_EXTRA = "episode_url";

    private Player player;
    private DbHelper dbHelper;

    private CompositeDisposable compositeDisposable;

    private final MediaServiceEventManagerImpl mediaServiceEventManager = new MediaServiceEventManagerImpl();

    private LocalBroadcastManager broadcastManager;

    public MediaService() {
        super();
        this.player = EMApplication.getMediaComponent().getPlayer();
        this.dbHelper = EMApplication.getEmComponent().getDbHelper();
    }

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
        this.compositeDisposable = new CompositeDisposable();
        this.broadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        this.player.addListener(this.playerListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.compositeDisposable.dispose();
        this.player.removeListener(this.playerListener);
        this.player.release();
        this.player = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        switch (intent.getAction()) {
            case PLAY_PAUSE_TOGGLE_ACTION:
                this.compositeDisposable.add(
                        Observable.just((UUID) intent.getSerializableExtra(EPISODE_CODE_EXTRA))
                                .subscribeOn(Schedulers.io())
                                .map((episodeCode) -> Episode.read(this.dbHelper, episodeCode))
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe((episode) -> {
                                    Uri contentUrl = Uri.parse(episode.getContentUrl());
                                    if (contentUrl.equals(this.player.getPlayingUrl())) {
                                        this.player.togglePlayStop();
                                    }
                                    else {
                                        this.player.play(contentUrl, episode.getCurrentPosition());
                                        this.compositeDisposable.add(
                                                Observable.just(episode.getPodcastCode())
                                                        .subscribeOn(Schedulers.io())
                                                        .map((podcastCode) -> Podcast.read(this.dbHelper, podcastCode))
                                                        .observeOn(AndroidSchedulers.mainThread())
                                                        .subscribe((podcast) -> {
                                                            {
                                                                Bundle bundle = new Bundle();
                                                                bundle.putString("podcast_title", podcast.getTitle());
                                                                bundle.putString("episode_title", episode.getTitle());
                                                                EMApplication.getEmComponent().getFirebaseAnalytics().logEvent("play_episode", bundle);
                                                            }
                                                            mediaServiceEventManager.onEpisodeChanged(podcast.getTitle(), episode.getTitle());
                                                        }));
                                    }
                                }));
                break;
            case DOWNLOAD_ACTION:
                throw new IllegalArgumentException();
// TODO
//                if (intent.getData() != null) {
//                    download(intent.getData());
//                }
//                break;
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MediaServiceBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }

    private void download(Uri uri) {
// TODO
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

    Player.PlayerListener playerListener = new Player.PlayerListener() {

        @Override
        public void onNoNetwork() {
            Intent broadcast_intent = new Intent(NO_NETWORK_BROADCAST_ACTION);
            MediaService.this.broadcastManager.sendBroadcast(broadcast_intent);
        }

        @Override
        public void onContentNotFound() {
            Intent broadcast_intent = new Intent(CONTENT_NOT_FOUND_BROADCAST_ACTION);
            broadcast_intent.putExtra(EPISODE_URL_EXTRA, MediaService.this.player.getPlayingUrl());
            MediaService.this.broadcastManager.sendBroadcast(broadcast_intent);
        }

        @Override
        public void onNoAudioFocus() {
            // Do nothing
        }

        @Override
        public void onPlay() {
            Uri playingUrl = MediaService.this.player.getPlayingUrl();
            registerReceiver(MediaService.this.becomingNoisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));

            Notification notification = new Notification.Builder(MediaService.this)
                    .setSmallIcon(R.mipmap.ic_notification)
                    .setContentTitle(getResources().getString(R.string.media_service_notification_title))
                    .build();
            startForeground(123, notification);
            MediaService.this.compositeDisposable.add(
                    Observable.just(playingUrl)
                            .subscribeOn(Schedulers.io())
                            .subscribe(url -> {
                                Episode.setStatusListeningIfRequired(MediaService.this.dbHelper, url.toString());
                                this.broadcastEmitEpisodeStatusChanged();
                            }));

        }

        @Override
        public void onPositionChanged(int positionSec) {
            Uri playingUrl = MediaService.this.player.getPlayingUrl();
            MediaService.this.compositeDisposable.add(
                    Observable.just(playingUrl)
                            .subscribeOn(Schedulers.io())
                            .subscribe(url -> {
                                Episode.updatePosition(MediaService.this.dbHelper, url.toString(), positionSec);
                            }));
        }

        @Override
        public void onStop(int positionSec) {
            stopForeground(true);
            unregisterReceiver(MediaService.this.becomingNoisyReceiver);
            onPositionChanged(positionSec);
        }

        @Override
        public void onCompleted() {
            Uri playingUrl = MediaService.this.player.getPlayingUrl();
            MediaService.this.compositeDisposable.add(
                    Observable.just(playingUrl)
                            .subscribeOn(Schedulers.io())
                            .doOnNext(url -> {
                                Episode.setStatusCompleted(MediaService.this.dbHelper, url.toString());
                            })
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(url -> {
                                MediaService.this.mediaServiceEventManager.onEpisodeChanged(null, null);
                                this.broadcastEmitEpisodeStatusChanged();
                                MediaService.this.stopSelf();
                            }));
        }

        private void broadcastEmitEpisodeStatusChanged() {
            Intent broadcast_intent = new Intent(EPISODE_STATUS_CHANGED_BROADCAST_ACTION);
            MediaService.this.broadcastManager.sendBroadcast(broadcast_intent);
        }
    };

    private final BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Disconnecting headphones - stop playback
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                player.stop();
            }
        }
    };

    public class MediaServiceBinder extends Binder {

        public void mountPlaybackControlView(EMPlaybackControlView view) {
            view.setPlayer(MediaService.this.player.asExoPlayer());
            view.setMediaServiceEventManager(MediaService.this.mediaServiceEventManager);
            view.setControlDispatcher(new PlaybackControlView.ControlDispatcher() {

                @Override
                public boolean dispatchSetPlayWhenReady(ExoPlayer player, boolean playWhenReady) {
                    MediaService.this.player.togglePlayStop();
                    return true;
                }

                @Override
                public boolean dispatchSeekTo(ExoPlayer player, int windowIndex, long positionMs) {
                    MediaService.this.player.asExoPlayer().seekTo(windowIndex, positionMs);
                    return true;
                }
            });
        }

        public void unMountPlaybackControlView(EMPlaybackControlView view) {
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
