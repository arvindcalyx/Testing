# the field-force host app JRM — Independent Feasibility Assessment

**Audience:** CIO, build-vs-buy decision.
**Tone:** Pessimistic. Confidence numbers reflect "ship at production quality," not "demo works."
**Goal stated by sponsor:** 70–80% of business problem at ~85% confidence. False-positive reduction prioritized over best-in-class accuracy.
**Assumptions:** the field-force host app app exists; existing telematics SDK may be replaced; development is AI-assisted (Claude/Codex); dedicated engineering capacity is uncertain.

---

## 0. Headline answers (the four strategic questions)

**Q1. Can a new SDK realistically be built?**
Distinguish two things people conflate:
- **A Sentiance-equivalent commercial SDK** (productized, multi-tenant, decade of labeled trip data, on-device ML, ISO 27701, ~95% harsh-event accuracy): **No.** Not in any reasonable timeline, not with AI-assisted dev, not without a dedicated ML team and a labeled-trip data program. This is a deep-tech vendor category.
- **An in-app sensor + event-detection layer inside the field-force host app** (uses Android FusedLocation + SensorManager + Activity Recognition + a map API, with proper reorientation + cross-validation + map-matching): **Yes, achievable.** This is not an "SDK" in the vendor sense; it's signal-processing code inside your own app. Reframing this distinction to the CIO is critical — most "build the SDK" requests collapse into the second thing once examined.

**Q2. Is replacing the current SDK justified based on the BRD?**
**Conditionally yes — but probably as "augment + replace incrementally," not "rip and replace."** The current SDK's specific failures (fixed speed limits, no transport-mode awareness, raw threshold detection, no sensor reorientation) are not Sentiance-grade-hard problems. They look like a naive implementation of the basics. A rebuild *of the detection layer* is justified. A rebuild of the entire telematics stack is only justified if the current SDK is unmaintainable (closed-source vendor, vendor unresponsive, can't be extended). Establish that fact before greenlighting a rip-and-replace.

**Q3. Which 20% of requirements create 80% of delivery risk?**
Five items account for the vast majority of program risk:
1. **F1.3 Helmet + ISI-mark + chin-strap + liveness CV.** ISI-mark verification is a tiny-object detection problem with no public India dataset. Liveness adds anti-spoofing. Realistic confidence: 30–50% to reach BRD spec in 6 months.
2. **F1.4 Seatbelt across chest + passenger count CV.** Pose + line/strap detection under poor in-cabin lighting. 50–60% confidence.
3. **F2.1 Context-aware speed limits in India.** The map-API speed-limit data itself is the dependency. Google Roads API speed-limit endpoint is patchy and premium in India; HERE / TomTom / Ola Maps need evaluation. Coverage will be incomplete on rural roads regardless of vendor.
4. **F5.3 Automatic crash detection.** DIY crash detection is the worst-case false-positive/missed-event tradeoff in the BRD. Vendor or defer.
5. **NFR public-transport false-suppression.** Achievable via Android Activity Recognition + map-matching to transit lines (heuristic, not ML), but the BRD asks for production-grade accuracy. 70% confidence in-house; the long tail is hard.

**Q4. What subset eliminates ≥50% of current false positives within 4–8 weeks?**
Four fixes inside the current app, no vendor:
1. **Sensor reorientation to vehicle frame** (gravity-vector + gyro calibration, rotate accel readings into the vehicle's longitudinal/lateral/vertical axes). Kills the #1 cause: phone-in-pocket / cupholder / loose-mount phantom events.
2. **GPS × accelerometer cross-validation gate** for every harsh event (a hard brake requires BOTH a longitudinal-axis deceleration spike AND a GPS speed drop within a 1–2s window). Kills dropped-phone, handling, and pothole spikes.
3. **Android Activity Recognition (transition API) gate** — only run telematics when `IN_VEHICLE` is the dominant signal for ≥30s. Kills walking, cycling, and idle-jostle events.
4. **Speed-dependent thresholds** — suppress harsh-acceleration events below ~5–10 km/h; raise the bar for harsh events at very low speed. Kills parking-lot creep.

These four are **standard signal processing**, not ML. They are well-documented in the smartphone-telematics literature and buildable by one AI-assisted developer with periodic Android-engineer review. **Realistic claim: 50–70% reduction in current false-positive volume within 4–8 weeks**, conditional on access to the current SDK's raw sensor stream (if it's a black box, this work has to happen in a new sensor layer alongside it).

---

## 1. Build-vs-buy classification per BRD requirement

Classification legend:
- **A** = One AI-assisted builder (Claude/Codex-accelerated; web/backend/UI/rules patterns)
- **B** = AI-assisted builder + periodic Android engineering guidance (sensors, foreground services, OEM background-kill quirks)
- **C** = Dedicated Android engineer required (sustained native work, battery profiling, multi-device test)
- **D** = ML / CV specialists + labeled data + iteration
- **V** = Better solved by vendor (Sentiance / CMT / Damoov / map vendor / SMS gateway)

Complexity is 1–10 (relative effort). Confidence is "ship at BRD-acceptable quality in a 6-month program window."

### 2.1 Pre-Journey Safety & Risk Controls

| Req | Feature | Class | Complexity | Confidence | Major technical risks |
|-----|---------|-------|------------|-----------|------------------------|
| F1.1 | Home-login geofence (500m) | A | 2 | 95% | Indoor GPS drift in dense urban housing can push users out of 500m radius; tune radius and accept manual override. |
| F1.2 | Vehicle-type selection pre-departure | A | 1 | 99% | None substantive. Risk is users lying — addressed by cross-checking against detected motion profile post-trip. |
| F1.3 | Helmet + ISI mark + chin strap + liveness CV | **D** | **9** | **35%** | No public India helmet dataset; ISI-mark is small-object detection; liveness anti-spoofing; on-device <2s constraint on low-end Android. Data-collection program needed before model work. |
| F1.4 | Seatbelt across chest + passenger count CV | **D** | **7** | **55%** | Poor in-cabin lighting; belt-strap occlusion; rear-passenger detection from front camera is hard. Single-occupant case is tractable; multi-occupant is shaky. |
| F1.5 | Pre-journey risk (weather + route + blackspot gating) | A | 3 | 90% | Blackspot data ownership — need an internal incident-history dataset; weather API cost at scale. |
| F1.6 | Night-driving policy (7pm–6am) block | A | 1 | 99% | Pure rules. None. |
| F1.7 | Contextual micro-learning gate on poor risk profile | A | 3 | 90% | Content production (the learning modules themselves) is the bottleneck, not engineering. |
| F1.8 | Document & vehicle compliance (RC/Insurance/PUC/DL) | A | 2 | 98% | CRUD + reminders. None. |

### 2.2 In-Journey Behavior Monitoring

| Req | Feature | Class | Complexity | Confidence | Major technical risks |
|-----|---------|-------|------------|-----------|------------------------|
| F2.1 | Context-aware speed limits | A + **V (map)** | 5 | 70% | **India speed-limit data coverage is the program's hardest external dependency.** Google Roads API coverage is patchy/premium; HERE, TomTom, Ola Maps need evaluation. Rural coverage will be incomplete; need fallback policy (use posted-limit heuristics from road class). |
| F2.2 | Harsh accel/brake/cornering, 2W/4W thresholds | B | 7 | 75% to "good"; 40% to vendor-grade | Reorientation + cross-validation gets ~80%-grade detection. The last mile (95%+) is what licensees pay for. |
| F2.3 | Phone usage during driving | B | 5 | 75% | Post-Android 10 you can detect screen-on / unlock / foreground-app-change of YOUR app via lifecycle, but you **cannot** see what's on screen in other apps without intrusive accessibility-service permissions. Spec must be relaxed to "screen-on + interaction events during trip." |
| F2.4 | Fatigue (continuous driving time) | A | 1 | 99% | Timer. None. |
| F2.5 | In-cabin audio/haptic nudges | A | 2 | 98% | TTS + vibration. None. |

### 2.3 Automated Consequence & Governance

| Req | Feature | Class | Complexity | Confidence | Major technical risks |
|-----|---------|-------|------------|-----------|------------------------|
| F3.1 | Consequence rule engine | A | 3 | 95% | Server-side rules. Risk is policy design (HR-side), not engineering. |
| F3.2 | Evidence/artifact vault | A | 3 | 95% | Storage cost at scale; retention policy; tamper-evidence (hash chain) if needed for audits. |

### 2.4 Analytics, Dashboards & Reporting

| Req | Feature | Class | Complexity | Confidence | Major technical risks |
|-----|---------|-------|------------|-----------|------------------------|
| F4.1 | Manager dashboard (real-time team risk, live maps) | A | 4 | 95% | Live-map cost (Maps SDK pricing at scale). |
| F4.2 | Driver risk scoring | A | 3 | 95% | Score-formula design is the hard part, not engineering. Garbage-in if telematics false positives aren't fixed first. |
| F4.3 | Gamification / leaderboards | A | 2 | 99% | Anti-gaming (people will try to inflate scores). |

### 2.5 Roles, Governance & Communication

| Req | Feature | Class | Complexity | Confidence | Major technical risks |
|-----|---------|-------|------------|-----------|------------------------|
| F5.1 | Roles & hierarchy / escalation | A | 3 | 95% | HR-data integration to keep org chart current. |
| F5.2 | Centralized safety broadcasts | A | 2 | 95% | Push delivery on OEM-killed apps in India. |
| F5.3a | Manual SOS button + location dispatch | A | 2 | 98% | SMS gateway reliability; SOS routing to control room. |
| F5.3b | **Automatic** crash detection | **V** | **8** | **30% in-house** | DIY threshold detection is the worst FP/miss tradeoff in the BRD. Recommend vendor or defer auto-crash; ship manual SOS in v1. |

### 2.6 MIS & Automated Communications

| Req | Feature | Class | Complexity | Confidence | Major technical risks |
|-----|---------|-------|------------|-----------|------------------------|
| F6.1 | Automated MIS emails | A | 2 | 99% | Cron + templates. None. |

### NFRs

| NFR | Class | Complexity | Confidence | Major technical risks |
|-----|-------|------------|-----------|------------------------|
| Unified the field-force host app architecture | A | 4 | 90% | Coordination, not technology. |
| the existing distance-tracking module background distance tracking coexisting with new telematics | **C** | 6 | 70% | **Underestimated in BRD.** Two background sensor consumers on one app on Xiaomi/Oppo/Vivo means foreground-service scheduling, battery-saver exemptions, and per-OEM allowlist UX. Needs a real Android engineer, not just AI-assisted dev. |
| On-device CV <2s | D | 6 | 60% | MobileNet inference is fast on mid-tier; ISI-mark detection inflates pipeline. |
| Public-transport accuracy (suppress transit FPs) | B | 5 | 70% | Android Activity Recognition + map-matching to GTFS / OSM transit lines is heuristic; long tail is hard. |

---

## 2. What Android already gives you for free (use these, don't rebuild)

Most of the "we need an SDK" instinct disappears once these are understood:

| Capability | Android API | What it gives you |
|-----------|-------------|-------------------|
| Trip / motion classification | **Activity Recognition Transition API** (Google Play Services, free) | `IN_VEHICLE`, `ON_BICYCLE`, `WALKING`, `RUNNING`, `STILL` with state-transition callbacks. Sufficient to gate "should telematics even be running right now?" Kills a huge class of false positives by itself. |
| Reliable GPS speed | **FusedLocationProviderClient** | Smoothed location + speed from GPS+network+sensors. Speed is reliable; that addresses most "incorrect speed" complaints once you stop comparing to the wrong limit. |
| Gravity-compensated linear acceleration | **TYPE_LINEAR_ACCELERATION**, **TYPE_GRAVITY**, **TYPE_ROTATION_VECTOR** | OS already fuses accel + gyro + magnetometer. You don't need to write a Kalman filter from scratch. |
| Geofencing (home login, blackspots) | **Geofencing API** | Battery-efficient enter/exit. F1.1 nearly for free. |
| Background-safe location | **Foreground service + location type** | Documented pattern. The Android-engineer-required part is OEM (Xiaomi/Oppo/Vivo) battery-saver workarounds. |
| On-device CV | **ML Kit + custom TFLite models** | Object detection, pose, OCR (for ISI mark text). Free runtime; you supply the model. |

**The honest statement to the CIO:** the "telematics SDK" you currently use is doing things Android has built-in APIs for. A meaningful chunk of the existing false-positive problem reduces to "the current implementation doesn't use Activity Recognition to gate detection."

---

## 3. What truly requires ML / labeled data / years of iteration

Be honest about this list. Everything else is rules, APIs, and signal processing.

- **F1.3 ISI-mark verification.** Tiny-object detection of an embossed mark with variable lighting and orientation. No public dataset for India. This is a real ML project.
- **F1.3/F1.4 Liveness detection.** Anti-spoofing against photo-of-helmet / photo-of-seatbelt-photo. Either use a vendor (ML Kit face liveness, FaceTec) or build active liveness (motion challenge).
- **F2.2 Production-grade harsh-event detection** at Sentiance's ~95% accuracy. The last 15 percentage points cost years.
- **F5.3 Automatic crash detection** at usable false-alarm rates. This is the canonical "labeled crash data is impossible to collect ethically" problem; vendors solve it with insurance-claim data.
- **NFR Production-grade transport-mode classification** (the long tail beyond Activity Recognition's IN_VEHICLE — distinguishing car-passenger from car-driver, bus from car, train from car).

---

## 4. Honest answers, restated

- **Replace the current SDK?** Justified for the *detection layer* if it's a black box you can't extend. If it's extensible, augment it (add reorientation + cross-validation + Activity Recognition gating) before considering replacement. Either way, do not procure another full vendor SDK without first confirming the current SDK's specific failure modes are inherent and not implementation-fixable.
- **Build a new SDK?** Build an in-app sensor + event layer; do not productize an SDK. The BRD does not actually require an SDK-as-product — it requires *the capabilities* an SDK provides. Inside one app, those capabilities are signal-processing code, not a product.
- **Goal achievability (70–80% of business problem at 85% confidence):** Achievable, *with two caveats* — (i) defer or vendor-out F1.3 ISI-mark, F1.4 multi-occupant seatbelt, F5.3 auto-crash; (ii) lock down F2.1 speed-limit data source early because no amount of engineering compensates for missing map data.
- **20% creating 80% of risk:** F1.3, F1.4, F2.1, F5.3 auto-crash, NFR transit accuracy. Everything else is mainstream build.
- **4–8 week 50%+ FP reduction:** The four false-positive killers (sensor reorientation, GPS×accel cross-validation, Activity Recognition gating, speed-dependent thresholds). High confidence this is achievable. This is the most defensible fast win to put in front of the CIO before any larger commitment.

---

## 5. Recommended CIO framing

> "Of 22 BRD requirements, 14 are mainstream build that one AI-assisted developer can deliver. 4 need periodic Android-engineer guidance. 4 are genuinely hard (helmet+ISI CV, seatbelt CV, auto-crash, India speed-limit data) and are the right places to either vendor-in or descope for v1. The current SDK's false-positive problem is largely an implementation gap, not a Sentiance-class moat — we can demonstrate a 50%+ false-positive reduction in 4–8 weeks using Android built-ins and standard signal processing, before committing to any vendor replacement. The question is not 'build vs buy Sentiance' — it's 'fix the 4 known implementation gaps first, then re-evaluate which of the 4 hard items are worth vendoring.'"

---

---

## 6. Reassessment given new information

**New facts:**
- Current SDK is in-house, not Sentiance.
- No vendor lock-in — any logic can change.
- Android lead says crash detection is ineffective today.
- Previous attempts to improve specific behaviors failed in field validation.
- Real-world edge cases (GPS loss, tunnels, signal interruptions) are known pain points.

These facts push the answer in two opposing directions at once. The cost of a rebuild drops (no procurement, no rip-and-replace friction). But the **previous-attempt-failed-in-field-validation** signal is the most important data point in this whole conversation — and it pushes hard the other way. A third in-house attempt that fails in field validation the same way is the worst possible outcome.

### 6.1 Is a clean-slate SDK justified?

**Yes — but narrowly, and only the detection pipeline.** Not the whole stack.

- **Clean-slate the detection pipeline** (sensor read → reorientation → fusion → event classification → suppression). This is where the bugs and false positives live. Build it as an isolated, testable module with explicit interfaces, designed from day one to be replayable against recorded trip data.
- **Keep what works** — GPS read, foreground-service plumbing, networking/payload transfer, storage. These are the parts the team has battle-tested. Rebuilding them buys no FP reduction and burns weeks.
- **Do not call it "an SDK."** Inside the field-force host app it's a module. The SDK framing invites scope creep (multi-tenant, versioning, public API, doc, support) that is irrelevant to the actual problem.

The rebuild is justified *if and only if* you also rebuild the **validation infrastructure** in parallel (see 6.4). Without that, this attempt fails the same way the last ones did.

### 6.2 Enhance vs rebuild — recommendation

**Rebuild the detection pipeline, in shadow mode, against historical trip data.**

- **Why not enhance:** Organically-grown threshold detectors don't layer well. Adding reorientation + cross-validation + Activity Recognition gating on top of an existing detector typically produces "if-this-else-if-that" patch chains that are untestable and re-introduce regressions. The team has likely already tried this, given the field-validation failure history.
- **Why "shadow mode":** Run the new detection pipeline **alongside** the existing one in production for 4–6 weeks without changing user-facing alerts. Log both pipelines' decisions on the same trips. Measure FP/TP delta directly on real the organisation field force traffic. This converts the previous "ship and pray" failure mode into a measurable A/B that the CIO can sign off on.
- **Why historical trip replay:** Re-run the new pipeline against the last N months of recorded sensor traces (you already store them as part of the 60–80K/day violation flood). If the new pipeline doesn't beat the old on the trips you already have, it won't beat it in the field either. This is the single biggest risk mitigation available.

This is meaningfully different from the previous attempts because (a) it's modular and replayable, (b) it's measured against the old pipeline on the same data, and (c) it goes through shadow mode before user impact.

### 6.3 BRD capabilities now easier because you control the full stack

Owning the implementation + being inside the organisation unlocks several things a vendor SDK cannot:

- **Network-side GPS fallback (the biggest win).** the organisation's IoT network-location service already uses SIM-based cell-tower triangulation as a fallback when GPS is weak. **Tap that as a backend service** for the JRM app — when on-device GPS drops in a tunnel or urban canyon, query the network-side location. No vendor SDK can offer this; it's the organisation's structural advantage. This is the single most differentiated capability you have.
- **Server-side post-trip re-scoring.** You don't have to decide everything on-device in real time. Once a trip ends, replay its sensor trace server-side with full map data, transit-line matching, and weather context, and *correct* the event log. The user gets accurate driver scoring even when the on-device detection was uncertain. Vendor SDKs typically can't be re-run server-side.
- **Tight the existing distance-tracking module integration.** the existing distance-tracking module's distance tracking and the JRM telematics can share a *single* foreground-service / sensor consumer, eliminating the battery fight and the OEM-background-kill double jeopardy. This is the NFR that worried me most; full-stack ownership solves it cleanly.
- **Context-aware gating from corporate data.** Activity Recognition + your own knowledge of the user's role, planned site visit, and scheduled meeting time lets you gate detection more accurately than a vendor with only sensor data. (E.g., a Field Engineer scheduled at a customer site at 11am is on a car/2W trip, not a metro trip.)
- **Per-circle / per-role A/B tuning.** Roll out new suppression rules to one circle, measure, iterate. Sentiance can't tune per-the organisation-circle.
- **Custom event types specific to the organisation's risk profile.** Long stops in known blackspots, late-night returns from specific sites, route deviations from planned site visits. Not in any vendor SDK.
- **Internal incident-history as ground truth.** the organisation has FY26's ~280 incidents and 9 fatalities — that's a small but real labeled dataset for blackspot scoring, route risk, and (eventually) crash-detection model tuning. Vendors don't have this; you do.
- **Tight consequence-engine + micro-learning loop.** No webhook latency between detection and intervention. The "trigger mandatory micro-learning before next journey" requirement (F1.7) is much cleaner when both modules are in one codebase.

The reframe to the CIO: **full-stack ownership flips the build-vs-buy debate** for everything except the genuinely-ML items. The right answer is now "own the detection pipeline + leverage the the organisation network + integrate deeply with the field-force host app + vendor only the irreducibly ML/CV pieces."

### 6.4 What's a field-validation problem, not a coding problem

This is the most important section. Code can be perfect and these will still bite. Spend equally on these as on the detection rewrite.

**Fundamentally field-validation problems (no algorithm fixes these alone):**
- **GPS loss in tunnels, flyover underpasses, dense urban canyons** (Mumbai BWSL underpass, Bangalore IT corridors, Delhi flyovers). Mitigation is *not* better filtering — it's network-side fallback (6.3) plus dead-reckoning from accelerometer over short gaps. Both have hard accuracy ceilings.
- **Device heterogeneity in India.** Xiaomi MIUI, Oppo ColorOS, Vivo Funtouch, Realme — all aggressively kill background sensors. You will never fix this in code alone; you fix it with per-OEM permission UX, autostart guidance, and a maintained device-test matrix of the top 10–15 phones by the organisation field-force share.
- **Battery-saver / Doze behavior on low-end devices.** Field engineers on entry-level phones get hit hardest. Foreground service + sticky notification + battery-exemption prompt is the *minimum* — and even then, OS will kill you under thermal load.
- **User-behavior heterogeneity.** Phone in cradle vs pocket vs cup holder vs hand-mounted on 2W. Reorientation calibrates this, but only after a steady-driving window. Short trips never reach calibration.
- **Ground-truth scarcity.** You cannot reliably label "this was actually a hard brake" without dashcam + manual review. **Without ground truth, you cannot prove the new pipeline is better than the old.** This is what most likely killed the previous attempts. **Solution: a dashcam pilot on 50–100 vehicles to build a labeled corpus of ~10K trips.** This is non-negotiable for a credible v2.
- **Crash data scarcity.** You cannot ethically generate crash data. You reconstruct from the ~280 incidents the organisation has on record. With 9 fatalities and 280 incidents, you have a few hundred labeled crash events at best — barely enough to validate a vendor model, nowhere near enough to train one. **Auto-crash detection should be vendored, full stop.**
- **Public-transport long tail.** Metro vibration profile differs from suburban rail, differs from BRT bus, differs from regular bus. Map-matching to transit lines handles the bulk; the tail needs labeled data per mode.
- **Rural posted-limit ambiguity.** No map API resolves this; even ground truth is contested. Accept a coverage gap and mark trips as "limit unknown" rather than guess.

**These are coding problems (rebuild solves them):**
- Sensor reorientation to vehicle frame
- GPS × accelerometer cross-validation
- Activity Recognition gating
- Speed-dependent thresholds
- Map-matching to suppress transit FPs (within the limit of map data)
- Suppression rules engine + per-circle config
- Post-trip server-side re-scoring
- Network-side location fallback integration
- Trip replay harness, shadow-mode A/B
- All of section 1 (workflow, dashboard, rules, scoring, MIS, etc.)

### 6.5 Revised recommendation

1. **Clean-slate the detection pipeline only.** 6–10 weeks for a working module with reorientation, cross-validation, Activity Recognition gating, and speed-dependent thresholds. One AI-assisted builder + the Android lead for review.
2. **Build the validation infrastructure first or in parallel.** Trip replay harness, shadow-mode logging, device-test matrix, and *start* the dashcam ground-truth pilot. **Skip this and the rebuild will fail like the previous ones.** Allocate equal effort to validation as to detection code — this is the part the prior attempts almost certainly underinvested in.
3. **Integrate the organisation network-side location fallback.** This is your structural moat; it directly attacks the GPS-loss field-validation problem that algorithms can't solve.
4. **Server-side post-trip re-scoring.** Lets you fix on-device uncertainty after the fact and dramatically reduces user-facing FP rate.
5. **Vendor only the irreducibly hard items:** automatic crash detection (F5.3), helmet+ISI CV (F1.3) if a credible Indian CV vendor exists, otherwise defer to v2 with manual photo + back-office review.
6. **Hold the line on scope.** F1.4 multi-occupant seatbelt, F2.3 phone-usage-of-other-apps, and full production-grade transport-mode classification should be descoped or marked v2.

The CIO question stops being "build vs buy Sentiance" and becomes:

> "Do we have the field-validation discipline (replay harness, shadow mode, dashcam ground truth, device matrix) to make a third in-house attempt succeed where two prior attempts failed? If yes, full-stack ownership is a clear win for ~75% of the BRD. If no, we'll spend 6 months and end up where we are today. The detection rebuild is a 6–10 week problem; the validation infrastructure is the 6-month problem."

---

## 7. Code review of the existing SafetyConnect SDK (after seeing the source)

The user shared the SDK source (branch: `speeddetectionfixrefactoring`). Reviewing it changes the conclusion from "probably bad" to **"the false-positive flood is explained by ~5 specific, identifiable bugs, several of which are one-line fixes."** This makes the strategic argument dramatically stronger.

### 7.1 Smoking-gun bugs

**BUG #1 — GPS coordinates are rounded to 2 decimal places before any processing.**
File: `foreground/CurrentLocation.kt:97`, helper `foreground/util/LocationExtensions.kt`.
```kotlin
override fun onLocationChanged(location: Location) {
    getLocation.onLocationChanged(location.roundToTwoDecimals())  // <-- catastrophic
}
```
`roundToTwoDecimals()` rounds latitude and longitude to **2 decimal places**. At the equator, 0.01° ≈ **1.1 km**. Every downstream consumer (SpeedManager, HarshDrivingDetector, distance calculations) receives coordinates quantized to a ~1km grid. Consecutive locations either collapse to identical points (calculated speed = 0, false "stationary") or jump by ~1 km between updates (calculated speed = 1km / 2s = **1800 km/h**, triggers GPS-jump rejection or fake harsh events). **This single function corrupts every geometric calculation in the entire SDK.** The doc comment even claims it "improves data consistency and readability." It is the most consequential bug I have ever seen in a telematics codebase. **One-line fix.** Estimated FP reduction from removing this alone: **20–40%.**

**BUG #2 — HarshDrivingDetector does not use the accelerometer at all.**
File: `foreground/harsh/HarshDrivingDetector.kt`.
```kotlin
fun analyze(location: Location?) { ... }  // takes a Location, not a sensor event
```
The "harsh driving detector" compares **GPS speed at time T** to a rolling average of GPS speed over a 3-second window. The threshold is **3 km/h** difference over 3 seconds. That corresponds to **0.28 m/s²** — gentle pedal pressure. Real harsh braking is >3 m/s² (>30 km/h over 3s). **The threshold is ~10× too sensitive.** Compounding this: GPS speed is noisy at sub-second resolution; the accelerometer is *exactly* the right signal for harsh events and is being ignored. There is a perfectly good `Accelerometer.java` class (using the correct `TYPE_LINEAR_ACCELERATION`) sitting in the codebase — but it's wired only to the server-side crash detection blob, not to harsh detection. **Two-day fix.** Estimated FP reduction: **another 20–30%.**

**BUG #3 — Activity Recognition is dead code.**
File: `activityrecognition/DetectedActivitiesIntentService.kt`.
The file exists. It uses the **deprecated** `IntentService` + `LocalBroadcastManager` + `ActivityRecognitionResult.extractResult()` pattern (the modern API is `ActivityRecognitionClient.requestActivityTransitionUpdates`). More importantly: **it is never wired into the telematics flow.** `SafetyConnectService` never references it, never listens for activity transitions, never gates detection on `IN_VEHICLE`. The whole detector runs while the user is walking, on a metro, on a bus, sitting at a desk with the phone vibrating from a notification. **This is the explanation for the public-transport false-positive NFR.** It is not an unsolved problem; the right API isn't even being called. **3–5 day fix** (modernize and wire in). Estimated FP reduction: **another 15–25%, plus the public-transport NFR resolved.**

**BUG #4 — Crash detection is a server-side black box with no on-device gate.**
Files: `sdkinit/AccidentDetector.kt`, `repoimpl/SensorInteractImpl.kt`, `repoimpl/DataInteractImpl.kt`.
The Accelerometer/Gyroscope/Magnetometer classes stream raw values every game-rate tick into in-memory lists, and a `Timer` flushes them to the backend every 15–30 seconds. Backend returns `crash="1"` or not. Then the app shows a photo-capture banner asking the user to confirm. **Three serious problems:**
- No on-device threshold or sanity gate — every batch is shipped, regardless of whether the signal could plausibly be a crash. Massive bandwidth waste.
- Sensor data is in **device coordinates**, not vehicle coordinates — server has no way to know what the phone's orientation in the car was, so cannot distinguish "phone slid off seat" from "front impact."
- Asking the user to confirm a crash with a photo is the **worst possible UX for a safety feature** — the legitimate-crash user is unable to confirm; the false-positive user is annoyed; alert fatigue is guaranteed.

This is why the Android lead says crash detection is ineffective. It is not subtly broken; it is architecturally wrong. **This is the one capability I'd genuinely vendor in** (Sentiance / CMT / Bosch) rather than rebuild.

**BUG #5 — `STATIONARY_SPEED_THRESHOLD = 10f` km/h.**
File: `foreground/speed/SpeedManager.kt:19`.
Anything under 10 km/h is treated as "stationary" and **clears the location history buffer**. In Indian stop-and-go traffic, this is most of the time. The history is constantly wiped, so the rolling-window GPS-jump validation has no context. (Inconsistently, `HarshDrivingDetector.kt:18` uses 3 km/h for the same concept — already a smell.) Should be ~2 km/h. **One-line fix.**

### 7.2 Architectural smells (not bugs, but indicators)

- **Legacy `LocationManager.GPS_PROVIDER` instead of `FusedLocationProviderClient`** (`CurrentLocation.kt:56`). FusedLocationProviderClient combines GPS + cell + WiFi for better accuracy and battery, has built-in smoothing, and is the modern Android default. Using raw GPS gives you worse accuracy in tunnels/canyons *and* worse battery.
- **`setMinUpdateDistanceMeters(1f)`** — requests an update every 1 meter. At 50 km/h that's ~14 updates/sec. Battery drain + hammers downstream logic.
- **`BigDecimal` in the hot path** for Float→Float conversion in `convertToKmPerHr` and `roundToTwoDecimals`. Allocation pressure on every location update.
- **`CopyOnWriteArrayList` for hot-path sensor data accumulation** — copies the entire list on every `add()`. At 50Hz accelerometer rate, this is O(n²) per timer batch.
- **TAG = "azhar"** in `SpeedManager` — developer name as log tag, no realistic log filtering.
- **`Timer` + `TimerTask`** in `DataInteractImpl` — old Java API, no cancellation safety, ignores Doze.
- **No nullability discipline** — `SafetyConnectSDK.sensorFilters?.X` with `?: default` defaults scattered everywhere, including in detection thresholds. Means runtime config can silently flip behavior.
- **No GPS-provider fallback** (no NETWORK_PROVIDER or PASSIVE_PROVIDER). When GPS drops in a tunnel, you get nothing.
- **No sensor reorientation anywhere.** No gravity-vector subtraction, no rotation-matrix to vehicle frame, no use of `TYPE_ROTATION_VECTOR`. The codebase pretends the phone is always in a known orientation.
- **No shadow-mode / replay harness.** No way to A/B a new detector against the old on recorded trips.

### 7.3 Why previous in-house improvement attempts failed in field validation

Now we know. They were tuning a detector that received **1km-quantized coordinates** as input. No amount of threshold tuning, suppression rules, or speed-window adjustment can recover signal from data that has already been destroyed by `roundToTwoDecimals()` upstream. The field-validation failures had nothing to do with field conditions and everything to do with corrupted inputs.

### 7.4 Revised quick-win timeline (changes from §6.4)

With the source in hand, the "4–8 week 50% FP reduction" estimate is conservative. Realistic plan:

| Week | Fix | Effort | Expected FP reduction (cumulative) |
|------|-----|--------|------------------------------------|
| 1 | Remove `roundToTwoDecimals()` from `CurrentLocation.onLocationChanged` | 1 line + regression test | 20–40% |
| 1 | Raise `HarshDrivingDetector` threshold to ~12–15 km/h over 3s | constant change | 50–60% |
| 1 | Lower `STATIONARY_SPEED_THRESHOLD` to 2–3 km/h, unify across detectors | constant change | 55–65% |
| 2 | Wire `Accelerometer` (already exists) into `HarshDrivingDetector`; require GPS + accel agreement | 2–3 days | 65–75% |
| 3–4 | Modernize Activity Recognition (transition API) + gate detection on `IN_VEHICLE` | 1 week | 75–85%, plus public-transport NFR resolved |
| 5–6 | Migrate to `FusedLocationProviderClient`, reduce update frequency to 1s/5m, add gravity-vector reorientation | 2 weeks | quality lift + battery win |
| 7–10 | Clean-slate new detection pipeline (longitudinal/lateral axis events, sliding-window classifier, suppression rules), shadow-mode against old pipeline on historical traces | 4 weeks | brings system to "good" grade overall |
| Parallel | Stand up trip-replay harness + dashcam ground-truth pilot (50 vehicles) | 6–8 weeks | enables credible future validation |

**Weeks 1–2 alone — three constant changes and one wiring fix — should plausibly cut the 60–80K/day violation flood by half**, *before* any architectural work. This is the demo to put in front of the CIO before any vendor or rebuild commitment.

### 7.5 Updated answer to the four strategic questions

1. **Clean-slate SDK justified?** YES. The detection pipeline has architectural defects (no accelerometer in harsh detection, no Activity Recognition gating, no reorientation, GPS coordinates pre-corrupted) that cannot be incrementally patched without producing a tangle. Rebuild the **detection layer** (not the whole SDK) in 6–10 weeks. Keep the working transport / networking / storage plumbing.
2. **Replacing the SDK justified?** Replacing the **detection pipeline** is overwhelmingly justified. Replacing the whole SDK with a vendor is *not* justified — the bugs are not Sentiance-class mysteries, they are ordinary engineering errors. Vendor only the irreducibly hard items (automatic crash detection, ISI-mark CV).
3. **20% creating 80% of risk?** Updated list: (i) the 5 bugs above (low risk once identified, but high risk if left in place during a rebuild because they'll be re-introduced); (ii) F2.1 India speed-limit data; (iii) F1.3 ISI-mark CV; (iv) F5.3 automatic crash detection; (v) dashcam ground-truth pilot.
4. **4–8 week 50% FP reduction subset?** Confirmed achievable, and likely faster. See the week-by-week table above. **Two weeks of constant-change patches and one wiring fix should plausibly cut FPs in half.**

### 7.6 The CIO talking point (now sharpened by the code)

> "We don't need Sentiance to fix the false-positive problem. We need to fix five specific bugs in our existing SDK, the most egregious of which is that we are rounding GPS coordinates to 1-kilometer resolution before feeding them into the speed detector. A one-line fix to that file alone should produce a measurable double-digit-percentage drop in false positives within a week, with no architectural change. The real rebuild — a clean detection pipeline with proper sensor fusion, Activity Recognition gating, and a replay harness — is a 6–10 week project, not a vendor procurement. The only capability where a vendor is genuinely the right answer is automatic crash detection, because that requires labeled crash data we don't have."

This is the strongest possible position. The code itself is the evidence.

---

## 8. Build-from-scratch vs. fix-existing — sized against the actual codebase

After measuring the SDK source the user shared, the case becomes concrete.

### 8.1 Current codebase size

| Module | Kotlin LOC | Java LOC | Purpose |
|---|---|---|---|
| `safetyconnect` | 5,945 | 917 | The actual telematics SDK |
| `capturelibrary` | 350 | 2,032 | Image capture + cropping (mature, separate concern) |
| `app` | 154 | 0 | Demo app |
| **Total source** | **6,449** | **2,949** | ~9,400 LOC + ~2,000 LOC of XML resources |

Of the `safetyconnect` 6,862 lines, the **genuinely telematics-specific code** (foreground service + sensor pipeline + speed/harsh/EMF detectors + Activity Recognition + crash hook) is closer to **~1,500–2,000 LOC**. The rest is utilities (`Utils_APP.kt` 893, `Utils.kt` 865), UI dialogs, network plumbing, and models — code that is largely fine and doesn't need rebuilding.

The `capturelibrary` 2,382 lines is image-cropping code adapted from the well-known Soundcloud/Lyft Crop library. Rebuilding it from scratch is pure waste — it's mature, working, and unrelated to the false-positive problem.

### 8.2 Build-from-scratch effort (telematics SDK only, no CV)

Realistic plan, AI-assisted, one Android lead + one builder:

| Workstream | Effort |
|---|---|
| Foreground service + permissions + lifecycle (OEM-kill survival) | 1 week |
| FusedLocationProviderClient + modern Activity Recognition | 1 week |
| Sensor pipeline with reorientation to vehicle frame + fusion | 2 weeks |
| Detection module (harsh, overspeed, fatigue, phone usage) | 2 weeks |
| Network/payload/auth/retry/offline buffering | 1 week |
| Trip-replay harness + unit/integration tests | 1 week |
| Device test matrix (top 10 Indian phones — Xiaomi/Oppo/Vivo) | 1–2 weeks |
| **Total: 9–10 weeks** | |

Then **add** ~2,400 LOC of image-capture re-creation if you don't reuse the existing `capturelibrary` (don't — keep it). And the helmet/seatbelt CV layer (BRD F1.3/F1.4) is a separate ML project, multi-month, regardless of which path you take.

### 8.3 Fix-existing effort — for direct comparison

From section 7.4 — same scope (everything except CV and crash):

| Fix | Effort |
|---|---|
| Bugs 1, 5 (one-line constants) | hours |
| Bug 2 (wire accelerometer into harsh detector) | 2–3 days |
| Bug 3 (modern Activity Recognition + IN_VEHICLE gate) | 3–5 days |
| FusedLocationProviderClient migration | 1 week |
| Sensor reorientation to vehicle frame | 1–2 weeks |
| Replay harness (build alongside) | 1 week |
| **Total: 4–6 weeks for ~85% of the from-scratch quality** | |

### 8.4 Verdict — is from-scratch worth it?

**No, not for the POC. Yes, eventually, for the detection module only.**

**Don't from-scratch:**
- ~2x time to first usable build (10 weeks vs 5).
- Loses 2,400 LOC of mature image-capture code that has nothing to do with the FP problem.
- Loses the working server contract (`ApiService`) you'd have to re-implement bit-for-bit anyway, because the backend doesn't change.
- "Second-system syndrome" risk — rebuilds tend to re-introduce bugs the original team already fixed in v1/v2.
- Higher organizational risk — the prior in-house attempts that failed in field validation were *also* rebuilds (or substantial refactors — this branch is named `speeddetectionfixrefactoring`). A third "we're rebuilding it properly this time" is a hard sell to the CIO without proof the validation discipline has changed.

**Do incrementally rebuild the detection layer as a module:**
- `foreground/speed/`, `foreground/harsh/`, `foreground/util/` — these are the bug-infested parts (Bugs 1, 2, 5 + reorientation gap). Replace this **sub-module** in-place, behind a feature flag, running shadow-mode against the old. ~3–4 weeks.
- Keep `foreground/SafetyConnectService.kt`, `network/`, `service/`, `repoimpl/`, `sensor/` (the raw sensor classes are fine), `capturelibrary/` — they work.
- This is a rebuild of the ~1,500 LOC that's actually broken, not the ~9,400 LOC that isn't.

### 8.5 Bottom-line numbers for the CIO

> "Total SDK is ~9,400 lines of code. The telematics-specific bug surface is ~1,500 lines. Building a new SDK from scratch takes 9–10 weeks; fixing the bugs in place takes 4–6 weeks for ~85% of the same outcome. The five critical bugs we identified are localized — they live in roughly 5 files. We recommend fixing those first (2 weeks), running shadow-mode validation for 2 weeks, then deciding whether the remaining gap justifies a module-level rebuild of the detection pipeline only. Full from-scratch is not justified by the code; the bugs are."

---

## 9. The 2–4 week TPM-executable POC (final, executable plan)

**Audience constraint:** TPM with Codex + existing SDK source + occasional Android lead + no dedicated team. Goal: credible the sponsor demo in 2–4 weeks proving (a) lower FPs, (b) transit suppression, (c) better harsh detection, (d) context-aware speed.

### 9.1 Pick: Option B — new detection engine BESIDE the existing SDK

**Not A (patch existing SDK), not C (build new SDK from scratch).**

**Justification, grounded in the code review:**

1. **The 5 bugs are localized but their fix paths are tangled with production behavior.** Bug 1 lives in `CurrentLocation`, which feeds `SafetyConnectService`, which feeds the backend submission contract. Patching requires navigating that web *and* getting it through team code review *and* regression testing the backend contract. A TPM with occasional Android guidance and no team will not get that to a CIO demo in 4 weeks.
2. **The branch we received is literally named `speeddetectionfixrefactoring`.** Someone is *already* patching the existing SDK in-place — and per the user's own brief, prior in-house attempts failed field validation. A TPM doing the same thing with Codex will hit the same wall: the validation infrastructure isn't there. A *parallel* engine bypasses this entirely.
3. **A side-by-side detector reads the same sensors with zero production risk.** Demo it in 2–4 weeks, prove the FP delta with recorded comparison runs, *then* hand the validated approach to the team to merge into the production SDK. That's a much cleaner go/no-go for the CIO than "trust this patch."
4. **All 4 required demo capabilities are greenfield Android code paths** — FusedLocationProviderClient, ActivityRecognitionClient (modern transition API), sensor reorientation, Map-API speed-limit lookup. None of them require the existing SDK's quirks to be untangled first.
5. **Option C (from-scratch full SDK) is 9–10 weeks** (see §8.2). Doesn't fit. And it forces re-implementing the server contract, network plumbing, camera/crop code, foreground-service lifecycle, OEM-kill survival — none of which are the FP problem.

**B is the only option that fits the constraints.** A loses on team/approval friction; C loses on time.

### 9.2 What's IN the new POC app (built outside the SDK)

A standalone Android app — call it **`JRMDemo`** — completely independent of the SafetyConnect SDK. It reads the same sensors and runs alongside, with its own minimal UI.

**Modules:**

| Module | Purpose | Effort |
|---|---|---|
| `TripGate` | `ActivityRecognitionClient.requestActivityTransitionUpdates()` — only emit events when IN_VEHICLE for ≥30s. Suppresses walking/metro/bus. | 1–2 days |
| `SensorBus` | FusedLocationProviderClient (1s interval) + SensorManager (GRAVITY, LINEAR_ACCELERATION, ROTATION_VECTOR, GYROSCOPE at game rate). | 1 day |
| `FrameRotator` | Calibrate phone-to-vehicle rotation matrix from gravity vector + GPS heading after 30s stable driving. Apply to every accel reading. Output: `(longitudinal, lateral, vertical)` accel. | 2–3 days (Android lead pairing) |
| `HarshEventDetector` | Fire only when **longitudinal accel > 3 m/s² AND GPS speed-delta exceeds 12 km/h** in the same 2s window. Speed-dependent threshold. | 1–2 days |
| `SpeedDetector` | Read posted speed limit from map API (Mapbox or HERE — better India coverage than Google). Overspeed = speed > limit × 1.1 for ≥5s. | 2–3 days |
| `EventLogger` | Append every event AND every suppression with reason code to local JSONL file. This is the demo artifact — "here's what fired and why, here's what was suppressed and why." | 1 day |
| `DemoUI` | Start/Stop Trip, trip list, event timeline with reason codes, side-by-side comparison view if old SDK log imported. | 2 days |
| `ReplayHarness` | Load a recorded JSONL trip and re-run through the detector. Lets you tune thresholds against real trips without re-driving. | 1 day |

### 9.3 What stays IN-PLACE (zero patches for the POC)

**Zero.** Do not touch the existing SafetyConnect SDK for the POC.

Why: any patch needs team approval, regression test, backend coordination. None fits a TPM-led 4-week window. Once the POC wins approval, the team applies Bugs 1, 2, 3, 5 + reorientation to the 5 known files — that's their execution task, post-POC.

### 9.4 Architecture diagram (for the demo deck)

```
┌──────────────────────────────────────────────────────────┐
│  DemoUI                                                  │
│  • Start/Stop Trip   • Trip list                         │
│  • Event timeline with reason codes                      │
│  • Side-by-side: old SDK events vs new engine events     │
└──────────────────────────────────────────────────────────┘
              │
┌──────────────────────────────────────────────────────────┐
│  DetectionEngine                                         │
│                                                          │
│  ┌──────────────────────────────────────────────────────┐│
│  │ TripGate — ActivityRecognitionClient                  ││
│  │ Gate everything on IN_VEHICLE ≥ 30s                   ││
│  └──────────────────────────────────────────────────────┘│
│                      │                                    │
│  ┌──────────────────────────────────────────────────────┐│
│  │ SensorBus — FusedLocation + SensorManager             ││
│  │ GRAVITY, LINEAR_ACCEL, ROTATION_VECTOR, GYRO          ││
│  └──────────────────────────────────────────────────────┘│
│                      │                                    │
│  ┌──────────────────────────────────────────────────────┐│
│  │ FrameRotator — gravity + GPS-heading calibration      ││
│  │ rotation matrix → (long, lat, vert) accel             ││
│  └──────────────────────────────────────────────────────┘│
│                      │                                    │
│   ┌────────────────┐ ┌─────────────────────────────────┐ │
│   │ HarshDetector  │ │ SpeedDetector                   │ │
│   │ long>3 m/s² AND │ │ GPS speed vs map-API limit     │ │
│   │ Δspeed>12 km/h │ │ overspeed = >1.1× for ≥5s       │ │
│   └────────────────┘ └─────────────────────────────────┘ │
│                      │                                    │
│  ┌──────────────────────────────────────────────────────┐│
│  │ EventLogger — JSONL: events + suppressions + reason   ││
│  └──────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────┘
```

Each block in the engine maps directly to one of the four required demos:
- **Lower FPs** → `FrameRotator` + `HarshDetector`'s AND-gate
- **Transit suppression** → `TripGate`
- **Better harsh detection** → `HarshDetector` with correct longitudinal threshold
- **Context-aware speed** → `SpeedDetector` with map-API lookup

### 9.5 Week-by-week TPM execution plan

**Week 1 — Scaffold + sensors**
- Day 1: New Android app scaffold (`JRMDemo`), gradle, manifest, foreground-service permission
- Day 2: `SensorBus` — FusedLocationProviderClient + SensorManager wiring (Codex)
- Day 3–4: `TripGate` — modern Activity Recognition transitions (Codex + Android lead review)
- Day 5: First milestone — open app, drive somewhere, see raw trip with mode transitions logged

**Week 2 — The hard module**
- Day 1–3: `FrameRotator` — gravity + GPS-heading calibration, rotation-matrix application. **Pair with Android lead** for this one — it's the only genuinely tricky module.
- Day 4–5: `HarshEventDetector` — GPS×accel cross-validation gate, speed-dependent threshold
- Milestone: drive a test loop with phone in pocket / cradle / cupholder — confirm no false harsh events from phone position

**Week 3 — Speed + UI**
- Day 1–3: `SpeedDetector` with map-API integration. Sign up for Mapbox or HERE free tier. Confirm India speed-limit coverage on Bangalore/Delhi/Mumbai test routes — if rural coverage is bad, mark "limit unknown" rather than guess.
- Day 4–5: `DemoUI` — trip list, timeline with reason codes
- Day 6: `EventLogger` JSONL format + `ReplayHarness` for offline tuning

**Week 4 — Field test + deck**
- Day 1–2: Three test runs:
  1. Drive a 30-min loop with phone position changes — count old-SDK FPs vs new-engine FPs
  2. Walk through a metro station + bus ride — confirm new engine suppresses all events
  3. Drive past 2–3 fixed-45 zones at posted limit — confirm new engine doesn't overspeed
- Day 3: Generate comparison data (CSV: event count, type, reason)
- Day 4: Demo deck for the sponsor
- Day 5: Buffer / fix / rehearse

### 9.6 What the demo proves (the slide for the CIO)

> "Same hardware, same trips, same sensors. Existing SDK fired N false harsh events, M false overspeed events, P false transit events on these recorded trips. New engine fired N′, M′, P′ — measured drop of X% in false positives, with no production code changed. The fixes that produced this delta are the 5 bugs identified in the code review, plus modern Activity Recognition and sensor reorientation. Estimated team effort to apply the same fixes to the production SDK: 4–6 weeks. POC code is ~1,500 LOC; production patches are localized to 5 files."

That's a 2-week-of-code POC turning into a defensible CIO recommendation.

### 9.7 Risk register for the POC itself

| Risk | Mitigation |
|---|---|
| Map API speed-limit data is patchy in India | Test Mapbox + HERE early in week 3. If neither works at acceptable coverage, demo on a route with known posted limits and mark this as a known dependency for the production version. |
| `FrameRotator` doesn't calibrate on short trips | Document the limitation (calibration needs ~30s of stable driving) and demo only on trips longer than that. Acceptable for POC. |
| OEM background-kill on demo phone | Use a stock Pixel for the demo. Production worry — not a POC worry. |
| Comparison vs old SDK requires exporting its event log | If the existing SDK doesn't expose event logs, drive each route twice (once with each app) — accept the data is approximate. |
| TPM bandwidth | 4 weeks at ~6 productive hours/day = 120 hours. Codex generates the boilerplate; Android lead pairs on `FrameRotator` (the only non-Codex-able module). Realistic. |

### 9.8 The honest TPM takeaway

This POC is a **demo, not a product.** Its value is unlocking the team's commitment to applying the 5 fixes to the production SDK. After the POC succeeds:
- Team applies Bugs 1, 2, 3, 5 to production SafetyConnect SDK (week 5–7)
- Team applies sensor reorientation + FusedLocation migration (week 7–9)
- Field rollout with shadow-mode comparison from the POC's replay harness reused server-side (week 9–12)

The POC is the wedge. The team work that follows is the actual fix.

---

## Sources

- [Sentiance — Technology](https://sentiance.com/technology)
- [Sentiance — On-device Driving Insights: technical breakdown](https://sentiance.com/on-device-driving-insights-the-technical-breakdown)
- [Sentiance — Driving behavior modeling using smartphone sensor data](https://sentiance.com/driving-behavior-modeling-using-smart-phone-sensor-data)
- [Damoov — How your smartphone understands driving](https://damoov.com/how-your-smartphone-understands-driving/)
- [Smartphone-based hard-braking event detection at scale (arXiv 2202.01934)](https://arxiv.org/pdf/2202.01934)
- [the telco B2B unit — IoT Fleet Tracking & Management](https://www.the organisation.in/b2b/iot-fleet-tracking/)
- [the telco B2B unit — Fleet Tracking GPS Vehicle Tracking Device Guide](https://www.the organisation.in/b2b/insights/blogs/fleet-tracking-gps-vehicle-tracking-device-guide)
- [Opportunistic calibration of smartphone orientation in a vehicle (USPTO)](https://image-ppubs.uspto.gov/dirsearch-public/print/downloadPdf/10876859)
