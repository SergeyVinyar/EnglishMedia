package ru.vinyarsky.englishmedia;

import android.app.Application;

import ru.vinyarsky.englishmedia.media.MediaComponent;
import ru.vinyarsky.englishmedia.media.MediaModule;

public final class EMApplication extends Application {

    private static EMComponent emComponent;
    private static MediaComponent mediaComponent;

    public static EMComponent getEmComponent() {
        assert emComponent != null;
        return emComponent;
    }

    public static MediaComponent getMediaComponent() {
        if (mediaComponent == null)
            mediaComponent = emComponent.createMediaComponent(new MediaModule());
        return mediaComponent;
    }

    @Override
    public void onCreate() {
        super.onCreate();

//        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
//                .detectLeakedSqlLiteObjects()
//                .detectLeakedClosableObjects()
//                .detectLeakedRegistrationObjects()
//                .penaltyLog()
//                //.penaltyDeath()
//                .build()
//        );

        emComponent = DaggerEMComponent.builder()
                .eMModule(new EMModule(this.getApplicationContext()))
                .build();
    }
}
