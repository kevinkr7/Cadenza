import asyncio
from main import compare_vocals
import os
import shutil
import numpy as np
import soundfile as sf
from fastapi import UploadFile
import io

async def test():
    # create dummy files
    session_id = "test_session"
    os.makedirs("uploads", exist_ok=True)
    
    # 1. create dummy vocal.wav
    t = np.linspace(0, 3, 22050 * 3, endpoint=False)
    sig = (0.4 * np.sin(2 * np.pi * 440 * t)).astype(np.float32)
    sf.write(f"uploads/{session_id}_vocals.wav", sig, 22050)
    
    # 2. create dummy user recording
    user_audio_path = f"uploads/{session_id}_user.wav"
    sf.write(user_audio_path, sig, 22050)
    
    with open(user_audio_path, "rb") as f:
        user_recording = UploadFile(filename="user.wav", file=io.BytesIO(f.read()))
    
    try:
        res = await compare_vocals(session_id=session_id, user_recording=user_recording)
        print("SUCCESS")
    except Exception as e:
        import traceback
        traceback.print_exc()

asyncio.run(test())
