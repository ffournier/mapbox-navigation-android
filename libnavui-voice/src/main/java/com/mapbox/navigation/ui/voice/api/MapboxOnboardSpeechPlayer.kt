package com.mapbox.navigation.ui.voice.api

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.mapbox.navigation.ui.base.api.voice.SpeechPlayer
import com.mapbox.navigation.ui.base.api.voice.SpeechPlayerCallback
import com.mapbox.navigation.ui.base.model.voice.Announcement
import com.mapbox.navigation.ui.base.model.voice.SpeechState
import java.util.Locale

/**
 * Offline implementation of [SpeechPlayer].
 * @property context Context
 * @property language String
 */
internal class MapboxOnboardSpeechPlayer(
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
    private var volumeLevel: Float = DEFAULT_VOLUME_LEVEL
    private lateinit var clientCallback: SpeechPlayerCallback
    private var currentPlay: SpeechState.Play = SpeechState.Play(Announcement("", null, null))

    /**
     * Given [SpeechState.Play] [Announcement] the method will play the voice instruction.
     * If a voice instruction is already playing or other announcement are already queued,
     * the given voice instruction will be queued to play after.
     * @param state SpeechState Play Announcement object including the announcement text
     * and optionally a synthesized speech mp3.
     * @param callback
     */
    override fun play(state: SpeechState.Play, callback: SpeechPlayerCallback) {
        clientCallback = callback
        currentPlay = state
        if (isLanguageSupported) {
            play(state.announcement.announcement)
        }
    }

    /**
     * The method will set the volume to the specified level from [SpeechState.Volume].
     * Note that this API is not dynamic and only takes effect on the next play announcement.
     * If the volume is set to 0.0f, current play announcement (if any) is stopped though.
     * @param state SpeechState Volume level.
     */
    override fun volume(state: SpeechState.Volume) {
        volumeLevel = state.level
        if (textToSpeech.isSpeaking && state.level == MUTE_VOLUME_LEVEL) {
            textToSpeech.stop()
        }
    }

    /**
     * Clears any announcements queued.
     */
    override fun clear() {
        textToSpeech.stop()
    }

    /**
     * Releases the resources used by the speech player.
     * If called while an announcement is currently playing,
     * the announcement should end immediately and any announcements queued should be cleared.
     */
    override fun shutdown() {
        textToSpeech.setOnUtteranceProgressListener(null)
        textToSpeech.shutdown()
        volumeLevel = DEFAULT_VOLUME_LEVEL
    }

    private fun initializeWithLanguage(language: Locale) {
        val isLanguageAvailable =
            textToSpeech.isLanguageAvailable(language) == TextToSpeech.LANG_AVAILABLE
        if (!isLanguageAvailable) {
            return
        }
        isLanguageSupported = true
        textToSpeech.language = language
        textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onDone(utteranceId: String?) {
                donePlaying()
            }

            override fun onError(utteranceId: String?) {
                donePlaying()
            }

            override fun onStart(utteranceId: String?) {
                // Intentionally empty
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                donePlaying()
            }
        })
    }

    private fun donePlaying() {
        clientCallback.onDone(SpeechState.Done(currentPlay.announcement))
    }

    private fun play(announcement: String) {
        val bundle = Bundle()
        bundle.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volumeLevel)
        textToSpeech.speak(
            announcement,
            TextToSpeech.QUEUE_FLUSH,
            bundle,
            DEFAULT_UTTERANCE_ID
        )
    }

    private companion object {
        private const val DEFAULT_UTTERANCE_ID = "default_id"
        private const val DEFAULT_VOLUME_LEVEL = 0.5f
        private const val MUTE_VOLUME_LEVEL = 0.0f
    }
}
