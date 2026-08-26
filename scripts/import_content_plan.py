"""
VetriDaily — content bridge (runs in GitHub Actions, before publish_daily.py).

Pulls unused ContentPlanItems from SocialMediaTool and appends them to Supabase
`daily_content` as new days with `audio_url` NULL, so publish_daily.py generates
the audio on its next run.

Items are deliberately NOT marked used in SocialMediaTool — the same content still
goes out as a YouTube Short. Re-import is prevented by `daily_content.source_item_id`,
so this script is safe to run every night.

Required env vars:
  SUPABASE_URL                 (e.g. https://xxxx.supabase.co)
  SUPABASE_SERVICE_ROLE_KEY
  SMT_API_URL                  (e.g. http://ustech-001-site3.mtempurl.com/api)
  SMT_EMAIL
  SMT_PASSWORD

Optional:
  SMT_CONTENT_TYPE             filter, default "reel"
  IMPORT_LIMIT                 max days to append per run, default 30
  IMPORT_MIN_BODY_CHARS        skip items with a shorter body, default 40
"""

import os
import sys

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


SUPABASE_URL = env("SUPABASE_URL").rstrip("/")
SERVICE_ROLE_KEY = env("SUPABASE_SERVICE_ROLE_KEY")
SMT_API_URL = env("SMT_API_URL").rstrip("/")
SMT_EMAIL = env("SMT_EMAIL")
SMT_PASSWORD = env("SMT_PASSWORD")

CONTENT_TYPE = os.environ.get("SMT_CONTENT_TYPE", "reel").strip()
IMPORT_LIMIT = int(os.environ.get("IMPORT_LIMIT", "30"))
MIN_BODY_CHARS = int(os.environ.get("IMPORT_MIN_BODY_CHARS", "40"))

SB_HEADERS = {
    "apikey": SERVICE_ROLE_KEY,
    "Authorization": f"Bearer {SERVICE_ROLE_KEY}",
}


def login() -> str:
    """Exchange the service account credentials for a JWT."""
    if SMT_API_URL.startswith("http://"):
        print("  WARNING: SMT_API_URL is plain HTTP — credentials are sent unencrypted.")

    r = requests.post(
        f"{SMT_API_URL}/auth/login",
        json={"email": SMT_EMAIL, "password": SMT_PASSWORD},
        timeout=30,
    )
    if r.status_code != 200:
        raise RuntimeError(f"Login failed {r.status_code}: {r.text[:300]}")

    token = r.json().get("token")
    if not token:
        raise RuntimeError("Login succeeded but no token was returned")
    return token


def fetch_unused_items(token: str) -> list[dict]:
    params = {}
    if CONTENT_TYPE:
        params["contentType"] = CONTENT_TYPE

    r = requests.get(
        f"{SMT_API_URL}/contentplans/unused-items",
        headers={"Authorization": f"Bearer {token}"},
        params=params,
        timeout=60,
    )
    r.raise_for_status()
    return r.json()


def fetch_last_day() -> int:
    """Highest day currently in daily_content, published or not."""
    r = requests.get(
        f"{SUPABASE_URL}/rest/v1/daily_content",
        headers=SB_HEADERS,
        params={"select": "day", "order": "day.desc", "limit": "1"},
        timeout=30,
    )
    r.raise_for_status()
    rows = r.json()
    return int(rows[0]["day"]) if rows else 0


def fetch_imported_item_ids() -> set[int]:
    """ContentPlanItem ids already bridged, so nothing is imported twice."""
    r = requests.get(
        f"{SUPABASE_URL}/rest/v1/daily_content",
        headers=SB_HEADERS,
        params={"select": "source_item_id", "source_item_id": "not.is.null"},
        timeout=30,
    )
    if r.status_code == 400:
        raise RuntimeError(
            "daily_content.source_item_id is missing — run "
            "scripts/supabase_add_source_item_id.sql against Supabase first."
        )
    r.raise_for_status()
    return {int(row["source_item_id"]) for row in r.json() if row.get("source_item_id") is not None}


def insert_rows(rows: list[dict]) -> None:
    r = requests.post(
        f"{SUPABASE_URL}/rest/v1/daily_content",
        headers={
            **SB_HEADERS,
            "Content-Type": "application/json",
            "Prefer": "return=minimal",
        },
        json=rows,
        timeout=60,
    )
    if r.status_code not in (200, 201, 204):
        raise RuntimeError(f"Insert failed {r.status_code}: {r.text[:300]}")


def main() -> None:
    token = login()

    items = fetch_unused_items(token)
    print(f"SocialMediaTool returned {len(items)} unused item(s) of type '{CONTENT_TYPE}'")

    already = fetch_imported_item_ids()
    last_day = fetch_last_day()
    print(f"  daily_content currently ends at day {last_day}; {len(already)} item(s) already bridged")

    candidates = []
    skipped_short = 0
    for item in items:
        item_id = item.get("id") or item.get("Id")
        if item_id is None or int(item_id) in already:
            continue

        title = (item.get("title") or item.get("Title") or "").strip()
        body = (item.get("description") or item.get("Description") or "").strip()

        if len(body) < MIN_BODY_CHARS:
            skipped_short += 1
            continue

        candidates.append(
            {
                "source_item_id": int(item_id),
                "title": title or f"Day {last_day + len(candidates) + 1}",
                "body": body,
            }
        )

    if skipped_short:
        print(f"  skipped {skipped_short} item(s) with a body under {MIN_BODY_CHARS} chars")

    if not candidates:
        print("Nothing new to import. Done.")
        return

    if len(candidates) > IMPORT_LIMIT:
        print(f"  capping this run at {IMPORT_LIMIT} of {len(candidates)} available item(s)")
        candidates = candidates[:IMPORT_LIMIT]

    rows = []
    for offset, candidate in enumerate(candidates, start=1):
        rows.append({**candidate, "day": last_day + offset})

    insert_rows(rows)

    first_day = rows[0]["day"]
    final_day = rows[-1]["day"]
    print(f"\n✓ Imported {len(rows)} day(s): {first_day}–{final_day}")
    print("  audio_url left NULL — publish_daily.py will voice them one per run.")


if __name__ == "__main__":
    main()
