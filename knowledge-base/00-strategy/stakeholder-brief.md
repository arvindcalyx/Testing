# Journey Risk Management — Stakeholder Brief

**Audience:** Sponsors, line management, peers, partners — anyone who needs to understand what the JRM program is being asked to do, why today's system isn't delivering, what's solvable, what's genuinely hard, and what's buildable but needs time.

**Tone:** Honest about what we can do, honest about what we can't, honest about why.

---

## 1. What the business is asking for

The Business Requirements Document (BRD) for the Journey Risk Management initiative bundles roughly **22 distinct capabilities** under five themes:

1. **Pre-journey safety controls** — home-login check, vehicle-type selection, helmet detection (2-wheelers), seatbelt detection (4-wheelers), weather/route risk gating, night-driving policy, document compliance.
2. **In-journey behaviour monitoring** — context-aware speed limits, harsh braking/acceleration/cornering, phone-usage detection, fatigue timers, in-cabin nudges.
3. **Automated consequences** — rule-based engine that turns repeated unsafe behaviour into warnings, micro-learning, or escalations to line managers.
4. **Analytics, dashboards, reporting** — real-time manager view, driver risk scoring, gamification.
5. **Governance, communication, MIS** — role hierarchy, broadcasts, SOS, automated reports.

The underlying business goal is clear and serious: **road incidents are the #1 cause of workplace injuries in the field workforce** — roughly **300 incidents and 10 fatalities a year**. The FY27 target is zero fatalities and ≤90 serious injuries. The existing system has not moved these numbers because the people who are supposed to be helped by it have learned to ignore it.

---

## 2. Why the existing system isn't working — in plain English

The existing in-app telematics module produces **60,000–80,000 alert events every single day**. Most of them are wrong. Field engineers receive automated calls about unsafe driving that they didn't do. The natural human response is to stop listening. We are at the alert-fatigue stage — the system is technically running but practically dead.

A focused review of the existing module's source code identified **five specific defects and three architectural shortcuts** that together explain the false-positive flood. None of them are subtle. None of them require advanced research to fix.

### The five concrete defects

1. **GPS coordinates are being rounded to ~1 kilometre resolution before any calculation happens.** Whoever wrote it appears to have believed it would "improve data consistency." It does the opposite — every speed and distance calculation in the module is operating on coordinates that have already been mangled. Imagine measuring your driving speed using a GPS that only knows what kilometre square you're in. This is the single largest source of bad speed readings.

2. **The harsh-driving detector ignores the accelerometer entirely.** It tries to detect hard braking by looking at GPS speed alone, with a sensitivity threshold roughly 10× too aggressive. Real harsh braking is around 3 m/s² of deceleration; the detector currently fires on roughly 0.3 m/s² — gentle pedal pressure. Of course it produces thousands of false alerts a day. The phone's accelerometer is exactly the right signal for this, and there's a working accelerometer class sitting unused in the same codebase.

3. **The transport-mode awareness exists in the codebase but is not wired in.** There is a file that calls Google's Activity Recognition API — the standard way an Android app figures out whether the user is driving, walking, on a bus, etc. — but it uses a deprecated 2015-era pattern and nothing else in the system ever calls it. The result: alerts fire while the user is walking, on the metro, or on a bus. That is exactly the false-positive class the BRD calls out by name.

4. **Crash detection has no on-device intelligence at all.** The module ships raw sensor blobs to a backend every 15–30 seconds, the backend returns a yes/no, and then the user is asked to confirm a possible crash by taking a photograph. This is the wrong approach for several reasons — the most important being that the sensor data is not oriented to the vehicle frame, so the backend cannot tell the difference between a phone sliding off the seat and a frontal impact. The Android engineering lead confirms crash detection is ineffective today. We agree.

5. **The "stationary" speed threshold is set at 10 km/h.** Anything slower is treated as "not moving" and the module wipes its memory of recent locations. In stop-and-go city traffic, this happens constantly. The detector therefore has no usable history to validate the next GPS reading against, which produces yet more bad detections.

### The three architectural shortcuts

A. **The module uses an old Android location API instead of the modern fused one** that combines GPS with cell-tower and Wi-Fi signals. This makes things worse in tunnels and dense urban areas — exactly where the BRD lists known pain points.

B. **There is no sensor reorientation to the vehicle.** The accelerometer values are in the *phone's* reference frame. If the phone is in a pocket, cupholder, or loose mount, ordinary motion bleeds into the axes that the harsh-event detector reads, producing false events. This is the single biggest cause of harsh-event false positives that have nothing to do with how the person is actually driving.

C. **The high-frequency sensor data accumulation has performance bugs** that drain battery faster than necessary on the entry-level Android phones many field engineers use.

### The deeper point

The previous in-house attempt to improve this module was likely tuning thresholds and writing suppression rules on top of the GPS-rounding bug (#1). It is not surprising that field validation failed — you cannot rescue corrupted inputs with smarter downstream logic. The fixes are upstream.

---

## 3. What we can solve — and why we're confident

Below, we map the things the BRD asks for to what we can actually deliver, and the reasoning for each.

### High confidence we can solve (in-house, no vendor, no ML)

| BRD capability | What we will do | Why we're confident |
|---|---|---|
| **Context-aware speed limits** (replacing the fixed 45/60 km/h with the actual posted limit per road) | Read posted limits from a commercial map API; flag overspeed only when sustained against the real limit | GPS speed itself is reliable. The complaint "incorrect speed tracking" is mostly "wrong limit, not wrong speed." This is integration work, not research. |
| **Lower false positives on harsh events** | Reorient sensor data to the vehicle frame; require accelerometer AND GPS agreement before firing; correct the threshold to engineering-accurate values; suppress events below ~10 km/h | We have the diagnosis — defects #2, #5 and architectural shortcut B. Standard signal-processing approach; ample reference material. |
| **Suppress public-transport false alerts** (metro, bus) | Wire in the modern Activity Recognition API. Only run detection when the system is confident the user is `IN_VEHICLE` for at least 30 seconds | This is defect #3. The API exists in the codebase — the work is to call it correctly and let it gate the detector. |
| **Pre-journey vehicle-type selection** | Simple picker before trip starts; thresholds differ for 2-wheeler vs 4-wheeler | Trivial UI + a configuration branch. |
| **Fatigue timer / mandatory rest break** | Continuous-driving timer with policy-defined break interval | Trivial. |
| **In-cabin audio/haptic nudges** | Text-to-speech + vibration on detected unsafe behaviour | Trivial. |
| **Manual SOS button + location dispatch** | Persistent in-app button; sends live location to configured emergency contacts | Standard. |
| **Pre-journey weather / route risk gating** | API calls to weather + map providers; rule engine for vehicle-specific blocks | Integration work, no novel research. |
| **Night-driving policy block** | Time-of-day rule in the journey-start flow | Trivial. |
| **Document compliance tracking** (RC, insurance, PUC, licence with expiry alerts) | Standard CRUD + reminders | Trivial. |
| **Manager dashboard, risk scoring, gamification, leaderboards** | Standard web application + analytics | Standard product engineering. |
| **Roles & escalation hierarchy** | Standard role-based access control | Standard. |
| **Centralised safety broadcasts** | Push-notification fan-out targeted by role/location | Standard. |
| **Automated MIS emails** | Scheduled report jobs | Trivial. |
| **Consequence rule engine** | Server-side rules with configurable thresholds | The hard part is policy design, not engineering. |

**Why this section is long:** the majority of what the BRD asks for is mainstream engineering, not deep research. Roughly two-thirds of the requirements fall here. AI-assisted development tools (Claude, Codex, Cursor) accelerate this kind of work substantially.

---

## 4. What is genuinely hard — and why

These items in the BRD are not in the "mainstream engineering" category. Either the technology is hard, or the data needed to train it doesn't exist, or both. Being honest about these now prevents over-promising later.

### 4.1 Helmet detection with certification-mark verification, chin-strap, liveness

The BRD asks the app to verify, via the phone camera before the trip starts, that the rider is wearing a full-face helmet, that the certification mark is visible, that the chin strap is fastened, and that the photo is of a real human (liveness, not a photo of a photo).

**Why it's hard:**
- General "is there a helmet in the frame" is doable with off-the-shelf object-detection models.
- **Verifying the certification mark is the hard sub-problem.** It is a small, low-contrast embossed mark with extreme variation in lighting, angle, and helmet type. There is no public dataset of certification marks to train against. Building one would require a labelled corpus of thousands of helmet photos collected across geographies and conditions.
- **Liveness detection at on-device speed under arbitrary lighting** is a research area that vendors have spent years on. Doing it badly is worse than not doing it (creates a false sense of compliance).

**Honest position:** this is a multi-month machine-learning project, not a feature toggle. If certification-mark verification is non-negotiable, the right path is a specialist vendor or a dedicated data-collection-and-model-training programme that runs in parallel to the rest of JRM. The POC will not attempt this.

### 4.2 Seatbelt detection with multi-occupant counting

The BRD asks the app to verify the driver's seatbelt is across the chest (not just over the shoulder) and to count passengers and verify their belts.

**Why it's hard:**
- Driver-only, single occupant: tractable with pose estimation + line detection. Achievable with effort.
- Multi-occupant from a single phone camera is significantly harder. Rear-passenger detection from a front-mounted camera is essentially infeasible. In-cabin lighting at night is poor for vision models.

**Honest position:** scope a single-occupant driver-belt check as a v1 feature; defer multi-occupant verification or treat it as manual photo review by line managers.

### 4.3 Automatic crash detection at usable false-alarm rates

The BRD asks the app to detect a road accident automatically using the device's accelerometers and dispatch live location to emergency contacts.

**Why it's hard:**
- You cannot ethically generate crash data. You can only reconstruct historical incidents — and the workforce has roughly 280 incidents and 9 fatalities a year. That is a small validation set; it is nowhere near a training set.
- Distinguishing a real impact from "phone dropped on the floor" requires both correctly oriented sensor data **and** a model trained on real crash signatures. Specialist vendors (Sentiance, Cambridge Mobile Telematics, Bosch) have built their businesses on this exact capability over a decade, using insurance-claim data we do not have access to.
- A naive on-device threshold detector produces the worst possible tradeoff — it both misses real crashes and fires false alarms, both of which destroy user trust.

**Honest position:** **do not attempt automatic crash detection in-house.** Ship a robust **manual SOS button** in v1 (this is genuinely easy and high-value), and treat automatic crash detection as a separate vendor-evaluation track. The fatality risk is too serious to accept a half-built solution.

### 4.4 Production-grade accuracy on the long tail of harsh-event and transport-mode classification

The fixes we will apply will take harsh-event detection from "broken" to "good." They will not take it from "good" to the ~95% accuracy that specialist vendors quote.

**Why:**
- The last 10–15 percentage points of accuracy come from labelled trip data — thousands of hours of real driving with ground-truth annotations of what actually happened. Vendors have built this data over years; we have not.
- Edge cases (distinguishing aggressive-but-legal driving from genuinely unsafe driving, distinguishing a car passenger from a car driver, distinguishing a bus from a car in heavy traffic) require either large labelled datasets or in-house ML researchers.

**Honest position:** the BRD does not actually demand vendor-grade accuracy — it demands **the false-positive problem be solved**. Those are different bars. We can clear the second. We cannot promise the first without a multi-year investment or a vendor.

### 4.5 Real-world edge cases that no algorithm fully solves

GPS loss in tunnels, flyover underpasses, dense urban areas. Aggressive background-kill behaviour on certain Android phones (Xiaomi, Oppo, Vivo, Realme). Battery-saver mode silently disabling foreground services on entry-level devices. These are real-world constraints that no telematics system fully overcomes; the best systems mitigate them.

**Honest position:** treat these as ongoing engineering hygiene, not deliverables. Allocate time for a device-test matrix and per-manufacturer guidance to users. Accept that some coverage gaps are unavoidable.

### 4.6 Posted speed limit data coverage on rural roads

Map vendors (Google, Mapbox, HERE, TomTom) have variable coverage of posted speed limits across the country. Urban primary roads are usually well covered; rural roads frequently are not.

**Honest position:** when the map has no data, mark the segment "speed limit unknown" — do not guess. Document this gap. It is real and external; no engineering effort fixes it.

---

## 5. What can be built — but takes time when it's a solo effort

This category is the most important one to manage expectations on. These things are not hard in the deep-tech sense. They are buildable by any competent engineering team — including AI-assisted solo development with Codex or Claude. But they take a realistic amount of time.

### What "AI-assisted solo" actually accelerates

- Boilerplate code (UI screens, data models, API clients, CRUD)
- Pattern-following code (anywhere a similar example exists in open source)
- Refactoring within well-defined boundaries
- Test stubs and documentation

### What AI-assisted solo does *not* substantially accelerate

- Decisions that require knowing the business policy (e.g., "what should the consequence ladder be?")
- Native Android sensor / battery / background-execution work where the right answer depends on hardware behaviour
- Anything that requires running on a physical device and watching what happens
- Per-OEM Android background-kill workarounds — these are tribal-knowledge fixes per manufacturer
- Model training, data labelling, and field validation

### Honest sizing

| Scope | Realistic timeline, solo + AI-assisted |
|---|---|
| **The 4-week POC** — parallel detection engine demonstrating false-positive reduction on harsh events, transport-mode suppression, context-aware speed | 4 weeks |
| Applying the diagnosed fixes to the **existing** in-app module in place | 4–6 weeks (needs occasional senior Android-engineer review and team approval to merge) |
| Full set of mainstream BRD capabilities (everything in §3 of this doc) | 6–9 months solo; 3–4 months with a small team |
| Helmet / seatbelt CV pipeline at production grade | Multi-month ML project regardless of team size; needs data-collection programme first |
| Automatic crash detection at acceptable accuracy | Vendor-evaluate, do not build |

### The single biggest force multiplier

It is not Claude, Codex, or Cursor. It is **field-validation infrastructure** — the ability to record real trips, replay them through both the old and new detectors, and compare false-positive and true-positive rates without re-driving every change.

Without this, every code change is a question of "we think it's better." With this, every code change is a question of "the data says it's X% better on Y trips." Two prior in-house attempts to improve the existing module appear to have failed for exactly this reason: changes were made on faith, not on measurement.

A trip-replay harness is part of the POC scope. It is arguably more valuable than the detection improvements themselves.

---

## 6. Bottom line

> The BRD describes a serious safety programme with 22 capabilities. The vast majority — roughly two-thirds — are mainstream engineering that can be built in-house with AI-assisted development on a realistic 6–9 month timeline. **The false-positive problem that is undermining the existing system today is not a vendor-grade research problem; it is five identifiable bugs and three architectural shortcuts, all fixable in 4–6 weeks once a parallel POC demonstrates the approach works.** Three BRD capabilities — helmet certification-mark verification, multi-occupant seatbelt verification, and automatic crash detection — are genuinely deep-tech or data-dependent and should either be vendored, deferred, or scoped down. **A 4-week solo POC** producing measured side-by-side false-positive comparisons is the right next step; it converts the "we think we can fix this" argument into "the data shows we can."

---

## Appendix A — One-paragraph version for very busy readers

The existing in-app safety module produces tens of thousands of false-positive alerts every day, which is why field engineers ignore it. A code review identified five specific bugs and three architectural shortcuts that explain almost all of the false positives — none of them require advanced research to fix. We propose a 4-week parallel proof-of-concept that demonstrates the fixes work, side-by-side with the existing module, without changing any production code. After the POC, the engineering team applies the same fixes to the production module in 4–6 weeks. Three BRD capabilities (helmet certification-mark detection, multi-occupant seatbelt verification, automatic crash detection) are genuinely hard and should be vendored or descoped — being honest about this now is part of the deliverable.

## Appendix B — Glossary for non-technical readers

- **False positive:** an alert that fires when it shouldn't have. The thing destroying user trust today.
- **Activity Recognition:** the standard Android service that classifies whether the user is driving, walking, on a bicycle, etc. Free, built into Android.
- **Fused location:** the modern Android way of getting GPS that combines satellite, cell-tower, and Wi-Fi signals — more accurate and battery-friendly than raw GPS.
- **Sensor reorientation:** the technique of mathematically rotating accelerometer readings from the phone's reference frame into the vehicle's reference frame, so that "forward acceleration" really means forward, regardless of how the phone is held.
- **Replay harness:** a tool that takes a recorded trip and re-runs it through the detection logic. Lets engineers measure whether a change actually improved things, without driving the route again.
- **Shadow mode:** running a new version of a detector alongside the old one in production, comparing their outputs, but only the old one's alerts reach users. Lets you measure improvements with zero risk.
- **Ground truth:** for telematics, this means: someone with a dashcam reviewed the trip and labelled what actually happened (real hard brake / phone dropped / pothole / etc.). Without ground truth, you can't prove a detector is better.
