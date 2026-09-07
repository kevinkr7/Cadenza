package com.example.repository

import android.util.Log
import com.example.BuildConfig
import com.example.database.VocaDao
import com.example.models.AvatarPreferences
import com.example.models.ChatMessage
import com.example.models.UserSession
import com.example.models.UserAccount
import com.example.network.GeminiContent
import com.example.network.GeminiGenerationConfig
import com.example.network.GeminiPart
import com.example.network.GeminiRequest
import com.example.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class VocaRepository(private val vocaDao: VocaDao) {

    // --- User Account Authentication Methods ---
    suspend fun insertUserAccount(user: UserAccount) = withContext(Dispatchers.IO) {
        vocaDao.insertUserAccount(user)
    }

    suspend fun getUserAccount(email: String): UserAccount? = withContext(Dispatchers.IO) {
        vocaDao.getUserAccount(email.trim().lowercase())
    }

    suspend fun getAllUserAccounts(): List<UserAccount> = withContext(Dispatchers.IO) {
        vocaDao.getAllUserAccounts()
    }

    suspend fun deleteUserAccount(email: String) = withContext(Dispatchers.IO) {
        vocaDao.deleteUserAccount(email.trim().lowercase())
    }

    // --- Database Queries (Reactive Flows) ---
    val allSessions: Flow<List<UserSession>> = vocaDao.getAllSessions()
    val chatHistory: Flow<List<ChatMessage>> = vocaDao.getChatHistory()
    val avatarConfigFlow: Flow<AvatarPreferences?> = vocaDao.getAvatarConfigFlow()

    suspend fun insertSession(session: UserSession) = withContext(Dispatchers.IO) {
        vocaDao.insertSession(session)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        vocaDao.clearAllSessions()
    }

    suspend fun insertChatMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        vocaDao.insertChatMessage(message)
    }

    suspend fun clearChatHistory() = withContext(Dispatchers.IO) {
        vocaDao.clearChatHistory()
    }

    suspend fun getAvatarConfig(): AvatarPreferences {
        return withContext(Dispatchers.IO) {
            vocaDao.getAvatarConfig() ?: AvatarPreferences().also {
                vocaDao.updateAvatarConfig(it)
            }
        }
    }

    suspend fun updateAvatarConfig(prefs: AvatarPreferences) = withContext(Dispatchers.IO) {
        vocaDao.updateAvatarConfig(prefs)
    }

    // --- Gemini AI Assistant Integration ---
    suspend fun askGeminiCoach(
        userInput: String,
        avatarPrefs: AvatarPreferences,
        chatHistoryList: List<ChatMessage>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w("VocaRepository", "Gemini API key is missing or is using placeholder. Falling back to local offline responses.")
            return@withContext getOfflineCoachReply(userInput, avatarPrefs)
        }

        try {
            // Build conversation history for context (keep recent 8 messages to prevent overflow)
            val historyParts = chatHistoryList.takeLast(8).map { msg ->
                GeminiContent(
                    role = if (msg.sender == "user") "user" else "model",
                    parts = listOf(GeminiPart(text = msg.messageText))
                )
            }

            // Append newest question
            val currentContent = GeminiContent(
                role = "user",
                parts = listOf(GeminiPart(text = userInput))
            )

            val fullContentsList = historyParts + currentContent

            // Create customized coach system instructions based on Avatar Settings
            val companionName = if (avatarPrefs.gender == "female") "Aria" else "Leo"
            val companionVibe = when (avatarPrefs.colorTheme) {
                "purple" -> "artsy, elegant, and poetic"
                "teal" -> "highly scientific, detail-oriented, and encouraging"
                "coral" -> "energetic, dramatic, and rockstar-like"
                else -> "professional, balanced, and studio-seasoned"
            }

            val systemInstruction = GeminiContent(
                role = "user", // Endpoint expects systemInstructions as a normal content block
                parts = listOf(GeminiPart(text = """
                    You are Cadenza, an expert mobile AI Vocal Coach. Your name is $companionName. 
                    You look like a virtual companion with a style of: Gender=${avatarPrefs.gender}, Hairstyle=${avatarPrefs.hairstyle}, Outfit=${avatarPrefs.outfit}, Accent color theme=${avatarPrefs.colorTheme}. 
                    Your personality is $companionVibe.
                    Provide professional, warm, highly practical, and motivating feedback about singing, chest voice, head voice, pitch accuracy, breathing support, phrasing, and dynamic volume control. 
                    Keep responses punchy, concise (2-4 sentences max), and optimized for reading on a mobile chat screen.
                    Always encourage the user, suggest simple 30-second breathing or vocal warm-up exercises when appropriate, and ask if they are ready for their next practice session!
                """.trimIndent()))
            )

            val request = GeminiRequest(
                contents = fullContentsList,
                systemInstruction = systemInstruction,
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.7f,
                    maxOutputTokens = 300
                )
            )

            val response = RetrofitClient.geminiService.generateContent(
                model = "gemini-1.5-flash",
                apiKey = apiKey,
                request = request
            )

            val textResult = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (textResult.isNullOrBlank()) {
                getOfflineCoachReply(userInput, avatarPrefs)
            } else {
                textResult
            }
        } catch (e: Exception) {
            Log.e("VocaRepository", "Gemini API call failed: ${e.message}", e)
            getOfflineCoachReply(userInput, avatarPrefs)
        }
    }

    // High quality offline fallback companion responses
    private fun getOfflineCoachReply(input: String, avatar: AvatarPreferences): String {
        val coachName = if (avatar.gender == "female") "Aria" else "Leo"
        val query = input.lowercase()

        return when {
            query.contains("warm") || query.contains("exercise") || query.contains("practice") -> {
                "Hello singer! Let's do a classic 'Lip Trill' (slurring pitch up and down the scale) for 30 seconds to expand your range and ease tension. This will relax your vocal cords and prepare you for hitting higher registers comfortably!"
            }
            query.contains("pitch") || query.contains("flat") || query.contains("sharp") -> {
                "Hitting the pitch center is all about internalizing the note before you make a sound. Focus on relaxing your jaw and maintaining robust airflow. Try practice singing simple intervals (fifths or octaves) to build muscle memory!"
            }
            query.contains("breath") || query.contains("diaphragm") || query.contains("support") -> {
                "Proper support comes from your diaphragm! Place your hand on your lower belly and take a deep breath; your expansion should feel horizontal, not up into your neck. Exhale with a steady 'Sss' sound to master dynamic control!"
            }
            query.contains("throat") || query.contains("hurt") || query.contains("strain") -> {
                "If it hurts, stop immediately! Strain usually happens when you force chest resonance high instead of shifting to your head voice (or mixed register). Drink warm water, swallow twice to lift your soft palate, and try again relaxed."
            }
            query.contains("high") || query.contains("notes") -> {
                "To hit high notes beautifully, avoid reaching or pointing your neck up. Instead, anchor your lower abdomen, raise your soft palate (as if starting a yawn), and let the sound resonate in your nasal cavities for head-voice shine!"
            }
            else -> {
                "Excellent question! As your personal vocal companion $coachName, I'm here to analyze your tone and assist you. Focus on maintaining a steady posture and relaxed shoulders. Ready to launch our singing session and log your accuracy?"
            }
        }
    }
}
