package com.example.models

data class CadenzaAnalysisResponse(
    val success: Boolean = false,

    val score: Int? = null,

    val note: String? = null,
    val frequency: Double? = null,
    val idealFrequency: Double? = null,
    val deviationCents: Double? = null,
    val pitchStatus: String? = null,

    val stability: String? = null,
    val stabilityCents: Double? = null,

    val averagePitch: Double? = null,
    val medianPitch: Double? = null,
    val minimumPitch: Double? = null,
    val maximumPitch: Double? = null,

    val durationSeconds: Double? = null,

    val practiceMode: String? = null,
    val practiceModeLabel: String? = null,
    val practiceModeConfidence: String? = null,
    val practiceModeMessage: String? = null,

    val pitchRangeCents: Double? = null,
    val distinctNoteCount: Int? = null,
    val noteChangeCount: Int? = null,

    val coachMessage: String? = null,
    val advice: String? = null,

    val pitchTrack: List<PitchTrackPoint> = emptyList(),

    val uploadedFilename: String? = null,
    val storedFilename: String? = null,

    val message: String? = null
)

data class PitchTrackPoint(
    val time: Double? = null,
    val frequency: Double? = null
)

data class UploadReferenceResponse(
    val success: Boolean = false,
    val sessionId: String? = null,
    val accompanimentUrl: String? = null,
    val vocalsUrl: String? = null,
    val originalFilename: String? = null
)

// ── New Models for /compare ──

data class CadenzaCompareResponse(
    val success: Boolean = false,
    val overall_score: Int = 0,
    val pitch_accuracy: Int = 0,
    val timing_accuracy: Int = 0,
    val stability: Int = 0,
    val tier: String = "",
    val feedback: List<String> = emptyList(),
    val opening: String = "",
    val encouragement: String = "",
    val pitch_curve: List<PitchTrackPoint> = emptyList(),
    val reference_curve: List<PitchTrackPoint> = emptyList(),
    val message: String? = null
)