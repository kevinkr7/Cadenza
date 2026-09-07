import math
from note_mapper import frequency_to_midi


def get_valid_frequencies_from_pitch_track(pitch_track: list) -> list:
    """
    Extracts valid frequency values from the pitch track.

    The pitch track contains values like:
        {"time": 0.104, "frequency": 156.46}

    Some frequency values may be None when no pitch was detected.
    This function removes those None values.
    """

    valid_frequencies = []

    for point in pitch_track:
        frequency = point.get("frequency")

        if frequency is not None and frequency > 0:
            valid_frequencies.append(float(frequency))

    return valid_frequencies


def calculate_pitch_range_cents(
    minimum_pitch: float,
    maximum_pitch: float
) -> float:
    """
    Calculates pitch range in cents.

    This tells how wide the pitch movement was.

    Small range usually means sustained note.
    Large range usually means melodic phrase.
    """

    if minimum_pitch <= 0 or maximum_pitch <= 0:
        return 0.0

    if maximum_pitch <= minimum_pitch:
        return 0.0

    pitch_range_cents = 1200 * math.log2(maximum_pitch / minimum_pitch)

    return round(pitch_range_cents, 2)


def convert_frequencies_to_midi_notes(frequencies: list) -> list:
    """
    Converts frequency values into MIDI note numbers.

    This helps us count how many different note regions appeared.
    """

    midi_notes = []

    for frequency in frequencies:
        try:
            midi_note = frequency_to_midi(frequency)
            midi_notes.append(midi_note)

        except Exception:
            continue

    return midi_notes


def count_note_changes(midi_notes: list) -> int:
    """
    Counts how many times the detected note changes over time.

    If the note changes many times, the recording is likely a phrase.
    """

    if len(midi_notes) <= 1:
        return 0

    changes = 0
    previous_note = midi_notes[0]

    for current_note in midi_notes[1:]:
        if current_note != previous_note:
            changes += 1
            previous_note = current_note

    return changes


def classify_practice_mode(
    pitch_summary: dict,
    pitch_track: list
) -> dict:
    """
    Classifies the user's recording into a practice mode.

    Possible modes:
        sustained_note
        melodic_phrase
        unclear_noisy
    """

    voiced_ratio = pitch_summary.get("voiced_ratio", 0)
    stability_cents = pitch_summary.get("stability_cents", 0)

    minimum_pitch = pitch_summary.get("minimum_pitch", 0)
    maximum_pitch = pitch_summary.get("maximum_pitch", 0)

    valid_frequencies = get_valid_frequencies_from_pitch_track(pitch_track)

    midi_notes = convert_frequencies_to_midi_notes(valid_frequencies)

    distinct_note_count = len(set(midi_notes))
    note_change_count = count_note_changes(midi_notes)

    pitch_range_cents = calculate_pitch_range_cents(
        minimum_pitch=minimum_pitch,
        maximum_pitch=maximum_pitch
    )

    valid_frequency_count = len(valid_frequencies)

    # Case 1: Not enough clear vocal pitch
    if voiced_ratio < 0.35 or valid_frequency_count < 5:
        return {
            "mode": "unclear_noisy",
            "label": "Unclear Recording",
            "confidence": "medium",
            "message": "The recording does not contain enough clear vocal pitch information. Try recording again in a quieter place.",
            "pitch_range_cents": pitch_range_cents,
            "distinct_note_count": distinct_note_count,
            "note_change_count": note_change_count,
            "voiced_ratio": voiced_ratio
        }

    # Case 2: Mostly one steady note
    if (
        stability_cents <= 35
        and pitch_range_cents <= 120
        and distinct_note_count <= 2
    ):
        return {
            "mode": "sustained_note",
            "label": "Sustained Note",
            "confidence": "high",
            "message": "This sounds like a sustained-note practice recording. Your main goal here is to keep the note steady and centered.",
            "pitch_range_cents": pitch_range_cents,
            "distinct_note_count": distinct_note_count,
            "note_change_count": note_change_count,
            "voiced_ratio": voiced_ratio
        }

    # Case 3: Multiple notes / phrase-like movement
    if (
        stability_cents >= 80
        or pitch_range_cents >= 180
        or distinct_note_count >= 4
        or note_change_count >= 5
    ):
        return {
            "mode": "melodic_phrase",
            "label": "Melodic Phrase",
            "confidence": "high",
            "message": "This sounds like a melodic phrase because your pitch moved across multiple notes. For phrase analysis, note movement is expected.",
            "pitch_range_cents": pitch_range_cents,
            "distinct_note_count": distinct_note_count,
            "note_change_count": note_change_count,
            "voiced_ratio": voiced_ratio
        }

    # Default case
    return {
        "mode": "sustained_note",
        "label": "Sustained Note Attempt",
        "confidence": "medium",
        "message": "This seems like a sustained-note attempt, but the pitch moved slightly. Try holding one clear note with steady breath support.",
        "pitch_range_cents": pitch_range_cents,
        "distinct_note_count": distinct_note_count,
        "note_change_count": note_change_count,
        "voiced_ratio": voiced_ratio
    }


if __name__ == "__main__":
    sample_pitch_summary = {
        "minimum_pitch": 128.57,
        "maximum_pitch": 214.98,
        "stability_cents": 252.42,
        "voiced_ratio": 0.90
    }

    sample_pitch_track = [
        {"time": 0.1, "frequency": 156.46},
        {"time": 0.2, "frequency": 169.64},
        {"time": 0.3, "frequency": 178.70},
        {"time": 1.0, "frequency": 214.98},
        {"time": 3.0, "frequency": 141.01},
        {"time": 5.0, "frequency": 189.32},
        {"time": 9.0, "frequency": 134.65}
    ]

    result = classify_practice_mode(
        pitch_summary=sample_pitch_summary,
        pitch_track=sample_pitch_track
    )

    print(result)