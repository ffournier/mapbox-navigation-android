package com.mapbox.navigation.ui.voice.api

import com.mapbox.navigation.ui.base.api.voice.SpeechPlayer
import com.mapbox.navigation.ui.base.model.voice.Announcement
import com.mapbox.navigation.ui.base.model.voice.SpeechState

/**
 * Hybrid implementation of [SpeechPlayer] combining [MapboxOnboardSpeechPlayer] and
 * [MapboxOffboardSpeechPlayer] speech players.
 */
class MapboxHybridSpeechPlayer : SpeechPlayer {

    /**
     * Given [SpeechState.Play] [Announcement] the method will play the voice instruction.
     * If a voice instruction is already playing or other announcement are already queued,
     * the given voice instruction will be queued to play after.
     * @param state SpeechState Play Announcement object including the announcement text
     * and optionally a synthesized speech mp3.
     */
    override fun play(state: SpeechState.Play) {
        TODO("Not yet implemented")
    }

    /**
     * The method will set the volume to the specified level from [SpeechState.Volume].
     * @param state SpeechState Volume level.
     */
    override fun volume(state: SpeechState.Volume) {
        TODO("Not yet implemented")
    }

    /**
     * Releases the resources used by the speech player.
     * If called while an announcement is currently playing,
     * the announcement should end immediately and any announcements queued should be cleared.
     */
    override fun shutdown() {
        TODO("Not yet implemented")
    }
}
