package com.mapbox.navigation.ui.voice.api

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.ui.base.api.voice.VoiceApi
import com.mapbox.navigation.ui.base.api.voice.VoiceCallback

/**
 * Implementation of [VoiceApi] allowing you to retrieve voice instructions.
 * @property accessToken String
 * @property language String
 */
class MapboxVoiceApi(
    private val accessToken: String,
    private val language: String
) : VoiceApi {

    /**
     * Given [VoiceInstructions] the method returns a [File] wrapped inside [VoiceCallback]
     * @param voiceInstruction VoiceInstructions object representing [VoiceInstructions]
     * @param callback VoiceCallback contains [VoiceState.VoiceFile]
     */
    override fun retrieveVoiceFile(voiceInstruction: VoiceInstructions, callback: VoiceCallback) {
        TODO("Not yet implemented")
    }

    /**
     * The method stops the process of retrieving the File and destroys any related callbacks.
     */
    override fun cancel() {
        TODO("Not yet implemented")
    }
}
