package ru.vinyarsky.englishmedia.core

abstract class ClosableSequence<T>(sequence: Sequence<T>) : Sequence<T> by sequence, AutoCloseable

fun <T> Sequence<T>.asClosable(closeMethod: () -> Unit): ClosableSequence<T> {
    return object : ClosableSequence<T>(this) {
        override fun close() {
            closeMethod()
        }
    }
}