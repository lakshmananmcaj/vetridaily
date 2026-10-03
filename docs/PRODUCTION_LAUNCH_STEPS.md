# VetriDaily — Production Launch, Step by Step

*Written 3 October 2026. Specific to this launch; the general first-release notes are in
`PLAY_STORE_RELEASE_GUIDE.md`.*

## Where you are right now

| | |
|---|---|
| Closed test | ✅ 14 days, 12 testers, complete |
| Production access application | ✅ submitted 3 Oct, 4:52 PM — Google emails within ~7 days |
| Production track | Inactive |
| Code | `versionCode 4` / `1.2.0`, pushed, 40 tests passing |
| Built `.aab` | ❌ not yet |
| Screenshots | ⚠️ padded WhatsApp set only; no shop shown |
| Store description | ❌ still written for the old 70-day app |

**Production access being granted does not publish the app.** Two separate waits: the access
application (now), then the release review (after you upload).

---

# Part 1 — Do these while waiting

## Step 1. Verify 1.2.0 on your own phone

Nothing in 1.2.0 has run on hardware. Install the debug or release build and check:

- [ ] All five **பூஜை பொருட்கள்** links open murugandevotee.com
- [ ] Share a card → logo is **circular**, content **centred**, footer reads `VetriDaily • @murugandevotee`
- [ ] The shared link opens the **store page**, not the tester opt-in page
- [ ] Four tabs still work; Tamil does not clip
- [ ] Temple **வழி காட்டு** opens Maps
- [ ] Festival detail shows readable Tamil, **not `<p>` tags**

Anything wrong here is far cheaper to fix now than after a rejected release.

## Step 2. Take fresh screenshots

From the 1.2.0 build, on your phone: **Power + Volume Down**.

Take these six:

1. **கோவில்** — அறுபடை வீடு, the six abodes *(lead with this)*
2. **பண்டிகை** — year tabs + முருகன் நாட்கள்
3. **Temple detail** — scrolled so **வழி காட்டு** is visible
4. **இன்று** — scrolled so both countdown cards are **fully** visible
5. **மேலும்** — scrolled to show **பூஜை பொருட்கள்**
6. **இன்று** — the affirmation card with Play Audio

**Transfer by USB or Google Drive — never WhatsApp.** WhatsApp recompresses and downscales every
image; that is why the existing set is 738px instead of your phone's native width.

Save to `playstore-assets/screenshots-v1.2.0/`.

> Play caps phone screenshots at **2:1**. A 1080×2340 phone screenshot is 2.17:1 and will be
> rejected. Either pad the width to 1170×2340, or crop a little off the height. The padded set
> already in that folder shows the approach.

## Step 3. Finish Policy → App content

Every item must be green before a production release can be submitted.

| Item | Answer |
|---|---|
| Content rating questionnaire | Complete it — devotional content, no sensitive categories |
| Data safety | You collect **no** personal data. The form is still mandatory. |
| Target audience | Adults. **Do not** tick any under-13 bracket — that triggers Families policy. |
| Ads | **No** — there are no ad SDKs |
| In-app purchases | **No** — the shop is external physical goods |
| Privacy policy URL | `https://murugandevotee.com/policies/privacy` (verified live) |

**Why "No" to in-app purchases is correct:** murugandevotee.com sells physical items — vel,
vilakku, idols. Play mandates Play Billing only for *digital* goods. Physical goods may use any
payment method, so Razorpay on the web is fine and Google takes nothing.

**The line not to cross:** if a PDF, ebook or downloadable audio is ever sold through those links,
it becomes a digital good and a Play Billing violation. Keep the shop physical-only.

## Step 4. Rewrite the store description

The current one describes a 70-day affirmation app. It does not mention the Arupadai Veedu,
the festival calendar, vratham days, or the shop. Rewrite before submitting.

## Step 5. Build the signed bundle

Wait for Gradle sync to finish, then **Build → Generate Signed App Bundle / APK**:

1. **Android App Bundle** (not APK — Play rejects APKs for new releases)
2. Key store: `D:\Projects\AndroidProjects\keystore\vetri_daily_release.jks`
3. Keystore password → key alias → key password
4. Variant: **release** → Create

Output: `app\release\app-release.aab`

> Build from Android Studio, not the command line. This machine has 7.7 GB RAM and
> `assembleRelease` runs out of memory under a fresh JVM. Android Studio reuses its warm daemon.
> See `project_build_memory_limits` in memory.

---

# Part 2 — When production access is granted

You get an email. Then:

## Step 6. Create the production release

**Production → Create new release**

1. Upload `app-release.aab`
2. Release name: `1.2.0` (internal only)
3. Release notes — see below
4. Rollout percentage: **start at 20%**, not 100%

> Staged rollout matters for a first launch. If a crash appears you halt it, having exposed a
> fifth of installs instead of all of them. Raise to 50% then 100% over a few days.

## Step 7. Release notes

```
<en-US>
VetriDaily 1.2.0

• Four sections: Today, Temples, Festivals, More
• அறுபடை வீடு — all six abodes with timings and directions
• Festival calendar by month with muhurat timings
• Countdown to the next Sashti and festival
• Optional evening reminder before festivals and Sashti
• Pooja items — vel, vilakku and idols
</en-US>
```

## Step 8. Submit and wait

First production reviews commonly take a few days and can take up to a week. You cannot speed it
up; avoid submitting changes mid-review, which restarts the clock.

---

# Part 3 — Once live

## Step 9. Switch the daily audio pipeline on

Still never triggered. `.github/workflows/daily_audio.yml` runs at 22:30 UTC (4 AM IST).

- Actions tab → **Generate Daily Audio** → **Run workflow** — test it manually first
- Watch the **"Import new days from the content plan"** step; `import_content_plan.py` has never
  executed anywhere
- All seven secrets are already set

## Step 10. Watch these for the first fortnight

| Where | What for |
|---|---|
| Play Console → Monitor and improve → Crashes and ANRs | Any crash at all, at 20% rollout |
| Vitals | Bad behaviour thresholds that can suppress your listing |
| Reviews | First reviews shape install rates disproportionately |

## Step 11. Verify `FestivalReminderWorker` actually fires

It has never been observed running. It checks at 7 PM whether tomorrow is a festival or Sashti.
The next Sashti after launch is the real test. If it throws, nobody finds out until then.

---

# Known items not blocking launch

- **Sashti shows once a month, not twice.** The backend samples tithi once daily at 6 AM IST and
  skips the தேய்பிறை occurrence. A website fix; the app will show both the moment the API does.
- **Three sources disagree** on September's Sashti date. Reconcile before people plan a vratham.
- **No tests** for the HTML stripper, share-card renderer, or the reminder worker — all need
  Robolectric.
- **Play Billing** deliberately deferred. Only needed if you ever sell digital goods.
- **R8 / minification** off. Enabling it shrinks the app but will break Gson reflection without
  keep rules. Do it deliberately, with a full test pass, not before a launch.
