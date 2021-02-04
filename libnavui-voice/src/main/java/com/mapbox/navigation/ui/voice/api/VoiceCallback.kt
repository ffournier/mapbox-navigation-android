package com.mapbox.navigation.ui.voice.api

import com.mapbox.navigation.ui.voice.model.VoiceState

/**
 * Interface definition for a callback to be invoked when a [File] is processed.
 */
internal interface VoiceCallback {

    /**
     * Invoked when [File] is ready.
     * @param instructionFile VoiceFile represents instruction file to be played.
     */
    fun onVoiceFileReady(instructionFile: VoiceState.VoiceFile)

    /**
     * Invoked when there is an error retrieving the voice instruction file.
     * @param error error message.
     */
    fun onError(error: VoiceState.VoiceError)
}
