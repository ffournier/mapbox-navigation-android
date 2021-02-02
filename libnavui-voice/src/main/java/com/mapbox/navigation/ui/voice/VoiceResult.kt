package com.mapbox.navigation.ui.voice

import com.mapbox.api.speech.v1.MapboxSpeech
import okhttp3.ResponseBody

internal sealed class VoiceResult {
    data class VoiceRequest(
        val request: MapboxSpeech.Builder
    ) : VoiceResult()

    sealed class Voice : VoiceResult() {
        data class Success(val data: ResponseBody) : Voice()
        data class Failure(val error: String?) : Voice()
        data class Empty(val error: String) : Voice()
    }
}
