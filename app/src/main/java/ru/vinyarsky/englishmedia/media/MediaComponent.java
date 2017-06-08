package ru.vinyarsky.englishmedia.media;

import dagger.Subcomponent;

@Subcomponent(modules = {MediaModule.class})
@MediaScope
public interface MediaComponent {

    Player getPlayer();
}
