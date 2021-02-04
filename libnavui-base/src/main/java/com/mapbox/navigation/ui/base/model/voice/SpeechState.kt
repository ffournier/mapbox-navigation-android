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
         * The state is returned if the voice instruction is playing.
         */
        object Playing : Speech()

        /**
         * The state is returned if the voice instruction is stopped.
         */
        object Stopped : Speech()

        /**
         * The state is returned if there is an error playing the voice instruction
         * @property exception String error message
         */
        data class Error(val exception: String?) : Speech()
    }

    /**
     * Immutable object defining side effects for voice announcements because of various
     * other events happening.
     */
    sealed class Volume : SpeechState() {

        /**
         *
         */
        object Mute : Volume()

        /**
         *
         */
        object Unmute : Volume()
    }
}
