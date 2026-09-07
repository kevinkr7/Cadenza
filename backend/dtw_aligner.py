"""
dtw_aligner.py  —  Dynamic Time Warping Alignment

Problem: a singer may perform the correct notes but at a different tempo
or with slight timing offsets relative to the reference.

Naive frame-by-frame comparison would incorrectly penalise correct notes
sung at a slightly different time. DTW finds the optimal warping path
that aligns the two pitch sequences regardless of local timing differences.

Library: dtaidistance
    https://dtaidistance.readthedocs.io
    Fast DTW implementation in C with a Python interface.
    Falls back to a pure-numpy implementation if dtaidistance is not available.

Usage
-----
    from dtw_aligner import align_pitch_curves

    path, cost = align_pitch_curves(ref_f0, user_f0)
    # path is a list of (ref_index, user_index) pairs
"""

import numpy as np
from typing import List, Tuple


# ── Constants ──────────────────────────────────────────────────────────────

# Replace NaN (unvoiced frame) with this frequency before DTW.
# We use a very low out-of-range value so the distance is large
# when one sequence is voiced and the other is not.
NAN_FILL_HZ = 1.0

# Convert Hz to a pitch space that is perceptually linear (semitones).
# This makes a 1-semitone error at A4 equivalent to a 1-semitone error at C3.
def _hz_to_semitones(freq: np.ndarray) -> np.ndarray:
    """Convert Hz to semitone scale (MIDI note space, float)."""
    safe = np.where(np.isnan(freq) | (freq <= 0), NAN_FILL_HZ, freq)
    return 12.0 * np.log2(safe / 440.0) + 69.0   # same as librosa.hz_to_midi


# ── DTW backend selection ──────────────────────────────────────────────────

def _dtaidistance_available() -> bool:
    try:
        import dtaidistance  # noqa: F401
        return True
    except ImportError:
        return False


# ── dtaidistance implementation ────────────────────────────────────────────

def _dtw_dtaidistance(
    seq_a: np.ndarray,
    seq_b: np.ndarray,
    window: int | None = None,
) -> Tuple[List[Tuple[int, int]], float]:
    from dtaidistance import dtw as dtai_dtw

    kwargs = {}
    if window is not None:
        kwargs["window"] = window

    distance = dtai_dtw.distance_fast(seq_a, seq_b, **kwargs)
    path     = dtai_dtw.warping_path(seq_a, seq_b, **kwargs)

    return path, float(distance)


# ── Pure-numpy fallback DTW ────────────────────────────────────────────────

def _dtw_numpy(
    seq_a: np.ndarray,
    seq_b: np.ndarray,
    window: int | None = None,
) -> Tuple[List[Tuple[int, int]], float]:
    """
    Classic O(N·M) DTW using dynamic programming.

    For typical vocal recordings (< 60 s at 256 hop-length → ~5 000 frames)
    this is fast enough without C acceleration.
    """
    n, m = len(seq_a), len(seq_b)

    # Cost matrix (Euclidean distance between semitone values).
    cost = np.full((n, m), np.inf, dtype=np.float64)

    # Apply Sakoe-Chiba band if requested.
    w = window if window is not None else max(n, m)

    cost[0, 0] = abs(seq_a[0] - seq_b[0])

    for i in range(1, n):
        cost[i, 0] = cost[i - 1, 0] + abs(seq_a[i] - seq_b[0])

    for j in range(1, m):
        cost[0, j] = cost[0, j - 1] + abs(seq_a[0] - seq_b[j])

    for i in range(1, n):
        j_start = max(1, i - w)
        j_end   = min(m, i + w + 1)
        for j in range(j_start, j_end):
            d = abs(seq_a[i] - seq_b[j])
            cost[i, j] = d + min(cost[i - 1, j], cost[i, j - 1], cost[i - 1, j - 1])

    # Traceback.
    path: List[Tuple[int, int]] = []
    i, j = n - 1, m - 1

    while i > 0 or j > 0:
        path.append((i, j))

        if i == 0:
            j -= 1
        elif j == 0:
            i -= 1
        else:
            candidates = {
                cost[i - 1, j - 1]: (i - 1, j - 1),
                cost[i - 1, j]:     (i - 1, j),
                cost[i, j - 1]:     (i, j - 1),
            }
            i, j = candidates[min(candidates)]

    path.append((0, 0))
    path.reverse()

    total_distance = float(cost[n - 1, m - 1])
    return path, total_distance


# ── Public API ─────────────────────────────────────────────────────────────

def align_pitch_curves(
    ref_f0: np.ndarray,
    user_f0: np.ndarray,
    sakoe_chiba_radius: int | None = None,
) -> Tuple[List[Tuple[int, int]], float]:
    """
    Align two F0 pitch curves using Dynamic Time Warping.

    Parameters
    ----------
    ref_f0               : np.ndarray  reference pitch curve (Hz, with NaN)
    user_f0              : np.ndarray  user pitch curve    (Hz, with NaN)
    sakoe_chiba_radius   : int | None  optional Sakoe-Chiba constraint width
                                       (in frames). None = unconstrained.

    Returns
    -------
    path     : list of (ref_idx, user_idx) tuples — the optimal warp path
    distance : float — the total DTW distance (lower = better alignment)
    """

    # Convert to perceptual pitch space.
    ref_semi  = _hz_to_semitones(ref_f0).astype(np.float64)
    user_semi = _hz_to_semitones(user_f0).astype(np.float64)

    if _dtaidistance_available():
        path, distance = _dtw_dtaidistance(ref_semi, user_semi, window=sakoe_chiba_radius)
    else:
        path, distance = _dtw_numpy(ref_semi, user_semi, window=sakoe_chiba_radius)

    return path, distance


def build_aligned_pairs(
    ref_f0:   np.ndarray,
    ref_times: np.ndarray,
    user_f0:   np.ndarray,
    user_times: np.ndarray,
    path: List[Tuple[int, int]],
) -> List[dict]:
    """
    Given the DTW warp path, produce a list of (ref, user) frame pairs
    with their time and frequency values.

    This is used by the performance analyzer to compute per-frame errors.
    """
    pairs = []

    for ref_idx, usr_idx in path:
        ref_freq  = ref_f0[ref_idx]  if ref_idx  < len(ref_f0)   else np.nan
        user_freq = user_f0[usr_idx] if usr_idx < len(user_f0)  else np.nan
        ref_t     = float(ref_times[ref_idx])   if ref_idx  < len(ref_times)  else 0.0
        user_t    = float(user_times[usr_idx])  if usr_idx < len(user_times) else 0.0

        pairs.append({
            "ref_time":    round(ref_t,    4),
            "user_time":   round(user_t,   4),
            "ref_freq":    None if np.isnan(ref_freq)  else round(float(ref_freq),  3),
            "user_freq":   None if np.isnan(user_freq) else round(float(user_freq), 3),
        })

    return pairs


# ── CLI smoke-test ─────────────────────────────────────────────────────────

if __name__ == "__main__":
    import json

    # Simulate a reference singing A4 (440 Hz) and user singing slightly flat.
    sr  = 22050
    hop = 256
    dur = 2.0  # seconds
    n   = int(sr * dur / hop)

    ref_f0  = np.full(n, 440.0)
    user_f0 = np.full(n, 430.0)  # ~40 cents flat

    # Introduce some NaN (unvoiced) frames.
    ref_f0[0:3]   = np.nan
    user_f0[-3:]  = np.nan

    path, dist = align_pitch_curves(ref_f0, user_f0)

    print(f"DTW distance : {dist:.4f}")
    print(f"Path length  : {len(path)} pairs")
    print(f"First 5 pairs: {path[:5]}")
    print(f"Backend used : {'dtaidistance' if _dtaidistance_available() else 'numpy-fallback'}")
