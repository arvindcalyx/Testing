# Journey Risk Management POC — Handoff Brief

**Purpose of this doc:** Self-contained brief for a fresh AI coding session to build a Journey Risk Management (JRM) Proof-of-Concept Android app. Generic; no proprietary content.

**Audience:** A new Claude/Codex session with no prior context. Reads this doc, opens the empty repo, and starts building.

---

## 1. The problem we're solving

A large field-services workforce (~thousands of mobile field engineers) drives daily as part of their job. Road incidents are the #1 source of workplace injuries (~70%) with ~300 incidents and ~10 fatalities per year across the workforce.

There is an existing telematics SDK embedded in the workforce's daily-use mobile app. It detects overspeeding, harsh braking, and harsh acceleration. It produces **60,000–80,000 violation events per day** — the vast majority of which are **false positives**. Users have learned to ignore the resulting alerts (alert fatigue), so the system no longer changes behaviour.

The existing SDK has known defects (audited separately): GPS coordinates rounded to ~1 km resolution before processing, harsh-event detection using GPS speed only with a threshold ~10× too sensitive, the Activity Recognition API present but never wired in, no sensor reorientation to the vehicle frame, no on-device gating for crash detection. Two prior in-house improvement attempts failed in field validation — most likely because they tuned thresholds on top of already-corrupted input data, and there was no replay harness / shadow-mode comparison to prove improvements.

**The POC's job is to demonstrate, side-by-side with the existing SDK, that a correctly-built detection pipeline produces dramatically fewer false positives — without modifying any production code.**

---

## 2. POC strategy: parallel engine, no production changes

Three options were considered:

| Option | Verdict |
|---|---|
| A. Patch the existing SDK in place | Rejected. Patches require team approval, regression test, backend coordination — none fit a 4-week solo POC. |
| B. **Build a new detection engine in a standalone demo app, side-by-side with the existing one** | **CHOSEN.** Zero production risk. Measurable side-by-side. TPM-executable. |
| C. Rebuild the SDK from scratch | Rejected. 9–10 weeks, doesn't fit window; forces re-implementing server contract, camera plumbing, lifecycle — none of which are the false-positive problem. |

The POC is a **demo, not a product.** Its value is producing measurable proof that the engineering team can then use to justify applying the same fixes to the production SDK in a follow-on project.

---

## 3. Tech stack (mandatory)

- **Android** (Kotlin), min SDK 26, target SDK 34+
- **Build:** Gradle (Kotlin DSL preferred)
- **Architecture:** single-module app, Jetpack Compose UI, Kotlin Coroutines + Flow
- **Location:** Google Play Services `FusedLocationProviderClient`
- **Activity Recognition:** Google Play Services `ActivityRecognitionClient` transition API (NOT the deprecated `IntentService` pattern)
- **Sensors:** Android `SensorManager` — `TYPE_GRAVITY`, `TYPE_LINEAR_ACCELERATION`, `TYPE_ROTATION_VECTOR`, `TYPE_GYROSCOPE`
- **Map API for posted speed limits:** Mapbox or HERE (decide in Week 3; Google Roads API speed-limit coverage in the target market is patchy/premium)
- **Persistence:** local JSONL files on app storage for trip traces and event logs
- **Background work:** foreground service with `locationType` for sustained tracking
- **Logging:** Timber

No backend required. No vendor SDKs. No ML models. No camera/CV (out of scope — see §8).

---

## 4. Architecture

```
┌──────────────────────────────────────────────────────────┐
│  DemoUI (Jetpack Compose)                                │
│  • Start/Stop Trip                                       │
│  • Trip list                                             │
│  • Event timeline with reason codes                      │
│  • Side-by-side comparison view (optional)               │
└──────────────────────────────────────────────────────────┘
              │
┌──────────────────────────────────────────────────────────┐
│  DetectionEngine                                         │
│                                                          │
│  ┌──────────────────────────────────────────────────────┐│
│  │ TripGate — ActivityRecognitionClient transitions      ││
│  │ Emit events only when IN_VEHICLE confidence ≥ 75      ││
│  │ for ≥ 30 consecutive seconds. Suppress otherwise.     ││
│  └──────────────────────────────────────────────────────┘│
│                      │                                    │
│  ┌──────────────────────────────────────────────────────┐│
│  │ SensorBus — FusedLocationProviderClient + SensorMgr   ││
│  │ Location every 1s; sensors at game rate (~50 Hz)      ││
│  └──────────────────────────────────────────────────────┘│
│                      │                                    │
│  ┌──────────────────────────────────────────────────────┐│
│  │ FrameRotator — phone-to-vehicle calibration           ││
│  │ Use TYPE_GRAVITY + GPS heading during stable          ││
│  │ IN_VEHICLE driving (≥ 30s) to compute rotation matrix.││
│  │ Apply to every linear-acceleration sample.            ││
│  │ Output: (longitudinal, lateral, vertical) accel.      ││
│  │ Recalibrate if gravity vector drifts > threshold.     ││
│  └──────────────────────────────────────────────────────┘│
│                      │                                    │
│   ┌────────────────┐ ┌─────────────────────────────────┐ │
│   │ HarshDetector  │ │ SpeedDetector                   │ │
│   │ Fires only if  │ │ GPS speed (FusedLocation)       │ │
│   │ |long_accel| > │ │ vs map-API posted limit         │ │
│   │ 3 m/s² AND     │ │ Overspeed = speed > limit × 1.1 │ │
│   │ |Δgps_speed| > │ │ sustained ≥ 5 s                  │ │
│   │ 12 km/h within │ │                                  │ │
│   │ 2 s window.    │ │ Mark "limit unknown" if map      │ │
│   │ Suppress under │ │ has no coverage — never guess.   │ │
│   │ 10 km/h.       │ │                                  │ │
│   └────────────────┘ └─────────────────────────────────┘ │
│                      │                                    │
│  ┌──────────────────────────────────────────────────────┐│
│  │ EventLogger — JSONL on app storage                    ││
│  │ One line per event AND per suppression                ││
│  │ Includes reason code so the demo can answer "why".    ││
│  └──────────────────────────────────────────────────────┘│
│                      │                                    │
│  ┌──────────────────────────────────────────────────────┐│
│  │ ReplayHarness — load saved trip, re-run pipeline      ││
│  │ Lets you tune thresholds against real trips           ││
│  │ without re-driving.                                    ││
│  └──────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────┘
```

Each engine block maps 1:1 to one of the four demos in §7:
- Lower FPs → `FrameRotator` + `HarshDetector` AND-gate
- Transit suppression → `TripGate`
- Better harsh detection → `HarshDetector` with correct threshold
- Context-aware speed → `SpeedDetector`

---

## 5. Mapping the (anonymised) Business Requirements to this POC

The full program has ~22 requirements grouped under Pre-Journey Controls, In-Journey Monitoring, Consequences, Analytics, Roles, MIS. **The POC scope is a deliberately narrow slice** — only the requirements where the false-positive problem can be demonstrably attacked in 4 weeks.

Classification legend:
- **POC** — built in this 4-week demo
- **Patch** — fix lives in the existing production SDK; team task after POC, well-understood
- **Defer** — out of POC scope; build later in a normal program track
- **ML** — requires labelled data / model training / ML specialists
- **Vendor** — better solved by a third-party SDK than built in-house

### 5.1 Pre-Journey Safety Controls

| Req | Description | POC? | Notes |
|---|---|---|---|
| F1.1 | Geofenced home-login check before trip | Defer | Trivial later (Android Geofencing API). Not relevant to FP demo. |
| F1.2 | Pre-trip vehicle-type selection (2W / 4W / public transit / cab) | POC (minimal) | Add a simple "trip type" picker on the Start Trip screen so the harsh-event thresholds can branch 2W vs 4W. |
| F1.3 | Helmet detection via computer vision (presence, certification mark, chin strap, liveness) | ML / Vendor | Out of POC. Multi-month ML project; no public dataset for the certification mark; liveness needs anti-spoofing. |
| F1.4 | Seatbelt detection via CV (driver belt across chest, passenger count) | ML / Vendor | Out of POC. Same reasoning. |
| F1.5 | Pre-journey weather + route risk gating | Defer | API-stitching exercise; not relevant to FP demo. |
| F1.6 | Night-driving policy block (e.g., 19:00–06:00) | Defer | Pure rules. Not relevant to FP demo. |
| F1.7 | Mandatory micro-learning gate on poor driver score | Defer | Workflow; not relevant to FP demo. |
| F1.8 | Vehicle-document compliance tracking (RC / Insurance / License) | Defer | CRUD; not relevant to FP demo. |

### 5.2 In-Journey Behaviour Monitoring (the core POC area)

| Req | Description | POC? | Notes |
|---|---|---|---|
| F2.1 | Context-aware speed limits (replace fixed 45/60 km/h with actual posted limit per road) | **POC** | `SpeedDetector` module. Hardest external dependency: map-API speed-limit coverage in the target geography. Mark "limit unknown" rather than guess. |
| F2.2 | Harsh acceleration / braking / cornering with low false positives, thresholds tuned per vehicle type | **POC** | `FrameRotator` + `HarshDetector`. Headline demo. Cornering can be approximated using lateral-accel component after reorientation. |
| F2.3 | Phone-usage detection during driving | Defer | OS-level screen-state + interaction events during a detected trip. Doable but not the FP headline. Note: post-Android 10 you cannot observe content of OTHER apps without intrusive accessibility permissions — spec must be relaxed to "screen-on + interaction events during trip." |
| F2.4 | Continuous-driving fatigue timer with mandatory rest break | Defer | Trivial timer. Not relevant to FP demo. |
| F2.5 | In-cabin audio / haptic nudges on unsafe behaviour | POC (minimal) | TTS + vibration on harsh event for demo theatre. |
| (NFR) | Public-transport suppression (no false alerts when on metro / bus / train) | **POC** | `TripGate` using modern `ActivityRecognitionClient`. Second headline demo. |

### 5.3 Consequences & Governance

| Req | Description | POC? | Notes |
|---|---|---|---|
| F3.1 | Rule-based consequence engine (warnings → micro-learning → line-manager notice) | Defer | Backend / dashboard. Not relevant to FP demo. |
| F3.2 | Evidence / artifact storage for audit | Defer | Backend. |

### 5.4 Analytics, Dashboards, Reporting

| Req | Description | POC? | Notes |
|---|---|---|---|
| F4.1 | Manager dashboard (real-time team risk, live maps, actionable insights) | Defer | Web app. Not relevant to FP demo. |
| F4.2 | Driver risk score | Defer | Garbage-in if FPs not fixed first; that's why POC focuses on FPs. |
| F4.3 | Gamification / leaderboards | Defer | UI + cron. Not relevant to FP demo. |

### 5.5 Roles, Governance, Communication

| Req | Description | POC? | Notes |
|---|---|---|---|
| F5.1 | Roles & escalation hierarchy | Defer | Standard RBAC. |
| F5.2 | Centralised safety broadcasts | Defer | Push-notifications service. |
| F5.3a | Manual SOS button with live-location dispatch | Defer | Standard. Ship in v1 of the program. |
| F5.3b | **Automatic** crash detection | Vendor | Strong recommendation: do NOT build in-house. Crash data is impossible to generate ethically; vendor (Sentiance / CMT / Bosch-class) is the only credible path. The POC should explicitly not attempt this. |

### 5.6 MIS / Communications

| Req | Description | POC? | Notes |
|---|---|---|---|
| F6.1 | Automated MIS emails (daily / weekly / monthly compliance and trend reports) | Defer | Cron + templates. |

### 5.7 Summary

- **In POC (4 weeks):** F1.2 (vehicle type), **F2.1 (context-aware speed), F2.2 (harsh detection)**, F2.5 (nudges), **public-transport suppression NFR**.
- **Patch later (team task, well-scoped):** all of the bugs in the existing SDK that correspond to the POC capabilities — once the POC proves the fixes work.
- **Defer (normal program track):** F1.1, F1.5–F1.8, F2.3, F2.4, F3.x, F4.x, F5.1, F5.2, F5.3a, F6.1.
- **Out of POC, requires ML or vendor:** F1.3 helmet CV, F1.4 seatbelt CV, F5.3b automatic crash.

---

## 5.8 Root-cause bugs in the existing SDK → business pain → fix path

A code review of the existing telematics SDK identified five specific bugs plus three architectural deficiencies that account for the majority of the daily false-positive flood. **Each bug is the diagnosed cause of a specific business pain point in the BRD.** This is the table the fresh AI session needs to internalise — it explains *why* the POC architecture in §4 looks the way it does.

### 5.8.1 The five smoking-gun bugs

| # | Bug (generic) | BRD pain point it causes | POC demonstration | Production patch (post-POC, ~1 file each) |
|---|---|---|---|---|
| 1 | GPS coordinates rounded to 2 decimal places (~1 km grid) **before** any processing. Doc-comment claims it "improves data consistency." | "Employees frequently prove data accuracy issues, particularly regarding incorrect speed tracking" — and most FP events stem from corrupted distance/speed calculations | New engine never rounds raw GPS; passes full precision through the pipeline | Remove the rounding call from the location-reception layer. One-line patch. Single largest FP win available. |
| 2 | Harsh-event detector uses **GPS speed only** (no accelerometer). Threshold is 3 km/h delta over 3 s ≈ 0.28 m/s² — about 10× too sensitive (real harsh threshold ≈ 3 m/s²) | "Tracking scope focuses only on overspeeding, harsh braking, harsh acceleration"; users ignore alerts due to volume; alert fatigue | `HarshEventDetector` requires longitudinal accel AND GPS-Δspeed agreement; threshold set at engineering-correct value; speed-dependent gate | Inject existing accelerometer class into harsh detector; raise threshold to ~3 m/s²; add speed-gate. 2–3 day patch. |
| 3 | Activity Recognition class file present but **never wired in**; uses deprecated 2015-era `IntentService` + `LocalBroadcastManager` pattern | "SDK cannot detect the type of transport being used. False alerts generated even when employees are safely using public transport" — the explicit NFR | `TripGate` uses modern `ActivityRecognitionClient` transition API; gates all detection on `IN_VEHICLE ≥ 30s` | Replace deprecated API; wire into foreground service. 3–5 day patch. Solves the public-transport NFR entirely. |
| 4 | Crash detection has no on-device gate. Raw sensor blobs streamed every 15–30 s to backend; backend returns `crash=1` or not; user then asked to confirm via photo. Sensor data in device frame, not vehicle frame. | "9 fatalities in FY26" + Android lead says crash detection is ineffective; photo confirmation is wrong UX for a safety feature; bandwidth waste | POC explicitly does NOT attempt crash detection — flag as vendor-track in the demo deck | Production: add on-device peak-magnitude pre-filter; apply reorientation; replace photo-confirm with "Are you OK?" countdown. Or vendor it (recommended). |
| 5 | Stationary speed threshold set at 10 km/h; below this, location/speed history is **cleared**. Inconsistent (3 km/h elsewhere in same codebase) | Stop-and-go traffic constantly wipes history → GPS-jump validation has no context → speed readings unstable → more false events | New engine uses 2 km/h threshold; shared constant across detectors | Lower the constant; unify across files. One-line patch. |

### 5.8.2 The three architectural deficiencies

| # | Deficiency | BRD pain point it causes | POC approach | Production approach |
|---|---|---|---|---|
| A | **No sensor reorientation to vehicle frame.** Accelerometer values are in the phone's reference frame, so phone position (pocket / cupholder / loose mount) bleeds gravity into the horizontal axes — ordinary motion reads as harsh events | The single biggest cause of harsh-event false positives unrelated to actual driving | `FrameRotator` module: calibrate phone-to-vehicle rotation matrix from gravity vector + GPS heading after 30 s stable driving; apply to every accel sample | Add same reorientation pipeline upstream of the harsh detector. 1–2 weeks. |
| B | Uses legacy `LocationManager.GPS_PROVIDER` instead of `FusedLocationProviderClient`; sets minimum-update-distance at 1 metre (→ ~14 updates/sec at 50 km/h) | Worse accuracy in tunnels / urban canyons (BRD lists "GPS loss, tunnels, signal interruptions" as known pain); worse battery; hammers downstream logic | `FusedLocationProviderClient` with 1s interval, 5m minimum distance, high-accuracy priority | Same migration. Standard Android pattern. 1 week. |
| C | High-frequency sensor data accumulated in `CopyOnWriteArrayList` (O(n²) per batch); `BigDecimal` allocations in hot path; `Timer`/`TimerTask` for batch flushing (ignores Doze) | Battery drain on field-engineer phones (low-end devices hit hardest); data loss during Doze | Coroutines + `Flow.sample()` + bounded ring buffer | Replace data accumulation primitives; coroutines for batching. ~1 week. |

### 5.8.3 The root-cause lesson for the executive sponsor

> "The daily false-positive flood is not the price of doing smartphone telematics. It is the cumulative effect of five identifiable bugs and three architectural shortcuts. None of them require advanced ML or a vendor SDK to fix. The largest single contributor is a one-line bug that rounds GPS coordinates to 1 km resolution before the speed detector sees them — fixing that one line alone is expected to produce a double-digit-percent reduction in false positives within a week. The POC demonstrates this in a parallel app, side-by-side with the existing pipeline, so the engineering team has a measured baseline before they touch production code."

### 5.8.4 What this section does NOT cover

These are real but separate from the POC scope:
- **Credentials / secrets in source** (any project's network module should be audited for hardcoded auth — flag to security, rotate, move to runtime config).
- **OEM background-execution survival** on aggressive Android skins. Real production concern; flagged in §9.
- **Helmet / seatbelt CV (BRD F1.3 / F1.4)** — separate ML project, multi-month, needs labelled data.
- **Automatic crash detection (BRD F5.3b)** — vendor track.
- **Map-API speed-limit coverage** in the target geography — real external dependency, flagged in §6 and §9.

---

## 6. Week-by-week execution plan

Assumes one TPM + AI coding assistant + occasional senior Android engineer review (~half a day per week).

### Week 1 — Scaffold + sensors

- App scaffold (single-module Android app, Compose, foreground-service permission)
- `SensorBus` — `FusedLocationProviderClient` (1 s interval, high accuracy) + `SensorManager` registrations for the four sensor types
- `TripGate` — `ActivityRecognitionClient.requestActivityTransitionUpdates()` with transitions for `IN_VEHICLE`, `WALKING`, `RUNNING`, `ON_BICYCLE`, `STILL`. Gate emits Boolean `isDriving` Flow.
- **Milestone:** open app, drive 10 minutes, see raw trip with mode transitions logged.

### Week 2 — The hard module + harsh detection

- `FrameRotator` — calibration from gravity vector + GPS heading during stable driving. Rotation-matrix application to linear-acceleration. Recalibrate on gravity drift. **This is the only module that needs senior-engineer pairing.** Reference: any standard reference on quaternion-based reorientation; ample examples in open-source telematics libraries.
- `HarshEventDetector` — sliding window over rotated longitudinal accel + GPS speed delta. AND-gate. Speed-dependent threshold (suppress at <10 km/h).
- **Milestone:** drive a 20-minute loop with phone in cradle, then pocket, then cupholder. Confirm zero false harsh events caused by phone position.

### Week 3 — Speed + UI

- `SpeedDetector` — map-API integration. Sign up for Mapbox (preferred) or HERE free tier. Implement posted-limit lookup at current location. Cache aggressively to stay in free tier. **Mark "limit unknown" rather than guess on rural roads.**
- `DemoUI` — Compose screens: trip list, trip detail with event timeline, each event showing its reason code.
- `EventLogger` — JSONL format. One file per trip. Schema in §10.
- `ReplayHarness` — load a trip's raw-sensor JSONL and re-run through the detector. Crucial for threshold tuning without re-driving.
- **Milestone:** drive past 2–3 fixed-low-limit zones at actual posted limit. New engine reports correct overspeed status; existing SDK (run alongside on same phone or second phone) would have fired false overspeed.

### Week 4 — Field test + demo deck

- Three structured test runs:
  1. **Harsh-event run:** 30-minute drive loop, phone position changes at minute 10 and 20.
  2. **Transit-suppression run:** Walk 5 min, ride bus/metro 10 min, walk 5 min — both apps running.
  3. **Context-speed run:** Drive past 3 fixed-low-limit zones at posted limit + 1 deliberate moderate overspeed event for true-positive verification.
- Generate comparison CSVs: count of events fired, per type, per app, with reasons.
- Demo deck (≤ 10 slides) for the executive sponsor: problem framing, what was wrong, what the POC changed, the comparison numbers, the ask for follow-on engineering.

---

## 7. What the demo proves (the slide for the sponsor)

> "Same hardware, same trips, same sensors, no production code changed. The existing detection pipeline fired **N₁** false harsh events, **N₂** false overspeed events, **N₃** false transit-induced events across these recorded trips. The new POC engine fired **N₁′, N₂′, N₃′** on the same trips — measured drop of **X %** in false positives.
>
> The new engine differs from the existing one in five specific, well-understood ways: it preserves GPS precision, it requires accelerometer agreement before flagging a harsh event, it gates everything on Activity Recognition's IN_VEHICLE signal, it reorients sensor data to the vehicle frame, and it reads the actual posted speed limit from a map API instead of using a hard-coded value.
>
> Estimated engineering effort to apply the equivalent fixes to the production SDK: **4–6 weeks of one Android engineer.** POC source code is ~1,500 LOC, available for review."

---

## 8. Explicit out-of-scope (do not build in the POC)

- Camera / computer vision (helmet, seatbelt, certification-mark detection)
- Automatic crash detection (recommend vendor in production; not POC-scope)
- Backend services (event ingestion, scoring API, dashboard)
- User management, authentication, RBAC, organisational hierarchy
- Push notifications, SMS, email, IVR
- Document compliance tracking (RC / insurance / licence)
- Geofencing for home-login or blackspots
- Weather / route-risk API integration
- Micro-learning content / gamification
- Multi-language UI (English-only for POC)
- Tablet layouts, foldables, Wear OS

If the user asks for any of the above, push back: it dilutes the FP demo and inflates the timeline.

---

## 9. Things that genuinely cannot be done in the POC (hard limits)

| Limitation | Why | How the demo handles it |
|---|---|---|
| Posted speed limits may be missing on rural roads | Map vendors do not have complete India coverage | `SpeedDetector` marks segment as "limit unknown" rather than guess; demo route should be chosen on roads with confirmed coverage. |
| Calibration needs ≥ ~30 s of stable driving | Reorientation needs a stable gravity + heading signal | Demo only on trips longer than that. Short trips show as "uncalibrated" — accept and document. |
| Cannot prove production-grade harsh-event accuracy | No labelled ground-truth trips available in POC scope | Demo claims **"large reduction in false positives versus existing pipeline,"** NOT "vendor-grade accuracy." Manage expectations on the slide. |
| Cannot demonstrate auto-crash detection | Requires real crash data + ML training | Slide explicitly recommends vendor for this capability. |
| Cannot demonstrate helmet / seatbelt CV | Requires labelled image data + model training | Slide acknowledges as a separate workstream. |
| Background-execution survival on aggressive OEM Android skins (some manufacturers' battery-savers aggressively kill foreground services) | Per-OEM autostart / battery-exemption UX is a real-product concern, not a POC concern | Demo on a stock Android device (Pixel). Flag as a production-engineering item. |
| Side-by-side comparison requires the existing SDK to expose its event log | If existing SDK does not, the comparison is approximate | Drive each route twice, once with each app, on the same phone if possible — accept that the comparison is illustrative, not bit-exact. |

---

## 10. Suggested JSONL schemas (so AI can scaffold quickly)

**Trip header line** (first line of each trip file):
```json
{"type":"trip_header","trip_id":"<uuid>","start_ts":"<iso8601>","vehicle_type":"4W","app_version":"0.1.0","device_model":"Pixel 7","os_version":"34"}
```

**Sensor sample line** (high-frequency):
```json
{"type":"sensor","ts":"<iso8601>","ax":0.12,"ay":-0.03,"az":9.78,"gx":0.0,"gy":0.0,"gz":0.0,"gravity_x":0.1,"gravity_y":-0.2,"gravity_z":9.79}
```

**Location sample line:**
```json
{"type":"location","ts":"<iso8601>","lat":12.971598,"lng":77.594566,"speed_mps":12.5,"bearing":87.4,"accuracy_m":4.2,"has_speed":true}
```

**Activity-recognition transition line:**
```json
{"type":"activity","ts":"<iso8601>","activity":"IN_VEHICLE","transition":"ENTER","confidence":92}
```

**Event line:**
```json
{"type":"event","ts":"<iso8601>","event":"HARSH_BRAKING","severity":"MEDIUM","long_accel_mps2":-3.7,"gps_speed_delta_kmh":-14.2,"window_s":1.8,"reason":"long_accel and gps_delta both exceeded thresholds"}
```

**Suppression line** (this is what makes the demo defensible):
```json
{"type":"suppression","ts":"<iso8601>","would_be_event":"HARSH_BRAKING","reason":"gps_delta exceeded but long_accel did not — likely GPS noise, not real braking"}
```

Suppressions are first-class artifacts. They are how we prove, line-by-line, why the new engine fires less often than the old.

---

## 11. Definition of Done for the POC

The POC is complete when **all** of these are true:

1. Standalone Android app runs end-to-end: start trip → drive → stop trip → view event timeline.
2. `TripGate` correctly suppresses all events during a walking segment AND during a metro / bus ride (verified in test run 2).
3. `FrameRotator` produces stable longitudinal/lateral/vertical axes within 60 s of starting a stable drive (logged + visualisable).
4. `HarshDetector` fires on deliberate hard braking (true positive) AND does NOT fire on pothole / phone-handling events (no false positive).
5. `SpeedDetector` reads posted limit from map API on a route with verified coverage AND correctly classifies overspeed vs in-limit.
6. `EventLogger` produces JSONL with reason codes for every event AND every suppression.
7. `ReplayHarness` can load a saved trip and produce the same event sequence as the live run.
8. Comparison CSVs generated for at least 3 test trips showing event counts vs the existing pipeline.
9. ≤ 10-slide demo deck ready for the sponsor.

---

## 12. What the fresh AI session should ask the user before starting

Before writing any code, the fresh session should confirm:

1. **Map API choice:** Mapbox or HERE? (Mapbox preferred; both have free tiers. Need an API key from the user.)
2. **Target Android device** for the demo (recommend stock Pixel for predictable background behaviour).
3. **Vehicle type** for the primary demo drives (2W or 4W) — affects default thresholds.
4. **Existing-SDK event-log access:** can the existing app's events be exported for true side-by-side, or does the comparison have to be from two separate drives?
5. **Repository:** confirm the new repo location and whether to initialise with a standard Android Studio project structure or a more minimal Gradle scaffold.

---

## 13. Hand-off summary for the next session

Read this whole doc. Then:

1. Set up the Android project per §3.
2. Build the engine in the order in §4 (TripGate → SensorBus → FrameRotator → detectors → logger → UI → replay).
3. Stick to the week-by-week plan in §6. If you find yourself adding things from §8 (out-of-scope), stop and check with the user.
4. The headline deliverable is the comparison data in §7, not pretty UI. Optimise for "we can prove the difference."
5. Acknowledge §9 limits up front in the demo deck — managing expectations is part of the deliverable.

Total scope: ~1,500 LOC of Kotlin, 4 weeks, one TPM + AI + part-time Android engineer review.
