"""
reference_builder.py

Builds the vocal reference template from the separated reference track.

This module takes the vocal stem (already extracted by vocal_separator.py)
and produces a structured representation of what the singer *should* sing:
    - per-segment pitch curve
    - dominant note per segment
    - note onset / offset timings
    - per-segment confidence

The template is what we later align the user's performance against using DTW.
"""

import numpy as np
import librosa
from typing import List, Dict, Any

from vad import detect_voiced_segments, VoicedSegment
from note_mapper import map_frequency_to_note


# ── Constants ──────────────────────────────────────────────────────────────

TARGET_SR   = 22050
HOP_LENGTH  = 256
FMIN        = librosa.note_to_hz("C2")
FMAX        = librosa.note_to_hz("C7")


# ── Pitch extraction (pyin) ────────────────────────────────────────────────

def _extract_pitch_curve(
    audio: np.ndarray,
    sample_rate: int = TARGET_SR,
) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    """
    Run pyin on the full audio and return (f0, times, voiced_flag).

    pyin gives better results than piptrack for monophonic vocals
    because it uses a probabilistic model specifically designed for
    fundamental frequency estimation.
    """
    f0, voiced_flag, voiced_probs = librosa.pyin(
        audio,
        fmin=FMIN,
        fmax=FMAX,
        sr=sample_rate,
        hop_length=HOP_LENGTH,
    )

    times = librosa.times_like(f0, sr=sample_rate, hop_length=HOP_LENGTH)

    return f0, times, voiced_flag


def _frames_for_segment(
    times: np.ndarray,
    start: float,
    end: float,
) -> np.ndarray:
    """Returns a boolean mask for frames inside [start, end]."""
    return (times >= start) & (times <= end)


# ── Note-sequence builder ──────────────────────────────────────────────────

def _dominant_note(f0_segment: np.ndarray) -> Dict[str, Any] | None:
    """
    Given a sequence of f0 values for one segment (with NaNs for unvoiced),
    return the dominant note based on the median voiced frequency.
    """
    voiced = f0_segment[~np.isnan(f0_segment)]
    if len(voiced) == 0:
        return None

    median_freq = float(np.median(voiced))
    return map_frequency_to_note(median_freq)


def _build_pitch_curve_for_segment(
    f0: np.ndarray,
    times: np.ndarray,
    mask: np.ndarray,
) -> List[Dict[str, Any]]:
    """
    Build a time-indexed pitch curve list for one segment.

    Each entry: { time, frequency, midi, note, cents }
    NaN (unvoiced) frames are kept as { time, frequency: null }.
    """
    curve = []

    for i in np.where(mask)[0]:
        freq = f0[i]
        t    = float(times[i])

        if np.isnan(freq):
            curve.append({"time": round(t, 4), "frequency": None, "note": None, "midi": None, "cents": None})
        else:
            note_info = map_frequency_to_note(float(freq))
            curve.append({
                "time":      round(t, 4),
                "frequency": round(float(freq), 3),
                "note":      note_info["note"],
                "midi":      note_info["midi_number"],
                "cents":     round(note_info["deviation_cents"], 2),
            })

    return curve


def _segment_confidence(f0_segment: np.ndarray) -> float:
    """
    Fraction of frames in the segment that are voiced.
    Used as a confidence measure for the reference note.
    """
    voiced_count = int(np.sum(~np.isnan(f0_segment)))
    total_count  = len(f0_segment)
    if total_count == 0:
        return 0.0
    return round(voiced_count / total_count, 3)


# ── Main builder ───────────────────────────────────────────────────────────

def build_reference_template(
    vocal_audio: np.ndarray,
    sample_rate: int = TARGET_SR,
    song_name: str   = "reference",
) -> Dict[str, Any]:
    """
    Build the full vocal reference template from a separated vocal track.

    Parameters
    ----------
    vocal_audio  : np.ndarray  float32 mono vocal waveform
    sample_rate  : int
    song_name    : str         identifier stored in the template

    Returns
    -------
    dict:
        song        str         identifier
        duration    float       total duration in seconds
        segments    list[dict]  per-segment note / pitch info
        pitch_curve list[dict]  full pitch curve (all segments)
        f0_array    np.ndarray  raw f0 values (for DTW input)
        time_array  np.ndarray  corresponding time values
        meta        dict        summary statistics
    """

    total_duration = len(vocal_audio) / sample_rate

    # ── 1. Voiced Activity Detection ──────────────────────────────────
    voiced_segments: List[VoicedSegment] = detect_voiced_segments(
        vocal_audio, sample_rate=sample_rate
    )

    # ── 2. Pitch extraction over full audio ───────────────────────────
    f0, times, voiced_flag = _extract_pitch_curve(vocal_audio, sample_rate)

    # ── 3. Build per-segment template ────────────────────────────────
    segments = []

    for seg in voiced_segments:
        mask = _frames_for_segment(times, seg.start, seg.end)

        if not np.any(mask):
            continue

        f0_seg    = f0[mask]
        note_info = _dominant_note(f0_seg)

        if note_info is None:
            continue

        pitch_curve = _build_pitch_curve_for_segment(f0, times, mask)
        confidence  = _segment_confidence(f0_seg)

        segments.append({
            "start":       seg.start,
            "end":         seg.end,
            "duration":    round(seg.duration, 4),
            "note":        note_info["note"],
            "midi":        note_info["midi_number"],
            "frequency":   note_info["frequency"],
            "cents":       note_info["deviation_cents"],
            "confidence":  confidence,
            "pitch_curve": pitch_curve,
        })

    # ── 4. Full pitch curve (for DTW input) ──────────────────────────
    full_curve = []
    for i in range(len(times)):
        freq = f0[i]
        full_curve.append({
            "time":      round(float(times[i]), 4),
            "frequency": None if np.isnan(freq) else round(float(freq), 3),
        })

    # ── 5. Summary statistics ─────────────────────────────────────────
    voiced_freqs = f0[~np.isnan(f0)]
    meta = {
        "total_segments":    len(segments),
        "voiced_frame_count": int(len(voiced_freqs)),
        "total_frame_count":  int(len(f0)),
        "voiced_ratio":       round(len(voiced_freqs) / max(len(f0), 1), 3),
    }

    if len(voiced_freqs) > 0:
        meta["lowest_frequency"]  = round(float(np.min(voiced_freqs)), 2)
        meta["highest_frequency"] = round(float(np.max(voiced_freqs)), 2)

        lowest_note  = map_frequency_to_note(float(np.min(voiced_freqs)))
        highest_note = map_frequency_to_note(float(np.max(voiced_freqs)))

        meta["lowest_note"]  = lowest_note["note"]
        meta["highest_note"] = highest_note["note"]

    return {
        "song":        song_name,
        "duration":    round(total_duration, 3),
        "segments":    segments,
        "pitch_curve": full_curve,
        "f0_array":    f0,
        "time_array":  times,
        "meta":        meta,
    }


# ── CLI smoke-test ─────────────────────────────────────────────────────────

if __name__ == "__main__":
    import json, os, soundfile as sf

    os.makedirs("uploads", exist_ok=True)

    # Build a synthetic reference: two notes held for 2 seconds each.
    sr   = TARGET_SR
    t_a  = np.linspace(0, 2, sr * 2, endpoint=False)
    t_b  = np.linspace(0, 2, sr * 2, endpoint=False)
    sig  = np.concatenate([
        0.4 * np.sin(2 * np.pi * 440 * t_a),    # A4
        np.zeros(sr // 4),                        # brief silence
        0.4 * np.sin(2 * np.pi * 494 * t_b),    # B4
    ]).astype(np.float32)

    test_path = "uploads/_test_ref.wav"
    sf.write(test_path, sig, sr)

    import librosa as _lb
    audio, _ = _lb.load(test_path, sr=sr, mono=True)

    template = build_reference_template(audio, sample_rate=sr, song_name="test_two_notes")

    # Summarize without printing huge arrays.
    summary = {
        "song":     template["song"],
        "duration": template["duration"],
        "meta":     template["meta"],
        "segment_count": len(template["segments"]),
        "segments": [
            {k: v for k, v in seg.items() if k != "pitch_curve"}
            for seg in template["segments"]
        ],
    }
    print(json.dumps(summary, indent=2))
