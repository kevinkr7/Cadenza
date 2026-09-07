"""
audio_preprocessor.py

Validates and normalizes audio files before any processing.

Both the reference track and the user recording must pass through
this stage to guarantee every downstream algorithm receives
predictable, consistent input.

Normalization target:
    Sample rate : 22050 Hz
    Channels    : Mono
    dtype       : float32
    Amplitude   : peak-normalized to [-1, 1]
"""

import os
import numpy as np
import librosa
import soundfile as sf


# ── Constants ──────────────────────────────────────────────────────────────

TARGET_SR        = 22050
MIN_DURATION_SEC = 0.5
MAX_DURATION_SEC = 600.0   # 10 minutes — sanity cap
CLIPPING_THRESHOLD = 0.98  # amplitude above this is considered clipped


# ── Helpers ────────────────────────────────────────────────────────────────

def _check_file_exists(file_path: str) -> None:
    if not os.path.isfile(file_path):
        raise FileNotFoundError(f"Audio file not found: {file_path}")


def _check_extension(file_path: str) -> None:
    allowed = {".wav", ".mp3", ".m4a", ".flac", ".ogg", ".aac"}
    ext = os.path.splitext(file_path)[1].lower()
    if ext not in allowed:
        raise ValueError(
            f"Unsupported file format '{ext}'. "
            f"Allowed formats: {sorted(allowed)}"
        )


def _detect_clipping(audio: np.ndarray, threshold: float = CLIPPING_THRESHOLD) -> bool:
    """Returns True if more than 0.1 % of samples are clipped."""
    clipped_samples = np.sum(np.abs(audio) >= threshold)
    clipped_ratio   = clipped_samples / max(len(audio), 1)
    return clipped_ratio > 0.001


def _is_silent(audio: np.ndarray) -> bool:
    """Returns True if the peak amplitude is below a usable threshold."""
    return float(np.max(np.abs(audio))) < 0.003


# ── Core function ──────────────────────────────────────────────────────────

def load_and_validate(file_path: str, label: str = "audio") -> dict:
    """
    Load, validate, and normalize a single audio file.

    Parameters
    ----------
    file_path : str
        Absolute or relative path to the audio file.
    label : str
        Human-readable label used in error messages
        (e.g. "reference" or "user_recording").

    Returns
    -------
    dict with keys:
        audio       np.ndarray  — float32 mono waveform
        sample_rate int         — always TARGET_SR
        duration    float       — duration in seconds
        warnings    list[str]   — non-fatal quality notes
        was_clipped bool
        was_silent  bool (before normalization)
    """

    # ── 1. File-level checks ──────────────────────────────────────────
    _check_file_exists(file_path)
    _check_extension(file_path)

    # ── 2. Load ───────────────────────────────────────────────────────
    try:
        audio, sr = librosa.load(file_path, sr=TARGET_SR, mono=True, dtype=np.float32)
    except Exception as exc:
        raise RuntimeError(
            f"Could not decode {label} file '{file_path}': {exc}"
        ) from exc

    warnings = []

    # ── 3. Duration checks ────────────────────────────────────────────
    duration = len(audio) / TARGET_SR

    if duration < MIN_DURATION_SEC:
        raise ValueError(
            f"{label.capitalize()} audio is too short "
            f"({duration:.2f}s). Minimum is {MIN_DURATION_SEC}s."
        )

    if duration > MAX_DURATION_SEC:
        raise ValueError(
            f"{label.capitalize()} audio is too long "
            f"({duration:.0f}s). Maximum is {MAX_DURATION_SEC}s."
        )

    # ── 4. Quality checks ─────────────────────────────────────────────
    was_silent  = _is_silent(audio)
    was_clipped = _detect_clipping(audio)

    if was_silent:
        warnings.append(f"{label.capitalize()} audio appears to be silent.")

    if was_clipped:
        warnings.append(
            f"{label.capitalize()} audio contains clipped samples. "
            "Consider re-recording at a lower input gain."
        )

    # ── 5. Normalize amplitude ────────────────────────────────────────
    peak = float(np.max(np.abs(audio)))
    if peak > 0:
        audio = audio / peak

    return {
        "audio":       audio,
        "sample_rate": TARGET_SR,
        "duration":    round(duration, 3),
        "warnings":    warnings,
        "was_clipped": was_clipped,
        "was_silent":  was_silent,
    }


def validate_pair(
    reference_path: str,
    user_path: str,
) -> tuple[dict, dict]:
    """
    Convenience wrapper that validates both files and returns
    a (reference_data, user_data) tuple.

    Raises immediately on any hard error.
    """
    reference_data = load_and_validate(reference_path, label="reference")
    user_data      = load_and_validate(user_path,      label="user_recording")
    return reference_data, user_data


# ── CLI smoke-test ─────────────────────────────────────────────────────────

if __name__ == "__main__":
    import soundfile as sf, json, sys

    # Create a tiny test WAV and validate it.
    test_path = "uploads/_test_preprocess.wav"
    os.makedirs("uploads", exist_ok=True)

    t   = np.linspace(0, 2, int(TARGET_SR * 2), endpoint=False)
    sig = (0.4 * np.sin(2 * np.pi * 440 * t)).astype(np.float32)
    sf.write(test_path, sig, TARGET_SR)

    result = load_and_validate(test_path, label="test_tone")

    # Don't print the full waveform — just the metadata.
    summary = {k: v for k, v in result.items() if k != "audio"}
    summary["audio_shape"] = result["audio"].shape
    print(json.dumps(summary, indent=2))
