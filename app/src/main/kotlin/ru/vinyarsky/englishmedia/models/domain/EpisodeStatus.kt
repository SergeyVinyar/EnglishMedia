package ru.vinyarsky.englishmedia.models.domain

/**
 * Episode listening status
 */
enum class EpisodeStatus {

    /**
     * New episode, listening hasn't been started yet
     */
    NEW,

    /**
     * Listening has been started but not completed
     */
    LISTENING,

    /**
     * The whole episode has been listened
     */
    COMPLETED;
}