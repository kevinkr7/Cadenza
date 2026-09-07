package com.example.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

// Room Entity for storing vocal coaching session history
@Entity(tableName = "user_sessions")
data class UserSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val songTitle: String = "Free Practice",
    val durationSec: Int,
    val overallScore: Int,
    val pitchAccuracy: Int,
    val timingAccuracy: Int,
    val vocalStability: Int,
    val detectedNotes: String, // Stringified list of notes, e.g., "C4, E4, G4, A4"
    val averageFrequencyHz: Float,
    val feedbackSuggestion: String
) : Serializable

// Room Entity for storing AI Companion chat history
@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String, // "user" or "ai"
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

// Room Entity for storing Companion Avatar customization state (single row, primary key = 1)
@Entity(tableName = "avatar_preferences")
data class AvatarPreferences(
    @PrimaryKey val id: Int = 1,
    val gender: String = "female", // "female" or "male"
    val hairstyle: String = "vibrant", // "classic", "modern", "vibrant", "retro"
    val outfit: String = "studio", // "casual", "formal", "studio", "rockstar"
    val colorTheme: String = "teal" // "teal", "purple", "coral", "indigo"
) : Serializable

// Room Entity for storing registered user accounts
@Entity(tableName = "user_accounts")
data class UserAccount(
    @PrimaryKey val email: String,
    val displayName: String,
    val passwordHash: String,
    val salt: String,
    val createdAtTimestamp: Long = System.currentTimeMillis()
) : Serializable

// Retrofit API Request & Response Data Models for Future/Placeholder Backend Integration
data class AudioUploadResponse(
    val success: Boolean,
    val message: String,
    val analysis: AudioAnalysisResult
)

data class AudioAnalysisResult(
    val pitchAccuracy: Int,
    val timingAccuracy: Int,
    val vocalStability: Int,
    val overallScore: Int,
    val detectedNotes: List<String>,
    val averageFrequency: Float,
    val feedback: List<String>
)

data class CoachingRecommendation(
    val recommendationId: String,
    val focusArea: String,
    val description: String,
    val difficulty: String, // "Beginner", "Intermediate", "Advanced"
    val miniExerciseName: String
)

data class UserPerformanceReport(
    val totalPracticeSessions: Int,
    val averageOverallScore: Float,
    val highestScore: Int,
    val totalPracticeMinutes: Float,
    val streakDays: Int,
    val dailyProgress: List<DailyScoreData>
)

data class DailyScoreData(
    val dateLabel: String,
    val score: Int
)
