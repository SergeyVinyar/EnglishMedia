package ru.vinyarsky.englishmedia.media;

/* package */ interface AudioFocus {

    boolean ensureAudioFocus();

    void abandonAudioFocus();

    void addListener(AudioFocusListener listener);

    void removeListener(AudioFocusListener listener);

    interface AudioFocusListener {
        void onPause();
        void onResume();
    }
}
