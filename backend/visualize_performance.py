import matplotlib.pyplot as plt
import numpy as np

def hz_to_midi(hz_array):
    """Convert an array of Hz to MIDI notes (semitones)."""
    hz_safe = np.where(np.isnan(hz_array) | (hz_array <= 0), 1.0, hz_array)
    midi = 12.0 * np.log2(hz_safe / 440.0) + 69.0
    return np.where(np.isnan(hz_array), np.nan, midi)

def plot_performance(ref_f0, ref_times, user_f0, user_times, aligned_pairs, output_path="uploads/performance_report.png"):
    """
    Generates a 2-panel plot comparing the user's pitch against the reference.
    1. Unaligned (Raw time domain)
    2. DTW Aligned (Warped time domain)
    """
    
    # Convert pitches to MIDI for perceptually linear plotting
    ref_midi = hz_to_midi(ref_f0)
    user_midi = hz_to_midi(user_f0)
    
    # Set up figure
    fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(12, 10))
    fig.suptitle('Vocal Performance Analysis', fontsize=16)
    
    # --- 1. Raw Time Domain Plot ---
    ax1.plot(ref_times, ref_midi, label='Reference Pitch', color='blue', linewidth=2, alpha=0.7)
    ax1.plot(user_times, user_midi, label='User Pitch', color='orange', linewidth=2, alpha=0.8)
    ax1.set_title("Unaligned Pitch Curves (Raw Time Domain)")
    ax1.set_xlabel("Time (seconds)")
    ax1.set_ylabel("Pitch (MIDI Semitones)")
    ax1.legend()
    ax1.grid(True, alpha=0.3)
    
    # --- 2. DTW Aligned Plot ---
    # Extract data from aligned pairs
    aligned_ref_midi = []
    aligned_user_midi = []
    
    for pair in aligned_pairs:
        r_f = pair["ref_freq"]
        u_f = pair["user_freq"]
        
        # Convert to MIDI, use nan if None
        if r_f is not None and r_f > 0:
            aligned_ref_midi.append(12.0 * np.log2(r_f / 440.0) + 69.0)
        else:
            aligned_ref_midi.append(np.nan)
            
        if u_f is not None and u_f > 0:
            aligned_user_midi.append(12.0 * np.log2(u_f / 440.0) + 69.0)
        else:
            aligned_user_midi.append(np.nan)
            
    x_axis = range(len(aligned_pairs))
    
    ax2.plot(x_axis, aligned_ref_midi, label='Reference Pitch', color='blue', linewidth=2, alpha=0.7)
    ax2.plot(x_axis, aligned_user_midi, label='User Pitch', color='orange', linewidth=2, alpha=0.8)
    
    # Highlight areas where user was highly unstable or off-pitch
    # A simple highlight for frames where error > 1 semitone
    diffs = np.abs(np.array(aligned_ref_midi) - np.array(aligned_user_midi))
    bad_frames = np.where(diffs > 1.0)[0] # more than 1 semitone off
    
    if len(bad_frames) > 0:
        ax2.scatter(bad_frames, np.array(aligned_user_midi)[bad_frames], color='red', s=10, label='Error > 1 Semitone', zorder=5)

    ax2.set_title("DTW Aligned Pitch Curves (Note Matching)")
    ax2.set_xlabel("Warped Frame Index (Aligned Time)")
    ax2.set_ylabel("Pitch (MIDI Semitones)")
    ax2.legend()
    ax2.grid(True, alpha=0.3)
    
    # Save the plot
    plt.tight_layout(rect=[0, 0.03, 1, 0.95])
    plt.savefig(output_path, dpi=150)
    print(f"\n[Visualizer] Saved performance visualization to {output_path}")
    plt.close()

if __name__ == "__main__":
    pass
