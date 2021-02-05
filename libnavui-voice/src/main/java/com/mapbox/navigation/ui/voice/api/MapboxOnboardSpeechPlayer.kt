package com.mapbox.navigation.ui.voice.api

import android.content.Context
import android.speech.tts.TextToSpeech
import com.mapbox.navigation.ui.base.api.voice.SpeechPlayer
import com.mapbox.navigation.ui.base.model.voice.Announcement
import com.mapbox.navigation.ui.base.model.voice.SpeechState
import java.util.*

/**
 * Offline implementation of [SpeechPlayer].
 * @property context Context
 * @property language String
 */
class MapboxOnboardSpeechPlayer(
    private val context: Context,
    private val language: String
) : SpeechPlayer {

    private var isLanguageSupported: Boolean = false
    private val textToSpeech: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        val ableToInitialize = status == TextToSpeech.SUCCESS
        if (!ableToInitialize) {
            return@TextToSpeech
        }
        initializeWithLanguage(Locale(language))
    }

    /**
     * Given [SpeechState.Play] [Announcement] the method will play the voice instruction.
     * If a voice instruction is already playing or other announcement are already queued,
     * the given voice instruction will be queued to play after.
     * @param state SpeechState Play Announcement object including the announcement text
     * and optionally a synthesized speech mp3.
     */
    override fun play(state: SpeechState.Play) {
        if (isLanguageSupported) {
            textToSpeech.speak(
                state.announcement.announcement,
                TextToSpeech.QUEUE_ADD,
                null,
                DEFAULT_UTTERANCE_ID
            )
        }
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
        textToSpeech.shutdown()
    }

    private fun initializeWithLanguage(language: Locale) {
        val isLanguageAvailable =
            textToSpeech.isLanguageAvailable(language) == TextToSpeech.LANG_AVAILABLE
        if (!isLanguageAvailable) {
            return
        }
        isLanguageSupported = true
        textToSpeech.language = language
    }

    private companion object {
        private const val DEFAULT_UTTERANCE_ID = "default_id"
    }
}
