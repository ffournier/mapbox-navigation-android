package com.mapbox.navigation.ui.base.model.voice

import java.io.File

/**
 * @property announcement retrieved from [VoiceInstructions]. Can be either
 * SSML or normal announcement text depending on the [SpeechApi] used and
 * its availability.
 * @property file synthesized speech mp3.
 * This is optional as it's only needed by hybrid and offboard speech APIs.
 */
data class Announcement(
    val announcement: String,
    val file: File?
)
