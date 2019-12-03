package ru.vinyarsky.englishmedia.core

interface Converter<S, T> {

    fun convert(source: S): T?

    fun reverse(source: T): S?
}