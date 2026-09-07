import math


NOTE_NAMES = [
    "C", "C#", "D", "D#",
    "E", "F", "F#", "G",
    "G#", "A", "A#", "B"
]


def frequency_to_midi(frequency: float) -> int:
    if frequency <= 0:
        raise ValueError("Frequency must be greater than 0")

    midi_number = 69 + 12 * math.log2(frequency / 440.0)
    return round(midi_number)


def midi_to_note_name(midi_number: int) -> str:
    note_name = NOTE_NAMES[midi_number % 12]
    octave = (midi_number // 12) - 1

    return f"{note_name}{octave}"


def midi_to_frequency(midi_number: int) -> float:
    frequency = 440.0 * (2 ** ((midi_number - 69) / 12))
    return frequency


def calculate_cents_deviation(actual_frequency: float, ideal_frequency: float) -> float:
    if actual_frequency <= 0 or ideal_frequency <= 0:
        raise ValueError("Frequencies must be greater than 0")

    cents = 1200 * math.log2(actual_frequency / ideal_frequency)
    return cents


def get_pitch_status(cents_deviation: float) -> str:
    if cents_deviation < -30:
        return "flat"

    elif -30 <= cents_deviation < -10:
        return "slightly flat"

    elif -10 <= cents_deviation <= 10:
        return "in tune"

    elif 10 < cents_deviation <= 30:
        return "slightly sharp"

    else:
        return "sharp"


def map_frequency_to_note(frequency: float) -> dict:
    midi_number = frequency_to_midi(frequency)
    note_name = midi_to_note_name(midi_number)
    ideal_frequency = midi_to_frequency(midi_number)
    cents_deviation = calculate_cents_deviation(frequency, ideal_frequency)
    pitch_status = get_pitch_status(cents_deviation)

    return {
        "frequency": round(frequency, 2),
        "midi_number": midi_number,
        "note": note_name,
        "ideal_frequency": round(ideal_frequency, 2),
        "deviation_cents": round(cents_deviation, 2),
        "status": pitch_status
    }


if __name__ == "__main__":
    test_frequencies = [
        440.0,
        438.0,
        445.0,
        261.63,
        329.63,
        392.00
    ]

    for freq in test_frequencies:
        result = map_frequency_to_note(freq)
        print(result)