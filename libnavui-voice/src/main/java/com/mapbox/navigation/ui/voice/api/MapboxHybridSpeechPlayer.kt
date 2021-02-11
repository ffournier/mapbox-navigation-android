package com.mapbox.navigation.ui.voice.api

import android.content.Context
import com.mapbox.navigation.ui.base.api.voice.SpeechPlayer
import com.mapbox.navigation.ui.base.api.voice.SpeechPlayerCallback
import com.mapbox.navigation.ui.base.model.voice.Announcement
import com.mapbox.navigation.ui.base.model.voice.SpeechState
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Hybrid implementation of [SpeechPlayer] combining [MapboxOnboardSpeechPlayer] and
 * [MapboxOffboardSpeechPlayer] speech players.
 * @property context Context
 * @property accessToken String
 * @property language String
 */
class MapboxHybridSpeechPlayer(
    private val context: Context,
    private val accessToken: String,
    private val language: String
) : SpeechPlayer {

    private val queue: Queue<SpeechState.Play> = ConcurrentLinkedQueue()
    private val offboardSpeechPlayer: MapboxOffboardSpeechPlayer =
        MapboxOffboardSpeechPlayer(context, accessToken, language)
    private val onboardSpeechPlayer: MapboxOnboardSpeechPlayer =
        MapboxOnboardSpeechPlayer(context, language)
    private var clientCallback: SpeechPlayerCallback? = null
    private var localCallback: SpeechPlayerCallback = object : SpeechPlayerCallback {
        override fun onDone(state: SpeechState.Done) {
            val announcement = queue.poll()
            clientCallback?.onDone(SpeechState.Done(announcement.announcement))
            play()
        }
    }

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
        queue.add(state)
        if (queue.size == 1) {
            play()
        }
    }

    /**
     * The method will set the volume to the specified level from [SpeechState.Volume].
     * @param state SpeechState Volume level.
     */
    override fun volume(state: SpeechState.Volume) {
        offboardSpeechPlayer.volume(state)
        onboardSpeechPlayer.volume(state)
    }

    /**
     * Clears any announcements queued.
     */
    override fun clear() {
        queue.clear()
        offboardSpeechPlayer.clear()
        onboardSpeechPlayer.clear()
        clientCallback = null
    }

    /**
     * Releases the resources used by the speech player.
     * If called while an announcement is currently playing,
     * the announcement should end immediately and any announcements queued should be cleared.
     */
    override fun shutdown() {
        offboardSpeechPlayer.shutdown()
        onboardSpeechPlayer.shutdown()
    }

    private fun play() {
        if (queue.isNotEmpty()) {
            val currentPlay = queue.peek()
            currentPlay.announcement.file?.let {
                offboardSpeechPlayer.play(currentPlay, localCallback)
            } ?: onboardSpeechPlayer.play(currentPlay, localCallback)
        }
    }
}
