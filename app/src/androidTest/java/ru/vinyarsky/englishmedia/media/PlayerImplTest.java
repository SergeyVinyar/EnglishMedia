package ru.vinyarsky.englishmedia.media;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
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

    private HandlerThread handlerThread;
    private Handler handler;

    private Context targetContext;
    private Context testAppContext;
    private AudioFocus audioFocus;
    private OkHttpClient httpClient;

    private Player player;

    private static final String FAKE_URL = "http://www.abc.com/test_mp3";

    @Before
    public void before() {
        targetContext = InstrumentationRegistry.getTargetContext();
        testAppContext = InstrumentationRegistry.getContext();
        audioFocus = mock(AudioFocus.class);
        httpClient = mock(OkHttpClient.class);
    }

    private void PrepareAudioFocusSuccess() {
        when(audioFocus.ensureAudioFocus()).thenReturn(true);
    }

    private void PrepareHttpSuccess() throws IOException {
        InputStream data = testAppContext.getResources().getAssets().open("test.mp3");

        when(httpClient.newCall(any(Request.class))).then(args -> {
            Request request = args.getArgumentAt(0, Request.class);

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
    }

    private void runAsync(Runnable runnable) {
        if (handlerThread == null) {
            handlerThread = new HandlerThread("TestThread");
            handlerThread.start();
            handler = new Handler(handlerThread.getLooper());
        }
        handler.post(runnable);
    }

    @Test
    public void playAndStop() throws Exception {
        PrepareAudioFocusSuccess();
        PrepareHttpSuccess();
        Player.PlayerListener playerListener = mock(Player.PlayerListener.class);

        CountDownLatch countDownLatch1 = new CountDownLatch(1);
        runAsync(() -> {
            player = new PlayerImpl(targetContext, audioFocus, httpClient);
            player.addListener(playerListener);
            countDownLatch1.countDown();
        });

        countDownLatch1.await();
        assertEquals(null, player.getPlayingUrl());

        CountDownLatch countDownLatch2 = new CountDownLatch(1);
        runAsync(() -> {
            player.play(Uri.parse(FAKE_URL), 0);
            countDownLatch2.countDown();
        });

        countDownLatch2.await();
        assertEquals(Uri.parse(FAKE_URL), player.getPlayingUrl());
        verify(audioFocus).ensureAudioFocus();
        verify(playerListener).onPlay();

        Thread.sleep(70 * 1000);

        // 3 invocations with different time positions
        ArgumentCaptor<Integer> onPositionChangedArguments1 = ArgumentCaptor.forClass(Integer.class);
        verify(playerListener, times(3)).onPositionChanged(onPositionChangedArguments1.capture());
        List<Integer> args = onPositionChangedArguments1.getAllValues();
        assertEquals(args.size(), args.stream().distinct().count());

        CountDownLatch countDownLatch3 = new CountDownLatch(1);
        runAsync(() -> {
            player.stop();
            countDownLatch3.countDown();
        });

        countDownLatch3.await();
        verify(playerListener).onStop(anyInt());
        verify(audioFocus).abandonAudioFocus();

        // + 4th invocation
        ArgumentCaptor<Integer> onPositionChangedArguments2 = ArgumentCaptor.forClass(Integer.class);
        verify(playerListener, times(4)).onPositionChanged(onPositionChangedArguments2.capture());
        args = onPositionChangedArguments2.getAllValues();
        assertEquals(args.size(), args.stream().distinct().count());

        CountDownLatch countDownLatch4 = new CountDownLatch(1);
        runAsync(() -> {
            player.release();
            countDownLatch4.countDown();
        });

        countDownLatch4.await();
    }

    @Test
    public void stop() throws Exception {

    }

    @Test
    public void togglePlayStop() throws Exception {

    }

    @Test
    public void getPlayingUrl() throws Exception {

    }
}