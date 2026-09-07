"""
test_pipeline.py

End-to-end smoke test for the Cadenza MVP comparison pipeline.

This script:
    1. Generates a synthetic "reference" tone (A4 = 440 Hz, 3 seconds)
    2. Generates a synthetic "user" tone (slightly flat: 430 Hz, 3 seconds)
    3. Runs the full comparison pipeline without the HTTP layer
    4. Prints the JSON result

Run from the backend/ directory:
    python test_pipeline.py

You do not need a running server for this test.
All processing happens in-process so you can validate the math directly.
"""

import os
import json
import numpy as np
import soundfile as sf

# ── Pipeline imports ───────────────────────────────────────────────────────
from audio_preprocessor  import load_and_validate
from vocal_separator     import _fallback_passthrough       # use passthrough for tests
from reference_builder   import build_reference_template
from vad                 import detect_voiced_segments
from dtw_aligner         import align_pitch_curves, build_aligned_pairs
from performance_analyzer import analyze_performance
from score_engine        import compute_score
from comparison_feedback  import generate_comparison_feedback
import librosa


# ── Helpers ────────────────────────────────────────────────────────────────

TARGET_SR = 22050

def _make_tone(frequency: float, duration: float, sr: int = TARGET_SR) -> np.ndarray:
    """Generate a clean sine-wave tone with short fade-in/out."""
    t     = np.linspace(0, duration, int(sr * duration), endpoint=False)
    audio = 0.4 * np.sin(2 * np.pi * frequency * t).astype(np.float32)

    fade = int(sr * 0.05)
    if fade > 0:
        audio[:fade]  *= np.linspace(0, 1, fade)
        audio[-fade:] *= np.linspace(1, 0, fade)

    return audio


def _make_two_note_sequence(
    note_a_hz: float,
    note_b_hz: float,
    dur_each: float = 2.0,
    gap: float = 0.3,
    sr: int = TARGET_SR,
) -> np.ndarray:
    """Two tones separated by a short silence — simulates a small phrase."""
    tone_a  = _make_tone(note_a_hz, dur_each, sr)
    silence = np.zeros(int(sr * gap), dtype=np.float32)
    tone_b  = _make_tone(note_b_hz, dur_each, sr)
    return np.concatenate([tone_a, silence, tone_b])


# ── Test ───────────────────────────────────────────────────────────────────

def run_test():
    os.makedirs("uploads", exist_ok=True)

    print("=" * 60)
    print("Cadenza MVP Pipeline — Synthetic Smoke Test")
    print("=" * 60)

    # ── 1. Generate audio ─────────────────────────────────────────────
    print("\n[1/9] Generating synthetic audio ...")

    # Reference: A4 (440 Hz) → B4 (493.88 Hz)
    ref_audio = _make_two_note_sequence(440.0, 493.88, dur_each=2.0)

    # User: slightly flat on both notes (~40 cents flat each)
    user_audio = _make_two_note_sequence(430.0, 481.0, dur_each=2.0)

    ref_path  = "uploads/_test_reference.wav"
    user_path = "uploads/_test_user.wav"

    sf.write(ref_path,  ref_audio,  TARGET_SR)
    sf.write(user_path, user_audio, TARGET_SR)

    print(f"   Reference : {ref_path}  ({len(ref_audio)/TARGET_SR:.2f}s)")
    print(f"   User      : {user_path} ({len(user_audio)/TARGET_SR:.2f}s)")

    # ── 2. Validate ───────────────────────────────────────────────────
    print("\n[2/9] Validating audio files ...")
    ref_data  = load_and_validate(ref_path,  label="reference")
    user_data = load_and_validate(user_path, label="user_recording")

    print(f"   Reference : OK — {ref_data['duration']}s, "
          f"warnings={ref_data['warnings']}")
    print(f"   User      : OK — {user_data['duration']}s, "
          f"warnings={user_data['warnings']}")

    # ── 3. Vocal separation (passthrough for synthetic tones) ─────────
    print("\n[3/9] Vocal separation (passthrough — no Spleeter for test tones) ...")
    ref_vocal = ref_data["audio"]   # pure sine — no separation needed
    print("   Skipped Spleeter for synthetic tone (passthrough).")

    # ── 4. Pitch extraction ───────────────────────────────────────────
    print("\n[4/9] Extracting pitch tracks (pyin) ...")

    HOP     = 256
    FMIN_HZ = librosa.note_to_hz("C2")
    FMAX_HZ = librosa.note_to_hz("C7")

    ref_f0, _, _ = librosa.pyin(ref_vocal,          fmin=FMIN_HZ, fmax=FMAX_HZ, sr=TARGET_SR, hop_length=HOP)
    ref_times     = librosa.times_like(ref_f0, sr=TARGET_SR, hop_length=HOP)

    user_f0, _, _ = librosa.pyin(user_data["audio"], fmin=FMIN_HZ, fmax=FMAX_HZ, sr=TARGET_SR, hop_length=HOP)
    user_times     = librosa.times_like(user_f0, sr=TARGET_SR, hop_length=HOP)

    voiced_ref  = int(np.sum(~np.isnan(ref_f0)))
    voiced_user = int(np.sum(~np.isnan(user_f0)))

    print(f"   Reference : {len(ref_f0)} frames, {voiced_ref} voiced")
    print(f"   User      : {len(user_f0)} frames, {voiced_user} voiced")

    # ── 5. Reference template ─────────────────────────────────────────
    print("\n[5/9] Building reference vocal template ...")
    ref_template = build_reference_template(ref_vocal, sample_rate=TARGET_SR, song_name="test_A4_B4")

    print(f"   Segments  : {len(ref_template['segments'])}")
    for seg in ref_template["segments"]:
        print(f"   -> {seg['note']} ({seg['start']:.2f}s-{seg['end']:.2f}s) "
              f"confidence={seg['confidence']}")

    # ── 6. DTW Alignment ──────────────────────────────────────────────
    print("\n[6/9] DTW alignment ...")
    dtw_path, dtw_dist = align_pitch_curves(ref_f0, user_f0)
    aligned_pairs = build_aligned_pairs(ref_f0, ref_times, user_f0, user_times, dtw_path)

    print(f"   DTW distance : {dtw_dist:.4f}")
    print(f"   Path length  : {len(dtw_path)} pairs")

    # ── 7. Performance analysis ───────────────────────────────────────
    print("\n[7/9] Analysing performance ...")
    performance = analyze_performance(
        ref_f0        = ref_f0,
        ref_times     = ref_times,
        ref_segments  = ref_template["segments"],
        user_f0       = user_f0,
        user_times    = user_times,
        aligned_pairs = aligned_pairs,
        dtw_path      = dtw_path,
    )

    pa = performance["pitch_accuracy"]
    ta = performance["timing_accuracy"]
    st = performance["stability"]
    vr = performance["vocal_range"]

    print(f"   Pitch accuracy  : {pa['accuracy_score']} "
          f"(MAE={pa.get('mae_cents')} cents, dir={pa.get('pitch_direction')})")
    print(f"   Timing accuracy : {ta['timing_score']} "
          f"(mean onset err={ta.get('mean_onset_error_ms')} ms)")
    print(f"   Stability       : {st['stability_score']} "
          f"(mean={st.get('mean_stability_cents')} cents)")
    print(f"   Vocal range     : {vr.get('lowest')} to {vr.get('highest')} "
          f"({vr.get('range_semitones')} semitones)")

    # ── 8. Score ──────────────────────────────────────────────────────
    print("\n[8/9] Computing score ...")
    scores = compute_score(performance)

    print(f"   Overall score : {scores['overall_score']} / 100  [{scores['tier']}]")
    print(f"   Pitch  : {scores['pitch_accuracy']} × 0.60 = "
          f"{scores['component_scores']['pitch_accuracy']['contribution']}")
    print(f"   Timing : {scores['timing_accuracy']} × 0.25 = "
          f"{scores['component_scores']['timing_accuracy']['contribution']}")
    print(f"   Stable : {scores['stability']} × 0.15 = "
          f"{scores['component_scores']['stability']['contribution']}")

    # ── 9. Feedback ───────────────────────────────────────────────────
    print("\n[9/9] Generating coaching feedback ...")
    feedback = generate_comparison_feedback(scores, performance)

    print(f"\n   Opening      : {feedback['opening']}")
    for line in feedback["feedback_lines"]:
        print(f"   • {line}")
    print(f"\n   Encouragement: {feedback['encouragement']}")

    # ── Final JSON ────────────────────────────────────────────────────
    result = {
        "overall_score":   scores["overall_score"],
        "pitch_accuracy":  scores["pitch_accuracy"],
        "timing_accuracy": scores["timing_accuracy"],
        "stability":       scores["stability"],
        "tier":            scores["tier"],
        "vocal_range":     vr,
        "feedback":        feedback["feedback_lines"],
        "opening":         feedback["opening"],
        "encouragement":   feedback["encouragement"],
    }

    print("\n" + "=" * 60)
    print("Final JSON output (Android-ready):")
    print("=" * 60)
    print(json.dumps(result, indent=2))
    print("\n[DONE]  Pipeline test complete.")


if __name__ == "__main__":
    run_test()
