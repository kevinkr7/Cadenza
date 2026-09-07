import os
import sounddevice as sd
import soundfile as sf


OUTPUT_FOLDER = "uploads"
OUTPUT_FILE = "my_voice_test.wav"

SAMPLE_RATE = 22050
DURATION_SECONDS = 5
CHANNELS = 1


def record_voice():
    """
    Records the user's voice from the microphone and saves it as a WAV file.

    This is used only for backend testing.
    Later, the Android app will record and upload audio directly.
    """

    os.makedirs(OUTPUT_FOLDER, exist_ok=True)

    output_path = os.path.join(OUTPUT_FOLDER, OUTPUT_FILE)

    print("=" * 60)
    print("Cadenza Voice Recording Test")
    print("=" * 60)
    print()
    print(f"Recording duration: {DURATION_SECONDS} seconds")
    print(f"Sample rate: {SAMPLE_RATE} Hz")
    print(f"Output file: {output_path}")
    print()
    print("Instructions:")
    print("1. Sit in a quiet place.")
    print("2. Keep your mouth around 15-20 cm from the microphone.")
    print("3. Sing one steady note using 'Aaah'.")
    print("4. Try not to change the note too much.")
    print()
    input("Press ENTER when you are ready to start recording...")

    print()
    print("Recording started. Sing now...")

    audio = sd.rec(
        int(DURATION_SECONDS * SAMPLE_RATE),
        samplerate=SAMPLE_RATE,
        channels=CHANNELS,
        dtype="float32"
    )

    sd.wait()

    sf.write(
        output_path,
        audio,
        SAMPLE_RATE
    )

    print()
    print("Recording completed.")
    print(f"Saved file: {output_path}")
    print()
    print("Now upload this file to FastAPI /analyze:")
    print("http://127.0.0.1:8000/docs")


if __name__ == "__main__":
    record_voice()