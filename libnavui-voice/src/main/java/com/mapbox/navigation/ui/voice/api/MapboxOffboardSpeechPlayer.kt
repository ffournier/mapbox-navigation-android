package com.mapbox.navigation.ui.voice.api

import android.content.Context
import android.media.MediaPlayer
import com.mapbox.navigation.ui.base.api.voice.SpeechPlayer
import com.mapbox.navigation.ui.base.model.voice.Announcement
import com.mapbox.navigation.ui.base.model.voice.SpeechState
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Online implementation of [SpeechPlayer].
 * Will retrieve synthesized speech mp3s from Mapbox's API Voice.
 * @property context Context
 * @property accessToken String
 * @property language String
 */
class MapboxOffboardSpeechPlayer(
    private val context: Context,
    private val accessToken: String,
    private val language: String
) : SpeechPlayer {

    private var mediaPlayer = MediaPlayer()
    private var volumeLevel: Float = 0.5f

    /**
     * Given [SpeechState.Play] [Announcement] the method will play the voice instruction.
     * If a voice instruction is already playing or other announcement are already queued,
     * the given voice instruction will be queued to play after.
     * @param state SpeechState Play Announcement object including the announcement text
     * and optionally a synthesized speech mp3.
     */
    override fun play(state: SpeechState.Play) {
        state.announcement.file?.let {
            setupMediaPlayer(it)
        }
    }

    /**
     * The method will set the volume to the specified level from [SpeechState.Volume].
     * @param state SpeechState Volume level.
     */
    override fun volume(state: SpeechState.Volume) {
        volumeLevel = state.level
        // FIXME: This may cause IllegalStateException
        setVolume(volumeLevel)
    }

    /**
     * Releases the resources used by the speech player.
     * If called while an announcement is currently playing,
     * the announcement should end immediately and any announcements queued should be cleared.
     */
    override fun shutdown() {
        mediaPlayer.release()
    }

    private fun setupMediaPlayer(instruction: File) {
        if (!instruction.canRead()) {
            return
        }
        try {
            FileInputStream(instruction).use { fis ->
                mediaPlayer = MediaPlayer()
                // FIXME: This may cause IllegalStateException
                setVolume(volumeLevel)
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
            mp.start()
        }
        mediaPlayer.setOnCompletionListener { mp ->
            mp.release()
        }
    }

    private fun setVolume(level: Float) {
        mediaPlayer.setVolume(level, level)
    }
}
