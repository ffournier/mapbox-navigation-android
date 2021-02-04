package com.mapbox.navigation.ui.voice.api

import android.content.Context
import android.media.MediaPlayer
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.ui.base.api.voice.SpeechApi
import com.mapbox.navigation.ui.base.api.voice.SpeechCallback
import com.mapbox.navigation.ui.base.api.voice.SpeechPlayer
import com.mapbox.navigation.ui.base.model.voice.Announcement
import com.mapbox.navigation.ui.base.model.voice.SpeechState
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
class MapboxOffboardSpeechPlayerApi(
    private val context: Context,
    private val accessToken: String,
    private val language: String
) : SpeechApi, SpeechPlayer {

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
    override fun generate(voiceInstruction: VoiceInstructions, callback: SpeechCallback) {
        voiceAPI.retrieveVoiceFile(
            voiceInstruction,
            object : VoiceCallback {
                override fun onVoiceFileReady(instructionFile: VoiceState.VoiceFile) {
                    val announcement = voiceInstruction.ssmlAnnouncement()
                    val isValidAnnouncement = !announcement.isNullOrBlank()

                    if (isValidAnnouncement) {
                        callback.onAvailable(
                            SpeechState.Speech.Available(
                                Announcement(announcement!!, instructionFile.instructionFile)
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

                override fun onError(error: VoiceState.VoiceError) {
                    TODO("Not yet implemented")
                }
            }
        )
    }

    /**
     * Given [Announcement] the method will play the voice instruction.
     * If a voice instruction is already playing or other announcement are already queued,
     * the given voice instruction will be queued to play after.
     * @param announcement Announcement object including the announcement text and optionally
     * a synthesized speech mp3.
     */
    override fun play(announcement: Announcement) {
        announcement.file?.let {
            setupMediaPlayer(it)
        }
    }

    /**
     * If called while an announcement is currently playing,
     * the announcement should end immediately and any announcements queued should be cleared.
     */
    override fun stop() {
        stopMediaPlayer()
    }

    /**
     * Releases the resources used by the speech player.
     */
    override fun shutdown() {
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
