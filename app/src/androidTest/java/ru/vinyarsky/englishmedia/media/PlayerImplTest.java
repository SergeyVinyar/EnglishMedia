package ru.vinyarsky.englishmedia.media;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import static org.mockito.Mockito.*;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

@RunWith(AndroidJUnit4.class)
public class PlayerImplTest {

    private Context testAppContext;
    private Context targetContext;

    private HandlerThread handlerThread;
    private Handler handler;

    private Random random = new Random();
    private Player player;

    @Before
    public void before() {
        testAppContext = InstrumentationRegistry.getContext();
        targetContext = InstrumentationRegistry.getTargetContext();
    }

    @After
    public void after() throws Exception {
        runAsyncAndWait(() -> {
            player.release();
        });
    }

    /**
     * Random URL to avoid cache influence
     */
    private Uri getRandomUrl() {
        return Uri.parse("http://www.abc.com/" + random.nextInt());
    }

    private AudioFocus PrepareAudioFocusSuccess() {
        AudioFocus audioFocus = mock(AudioFocus.class);
        when(audioFocus.ensureAudioFocus()).thenReturn(true);
        return audioFocus;
    }

    private AudioFocus PrepareAudioFocusFail() {
        AudioFocus audioFocus = mock(AudioFocus.class);
        when(audioFocus.ensureAudioFocus()).thenReturn(false);
        return audioFocus;
    }

    private OkHttpClient PrepareHttpSuccess() throws IOException {
        OkHttpClient httpClient = mock(OkHttpClient.class);

        when(httpClient.newCall(any(Request.class))).then(args -> {
            Request request = args.getArgumentAt(0, Request.class);

            InputStream data = testAppContext.getResources().getAssets().open("test.mp3");
            Response response = new Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .body(new ResponseBody() {
                        @Override
                        public MediaType contentType() {
                            return MediaType.parse("audio/mpeg");
                        }

                        @Override
                        public long contentLength() {
                            return 1024 * 883;
                        }

                        @Override
                        public BufferedSource source() {
                            try {
                                return new Buffer().readFrom(data);
                            } catch (IOException e) {
                                return null;
                            }
                        }
                    })
                    .build();

            Call call = mock(Call.class);
            when(call.execute()).thenReturn(response);
            return call;
        });

        return httpClient;
    }

    private OkHttpClient PrepareHttp404() throws IOException {
        OkHttpClient httpClient = mock(OkHttpClient.class);

        when(httpClient.newCall(any(Request.class))).then(args -> {
            Request request = args.getArgumentAt(0, Request.class);

            Response response = new Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(404)
                    .body(new ResponseBody() {
                        @Override
                        public MediaType contentType() {
                            return MediaType.parse("audio/mpeg");
                        }

                        @Override
                        public long contentLength() {
                            return 0;
                        }

                        @Override
                        public BufferedSource source() {
                            return new Buffer();
                        }
                    })
                    .build();

            Call call = mock(Call.class);
            when(call.execute()).thenReturn(response);
            return call;
        });

        return httpClient;
    }

    private void runAsyncAndWait(Runnable runnable) throws InterruptedException {
        if (handlerThread == null) {
            handlerThread = new HandlerThread("TestThread");
            handlerThread.start();
            handler = new Handler(handlerThread.getLooper());
        }
        CountDownLatch countDownLatch = new CountDownLatch(1);
        handler.post(() -> {
            runnable.run();
            countDownLatch.countDown();
        });
        countDownLatch.await();
    }

    /**
     * Normal playback start and stop
     */
    @Test
    public void playAndStop() throws Exception {
        AudioFocus audioFocus = PrepareAudioFocusSuccess();
        OkHttpClient httpClient = PrepareHttpSuccess();
        Player.PlayerListener playerListener = mock(Player.PlayerListener.class);
        Uri url = getRandomUrl();

        runAsyncAndWait(() -> {
            player = new PlayerImpl(targetContext, audioFocus, httpClient);
            player.addListener(playerListener);
        });

        assertEquals(null, player.getPlayingUrl());

        runAsyncAndWait(() -> {
            player.play(url, 0);
        });

        // onPlay occurs after initial buffering
        Thread.sleep(1000);

        assertEquals(url, player.getPlayingUrl());
        verify(audioFocus).ensureAudioFocus();
        verify(playerListener).onPlay();

        Thread.sleep(70 * 1000);

        // 3 invocations with different time positions
        ArgumentCaptor<Integer> onPositionChangedArguments1 = ArgumentCaptor.forClass(Integer.class);
        verify(playerListener, times(3)).onPositionChanged(onPositionChangedArguments1.capture());
        List<Integer> args = onPositionChangedArguments1.getAllValues();
        assertEquals(args.size(), args.stream().distinct().count());

        runAsyncAndWait(() -> {
            player.stop();
        });

        verify(playerListener).onStop(anyInt());
        verify(audioFocus).abandonAudioFocus();

        // + 4th invocation
        ArgumentCaptor<Integer> onPositionChangedArguments2 = ArgumentCaptor.forClass(Integer.class);
        verify(playerListener, times(4)).onPositionChanged(onPositionChangedArguments2.capture());
        args = onPositionChangedArguments2.getAllValues();
        assertEquals(args.size(), args.stream().distinct().count());
    }

    /**
     * Playback two files
     */
    @Test
    public void playTwoMedia() throws Exception {
        AudioFocus audioFocus = PrepareAudioFocusSuccess();
        OkHttpClient httpClient = PrepareHttpSuccess();
        Player.PlayerListener playerListener = mock(Player.PlayerListener.class);
        Uri url1 = getRandomUrl();

        runAsyncAndWait(() -> {
            player = new PlayerImpl(targetContext, audioFocus, httpClient);
            player.addListener(playerListener);
            player.play(url1, 0);
        });

        Thread.sleep(10 * 1000);

        assertEquals(url1, player.getPlayingUrl());
        verify(playerListener).onPlay();

        runAsyncAndWait(() -> {
            player.stop();
        });

        Thread.sleep(10 * 1000);

        assertEquals(url1, player.getPlayingUrl());
        verify(playerListener).onStop(anyInt());

        Uri url2 = getRandomUrl();

        runAsyncAndWait(() -> {
            player.play(url2, 0);
        });

        Thread.sleep(10 * 1000);

        assertEquals(url2, player.getPlayingUrl());
        verify(playerListener, times(2)).onPlay();
        verify(playerListener, never()).onCompleted();

        runAsyncAndWait(() -> {
            player.stop();
        });

        assertEquals(url2, player.getPlayingUrl());
        verify(playerListener, times(2)).onStop(anyInt());
    }

    /**
     * Playback two files: one to the end and other for a while
     */
    @Test
    public void playOneFileToCompletedThenSecondFile() throws Exception {
        AudioFocus audioFocus = PrepareAudioFocusSuccess();
        OkHttpClient httpClient = PrepareHttpSuccess();
        Player.PlayerListener playerListener = mock(Player.PlayerListener.class);
        Uri url1 = getRandomUrl();

        runAsyncAndWait(() -> {
            player = new PlayerImpl(targetContext, audioFocus, httpClient);
            player.addListener(playerListener);
            player.play(url1, 0);
        });

        // test.mp3 file is 1:41 length
        Thread.sleep((1 * 60 + 50) * 1000);

        assertEquals(null, player.getPlayingUrl());
        verify(playerListener).onPlay();
        verify(playerListener).onStop(anyInt());
        verify(playerListener).onCompleted();

        Thread.sleep(10 * 1000);

        Uri url2 = getRandomUrl();

        runAsyncAndWait(() -> {
            player.play(url2, 0);
        });

        Thread.sleep(10 * 1000);

        assertEquals(url2, player.getPlayingUrl());
        verify(playerListener, times(2)).onPlay();
        verify(playerListener).onStop(anyInt());
        verify(playerListener).onCompleted();

        runAsyncAndWait(() -> {
            player.stop();
        });

        assertEquals(url2, player.getPlayingUrl());
        verify(playerListener, times(2)).onPlay();
        verify(playerListener, times(2)).onStop(anyInt());
        verify(playerListener).onCompleted();
    }

    /**
     * Couldn't acquire audio focus
     */
    @Test
    public void playNoAudioFocus() throws Exception {
        AudioFocus audioFocus = PrepareAudioFocusFail();
        OkHttpClient httpClient = PrepareHttpSuccess();
        Player.PlayerListener playerListener = mock(Player.PlayerListener.class);
        Uri url = getRandomUrl();

        runAsyncAndWait(() -> {
            player = new PlayerImpl(targetContext, audioFocus, httpClient);
            player.addListener(playerListener);
            player.play(url, 0);
        });

        verify(playerListener).onNoAudioFocus();
        verify(playerListener, never()).onPlay();
        verify(playerListener, never()).onPositionChanged(anyInt());
    }

    /**
     * HTTP 404
     */
    @Test
    public void playContentNotFound() throws Exception {
        AudioFocus audioFocus = PrepareAudioFocusSuccess();
        OkHttpClient httpClient = PrepareHttp404();
        Player.PlayerListener playerListener = mock(Player.PlayerListener.class);
        Uri url = getRandomUrl();

        runAsyncAndWait(() -> {
            player = new PlayerImpl(targetContext, audioFocus, httpClient);
            player.addListener(playerListener);
            player.play(url, 0);
        });

        // OkHttp tries to get content several times
        Thread.sleep(5 * 1000);

        verify(playerListener).onPlay();
        verify(playerListener).onStop(anyInt());
        verify(playerListener, atLeastOnce()).onContentNotFound();

        assertEquals(false, player.asExoPlayer().getPlayWhenReady());
    }

    /**
     * Play until the end of file
     */
    @Test
    public void playToComplete() throws Exception {
        AudioFocus audioFocus = PrepareAudioFocusSuccess();
        OkHttpClient httpClient = PrepareHttpSuccess();
        Player.PlayerListener playerListener = mock(Player.PlayerListener.class);
        Uri url = getRandomUrl();

        runAsyncAndWait(() -> {
            player = new PlayerImpl(targetContext, audioFocus, httpClient);
            player.addListener(playerListener);
            player.play(url, 0);
        });

        // test.mp3 file is 1:41 length
        Thread.sleep((1 * 60 + 50) * 1000);

        verify(playerListener).onStop(anyInt());
        verify(playerListener).onCompleted();
    }
}