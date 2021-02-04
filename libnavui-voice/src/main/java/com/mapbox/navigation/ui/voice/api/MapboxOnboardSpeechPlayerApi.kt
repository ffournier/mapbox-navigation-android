package com.mapbox.navigation.ui.voice.api

import android.content.Context
import android.speech.tts.TextToSpeech
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.ui.base.api.voice.SpeechApi
import com.mapbox.navigation.ui.base.api.voice.SpeechCallback
import com.mapbox.navigation.ui.base.api.voice.SpeechPlayer
import com.mapbox.navigation.ui.base.model.voice.Announcement
import com.mapbox.navigation.ui.base.model.voice.SpeechState
import java.util.Locale

/**
 * Offline implementation of [SpeechApi].
 * @property context Context
 * @property language String
 */
class MapboxOnboardSpeechPlayerApi(
    private val context: Context,
    private val language: String
) : SpeechApi, SpeechPlayer {

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
    override fun generate(voiceInstruction: VoiceInstructions, callback: SpeechCallback) {
        val announcement = voiceInstruction.announcement()
        val isValidAnnouncement = !announcement.isNullOrBlank()
        val canPlay = isValidAnnouncement && isLanguageSupported

        if (canPlay) {
            callback.onAvailable(
                SpeechState.Speech.Available(
                    Announcement(announcement!!, null)
                )
            )
        } else {
            callback.onError(
                SpeechState.Speech.Error(
                    "VoiceInstructions#ssmlAnnouncement can't be null"
                )
            )
        }
    }

    /**
     * Given [Announcement] the method will play the voice instruction.
     * If a voice instruction is already playing or other announcement are already queued,
     * the given voice instruction will be queued to play after.
     * @param announcement Announcement object including the announcement text and optionally
     * a synthesized speech mp3.
     */
    override fun play(announcement: Announcement) {
        textToSpeech.speak(
            announcement.announcement,
            TextToSpeech.QUEUE_ADD,
            null,
            DEFAULT_UTTERANCE_ID
        )
    }

    /**
     * If called while an announcement is currently playing,
     * the announcement should end immediately and any announcements queued should be cleared.
     */
    override fun stop() {
        textToSpeech.stop()
    }

    /**
     * Releases the resources used by the speech player.
     */
    override fun shutdown() {
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
