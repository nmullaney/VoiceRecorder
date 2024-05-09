package com.osunick.voicerecorder.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.osunick.voicerecorder.VoiceRecorderActivity

class VRSpeechRecognizer(
    private val context: Context,
    val onSpeechEventListener: OnSpeechEventListener) {

    private var speechRecognizer: SpeechRecognizer? = null

    fun init() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
           onSpeechEventListener.onRecognitionNotAvailable()
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object: RecognitionListener {
            override fun onReadyForSpeech(results: Bundle?) {
                Log.d(TAG, "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Speech begin")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // This is chatty since it logs whenever the volume changes
                //Log.d(TAG, "Rms changed: $rmsdB")
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                Log.d(TAG, "Buffer received")
            }

            override fun onEndOfSpeech() {
                Log.d(TAG, "Speech ended")
                onSpeechEventListener.onSpeechEnded()
            }

            override fun onError(error: Int) {
                Log.d(TAG, "Error: $error")
                onSpeechEventListener.onSpeechError(error)
            }

            override fun onResults(results: Bundle?) {
                Log.d(TAG, "Results: $results")
                val data: ArrayList<String>? =
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                Log.d(TAG, "Speech recognition results received: $data, size: ${data?.size}")
                data?.let {
                    onSpeechEventListener.onSpeechRecognized(it.joinToString(" "))
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                Log.d(TAG, "Results: $partialResults")
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                Log.d(TAG, "On event: $eventType $params")
            }
        })
    }

    fun startListening() {
        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, LANGUAGE)
        }
        speechRecognizer?.startListening(recognizerIntent)
    }

    fun destroy() {
        speechRecognizer?.destroy()
    }

    companion object {
        const val TAG = "SpeechRecognizer"
        const val LANGUAGE = "en-US"
    }
}

interface OnSpeechEventListener {
    fun onRecognitionNotAvailable()
    fun onSpeechRecognized(speech: String)
    fun onSpeechError(error: Int)
    fun onSpeechEnded()
}