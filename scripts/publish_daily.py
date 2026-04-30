"""
VetriDaily — daily audio publisher (runs in GitHub Actions).

Picks the next row in `daily_content` where `audio_url IS NULL`,
generates audio via ElevenLabs, stitches the common closing line,
uploads to Supabase Storage, and writes the public URL back to the row.

Required env vars:
  ELEVEN_API_KEY
  ELEVEN_VOICE_ID
  SUPABASE_URL                 (e.g. https://xxxx.supabase.co)
  SUPABASE_SERVICE_ROLE_KEY
"""

import os
import sys
import subprocess
import tempfile
from pathlib import Path

import requests

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")


def env(name: str) -> str:
    val = os.environ.get(name, "").strip()
    if not val:
        print(f"ERROR: missing env var {name}")
        sys.exit(1)
    return val


ELEVEN_API_KEY = env("ELEVEN_API_KEY")
ELEVEN_VOICE_ID = env("ELEVEN_VOICE_ID")
SUPABASE_URL = env("SUPABASE_URL").rstrip("/")
SERVICE_ROLE_KEY = env("SUPABASE_SERVICE_ROLE_KEY")

ELEVEN_MODEL = os.environ.get("ELEVEN_MODEL", "eleven_multilingual_v2").strip()
STABILITY = float(os.environ.get("ELEVEN_STABILITY", "0.5"))
SIMILARITY = float(os.environ.get("ELEVEN_SIMILARITY", "0.75"))
BUCKET = os.environ.get("SUPABASE_BUCKET", "audio").strip()

SCRIPT_DIR = Path(__file__).resolve().parent
COMMON_MP3 = SCRIPT_DIR / "assets" / "common.mp3"
WORK_DIR = Path(tempfile.gettempdir()) / "vetridaily"
WORK_DIR.mkdir(parents=True, exist_ok=True)

SB_HEADERS = {
    "apikey": SERVICE_ROLE_KEY,
    "Authorization": f"Bearer {SERVICE_ROLE_KEY}",
}


def fetch_next_pending() -> dict | None:
    """Return the lowest-day row in daily_content where audio_url IS NULL."""
    r = requests.get(
        f"{SUPABASE_URL}/rest/v1/daily_content",
        headers=SB_HEADERS,
        params={
            "select": "id,day,title,body,audio_url",
            "audio_url": "is.null",
            "order": "day.asc",
            "limit": "1",
        },
        timeout=30,
    )
    r.raise_for_status()
    rows = r.json()
    return rows[0] if rows else None


def tts(text: str, out_path: Path) -> None:
    r = requests.post(
        f"https://api.elevenlabs.io/v1/text-to-speech/{ELEVEN_VOICE_ID}",
        headers={"xi-api-key": ELEVEN_API_KEY, "Content-Type": "application/json"},
        json={
            "text": text,
            "model_id": ELEVEN_MODEL,
            "voice_settings": {"stability": STABILITY, "similarity_boost": SIMILARITY},
        },
        timeout=180,
    )
    if r.status_code != 200:
        raise RuntimeError(f"ElevenLabs error {r.status_code}: {r.text[:300]}")
    out_path.write_bytes(r.content)


def stitch(daily_mp3: Path, final_mp3: Path) -> None:
    list_file = final_mp3.with_suffix(".concat.txt")
    list_file.write_text(
        f"file '{daily_mp3.as_posix()}'\nfile '{COMMON_MP3.as_posix()}'\n",
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


def upload_to_storage(mp3_path: Path, day: int) -> str:
    object_name = f"day-{day}.mp3"
    url = f"{SUPABASE_URL}/storage/v1/object/{BUCKET}/{object_name}"
    headers = {
        **SB_HEADERS,
        "Content-Type": "audio/mpeg",
        "x-upsert": "true",
    }
    data = mp3_path.read_bytes()
    r = requests.post(url, headers=headers, data=data, timeout=180)
    if r.status_code in (200, 201):
        pass
    elif r.status_code == 409:  # already exists, overwrite via PUT
        r = requests.put(url, headers=headers, data=data, timeout=180)
        r.raise_for_status()
    else:
        raise RuntimeError(f"Upload failed {r.status_code}: {r.text[:300]}")

    return f"{SUPABASE_URL}/storage/v1/object/public/{BUCKET}/{object_name}"


def update_row(row_id: int, audio_url: str) -> None:
    r = requests.patch(
        f"{SUPABASE_URL}/rest/v1/daily_content?id=eq.{row_id}",
        headers={
            **SB_HEADERS,
            "Content-Type": "application/json",
            "Prefer": "return=minimal",
        },
        json={"audio_url": audio_url},
        timeout=30,
    )
    r.raise_for_status()


def get_eleven_quota() -> tuple[int, int]:
    r = requests.get(
        "https://api.elevenlabs.io/v1/user/subscription",
        headers={"xi-api-key": ELEVEN_API_KEY},
        timeout=30,
    )
    r.raise_for_status()
    d = r.json()
    return int(d["character_count"]), int(d["character_limit"])


def main() -> None:
    if not COMMON_MP3.exists():
        print(f"ERROR: missing common.mp3 at {COMMON_MP3}")
        sys.exit(1)

    row = fetch_next_pending()
    if not row:
        print("No pending rows in daily_content. Nothing to do.")
        return

    day = row["day"]
    title = row.get("title") or "(no title)"
    body = row["body"]
    print(f"Processing day {day}: {title}")
    print(f"  body chars: {len(body)}")

    daily_only = WORK_DIR / f"day-{day}-only.mp3"
    final = WORK_DIR / f"day-{day}.mp3"

    print("  generating TTS...")
    tts(body, daily_only)

    print("  stitching with common.mp3...")
    stitch(daily_only, final)

    print("  uploading to Supabase Storage...")
    public_url = upload_to_storage(final, day)

    print(f"  updating DB row id={row['id']}...")
    update_row(row["id"], public_url)

    used, limit = get_eleven_quota()
    print(f"\n✓ Day {day} published")
    print(f"  audio_url: {public_url}")
    print(f"  ElevenLabs quota: {used:,}/{limit:,} ({limit - used:,} remaining)")


if __name__ == "__main__":
    main()
