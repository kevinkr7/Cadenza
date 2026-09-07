"""
performance_analyzer.py

After DTW temporal alignment, compute detailed performance metrics by
comparing the user's pitch curve against the reference frame-by-frame
(through the DTW warp path).

Three primary metrics:
    1. Pitch Accuracy  — how close the user's frequency is to the reference
    2. Timing Accuracy — how well the user's note onsets match the reference
    3. Pitch Stability — how steady the user holds each note

All measurements are in musical cents (1 semitone = 100 cents),
which is perceptually linear and instrument-independent.
"""

import numpy as np
from typing import List, Dict, Any, Tuple

from note_mapper import map_frequency_to_note, frequency_to_midi, midi_to_note_name


# ── Constants ──────────────────────────────────────────────────────────────

# A deviation within ±10 cents is considered "in tune" (good accuracy).
INTUNE_THRESHOLD_CENTS = 10.0

# Minimum number of voiced aligned frames required for a reliable measurement.
MIN_VOICED_PAIRS = 5


# ── Helpers ────────────────────────────────────────────────────────────────

def _cents_between(freq_a: float, freq_b: float) -> float:
    """Signed cents from freq_a to freq_b. Positive = freq_b is higher."""
    if freq_a <= 0 or freq_b <= 0:
        return 0.0
    return 1200.0 * np.log2(freq_b / freq_a)


def _voiced_pairs(aligned_pairs: List[dict]) -> List[dict]:
    """Filter to pairs where both reference and user have a valid frequency."""
    return [
        p for p in aligned_pairs
        if p["ref_freq"] is not None and p["user_freq"] is not None
        and p["ref_freq"] > 0 and p["user_freq"] > 0
    ]


# ── 1. Pitch Accuracy ──────────────────────────────────────────────────────

def compute_pitch_accuracy(aligned_pairs: List[dict]) -> Dict[str, Any]:
    """
    For each aligned (ref, user) frame pair where both are voiced,
    measure the signed cents deviation: user_freq vs ref_freq.

    Aggregate into:
        mean_deviation_cents   — overall flat/sharp bias
        mae_cents              — mean absolute error
        rmse_cents             — root-mean-square error
        in_tune_ratio          — fraction of frames within ±10 cents
        accuracy_score         — 0-100 score derived from MAE
    """

    pairs = _voiced_pairs(aligned_pairs)

    if len(pairs) < MIN_VOICED_PAIRS:
        return {
            "accuracy_score":       0,
            "mean_deviation_cents": None,
            "mae_cents":            None,
            "rmse_cents":           None,
            "in_tune_ratio":        None,
            "voiced_pair_count":    len(pairs),
            "reliable":             False,
        }

    deviations = np.array([
        _cents_between(p["ref_freq"], p["user_freq"])
        for p in pairs
    ])

    mean_dev  = float(np.mean(deviations))
    mae       = float(np.mean(np.abs(deviations)))
    rmse      = float(np.sqrt(np.mean(deviations ** 2)))
    in_tune   = float(np.mean(np.abs(deviations) <= INTUNE_THRESHOLD_CENTS))

    # Score: 100 at 0 MAE, decays linearly, minimum 0 at 100+ cents error.
    raw_score = max(0.0, 100.0 - mae)
    score     = int(round(raw_score))

    return {
        "accuracy_score":       score,
        "mean_deviation_cents": round(mean_dev, 2),
        "mae_cents":            round(mae, 2),
        "rmse_cents":           round(rmse, 2),
        "in_tune_ratio":        round(in_tune, 3),
        "voiced_pair_count":    len(pairs),
        "reliable":             len(pairs) >= MIN_VOICED_PAIRS,
        "pitch_direction":      "flat" if mean_dev < -5 else ("sharp" if mean_dev > 5 else "centered"),
    }


# ── 2. Timing Accuracy ────────────────────────────────────────────────────

def _detect_note_onsets(
    f0: np.ndarray,
    times: np.ndarray,
) -> List[Dict[str, Any]]:
    """
    Detect note onsets from an F0 curve.

    An onset is a transition from NaN→voiced or a jump of ≥100 cents
    from one voiced frame to the next (new note).
    """
    onsets = []
    prev_voiced = False
    prev_midi   = None

    for i in range(len(f0)):
        freq = f0[i]
        t    = float(times[i])

        if np.isnan(freq) or freq <= 0:
            prev_voiced = False
            prev_midi   = None
            continue

        midi = int(round(12.0 * np.log2(freq / 440.0) + 69.0))

        is_new_note = (
            not prev_voiced
            or (prev_midi is not None and abs(midi - prev_midi) >= 1)
        )

        if is_new_note:
            onsets.append({
                "time":      round(t, 4),
                "frequency": round(float(freq), 3),
                "midi":      midi,
                "note":      midi_to_note_name(midi),
            })

        prev_voiced = True
        prev_midi   = midi

    return onsets


def compute_timing_accuracy(
    ref_f0: np.ndarray,
    ref_times: np.ndarray,
    user_f0: np.ndarray,
    user_times: np.ndarray,
    dtw_path: List[Tuple[int, int]],
) -> Dict[str, Any]:
    """
    Compare reference and user note onset times (after DTW alignment).

    For each matched reference onset, find the closest user onset
    and compute the offset error in seconds and milliseconds.

    Returns timing_score (0-100), mean onset error, and per-note breakdown.
    """

    ref_onsets  = _detect_note_onsets(ref_f0,  ref_times)
    user_onsets = _detect_note_onsets(user_f0, user_times)

    if not ref_onsets or not user_onsets:
        return {
            "timing_score":        50,  # neutral — not enough data
            "mean_onset_error_ms": None,
            "onset_count":         len(ref_onsets),
            "matched_count":       0,
            "reliable":            False,
            "details":             [],
        }

    # Build a DTW-warped time lookup: ref_time → user_time.
    # For each reference frame index, find the corresponding user time.
    ref_idx_to_user_time: Dict[int, float] = {}
    for ref_idx, usr_idx in dtw_path:
        if usr_idx < len(user_times):
            ref_idx_to_user_time[ref_idx] = float(user_times[usr_idx])

    # For each reference onset, find the expected user time via the warp.
    details    = []
    errors_ms  = []

    for ref_onset in ref_onsets:
        ref_t = ref_onset["time"]

        # Find the ref frame index closest to this onset time.
        closest_ref_idx = int(np.argmin(np.abs(ref_times - ref_t)))
        expected_user_t = ref_idx_to_user_time.get(closest_ref_idx)

        if expected_user_t is None:
            continue

        # Find the actual user onset closest to expected_user_t.
        user_onset_times = np.array([o["time"] for o in user_onsets])
        closest_usr_idx  = int(np.argmin(np.abs(user_onset_times - expected_user_t)))
        actual_user_t    = float(user_onset_times[closest_usr_idx])

        error_sec = actual_user_t - expected_user_t
        error_ms  = error_sec * 1000.0
        errors_ms.append(abs(error_ms))

        details.append({
            "note":                ref_onset["note"],
            "ref_onset":           ref_t,
            "expected_user_onset": round(expected_user_t, 4),
            "actual_user_onset":   round(actual_user_t,   4),
            "error_ms":            round(error_ms, 1),
            "direction":           "late" if error_ms > 0 else "early",
        })

    if not errors_ms:
        return {
            "timing_score":        50,
            "mean_onset_error_ms": None,
            "onset_count":         len(ref_onsets),
            "matched_count":       0,
            "reliable":            False,
            "details":             [],
        }

    mean_error_ms = float(np.mean(errors_ms))

    # Score: 100 at 0 ms, decays to 0 at 500 ms mean error.
    score = int(round(max(0.0, 100.0 - (mean_error_ms / 500.0) * 100.0)))

    return {
        "timing_score":        score,
        "mean_onset_error_ms": round(mean_error_ms, 1),
        "onset_count":         len(ref_onsets),
        "matched_count":       len(details),
        "reliable":            len(details) >= 2,
        "details":             details,
    }


# ── 3. Pitch Stability ─────────────────────────────────────────────────────

def compute_pitch_stability(
    user_f0: np.ndarray,
    user_times: np.ndarray,
    ref_segments: List[dict],
) -> Dict[str, Any]:
    """
    For each reference segment (a note the user is supposed to hold),
    measure how stable the user's pitch is within the corresponding
    time window.

    Stability is measured as the standard deviation of the user's voiced
    F0 values within that window, in cents from the median.

    Returns stability_score (0-100) and per-note breakdown.
    """

    if not ref_segments:
        return {
            "stability_score": 50,
            "mean_stability_cents": None,
            "reliable": False,
            "details": [],
        }

    details        = []
    stability_vals = []

    for seg in ref_segments:
        start, end = seg["start"], seg["end"]

        # Extract user frames in this time window.
        mask  = (user_times >= start) & (user_times <= end)
        freqs = user_f0[mask]

        voiced = freqs[~np.isnan(freqs)]
        voiced = voiced[voiced > 0]

        if len(voiced) < 3:
            continue

        median_f = float(np.median(voiced))
        cents    = 1200.0 * np.log2(voiced / median_f)
        std_c    = float(np.std(cents))

        stability_vals.append(std_c)
        details.append({
            "note":             seg["note"],
            "start":            start,
            "end":              end,
            "stability_cents":  round(std_c, 2),
            "voiced_frames":    len(voiced),
            "stability_label":  (
                "stable"            if std_c <= 15 else
                "moderately stable" if std_c <= 40 else
                "unstable"
            ),
        })

    if not stability_vals:
        return {
            "stability_score":      50,
            "mean_stability_cents": None,
            "reliable":             False,
            "details":              details,
        }

    mean_stab  = float(np.mean(stability_vals))

    # Score: 100 at 0 cents deviation, 0 at 80+ cents.
    score = int(round(max(0.0, 100.0 - (mean_stab / 80.0) * 100.0)))

    return {
        "stability_score":      score,
        "mean_stability_cents": round(mean_stab, 2),
        "reliable":             True,
        "details":              details,
    }


# ── 4. Vocal Range ─────────────────────────────────────────────────────────

def compute_vocal_range(user_f0: np.ndarray) -> Dict[str, Any]:
    """
    Find the lowest and highest stable notes in the user's performance.

    We use the 5th/95th percentile of voiced frequencies to avoid
    letting isolated outlier frames distort the range.
    """
    voiced = user_f0[~np.isnan(user_f0)]
    voiced = voiced[voiced > 0]

    if len(voiced) < 5:
        return {"lowest": None, "highest": None, "range_semitones": None}

    low_freq  = float(np.percentile(voiced, 5))
    high_freq = float(np.percentile(voiced, 95))

    low_note  = map_frequency_to_note(low_freq)
    high_note = map_frequency_to_note(high_freq)

    semitones = 12.0 * np.log2(high_freq / low_freq) if low_freq > 0 else 0.0

    return {
        "lowest":          low_note["note"],
        "highest":         high_note["note"],
        "lowest_freq":     round(low_freq,  2),
        "highest_freq":    round(high_freq, 2),
        "range_semitones": round(semitones, 1),
    }


# ── Master analyzer ────────────────────────────────────────────────────────

def analyze_performance(
    ref_f0:       np.ndarray,
    ref_times:    np.ndarray,
    ref_segments: List[dict],
    user_f0:      np.ndarray,
    user_times:   np.ndarray,
    aligned_pairs: List[dict],
    dtw_path:     List[Tuple[int, int]],
) -> Dict[str, Any]:
    """
    Run all three metrics and vocal range, return a combined result dict.
    """

    pitch   = compute_pitch_accuracy(aligned_pairs)
    timing  = compute_timing_accuracy(ref_f0, ref_times, user_f0, user_times, dtw_path)
    stab    = compute_pitch_stability(user_f0, user_times, ref_segments)
    v_range = compute_vocal_range(user_f0)

    return {
        "pitch_accuracy":  pitch,
        "timing_accuracy": timing,
        "stability":       stab,
        "vocal_range":     v_range,
    }
