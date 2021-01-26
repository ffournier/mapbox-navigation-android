package com.mapbox.navigation.ui.base.model.voice

import com.mapbox.navigation.ui.base.MapboxState
import java.io.File

/**
 * Immutable object representing the voice data to be played.
 */
sealed class VoiceState : MapboxState {
    /**
     * State representing data about the instruction file.
     * @property instructionFile [File]
     */
    data class VoiceFile(var instructionFile: File) : VoiceState()

    /**
     * Immutable object defining side effects for voice because of various
     * other events happening.
     */
    sealed class VoiceSideEffects : VoiceState() {

        /**
         * State determining if voice announcements will be played or not.
         * @property isMuted Boolean true if should be muted, false if should not
         */
        data class UpdateMute(val isMuted: Boolean) : VoiceSideEffects()
    }

    /**
     * The state is returned in case of any errors while preparing the voice instruction
     */
    sealed class VoiceFailure : VoiceState() {
        /**
         * The state is returned if there is an error preparing the [File]
         * @property exception String error message
         */
        data class VoiceError(val exception: String?) : VoiceFailure()
    }
}
