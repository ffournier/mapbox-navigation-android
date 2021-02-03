package com.mapbox.navigation.ui.voice

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.ui.base.api.voice.SpeechApi
import com.mapbox.navigation.ui.base.api.voice.SpeechCallback

/**
 * Online implementation of [SpeechApi].
 * Will retrieve synthesized speech mp3s from Mapbox's API Voice.
 */
class MapboxOffboardSpeechPlayer : SpeechApi {

    /**
     * Given [VoiceInstructions] the method will play the given voice instruction (SSML).
     * If a voice instruction is already playing or other announcement are already queued,
     * the given voice instruction will be queued to play after.
     * @param voiceInstruction VoiceInstructions object representing [VoiceInstructions]
     * @param callback SpeechCallback
     */
    override fun play(voiceInstruction: VoiceInstructions, callback: SpeechCallback) {
        TODO("Not yet implemented")
    }

    /**
     * If called while an announcement is currently playing,
     * the announcement should end immediately and any announcements queued should be cleared.
     * @param callback SpeechCallback
     */
    override fun stop(callback: SpeechCallback) {
        TODO("Not yet implemented")
    }

    /**
     * Releases the resources used by the speech player.
     * @param callback SpeechCallback
     */
    override fun shutdown(callback: SpeechCallback) {
        TODO("Not yet implemented")
    }
}
