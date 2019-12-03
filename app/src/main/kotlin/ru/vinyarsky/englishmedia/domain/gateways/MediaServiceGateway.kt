package ru.vinyarsky.englishmedia.domain.gateways

interface MediaServiceGateway {

    fun startServiceAsForeground()

    fun stopService()
}