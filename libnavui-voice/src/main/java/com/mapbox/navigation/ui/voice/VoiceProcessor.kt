package com.mapbox.navigation.ui.voice

import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.api.speech.v1.MapboxSpeech
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.IOException

internal object VoiceProcessor {

    private const val SSML_TEXT_TYPE = "ssml"

    /**
     * The function takes [VoiceAction] performs business logic and returns [VoiceResult]
     * @param action VoiceAction user specific commands
     * @return VoiceResult
     */
    fun process(action: VoiceAction): VoiceResult {
        return when (action) {
            is VoiceAction.PrepareVoiceRequest -> {
                prepareRequest(action.instruction)
            }
            is VoiceAction.ProcessVoiceResponse -> {
                processResponse(action.response)
            }
        }
    }

    private fun prepareRequest(instruction: VoiceInstructions): VoiceResult {
        val request = MapboxSpeech.builder()
            .instruction(instruction.ssmlAnnouncement() ?: "")
            .textType(SSML_TEXT_TYPE)
        return VoiceResult.VoiceRequest(request)
    }

    private fun processResponse(response: Response<ResponseBody>): VoiceResult {
        when {
            response.isSuccessful -> {
                response.body()?.let {
                    return VoiceResult.Voice.Success(it)
                } ?: return VoiceResult.Voice.Empty("No data available")
            }
            !response.isSuccessful -> {
                return try {
                    VoiceResult.Voice.Failure(response.errorBody()?.string())
                } catch (exception: IOException) {
                    VoiceResult.Voice.Failure(exception.localizedMessage)
                }
            }
            else -> {
                return VoiceResult.Voice.Failure("Unknown error")
            }
        }
    }
}
