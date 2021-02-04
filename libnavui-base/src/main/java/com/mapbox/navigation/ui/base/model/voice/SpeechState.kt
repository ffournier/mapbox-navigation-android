package com.mapbox.navigation.ui.base.model.voice

import com.mapbox.navigation.ui.base.MapboxState

/**
 * Immutable object representing the speech player data.
 */
sealed class SpeechState : MapboxState {

    /**
     * The structure represents different state for a Speech.
     */
    sealed class Speech : SpeechState() {

        /**
         * The state is returned when the speech is ready to be played on the UI.
         * @property announcement
         */
        data class Available(val announcement: Announcement) : Speech()

        /**
         * The state is returned if there is an error playing the voice instruction
         * @property exception String error message
         */
        data class Error(val exception: String?) : Speech()
    }

    /**
     * The state is returned if the voice instruction is playing.
     */
    data class Play(val announcement: Announcement) : SpeechState()

    /**
     * The state is returned if the voice instruction is stopping.
     */
    object Stop : SpeechState()

    /**
     * The state is returned if the voice instruction is shutting down.
     */
    object Shutdown : SpeechState()
}
