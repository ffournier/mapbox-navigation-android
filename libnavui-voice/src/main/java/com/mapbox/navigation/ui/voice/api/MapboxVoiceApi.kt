package com.mapbox.navigation.ui.voice.api

import android.content.Context
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.ui.base.api.voice.VoiceApi
import com.mapbox.navigation.ui.base.api.voice.VoiceCallback
import com.mapbox.navigation.ui.base.model.voice.VoiceState
import com.mapbox.navigation.ui.voice.VoiceAction
import com.mapbox.navigation.ui.voice.VoiceProcessor
import com.mapbox.navigation.ui.voice.VoiceResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

/**
 * Implementation of [VoiceApi] allowing you to retrieve voice instructions.
 * @property context Context
 * @property accessToken String
 * @property language String
 */
class MapboxVoiceApi(
    private val context: Context,
    private val accessToken: String,
    private val language: String
) : VoiceApi {

    /**
     * Given [VoiceInstructions] the method returns a [File] wrapped inside [VoiceCallback]
     * @param voiceInstruction VoiceInstructions object representing [VoiceInstructions]
     * @param callback VoiceCallback contains [VoiceState.VoiceFile]
     */
    override fun retrieveVoiceFile(voiceInstruction: VoiceInstructions, callback: VoiceCallback) {
        val action = VoiceAction.PrepareVoiceRequest(voiceInstruction)
        when (val result = VoiceProcessor.process(action)) {
            is VoiceResult.VoiceRequest -> {
                val mapboxSpeech = result.request
                    .accessToken(accessToken)
                    .language(language)
                    .build()
                mapboxSpeech.enqueueCall(object : Callback<ResponseBody> {
                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                        val voiceAction = VoiceAction.ProcessVoiceResponse(response)
                        when (val res = VoiceProcessor.process(voiceAction)) {
                            is VoiceResult.Voice.Success -> {
                                CoroutineScope(Dispatchers.Main).launch {
                                    val voiceFile = generateVoiceFileFrom(res.data)
                                    callback.onVoiceFileReady(VoiceState.VoiceFile(voiceFile))
                                }
                            }
                            is VoiceResult.Voice.Failure -> {
                                callback.onFailure(
                                    VoiceState.VoiceFailure.VoiceError(res.error)
                                )
                            }
                            is VoiceResult.Voice.Empty -> {
                                callback.onFailure(
                                    VoiceState.VoiceFailure.VoiceError(res.error)
                                )
                            }
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        callback.onFailure(
                            VoiceState.VoiceFailure.VoiceError(t.localizedMessage)
                        )
                    }
                })
            }
        }
    }

    private suspend fun generateVoiceFileFrom(data: ResponseBody): File =
        withContext(Dispatchers.IO) {
            val cacheDirectory = context.applicationContext.cacheDir
            val instructionsCacheDirectory = File(cacheDirectory, MAPBOX_INSTRUCTIONS_CACHE)
                .also { it.mkdirs() }
            File(instructionsCacheDirectory, "${retrieveUniqueId()}$MP3_EXTENSION")
                .apply { outputStream().use { data.byteStream().copyTo(it) } }
        }

    private fun retrieveUniqueId(): String = (++uniqueId).toString()

    /**
     * The method stops the process of retrieving the File and destroys any related callbacks.
     */
    override fun cancel() {
        TODO("Not yet implemented")
    }

    private companion object {
        private const val MAPBOX_INSTRUCTIONS_CACHE = "mapbox_instructions_cache"
        private const val MP3_EXTENSION = ".mp3"
        private var uniqueId = 0
    }
}
