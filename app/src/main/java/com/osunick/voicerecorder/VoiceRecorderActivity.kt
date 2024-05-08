package com.osunick.voicerecorder

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.osunick.voicerecorder.extensions.toast
import com.osunick.voicerecorder.speech.OnSpeechEventListener
import com.osunick.voicerecorder.speech.VRSpeechRecognizer
import com.osunick.voicerecorder.ui.compose.VRScaffold
import com.osunick.voicerecorder.ui.theme.VoiceRecorderTheme
import com.osunick.voicerecorder.viewmodel.LogEvent
import com.osunick.voicerecorder.viewmodel.LogsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class VoiceRecorderActivity : ComponentActivity() {

    private val viewModel: LogsViewModel by viewModels()

    private lateinit var speechRecognizer: VRSpeechRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSpeechRecognizer()
        setupEventListener()
        enableEdgeToEdge()
        setContent {
            VoiceRecorderTheme {
                VRScaffold(viewModel.uiState, viewModel.messageFlow, viewModel.eventsFlow)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer = VRSpeechRecognizer(this, object: OnSpeechEventListener {
            override fun onRecognitionNotAvailable() {
                toast("No speech recognition available")
            }

            override fun onSpeechRecognized(speech: String) {
                viewModel.saveVoiceRecording(speech)
            }
        })
        speechRecognizer.init()
    }

    private fun setupEventListener() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.eventsFlow.collectLatest {
                    when (it) {
                        LogEvent.None -> { /* No-op */
                        }

                        LogEvent.Save -> viewModel.saveMessage()
                        LogEvent.Share -> {
                            share()
                        }

                        is LogEvent.UpdateLog -> viewModel.updateMessage(it.logText)
                        LogEvent.StartRecording -> startRecording()
                        is LogEvent.DeleteLog -> deleteLog(it.id)
                    }
                    if (it != LogEvent.None) {
                        viewModel.clearEvent()
                    }
                }
            }
        }
    }

    private fun logsDir(): File {
        val logsDir = File(filesDir, "logs/")
        if (!logsDir.exists()) {
            logsDir.mkdir()
        }
        return logsDir
    }

    private fun share() {
        lifecycleScope.launch {
            val file: File = viewModel.createFile(logsDir())
            val fileUri = FileProvider.getUriForFile(
                this@VoiceRecorderActivity,
                packageName.plus(".fileprovider"),
                file)
            val shareIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, fileUri)
                type = "text/plain"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_file)))
        }
    }

    private fun deleteLog(id: Int) =
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_log)
            .setMessage(R.string.are_you_sure_delete)
            .setPositiveButton(R.string.delete) { dialog, _ ->
                viewModel.deleteMessage(id)
                dialog?.dismiss()
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog?.dismiss()
            }.show()


    private fun startRecording() {
        if (checkAudioPermission()) {
            // Start recording
            viewModel.setIsRecording()
            speechRecognizer.startListening()
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                startRecording()
            } else {
                toast(R.string.cannot_recognize_speech_without_this_permission)
            }
        }

    private fun checkAudioPermission(): Boolean {
        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                return true
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this, android.Manifest.permission.RECORD_AUDIO
            ) -> {
                AlertDialog.Builder(this)
                    .setTitle(R.string.audio_permission_required)
                    .setMessage(R.string.need_audio_permission)
                    .setPositiveButton(
                        android.R.string.ok
                    ) { dialog, _ -> dialog?.dismiss() }
                    .setNegativeButton(
                        android.R.string.cancel
                    ) { dialog, _ -> dialog?.dismiss() }.show()
                return false
            }

            else -> {
                requestPermissionLauncher.launch(
                    android.Manifest.permission.RECORD_AUDIO
                )
                return false
            }
        }
    }
}
