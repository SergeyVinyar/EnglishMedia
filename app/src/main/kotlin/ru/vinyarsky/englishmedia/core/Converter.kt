package ru.vinyarsky.englishmedia.core

/**
 * Generic converter interface from S to T (and vice versa)
 */
interface Converter<S, T> {

    /**
     * Converts S to T
     *
     * @param source Object to be converted
     * @return Converted object (null if cannot be converted)
     */
    fun convert(source: S): T?

    /**
     * Converts T to S
     *
     * @param source Object to be converted
     * @return Converted object (null if cannot be converted)
     */
    fun reverse(source: T): S?
}