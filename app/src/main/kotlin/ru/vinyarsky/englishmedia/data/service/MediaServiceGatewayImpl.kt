package ru.vinyarsky.englishmedia.data.service

import android.content.Context
import ru.vinyarsky.englishmedia.domain.gateways.MediaServiceGateway

class MediaServiceGatewayImpl(context: Context) : MediaServiceGateway {

    private val applicationContext = context.applicationContext

    override fun startServiceAsForeground() {
        applicationContext.startService(MediaService.getIntent(applicationContext))
    }

    override fun stopService() {
        applicationContext.stopService(MediaService.getIntent(applicationContext))
    }
}