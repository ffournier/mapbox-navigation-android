package com.mapbox.navigation.ui.base.model.voice

import com.mapbox.navigation.ui.base.MapboxState
import java.io.File

// TODO: Move into libnavui-voice internal
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
