package ru.vinyarsky.englishmedia;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Component;

import ru.vinyarsky.englishmedia.media.MediaComponent;
import ru.vinyarsky.englishmedia.media.MediaModule;

@Component(modules = {EMModule.class})
@Singleton
public interface EMComponent {

    Context getContext();

    MediaComponent createMediaComponent(MediaModule mediaModule);
}