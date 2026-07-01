# Journey Risk Management — Executive Feasibility Assessment

**Prepared for:** CIO; Program Sponsor; Security Leadership; Android Engineering Lead
**Author posture:** Principal Android Architect / Telematics Engineer / CTO Reviewer
**Stance:** Brutally honest. No optimism. No marketing. Technical truth.

---

# Executive Summary

The JRM business case is sound and the stated targets (zero fatalities, materially reduced lost-time injuries, ≥95% PPE verification) are appropriate. The barrier to achieving them is not unsolved telematics science — it is the **current implementation quality of the in-house telematics SDK**, plus a small set of capabilities in the BRD that genuinely require either labelled data we do not have, or vendor technology.

**The tens of thousands of false-positive violations per day are not the price of mobile telematics.** A code-level review of the existing SDK identified **five specific defects and three architectural shortcuts** that together explain the bulk of the false-positive flood. The largest single contributor is a one-line bug that rounds GPS coordinates to ~1 km resolution before the speed detector ever runs. None of these defects require advanced ML to correct.

**Why prior in-house improvement attempts failed in field validation:** they tuned thresholds on top of corrupted input data, and lacked a replay harness to measure improvement objectively. This is a missing-infrastructure problem, not a missing-skill problem.

**Recommendation: Option B — build a new detection engine alongside the existing SDK as a 4-week POC**, then have the engineering team apply the same fixes to the production SDK in a follow-on 4–6 week patch cycle. **Do not procure a telematics vendor SDK as a first move.** Procurement should be evaluated only after the POC has demonstrated the false-positive reduction is real and measurable — and even then, only for genuinely deep-tech capabilities (automatic crash detection, helmet certification-mark CV).

**Realistic delivery profile, solo TPM-builder + AI coding assistant + occasional Android-lead guidance:**
- 4 weeks: parallel-engine POC demonstrating false-positive reduction, transport-mode suppression, context-aware speed limits.
- 2–4 months: production-grade fixes applied to existing SDK; manager dashboard; risk scoring; consequence engine; document compliance.
- 6–9 months: full mainstream BRD scope ex computer-vision and ex automatic crash detection.
- **Not deliverable in any of the above timelines:** helmet certification-mark detection, multi-occupant seatbelt verification, automatic crash detection at acceptable accuracy. These require vendor evaluation or a dedicated multi-month ML programme.

**Security finding requiring immediate action regardless of program direction:** the existing SDK contains a hardcoded HTTP Basic Auth credential in source, transmitted to the production gateway on every API call. The credential value is a weak default. This must be rotated, removed from source, and migrated to runtime configuration as a separate security task, on its own timeline.

**The single biggest force multiplier across the program is not AI coding tools — it is field-validation infrastructure (replay harness, shadow-mode comparison, dashcam ground-truth pilot).** Without it, the next in-house improvement attempt will fail the same way the previous ones did. The POC includes the replay harness for exactly this reason.

---

# BRD Requirement Assessment

Classification legend:
- **Easily Buildable** — standard pattern, one builder, well-trodden Android APIs
- **Buildable** — mainstream engineering, may need occasional Android-lead review
- **Buildable With Android Support** — sustained Android-engineer pairing required for sensors / lifecycle / OEM-specific behaviour
- **Needs Significant Validation** — code can be written; the hard part is proving it works in the field (requires replay harness, device matrix, dashcam ground truth)
- **Needs ML Specialists** — requires labelled training data the program does not have, plus model training expertise
- **Better Solved By Vendor** — vendor IP is the moat; rebuilding in-house is not the right ROI

| Requirement | Buildable In-House | Difficulty | Confidence | Notes |
|---|---|---|---|---|
| **F1.1** Home-login geofence (500 m) | Yes | Easily Buildable | 95% | Android Geofencing API. GPS drift in dense urban housing → tune radius, allow manual override. |
| **F1.2** Vehicle-type selection pre-departure | Yes | Easily Buildable | 99% | Trivial UI. Cross-check selection against detected motion profile post-trip. |
| **F1.3** Helmet + certification-mark + chin strap + liveness CV | No | Needs ML Specialists | 30% in 6 months | No public dataset for the target market's helmet certification mark. The mark itself is a small-object detection problem. Liveness is anti-spoofing research. **Vendor or descope.** |
| **F1.4** Seatbelt across chest + passenger count CV | Partial | Needs ML Specialists | 50% single-occupant; <30% multi-occupant | Driver-only is tractable. Multi-occupant from front camera under night lighting is essentially infeasible. Descope multi-occupant. |
| **F1.5** Pre-journey weather + route + blackspot risk gating | Yes | Buildable | 90% | API stitching + rules engine. Blackspot data is internal; need to formalise the dataset. |
| **F1.6** Night-driving policy block | Yes | Easily Buildable | 99% | Pure rules. |
| **F1.7** Contextual micro-learning gate on poor risk profile | Yes | Buildable | 90% | Engineering is trivial. Content production is the bottleneck. |
| **F1.8** Document & vehicle compliance tracking | Yes | Easily Buildable | 98% | Standard CRUD + reminders. |
| **F2.1** Context-aware speed limits via map API | Yes | Buildable | 70% | **Largest external dependency in the program.** Speed-limit coverage is patchy across map vendors for the target geography. Mark "limit unknown" rather than guess. Evaluate multiple map providers. |
| **F2.2** Harsh acceleration/braking/cornering, 2W/4W thresholds | Yes | Buildable With Android Support | 75% to "good"; 40% to vendor-grade | Reorientation + cross-validation gets ~80% accuracy. Last 15% costs years of labelled data. Acceptable for BRD intent. |
| **F2.3** Phone usage during driving | Yes | Buildable With Android Support | 70% | Post-Android 10, you cannot observe other apps' content without intrusive accessibility permissions. **Spec must be relaxed to "screen-on + interaction events during trip."** |
| **F2.4** Fatigue (continuous driving time, mandatory rest) | Yes | Easily Buildable | 99% | Timer. |
| **F2.5** In-cabin audio/haptic nudges | Yes | Easily Buildable | 98% | TTS + vibration. |
| **F3.1** Automated consequence rule engine | Yes | Buildable | 95% | Engineering trivial; policy design is the work. |
| **F3.2** Evidence/artifact storage | Yes | Buildable | 95% | Storage cost at scale; consider tamper-evidence (hash chain) for audit defensibility. |
| **F4.1** Manager dashboard, real-time team risk, live maps | Yes | Buildable | 95% | Standard web app. Live-map cost grows with fleet scale. |
| **F4.2** Driver risk scoring | Yes | Buildable | 95% | Score formula design is the hard part. **Garbage-in if F2.x false positives are not fixed first.** |
| **F4.3** Gamification / leaderboards | Yes | Easily Buildable | 99% | Anti-gaming controls needed. |
| **F5.1** Roles & escalation hierarchy | Yes | Buildable | 95% | Standard RBAC. HR-data integration to keep org chart current is the integration cost. |
| **F5.2** Centralised safety broadcasts | Yes | Easily Buildable | 95% | Push notification fan-out. Delivery on OEM-killed apps in the target market is the failure mode. |
| **F5.3a** Manual SOS button + location dispatch | Yes | Easily Buildable | 98% | Persistent button, location-to-emergency-contacts. SMS gateway + SOS routing. |
| **F5.3b** Automatic crash detection | No | Better Solved By Vendor | 30% in-house | DIY threshold detection is worst-case FA/miss tradeoff. **Strong recommendation: specialist vendor.** Insurance-claim data is the moat we cannot replicate. |
| **F6.1** Automated MIS emails | Yes | Easily Buildable | 99% | Cron + templates. |
| **NFR** Unified host-application architecture | Yes | Buildable | 90% | Coordination problem, not technology. |
| **NFR** Existing distance-tracking module + telematics coexistence | Yes | Buildable With Android Support | 70% | Underestimated in BRD. Two background sensor consumers on aggressive-Android-skin OEMs requires foreground-service scheduling, battery-saver exemptions, per-OEM allowlist UX. Needs real Android engineer time. |
| **NFR** On-device CV <2 s | Yes | Needs ML Specialists | 60% | MobileNet inference is fast; small-mark detection adds pipeline depth. |
| **NFR** Public-transport suppression | Yes | Buildable With Android Support | 70% | Modern Activity Recognition + map-matching gets the bulk. Long-tail edge cases (suburban rail vs metro vs car-passenger) need labelled data. |

**Summary count:** of 26 items above, **17 are Easily Buildable or Buildable** in-house. **4 need Buildable With Android Support.** **4 are Needs ML Specialists.** **1 is Better Solved By Vendor.** This is not a deep-tech program; it is a mainstream engineering program with a small deep-tech tail.

---

# Existing SDK Findings

Source-code review was conducted on a recent branch of the existing telematics SDK (~9,400 lines of code). The branch name itself indicates an ongoing in-place speed-detection fix/refactor effort. Findings are grouped as **Bugs** (specific defects with clear fixes), **Architectural Limitations** (systemic shortcuts requiring refactor), and **Fundamental Telematics Limitations** (industry-wide constraints no implementation fully overcomes).

## Bugs

### Bug 1 — GPS coordinates rounded to ~1 km resolution before processing

- **Location:** location-reception layer, single helper function applied at the boundary
- **Problem:** Latitude/longitude rounded to 2 decimal places before being passed to the speed and harsh detectors. 2 decimal places of latitude ≈ 1.1 km. The helper's docstring claims it "improves data consistency."
- **Impact:** Every speed and distance calculation in the SDK operates on coordinates quantised to a ~1 km grid. Consecutive locations either collapse to identical points (false "stationary") or jump by ~1 km (calculated speed ~1800 km/h, triggers fake events). This single function corrupts every geometric calculation downstream.
- **Likely contribution to false positives:** 20–40%. The single highest-leverage bug.
- **Estimated effort to fix:** **One line.** Remove the rounding call from the data path. Rounding remains acceptable for display.
- **Supports:** Fix Existing SDK. Strongly suggests prior improvement attempts were tuning on top of this bug.

### Bug 2 — Harsh-driving detector ignores the accelerometer; threshold ~10× too sensitive

- **Location:** harsh-driving detection module
- **Problem:** Detector accepts a `Location` (not a sensor event) and compares GPS speed at time T to a rolling 3-second average. Threshold is 3 km/h Δ over 3 s ≈ 0.28 m/s². Real harsh braking is >3 m/s² (≈ >30 km/h Δ over 3 s).
- **Impact:** Detector fires on gentle pedal pressure. GPS speed is noisy at sub-second resolution and is the wrong signal for harsh-event classification. The accelerometer class exists in the codebase, correctly using `TYPE_LINEAR_ACCELERATION`, but is wired only to the server-side crash blob, not to harsh detection.
- **Likely contribution to false positives:** 20–30%.
- **Estimated effort to fix:** 2–3 days with occasional Android-lead review. Inject existing accelerometer class into the detector; require GPS + accel agreement; raise threshold to ~3 m/s²; suppress events below 10 km/h; branch 2W/4W thresholds.
- **Supports:** Fix Existing SDK.

### Bug 3 — Activity Recognition class file is dead code

- **Location:** activity-recognition module
- **Problem:** The file exists. It uses the deprecated `IntentService` + `LocalBroadcastManager` + `ActivityRecognitionResult.extractResult()` pattern (Android Marshmallow era). **It is never invoked from the foreground service.** No code in the telematics flow gates detection on `IN_VEHICLE`.
- **Impact:** Telematics runs while the user is walking, on a metro, on a bus, sitting at a desk with the phone vibrating. This is the entire explanation for the public-transport false-positive NFR.
- **Likely contribution to false positives:** 15–25%, plus the explicit transit-suppression NFR.
- **Estimated effort to fix:** 3–5 days. Replace with `ActivityRecognitionClient.requestActivityTransitionUpdates`; gate the foreground service on `IN_VEHICLE` ≥ 30 s.
- **Supports:** Fix Existing SDK.

### Bug 4 — Crash detection has no on-device gate; server-side black box

- **Location:** accident-detection module + sensor-interaction implementation
- **Problem:** Accelerometer/gyroscope/magnetometer values are streamed at game rate into in-memory lists, flushed every 15–30 s to the backend, which returns `crash="1"` or not. The app then displays a photo-confirmation banner asking the user "did you crash?"
- **Impact:** No on-device threshold gating — every batch ships regardless of plausibility (bandwidth waste). Sensor data is in **device coordinates**, not vehicle coordinates, so the server cannot distinguish "phone slid off seat" from "frontal impact." The photo-confirmation flow is the worst possible UX for a safety feature (a real crash victim cannot interact; a false-positive user is annoyed; alert fatigue is guaranteed). The Android lead's assessment that crash detection is ineffective today is correct.
- **Likely contribution to false positives:** crash-specific, but crash-FP cost is qualitatively higher (false safety dispatches).
- **Estimated effort to fix:** Significant. Even with on-device pre-filter and reorientation, in-house crash detection without labelled crash data will plateau well below vendor accuracy.
- **Supports:** **Vendor.** Recommend retaining only manual SOS in v1.

### Bug 5 — Stationary speed threshold is 10 km/h; wipes history in traffic

- **Location:** speed-management module
- **Problem:** Speed under 10 km/h is treated as "stationary" and clears the location/speed history buffer. The same concept is inconsistently set to 3 km/h in the harsh detector.
- **Impact:** In dense stop-and-go traffic typical of the target market, history is constantly wiped. GPS-jump validation has no context to operate against. Compounds Bugs 1–2.
- **Likely contribution to false positives:** 5–10% (compounding effect with other bugs).
- **Estimated effort to fix:** One-line constant change to ~2 km/h. Unify the constant across modules.
- **Supports:** Fix Existing SDK.

## Architectural Limitations

### Limitation A — No sensor reorientation to vehicle frame

- **Problem:** Accelerometer values across the entire SDK are in the **phone's** reference frame. There is no quaternion/rotation-matrix calibration to align phone axes with vehicle axes.
- **Impact:** Phone-in-pocket, phone-in-cupholder, loose-mount, hand-held scenarios all bleed gravity into horizontal axes. Ordinary motion reads as harsh events. This is the single biggest cause of harsh-event false positives that are unrelated to driving behaviour.
- **Effort to fix:** 1–2 weeks. Calibrate from `TYPE_GRAVITY` + GPS heading during stable IN_VEHICLE driving; apply rotation matrix to every linear-acceleration sample; recalibrate on gravity drift.
- **Supports:** Either path, but a clean module is easier to add to a new detection engine than to retrofit. Argues for Option B (parallel engine) for the POC.

### Limitation B — Legacy LocationManager.GPS_PROVIDER instead of FusedLocationProviderClient

- **Problem:** SDK uses raw `GPS_PROVIDER` with `setMinUpdateDistanceMeters(1f)` (every metre, ~14 updates/sec at 50 km/h).
- **Impact:** Worse accuracy in tunnels / urban canyons (BRD lists this as a known pain point). No built-in smoothing. Higher battery drain. No provider fallback when GPS drops.
- **Effort to fix:** 1 week. Migrate to `FusedLocationProviderClient` with 1 s interval, 5 m minimum distance, high-accuracy priority.
- **Supports:** Either path.

### Limitation C — Hot-path performance shortcuts

- **Problem:** `CopyOnWriteArrayList` for high-frequency sensor accumulation (O(n²) per timer tick); `BigDecimal` allocations in Float→Float conversions in hot paths; `Timer`/`TimerTask` for batch flushing (no Doze awareness).
- **Impact:** Battery drain (entry-level Android phones hit hardest); GC churn; data loss when Doze fires.
- **Effort to fix:** 1 week. Replace primitives with coroutines + `Flow.sample()` + bounded ring buffer.
- **Supports:** Either path.

## Security Finding (separate from telematics defects)

### Hardcoded Basic Auth credential in network module

- **Problem:** The network module sets a hardcoded `Authorization: Basic <token>` header on every outbound request to the production gateway. The base64-decoded credential is a weak default value. Both endpoint URL and credential are visible in source.
- **Impact:** Source-exposed production credential. Risk if source ever leaves the controlled environment (laptop loss, accidental public repo, contractor access). Weak default values are also exposed to anyone with source access today.
- **Estimated effort to fix:**
  1. Rotate the credential at the gateway. Hours.
  2. Remove header from source; inject from runtime configuration. 1–2 days.
  3. Migrate to short-lived bearer token (enterprise SSO/OAuth). 1 week.
- **Action:** Security Leadership should treat this as an immediate finding, decoupled from JRM program timeline.

## Fundamental Telematics Limitations (no implementation fully solves these)

| Limitation | Why it cannot be fully solved by any code change |
|---|---|
| GPS loss in tunnels, flyover underpasses, dense urban canyons | Physical signal occlusion. Mitigations: network-side cell-tower triangulation fallback (the host telco's existing infrastructure supports this — leverage it); short-window dead-reckoning from accelerometer. Both have hard accuracy ceilings. |
| OEM background-execution kills (aggressive Android-skin manufacturers common in the target market) | Per-manufacturer aggressive battery-savers. Not a code problem. Mitigations: per-OEM autostart-permission UX, maintained device-test matrix of top 10–15 phones. |
| Battery-saver / Doze under thermal load | OS-level resource governance. Cannot be code-overridden. |
| User-behaviour heterogeneity (phone position varies by trip and user) | Reorientation calibration helps but requires ≥30 s stable driving — short trips never reach calibration. |
| Ground-truth scarcity for harsh-event validation | You cannot label "this was a real hard brake" without dashcam + manual review. **Without ground truth, you cannot prove a new pipeline is better than the old.** Likely root cause of prior in-house improvement-attempt failures. **Solution: dashcam pilot on 50–100 vehicles for ~10K labelled trips. Non-negotiable for the program.** |
| Crash data scarcity | A few hundred historical incidents and a handful of fatalities per year is a small validation set, not a training set. Specialist vendors solve this with insurance-claim data the program does not have. |
| Rural posted-limit ambiguity | No map vendor has complete coverage in the target market. Even ground truth is contested (signage compliance varies). Accept gap and mark "limit unknown." |

---

# What Can Realistically Be Improved In 4–8 Weeks

High-confidence items only. Each is supported by direct evidence from the SDK review and uses APIs already available in the codebase or Android SDK.

## 1. False-positive reduction — Bug 1 fix (GPS rounding)

- **Why:** Single highest-leverage defect identified. Code review confirms its scope.
- **Expected impact:** 20–40% reduction in false-positive volume, measurable within days of deployment.
- **Technical approach:** Remove the rounding call from the location-reception path. Coordinate-rounding remains acceptable only for display.
- **Effort:** Hours, including a regression test.
- **Confidence:** Very high.

## 2. False-positive reduction — Bug 5 (stationary threshold) + consistency fix

- **Why:** Compounds with Bug 1. Inconsistent constants across modules indicate an unverified fix history.
- **Expected impact:** 5–10% additional reduction, primarily in stop-and-go traffic.
- **Technical approach:** Lower stationary threshold to ~2 km/h. Unify into a single configuration constant referenced by both speed and harsh modules.
- **Effort:** Hours.
- **Confidence:** Very high.

## 3. Harsh-event quality — Bug 2 fix (wire accelerometer + raise threshold)

- **Why:** Existing accelerometer class is unused in harsh detection. The 0.28 m/s² threshold is provably wrong.
- **Expected impact:** Cumulative 50–65% false-positive reduction after Bugs 1+5+2 applied together.
- **Technical approach:** Inject accelerometer; require GPS Δspeed AND longitudinal-axis accel agreement within a 2 s window before firing; threshold to ~3 m/s²; speed-gate <10 km/h; 2W/4W threshold branching.
- **Effort:** 2–3 days with occasional Android-lead review.
- **Confidence:** High.

## 4. Public-transport suppression — Bug 3 fix (modern Activity Recognition + IN_VEHICLE gate)

- **Why:** The transport-mode suppression NFR is currently failing because the API is not being called. Modern API is straightforward.
- **Expected impact:** Cumulative 70–85% false-positive reduction after all four bugs above. **Resolves the public-transport suppression NFR.**
- **Technical approach:** Replace deprecated `IntentService` pattern with `ActivityRecognitionClient.requestActivityTransitionUpdates`. Foreground service subscribes to transitions; detection is gated on `IN_VEHICLE` confidence ≥ 75 sustained ≥ 30 s.
- **Effort:** 3–5 days.
- **Confidence:** High.

## 5. Parallel-engine POC + replay harness

- **Why:** The single biggest reason prior in-house improvement attempts failed was the absence of a way to objectively compare new vs old detector output. The POC builds the validation infrastructure as a first-class artifact.
- **Expected impact:** Demo-ready evidence that the false-positive reduction claimed above is real, measured side-by-side on real trips. Reusable server-side for ongoing iteration.
- **Technical approach:** Standalone Android demo app that runs alongside the existing telematics SDK on the same phone, reads sensors independently, logs every event and every suppression with reason codes, and produces comparison CSVs.
- **Effort:** 4 weeks for one TPM-builder + AI coding assistant + occasional Android lead.
- **Confidence:** High.

## What 4–8 weeks will NOT deliver

- Sensor reorientation in production (lives in the POC engine; needs 1–2 weeks to add to the production SDK)
- FusedLocationProviderClient migration in production (1 week, post-POC)
- Helmet / seatbelt CV
- Automatic crash detection
- Manager dashboard
- Driver risk score
- Document compliance
- Anything outside the false-positive-reduction workstream

---

# What Can Realistically Be Delivered In 2–4 Months

Assumes the 4-week POC has been completed and accepted, and the team has greenlit applying the diagnosed fixes to the production SDK.

| Theme | Deliverable | Confidence |
|---|---|---|
| **False-positive reduction** | Bugs 1, 2, 3, 5 applied to production SDK; FusedLocationProviderClient migration; sensor reorientation pipeline upstream of harsh detector | **High (85%)** for cumulative ≥70% FP reduction; the actual figure will depend on the dashcam-validation pilot |
| **Transport suppression** | Modern Activity Recognition wired in; IN_VEHICLE gating in production | **High (90%)** for the NFR being met for metro / bus / walking |
| **Context-aware speed** | Map-API integration in production; segment-level posted-limit lookups; "limit unknown" fallback on coverage gaps | **Medium (70%)** — bounded by external map data quality on rural roads, not by engineering |
| **Harsh-event quality** | GPS+accel cross-validation; vehicle-frame reorientation; 2W/4W thresholds; speed-gating | **Medium-High (75%)** for "good" quality; vendor-grade (~95%) is not achievable in this timeline |
| **Manager dashboard** | Real-time team-risk overview; live journey maps; per-engineer event timeline | **High (90%)** — standard web engineering |
| **Driver risk scoring** | Composite score from event count + severity + history; per-engineer trend | **High (95%)** engineering; **Medium (70%)** for score validity, conditional on FP reduction working |
| **Governance** | Roles & hierarchy; consequence rule engine; evidence vault; centralised broadcasts; MIS emails | **High (90%)** across the set |
| **Documents & compliance** | Vehicle/driver document tracking with expiry alerts | **High (98%)** |
| **Manual SOS** | Persistent in-app SOS with location dispatch to emergency contacts | **High (98%)** |

**What 2–4 months will NOT deliver:**
- Helmet certification-mark CV
- Multi-occupant seatbelt CV
- Automatic crash detection at acceptable accuracy
- Dashcam ground-truth pilot results (the pilot itself starts in this window; results take 3–6 months)
- Production-grade transport-mode classification at the long-tail (suburban rail vs metro vs bus-in-traffic edge cases)

---

# Problems That Remain Hard

For each, the difficulty is categorised as: **Data** (training/validation data does not exist), **ML** (modelling expertise required), **Validation** (cannot be proven correct without infrastructure), **Device Diversity** (per-OEM behaviour), or **Industry-Wide** (no vendor fully solves it either).

## Automatic crash detection (BRD F5.3b)

**Why hard:** Data + Validation + Industry-Wide.
- Cannot ethically generate crash data. Only reconstruction from historical incidents is possible.
- A few hundred incidents and a handful of fatalities per year is a small validation set; nowhere near a training set.
- Distinguishing real impact from "phone dropped on the floor" requires correctly oriented sensor data AND a model trained on real crash signatures.
- Specialist vendors built their businesses around this exact capability over ~10 years using insurance-claim data the program does not have access to.

**Recommendation:** Do not build in-house. Ship manual SOS in v1. Run vendor evaluation in parallel.

## Helmet detection with certification mark + chin strap + liveness (BRD F1.3)

**Why hard:** Data + ML.
- Generic helmet object-detection is solvable.
- **Certification-mark verification is the hard sub-problem.** Small low-contrast embossed mark; variable lighting/angle/helmet type. No public dataset for the target-market certification mark.
- Building a dataset requires thousands of labelled helmet photos across geographies and conditions — a data-collection programme that takes months before any model training begins.
- Liveness detection at on-device speed under arbitrary lighting is an active research area.

**Recommendation:** Treat as a multi-month ML/data programme on its own track. Either specialist vendor or in-house programme; do not pretend it is a feature toggle in the main JRM track.

## Seatbelt detection — multi-occupant (BRD F1.4)

**Why hard:** ML + Device Diversity.
- Driver-only single-occupant case is tractable with pose estimation.
- **Rear-passenger detection from a front-mounted phone camera is essentially infeasible.**
- In-cabin lighting at night is poor for vision models.

**Recommendation:** Scope a single-occupant driver-belt check as v1. Defer or descope multi-occupant; treat as line-manager manual photo review.

## Production-grade transport-mode classification (NFR)

**Why hard:** Data + ML.
- Android Activity Recognition + map-matching gets the bulk of the value (>80% of cases).
- The long tail — suburban rail vs metro vs BRT bus vs car-in-traffic, distinguishing car-passenger from car-driver — requires labelled trip data per mode.
- Vendors achieve their accuracy here via years of accumulated user-trip data with manual review.

**Recommendation:** Accept that in-house will hit a ceiling around 75–85% on edge cases. For BRD intent (suppress obvious transit false positives), in-house is sufficient.

## Production-grade harsh-event accuracy (BRD F2.2)

**Why hard:** Data + Validation.
- The first 80% of accuracy is engineering (reorientation, cross-validation, thresholds, gating).
- The last 15% costs years of labelled data and field iteration.
- Vendor "95% accuracy" claims rest on data and customer base in-house cannot match in any reasonable timeline.

**Recommendation:** Target "good" (≥70% reduction in false positives, comparable true-positive rate). Acknowledge the accuracy ceiling. The dashcam pilot will tell you whether the ceiling is acceptable.

## Real-world telematics edge cases (BRD lists tunnels, signal loss)

**Why hard:** Industry-Wide.
- GPS occlusion in tunnels, underpasses, dense urban canyons is a physical signal problem.
- Best mitigation: network-side cell-tower triangulation as fallback. **The host telco already has the infrastructure for this; leveraging it as a backend service for the JRM app is a structural advantage no vendor SDK can match.** Worth explicit architectural commitment.
- Short-window dead-reckoning from accelerometer covers brief gaps.

**Recommendation:** Build the network-side fallback hook as a v1 architectural commitment. This is one place where the host telco can actually do better than vendors because of the SIM/network ownership.

## OEM background-execution survival on entry-level Android (cross-cutting)

**Why hard:** Device Diversity.
- Aggressive Android-skin manufacturers common in the target market kill background sensor consumers regardless of foreground-service status.
- This is per-manufacturer tribal knowledge; not solvable by algorithm.
- Mitigations: per-OEM autostart-permission UX, maintained device-test matrix, ongoing engineering hygiene.

**Recommendation:** Allocate sustained engineering time for the device matrix. Do not treat as a deliverable; treat as ongoing maintenance.

---

# Build vs Enhance vs Buy

## Option A — Enhance Existing SDK

**Pros:**
- No second codebase to maintain.
- Fixes land in production directly.
- Lowest total LOC delta.

**Cons:**
- Every change risks breaking production for thousands of field engineers.
- The SDK's quirks are tangled — bugs in one module feed downstream modules and the backend contract. Patches require navigating that web safely.
- Requires team approval, regression test, backend coordination on every change.
- The branch reviewed is named to indicate an ongoing in-place fix/refactor effort — someone is already attempting Option A, and prior attempts failed field validation. Repeating Option A with the same tooling pattern that produced prior failures, without new validation infrastructure, is the same bet as before.
- Hard to A/B test in production without significant additional engineering.

**Risks:** High. Patch quality cannot be measured before production rollout without a replay harness, which doesn't exist.

**Timeline:** 4–6 weeks of engineering, plus 4–6 weeks of validation cycles. Realistically 3–4 months end-to-end with the team's standard release process.

**Resource requirement:** Full Android engineer, full backend engineer, QA. Cannot be done by TPM-builder alone.

## Option B — Build New Detection Engine Alongside Existing SDK ✅ **RECOMMENDED**

**Pros:**
- Zero production risk. New code never reaches users in v1.
- Replay harness + side-by-side comparison built in from day one.
- Demonstrable in 4 weeks: same trip, both detectors, measured FP delta with reasons.
- TPM-builder + AI coding assistant + occasional Android lead is the realistic team profile.
- Produces validation infrastructure that's reusable forever — even if the detection module itself gets rewritten later.
- Converts the program's central question from "will it work?" to "the data says it works, here's the delta — now apply it to production."

**Cons:**
- A second codebase exists temporarily.
- Comparison vs old SDK requires the old SDK's event log to be exportable (or two separate drives — approximate but usable).
- Does not, by itself, change anything users experience.

**Risks:** Low. POC failure is recoverable; production is untouched.

**Timeline:** 4 weeks for POC; 4–6 weeks for team to apply fixes to production after POC validates approach.

**Resource requirement:** 1 TPM-builder + AI coding assistant + ~0.5 day/week of Android-lead time during POC. Production patch is a separate team task post-POC.

## Option C — Build Entirely New SDK from Scratch

**Pros:**
- Clean architecture; no accumulated quirks.
- Forces explicit decisions on every interface.

**Cons:**
- 9–10 weeks for telematics-only scope (no CV, no crash). Does not fit any reasonable window.
- Loses ~2,400 LOC of mature image-capture / cropping code that has nothing to do with the FP problem.
- Requires re-implementing the server contract bit-for-bit because the backend does not change.
- "Second-system syndrome" — rebuilds re-introduce bugs the original team already fixed in v1/v2.
- Same validation gap as Option A — without dashcam ground truth and replay harness, the new SDK fails field validation the same way the old one did.

**Risks:** High. The pattern of "rebuild fixes everything" has a very poor track record in telematics specifically.

**Timeline:** 9–10 weeks for first usable build; 4–6 months to reach feature parity with current SDK; longer for validation.

**Resource requirement:** Dedicated Android team. Not TPM-buildable.

## Option D — Buy a Specialist Telematics Vendor (Sentiance, Cambridge Mobile Telematics, HyperTrack, etc.)

**Pros:**
- Industry-leading false-positive suppression out of the box.
- Vendor-managed ML, vendor-managed validation.
- ISO 27001/27701 + GDPR posture is the vendor's problem.
- Automatic crash detection comes with the package (the genuinely hard capability we cannot match).
- Cuts time-to-credible-telematics from months to weeks of integration.

**Cons:**
- License cost at scale (vendor SDKs are typically per-vehicle/month or per-trip pricing).
- Vendor lock-in. SDK becomes external dependency.
- Loses the network-side advantage (cell-tower triangulation fallback) unless vendor exposes hooks for it.
- Cannot tune per-geography / per-role / per-fleet-customer.
- Cannot leverage internal incident history for blackspot scoring.
- Procurement, legal, data-residency review — typically 2–6 months before integration even begins.

**Risks:** Medium. Vendor reliability and pricing model risk; SDK behaviour on aggressive-Android-skin devices is mostly vendor-untested.

**Timeline:** 3–4 months including procurement + integration; immediate FP improvement upon integration completion.

**Resource requirement:** 2–4 integration engineers + procurement/legal/security cycles.

## Recommendation: **Option B**

**Justification, supported directly by the SDK review:**

1. **The bugs are diagnosed and localised.** Five specific defects in five identifiable files. Vendor procurement to fix what is essentially a five-bug list is a disproportionate response.

2. **Prior in-house improvement attempts (which were Option A in spirit) failed because the validation infrastructure was absent.** Option B is the only path that builds the validation infrastructure as a first-class artifact, not an afterthought.

3. **Option A is the same bet we have already lost twice.** The branch name reviewed is evidence of an attempt currently underway. Asking the team to make a further attempt with the same tooling pattern that produced the previous failures is not a serious proposal.

4. **Option C does not fit any reasonable window** and rebuilds infrastructure that is not the problem (image capture, network plumbing, server contract).

5. **Option D is the right move for the genuinely hard capabilities (auto-crash, possibly helmet CV) — but premature for the rest of the program.** Procuring a full telematics vendor to fix bugs we have already identified is paying for a Ferrari to deliver groceries.

6. **Option B's POC is the only path that produces the evidence base needed to make Option D decisions credibly.** After the POC, the question of vendor procurement is informed by measured data, not by speculation about what a vendor "might give us."

**Sequence:**

> **Week 0–4:** Option B — build parallel POC + replay harness.
> **Week 4–10:** Team applies Bugs 1, 2, 3, 5 + sensor reorientation to production SDK; FusedLocationProviderClient migration. Dashcam pilot kicks off in parallel.
> **Week 6 onwards:** Vendor evaluation for **automatic crash detection only**. Manual SOS ships in v1.
> **Month 3–9:** Mainstream BRD capabilities — dashboards, governance, MIS, document compliance, F1.x pre-journey controls ex CV.
> **Parallel track (multi-month):** Helmet CV — either vendor evaluation or in-house data-collection programme + ML hire.

---

# Recommendation To Leadership

## 1. What should the organisation do first?

**Approve the 4-week Option B POC.** It carries no production risk, produces measurable false-positive comparison data, and converts the program's central question from speculation to evidence. It does not require team allocation beyond ~0.5 day/week of Android-lead review. **It also builds the replay harness — the single most valuable infrastructure the program needs and currently lacks.**

In parallel, **action the security finding immediately** — rotate the hardcoded credential, remove it from source, migrate to runtime config. This is decoupled from the JRM program timeline and should not wait.

## 2. What should the organisation NOT do yet?

- **Do not procure a vendor telematics SDK as a first move.** Procurement before POC validation is paying for an unknown delta. After the POC, vendor evaluation is informed by measured baseline.
- **Do not greenlight a full SDK rebuild.** Option C is the wrong response to a localised five-bug problem.
- **Do not allocate the engineering team to "fix the SDK harder."** The same pattern has failed before. Option B is the change in pattern.
- **Do not promise BRD F1.3 (certification-mark helmet CV) or F1.4 (multi-occupant seatbelt) for v1.** Set executive expectations early that these are vendor-or-multi-month-ML items.
- **Do not promise F5.3b (automatic crash detection) without vendor evaluation.** Manual SOS in v1; auto-crash on a separate track.

## 3. What should be validated before any vendor procurement?

Before any telematics-vendor procurement enters legal/contracting:

1. **POC measured false-positive delta.** The POC produces side-by-side comparison CSVs. If the in-house fixes deliver ≥70% FP reduction (the target), the case for full-stack vendor procurement weakens substantially.
2. **Target-market device-coverage commitment from candidate vendors.** Most vendors are validated on US/EU device populations. Get a written commitment on aggressive-Android-skin OEM background-execution survival before contract.
3. **Data-residency posture.** If sensor data is allowed to leave the target market under the vendor's architecture, this must be reconciled with regulatory and security posture in advance.
4. **Auto-crash detection accuracy data on roads representative of the target market.** Most vendor accuracy claims rest on US/UK insurance-claim data. Local driving and road conditions differ materially.
5. **Whether the vendor SDK exposes hooks for the host telco's network-side cell-tower triangulation fallback.** If not, the organisation loses its structural advantage by adopting the vendor stack.

For **automatic crash detection specifically**, vendor procurement is justified earlier — the in-house data and capability gap is genuine and not closeable.

## 4. Which BRD requirements should be deferred or descoped?

| Requirement | Disposition | Reason |
|---|---|---|
| F1.3 Helmet + certification-mark + chin strap + liveness CV | **Descope from v1.** Vendor evaluation or multi-month ML programme on separate track. | No public dataset; certification-mark is small-object detection; liveness is research. |
| F1.4 Multi-occupant seatbelt | **Descope to single-occupant in v1.** Multi-occupant marked as line-manager manual photo review. | Rear-passenger detection from front camera is essentially infeasible. |
| F2.3 Phone-usage detection of OTHER apps | **Relax spec to "screen-on + interaction during trip."** | Post-Android 10, observing other apps' content requires intrusive accessibility permissions. |
| F5.3b Automatic crash detection | **Defer to vendor-evaluation track.** Manual SOS (F5.3a) ships in v1. | Data scarcity; cannot match vendor accuracy in-house. |
| F2.1 Context-aware speed limits | **Ship with "limit unknown" fallback explicit.** | External map data has coverage gaps in the target market that no engineering closes. |
| F4.2 Driver risk scoring | **Defer behind FP reduction.** Risk score should not ship until F2.x FP problem is fixed, or the scoring is garbage-in. | Score validity depends on detector quality. |

---

## Final word

The JRM program is **buildable**. It is not a deep-tech project; it is a mainstream engineering project with a small deep-tech tail (crash detection, helmet CV) that should be vendored or descoped. The current SDK's failures are diagnosed bugs, not telematics-frontier problems. **The risk to the program is organisational, not technical** — specifically, repeating the same pattern of unmeasured in-place improvement that has already failed previously. The Option B POC is the change in pattern. The dashcam ground-truth pilot is the change in capability. Without both, no further investment will produce different results.

There is nothing in the BRD that justifies declaring the program impossible. There is also nothing in the existing implementation that justifies declaring it ready. The path between those two states is concrete, measurable, and within the reach of the resources currently available.
