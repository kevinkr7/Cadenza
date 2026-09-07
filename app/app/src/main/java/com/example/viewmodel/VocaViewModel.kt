package com.example.viewmodel

import android.app.Application
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.VocaDatabase
import com.example.models.AvatarPreferences
import com.example.models.ChatMessage
import com.example.models.UserAccount
import com.example.models.UserSession
import com.example.repository.CadenzaAuthManager
import com.example.repository.VocaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed interface AuthState {
    object SignedOut : AuthState
    object Loading : AuthState
    data class LoggedIn(val email: String, val displayName: String) : AuthState
    data class Error(val message: String) : AuthState
}

class VocaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VocaRepository
    private val authManager: CadenzaAuthManager = CadenzaAuthManager(application.applicationContext)

    // --- State Properties ---
    val sessions: StateFlow<List<UserSession>>
    val chatHistory: StateFlow<List<ChatMessage>>
    val avatarConfig: StateFlow<AvatarPreferences>

    private val _authState = MutableStateFlow<AuthState>(
        if (authManager.isLoggedIn && !authManager.currentEmail.isNullOrBlank()) {
            AuthState.LoggedIn(
                email = authManager.currentEmail!!,
                displayName = authManager.currentDisplayName ?: "Vocalist"
            )
        } else {
            AuthState.SignedOut
        }
    )
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Recording and Simulation State
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationSec = MutableStateFlow(0)
    val recordingDurationSec: StateFlow<Int> = _recordingDurationSec.asStateFlow()

    private val _activePitchFreqHz = MutableStateFlow(0f)
    val activePitchFreqHz: StateFlow<Float> = _activePitchFreqHz.asStateFlow()

    private val _activePitchNote = MutableStateFlow("-")
    val activePitchNote: StateFlow<String> = _activePitchNote.asStateFlow()

    private val _pitchStability = MutableStateFlow(0) // 0 - 100%
    val pitchStability: StateFlow<Int> = _pitchStability.asStateFlow()

    private val _vocalAccuracy = MutableStateFlow(0) // 0 - 100%
    val vocalAccuracy: StateFlow<Int> = _vocalAccuracy.asStateFlow()

    private val _pitchPoints = MutableStateFlow<List<Float>>(emptyList())
    val pitchPoints: StateFlow<List<Float>> = _pitchPoints.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _isAILoading = MutableStateFlow(false)
    val isAILoading: StateFlow<Boolean> = _isAILoading.asStateFlow()

    private val _lastSessionResult = MutableStateFlow<UserSession?>(null)
    val lastSessionResult: StateFlow<UserSession?> = _lastSessionResult.asStateFlow()

    private var recordingJob: Job? = null

    init {
        val database = VocaDatabase.getDatabase(application)
        repository = VocaRepository(database.vocaDao())

        // Database reactive streams
        sessions = repository.allSessions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        chatHistory = repository.chatHistory.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Set initial avatar defaults or read existing config
        avatarConfig = repository.avatarConfigFlow
            .filterNotNull()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = AvatarPreferences()
            )

        // Seed initial chatbot welcome message if history is empty
        viewModelScope.launch {
            repository.chatHistory.first().let { history ->
                if (history.isEmpty()) {
                    val avatar = repository.getAvatarConfig()
                    val companionName = if (avatar.gender == "female") "Aria" else "Leo"
                    repository.insertChatMessage(
                        ChatMessage(
                            sender = "ai",
                            messageText = "Hi there! I am your Cadenza AI Vocal Coach, $companionName. Tap 'Start Singing Session' above to begin your training or ask me any vocal technique question!"
                        )
                    )
                }
            }
        }
    }

    // --- First-Time User Onboarding & Navigation Routines ---
    fun hasCompletedOnboarding(): Boolean = authManager.hasCompletedOnboarding

    fun completeOnboarding() {
        authManager.hasCompletedOnboarding = true
    }

    fun getInitialDestination(): String {
        return if (!authManager.isLoggedIn) {
            "login"
        } else if (!authManager.hasCompletedOnboarding) {
            "onboarding"
        } else {
            "dashboard"
        }
    }

    // --- Authentication Actions with Persistent Storage ---
    fun doLogin(email: String, pass: String, onSuccess: (isFirstTime: Boolean) -> Unit = {}) {
        val trimmedEmail = email.trim().lowercase()
        if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            _authState.value = AuthState.Error("Please provide a valid email address.")
            return
        }
        if (pass.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters.")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            delay(500)

            val account = repository.getUserAccount(trimmedEmail)
            if (account == null) {
                _authState.value = AuthState.Error("Account not found. Please create an account.")
                return@launch
            }

            val computedHash = authManager.hashPassword(pass, account.salt)
            if (computedHash != account.passwordHash) {
                _authState.value = AuthState.Error("Incorrect password. Please try again.")
                return@launch
            }

            authManager.saveSession(account.email, account.displayName)
            _authState.value = AuthState.LoggedIn(account.email, account.displayName)
            onSuccess(!authManager.hasCompletedOnboarding)
        }
    }

    fun doRegister(email: String, name: String, pass: String, onSuccess: (isFirstTime: Boolean) -> Unit = {}) {
        val trimmedEmail = email.trim().lowercase()
        val trimmedName = name.trim()

        if (trimmedName.isBlank()) {
            _authState.value = AuthState.Error("Please enter your name or artist nickname.")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            _authState.value = AuthState.Error("Please provide a valid email address.")
            return
        }
        if (pass.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters.")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            delay(500)

            val existing = repository.getUserAccount(trimmedEmail)
            if (existing != null) {
                _authState.value = AuthState.Error("An account with this email already exists. Please sign in.")
                return@launch
            }

            val salt = authManager.generateSalt()
            val hash = authManager.hashPassword(pass, salt)
            val newAccount = UserAccount(
                email = trimmedEmail,
                displayName = trimmedName,
                passwordHash = hash,
                salt = salt
            )

            repository.insertUserAccount(newAccount)
            authManager.saveSession(newAccount.email, newAccount.displayName)
            _authState.value = AuthState.LoggedIn(newAccount.email, newAccount.displayName)
            onSuccess(!authManager.hasCompletedOnboarding)
        }
    }

    fun doGoogleSignIn(onSuccess: (isFirstTime: Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            delay(600)

            val demoEmail = "google.artist@cadenza.app"
            val demoName = "Cadenza Vocalist"

            var account = repository.getUserAccount(demoEmail)
            if (account == null) {
                val salt = authManager.generateSalt()
                val hash = authManager.hashPassword("google_demo_pass", salt)
                account = UserAccount(
                    email = demoEmail,
                    displayName = demoName,
                    passwordHash = hash,
                    salt = salt
                )
                repository.insertUserAccount(account)
            }

            authManager.saveSession(account.email, account.displayName)
            _authState.value = AuthState.LoggedIn(account.email, account.displayName)
            onSuccess(!authManager.hasCompletedOnboarding)
        }
    }

    fun clearAuthError() {
        if (_authState.value is AuthState.Error) {
            _authState.value = AuthState.SignedOut
        }
    }

    fun doForgotPassword(email: String): String {
        val trimmed = email.trim().lowercase()
        return if (Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
            "Password reset instructions have been dispatched to $trimmed."
        } else {
            "Please supply a valid email address."
        }
    }

    fun doLogout() {
        authManager.clearSession()
        _authState.value = AuthState.SignedOut
    }

    // --- Avatar Customization ---
    fun updateAvatar(gender: String, hairstyle: String, outfit: String, theme: String) {
        viewModelScope.launch {
            val updated = AvatarPreferences(
                id = 1,
                gender = gender,
                hairstyle = hairstyle,
                outfit = outfit,
                colorTheme = theme
            )
            repository.updateAvatarConfig(updated)
            
            // Send companionship system alert to chat
            val coachName = if (gender == "female") "Aria" else "Leo"
            repository.insertChatMessage(
                ChatMessage(
                    sender = "ai",
                    messageText = "System: Assistant updated to $coachName with style modifications. Ready to sing!"
                )
            )
        }
    }

    // --- AI Companion Coaching Chat ---
    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            // Insert user message
            val userMsg = ChatMessage(sender = "user", messageText = text)
            repository.insertChatMessage(userMsg)

            // Show typing indicator
            _isAILoading.value = true

            // Wait a moment and ask Gemini
            delay(1000)
            val currentAvatar = repository.getAvatarConfig()
            val currentHistory = chatHistory.value
            val aiResponse = repository.askGeminiCoach(text, currentAvatar, currentHistory)

            // Insert coach response
            val aiMsg = ChatMessage(sender = "ai", messageText = aiResponse)
            repository.insertChatMessage(aiMsg)

            _isAILoading.value = false
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearChatHistory()
            val avatar = repository.getAvatarConfig()
            val companionName = if (avatar.gender == "female") "Aria" else "Leo"
            repository.insertChatMessage(
                ChatMessage(
                    sender = "ai",
                    messageText = "Chat records cleared! Ask $companionName your next vocal query."
                )
            )
        }
    }

    // --- Audio Session Recording Simulator ---
    fun toggleRecording() {
        if (_isRecording.value) {
            // Stop recording & Analyze
            _isRecording.value = false
            recordingJob?.cancel()
            analyzeFinishedSession()
        } else {
            // Start recording
            _isRecording.value = true
            _recordingDurationSec.value = 0
            _pitchPoints.value = emptyList()
            _activePitchFreqHz.value = 0f
            _activePitchNote.value = "-"
            _pitchStability.value = 0
            _vocalAccuracy.value = 0

            recordingJob = viewModelScope.launch {
                // Musical scale lookup
                val notes = listOf(
                    "C4" to 261.63f, "D4" to 293.66f, "E4" to 329.63f, "F4" to 349.23f,
                    "G4" to 392.00f, "A4" to 440.00f, "B4" to 493.88f, "C5" to 523.25f,
                    "E4" to 329.63f, "G4" to 392.00f
                )

                while (true) {
                    delay(350)
                    _recordingDurationSec.value += 1
                    
                    // Pitch tracker simulations: occasional silence or wandering notes
                    val noteSample = notes[Random.nextInt(notes.size)]
                    // Random fluctuation within 5Hz
                    val fluctuation = Random.nextFloat() * 10f - 5f
                    val actualFreq = noteSample.second + fluctuation
                    
                    _activePitchFreqHz.value = actualFreq
                    _activePitchNote.value = noteSample.first
                    
                    // Stability index simulation (ranges 60% to 100%)
                    _pitchStability.value = Random.nextInt(75, 100)
                    
                    // Accuracy index simulation
                    _vocalAccuracy.value = Random.nextInt(70, 99)

                    // Add to pitch wave sequence
                    val currentList = _pitchPoints.value.toMutableList()
                    if (currentList.size > 25) {
                        currentList.removeAt(0)
                    }
                    currentList.add(actualFreq)
                    _pitchPoints.value = currentList
                }
            }
        }
    }

    private fun analyzeFinishedSession() {
        viewModelScope.launch {
            _isAnalyzing.value = true
            delay(1500) // Professional analysis loading screen

            // Calculate final performance metrics based on simulating results
            val duration = _recordingDurationSec.value.coerceAtLeast(3)
            val overall = Random.nextInt(75, 96)
            val pitchAcc = Random.nextInt(72, 98)
            val timingAcc = Random.nextInt(78, 97)
            val stability = Random.nextInt(74, 95)
            
            val detectedNotesList = listOf("C4", "E4", "G4", "A4", "D4", "C5")
                .shuffled()
                .take(4)
                .joinToString(", ")

            val averageFreq = 345.5f + (Random.nextFloat() * 30f - 15f)

            // Helpful educational coach advice cards
            val feedbackList = listOf(
                "Great projection! Watch your pitch breath support during high E4 notes; tightening your neck causes slight flattening.",
                "Fantastic chest resonance. Ensure you swallow to relax your larynx when transitioning up to head register.",
                "Your mid-range stability score is superb. Focus on landing note attacks cleanly without vocal slides or scoops.",
                "Wonderful timing and phrasing stability! Maintain diaphragm push to sustain tail notes evenly.",
                "Brilliant range representation! Try practicing G4 note scaling to eliminate minor flatting under throat dryness."
            )
            val advice = feedbackList[Random.nextInt(feedbackList.size)]

            val newSession = UserSession(
                durationSec = duration,
                overallScore = overall,
                pitchAccuracy = pitchAcc,
                timingAccuracy = timingAcc,
                vocalStability = stability,
                detectedNotes = if (detectedNotesList.isBlank()) "C4, G4" else detectedNotesList,
                averageFrequencyHz = averageFreq,
                feedbackSuggestion = advice
            )

            // Save to local database persistence!
            repository.insertSession(newSession)
            _lastSessionResult.value = newSession
            _isAnalyzing.value = false
        }
    }

    fun clearAllSessionHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _lastSessionResult.value = null
        }
    }
}
