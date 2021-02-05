package com.mapbox.navigation.ui.voice.api

import android.content.Context
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.ui.base.api.voice.SpeechApi
import com.mapbox.navigation.ui.base.api.voice.SpeechCallback
import com.mapbox.navigation.ui.base.model.voice.Announcement
import com.mapbox.navigation.ui.base.model.voice.SpeechState
import com.mapbox.navigation.ui.voice.model.VoiceState

/**
 *
 * @property context Context
 * @property accessToken String
 * @property language String
 */
class MapboxSpeechApi(
    private val context: Context,
    private val accessToken: String,
    private val language: String
) : SpeechApi {

    private val voiceAPI = MapboxVoiceApi(context, accessToken, language)

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
                    val announcement = voiceInstruction.announcement()
                    val ssmlAnnouncement = voiceInstruction.ssmlAnnouncement()
                    if (!announcement.isNullOrBlank()) {
                        callback.onAvailable(
                            SpeechState.Speech.Available(
                                Announcement(
                                    announcement,
                                    ssmlAnnouncement,
                                    instructionFile.instructionFile
                                )
                            )
                        )
                    } else {
                        callback.onError(
                            SpeechState.Speech.Error(
                                "Announcement can't be null or blank"
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
}
