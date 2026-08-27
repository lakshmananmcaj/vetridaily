# VetriDaily — Product & Revenue Blueprint

*Drafted 26 Aug 2026. Recovered from artifact `3768d371` and committed here so it stays.*
*App: `com.murugan.dailycalm` · v1.0.1*

> **You already built the factory. The app is the only node not plugged in.**
>
> InformationNeeds.com generates the content. SocialMediaTool turns it into 4–5 Tamil Shorts a day.
> The YouTube channel reaches devotees. VetriDaily — the one piece that can actually charge money —
> receives nothing from any of it. This plan wires it in, then monetizes it.

**State at drafting:** 70 days of content, all audio published, then a cliff · 0 API requests,
Supabase flagged for pause · 30 temples seeded, all 6 Arupadai Veedu in Tamil · 4–5 Shorts published
daily, already automated.

---

## Part 1 — What VetriDaily can show

Grouped by what it costs you. Tier A is live today. Tiers B and C are content you already own and
already pay to produce — the app simply isn't reading it. Only Tier D needs new creative work.

### Tier A — Live in the app today

| Content | Source | Status |
|---|---|---|
| Daily affirmation — அருள் வாக்கு, Tamil title + body | Supabase `daily_content` | Live · 70 days |
| Daily audio — TTS stitched with `common.mp3` | ElevenLabs → Supabase Storage | Live · 70/70 |
| Day progression & unlock mechanic | `DayProgressStore` | Uncapped bug |
| Daily reminder notification | WorkManager | Live |
| Share card image | `ShareUtils.kt` | Uncommitted |

### Tier B — Ready from informationneeds.com (wiring only, no new content)

| Content | Endpoint / source | Value to app |
|---|---|---|
| இன்றைய ராசி பலன் — today's horoscope | `spiritual/horoscope/today` | Daily return reason |
| Weekly · monthly · yearly horoscope | `horoscope/weekly\|monthly\|yearly` | Depth · pack material |
| பஞ்சாங்கம் — panchangam, Rahu Kalam, Nalla Neram | `PanchangamCalculatorService` | Utility hook |
| Festivals — Tamil names, `UpcomingDates`, muhurtham start/end, long-form Tamil detail, featured image | `festivals/upcoming` · `festivals/{slug}` | Countdown · audio · share |
| 30 temples — 6 Arupadai Veedu with Tamil history, GPS, timings, YouTube IDs | `seed-temples-tamilnadu.sql` | **The wedge** |
| விரத நாட்கள் — Sashti, Pradosham, Ekadashi, Amavasai, Pournami, Chaturthi, with tithi and paksha | `special-dates` · `special-dates/yearly` | Recurring retention |
| Chandrashtama · Girivalam · muhurtham · property & vehicle purchase days | `spiritual/*` | High-intent dates |
| Birth chart · marriage porutham · numerology | `birth-chart`, `marriage-porutham` | Paid tools |
| Baby names (Murugan names) | BabyNames DTOs | Cross-sell |
| Articles & web stories | Articles / WebStory API | Portal traffic |
| 6 finished PDFs incl. Temple Travel Guide (Arupadai Veedu) | `bin/Release/DigitalProducts/` | Sell as-is |

### Tier C — Ready from the YouTube automation

| Content | Source | Bridge |
|---|---|---|
| `ContentPlanItem` pool — AI-written Tamil அருள் வாக்கு, with `IsUsed` marking surplus | SocialMediaTool DB | Maps 1:1 → `daily_content` |
| Series already defined: `daily`, `porri`, `thiruppugazh`, `temple` | `FestivalVideosController` | Become paid packs |
| Shorts library, growing 4–5/day | YouTube · Murugan devotee | Embed + install funnel |

### Tier D — Worth creating new

| Content | Why | Effort |
|---|---|---|
| Kanda Sashti Kavasam with Tamil meaning | Most-recited Murugan hymn; free versions never explain the meaning | Medium |
| வெற்றி prayer packs — exams, job, marriage, health | Highest purchase intent, and literally your brand name | Medium |
| Arupadai Veedu offline audio guide | Works at the temple with no signal; pilgrims pay for this | Large |
| Personalized sankalpam audio — name, nakshatram, gothram spoken aloud | Uncopyable by any calendar app or YouTube channel | Large |

---

## Part 2 — The screen plan

Four tabs across the bottom. Everything new is added **around** the affirmation, never in front of it.

> **The daily affirmation stays exactly where it is.** It is the whole reason the app isn't a
> calendar. Nithra and Om Tamil Calendar are reference tools: open, check a date, close. VetriDaily
> is a *practice* — the day card, the Tamil body text, the audio, the streak. That screen is the
> product. The new tabs exist to give people a reason to return after day 70, not to replace day one.

| இன்று · Today | கோவில் · Temples | பஞ்சாங்கம் · Panchangam | மேலும் · More |
|---|---|---|---|
| Day N card — Tamil title + body | Arupadai Veedu — the six, as a journey | Full daily panchangam | Packs & store |
| Play / pause audio | All 30 temples, filter by district | விரத நாட்கள் — Sashti first | Remove ads |
| Day navigation ‹ › | Tamil history & significance | Pradosham · Ekadashi · Pournami | Reminder settings |
| Share card | Timings, how to reach, map | Festival calendar, month view | Watch on YouTube → |
| + அடுத்த சஷ்டி countdown | Linked YouTube video | Horoscope — daily to yearly | Read more on the portal → |
| + Rahu Kalam strip | Yatra Guide PDF upsell | | |
| + Your rasi today | | | |
| + Next festival countdown | | | |

**One rule about the day lock.** Today stays gated — day N unlocks on day N, because that scarcity is
what builds the habit. The other three tabs are always open. They're utility, not practice, so
locking them would only punish people. This is also what fixes the day-71 cliff: the journey can
complete gracefully while the app still has daily reasons to be opened.

---

## Part 3 — How it makes money

Four layers, ordered by what actually pays first at your scale — the reverse of the order most
people build them in.

### Layer 1 — In-app purchases → *the real revenue early*

One-time unlocks, not subscriptions. Tamil devotional audiences resist recurring charges, and a
subscription obliges you to keep shipping content forever to justify it.

| Product | Price | Note |
|---|---:|---|
| Remove ads | ₹99 | Typically the highest converter in India |
| Content pack — Thiruppugazh / Porri / Temple | ₹99–199 | Series already defined in your automation |
| Digital guides (6 PDFs) | ₹49–149 | Already generated and sitting on disk |
| வெற்றி prayer packs — exam, job, marriage | ₹99 | Seasonal spikes around exam results |
| Personalized sankalpam audio | ₹199–499 | Nobody else can offer this |

Implementation: `com.android.billingclient:billing-ktx`, one-time products, entitlements cached
locally and re-verified on launch.

### Layer 2 — Ads → *the floor, not the plan*

Honest math first. Indian eCPM runs roughly **$0.50–$2.00** overall. Banners are weakest
($0.20–0.80 globally), interstitials mid ($2.50–5.00), rewarded video strongest ($10–22 globally) —
and India sits well below tier-1 rates in every one. At a few hundred daily users this is pocket
change. It becomes real only after the funnel works, which is why it sits below IAP.

**Rewarded is the format that fits India best** — people will happily watch 30 seconds to unlock
something rather than pay, and it never interrupts anyone who didn't opt in.

- Native ad inside list screens — temples, festivals, articles
- Rewarded: watch to unlock one premium item or tomorrow's audio early
- Banner pinned to the bottom of the Panchangam tab
- **Never** on the Today affirmation card — that screen is the product
- **Never** an interstitial during or around audio playback

#### Enabling AdMob — the actual checklist

1. **Create the AdMob account and add the app** — link it to the same Play Console account that owns `com.murugan.dailycalm`
2. **Create ad units** — one Native, one Rewarded, one Banner; each returns a unit ID
3. **Add the dependency** — `implementation("com.google.android.gms:play-services-ads:24.x")`
4. **Declare the app ID in the manifest** — `<meta-data android:name="com.google.android.gms.ads.APPLICATION_ID" android:value="ca-app-pub-…~…"/>`
5. **Initialise the SDK** — `MobileAds.initialize(context)` once at startup, off the main thread
6. **Render in Compose** — wrap `AdView` in an `AndroidView`; load rewarded via `RewardedAd.load(…)`
7. **Add the UMP consent SDK** — required for EEA visitors and the right default under India's DPDP Act; consent must resolve before the first ad request
8. **Host `app-ads.txt` on informationneeds.com** — set it as your Play Console developer website and publish the file at the root; protects your ad spend from spoofing, costs nothing
9. **Use Google's test unit IDs while developing** — tapping your own live ads is the fastest way to get banned
10. **Declare it in Play Console** — tick "Contains ads" and update Data Safety before shipping

### Layer 3 — Cross-traffic to the portal → *monetizes the same reader twice*

Every temple, festival and article detail screen gets a **மேலும் படிக்க** link into
informationneeds.com. One devotee then produces an app session *and* a portal pageview. Your web
stories and AMP stories are already built for exactly this inbound traffic.

The return leg already exists — there's a **Get App** button in the portal header. Point it at
VetriDaily and every visitor to the முக்கியமான நாட்கள் page becomes a potential install, from an
audience that arrived already looking up vratham dates.

### Layer 4 — The YouTube funnel → *the piece that fixes zero users*

Shorts pay very little per view. But you publish 4–5 a day to an audience of Tamil Murugan devotees,
and that is a warm, self-renewing install channel no calendar competitor can take from you. One
converted viewer buying a ₹199 unlock outweighs an enormous number of Shorts impressions.

- Pinned comment + description link to the Play Store on every Short
- End-screen card on every upload
- One thing that exists **only** in the app, and say so on the channel

---

## Part 4 — Wiring the content in

```
InformationNeeds API                SocialMediaTool                 YouTube Shorts
Panchangam · horoscope    ──→       ContentGenerationJob    ──→     4–5 published daily
temples · festivals                 Hangfire
        │                                   │
        │ B · Direct API reads              │ A · ContentPlanItem bridge
        │ live panchangam & horoscope       │ DayNumber→day · Title→title · Description→body
        ↓                                   ↓
                        VetriDaily  (currently receives nothing)
```

### Integration A — surplus content becomes app days

`ContentPlanItem` already carries `DayNumber` (1–30), `Title`, `Description`, and an `IsUsed` flag.
That is very nearly the schema of `daily_content`. Import the unused items as rows, and
`publish_daily.py` attaches audio on its next nightly run with no further work. **The app stops
running out of content permanently, at zero marginal cost.**

### Integration B — read live data directly

Panchangam and horoscope change every day and need no audio, so copying them into Supabase would only
create drift. The app should call the InformationNeeds API for those. Keep Supabase for the daily
practice content, where audio has to be attached and offline playback matters.

### Integration C — festivals, the highest-leverage feed

`FestivalDetailDto` ships `FestivalNameTamil`, a long-form `FestivalDetail`, Tamil short descriptions,
a `FeaturedImage`, and a first-class `UpcomingDates` array — with `StartingTime` and `EndTime` on
every date. That last pair is the **muhurtham window**, the single most-checked number on a festival day.

Because the detail text is already Tamil prose, it drops straight into `publish_daily.py` — meaning
festival days can get their own narrated audio through the pipeline you already run, with no new tooling.

| Field | Becomes |
|---|---|
| `FestivalNameTamil` + `UpcomingDates` | Countdown card on the Today tab |
| `StartingTime` / `EndTime` | Muhurtham window — the reason people open the app that morning |
| `FestivalDetail` (Tamil prose) | Festival-day audio via the existing TTS pipeline |
| `FeaturedImage` + Tamil name | Festival share card — the most-forwarded format in Tamil WhatsApp groups |
| `festivals/upcoming` | Scheduled push notification, days ahead |

### Integration D — சஷ்டி, the recurring hook you already compute

`SpecialDatesCalculatorService` returns Sashti as a first-class list — monthly and yearly, each entry
carrying date, weekday, tithi number and paksha, cached 24h at `spiritual/special-dates`. **It is
already built, already served, and nothing consumes it.**

Sashti is Murugan's vratham day, and it comes around twice a month. That is the difference between a
festival feature and a retention feature: **Thai Poosam earns one app open a year, but Sashti earns
twenty-plus** — and unlike panchangam or horoscope, it belongs to your deity specifically. No general
Tamil calendar treats it as more than a row in a table. For VetriDaily it is the natural second ritual
after the daily affirmation: a countdown, a vratham guide, and its own narrated audio on the day.

கந்த சஷ்டி then becomes the annual peak — six days of observance, the biggest Murugan event of the
year, and the obvious anchor for a paid pack released ahead of it. Pradosham, Ekadashi, Amavasai and
Pournami come free in the same response.

**Festivals are your install spikes.** Kanda Sashti, Thai Poosam and Panguni Uthiram are predictable,
dated and emotionally charged — and your channel already publishes festival Shorts. Pair a scheduled
push with a shareable festival card and each festival becomes an acquisition event you can plan
months ahead, rather than a date that quietly passes.

---

## Part 5 — Build order

These are genuinely sequential. Shipping ads or billing before the funnel exists means monetizing an
audience of nobody.

### Phase 0 — Stop the bleeding
Small, contained, unblocks everything else.
- Cap day progression; add a real completion screen instead of an error
- Separate "no content for this day" from "network failed"
- Keep Supabase alive and re-enable the disabled GitHub cron
- Commit the half-finished share feature

### Phase 1 — Content bridge
`ContentPlanItem` → `daily_content` importer. The app never runs dry again, and the paywall gets
something real to sell.

### Phase 2 — Screen expansion
Bottom navigation; Today extended with Rahu Kalam, rasi and festival countdown; Temples tab built on
the Arupadai Veedu data; Panchangam tab.

### Phase 3 — Funnel
YouTube → Play Store links on every Short, finished share cards, and app → portal deep links.
**This is the phase that creates the users the next phase charges.**

### Phase 4 — Revenue
Play Billing with remove-ads and packs first, then AdMob under the placement rules above.

---

## Part 6 — Decisions (RESOLVED 2026-08-26)

All five answered by the user. These are settled — build to them.

**1. Direct API, not a Supabase proxy.** ✅ **DECIDED: direct.**
The app calls `api.informationneeds.com` for panchangam, horoscope, festivals, temples and special
dates. Supabase keeps only the daily practice content, where audio is attached and offline playback
matters. Consequences to handle in Phase 2:
- The API is served over **HTTPS**, so no `usesCleartextTraffic` exception is needed (Android 9+
  blocks plain HTTP by default)
- Review rate limits for mobile traffic — one app open can fan out to several endpoints
- Cache aggressively client-side: festivals and temples change rarely, panchangam changes daily

**2. The 70-day journey stays gated.** ✅ **DECIDED: yes, keep the daily unlock.**
It is the habit mechanic and the reason this isn't a calendar. With the Phase 1 content bridge
feeding it, the journey continues past 70 instead of ending.

**3. Free/paid boundary: sell depth, not access.** ✅ **DECIDED.**
All daily content stays free. Revenue comes from packs and remove-ads. Gating the daily practice
would kill the habit that makes people willing to buy anything.

**4. informationneeds.com runs AdSense.** ✅ **CONFIRMED: yes.**
This **promotes Layer 3 (portal cross-traffic)**. Every temple, festival and article detail screen
gets a மேலும் படிக்க link into the portal, and each tap earns AdSense revenue immediately — with no
Play Billing, no AdMob setup, no consent SDK, and no Play Console declaration. It is the fastest
revenue path in the entire plan and should ship **during Phase 2**, not wait for Phase 4.

**5. App is uploaded to Play Console in TESTING, not production.** ✅ **CONFIRMED.**
This **de-escalates Phase 0** — there are no public users sitting on a day-71 error screen. It also
means the fixes already committed in `58ab519` reach testers on the next testing-track release.
The window before public launch is the right time to land Phase 2, so the app launches with four
tabs rather than one screen.

### What the answers changed

| Decision | Effect on the plan |
|---|---|
| AdSense = yes | Portal links move **up** — ship in Phase 2, earn before Play Billing exists |
| Testing, not live | Phase 0 urgency **drops**; more room to build Phase 2 before public launch |
| Direct API | No proxy layer to build; needs a second Retrofit client + rate-limit review |

---

## Companion documents

- `VETRIDAILY_GROWTH_PLAN.md` — revenue streams and build order (partially overlaps this)
- `FESTIVAL_FEATURE_SPEC.md` — festival screens, festival→product map, revenue math
- `PLAY_STORE_RELEASE_GUIDE.md` — release process
