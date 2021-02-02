package com.mapbox.navigation.ui.base.api.voice

import com.mapbox.navigation.ui.base.model.voice.VoiceState

// TODO: Move into libnavui-voice internal
/**
 * Interface definition for a callback to be invoked when a [File] is processed.
 */
interface VoiceCallback {

    /**
     * Invoked when [File] is ready.
     * @param instructionFile VoiceFile represents instruction file to be played.
     */
    fun onVoice(instructionFile: VoiceState.VoiceFile)

    /**
     * Invoked when there is an error retrieving the voice instruction file.
     * @param error error message.
     */
    fun onFailure(error: VoiceState.VoiceFailure)
}
