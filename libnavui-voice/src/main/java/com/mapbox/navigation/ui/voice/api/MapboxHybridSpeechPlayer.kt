package com.mapbox.navigation.ui.voice.api

import android.content.Context
import android.util.Log
import com.mapbox.navigation.ui.base.api.voice.SpeechPlayer
import com.mapbox.navigation.ui.base.model.voice.Announcement
import com.mapbox.navigation.ui.base.model.voice.SpeechState
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.monitorChannelWithException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
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

    private val onDoneChannel = Channel<SpeechState.Done>(Channel.UNLIMITED)
    private val onDoneReceiveChannel: ReceiveChannel<SpeechState.Done> = onDoneChannel
    private val queue: Queue<SpeechState.Play> = ConcurrentLinkedQueue()
    private val offboardSpeechPlayer: MapboxOffboardSpeechPlayer =
        MapboxOffboardSpeechPlayer(context, accessToken, language).apply {
            setDonePlayingChannel(onDoneChannel)
        }
    private val onboardSpeechPlayer: MapboxOnboardSpeechPlayer =
        MapboxOnboardSpeechPlayer(context, language).apply {
            setDonePlayingChannel(onDoneChannel)
        }
    private val onDoneJob: Job =
        ThreadController.getMainScopeAndRootJob().scope.monitorChannelWithException(
            onDoneReceiveChannel,
            {
                val current = queue.poll()
                Log.d("DEBUG", "DEBUG Hybrid poll ${current.javaClass.name}@${Integer.toHexString(current.hashCode())}")
                play()
            }
        )

    /**
     * Given [SpeechState.Play] [Announcement] the method will play the voice instruction.
     * If a voice instruction is already playing or other announcement are already queued,
     * the given voice instruction will be queued to play after.
     * @param state SpeechState Play Announcement object including the announcement text
     * and optionally a synthesized speech mp3.
     */
    override fun play(state: SpeechState.Play) {
        queue.add(state)
        Log.d("DEBUG", "DEBUG Hybrid add ${state.javaClass.name}@${Integer.toHexString(state.hashCode())}")
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
    }

    /**
     * Releases the resources used by the speech player.
     * If called while an announcement is currently playing,
     * the announcement should end immediately and any announcements queued should be cleared.
     */
    override fun shutdown() {
        offboardSpeechPlayer.shutdown()
        onboardSpeechPlayer.shutdown()
        onDoneJob.cancel()
    }

    private fun play() {
        if (queue.isNotEmpty()) {
            val currentPlay = queue.peek()
            currentPlay.announcement.file?.let {
                offboardSpeechPlayer.play(currentPlay)
            } ?: onboardSpeechPlayer.play(currentPlay)
        }
    }
}
