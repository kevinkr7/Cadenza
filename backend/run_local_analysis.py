import os
import json
import librosa
import numpy as np
import warnings

# Suppress warnings for cleaner output
warnings.filterwarnings('ignore')

from audio_preprocessor import validate_pair
from reference_builder import build_reference_template
from dtw_aligner import align_pitch_curves, build_aligned_pairs
from performance_analyzer import analyze_performance
from score_engine import compute_score
from comparison_feedback import generate_comparison_feedback

def run_local_analysis():
    print("=" * 60)
    print("Cadenza MVP Pipeline — Running on real samples")
    print("=" * 60)

    # Use the files you uploaded
    vocals_path = "uploads/vocals.wav"  # The isolated reference vocals
    user_path = "uploads/record.wav"    # The user's singing recording

    if not os.path.exists(vocals_path):
        print(f"Error: Could not find {vocals_path}")
        return
    if not os.path.exists(user_path):
        print(f"Error: Could not find {user_path}")
        return

    print(f"\n[1/8] Loading and validating audio files...")
    # Validate and normalize
    ref_data, user_data = validate_pair(vocals_path, user_path)
    
    # Reload reference with librosa as done in main.py
    ref_vocal, ref_sr = librosa.load(vocals_path, sr=22050, mono=True)
    user_audio = user_data["audio"]

    print(f"   Original Reference vocals: {len(ref_vocal)/ref_sr:.2f}s")
    print(f"   User recording: {len(user_audio)/ref_sr:.2f}s")

    # Efficient approach: Snip the original audio based on the length of the recorded audio exactly
    print("\n[Snipping] Snipping reference audio to exactly match user recording length...")
    ref_vocal = ref_vocal[:len(user_audio)]
    print(f"   Snipped Reference vocals: {len(ref_vocal)/ref_sr:.2f}s")

    print("\n[2/8] Extracting pitch tracks (pyin)... this might take a minute.")
    HOP = 512
    FMIN_HZ = librosa.note_to_hz("C2")
    FMAX_HZ = librosa.note_to_hz("C7")

    ref_f0, _, _ = librosa.pyin(ref_vocal, fmin=FMIN_HZ, fmax=FMAX_HZ, sr=ref_sr, hop_length=HOP)
    ref_times = librosa.times_like(ref_f0, sr=ref_sr, hop_length=HOP)

    user_f0, _, _ = librosa.pyin(user_audio, fmin=FMIN_HZ, fmax=FMAX_HZ, sr=ref_sr, hop_length=HOP)
    user_times = librosa.times_like(user_f0, sr=ref_sr, hop_length=HOP)

    print("\n[3/8] Building reference vocal template...")
    ref_template = build_reference_template(
        vocal_audio=ref_vocal,
        sample_rate=ref_sr,
        song_name="Reference Track"
    )

    print("\n[4/8] DTW alignment...")
    radius = int(10.0 * ref_sr / HOP)
    dtw_path, dtw_distance = align_pitch_curves(ref_f0, user_f0, sakoe_chiba_radius=radius)
    aligned_pairs = build_aligned_pairs(ref_f0, ref_times, user_f0, user_times, dtw_path)

    print("\n[5/8] Analyzing performance...")
    performance = analyze_performance(
        ref_f0=ref_f0,
        ref_times=ref_times,
        ref_segments=ref_template["segments"],
        user_f0=user_f0,
        user_times=user_times,
        aligned_pairs=aligned_pairs,
        dtw_path=dtw_path,
    )

    print("\n[5.5/8] Generating Visualization...")
    try:
        from visualize_performance import plot_performance
        plot_path = "uploads/performance_plot.png"
        plot_performance(ref_f0, ref_times, user_f0, user_times, aligned_pairs, output_path=plot_path)
    except Exception as e:
        print(f"   [Error] Could not generate visualization: {e}")

    print("\n[6/8] Computing score...")
    scores = compute_score(performance)

    print("\n[7/8] Generating feedback...")
    feedback = generate_comparison_feedback(scores, performance)

    print("\n[8/8] Analysis Results:")
    print("=" * 60)
    result = {
        "overall_score": scores["overall_score"],
        "pitch_accuracy": scores["pitch_accuracy"],
        "timing_accuracy": scores["timing_accuracy"],
        "stability": scores["stability"],
        "tier": scores["tier"],
        "feedback": feedback["feedback_lines"],
        "opening": feedback["opening"],
        "encouragement": feedback["encouragement"],
        "pitch_advice": feedback["pitch_advice"],
        "timing_advice": feedback["timing_advice"],
        "stability_advice": feedback["stability_advice"],
    }
    
    print(json.dumps(result, indent=2))
    print("=" * 60)

if __name__ == "__main__":
    run_local_analysis()
