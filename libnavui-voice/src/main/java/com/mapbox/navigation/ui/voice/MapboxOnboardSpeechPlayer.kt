package com.mapbox.navigation.ui.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.ui.base.api.voice.SpeechApi
import com.mapbox.navigation.ui.base.api.voice.SpeechCallback
import java.util.Locale

/**
 * Offline implementation of [SpeechApi].
 * @property context Context
 * @property language String
 */
class MapboxOnboardSpeechPlayer(
    private val context: Context,
    private val language: String
) : SpeechApi {

    private var isLanguageSupported: Boolean = false
    private val textToSpeech: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        val ableToInitialize = status == TextToSpeech.SUCCESS
        if (!ableToInitialize) {
            return@TextToSpeech
        }
        initializeWithLanguage(Locale(language))
    }

    /**
     * Given [VoiceInstructions] the method will play the given voice instruction.
     * If a voice instruction is already playing or other announcement are already queued,
     * the given voice instruction will be queued to play after.
     * @param voiceInstruction VoiceInstructions object representing [VoiceInstructions]
     * @param callback SpeechCallback
     */
    override fun play(voiceInstruction: VoiceInstructions, callback: SpeechCallback) {
        val announcement = voiceInstruction.announcement()
        val isValidAnnouncement = !announcement.isNullOrBlank()
        val canPlay = isValidAnnouncement && isLanguageSupported
        if (!canPlay) {
            return
        }

        textToSpeech.speak(announcement, TextToSpeech.QUEUE_ADD, null, DEFAULT_UTTERANCE_ID)
    }

    /**
     * If called while an announcement is currently playing,
     * the announcement should end immediately and any announcements queued should be cleared.
     * @param callback SpeechCallback
     */
    override fun stop(callback: SpeechCallback) {
        textToSpeech.stop()
    }

    /**
     * Stops and shuts down the speech player.
     * @param callback SpeechCallback
     */
    override fun shutdown(callback: SpeechCallback) {
        textToSpeech.stop()
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
