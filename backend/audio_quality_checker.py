import numpy as np
import librosa


def analyze_audio_quality(audio_data, sample_rate: int) -> dict:
    """
    Checks whether the uploaded audio contains enough usable voice signal.

    This is used before pitch detection.

    It catches cases like:
    1. User did not sing
    2. Mic did not capture voice clearly
    3. Audio is almost silent
    4. Recording contains mostly uniform noise
    """

    if audio_data is None or len(audio_data) == 0:
        return {
            "is_usable": False,
            "reason": "empty_audio",
            "message": "Please sing something. The recording seems to be empty.",
            "average_rms": 0.0,
            "peak_amplitude": 0.0,
            "active_frame_ratio": 0.0,
            "spectral_flatness": 1.0
        }

    audio_data = np.asarray(audio_data, dtype=np.float32)

    # Remove NaN or infinite values if any
    audio_data = np.nan_to_num(audio_data)

    duration_seconds = len(audio_data) / sample_rate

    if duration_seconds < 0.5:
        return {
            "is_usable": False,
            "reason": "too_short",
            "message": "Please sing for a little longer so I can analyze your voice.",
            "average_rms": 0.0,
            "peak_amplitude": 0.0,
            "active_frame_ratio": 0.0,
            "spectral_flatness": 1.0
        }

    peak_amplitude = float(np.max(np.abs(audio_data)))

    rms_values = librosa.feature.rms(
        y=audio_data,
        frame_length=2048,
        hop_length=512
    )[0]

    average_rms = float(np.mean(rms_values))
    max_rms = float(np.max(rms_values))

    # How many frames contain meaningful sound?
    active_threshold = 0.01
    active_frames = rms_values > active_threshold
    active_frame_ratio = float(np.mean(active_frames))

    spectral_flatness_values = librosa.feature.spectral_flatness(
        y=audio_data
    )[0]

    spectral_flatness = float(np.mean(spectral_flatness_values))

    # Case 1: Almost complete silence
    if peak_amplitude < 0.008 or average_rms < 0.003:
        return {
            "is_usable": False,
            "reason": "too_quiet",
            "message": "Please sing something. I could not hear a clear voice.",
            "average_rms": round(average_rms, 6),
            "peak_amplitude": round(peak_amplitude, 6),
            "active_frame_ratio": round(active_frame_ratio, 3),
            "spectral_flatness": round(spectral_flatness, 3)
        }

    # Case 2: Very little active audio
    if active_frame_ratio < 0.08:
        return {
            "is_usable": False,
            "reason": "not_enough_voice_activity",
            "message": "Please sing something clearly near the microphone.",
            "average_rms": round(average_rms, 6),
            "peak_amplitude": round(peak_amplitude, 6),
            "active_frame_ratio": round(active_frame_ratio, 3),
            "spectral_flatness": round(spectral_flatness, 3)
        }

    # Case 3: Noise-like sound, not vocal-like
    # High spectral flatness usually means hiss/noise rather than a tonal voice.
    if spectral_flatness > 0.55 and average_rms < 0.02:
        return {
            "is_usable": False,
            "reason": "mostly_noise",
            "message": "Please sing something. The recording mostly sounds like noise, not a clear voice.",
            "average_rms": round(average_rms, 6),
            "peak_amplitude": round(peak_amplitude, 6),
            "active_frame_ratio": round(active_frame_ratio, 3),
            "spectral_flatness": round(spectral_flatness, 3)
        }

    return {
        "is_usable": True,
        "reason": "usable_audio",
        "message": "Audio contains enough signal for analysis.",
        "average_rms": round(average_rms, 6),
        "max_rms": round(max_rms, 6),
        "peak_amplitude": round(peak_amplitude, 6),
        "active_frame_ratio": round(active_frame_ratio, 3),
        "spectral_flatness": round(spectral_flatness, 3)
    }