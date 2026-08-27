# VetriDaily — Revenue & Growth Plan

*Written 26 August 2026. Everything cited here exists in your codebases today.*

Your app is a one-screen MVP with **no monetization at all**. But you already own a content API,
a seven-product digital store, push infrastructure, and a Murugan YouTube channel.
**The work isn't building content — it's wiring what you already have into the app.**

---

## What you already own

Every idea below is cheap because of this inventory. Nothing here needs to be built from
scratch — it needs a Retrofit interface and a screen.

| Asset | What it gives you |
|---|---|
| `festivals/*` endpoints | Upcoming, by month, by year, by slug. Full detail DTOs. Live today. |
| `SpecialDatesCalculatorService` | Amavasai, Pournami, Ekadashi, Pradosham, **Sashti**, Chaturthi |
| `PanchangamCalculatorService` | Daily panchangam, endpoint already public |
| 7 sellable PDFs | Temple Travel Guide TN, Tamil Panchangam 2026, Numerology, Vastu, Baby Names, Marriage Porutham, Graha |
| `PushNotificationService` | Already built — the single biggest lever on retention |
| Report services | `BirthChartCalculatorService`, `MarriagePoruthamService`, `HoroscopeReportService` |
| `GeminiService` | AI generation for festival copy, descriptions, video scripts |
| YouTube channel | Murugan devotional — currently zero connection to the app |

---

## Part A — Seven ways to earn

Ordered by effort-to-first-rupee, not by size of prize. The first three are reachable before you
launch; the last two need an audience first.

### 1. Sell the PDFs you already made

You have seven finished digital products sitting in a build folder. **Temple-Travel-Guide-TN** is
the obvious one — a Murugan devotee app whose audience wants to visit Arupadai Veedu is the exact
buyer for a Tamil Nadu temple guide. **Tamil-Panchangam-2026** sells itself next to a festival
calendar.

*Lowest effort · ₹99–₹199 one-time · Play Billing · No new content*

**To build**
- Google Play Billing Library — one-time products (`INAPP`, not `SUBS`)
- A simple store screen; deliver the PDF from your existing `DigitalStoreService`
- Gate the download behind a purchase token verified server-side

### 2. Contextual selling on festival days

A generic store is ignored. A store that appears *on the right day* converts. On Thai Poosam,
surface the temple guide. On the first day of the Tamil year, surface the panchangam. The festival
API tells you what day it is; the store already has the product. **This is the highest-leverage
idea in this document and it costs almost nothing.**

*Best ROI · Needs idea 1 + Part B · Timing-driven*

**To build**
- Map festival `slug` → product id in a small config table
- Show a single tasteful card on the festival's detail screen — never an interstitial
- Fire the matching push the evening before via `PushNotificationService`

### 3. Personalized reports as the premium tier

You have `BirthChartCalculatorService`, `MarriagePoruthamService` and `HoroscopeReportService`
already written. Personalized output is the one thing users pay real money for, because it can't be
copied or found free elsewhere. Marriage porutham in particular has a strong, urgent, family-driven
willingness to pay in Tamil households.

*Highest ticket · ₹199–₹499 · Backend done · Needs birth-detail form*

**To build**
- Birth-details input (date, time, place) with a place picker
- Purchase → call your report service → render + offer PDF export
- Store the chart locally so repeat reports skip re-entry

### 4. VetriDaily Plus subscription

Recurring revenue beats one-time, but only once there's enough in the app to be worth subscribing
to. Bundle: full daily-audio archive, festival reminders, complete panchangam, ad-free, early
access. Price low and annual — devotional audiences renew well when the price is close to a temple
offering.

*Recurring · ₹49/mo or ₹399/yr · Needs depth first*

**To build**
- Play Billing subscriptions + server-side entitlement check
- Free tier keeps today's content; Plus unlocks the archive
- Annual plan priced at ~8 months to push yearly commitment

### 5. Native ads — carefully, or not at all

AdMob is the fastest money to wire up and the easiest way to cheapen a devotional app. If you do it:
native ads only, placed *below* the day's content, never an interstitial, and never during audio
playback. Be realistic — at a few hundred daily users this is pocket change, and it competes with
your own store for attention.

*Fast to wire · Low ceiling · Brand risk*

**To build**
- AdMob native ad unit; suppress entirely for Plus subscribers
- Consider skipping until ideas 1 and 3 are live — they earn more per user

### 6. Sponsor a day's audio

Devotees give. The temple model — sponsoring an archanai or annadhanam — translates directly: a user
pays to sponsor one day's audio and their name (or a family member's) appears as a dedication on
that day. It's emotionally native to the audience rather than a foreign "tip jar".

*Culturally native · ₹101 / ₹501 tiers · Check Play policy*

**To build**
- Sell as a digital good through Play Billing, **not** as a donation — Play restricts donations
- A `sponsor_name` column on `daily_content`; render as a dedication line
- Cap one sponsor per day so it stays scarce and meaningful

### 7. YouTube AdSense, fed by the app

Your channel already monetizes (or can). The app is a free, high-intent traffic source — and unlike
ads inside the app, sending someone to your own channel costs the user nothing and doesn't cheapen
the experience. See Part C.

*No app-side cost · Compounding*

---

## Part B — The festival feature

You asked for festival details in the app. This is almost entirely a client-side job — the backend
exists and is public.

Retrofit is already a dependency. Add a second API interface pointing at
`api.informationneeds.com` and you have festivals, panchangam and special dates without writing a
line of backend code.

| Screen | Endpoint you already have | Why it matters |
|---|---|---|
| Festival list | `GET /api/spiritual/festivals/upcoming?count=10` | Home-screen card: "next festival in 6 days" |
| Year calendar | `GET /api/spiritual/festivals/year/{year}` | The full-year view devotees screenshot and share |
| Month view | `GET /api/spiritual/festivals/month/{m}/{y}` | Natural calendar browsing |
| Festival detail | `GET /api/spiritual/festivals/{slug}` | The page that sells a PDF and links a video |
| Daily panchangam | `GET /api/spiritual/panchangam` | Daily-return habit, independent of your content pipeline |
| Amavasai dates | `GET /api/spiritual/amavasya/{year}` | Ancestor rites — strong reminder trigger |

### The Murugan angle

**Sashti is your festival.** `SpecialDatesCalculatorService` already computes monthly Sashti
(tithi 6) alongside Amavasai, Pournami, Ekadashi and Pradosham. For a Murugan devotee app, monthly
Sashti and the six-day Skanda Sashti are the observances that define the calendar — and you already
have `sashti-audio.m4a` sitting in your rasi repo. Lead the festival feature with Sashti rather
than treating it as one row among many.

Festivals worth dedicated treatment, each with its own audio, video link and product tie-in:

| Festival | Tamil month | Why it earns |
|---|---|---|
| Skanda Sashti | Aippasi | Six-day observance — a natural six-part paid audio series |
| Thai Poosam | Thai | Peak kavadi/pilgrimage season → temple guide sells |
| Panguni Uthiram | Panguni | Marriage-linked → porutham report sells |
| Vaikasi Visakam | Vaikasi | Murugan's birth star — the channel's biggest video day |
| Karthigai Deepam | Karthigai | Mass-participation festival, high sharing |
| Monthly Sashti | every month | Twelve recurring engagement peaks per year |

---

## Part C — Feeding the YouTube channel

The app has your most devoted users. The channel needs exactly those people. Right now the two are
completely disconnected.

**1. End-of-audio handoff.** The moment the daily audio finishes is the single best placement in the
entire app — the user is engaged, unhurried, and has nothing else to do. Show one video card there.
Not a banner, not a popup: one card, one video, chosen for the day.

**2. Every festival page links its video.** Map festival `slug` → YouTube video id. Someone reading
about Thai Poosam wants to watch Thai Poosam content. This also gives you a reason to make one video
per festival, which is a year's content calendar decided for you.

**3. Deep-link to the YouTube app, not the browser.** Opening `vnd.youtube:` hands the view to the
native app, where the user is signed in. Signed-in views count properly toward watch time and
subscription prompts actually work. A browser tab wastes the click.

**4. Ask for the subscribe late, once.** A subscribe prompt on day 1 gets dismissed and trains the
user to ignore you. Gate it behind real commitment — day 7, or after five completed audios — and
show it exactly once. Your `DayProgressStore` already knows the number.

**5. Share sheets carry the channel.** You just built the share feature. Every shared image should
carry the channel handle in its footer. Shares go into family WhatsApp groups — precisely your target
demographic, reached for free.

**6. Push on upload day.** You already run `PushNotificationService`. A notification when a new video
lands turns installs into first-hour views, which is the signal YouTube's algorithm weighs most
heavily.

---

## Part D — What to build first

Sequenced so each step makes the next one more valuable. Don't build the subscription before there's
something to subscribe to.

| # | Ship | Why now | Depends on |
|---|---|---|---|
| 1 | Festival list + detail screens | API exists; adds real depth before launch | — |
| 2 | YouTube card after audio | One screen, immediate channel lift | — |
| 3 | Play Store launch | Nothing earns until it ships | 1, 2 |
| 4 | Play Billing + PDF store | First real revenue, no new content | 3 |
| 5 | Festival-timed offers + push | Turns the store from passive to converting | 1, 4 |
| 6 | Personalized reports | Highest ticket; needs a trusting audience | 4 |
| 7 | VetriDaily Plus | Only worth it once the archive has depth | 1, 6 |

### Be realistic about the numbers

None of this earns meaningfully below roughly a thousand engaged users. At a few hundred daily users,
ads earn a rounding error and a subscription won't clear Play's payout threshold. The order above
front-loads the things that **grow** the audience — festivals, push, YouTube — because audience is
the input every revenue idea multiplies. Monetize second.

---

## Sources

Endpoints read from `SpiritualController.cs` and `SpecialDatesCalculatorService.cs`; product list
from the API's `DigitalProducts` build output; app structure from `com.murugan.dailycalm`.

A formatted version of this document is at `docs/VETRIDAILY_GROWTH_PLAN.html`.
