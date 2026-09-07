package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.CadenzaAudioRecorder
import com.example.models.CadenzaAnalysisResponse
import com.example.models.CadenzaCompareResponse
import com.example.repository.CadenzaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class PracticeState {
    IDLE,
    SEPARATING,
    READY_TO_RECORD,
    COUNTDOWN,
    RECORDING,
    ANALYZING,
    RESULTS
}

data class CadenzaPracticeUiState(
    val state: PracticeState = PracticeState.IDLE,
    
    val sessionId: String? = null,
    val accompanimentUrl: String? = null,
    val referenceFilename: String? = null,
    
    val recordedFilePath: String? = null,
    val compareResult: CadenzaCompareResponse? = null,

    val countdownValue: Int = 3,
    val statusMessage: String = "Ready to practice",
    val errorMessage: String? = null
)

class CadenzaPracticeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = CadenzaRepository()

    private val audioRecorder = CadenzaAudioRecorder(
        context = application.applicationContext
    )
    
    private val audioPlayer = com.example.audio.CadenzaAudioPlayer(
        context = application.applicationContext
    )

    private val _uiState = MutableStateFlow(CadenzaPracticeUiState())
    val uiState: StateFlow<CadenzaPracticeUiState> = _uiState.asStateFlow()

    fun hasRecordingPermission(): Boolean {
        return audioRecorder.hasRecordingPermission()
    }
    
    // ── 1. Upload Reference ──
    fun uploadReference(uri: android.net.Uri) {
        _uiState.update {
            it.copy(
                state = PracticeState.SEPARATING,
                statusMessage = "Extracting vocals...",
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                // Copy URI to temp file
                val context = getApplication<Application>().applicationContext
                val tempFile = File.createTempFile("ref_", ".wav", context.cacheDir)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                val result = withContext(Dispatchers.IO) {
                    repository.uploadReferenceAudio(tempFile)
                }

                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.sessionId != null) {
                            // Backend IP from RetrofitClient (hardcoded hack to build full URL)
                            // Usually you'd return the absolute URL from backend or configure Retrofit.
                            val baseUrl = "http://10.196.239.136:8000" 
                            val fullUrl = baseUrl + response.accompanimentUrl
                            
                            _uiState.update {
                                it.copy(
                                    state = PracticeState.READY_TO_RECORD,
                                    sessionId = response.sessionId,
                                    accompanimentUrl = fullUrl,
                                    referenceFilename = response.originalFilename,
                                    statusMessage = "Ready to Record",
                                    errorMessage = null
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    state = PracticeState.IDLE,
                                    errorMessage = "Backend failed to process reference.",
                                    statusMessage = "Upload Failed"
                                )
                            }
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update {
                            it.copy(
                                state = PracticeState.IDLE,
                                errorMessage = exception.message ?: "Failed to upload.",
                                statusMessage = "Upload failed"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        state = PracticeState.IDLE,
                        errorMessage = e.message,
                        statusMessage = "Error processing file"
                    )
                }
            }
        }
    }

    // ── 2. Start Recording ──
    fun startRecording() {
        val currentState = _uiState.value
        
        if (currentState.state != PracticeState.READY_TO_RECORD) {
            _uiState.update { it.copy(errorMessage = "Not ready to record.") }
            return
        }

        if (!audioRecorder.hasRecordingPermission()) {
            _uiState.update {
                it.copy(
                    errorMessage = "Microphone permission is required.",
                    statusMessage = "Permission needed"
                )
            }
            return
        }

        _uiState.update { 
            it.copy(
                state = PracticeState.COUNTDOWN,
                countdownValue = 3,
                statusMessage = "Get ready..."
            ) 
        }

        viewModelScope.launch {
            for (i in 3 downTo 1) {
                _uiState.update { it.copy(countdownValue = i) }
                kotlinx.coroutines.delay(1000)
            }

            val recordingResult = audioRecorder.startRecording()

            recordingResult.fold(
                onSuccess = { file ->
                    _uiState.update {
                        it.copy(
                            state = PracticeState.RECORDING,
                            recordedFilePath = file.absolutePath,
                            statusMessage = "Recording... Sing now",
                            errorMessage = null
                        )
                    }
                    
                    // Play accompaniment
                    currentState.accompanimentUrl?.let { url ->
                        audioPlayer.play(url) {
                            // On completion, automatically stop recording if still recording
                            if (_uiState.value.state == PracticeState.RECORDING) {
                                stopRecordingAndAnalyze()
                            }
                        }
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            state = PracticeState.READY_TO_RECORD,
                            errorMessage = exception.message ?: "Failed to start recording.",
                            statusMessage = "Recording failed"
                        )
                    }
                }
            )
        }
    }

    // ── 3. Stop Recording & Compare ──
    fun stopRecordingAndAnalyze() {
        if (_uiState.value.state != PracticeState.RECORDING) return

        audioPlayer.stop()
        val stopResult = audioRecorder.stopRecording()
        val sessionId = _uiState.value.sessionId

        stopResult.fold(
            onSuccess = { recordedFile ->
                _uiState.update {
                    it.copy(
                        state = PracticeState.ANALYZING,
                        recordedFilePath = recordedFile.absolutePath,
                        statusMessage = "Comparing your voice...",
                        errorMessage = null
                    )
                }

                if (sessionId != null) {
                    analyzeRecordedFile(sessionId, recordedFile)
                } else {
                     _uiState.update {
                        it.copy(
                            state = PracticeState.READY_TO_RECORD,
                            errorMessage = "No active session ID found."
                        )
                    }
                }
            },
            onFailure = { exception ->
                _uiState.update {
                    it.copy(
                        state = PracticeState.READY_TO_RECORD,
                        errorMessage = exception.message ?: "Failed to stop recording.",
                        statusMessage = "Recording stop failed"
                    )
                }
            }
        )
    }

    private fun analyzeRecordedFile(sessionId: String, audioFile: File) {
        viewModelScope.launch {
            val analysisResult = withContext(Dispatchers.IO) {
                repository.comparePerformance(sessionId, audioFile)
            }

            analysisResult.fold(
                onSuccess = { response ->
                    _uiState.update {
                        it.copy(
                            state = PracticeState.RESULTS,
                            compareResult = response,
                            statusMessage = if (response.success) "Analysis completed" else "Could not detect clear pitch",
                            errorMessage = if (response.success) null else (response.message ?: "Analysis failed.")
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            state = PracticeState.READY_TO_RECORD, // Can retry
                            errorMessage = exception.message ?: "Comparison failed.",
                            statusMessage = "Analysis failed"
                        )
                    }
                }
            )
        }
    }

    fun clearResult() {
        _uiState.update {
            CadenzaPracticeUiState(
                state = PracticeState.IDLE,
                statusMessage = "Select a song to practice"
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
        if (audioRecorder.isCurrentlyRecording()) {
            audioRecorder.stopRecording()
        }
    }
}