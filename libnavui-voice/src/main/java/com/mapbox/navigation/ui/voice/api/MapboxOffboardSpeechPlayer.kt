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
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

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

    private var mediaPlayer: MediaPlayer? = null
    private var volumeLevel: Float = DEFAULT_VOLUME_LEVEL
    private var queue: Queue<File> = ConcurrentLinkedQueue()

    /**
     * Given [SpeechState.Play] [Announcement] the method will play the voice instruction.
     * If a voice instruction is already playing or other announcement are already queued,
     * the given voice instruction will be queued to play after.
     * @param state SpeechState Play Announcement object including the announcement text
     * and optionally a synthesized speech mp3.
     */
    override fun play(state: SpeechState.Play) {
        state.announcement.file?.let {
            queue.add(it)
        }
        if (queue.size == 1) {
            play()
        }
    }

    /**
     * The method will set the volume to the specified level from [SpeechState.Volume].
     * @param state SpeechState Volume level.
     */
    override fun volume(state: SpeechState.Volume) {
        volumeLevel = state.level
        setVolume(volumeLevel)
    }

    /**
     * Releases the resources used by the speech player.
     * If called while an announcement is currently playing,
     * the announcement should end immediately and any announcements queued should be cleared.
     */
    override fun shutdown() {
        queue.clear()
        mediaPlayer?.release()
        mediaPlayer = null
        volumeLevel = DEFAULT_VOLUME_LEVEL
    }

    private fun play() {
        if (queue.isNotEmpty()) {
            setupMediaPlayer(queue.peek())
        }
    }

    private fun setupMediaPlayer(instruction: File) {
        if (!instruction.canRead()) {
            return
        }
        try {
            FileInputStream(instruction).use { fis ->
                mediaPlayer = MediaPlayer()
                setVolume(volumeLevel)
                mediaPlayer?.setDataSource(fis.fd)
                mediaPlayer?.prepareAsync()
                addListeners()
            }
        } catch (ex: FileNotFoundException) {
        } catch (ex: IOException) {
        }
    }

    private fun addListeners() {
        mediaPlayer?.setOnErrorListener { _, _, _ ->
            false
        }
        mediaPlayer?.setOnPreparedListener { mp ->
            mp.start()
        }
        mediaPlayer?.setOnCompletionListener { mp ->
            mp.release()
            mediaPlayer = null
            queue.poll()
            play()
        }
    }

    private fun setVolume(level: Float) {
        mediaPlayer?.setVolume(level, level)
    }

    private companion object {
        private const val DEFAULT_VOLUME_LEVEL = 0.5f
    }
}
