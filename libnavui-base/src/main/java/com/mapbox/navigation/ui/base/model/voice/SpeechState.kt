package com.mapbox.navigation.ui.base.model.voice

import com.mapbox.navigation.ui.base.MapboxState

/**
 * Immutable object representing the speech player data.
 */
sealed class SpeechState : MapboxState {
    /**
     * The state is returned if the voice instruction is playing.
     */
    object IsPlaying : SpeechState()

    /**
     * The state is returned if the voice instruction is stopped.
     */
    object IsStopped : SpeechState()

    /**
     * Immutable object defining side effects for voice announcements because of various
     * other events happening.
     */
    sealed class SpeechSideEffects : SpeechState() {

        /**
         * State determining if voice announcements will be played or not.
         * @property isMuted Boolean true if should be muted, false if should not
         */
        data class UpdateMute(val isMuted: Boolean) : SpeechSideEffects()
    }

    /**
     * The state is returned in case of any errors while playing the voice instruction
     */
    sealed class SpeechFailure : SpeechState() {
        /**
         * The state is returned if there is an error playing the voice instruction
         * @property exception String error message
         */
        data class SpeechError(val exception: String?) : SpeechFailure()
    }
}
