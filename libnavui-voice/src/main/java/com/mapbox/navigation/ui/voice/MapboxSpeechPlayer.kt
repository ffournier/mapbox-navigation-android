package com.mapbox.navigation.ui.voice

import com.mapbox.navigation.ui.base.MapboxStateMachine
import com.mapbox.navigation.ui.base.api.voice.SpeechApi
import com.mapbox.navigation.ui.base.api.voice.SpeechPlayer
import com.mapbox.navigation.ui.base.model.voice.SpeechState

/**
 * Default speech player that applies [SpeechState] based on the [SpeechApi] given.
 */
class MapboxSpeechPlayer(
    private val speechPlayer: SpeechPlayer
) : MapboxStateMachine<SpeechState> {

    /**
     * Entry point for the [MapboxSpeechPlayer] to play voice instructions based
     * on a [SpeechState] and the [SpeechApi] given.
     */
    override fun apply(state: SpeechState) {
        when (state) {
            is SpeechState.Speech.Available -> {
            }
            is SpeechState.Play -> {
                play(state)
            }
            is SpeechState.Stop -> {
                speechPlayer.stop()
            }
            is SpeechState.Shutdown -> {
                speechPlayer.shutdown()
            }
            is Error -> {
            }
            else -> {
            }
        }
    }

    private fun play(state: SpeechState.Play) {
        speechPlayer.play(state.announcement)
    }
}
