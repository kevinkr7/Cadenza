import os
import json
import numpy as np
import librosa
import soundfile as sf

from note_mapper import map_frequency_to_note
from feedback_engine import generate_performance_feedback
from practice_classifier import classify_practice_mode
from audio_quality_checker import analyze_audio_quality

DEFAULT_SAMPLE_RATE = 22050
DEFAULT_HOP_LENGTH = 256

FMIN_NOTE = "C2"
FMAX_NOTE = "C7"


def generate_test_tone(
    file_path: str = "uploads/test_A4.wav",
    frequency: float = 440.0,
    duration: float = 3.0,
    sample_rate: int = DEFAULT_SAMPLE_RATE
) -> str:
    """
    Generates a clean test audio file.

    This is useful because before testing with real singing,
    we should first confirm that the pitch detector works on a known frequency.

    Example:
        440 Hz should be detected as A4.
    """

    folder_name = os.path.dirname(file_path)

    if folder_name:
        os.makedirs(folder_name, exist_ok=True)

    time_values = np.linspace(
        0,
        duration,
        int(sample_rate * duration),
        endpoint=False
    )

    audio = 0.3 * np.sin(2 * np.pi * frequency * time_values)

    fade_length = int(sample_rate * 0.05)

    if fade_length > 0:
        fade_in = np.linspace(0, 1, fade_length)
        fade_out = np.linspace(1, 0, fade_length)

        audio[:fade_length] *= fade_in
        audio[-fade_length:] *= fade_out

    sf.write(file_path, audio, sample_rate)

    return file_path


def load_audio_file(audio_file_path: str):
    """
    Loads an audio file and converts it into mono audio.

    Librosa returns:
        y  -> audio signal as numbers
        sr -> sample rate
    """

    if not os.path.exists(audio_file_path):
        raise FileNotFoundError(f"Audio file not found: {audio_file_path}")

    audio, sample_rate = librosa.load(
        audio_file_path,
        sr=DEFAULT_SAMPLE_RATE,
        mono=True
    )

    return audio, sample_rate


def prepare_audio(audio: np.ndarray, sample_rate: int) -> np.ndarray:
    """
    Cleans and prepares the audio before pitch detection.

    Steps:
    1. Remove silence from beginning and end.
    2. Normalize volume.
    3. Check whether enough audio is available.
    """

    if audio is None or len(audio) == 0:
        raise ValueError("Audio data is empty.")

    trimmed_audio, _ = librosa.effects.trim(
        audio,
        top_db=30
    )

    if len(trimmed_audio) == 0:
        raise ValueError("Audio contains only silence.")

    max_amplitude = np.max(np.abs(trimmed_audio))

    if max_amplitude > 0:
        trimmed_audio = trimmed_audio / max_amplitude

    minimum_duration_seconds = 0.2
    minimum_samples = int(sample_rate * minimum_duration_seconds)

    if len(trimmed_audio) < minimum_samples:
        raise ValueError("Audio is too short for pitch detection.")

    return trimmed_audio


def classify_pitch_stability(stability_cents: float) -> str:
    """
    Classifies how stable the user's pitch is.

    Lower cents variation means better stability.
    """

    if stability_cents <= 15:
        return "stable"

    elif stability_cents <= 35:
        return "moderately stable"

    else:
        return "unstable"


def extract_pitch_track(
    audio: np.ndarray,
    sample_rate: int
):
    """
    Extracts pitch values over time using librosa.pyin().

    pyin returns:
        f0           -> detected pitch values
        voiced_flag  -> whether each frame has voice/pitch
        voiced_probs -> confidence of voiced frames
    """

    fmin = librosa.note_to_hz(FMIN_NOTE)
    fmax = librosa.note_to_hz(FMAX_NOTE)

    f0, voiced_flag, voiced_probs = librosa.pyin(
        audio,
        fmin=fmin,
        fmax=fmax,
        sr=sample_rate,
        hop_length=DEFAULT_HOP_LENGTH
    )

    times = librosa.times_like(
        f0,
        sr=sample_rate,
        hop_length=DEFAULT_HOP_LENGTH
    )

    return f0, times, voiced_flag, voiced_probs


def summarize_pitch(f0_values: np.ndarray):
    """
    Creates summary statistics from detected pitch values.
    """

    voiced_frequencies = f0_values[~np.isnan(f0_values)]

    if len(voiced_frequencies) == 0:
        return None

    average_pitch = float(np.mean(voiced_frequencies))
    median_pitch = float(np.median(voiced_frequencies))
    minimum_pitch = float(np.min(voiced_frequencies))
    maximum_pitch = float(np.max(voiced_frequencies))

    cents_from_median = 1200 * np.log2(voiced_frequencies / median_pitch)
    stability_cents = float(np.std(cents_from_median))

    stability_status = classify_pitch_stability(stability_cents)

    return {
        "average_pitch": round(average_pitch, 2),
        "median_pitch": round(median_pitch, 2),
        "minimum_pitch": round(minimum_pitch, 2),
        "maximum_pitch": round(maximum_pitch, 2),
        "stability_cents": round(stability_cents, 2),
        "stability_status": stability_status,
        "voiced_frame_count": int(len(voiced_frequencies)),
        "total_frame_count": int(len(f0_values)),
        "voiced_ratio": round(len(voiced_frequencies) / len(f0_values), 2)
    }


def create_pitch_track_response(f0_values: np.ndarray, times: np.ndarray):
    """
    Creates a simplified pitch track for graphing.

    This will later be sent to Android so that the app can draw:
        Time vs Pitch
    """

    pitch_track = []

    if len(times) == 0:
        return pitch_track

    step = max(1, len(times) // 100)

    for index in range(0, len(times), step):
        pitch = f0_values[index]

        pitch_track.append({
            "time": round(float(times[index]), 3),
            "frequency": None if np.isnan(pitch) else round(float(pitch), 2)
        })

    return pitch_track


def analyze_audio_pitch(audio_file_path: str) -> dict:
    """
    Main function used by Cadenza.

    This function:
    1. Loads audio
    2. Cleans audio
    3. Detects pitch track
    4. Calculates average/median pitch
    5. Maps frequency to musical note
    6. Generates feedback
    """

    audio, sample_rate = load_audio_file(audio_file_path)
    audio_quality = analyze_audio_quality(
        audio_data=audio,
        sample_rate=sample_rate
    )

    if not audio_quality["is_usable"]:
        return {
            "success": False,
            "audio_file": audio_file_path,
            "message": audio_quality["message"],
            "failure_reason": audio_quality["reason"],
            "audio_quality": audio_quality,
            "pitch_summary": {},
            "note_data": {},
            "feedback": {},
            "practice_mode": {
                "mode": "no_voice_detected",
                "label": "No Clear Voice",
                "confidence": "high",
                "message": audio_quality["message"]
            },
            "pitch_track": []
        }

    prepared_audio = prepare_audio(audio, sample_rate)

    f0_values, times, voiced_flag, voiced_probs = extract_pitch_track(
        prepared_audio,
        sample_rate
    )

    pitch_summary = summarize_pitch(f0_values)

    if pitch_summary is None:
        return {
            "success": False,
            "message": "No clear pitch detected. Try singing a sustained note more clearly.",
            "audio_file": audio_file_path
        }

    note_data = map_frequency_to_note(
        pitch_summary["median_pitch"]
    )

    pitch_track = create_pitch_track_response(
    f0_values,
    times
    )

    practice_mode = classify_practice_mode(
        pitch_summary=pitch_summary,
        pitch_track=pitch_track
    )

    feedback = generate_performance_feedback(
        note_data=note_data,
        pitch_summary=pitch_summary
    )

    duration_seconds = len(prepared_audio) / sample_rate

    return {
    "success": True,
    "audio_file": audio_file_path,
    "duration_seconds": round(duration_seconds, 2),
    "pitch_summary": pitch_summary,
    "note_data": note_data,
    "feedback": feedback,
    "practice_mode": practice_mode,
    "pitch_track": pitch_track
    }


if __name__ == "__main__":
    test_file = "uploads/test_A4.wav"

    print("Generating test audio...")
    generate_test_tone(
        file_path=test_file,
        frequency=440.0,
        duration=3.0
    )

    print("Analyzing pitch...")
    result = analyze_audio_pitch(test_file)

    compact_result = result.copy()

    if "pitch_track" in compact_result:
        compact_result["pitch_track"] = compact_result["pitch_track"][:10]

    print(json.dumps(compact_result, indent=4))