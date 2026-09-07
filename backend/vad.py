"""
vad.py  —  Voiced Activity Detection

Not every frame in a recording contains singing.
Before pitch extraction we must identify which time intervals
actually contain voice, separating them from:
    - silence / breath between phrases
    - instrumental sections in the reference
    - background noise

Method
------
We use a combination of three signals available from librosa:
    1. RMS energy        — frames with very low energy are silent
    2. Zero-crossing rate — very high ZCR with low energy → noise-like
    3. pyin voiced flag  — use the probability output if pitch is being
                           extracted anyway (optional fast path)

The output is a list of voiced segments (start, end) in seconds,
which the rest of the pipeline uses as a mask.
"""

import numpy as np
import librosa
from typing import List, Tuple


# ── Constants ──────────────────────────────────────────────────────────────

HOP_LENGTH  = 512
FRAME_LEN   = 2048
TARGET_SR   = 22050

# Energy threshold: frames below this RMS are considered silent.
RMS_SILENCE_THRESHOLD = 0.02

# Minimum voiced segment duration in seconds (ignore very short pops).
MIN_SEGMENT_DURATION_SEC = 0.15

# Margin to add around each detected segment (avoids clipping onsets).
SEGMENT_MARGIN_SEC = 0.05


# ── Segment type ───────────────────────────────────────────────────────────

class VoicedSegment:
    """A contiguous time interval classified as voiced (singing)."""

    def __init__(self, start: float, end: float):
        self.start = round(start, 4)
        self.end   = round(end,   4)

    @property
    def duration(self) -> float:
        return self.end - self.start

    def to_dict(self) -> dict:
        return {
            "start":    self.start,
            "end":      self.end,
            "duration": round(self.duration, 4),
        }

    def __repr__(self) -> str:
        return f"VoicedSegment({self.start:.3f}s → {self.end:.3f}s)"


# ── Core detection ─────────────────────────────────────────────────────────

def detect_voiced_segments(
    audio: np.ndarray,
    sample_rate: int = TARGET_SR,
    rms_threshold: float = RMS_SILENCE_THRESHOLD,
    min_duration: float  = MIN_SEGMENT_DURATION_SEC,
    margin: float        = SEGMENT_MARGIN_SEC,
) -> List[VoicedSegment]:
    """
    Detect which time intervals contain voiced (singing) audio.

    Parameters
    ----------
    audio        : np.ndarray  float32 mono waveform
    sample_rate  : int
    rms_threshold: float       RMS frames below this → silent
    min_duration : float       minimum voiced segment length in seconds
    margin       : float       seconds to extend each segment boundary

    Returns
    -------
    list of VoicedSegment objects sorted by start time.
    """

    if len(audio) == 0:
        return []

    # ── Compute RMS per frame ─────────────────────────────────────────
    rms = librosa.feature.rms(
        y=audio,
        frame_length=FRAME_LEN,
        hop_length=HOP_LENGTH,
    )[0]

    # Boolean mask: True = voiced frame
    voiced_mask = rms > rms_threshold

    # ── Convert frame mask to time segments ──────────────────────────
    frame_times = librosa.frames_to_time(
        np.arange(len(voiced_mask)),
        sr=sample_rate,
        hop_length=HOP_LENGTH,
    )

    segments: List[VoicedSegment] = []
    in_segment  = False
    seg_start   = 0.0

    for i, is_voiced in enumerate(voiced_mask):
        t = float(frame_times[i])

        if is_voiced and not in_segment:
            seg_start   = max(0.0, t - margin)
            in_segment  = True

        elif not is_voiced and in_segment:
            seg_end    = min(float(audio.shape[0]) / sample_rate, t + margin)
            duration   = seg_end - seg_start

            if duration >= min_duration:
                segments.append(VoicedSegment(seg_start, seg_end))

            in_segment = False

    # Close any segment still open at end of file.
    if in_segment:
        seg_end  = float(len(audio)) / sample_rate
        duration = seg_end - seg_start
        if duration >= min_duration:
            segments.append(VoicedSegment(seg_start, seg_end))

    # ── Merge overlapping / adjacent segments ────────────────────────
    segments = _merge_segments(segments, gap_threshold=0.2)

    return segments


def _merge_segments(
    segments: List[VoicedSegment],
    gap_threshold: float = 0.2,
) -> List[VoicedSegment]:
    """
    Merge consecutive segments whose gap is smaller than gap_threshold.

    This avoids fragmenting a single sung phrase into many tiny pieces
    due to brief breath/onset dips.
    """
    if not segments:
        return []

    merged = [segments[0]]

    for seg in segments[1:]:
        prev = merged[-1]
        gap  = seg.start - prev.end

        if gap <= gap_threshold:
            # Extend previous segment to cover this one.
            merged[-1] = VoicedSegment(prev.start, seg.end)
        else:
            merged.append(seg)

    return merged


# ── Audio masking ──────────────────────────────────────────────────────────

def extract_voiced_audio(
    audio: np.ndarray,
    segments: List[VoicedSegment],
    sample_rate: int = TARGET_SR,
) -> np.ndarray:
    """
    Concatenate all voiced regions of the audio into a single array.

    Useful when you want to run pyin only on voiced portions
    (saves time and reduces false detections).
    """
    if not segments:
        return np.array([], dtype=np.float32)

    chunks = []
    for seg in segments:
        start_sample = int(seg.start * sample_rate)
        end_sample   = int(seg.end   * sample_rate)
        chunks.append(audio[start_sample:end_sample])

    return np.concatenate(chunks).astype(np.float32)


def voiced_ratio(
    segments: List[VoicedSegment],
    total_duration: float,
) -> float:
    """Fraction of the total recording that was classified as voiced."""
    if total_duration <= 0:
        return 0.0
    voiced_total = sum(s.duration for s in segments)
    return round(min(voiced_total / total_duration, 1.0), 4)


# ── CLI smoke-test ─────────────────────────────────────────────────────────

if __name__ == "__main__":
    import json, os, soundfile as sf

    os.makedirs("uploads", exist_ok=True)

    # Build a signal: 1s silence → 2s tone → 0.5s silence → 1s tone
    sr  = TARGET_SR
    silence = np.zeros(sr, dtype=np.float32)
    tone_a  = (0.4 * np.sin(2 * np.pi * 440 * np.linspace(0, 2, sr * 2))).astype(np.float32)
    tone_b  = (0.4 * np.sin(2 * np.pi * 330 * np.linspace(0, 1, sr * 1))).astype(np.float32)
    audio   = np.concatenate([silence, tone_a, silence[:sr // 2], tone_b])

    test_path = "uploads/_test_vad.wav"
    sf.write(test_path, audio, sr)

    segments = detect_voiced_segments(audio, sample_rate=sr)

    print(f"Detected {len(segments)} voiced segments:")
    for s in segments:
        print(f"  {s}")

    ratio = voiced_ratio(segments, len(audio) / sr)
    print(f"Voiced ratio: {ratio:.2%}")
