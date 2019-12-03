package ru.vinyarsky.englishmedia.domain.gateways

import android.net.Uri

interface PlayerGateway {

    val playingUrl: Uri?

    fun release()

    fun play(url: Uri, startFromPositionSec: Int? = null)

    fun stop()

    fun togglePlayStop()

    fun addListener(listener: Listener)

    fun removeListener(listener: Listener)

    interface Listener {
        fun onNoNetwork()
        fun onContentNotFound()

        fun onResetDueError()

        fun onPlay()
        fun onPositionChanged(positionSec: Int)
        fun onStop(positionSec: Int)
        fun onCompleted()
    }
}

