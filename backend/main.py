import os
import uuid
import shutil
from fastapi import FastAPI, UploadFile, File, HTTPException, Form
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
import soundfile as sf
from pitch_detector import analyze_audio_pitch

# ── New pipeline modules ───────────────────────────────────────────────────
from audio_preprocessor import validate_pair
from vocal_separator     import extract_vocals
from reference_builder   import build_reference_template
from vad                 import detect_voiced_segments
from dtw_aligner         import align_pitch_curves, build_aligned_pairs
from performance_analyzer import analyze_performance
from score_engine        import compute_score
from comparison_feedback  import generate_comparison_feedback
import librosa
import numpy as np


app = FastAPI(
    title="Cadenza Vocal Analysis Backend",
    description="Backend API for pitch detection, note mapping, and vocal feedback generation.",
    version="2.0.0"
)


app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


UPLOAD_FOLDER = "uploads"

os.makedirs(UPLOAD_FOLDER, exist_ok=True)
app.mount("/uploads", StaticFiles(directory=UPLOAD_FOLDER), name="uploads")


def save_uploaded_audio(file: UploadFile) -> tuple[str, str, str]:
    """
    Saves the uploaded audio file into the uploads folder.

    Returns:
        original_filename
        stored_filename
        saved_file_path
    """

    if file.filename is None or file.filename.strip() == "":
        raise HTTPException(
            status_code=400,
            detail="No audio file was uploaded."
        )

    allowed_extensions = [".wav", ".mp3", ".m4a", ".flac", ".ogg"]

    original_filename = file.filename
    file_extension = os.path.splitext(original_filename)[1].lower()

    if file_extension not in allowed_extensions:
        raise HTTPException(
            status_code=400,
            detail=f"Unsupported file format '{file_extension}'. Please upload one of: {allowed_extensions}"
        )

    stored_filename = f"{uuid.uuid4()}{file_extension}"
    saved_file_path = os.path.join(UPLOAD_FOLDER, stored_filename)

    try:
        with open(saved_file_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)

    except Exception as error:
        raise HTTPException(
            status_code=500,
            detail=f"Failed to save uploaded audio file: {str(error)}"
        )

    finally:
        file.file.close()

    return original_filename, stored_filename, saved_file_path


def build_mobile_response(analysis_result: dict) -> dict:
    """
    Converts the detailed backend analysis result into a clean Android-friendly response.

    This function protects the Android app from needing to understand
    the full internal backend structure.
    """

    if not analysis_result.get("success", False):
        message = analysis_result.get(
            "message",
            "Please sing something. I could not hear a clear voice."
        )

        practice_mode = analysis_result.get("practice_mode", {})
        audio_quality = analysis_result.get("audio_quality", {})

        return {
            "success": False,
            "message": message,

            "score": 0,

            "note": None,
            "frequency": None,
            "idealFrequency": None,
            "deviationCents": None,
            "pitchStatus": "unknown",

            "stability": "unknown",
            "stabilityCents": None,

            "averagePitch": None,
            "medianPitch": None,
            "minimumPitch": None,
            "maximumPitch": None,

            "durationSeconds": analysis_result.get("duration_seconds"),

            "practiceMode": practice_mode.get("mode", "no_voice_detected"),
            "practiceModeLabel": practice_mode.get("label", "No Clear Voice"),
            "practiceModeConfidence": practice_mode.get("confidence", "high"),
            "practiceModeMessage": practice_mode.get("message", message),

            "pitchRangeCents": None,
            "distinctNoteCount": 0,
            "noteChangeCount": 0,

            "coachMessage": message,
            "advice": "Move closer to the microphone and sing one clear sustained 'Aaah' sound.",

            "audioQuality": audio_quality,

            "pitchTrack": []
        }

    pitch_summary = analysis_result.get("pitch_summary", {})
    note_data = analysis_result.get("note_data", {})
    feedback = analysis_result.get("feedback", {})
    practice_mode = analysis_result.get("practice_mode", {})
    pitch_track = analysis_result.get("pitch_track", [])

    summary = feedback.get("summary", "")
    stability_message = feedback.get("stability_message", "")
    encouragement = feedback.get("encouragement", "")
    advice = feedback.get("advice", "")
    practice_mode_message = practice_mode.get("message", "")

    coach_message_parts = []

    if summary:
        coach_message_parts.append(summary)

    if stability_message:
        coach_message_parts.append(stability_message)

    if encouragement:
        coach_message_parts.append(encouragement)

    if practice_mode_message:
        coach_message_parts.append(practice_mode_message)

    coach_message = " ".join(coach_message_parts).strip()

    if not coach_message:
        coach_message = "Good attempt. Keep practicing with steady breath support."

    return {
        "success": True,

        "score": feedback.get("accuracy_score", 0),

        "note": note_data.get("note"),
        "frequency": note_data.get("frequency"),
        "idealFrequency": note_data.get("ideal_frequency"),
        "deviationCents": note_data.get("deviation_cents"),
        "pitchStatus": note_data.get("status"),

        "stability": pitch_summary.get("stability_status"),
        "stabilityCents": pitch_summary.get("stability_cents"),

        "averagePitch": pitch_summary.get("average_pitch"),
        "medianPitch": pitch_summary.get("median_pitch"),
        "minimumPitch": pitch_summary.get("minimum_pitch"),
        "maximumPitch": pitch_summary.get("maximum_pitch"),

        "durationSeconds": analysis_result.get("duration_seconds"),

        "practiceMode": practice_mode.get("mode"),
        "practiceModeLabel": practice_mode.get("label"),
        "practiceModeConfidence": practice_mode.get("confidence"),
        "practiceModeMessage": practice_mode.get("message"),
        "pitchRangeCents": practice_mode.get("pitch_range_cents"),
        "distinctNoteCount": practice_mode.get("distinct_note_count"),
        "noteChangeCount": practice_mode.get("note_change_count"),

        "coachMessage": coach_message,
        "advice": advice,

        "pitchTrack": pitch_track
    }


@app.get("/")
def home():
    """
    Basic health-check endpoint.
    """

    return {
        "message": "Cadenza backend is running",
        "status": "healthy"
    }


@app.get("/health")
def health_check():
    """
    Health-check endpoint for testing server availability.
    """

    return {
        "success": True,
        "service": "Cadenza Vocal Analysis Backend",
        "status": "online"
    }


@app.post("/analyze")
async def analyze_audio(file: UploadFile = File(...)):
    """
    Detailed analysis endpoint.

    This endpoint is useful for backend debugging because it returns
    the complete internal analysis result.
    """

    original_filename, stored_filename, saved_file_path = save_uploaded_audio(file)

    try:
        analysis_result = analyze_audio_pitch(saved_file_path)

        analysis_result["uploaded_filename"] = original_filename
        analysis_result["stored_filename"] = stored_filename

        return analysis_result

    except Exception as error:
        raise HTTPException(
            status_code=500,
            detail=f"Audio analysis failed: {str(error)}"
        )


@app.post("/analyze-mobile")
async def analyze_audio_for_mobile(file: UploadFile = File(...)):
    """
    Android-friendly analysis endpoint.

    This endpoint returns a simplified response that can be displayed
    easily inside the Cadenza mobile app.
    """

    original_filename, stored_filename, saved_file_path = save_uploaded_audio(file)

    try:
        analysis_result = analyze_audio_pitch(saved_file_path)

        mobile_response = build_mobile_response(analysis_result)

        mobile_response["uploadedFilename"] = original_filename
        mobile_response["storedFilename"] = stored_filename

        return mobile_response

    except Exception as error:
        raise HTTPException(
            status_code=500,
            detail=f"Mobile audio analysis failed: {str(error)}"
        )


# ── Full MVP Pipeline: Reference vs User comparison ────────────────────────

@app.post("/upload-reference")
async def upload_reference(
    reference: UploadFile = File(..., description="Reference track (song or isolated vocal)")
):
    """
    Step 1: Upload a reference song.
    Separates the vocals from the accompaniment and saves both.
    Returns the session ID and the URL to the accompaniment track.
    """
    ref_orig, ref_stored, ref_path = save_uploaded_audio(reference)
    session_id = str(uuid.uuid4())
    
    try:
        ref_vocal, ref_accomp, ref_sr, sep_meta = extract_vocals(ref_path)
        
        vocals_path = os.path.join(UPLOAD_FOLDER, f"{session_id}_vocals.wav")
        accomp_path = os.path.join(UPLOAD_FOLDER, f"{session_id}_accomp.wav")
        
        sf.write(vocals_path, ref_vocal, ref_sr)
        sf.write(accomp_path, ref_accomp, ref_sr)
        
        return {
            "success": True,
            "sessionId": session_id,
            "accompanimentUrl": f"/uploads/{session_id}_accomp.wav",
            "vocalsUrl": f"/uploads/{session_id}_vocals.wav",
            "originalFilename": ref_orig
        }
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Failed to process reference audio: {str(e)}"
        )
    finally:
        if os.path.isfile(ref_path):
            os.remove(ref_path)


@app.post("/compare")
async def compare_vocals(
    session_id:      str = Form(..., description="Session ID from /upload-reference"),
    user_recording:  UploadFile = File(..., description="User's recorded vocal performance"),
):
    """
    Step 2: Compare user recording to the separated reference vocals.
    """
    user_orig, user_stored, user_path = save_uploaded_audio(user_recording)
    
    vocals_path = os.path.join(UPLOAD_FOLDER, f"{session_id}_vocals.wav")
    if not os.path.isfile(vocals_path):
        raise HTTPException(status_code=404, detail="Session not found or expired.")

    try:
        # Load the separated reference vocal from disk.
        ref_vocal, ref_sr = librosa.load(vocals_path, sr=22050, mono=True)

        # ── 2. Validate & normalise user recording ────────────────────────
        # Since we already separated the reference, we just validate the user file
        # We can use a dummy for the reference in validate_pair, or just validate user directly.
        # But for simplicity, we pass vocals_path as if it's the reference.
        try:
            ref_data, user_data = validate_pair(vocals_path, user_path)
        except (FileNotFoundError, ValueError, RuntimeError) as ve:
            raise HTTPException(status_code=422, detail=str(ve))

        warnings = ref_data["warnings"] + user_data["warnings"]

        # User recording is already a pure vocal — no separation needed.
        user_audio = user_data["audio"]

        # ── Check Minimum Duration ────────────────────────────────────
        if user_data["duration"] < 30.0:
            raise HTTPException(
                status_code=400,
                detail="Please sing for at least 30 seconds to get an accurate analysis."
            )

        # ── 3. Align lengths (Efficient Snipping) ─────────────────────
        # Snip the original reference audio based on the length of the recorded audio exactly
        ref_vocal = ref_vocal[:len(user_audio)]

        # ── 4. Extract pitch tracks (pyin) ────────────────────────────
        HOP     = 512
        FMIN_HZ = librosa.note_to_hz("C2")
        FMAX_HZ = librosa.note_to_hz("C7")

        ref_f0, ref_voiced_flag, ref_voiced_probs = librosa.pyin(
            ref_vocal, fmin=FMIN_HZ, fmax=FMAX_HZ, sr=ref_sr, hop_length=HOP
        )
        ref_times = librosa.times_like(ref_f0, sr=ref_sr, hop_length=HOP)

        user_f0, user_voiced_flag, user_voiced_probs = librosa.pyin(
            user_audio, fmin=FMIN_HZ, fmax=FMAX_HZ, sr=ref_sr, hop_length=HOP
        )
        user_times = librosa.times_like(user_f0, sr=ref_sr, hop_length=HOP)

        # ── 5. Build reference template ───────────────────────────────
        ref_template = build_reference_template(
            vocal_audio=ref_vocal,
            sample_rate=ref_sr,
            song_name="Reference Track",
        )

        # ── 6. DTW Alignment ──────────────────────────────────────────
        # Restrict alignment to a 10-second window to massively reduce calculation time
        radius = int(10.0 * ref_sr / HOP)
        dtw_path, dtw_distance = align_pitch_curves(ref_f0, user_f0, sakoe_chiba_radius=radius)

        aligned_pairs = build_aligned_pairs(
            ref_f0, ref_times, user_f0, user_times, dtw_path
        )

        # ── 7. Performance analysis ───────────────────────────────────
        performance = analyze_performance(
            ref_f0        = ref_f0,
            ref_times     = ref_times,
            ref_segments  = ref_template["segments"],
            user_f0       = user_f0,
            user_times    = user_times,
            aligned_pairs = aligned_pairs,
            dtw_path      = dtw_path,
        )

        # ── 8. Score ──────────────────────────────────────────────────
        scores = compute_score(performance)

        # ── 9. Feedback ───────────────────────────────────────────────
        feedback = generate_comparison_feedback(scores, performance)

        # ── 10. Build pitch curves for Android visualization ──────────
        def _curve(f0_arr, times_arr):
            step = max(1, len(times_arr) // 200)
            return [
                {
                    "time":      round(float(times_arr[i]), 4),
                    "frequency": None if np.isnan(f0_arr[i]) else round(float(f0_arr[i]), 3),
                }
                for i in range(0, len(times_arr), step)
            ]

        # ── 11. Assemble response ─────────────────────────────────────
        return {
            "success":        True,

            # Core scores
            "overall_score":   scores["overall_score"],
            "pitch_accuracy":  scores["pitch_accuracy"],
            "timing_accuracy": scores["timing_accuracy"],
            "stability":       scores["stability"],
            "tier":            scores["tier"],

            # Vocal range
            "vocal_range":     performance["vocal_range"],

            # Natural-language feedback (what Android displays)
            "feedback":        feedback["feedback_lines"],
            "opening":         feedback["opening"],
            "encouragement":   feedback["encouragement"],
            "pitch_advice":    feedback["pitch_advice"],
            "timing_advice":   feedback["timing_advice"],
            "stability_advice": feedback["stability_advice"],

            # Visualization data
            "pitch_curve":     _curve(user_f0, user_times),
            "reference_curve": _curve(ref_f0,  ref_times),

            # Debugging / detail
            "component_scores":     scores["component_scores"],
            "dtw_distance":         round(dtw_distance, 4),
            "separation_method":    "pre_separated",
            "reference_template":   {
                "song":     ref_template["song"],
                "duration": ref_template["duration"],
                "meta":     ref_template["meta"],
                "segments": [
                    {k: v for k, v in seg.items() if k != "pitch_curve"}
                    for seg in ref_template["segments"]
                ],
            },
            "reference_duration":   ref_data["duration"],
            "user_duration":        user_data["duration"],
            "warnings":             warnings,

            # File identifiers
            # File identifiers
            "reference_file":       f"{session_id}_vocals",
            "user_recording_file":  user_orig,
        }

    except HTTPException:
        raise

    except Exception as error:
        raise HTTPException(
            status_code=500,
            detail=f"Comparison pipeline failed: {str(error)}"
        )

    finally:
        # Clean up user uploaded file. Reference vocals are kept for the session.
        try:
            if os.path.isfile(user_path):
                os.remove(user_path)
        except OSError:
            pass



