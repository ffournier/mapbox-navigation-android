package com.mapbox.navigation.ui.base.api.voice

import com.mapbox.api.directions.v5.models.VoiceInstructions

/**
 * An Api that allows you to play voice instruction based on [VoiceInstructions]
 */
interface SpeechApi {

    /**
     * Given [VoiceInstructions] the method will play the given voice instruction.
     * If a voice instruction is already playing or other announcement are already queued,
     * the given voice instruction will be queued to play after.
     * @param voiceInstruction VoiceInstructions object representing [VoiceInstructions]
     * @param callback SpeechCallback
     */
    fun play(
        voiceInstruction: VoiceInstructions,
        callback: SpeechCallback
    )

    /**
     * If called while an announcement is currently playing,
     * the announcement should end immediately and any announcements queued should be cleared.
     * @param callback SpeechCallback
     */
    fun stop(callback: SpeechCallback)

    /**
     * Releases the resources used by the speech player.
     * @param callback SpeechCallback
     */
    fun shutdown(callback: SpeechCallback)
}
