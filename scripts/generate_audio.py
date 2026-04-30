"""
VetriDaily — daily audio generator.

Reads day{N}.txt scripts from content/, generates ElevenLabs TTS audio,
stitches the common closing line, and outputs final MP3s to test-audio/.

Rotation: each day uses a different free key, so each free account uses
~1.5k chars/audio out of its 10k monthly quota.
"""

import os
import sys
import subprocess
from pathlib import Path
from typing import Tuple

import requests
from dotenv import load_dotenv

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")

SCRIPT_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = SCRIPT_DIR.parent
CONTENT_DIR = SCRIPT_DIR / "content"
OUTPUT_DIR = PROJECT_ROOT / "test-audio"
OUTPUT_DIR.mkdir(exist_ok=True)

load_dotenv(SCRIPT_DIR / ".env")

VOICE_ID = os.getenv("ELEVEN_VOICE_ID", "").strip()
MODEL = os.getenv("ELEVEN_MODEL", "eleven_multilingual_v2").strip()
STABILITY = float(os.getenv("ELEVEN_STABILITY", "0.5"))
SIMILARITY = float(os.getenv("ELEVEN_SIMILARITY", "0.75"))

SUB_KEY = ("SUB", os.getenv("ELEVEN_API_SUB", "").strip())
FREE_KEYS = [
    ("FREE_1", os.getenv("ELEVEN_API_FREE_1", "").strip()),
    ("FREE_2", os.getenv("ELEVEN_API_FREE_2", "").strip()),
    ("FREE_3", os.getenv("ELEVEN_API_FREE_3", "").strip()),
]

# USE_ROTATION = True when free keys have text_to_speech permission.
# Set to False to use only SUB key (Starter tier, 30k chars/month is plenty).
USE_ROTATION = False
ACTIVE_KEYS = [SUB_KEY] if not USE_ROTATION else FREE_KEYS

ELEVEN_BASE = "https://api.elevenlabs.io/v1"


def parse_script(path: Path) -> Tuple[str, str]:
    text = path.read_text(encoding="utf-8")
    if "---" in text:
        head, body = text.split("---", 1)
        title = head.replace("TITLE:", "").strip()
        return title, body.strip()
    return "", text.strip()


def tts(api_key: str, text: str, out_path: Path) -> None:
    url = f"{ELEVEN_BASE}/text-to-speech/{VOICE_ID}"
    headers = {"xi-api-key": api_key, "Content-Type": "application/json"}
    payload = {
        "text": text,
        "model_id": MODEL,
        "voice_settings": {
            "stability": STABILITY,
            "similarity_boost": SIMILARITY,
        },
    }
    resp = requests.post(url, headers=headers, json=payload, timeout=120)
    if resp.status_code != 200:
        raise RuntimeError(
            f"ElevenLabs error {resp.status_code}: {resp.text[:300]}"
        )
    out_path.write_bytes(resp.content)


def get_quota(api_key: str) -> Tuple[int, int]:
    resp = requests.get(
        f"{ELEVEN_BASE}/user/subscription",
        headers={"xi-api-key": api_key},
        timeout=30,
    )
    resp.raise_for_status()
    data = resp.json()
    return int(data["character_count"]), int(data["character_limit"])


def stitch(daily_mp3: Path, common_mp3: Path, final_mp3: Path) -> None:
    list_file = final_mp3.with_suffix(".concat.txt")
    list_file.write_text(
        f"file '{daily_mp3.as_posix()}'\nfile '{common_mp3.as_posix()}'\n",
        encoding="utf-8",
    )
    try:
        subprocess.run(
            [
                "ffmpeg", "-y", "-loglevel", "error",
                "-f", "concat", "-safe", "0",
                "-i", str(list_file),
                "-c", "copy", str(final_mp3),
            ],
            check=True,
        )
    finally:
        list_file.unlink(missing_ok=True)


def ensure_common_audio() -> Path:
    common_txt = CONTENT_DIR / "common.txt"
    common_mp3 = OUTPUT_DIR / "common.mp3"
    if common_mp3.exists():
        print(f"common.mp3 already exists — reusing.")
        return common_mp3

    text = common_txt.read_text(encoding="utf-8").strip()
    label, key = ACTIVE_KEYS[0]
    print(f"Generating common.mp3  ({len(text)} chars, key={label})")
    tts(key, text, common_mp3)
    print(f"  -> {common_mp3}")
    return common_mp3


def generate_day(day_path: Path, key_label: str, key: str, common_mp3: Path) -> int:
    title, body = parse_script(day_path)
    stem = day_path.stem  # e.g. "day1"
    daily_only = OUTPUT_DIR / f"{stem}_only.mp3"
    final = OUTPUT_DIR / f"{stem}.mp3"

    print(f"\n[{stem}]  title: {title or '(none)'}")
    print(f"  chars: {len(body)}  key: {key_label}")
    tts(key, body, daily_only)
    print(f"  generated daily-only mp3, stitching with common...")
    stitch(daily_only, common_mp3, final)
    daily_only.unlink(missing_ok=True)
    print(f"  -> {final}")
    return len(body)


def print_quotas() -> None:
    print("\n--- Quota usage ---")
    for label, key in [SUB_KEY] + FREE_KEYS:
        if not key:
            print(f"  {label}: (no key set)")
            continue
        try:
            used, limit = get_quota(key)
            remaining = limit - used
            print(f"  {label:6s}: {used:>7,} / {limit:>7,}   remaining: {remaining:>7,}")
        except Exception as e:
            print(f"  {label}: ERROR fetching quota — {e}")


def validate_env() -> None:
    missing = []
    if not VOICE_ID:
        missing.append("ELEVEN_VOICE_ID")
    for label, key in ACTIVE_KEYS:
        if not key or key.startswith("PASTE_") or key.startswith("paste_"):
            missing.append(f"ELEVEN_API_{label}")
    if missing:
        print("ERROR: missing or unset values in .env:")
        for m in missing:
            print(f"  - {m}")
        sys.exit(1)


def main() -> None:
    validate_env()
    common_mp3 = ensure_common_audio()
    day_files = sorted(CONTENT_DIR.glob("day*.txt"))
    if not day_files:
        print("No day*.txt files found in content/")
        return

    for i, day_path in enumerate(day_files):
        label, key = ACTIVE_KEYS[i % len(ACTIVE_KEYS)]
        try:
            generate_day(day_path, label, key, common_mp3)
        except Exception as e:
            print(f"  FAILED for {day_path.name}: {e}")

    print_quotas()


if __name__ == "__main__":
    main()
