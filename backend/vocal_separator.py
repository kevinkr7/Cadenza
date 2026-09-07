"""
vocal_separator.py

Separates the vocal stem from a full reference track using Demucs.

Demucs is a state-of-the-art music source separation model from Meta Research.
We use the 2-stems variant which produces:
    vocals      — the isolated vocal track
    no_vocals   — the instrumental backing (accompaniment)

Usage
-----
    from vocal_separator import extract_vocals

    vocal_audio, accomp_audio, sr, meta = extract_vocals("path/to/song.wav")
"""

import os
import tempfile
import warnings
import subprocess
import sys
import numpy as np
import librosa

# ── Constants ──────────────────────────────────────────────────────────────

TARGET_SR = 22050


# ── Core separation ────────────────────────────────────────────────────────

def extract_vocals(
    audio_path: str,
    output_dir: str | None = None,
) -> tuple[np.ndarray, np.ndarray, int, dict]:
    """
    Extract the vocal stem and accompaniment from a music file using Demucs.

    Parameters
    ----------
    audio_path : str
        Path to the reference track.
    output_dir : str | None
        Directory to write separated stems. Uses a temp dir if None.

    Returns
    -------
    tuple:
        vocal_audio   : np.ndarray   float32 mono waveform at TARGET_SR
        accomp_audio  : np.ndarray   float32 mono waveform at TARGET_SR
        sample_rate   : int          always TARGET_SR
        meta          : dict         separation metadata / warnings
    """

    use_temp = output_dir is None
    if use_temp:
        output_dir = tempfile.mkdtemp(prefix="cadenza_demucs_")

    try:
        # Run Demucs CLI
        # --two-stems=vocals will output vocals.wav and no_vocals.wav
        cmd = [
            sys.executable, "-m", "demucs",
            "--two-stems=vocals",
            "-o", output_dir,
            audio_path
        ]
        
        result = subprocess.run(cmd, capture_output=True, text=True)
        
        if result.returncode != 0:
            raise RuntimeError(f"Demucs process failed: {result.stderr}")

        # Demucs outputs to: output_dir/htdemucs/{base_name}/
        base_name = os.path.splitext(os.path.basename(audio_path))[0]
        # Sometimes demucs uses 'htdemucs' or 'htdemucs_ft' depending on the default model.
        # It's usually 'htdemucs' unless specified otherwise.
        model_name = "htdemucs"
        out_folder = os.path.join(output_dir, model_name, base_name)
        
        if not os.path.exists(out_folder):
            # Fallback if model folder is different
            folders = [f for f in os.listdir(output_dir) if os.path.isdir(os.path.join(output_dir, f))]
            if folders:
                out_folder = os.path.join(output_dir, folders[0], base_name)

        vocal_path = os.path.join(out_folder, "vocals.wav")
        accomp_path = os.path.join(out_folder, "no_vocals.wav")
        
        if not os.path.exists(vocal_path) or not os.path.exists(accomp_path):
            raise FileNotFoundError(f"Separated files not found in {out_folder}")

        # Load separated files
        vocal_mono, _ = librosa.load(vocal_path, sr=TARGET_SR, mono=True, dtype=np.float32)
        accomp_mono, _ = librosa.load(accomp_path, sr=TARGET_SR, mono=True, dtype=np.float32)

        # Normalize.
        v_peak = float(np.max(np.abs(vocal_mono))) if len(vocal_mono) > 0 else 0
        if v_peak > 0:
            vocal_mono = vocal_mono / v_peak
            
        a_peak = float(np.max(np.abs(accomp_mono))) if len(accomp_mono) > 0 else 0
        if a_peak > 0:
            accomp_mono = accomp_mono / a_peak

        return vocal_mono, accomp_mono, TARGET_SR, {
            "method":   "demucs:two-stems",
            "success":  True,
            "warnings": [],
        }

    except Exception as exc:
        # Demucs failed at runtime — fall back to full audio.
        fallback_vocal, fallback_accomp, fallback_sr, fallback_meta = _fallback_passthrough(audio_path)
        fallback_meta["warnings"].append(
            f"Demucs separation failed ({exc}). "
            "Using unseparated audio. Pitch comparison may be less accurate."
        )
        return fallback_vocal, fallback_accomp, fallback_sr, fallback_meta

    finally:
        if use_temp:
            # Clean up temporary files.
            import shutil
            shutil.rmtree(output_dir, ignore_errors=True)


# ── Fallback ───────────────────────────────────────────────────────────────

def _fallback_passthrough(audio_path: str) -> tuple[np.ndarray, np.ndarray, int, dict]:
    """
    When Demucs is unavailable or fails, load the raw audio
    and pass it through unchanged for both vocals and accompaniment.
    """
    audio, _ = librosa.load(audio_path, sr=TARGET_SR, mono=True, dtype=np.float32)

    peak = float(np.max(np.abs(audio))) if len(audio) > 0 else 0
    if peak > 0:
        audio = audio / peak

    return audio, audio, TARGET_SR, {
        "method":   "passthrough",
        "success":  False,
        "warnings": [
            "Demucs failed or is not installed. Using full-mix audio."
        ],
    }


# ── CLI smoke-test ─────────────────────────────────────────────────────────

if __name__ == "__main__":
    import json, soundfile as sf

    # Build a test WAV (pure sine = trivial case; no separation needed).
    test_path = "uploads/_test_separation.wav"
    os.makedirs("uploads", exist_ok=True)

    t   = np.linspace(0, 3, int(TARGET_SR * 3), endpoint=False)
    sig = (0.4 * np.sin(2 * np.pi * 440 * t)).astype(np.float32)
    sf.write(test_path, sig, TARGET_SR)

    vocal, accomp, sr, meta = extract_vocals(test_path)

    print(json.dumps({
        "vocal_shape": list(vocal.shape),
        "accomp_shape": list(accomp.shape),
        "sample_rate": sr,
        "meta":        meta,
    }, indent=2))
