package com.mapbox.navigation.ui.voice

import android.content.Context
import android.media.MediaPlayer
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.ui.base.api.voice.SpeechApi
import com.mapbox.navigation.ui.base.api.voice.SpeechCallback
import com.mapbox.navigation.ui.voice.api.MapboxVoiceApi
import com.mapbox.navigation.ui.voice.api.VoiceCallback
import com.mapbox.navigation.ui.voice.model.VoiceState
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Online implementation of [SpeechApi].
 * Will retrieve synthesized speech mp3s from Mapbox's API Voice.
 * @property context Context
 * @property accessToken String
 * @property language String
 */
class MapboxOffboardSpeechPlayer(
    private val context: Context,
    private val accessToken: String,
    private val language: String
) : SpeechApi {

    private var mediaPlayer = MediaPlayer()
    private val voiceAPI = MapboxVoiceApi(context, accessToken, language)
    private var isPlaying = false

    /**
     * Given [VoiceInstructions] the method will play the given voice instruction (SSML).
     * If a voice instruction is already playing or other announcement are already queued,
     * the given voice instruction will be queued to play after.
     * @param voiceInstruction VoiceInstructions object representing [VoiceInstructions]
     * @param callback SpeechCallback
     */
    override fun play(voiceInstruction: VoiceInstructions, callback: SpeechCallback) {
        voiceAPI.retrieveVoiceFile(
            voiceInstruction,
            object : VoiceCallback {
                override fun onVoiceFileReady(instructionFile: VoiceState.VoiceFile) {
                    setupMediaPlayer(instructionFile.instructionFile)
                }

                override fun onError(error: VoiceState.VoiceError) {
                    TODO("Not yet implemented")
                }
            }
        )
    }

    /**
     * If called while an announcement is currently playing,
     * the announcement should end immediately and any announcements queued should be cleared.
     * @param callback SpeechCallback
     */
    override fun stop(callback: SpeechCallback) {
        stopMediaPlayer()
    }

    /**
     * Releases the resources used by the speech player.
     * @param callback SpeechCallback
     */
    override fun shutdown(callback: SpeechCallback) {
        stopMediaPlayer()
    }

    private fun setupMediaPlayer(instruction: File) {
        if (!instruction.canRead()) {
            return
        }
        try {
            FileInputStream(instruction).use { fis ->
                mediaPlayer = MediaPlayer()
                mediaPlayer.setDataSource(fis.fd)
                mediaPlayer.prepareAsync()
                addListeners()
            }
        } catch (ex: FileNotFoundException) {
        } catch (ex: IOException) {
        }
    }

    private fun addListeners() {
        mediaPlayer.setOnErrorListener { _, _, _ ->
            false
        }
        mediaPlayer.setOnPreparedListener { mp ->
            isPlaying = true
            mp.start()
        }
        mediaPlayer.setOnCompletionListener { mp ->
            mp.release()
            isPlaying = false
        }
    }

    private fun stopMediaPlayer() {
        if (isPlaying) {
            mediaPlayer.stop()
            mediaPlayer.release()
        }
    }
}
