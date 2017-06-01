package ru.vinyarsky.englishmedia.media;

import javax.inject.Singleton;

import dagger.Subcomponent;

@Subcomponent(modules = {MediaModule.class})
@MediaScope
public interface MediaComponent {

    Player getPlayer();
}
