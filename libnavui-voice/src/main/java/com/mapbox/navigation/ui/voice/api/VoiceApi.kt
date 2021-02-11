package com.mapbox.navigation.ui.voice.api

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.ui.base.model.voice.Announcement

/**
 * An Api that allows you to retrieve voice instruction files based on [VoiceInstructions]
 */
internal interface VoiceApi {

    /**
     * Given [VoiceInstructions] the method returns a [File] wrapped inside [VoiceCallback]
     * @param voiceInstruction VoiceInstructions object representing [VoiceInstructions]
     * @param callback VoiceCallback contains [VoiceState.VoiceFile]
     */
    fun retrieveVoiceFile(
        voiceInstruction: VoiceInstructions,
        callback: VoiceCallback
    )

    /**
     * Given the [Announcement] the method may cleanup any associated files previously generated.
     * @param announcement
     */
    fun clean(announcement: Announcement)
}
