# Cursor Prompts — Personal Repo Bug-Fix Sweep (clean re-implementation)

**Repo:** `<your-personal-repo>` (the personal scrubbed copy of the SafetyConnect SDK).
**Branch to work on:** `applied-fixes` (create from `main` BEFORE starting any session).
**Source paths:** `com.test.*` (the scrubbed package, NOT `com.<company>.*`).

**Why we're doing this:** these 4 bug fixes have already been shipped at company, but those commits are company IP and cannot be cherry-picked onto a personal GitHub repo. We're re-implementing the same fixes against the scrubbed source so the personal repo has a clean, independently-developed working baseline for ongoing experimentation.

**Usage:** paste each numbered block into a FRESH Cursor session (Sonnet 4.5, agent mode), one at a time, in order. Wait for Cursor to commit and push each before sending the next. **Do not paste them all at once** — one bug = one commit.

**Model:** Sonnet 4.5 for all five. Haiku is too light for Bugs 2 + 3; Opus is overkill for any of them.

---

## Prompt 1 — Orientation + Bug 1 combined (one session)

Send this whole block in one go.

```
You are working on the SafetyConnect Android telematics SDK in this repository.
This is the FIRST of four bug-fix sessions on the `applied-fixes` branch.

REPO CONTEXT:
- Branch: `applied-fixes` (already exists, branched from `main`).
  All commits in this session and the next three go here.
- Package paths use `com.test.*` (this is a scrubbed reference SDK; the
  company production code uses different package names — irrelevant here).
- The base SDK on `main` is the unfixed baseline. Your fixes accumulate on
  `applied-fixes`. Never modify `main` directly.

OPERATING RULES (apply to this and every subsequent session):
1. ONE bug per commit. No combining. No "tidying" unrelated code.
2. Do NOT touch: AccidentDetector.kt, SensorInteractImpl.kt,
   DataInteractImpl.kt, NetworkModule.kt, anything under capturelibrary/.
3. Public SDK API stays backward-compatible. SafetyConnectSDK.kt's listener
   interface (SafetyConnectCommunicator) and existing sensorFilters fields
   must not change signature. New sensorFilters fields must have safe defaults.
4. NO unit tests. Field validation is the proof, not tests.
5. After each commit run `./gradlew :safetyconnect:assembleDebug` IF the
   environment supports it. If it doesn't (no JDK 17 / no Android SDK /
   network blocked), skip and note that in the commit body — rely on lint
   + grep + reading.
6. Commit messages: `fix(<area>): <one-line summary>` + body explaining what
   changed and why.

Before doing anything, read these files and confirm you understand the
structure:
- safetyconnect/src/main/java/SafetyConnectSDK.kt
- safetyconnect/src/main/java/com/test/safetyconnect/foreground/SafetyConnectService.kt
- safetyconnect/src/main/java/com/test/safetyconnect/foreground/CurrentLocation.kt
- safetyconnect/src/main/java/com/test/safetyconnect/foreground/util/LocationExtensions.kt
- safetyconnect/src/main/java/com/test/safetyconnect/foreground/speed/SpeedManager.kt
- safetyconnect/src/main/java/com/test/safetyconnect/foreground/harsh/HarshDrivingDetector.kt
- safetyconnect/src/main/java/com/test/safetyconnect/activityrecognition/DetectedActivitiesIntentService.kt

Reply "Ready" and wait for the bug instructions. Then I'll send Bug 1.
```

After it replies "Ready," paste this:

```
BUG 1 — GPS coordinates are being rounded to 2 decimal places (~1.1 km
resolution at the equator) BEFORE the speed and distance calculations consume
them. This is the single highest-leverage defect in the SDK. Fix: stop
rounding raw GPS coordinates anywhere in the data path.

EVIDENCE:
- CurrentLocation.kt onLocationChanged() calls `location.roundToTwoDecimals()`
  before passing the Location to consumers (around line 97).
- LocationExtensions.kt defines `roundToTwoDecimals()` and
  `roundToTwoDecimalPlaces()`. The docstring claims it "improves data
  consistency" — it does the opposite.

WHAT TO DO:
1. In CurrentLocation.kt onLocationChanged(), pass the raw Location through:
       getLocation.onLocationChanged(location)
   (Remove the .roundToTwoDecimals() call AND remove the unused import for it.)

2. Search the entire safetyconnect/ module for ALL other call sites of
   roundToTwoDecimals() or roundToTwoDecimalPlaces():
       grep -rn "roundToTwoDecimals\|roundToTwoDecimalPlaces" safetyconnect/ app/
   Confirm zero remaining references after step 1.

3. Delete LocationExtensions.kt entirely (it is now fully unused).

4. If assembleDebug runs in this environment, verify BUILD SUCCESSFUL.
   If it doesn't, skip and note it.

5. Commit message:
       fix(location): stop rounding GPS coordinates before speed/distance calculations

       roundToTwoDecimals() quantised lat/lng to ~1.1 km grid before
       SpeedManager and HarshDrivingDetector consumed them, corrupting every
       downstream distance/speed calculation. Rounding now eliminated.
       LocationExtensions.kt deleted (no longer referenced).

Push to `applied-fixes`. Report when done.
```

---

## Prompt 2 — Bug 5 (FRESH SESSION, Sonnet 4.5)

```
Continuing a multi-bug fix branch on the SafetyConnect SDK in this repo.

ALREADY DONE on `applied-fixes` (do NOT re-verify, do NOT re-read these files):
- Bug 1 (commit fix(location)): removed roundToTwoDecimals() from
  CurrentLocation.kt data path; deleted LocationExtensions.kt.

OPERATING RULES (unchanged from session 1, restated for clarity):
- ONE bug per commit. No combining. No "tidying" unrelated code.
- Do NOT touch: AccidentDetector.kt, SensorInteractImpl.kt,
  DataInteractImpl.kt, NetworkModule.kt, capturelibrary/.
- Public SDK API stays backward-compatible. New sensorFilters fields must
  have safe defaults; existing field types/names unchanged.
- NO unit tests.
- Be terse. Only read files I name. Do NOT re-explore the repo.

BUG 5 — The "stationary" speed threshold is set inconsistently across modules
and is set too high in SpeedManager. Fix: unify to ~2 km/h in a single shared
config.

EVIDENCE:
- SpeedManager.kt line ~19: `private const val STATIONARY_SPEED_THRESHOLD = 10f`
  → anything under 10 km/h wipes locationHistory + speedReadings. In dense
    stop-and-go traffic this is most of the time, leaving GPS-jump validation
    no context.
- HarshDrivingDetector.kt line ~18: `private val stationarySpeedThreshold = 3f`
  → different value for the same concept. Inconsistency is itself a smell.

WHAT TO DO:
1. Add a shared field to SafetyConnectSDK's `SensorFilters` data class:
       val stationarySpeedKmh: Float = 2f
   (New field with default — backward-compatible.)

2. In SpeedManager.kt:
   - Remove the local `STATIONARY_SPEED_THRESHOLD` constant.
   - Read the value from `SafetyConnectSDK.sensorFilters?.stationarySpeedKmh ?: 2f`
     at the comparison sites.

3. In HarshDrivingDetector.kt:
   - Remove the local `stationarySpeedThreshold` field.
   - Read the value from `SafetyConnectSDK.sensorFilters?.stationarySpeedKmh ?: 2f`
     at the comparison sites.

4. If assembleDebug runs here, BUILD SUCCESSFUL. Otherwise skip + note.

5. Commit message:
       fix(speed): unify stationary threshold across detectors, lower default to 2 km/h

       SpeedManager used 10 km/h and HarshDrivingDetector used 3 km/h for
       the same "is the vehicle moving" concept. In stop-and-go traffic the
       10 km/h value was wiping location history constantly, leaving GPS-jump
       validation no context. Both now read from
       sensorFilters.stationarySpeedKmh (default 2 km/h). Existing callers
       get the safer default automatically (backward-compatible).

Push to `applied-fixes`. Report when done.
```

---

## Prompt 3 — Bug 2 (FRESH SESSION, Sonnet 4.5)

```
Continuing a multi-bug fix branch on the SafetyConnect SDK in this repo.

ALREADY DONE on `applied-fixes`:
- Bug 1 (fix(location)): removed roundToTwoDecimals() from data path;
  deleted LocationExtensions.kt.
- Bug 5 (fix(speed)): added sensorFilters.stationarySpeedKmh (default 2f);
  SpeedManager and HarshDrivingDetector now read it.

Same operating rules as prior sessions. Be terse. Only read files I name.

BUG 2 — HarshDrivingDetector currently uses GPS speed only with a threshold
~10x too sensitive (3 km/h Δ over 3s ≈ 0.28 m/s², which is gentle pedal
pressure, not harsh braking; real harsh ≥ 3 m/s²). The Accelerometer class
already exists in the codebase — wire it in and require both signals to
agree before firing.

EVIDENCE:
- HarshDrivingDetector.kt analyze(location: Location?) — takes a Location,
  not a sensor event. Computes harshDrivingDiff from GPS speed averages.
  Threshold defaults to 3 km/h in sensorFilters.harshDrivingThresholdInKm.
- safetyconnect/src/main/java/com/test/safetyconnect/sensor/Accelerometer.java
  uses Sensor.TYPE_LINEAR_ACCELERATION (correct), exposes a Listener with
  onTranslation(ax, ay, az). Currently only wired to SensorInteractImpl
  (the crash-detection blob — out of scope).
- SafetyConnectService.kt registers TYPE_ACCELEROMETER in registerSensors()
  but does nothing with it (only routes magnetic events).

WHAT TO DO:
1. Add a new class:
   safetyconnect/src/main/java/com/test/safetyconnect/foreground/harsh/AccelerometerWindow.kt
   - Ring buffer of recent linear-acceleration samples (timestamp + ax + ay + az).
   - Method: fun peakMagnitudeWithin(windowMs: Long): Float
     returns the maximum sqrt(ax^2 + ay^2 + az^2) within the last windowMs.
   - Bounded size (e.g., last 2 seconds at game rate ≈ 100 samples).
   - Single-producer (sensor callback); synchronized add is fine.

2. Modify HarshDrivingDetector.kt:
   - Accept an AccelerometerWindow in the constructor (or setter).
   - Add a new sensorFilters field: harshAccelMps2Threshold: Float = 3.0f
     (new field with default — backward-compatible).
   - Fire ONLY if ALL true within the same 2s window:
       |harshDrivingDiff_kmh| >= sensorFilters.harshDrivingThresholdInKm
       AND accelerometerWindow.peakMagnitudeWithin(2000L) >=
           sensorFilters.harshAccelMps2Threshold
       AND currentSpeed >= sensorFilters.stationarySpeedKmh  (Bug 5 field)
   - The existing callbackFrequency debounce stays as-is.

3. Modify SafetyConnectService.kt:
   - In initializeManagers(), instantiate AccelerometerWindow and pass it
     to the new HarshDrivingDetector constructor.
   - In onSensorChanged / handleMagneticFieldSensor area, add a handler for
     Sensor.TYPE_LINEAR_ACCELERATION that appends to AccelerometerWindow.
   - CRITICAL: in registerSensors(), change the registration from
     Sensor.TYPE_ACCELEROMETER to Sensor.TYPE_LINEAR_ACCELERATION (gravity-
     removed). This is necessary for the threshold math to be meaningful.

4. Bump sensorFilters defaults in SafetyConnectSDK.kt:
   - harshDrivingThresholdInKm: change from 3f to 12f (12 km/h Δ over 3s).
   - harshAccelMps2Threshold: 3.0f (new field).
   - Existing callers inherit the safer defaults.

5. Do NOT touch AccidentDetector.kt, SensorInteractImpl.kt, or
   DataInteractImpl.kt. The crash-detection blob is separate.

6. If assembleDebug runs here, BUILD SUCCESSFUL. Otherwise skip + note.

7. Commit message:
       fix(harsh): require accelerometer + GPS agreement, correct threshold to 3 m/s²

       HarshDrivingDetector used GPS speed alone with a 3 km/h-over-3s
       threshold (~0.28 m/s²), ~10x too sensitive for actual harsh braking
       (>3 m/s²). Existing Accelerometer class (TYPE_LINEAR_ACCELERATION)
       is now wired via new AccelerometerWindow ring buffer. Detector fires
       only when GPS speed-delta AND accelerometer magnitude both exceed
       thresholds in the same 2s window. Defaults raised:
       harshDrivingThresholdInKm 3→12, harshAccelMps2Threshold=3.0 (new).
       Foreground service now registers TYPE_LINEAR_ACCELERATION instead
       of raw TYPE_ACCELEROMETER.

Push to `applied-fixes`. Report when done.
```

---

## Prompt 4 — Bug 3 (FRESH SESSION, Sonnet 4.5)

```
Continuing a multi-bug fix branch on the SafetyConnect SDK in this repo.

ALREADY DONE on `applied-fixes`:
- Bug 1 (fix(location)): GPS rounding removed; LocationExtensions.kt deleted.
- Bug 5 (fix(speed)): sensorFilters.stationarySpeedKmh added (default 2f);
  SpeedManager and HarshDrivingDetector read it.
- Bug 2 (fix(harsh)): AccelerometerWindow added; HarshDrivingDetector
  requires GPS+accel agreement; thresholds raised to 12 km/h Δ + 3 m/s²;
  foreground service registers TYPE_LINEAR_ACCELERATION.

Same operating rules. Be terse. Only read files I name.

BUG 3 — The Activity Recognition file exists but uses the deprecated 2015-era
IntentService + LocalBroadcastManager pattern, AND is never invoked from
SafetyConnectService. As a result, telematics runs while the user is walking,
on a bus, on a metro. This is the entire cause of the public-transport
false-positive problem.

EVIDENCE:
- safetyconnect/src/main/java/com/test/safetyconnect/activityrecognition/DetectedActivitiesIntentService.kt
  uses deprecated IntentService + LocalBroadcastManager.
- No code in safetyconnect/src/main/ ever references it.
- SafetyConnectService.kt runs the speed/harsh pipelines regardless of
  whether the user is driving.
- safetyconnect/build.gradle already depends on
  `com.google.android.gms:play-services-location:21.3.0` (around line 74) —
  which is where the modern ActivityRecognitionClient lives. Do NOT add a
  new dependency.

WHAT TO DO:
1. Delete DetectedActivitiesIntentService.kt and activityrecognition/Constants.kt
   if Constants only existed for the deleted service.

2. Create a new class:
   safetyconnect/src/main/java/com/test/safetyconnect/foreground/activity/TripGate.kt
   - Uses ActivityRecognitionClient.requestActivityTransitionUpdates() to
     subscribe to ENTER/EXIT transitions for:
       DetectedActivity.IN_VEHICLE
       DetectedActivity.WALKING
       DetectedActivity.RUNNING
       DetectedActivity.ON_BICYCLE
       DetectedActivity.STILL
   - Exposes `isDriving: StateFlow<Boolean>`:
       - becomes true 30 seconds after IN_VEHICLE ENTER (sustained)
       - becomes false immediately on IN_VEHICLE EXIT
       - becomes false on WALKING / RUNNING / ON_BICYCLE ENTER
   - start(context) / stop(context) lifecycle methods.
   - Use a PendingIntent + small BroadcastReceiver (NOT IntentService) for
     the callback. Register receiver programmatically.

3. Add to sensorFilters in SafetyConnectSDK.kt:
       val gateOnInVehicle: Boolean = true
       val inVehicleSustainSeconds: Int = 30
   (New fields with safe defaults.)

4. Modify SafetyConnectService.kt:
   - In initializeManagers(), instantiate TripGate and call
     tripGate.start(this).
   - In processLocationUpdate(), short-circuit BEFORE calling
     speedManager.processLocation when:
         sensorFilters.gateOnInVehicle && !tripGate.isDriving.value
     Log the suppression once per state change (do not spam).
   - In onDestroy / killService / cleanup, call tripGate.stop(this).

5. Manifest (safetyconnect/src/main/AndroidManifest.xml):
   Add ONLY these permissions:
       <uses-permission android:name="com.google.android.gms.permission.ACTIVITY_RECOGNITION" />
       <uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
   Do NOT add FOREGROUND_SERVICE_LOCATION or any other permissions.

6. Modify PermissionValidator.kt (or wherever permission checks live):
   - Add hasActivityRecognitionPermission(context): Boolean check for API 29+.
   - If denied: log a warning AND set gateOnInVehicle=false dynamically
     (graceful fallback — preserves telematics for users without the permission).

7. If assembleDebug runs here, BUILD SUCCESSFUL. Otherwise skip + note.

8. Commit message:
       fix(activity-recognition): wire modern Activity Recognition + gate detection on IN_VEHICLE

       DetectedActivitiesIntentService used the deprecated IntentService +
       LocalBroadcastManager pattern and was never invoked. Telematics ran
       while walking / on metro / on bus, which is the entire cause of the
       public-transport false-positive problem.

       Replaced with TripGate using ActivityRecognitionClient.requestActivity
       TransitionUpdates. SafetyConnectService gates speed + harsh detection
       on isDriving (IN_VEHICLE sustained ≥ 30s). Gate is configurable via
       sensorFilters.gateOnInVehicle (default true) and inVehicleSustainSeconds
       (default 30). ACTIVITY_RECOGNITION runtime permission handling added;
       if denied, gate auto-disables to preserve current behaviour for
       existing users without the permission.

Push to `applied-fixes`. Report when done.
```

---

## Prompt 5 — Docs (FRESH SESSION, Haiku 4.5 is fine)

```
Final session on this repo's initial setup. All 4 bug fixes are now on
`applied-fixes`. Your task is to add three documentation files so future
fix sessions have a durable handoff and don't pay tokens re-deriving
operating rules.

Switch to `main` branch. Create these three files under a new `docs/` folder:

1. docs/SESSION_HANDOFF.md
2. docs/ROADMAP.md
3. docs/BRD.md

I will paste content for each one in separate messages. Wait for each.

For each file:
- Create the file with EXACTLY the content I paste (no edits, no improvements).
- Do not add anything outside what I paste.

After all three are created:
- Commit on `main` with message:
    docs: add handoff, roadmap, BRD reference for ongoing fix sessions
- Push main.
- Then `git checkout applied-fixes && git merge main --no-ff -m "docs: bring handoff docs onto applied-fixes"` and push applied-fixes.
- Both branches now have the docs.

Report when done. Reply "Ready" and wait for the SESSION_HANDOFF.md content.
```

Then paste the three doc contents from earlier (the SESSION_HANDOFF.md, ROADMAP.md, BRD.md drafts) one at a time after Cursor confirms each file is created.

---

## Before you start — one git operation (no Cursor needed)

Create the `applied-fixes` branch. Easiest way: GitHub web UI → branch dropdown on the repo page → type `applied-fixes` → "Create branch: applied-fixes from main". Takes 10 seconds. Done.

OR locally:
```bash
git clone <your-personal-repo-url>
cd Testing
git checkout -b applied-fixes
git push -u origin applied-fixes
```

Once `applied-fixes` exists on GitHub, fire up Cursor Session 1 and paste Prompt 1.

---

## Expected costs (rough)

| Session | Model | Approx cost |
|---|---|---|
| 1 (orientation + Bug 1) | Sonnet 4.5 | $2–4 |
| 2 (Bug 5) | Sonnet 4.5 | $1–2 |
| 3 (Bug 2) | Sonnet 4.5 | $3–5 |
| 4 (Bug 3) | Sonnet 4.5 | $3–5 |
| 5 (docs) | Haiku 4.5 | <$1 |
| **Total** | | **~$10–17** |

If any single session climbs past these ranges, stop it and investigate — usually means the agent is re-exploring the repo or trying to fix something out of scope.
