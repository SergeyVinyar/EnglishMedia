package ru.vinyarsky.englishmedia.media;

import android.content.Context;
import android.media.AudioManager;

/* package */ class AudioFocus {

    private boolean hasAudioFocus = false;
    private AudioManager audioManager;
    private AudioFocusListener listener;

    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = (focusChange) -> {
        // No duck support because we primarily play speech (not music),
        // i.e. we always stop playing
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                this.listener.onResume();
                break;
            default:
                this.listener.onPause();
                break;
        }
    };

    /* package */ AudioFocus(Context context, AudioFocusListener listener) {
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.listener = listener;
    }

    /**
     * @return true - request granted
     */
    /* package */ boolean ensureAudioFocus() {
        if (this.hasAudioFocus)
            return true;
        this.hasAudioFocus = audioManager.requestAudioFocus(this.audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        return this.hasAudioFocus;
    }

    /* package */ void abandonAudioFocus() {
        if (!this.hasAudioFocus)
            return;
        audioManager.abandonAudioFocus(this.audioFocusChangeListener);
        this.hasAudioFocus = false;
    }

    /* package */ interface AudioFocusListener {
        void onPause();
        void onResume();
    }
}
