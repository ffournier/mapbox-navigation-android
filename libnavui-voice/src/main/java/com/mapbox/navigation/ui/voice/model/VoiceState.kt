package com.mapbox.navigation.ui.voice.model

import com.mapbox.navigation.ui.base.MapboxState
import java.io.File

/**
 * Immutable object representing the voice data to be played.
 */
internal sealed class VoiceState : MapboxState {
    /**
     * State representing data about the instruction file.
     * @property instructionFile [File]
     */
    data class VoiceFile(var instructionFile: File) : VoiceState()

    /**
     * The state is returned if there is an error preparing the [File]
     * @property exception String error message
     */
    data class VoiceError(val exception: String) : VoiceState()
}
