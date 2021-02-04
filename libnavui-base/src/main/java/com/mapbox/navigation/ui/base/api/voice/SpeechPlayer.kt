package com.mapbox.navigation.ui.base.api.voice

import com.mapbox.navigation.ui.base.model.voice.Announcement

/**
 * An Api that allows you to interact with the speech player
 */
interface SpeechPlayer {

    /**
     * Given [Announcement] the method will play the voice instruction.
     * If a voice instruction is already playing or other announcement are already queued,
     * the given voice instruction will be queued to play after.
     * @param announcement Announcement object including the announcement text and optionally
     * a synthesized speech mp3.
     */
    fun play(announcement: Announcement)

    /**
     * If called while an announcement is currently playing,
     * the announcement should end immediately and any announcements queued should be cleared.
     */
    fun stop()

    /**
     * Releases the resources used by the speech player.
     */
    fun shutdown()
}
