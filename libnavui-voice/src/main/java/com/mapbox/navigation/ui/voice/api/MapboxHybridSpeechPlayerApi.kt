package com.mapbox.navigation.ui.voice.api

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.ui.base.api.voice.SpeechApi
import com.mapbox.navigation.ui.base.api.voice.SpeechCallback
import com.mapbox.navigation.ui.base.api.voice.SpeechPlayer
import com.mapbox.navigation.ui.base.model.voice.Announcement

/**
 * Hybrid implementation of [SpeechApi] combining [MapboxOnboardSpeechPlayerApi] and
 * [MapboxOffboardSpeechPlayerApi] speech players.
 */
class MapboxHybridSpeechPlayerApi : SpeechApi, SpeechPlayer {

    /**
     * Given [VoiceInstructions] the method will play the given voice instruction.
     * If a voice instruction is already playing or other announcement are already queued,
     * the given voice instruction will be queued to play after.
     * @param voiceInstruction VoiceInstructions object representing [VoiceInstructions]
     * @param callback SpeechCallback
     */
    override fun generate(voiceInstruction: VoiceInstructions, callback: SpeechCallback) {
        TODO("Not yet implemented")
    }

    /**
     * Given [Announcement] the method will play the voice instruction.
     * If a voice instruction is already playing or other announcement are already queued,
     * the given voice instruction will be queued to play after.
     * @param announcement Announcement object including the announcement text and optionally
     * a synthesized speech mp3.
     */
    override fun play(announcement: Announcement) {
        TODO("Not yet implemented")
    }

    /**
     * If called while an announcement is currently playing,
     * the announcement should end immediately and any announcements queued should be cleared.
     */
    override fun stop() {
        TODO("Not yet implemented")
    }

    /**
     * Releases the resources used by the speech player.
     */
    override fun shutdown() {
        TODO("Not yet implemented")
    }
}
