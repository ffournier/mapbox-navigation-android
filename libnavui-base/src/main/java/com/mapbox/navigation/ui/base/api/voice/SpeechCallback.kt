package com.mapbox.navigation.ui.base.api.voice

import com.mapbox.navigation.ui.base.model.voice.SpeechState

/**
 * Interface definition for a callback to be invoked when a voice instruction is played.
 */
interface SpeechCallback {

    /**
     * Invoked when the voice instruction state changes.
     * @param state current SpeechState result of SpeechApi interactions.
     */
    fun onStateChanged(state: SpeechState)

    /**
     * Invoked when there is an error playing the voice instruction.
     * @param error error message.
     */
    fun onFailure(error: SpeechState.SpeechFailure)
}
