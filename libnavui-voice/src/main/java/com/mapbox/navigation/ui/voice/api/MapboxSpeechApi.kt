package com.mapbox.navigation.ui.voice.api

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.ui.base.api.voice.SpeechApi
import com.mapbox.navigation.ui.base.api.voice.SpeechCallback

/**
 * Implementation of [SpeechApi] allowing you to play voice instructions.
 * @property speechPlayer SpeechApi
 */
class MapboxSpeechApi(
    private val speechPlayer: SpeechApi
) : SpeechApi {

    /**
     * Given [VoiceInstructions] the method will play the given voice instruction.
     * If a voice instruction is already playing or other announcement are already queued,
     * the given voice instruction will be queued to play after.
     * @param voiceInstruction VoiceInstructions object representing [VoiceInstructions]
     * @param callback SpeechCallback
     */
    override fun play(voiceInstruction: VoiceInstructions, callback: SpeechCallback) {
        speechPlayer.play(voiceInstruction, callback)
    }

    /**
     * If called while an announcement is currently playing,
     * the announcement should end immediately and any announcements queued should be cleared.
     * @param callback SpeechCallback
     */
    override fun stop(callback: SpeechCallback) {
        speechPlayer.stop(callback)
    }
}
