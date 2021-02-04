package com.mapbox.navigation.ui.base.api.voice

import com.mapbox.navigation.ui.base.model.voice.SpeechState

/**
 * Interface definition for a callback to be invoked when a voice instruction is played.
 */
interface SpeechCallback {

    /**
     * Invoked when the voice instruction Playing state occurs.
     * @param state Playing SpeechState.
     */
    fun onPlaying(state: SpeechState.Speech.Playing)

    /**
     * Invoked when the voice instruction Stopped state occurs.
     * @param state Stopped SpeechState.
     */
    fun onStopped(state: SpeechState.Speech.Stopped)

    /**
     * Invoked when there is an error playing the voice instruction.
     * @param error error message.
     */
    fun onError(error: SpeechState.Speech.Error)
}
