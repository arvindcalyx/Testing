# Final Prompt — Merge + Docs (Claude Code session)

Paste this whole block into the same Claude Code session you used for the 4 bug fixes (since it's still yellow-zone) OR a fresh session — either works since the prompt is self-contained.

---

## THE PROMPT

```
Final step on the SafetyConnect SDK personal repo. Four bug fixes are
committed on `applied-fixes`. We're now going to:

1. Merge applied-fixes into main (so main becomes the working trunk with
   scrubbed SDK + 4 fixes).
2. Delete applied-fixes (it's served its purpose).
3. Add three documentation files under `docs/` so future fix sessions have
   durable context.
4. Commit + push.

STEP-BY-STEP (run these in order, confirm each before proceeding):

1. Sanity-check current state:
       git checkout applied-fixes
       git log --oneline main..applied-fixes
   Expect: exactly 4 commits (fix(location), fix(speed), fix(harsh),
   fix(activity-recognition)). If anything else is on this branch, STOP
   and report.

2. Merge applied-fixes into main as a fast-forward:
       git checkout main
       git pull --ff-only origin main
       git merge --ff-only applied-fixes
       git push origin main
   If fast-forward isn't possible (merge conflict, divergent history),
   STOP and report — do NOT force-push, do NOT create a merge commit.

3. Delete applied-fixes branch locally and on the remote:
       git branch -d applied-fixes
       git push origin --delete applied-fixes

4. Create the docs/ folder and add three files. I will paste the exact
   content for each one IN THE NEXT THREE MESSAGES. For each file:
   - Create it with EXACTLY the content I paste — no edits, no improvements,
     no auto-formatting changes.
   - Do not commit until all three are written.

5. Once all three docs are written, commit on main:
       git add docs/
       git commit -m "docs: add session handoff, roadmap, BRD for ongoing fix sessions

       Adds three durable docs so future Claude Code / Cursor sessions
       can read operating rules, roadmap, and BRD reference without
       re-deriving them from chat history.
       "
       git push origin main

6. Report back with:
   - `git log --oneline -8` (last 8 commits on main)
   - `git ls-tree HEAD --name-only` (top-level entries — confirm docs/
     is present)
   - `git branch -a` (confirm applied-fixes is gone, only main remains)

Reply "Ready" and wait for the three doc contents.
```

---

After Claude Code replies "Ready," paste each of the three blocks below in separate messages, one at a time.

---

## DOC 1 — paste this when asked for SESSION_HANDOFF.md

````
File path: docs/SESSION_HANDOFF.md

Content:

# Session Handoff — SafetyConnect SDK (personal sandbox)

**Purpose of this doc:** every Claude Code / Cursor / Codex session that
opens this repo should read this file FIRST. It sets the standing rules
and the project state.

## Repo purpose

This is a personal scrubbed copy of an Android telematics SDK, used as a
sandbox to develop and validate fixes that are ultimately applied to a
real (company-internal) version. The package paths (`com.test.*`) and
network configuration (`api.example.com`, `Basic dGVzdDp0ZXN0`) are
intentionally generic. Replace before any non-trivial use.

## Branch model

- `main` — the working trunk. Contains the scrubbed SDK plus all
  accepted fix commits. Never modify directly except via merged PRs.
- Feature branches — every new fix or experiment branches off `main`,
  work happens there, and merges back via PR after human review.

## Standing operating rules for any AI session in this repo

1. **ONE bug per commit.** No combining. No "tidying" unrelated code.
   If you find a different problem mid-task, note it and stop — do not
   fix it in this commit.

2. **Do NOT touch these files** (out of scope for any fix work):
   - `safetyconnect/.../sdkinit/AccidentDetector.kt`
   - `safetyconnect/.../repoimpl/SensorInteractImpl.kt`
   - `safetyconnect/.../repoimpl/DataInteractImpl.kt`
   - `safetyconnect/.../network/NetworkModule.kt`
   - Anything under `capturelibrary/`
   Crash detection is a separate vendor-evaluation track. capturelibrary
   is mature image-handling code unrelated to detection. NetworkModule
   has placeholder credentials and shouldn't be modified without auth
   changes being in scope.

3. **Public SDK API stays backward-compatible.**
   - `SafetyConnectCommunicator` interface (in `SafetyConnectSDK.kt`):
     do not change signatures, do not remove methods.
   - `SensorFilters` data class: new fields must have safe defaults.
     Do not rename or remove existing fields.
   - Public methods on `SafetyConnectSDK` companion object: no signature
     changes.

4. **NO unit tests unless explicitly asked.** Field validation against
   recorded sensor traces is the proof for this class of code, not unit
   tests. Adding test files inflates the diff and slows review.

5. **`./gradlew :safetyconnect:assembleDebug` may not run** in your
   environment (no JDK 17 / no Android SDK / corporate proxy blocking
   downloads). If it runs, use it. If it doesn't, skip the build check
   and rely on lint + grep + careful reading. Note it in the commit
   body so reviewers know.

6. **Branch from `main`, never modify main directly.** Create a feature
   branch like `fix/sensor-reorientation`, push to that branch only,
   open a PR for human review before merging back.

7. **Be terse.** Only read files explicitly named in the task. Do not
   re-explore the repo. Do not summarise what you just did unless asked.

8. **If you encounter the company name or any specific brand reference
   in the code or commits — STOP.** This repo is supposed to be fully
   scrubbed. Report the leak immediately; do not commit anything that
   references the real company.

## How to start a new fix session

Use this short starter prompt (saves tokens vs re-deriving everything):

    Starting a new bug-fix session for the SDK in this repo.

    READ FIRST: docs/SESSION_HANDOFF.md and docs/ROADMAP.md.

    TASK: <pick an item from ROADMAP.md "Next up" list, OR describe
    the new BRD requirement to implement>.

    Branch off `main`, create `fix/<short-name>`.

    Constraints from docs/SESSION_HANDOFF.md apply. Be terse.

    Reply "Ready" and wait for go-ahead.
````

---

## DOC 2 — paste this when asked for ROADMAP.md

````
File path: docs/ROADMAP.md

Content:

# Roadmap — SafetyConnect SDK Fix Work

Living document. Update after each fix lands.

## Already done (committed on main)

| Commit | Bug | Expected FP reduction |
|---|---|---|
| `fix(location)` | Bug 1 — GPS coordinate rounding removed | 20–40% |
| `fix(speed)` | Bug 5 — Stationary threshold unified at 2 km/h | 5–10% |
| `fix(harsh)` | Bug 2 — Harsh detector requires accelerometer + GPS agreement; threshold corrected to 3 m/s² | 20–30% |
| `fix(activity-recognition)` | Bug 3 — Modern Activity Recognition + IN_VEHICLE gate | 15–25%, plus resolves transit-suppression NFR |

**Cumulative expected FP reduction (not additive — fixes overlap): 60–80%.**

## Next up (in priority order)

1. **Sensor reorientation to vehicle frame**
   Calibrate phone-to-vehicle rotation matrix from `TYPE_GRAVITY` +
   GPS heading during stable IN_VEHICLE driving. Apply rotation to
   every linear-acceleration sample before the harsh detector reads it.
   Closes the largest remaining false-positive class (phone-in-pocket,
   cupholder, loose-mount events that bleed gravity into horizontal
   axes).
   - Effort: 1–2 weeks
   - Files: new `safetyconnect/.../foreground/calibration/FrameRotator.kt`;
     modify `SafetyConnectService.kt` to register `TYPE_GRAVITY` +
     `TYPE_ROTATION_VECTOR` and route into FrameRotator; modify
     `AccelerometerWindow.kt` to accept rotated samples; modify
     `HarshDrivingDetector.kt` to use longitudinal-axis component.
   - Confidence: High

2. **FusedLocationProviderClient migration**
   Replace legacy `LocationManager.GPS_PROVIDER` with the modern Google
   Play Services FusedLocationProviderClient. Improves accuracy in
   tunnels / urban canyons, smoother speed readings, better battery.
   - Effort: 1 week
   - Files: rewrite `CurrentLocation.kt`; minor adjustments in
     `SafetyConnectService` for the callback shape.
   - Confidence: High

3. **Context-aware speed limits (BRD F2.1)**
   Replace fixed `maxSpeedThreshold` with per-road posted-limit lookup
   via Google Roads API. "Limit unknown" fallback for uncovered roads.
   Requires a Google Maps Platform Mobility API key (caller-supplied).
   - Effort: 3 days engineering + map API access
   - Files: new `safetyconnect/.../foreground/speed/PostedLimitResolver.kt`,
     new `safetyconnect/.../network/RoadsApiService.kt`, new
     `safetyconnect/.../model/SpeedLimitResponse.kt`; modify
     `NetworkModule.kt` to add a Roads API factory; modify
     `SafetyConnectService.handleValidSpeed()` to use the resolver.
     ⚠️ NetworkModule.kt is in the do-not-touch list for general fix
     work — this is one of the rare exceptions. Confirm scope in the
     task prompt before touching it.
   - Confidence: Medium (bounded by India / target-market speed-limit
     data quality, not engineering)

4. **Modern foreground service lifecycle**
   Replace `java.util.Timer` / `TimerTask` with Kotlin coroutines +
   `Flow.sample()`. Replace `CopyOnWriteArrayList` (O(n²) for sensor
   accumulation) with a bounded ring buffer. Battery + reliability win.
   - Effort: 1 week
   - Files: `DataInteractImpl.kt`, `SafetyConnectService.kt`
     ⚠️ DataInteractImpl is in the do-not-touch list — exception applies
     here too. Confirm scope.
   - Confidence: High

5. **Phone-usage detection (BRD F2.3, relaxed scope)**
   Screen-on + interaction events during a detected trip. (Post-
   Android 10 you cannot observe content of OTHER apps without
   intrusive accessibility permissions — spec is relaxed accordingly.)
   - Effort: 3–5 days
   - Files: new `safetyconnect/.../foreground/phoneusage/PhoneUsageMonitor.kt`;
     modify `SafetyConnectService` lifecycle to start/stop with trip.
   - Confidence: Medium-High

## Deferred — do NOT attempt in this repo

- **Automatic crash detection (BRD F5.3b)** — vendor evaluation track.
  Labelled crash data does not exist; in-house attempts will produce
  unacceptable false-alarm / missed-crash tradeoffs.
- **Helmet + certification-mark CV (BRD F1.3)** — multi-month ML
  project, no public dataset for the certification mark, requires a
  dedicated data-collection programme.
- **Multi-occupant seatbelt CV (BRD F1.4)** — essentially infeasible
  from a front phone camera under typical in-cabin lighting.
- **Anything under `capturelibrary/`** — mature image-handling code,
  unrelated to the detection-quality problem.
- **NetworkModule auth migration** — separate security task on its
  own timeline; do not bundle with detection-quality work.

## BRD reference

See [docs/BRD.md](./BRD.md) for the full requirements list and
classification.
````

---

## DOC 3 — paste this when asked for BRD.md

````
File path: docs/BRD.md

Content:

# Business Requirements Reference — Journey Risk Management

This is a generic abstracted reference for the JRM-class telematics
program that this SDK is built for. Use it to prioritise fix work in
ROADMAP.md.

## Context

A large mobile field workforce drives daily as part of their job. Road
incidents are the #1 cause of workplace injuries. The existing in-app
telematics module produces tens of thousands of alert events per day,
the vast majority false positives. Users have learned to ignore the
resulting alerts (alert fatigue). The program's job is to fix this so
the alerting layer regains user trust and the safety pipeline (manager
dashboard, consequence engine, risk scoring) gets clean signal.

## Capability list

Classification:
- **POC** = scope for current fix-work program in this repo
- **Defer** = real, but not in scope for now
- **ML** = requires labelled data / model training / ML specialists
- **Vendor** = better solved by a third-party SDK

### Pre-journey controls
| # | Capability | Class | Notes |
|---|---|---|---|
| F1.1 | Geofenced home-login check before trip start | Defer | Android Geofencing API; trivial later. |
| F1.2 | Pre-trip vehicle-type selection (2W / 4W / public / cab) | POC (minimal) | Drives threshold branching. |
| F1.3 | Helmet detection via CV (presence, certification mark, chin strap, liveness) | ML / Vendor | No public dataset for the certification mark; multi-month ML project. |
| F1.4 | Seatbelt detection via CV (driver + passenger count) | ML / Vendor | Multi-occupant from front camera essentially infeasible. |
| F1.5 | Pre-journey weather + route + blackspot risk gating | Defer | API stitching + rules. |
| F1.6 | Night-driving policy block (e.g., 19:00–06:00) | Defer | Trivial rules. |
| F1.7 | Mandatory micro-learning gate on poor risk profile | Defer | Workflow + content. |
| F1.8 | Vehicle-document compliance tracking | Defer | Standard CRUD. |

### In-journey monitoring (the core fix area)
| # | Capability | Class | Notes |
|---|---|---|---|
| F2.1 | Context-aware speed limits via map API | POC | ROADMAP item 3. |
| F2.2 | Harsh acceleration / braking / cornering, 2W/4W thresholds | DONE | Bug 2 + Bug 5 commits cover braking/acceleration. Cornering uses lateral component once sensor reorientation lands (ROADMAP item 1). |
| F2.3 | Phone-usage detection during driving | POC (relaxed) | ROADMAP item 5. Spec relaxed to "screen-on + interaction events during trip" because post-Android 10 you cannot observe other apps' content without intrusive accessibility permissions. |
| F2.4 | Continuous-driving fatigue timer + mandatory rest | Defer | Trivial timer. |
| F2.5 | In-cabin audio/haptic nudges on unsafe behaviour | Defer | TTS + vibration. Out of scope for this SDK; lives in host app. |
| NFR | Public-transport suppression (no false alerts on metro/bus/train) | DONE | Bug 3 (Activity Recognition + IN_VEHICLE gate). |

### Consequences & governance
| # | Capability | Class | Notes |
|---|---|---|---|
| F3.1 | Rule-based consequence engine | Defer | Backend, not SDK. |
| F3.2 | Evidence / artifact storage | Defer | Backend. |
| F4.1 | Manager dashboard | Defer | Web app. |
| F4.2 | Driver risk score | Defer | Backend; garbage-in if F2.x FPs not fixed first — which is why FP fixes are the priority. |
| F4.3 | Gamification / leaderboards | Defer | UI + cron. |
| F5.1 | Roles & escalation hierarchy | Defer | RBAC. |
| F5.2 | Centralised safety broadcasts | Defer | Push notifications. |
| F5.3a | Manual SOS button + location dispatch | Defer | Trivial; lives in host app. |
| F5.3b | Automatic crash detection | Vendor | Deferred per ROADMAP "Deferred" section. |
| F6.1 | Automated MIS emails | Defer | Cron + templates. |

## Summary

- **Already done in this repo (main):** Bug 1, 5, 2, 3 → ~60–80%
  cumulative false-positive reduction, plus the transit-suppression NFR.
- **POC scope for this repo's ongoing fix work:** F2.1, F2.3, sensor
  reorientation (closes F2.2 cornering), FusedLocation migration,
  modern foreground lifecycle. All listed in ROADMAP.md.
- **Out of scope (vendor / multi-month ML / different system):** F1.3,
  F1.4, F5.3b, all F3/F4/F5 backend or host-app items.
````

---

## After Claude Code reports back

You should see roughly:
- 5 commits on main (initial scrubbed SDK + Bug 1 + Bug 5 + Bug 2 + Bug 3 + docs)
- Top-level: `safetyconnect/`, `app/`, `capturelibrary/`, `docs/`, `README.md`, `LICENSE`, `build.gradle`, `settings.gradle`
- `git branch -a` shows only `main` (`applied-fixes` gone locally + remote)

If any of that's wrong, send the output back to the same session for cleanup.

## Going forward

From this point on, every new fix session uses the short starter prompt embedded in `docs/SESSION_HANDOFF.md`:

```
Starting a new bug-fix session for the SDK in this repo.

READ FIRST: docs/SESSION_HANDOFF.md and docs/ROADMAP.md.

TASK: <pick an item from ROADMAP.md "Next up" list>

Branch off `main`, create `fix/<short-name>`.

Constraints from docs/SESSION_HANDOFF.md apply. Be terse.

Reply "Ready" and wait for go-ahead.
```

That's it — ~8 lines, the repo carries the rest. Setup pays for itself the first time you spin up the next fix.
