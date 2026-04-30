"""Quickly test which keys can do TTS and which can't."""
import os
import requests
from pathlib import Path
from dotenv import load_dotenv

load_dotenv(Path(__file__).resolve().parent / ".env")

VOICE_ID = os.getenv("ELEVEN_VOICE_ID", "").strip()

KEYS = [
    ("SUB",    os.getenv("ELEVEN_API_SUB", "").strip()),
    ("FREE_1", os.getenv("ELEVEN_API_FREE_1", "").strip()),
    ("FREE_2", os.getenv("ELEVEN_API_FREE_2", "").strip()),
    ("FREE_3", os.getenv("ELEVEN_API_FREE_3", "").strip()),
]

for label, key in KEYS:
    if not key:
        print(f"{label}: (no key set)")
        continue

    # 1. Quota
    try:
        r = requests.get(
            "https://api.elevenlabs.io/v1/user/subscription",
            headers={"xi-api-key": key}, timeout=20,
        )
        if r.status_code == 200:
            d = r.json()
            quota = f"{d['character_count']:,}/{d['character_limit']:,}"
            tier = d.get("tier", "unknown")
        else:
            quota = f"HTTP {r.status_code}"
            tier = "?"
    except Exception as e:
        quota = f"ERR {e}"
        tier = "?"

    # 2. TTS permission test (1 char)
    try:
        r = requests.post(
            f"https://api.elevenlabs.io/v1/text-to-speech/{VOICE_ID}",
            headers={"xi-api-key": key, "Content-Type": "application/json"},
            json={"text": "ஒம்", "model_id": "eleven_multilingual_v2"},
            timeout=30,
        )
        if r.status_code == 200:
            tts_perm = "OK"
        elif r.status_code == 401:
            tts_perm = "MISSING text_to_speech permission"
        else:
            tts_perm = f"HTTP {r.status_code}: {r.text[:120]}"
    except Exception as e:
        tts_perm = f"ERR {e}"

    print(f"{label:6s}  tier={tier:10s}  quota={quota:>20s}  tts={tts_perm}")
