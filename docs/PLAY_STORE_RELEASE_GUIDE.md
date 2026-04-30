# VetriDaily Play Store Release Guide

## 1) Play Store App Icon (512x512) Checklist

Use this for the **Play Store listing icon** (not launcher icon).

- File format: `PNG`
- Size: `512 x 512 px`
- Color space: `sRGB`
- Background: no forced transparency tricks; keep clean solid/soft background
- Readability: icon must still be clear at small size
- Composition:
  - Prefer symbol-first (Vel mark) over full text
  - Avoid tiny tagline text (won't be readable)
  - Keep 10-15% safe margin around symbol
- Branding consistency:
  - Match in-app color palette (gold + deep blue)
  - Keep same symbol as launcher icon
- Export names:
  - `vetri_daily_play_icon_512.png`

Pre-upload checks:
- View at 100%, 50%, 25%
- Check on light and dark backgrounds
- Ask 3 people: "Can you identify this in 2 seconds?"

---

## 2) Feature Graphic Text + Screenshot Plan

### A) Feature Graphic (required)

- Size: `1024 x 500 px`
- Format: PNG/JPG (no alpha preferred)
- Suggested headline:
  - `VetriDaily`
  - `Victory through devotion. Every single day.`
- Suggested subtext:
  - `Daily Tamil devotion, affirmation, and guided calm audio`
- Visual layout:
  - Left: Vel symbol/logo
  - Right: short brand text
  - Keep clean negative space

### B) Screenshot Plan (phone)

Capture at least 6 screenshots in this order:

1. **Home + Day Card**
   - Caption: `Daily devotional path, one day at a time`
2. **Affirmation Content Card**
   - Caption: `Read today’s Tamil affirmation`
3. **Audio Controls**
   - Caption: `Play, pause, and replay guided calm audio`
4. **Day Navigation**
   - Caption: `Revisit previous days anytime`
5. **Reminder Settings**
   - Caption: `Set your daily devotional reminder`
6. **Brand + Calm UI**
   - Caption: `Simple, focused, distraction-free experience`

Screenshot quality rules:
- Use real device frames only if consistent
- Avoid debug text and dev placeholders
- Keep status bar clean (good time/battery/signal)
- Use same language style across all screenshots

---

## 3) First Release Checklist (AAB + Signing + Store Listing)

## A) Build + Signing

1. Create release keystore (once, secure backup)
2. Configure signing in Android Studio/Gradle release config
3. Build AAB:
   - `Build > Generate Signed Bundle / APK > Android App Bundle`
4. Save output AAB
5. Keep keystore + passwords in secure backup (critical)

## B) Play Console Setup

1. Create app in Play Console
2. Set default language and app name (`VetriDaily`)
3. Fill Store Listing:
   - App name
   - Short description
   - Full description
   - App icon (512x512)
   - Feature graphic (1024x500)
   - Screenshots
4. Add contact email + privacy policy URL
5. Complete:
   - Data Safety form
   - Content Rating questionnaire
   - Target audience/app category
   - Ads declaration (yes/no)

## C) Release Creation

1. Go to `Production` (or `Closed testing` first)
2. Create new release
3. Upload AAB
4. Add release notes
5. Review all warnings/errors
6. Start rollout

## D) Final Validation Before Submit

- App launches correctly
- Day logic works (Day unlock model)
- Audio works on common devices
- Notifications work (Android 13+ permission handled)
- App icon and brand name are correct everywhere
- No test/debug text visible to users
- `DEBUG_FORCE_MAX_UNLOCKED_DAY=0` for production build

---

## Optional: Closed Testing First (Recommended)

Before full production:
- Run with 20-50 users for 3-7 days
- Track crash-free sessions, reminder reliability, content loading
- Fix critical issues, then publish production rollout
