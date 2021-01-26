package com.mapbox.navigation.ui.base.api.voice

import com.mapbox.api.directions.v5.models.VoiceInstructions

/**
 * An Api that allows you to retrieve voice instruction files based on [VoiceInstructions]
 */
interface VoiceApi {

    /**
     * Given [VoiceInstructions] the method returns a [File] wrapped inside [VoiceCallback]
     * @param voiceInstruction VoiceInstructions object representing [VoiceInstructions]
     * @param callback VoiceCallback contains [VoiceState.VoiceFile]
     */
    fun retrieveVoiceFile(
        voiceInstruction: VoiceInstructions,
        callback: VoiceCallback
    )

    /**
     * The method stops the process of retrieving the File and destroys any related callbacks.
     */
    fun cancel()
}
