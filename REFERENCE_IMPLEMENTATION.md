# SafetyConnect SDK — Reference Implementation

> **Status:** Canonical technical baseline for this repository.
> **Derived from:** current repository state at `origin/main` HEAD `345fe50`
> (merge of the debug-bypass change). Baseline for "before" comparisons is the
> initial commit `e11224c` ("Initial commit: SafetyConnect SDK reference").
> **Method:** every statement below is derived from files in this repository as
> they currently exist. Where a fact cannot be established from code in this
> repo, it is called out explicitly in **§8 Not Verifiable In This Repository**.
> **Line numbers** are given as of `345fe50` and will drift; symbols
> (class/method/field names) are the stable anchors.

---

## 0. Provenance & Scope

- **This repo is a reference/testing copy of the SDK.** It has been described by
  the maintainers as "cleaned up for the test repo" and may differ from the
  production SDK. It is not the host application.
- **Modules present:**
  - `safetyconnect/` — the SDK library (the subject of this document).
  - `app/` — a demo host activity (`com.test.agile.safetyconnect.MainActivity`)
    used to exercise the SDK.
  - `capturelibrary/` — a separate library dependency (not audited in depth here).
- **Implemented change history on `main`** (`git log`, oldest → newest):
  1. `e11224c` Initial commit: SafetyConnect SDK reference *(baseline)*
  2. `e81ab27` fix(location): stop rounding GPS coordinates before speed/distance calculations
  3. `ffb4dad` fix(speed): unify stationary threshold across detectors, lower default to 2 km/h
  4. `afcab25` fix(harsh): require accelerometer + GPS agreement, correct threshold to 3 m/s²
  5. `087e0a9` fix(activity-recognition): wire modern Activity Recognition + gate detection on IN_VEHICLE
  6. `57979d8`, `7df9e76` docs: knowledge-base / handoff markdown (non-code)
  7. `fd1a7a0` debug: bypass TripGate IN_VEHICLE suppression (DEBUG-only; see §4.5, §6)
  8. `345fe50` Merge PR #1 (brings `fd1a7a0` onto `main`)
- The five behavioural changes (2–5, 7) are documented in **§4**.

---

## 1. Current SDK Architecture Overview

### 1.1 Entry point / facade
- **`SafetyConnectSDK`** (`safetyconnect/src/main/java/SafetyConnectSDK.kt`) — a
  class with a `companion object` holding **static/global** state:
  - `sensorFilters: SensorFilters?` — the single global configuration object.
  - `activity: WeakReference<Activity>?` — a **weak** reference to the host activity.
  - `registerForCallBack: ConcurrentHashMap<SafetyTypes, SafetyConnectCommunicator?>`
    — registered host callbacks keyed by `SafetyTypes`.
  - `safetyConnect`, `detector` — weak references to the crash/image pipeline.
- Public API: `initSDK(...)`, `startService(activity)`, `killService(activity)`,
  `addObserver/removeObserver`, `isGpsPermissionEnabled()`, and a set of
  `notifyAll*` fan-out helpers that invoke host callbacks.
- **`SafetyConnectCommunicator`** (same file) is the host callback interface:
  `onSdkInitialized`, `overSpeedDetected`, `magneticFieldDetected`,
  `onHarshDrivingDetected`, `onCrashFallDetected`, `getFeedbackCallback`,
  `locationPermissionNotGranted`, `notificationPermissionsRequired`,
  `turnOnGpsLocation`. **All are SDK → host.** There is no host → SDK method for
  pushing driving/sensor state in (verified — see §7, §8).

### 1.2 Foreground safety pipeline (audited in depth)
- **`SafetyConnectService`** (`foreground/SafetyConnectService.kt`) — a
  `Service` implementing `SensorEventListener` and `CurrentLocation.GetLocation`.
  Declared in `safetyconnect/src/main/AndroidManifest.xml` with
  `android:foregroundServiceType="location"`. Owns and orchestrates:
  - **`SpeedManager`** (`foreground/speed/SpeedManager.kt`) — rolling-window GPS
    speed validation; emits `SpeedResult` (`Stationary` / `Collecting` / `Valid`
    / `Rejected`).
  - **`EmfDetector`** (`foreground/emf/EmfDetector.kt`) — magnetic-field (EMF)
    processing; beep + host callback + notification text.
  - **`HarshDrivingDetector`** (`foreground/harsh/HarshDrivingDetector.kt`) +
    **`AccelerometerWindow`** (`foreground/harsh/AccelerometerWindow.kt`) —
    harsh accel/brake detection requiring GPS + accelerometer agreement.
  - **`TripGate`** (`foreground/activity/TripGate.kt`) — Activity-Recognition
    driving-state gate (`isDriving: StateFlow<Boolean>`).
  - **`CurrentLocation`** (`foreground/CurrentLocation.kt`) — `LocationManager`
    GPS updates.
  - **`NotificationManager`** (`foreground/notification/NotificationManager.kt`)
    — the foreground notification (custom `RemoteViews`, `R.layout.notification_main`).
  - **`ServiceLifecycleManager`** (`foreground/lifecycle/ServiceLifecycleManager.kt`)
    — `startForeground` / `stopForeground` / `stopSelf` wrappers.
  - **`PermissionValidator`** (`foreground/permission/PermissionValidator.kt`)
    — location / notification / activity-recognition permission checks.
- **`Manager`** (`utils/Manager.java`) — a process-wide singleton
  `ThreadPoolExecutor` (core 5 / max 10, unbounded `LinkedBlockingQueue`) used
  via `runTask { }` for EMF processing and service-action handling.

### 1.3 Crash-detection / sensor-upload pipeline (overview only — not exhaustively audited)
- Started separately by `SafetyConnectSDK.startService(sensorFilters)` (the
  private overload) via **`SafetyConnect`** (`sdkinit/SafetyConnect.kt`) →
  **`AccidentDetector`** (`sdkinit/AccidentDetector.kt`) →
  `repoimpl/SensorInteractImpl` / `DataInteractImpl` → `network/NetworkModule`
  (Retrofit/OkHttp). `DataInteractImpl` runs a `java.util.Timer` that batches
  accelerometer/gyroscope/magnetometer samples and POSTs them; a backend
  response indicates crash detection. This pipeline is **in-process and
  independent of `SafetyConnectService`.**
- **`ImageDetector`** (`sdkinit/ImageDetector.kt`) — safety-equipment image
  detection. Present, not audited here.

### 1.4 Component diagram (foreground safety pipeline)
```
Host App (e.g. AW)
   │  initSDK(SensorFilters, Activity, Communicator)
   │  startService(Activity)
   ▼
SafetyConnectSDK (companion: sensorFilters, activity[Weak], callbacks)
   │  Context.startService(Intent ACTION_START_OR_RESUME_SERVICE)
   ▼
SafetyConnectService (foregroundServiceType=location)
   ├── CurrentLocation ──(GPS_PROVIDER)──► onLocationChanged
   │                                            │
   │                                            ▼
   │                                   processLocationUpdate
   │                                     │  [TripGate gate — see §4.4/§4.5]
   │                                     ▼
   │                                   SpeedManager.processLocation → SpeedResult
   │                                     │
   │                     ┌───────────────┼─────────────────┐
   │             Stationary          Collecting           Valid
   │                                                        │
   │                                        harshDrivingDetector.analyze (if enabled)
   │                                        fireOverSpeedingEvent (if median ≥ threshold)
   │                                                        │
   │                                        notifyAllOverSpeedDetectedListener → host
   ├── SensorManager: TYPE_MAGNETIC_FIELD ──► EmfDetector (Manager thread pool)
   └── SensorManager: TYPE_LINEAR_ACCELERATION ──► AccelerometerWindow → HarshDrivingDetector
```

---

## 2. Event Flow (Location → Processing → Alert)

Traced from `SafetyConnectService` and `CurrentLocation` as they exist now.

1. **Location acquisition** — `startSpeedService()` (called from `onStartCommand`)
   constructs `CurrentLocation(this)` **only if** `sensorFilters.isSpeedDetectionEnabled == true`
   **and** `locationManager == null`. `CurrentLocation.init` calls `location()`,
   which requests updates from **`LocationManager.GPS_PROVIDER`** (2000 ms interval,
   1 m min distance, high accuracy) delivered on the **main executor**. It also
   forwards one `getLastKnownLocation(GPS_PROVIDER)` if present.
2. **Delivery** — `CurrentLocation.onLocationChanged(Location)` forwards the **raw**
   `Location` (see §4.1) to `SafetyConnectService.onLocationChanged`.
3. **Kill guard** — if `serviceKilled`, the notification is hidden and the update
   is dropped.
4. **Trip gate** — `processLocationUpdate()` evaluates:
   ```
   if (!DEBUG_BYPASS_TRIP_GATE &&
       sensorFilters?.gateOnInVehicle == true &&
       tripGate?.isDriving?.value != true) { return }
   ```
   **On current `main`, `DEBUG_BYPASS_TRIP_GATE = true`, so this early return never
   fires** and every location proceeds (see §4.5, §6).
5. **Speed processing** — `SpeedManager.processLocation(location)`:
   - Reject if `accuracy > 50 m` (`MAX_ACCURACY_THRESHOLD`) or `!hasSpeed()`.
   - `Stationary` if `speed < stationarySpeedKmh` (default 2 km/h) → **clears**
     `locationHistory` + `speedReadings`.
   - Otherwise validate against history (`validateLocationJump`, only rejects when
     a computed speed exceeds `MAX_REALISTIC_SPEED = 140`), then `acceptLocation`.
   - `Collecting` until `MAX_LOCATIONS = 5` readings accumulate; then `Valid`
     with `currentSpeed` and `medianSpeed`.
6. **Result handling** (`SafetyConnectService`):
   - `Stationary` → `disableEmfSpeedInKmHr = 0`, notification "Speed: 0 km/hr".
   - `Collecting` → notification "Speed: N km/hr (collecting...)".
   - `Valid` → `handleValidSpeed(...)`:
     - if `harshDrivingCaptureEnabled == true` → `harshDrivingDetector.analyze(location)`.
     - if `maxSpeedThreshold <= medianSpeed` → `fireOverSpeedingEvent(location.apply { speed = medianSpeed / 3.6f })`.
     - updates notification with the converted speed.
7. **Overspeed alert** — `fireOverSpeedingEvent` throttles on
   `speedCallBackFrequency` (default 30 s; reset to "now" on every `onStartCommand`
   via `initializeDetectors()`), then calls
   `notifyAllOverSpeedDetectedListener(location, speedDetectionEdge)` →
   host `overSpeedDetected(...)`.
8. **EMF path (parallel)** — `onSensorChanged(TYPE_MAGNETIC_FIELD)` →
   `coroutineScope.launch` → `Manager.runTask { emfDetector.processSensorEvent(...) }`.
   `processSensorEvent` returns `null` immediately unless
   `isEMFDetectionEnabled == true`. When enabled it may beep (`mediaPlayer.start()`),
   update the notification's EMF line, and call `notifyAllMagneticFieldDetectedListener`
   (throttled by `emfCallBackFrequency`). EMF is additionally suppressed while
   `disableEmfMinimumThresholdInKm < disableEmfSpeedInKmHr`.
9. **Harsh path (parallel)** — `onSensorChanged(TYPE_LINEAR_ACCELERATION)` →
   `AccelerometerWindow.add(...)`. `HarshDrivingDetector` is only invoked from
   `handleValidSpeed` (step 6) and emits via `notifyAllHarshDrivingListener`.

**Alert channels that exist in the SDK:** foreground notification text, an
audible EMF beep (`R.raw.sound` via `MediaPlayer`), and the host callbacks. **There
is no SMS / telephony and no push-messaging code anywhere in the repo** (verified
by grep across `.kt`/`.java`/manifests; no `SmsManager`/`sendTextMessage`/
`SEND_SMS`). Any SMS/push must live in the host app.

---

## 3. Configuration Flow

- **Single source of config:** `SensorFilters` (data class at the bottom of
  `SafetyConnectSDK.kt`). Current defaults (verified):

  | Field | Default | Consumed by |
  |---|---|---|
  | `isEMFDetectionEnabled` | `false` | `EmfDetector.processSensorEvent` (returns null if not true) |
  | `isEMFACDetectionEnabled` | `true` | `EmfDetector` (AC vs DC path) |
  | `maxEmfThreshold` / `maxACEmfThreshold` / `maxAcEmfValue` | `60f` / `60f` / `5000f` | `EmfDetector` |
  | `isSpeedDetectionEnabled` | `false` | `startSpeedService` (creates `CurrentLocation`) |
  | `maxSpeedThreshold` | `60f` | `handleValidSpeed` overspeed comparison |
  | `isFallCrashDetectionEnabled` | `false` | crash pipeline |
  | `networkCallFrequency` / `sensorDataFrequency` | `15000` | crash/sensor upload |
  | `speedCallBackFrequency` | `30_000` | `fireOverSpeedingEvent` throttle |
  | `emfCallBackFrequency` / `crashCallBackFrequency` | `30_000` | EMF / crash throttles |
  | `harshDrivingThresholdInKm` | `12f` | `HarshDrivingDetector` (§4.3) |
  | `harshAccelMps2Threshold` | `3.0f` | `HarshDrivingDetector` accel gate (§4.3) |
  | `harshDrivingDurationFrequency` | `3000L` | harsh window |
  | `harshDrivingCallbackFrequency` | `90_000` | harsh throttle |
  | `harshDrivingCaptureEnabled` | `false` | gates `analyze()` call in `handleValidSpeed` |
  | `disableEmfMinimumThresholdInKm` | `3.0f` | EMF speed suppression |
  | `stationarySpeedKmh` | `2f` | `SpeedManager` + `HarshDrivingDetector` (§4.2) |
  | `gateOnInVehicle` | `true` | trip gate in `processLocationUpdate` (§4.4) |
  | `inVehicleSustainSeconds` | `30` | `TripGate.scheduleDrivingConfirm` (§4.4) |
  | `configuredEmfBeepDistance` / `configuredEmfBeepInterval` | `10.0` / `90_000` | `EmfDetector.shouldPlayEmfSound` |
  | `safetyType` | `null` | callback registration key |

- **Population:** `initSDK(sensorFilters, activity, callback)` →
  `initializeSensorFilter(...)` copies each field from the caller's object into the
  **global** `SafetyConnectSDK.sensorFilters` (a field-by-field copy, not a
  reference swap). `addActiveListener(...)` registers the callback under
  `sensorFilters.safetyType` and invokes `onSdkInitialized()` **once** per
  safetyType (only if not already registered).
- **Runtime mutation of config:** `startTripGate()` sets
  `sensorFilters.gateOnInVehicle = false` **at runtime** if
  `ACTIVITY_RECOGNITION` permission is not granted. This is the only place the SDK
  mutates a config field after init.
- **Demo values:** `app/.../MainActivity.kt` calls `initSDK` with
  `isSpeedDetectionEnabled = true`, `isEMFDetectionEnabled = true`,
  `maxSpeedThreshold = 80f`, etc., then calls `startService`. The demo does **not**
  request `ACTIVITY_RECOGNITION` (that permission is commented out in its
  permission array).

---

## 4. Implemented Changes

Each change is described relative to the initial commit `e11224c` (the baseline
present in this repo). "Original SDK" here means that baseline; the true upstream
production SDK is not in this repository (see §8).

### 4.1 Remove GPS coordinate rounding before speed/distance math
- **Title:** Stop rounding GPS coordinates (`e81ab27`).
- **Problem addressed:** Locations were rounded to 2 decimal places before being
  fed to the speed pipeline, degrading distance/bearing geometry.
- **Technical rationale:** 2-dp latitude/longitude is a ~1.1 km grid; snapping to
  it corrupts `distanceTo`/bearing and can distort `SpeedManager`'s jump checks.
- **Behaviour before:** `CurrentLocation.onLocationChanged` forwarded
  `location.roundToTwoDecimals()`; `foreground/util/LocationExtensions.kt` rounded
  latitude, longitude, altitude, speed, bearing, and accuracy to 2 dp (returning a
  copied `Location`).
- **Behaviour after:** `onLocationChanged` forwards the **raw** `Location`.
  `LocationExtensions.kt` is **deleted** (verified absent).
- **Affected modules/files:** `foreground/CurrentLocation.kt`; deleted
  `foreground/util/LocationExtensions.kt`.
- **Important implementation details:** The removed helper preserved `speed`,
  `hasSpeed()`, and `accuracy` (rounded, not dropped), so the `hasSpeed()` and
  50 m accuracy gates were not the thing being changed — only geometric fidelity.
- **Assumptions/limitations:** None material; the change increases fidelity.
- **Intentional deviations from original:** Removes a rounding utility that existed
  in the baseline.

### 4.2 Unify and lower the stationary-speed threshold
- **Title:** Unify stationary threshold; default 2 km/h (`ffb4dad`).
- **Problem addressed:** Two independent hardcoded stationary thresholds
  (`SpeedManager` = 10 km/h, `HarshDrivingDetector` = 3 km/h) and an over-high
  cutoff.
- **Technical rationale:** A single configurable threshold; 10 km/h treated slow
  driving as stationary and repeatedly cleared the rolling window.
- **Behaviour before:** `SpeedManager` used `const STATIONARY_SPEED_THRESHOLD = 10f`;
  `HarshDrivingDetector` used a private `stationarySpeedThreshold = 3f`.
- **Behaviour after:** Both read `SensorFilters.stationarySpeedKmh` (new field,
  default `2f`). `SpeedManager` STEP 2 and `HarshDrivingDetector.analyze/processHarshDriving`
  use it.
- **Affected modules/files:** `foreground/speed/SpeedManager.kt`,
  `foreground/harsh/HarshDrivingDetector.kt`, `SensorFilters` (new `stationarySpeedKmh`).
- **Important implementation details:** Below-threshold speeds return
  `SpeedResult.Stationary` and **clear** `locationHistory` + `speedReadings`. A
  lower threshold means fewer window resets, so the 5-reading `Valid` state is
  reached more readily.
- **Assumptions/limitations:** `2 km/h` is a default; GPS noise near 2 km/h can
  flip a reading between `Stationary` and `Collecting`.
- **Intentional deviations from original:** Replaces two hardcoded constants with
  one shared config value.

### 4.3 Harsh driving requires accelerometer + GPS agreement
- **Title:** Corroborate harsh events with the accelerometer (`afcab25`).
- **Problem addressed:** Harsh accel/brake events fired on GPS speed-delta alone,
  producing false positives.
- **Technical rationale:** Require GPS speed-delta and accelerometer magnitude to
  agree within a 2 s window before firing.
- **Behaviour before:** `HarshDrivingDetector()` had no accelerometer dependency and
  fired when `abs(speedDiff) >= threshold`. `SafetyConnectService.initSensors`
  registered `Sensor.TYPE_ACCELEROMETER`, but `onSensorChanged` had **no**
  accelerometer branch, so those events were dropped. `harshDrivingThresholdInKm`
  defaulted to `3.0f`.
- **Behaviour after:** `HarshDrivingDetector(accelerometerWindow)` fires only when
  `abs(harshDrivingDiff) >= harshDrivingThresholdInKm` **and**
  `peakAccel >= harshAccelMps2Threshold` **and** `currentSpeed >= stationarySpeedKmh`.
  The service registers `Sensor.TYPE_LINEAR_ACCELERATION` and feeds
  `AccelerometerWindow` via `handleLinearAcceleration`. New field
  `harshAccelMps2Threshold = 3.0f`; `harshDrivingThresholdInKm` default `3.0f → 12f`.
- **Affected modules/files:** `foreground/harsh/HarshDrivingDetector.kt`,
  new `foreground/harsh/AccelerometerWindow.kt`, `foreground/SafetyConnectService.kt`
  (sensor type + `handleLinearAcceleration` + `when` branch), `SensorFilters`.
- **Important implementation details:** `AccelerometerWindow` is a `@Synchronized`
  ring buffer (default 100 samples); `peakMagnitudeWithin(2000L)` returns `0f` when
  empty (cannot throw). Harsh analysis is invoked **only** from `handleValidSpeed`
  and **only** when `harshDrivingCaptureEnabled == true`. This path does not affect
  the overspeed decision.
- **Assumptions/limitations:** Requires a `TYPE_LINEAR_ACCELERATION` sensor; if the
  device lacks one, `getDefaultSensor` returns `null`, `registerListener(null)` is a
  no-op, the window stays empty, `peakAccel = 0`, and harsh events never fire
  (overspeed is unaffected). Harsh detection only runs while the speed pipeline
  reaches `Valid`.
- **Intentional deviations from original:** Switches accelerometer sensor type and
  adds an accelerometer-corroboration requirement plus a new window class.

### 4.4 Modern Activity Recognition + IN_VEHICLE trip gate
- **Title:** Wire Activity Transition API and gate telematics on IN_VEHICLE (`087e0a9`).
- **Problem addressed:** Replace the deprecated IntentService + broadcast Activity
  Recognition, and only run speed/harsh detection while actually driving.
- **Technical rationale:** Suppress speed/harsh processing unless the user is
  IN_VEHICLE, to reduce noise/false positives when not driving.
- **Behaviour before:** `processLocationUpdate` had **no gate** — it went straight
  to `speedManager.processLocation(location)`. The baseline contained
  `activityrecognition/DetectedActivitiesIntentService.kt` and
  `activityrecognition/Constants.kt`, which were **not** wired into
  `processLocationUpdate`. No `gateOnInVehicle` config existed.
- **Behaviour after:** New `TripGate` exposes `isDriving: StateFlow<Boolean>`
  (starts `false`). `processLocationUpdate` early-returns when
  `gateOnInVehicle == true && isDriving != true`. `TripGate` uses
  `ActivityRecognition.getClient(...).requestActivityTransitionUpdates(...)` with a
  broadcast `PendingIntent` and a `RECEIVER_NOT_EXPORTED` receiver on action
  `com.test.safetyconnect.ACTION_ACTIVITY_TRANSITIONS`. `isDriving` becomes `true`
  only after an `IN_VEHICLE` ENTER sustained `inVehicleSustainSeconds` (30 s), and
  `false` on `IN_VEHICLE` EXIT or `WALKING`/`RUNNING`/`ON_BICYCLE` ENTER (`STILL`
  causes no change). New config `gateOnInVehicle = true`, `inVehicleSustainSeconds = 30`.
  `PermissionValidator.hasActivityRecognitionPermission` added.
  `AndroidManifest.xml` now declares `ACTIVITY_RECOGNITION`; the old AR service
  entry is removed. The old `activityrecognition/*` files are **deleted** (verified
  absent).
- **Affected modules/files:** new `foreground/activity/TripGate.kt`;
  `foreground/SafetyConnectService.kt` (gate, `startTripGate`, cleanup `tripGate.stop`);
  `SensorFilters`; `foreground/permission/PermissionValidator.kt`;
  `safetyconnect/src/main/AndroidManifest.xml`; deleted `activityrecognition/*`.
- **Important implementation details:** `startTripGate()` calls `tripGate.start(this)`
  only if `ACTIVITY_RECOGNITION` is granted; otherwise it sets
  `gateOnInVehicle = false` (graceful fallback). `TripGate` maintains its **own**
  Activity-Recognition subscription and its own `_isDriving` state; there is **no
  API for the host to set driving state** (verified — no such setter exists).
- **Assumptions/limitations:** Requires Google Play Services and the
  `ACTIVITY_RECOGNITION` runtime permission. AR transition latency plus the added
  30 s sustain means `isDriving=true` lags trip start. If the permission is granted
  but transitions are not delivered to the receiver, `isDriving` stays `false` and —
  when the gate is active — the speed/harsh pipeline is suppressed.
- **Intentional deviations from original:** Introduces a driving-state gate in front
  of the speed pipeline that did not exist in the baseline, and replaces the old AR
  mechanism.

### 4.5 DEBUG-only: bypass the TripGate suppression
- **Title:** `DEBUG_BYPASS_TRIP_GATE` flag (`fd1a7a0`, merged via PR #1).
- **Problem addressed:** Isolate whether §4.4's gate is what stops the speed
  pipeline, without reverting any fix.
- **Technical rationale:** Neutralize **only** the gate's early return; keep all
  other code and TripGate infrastructure intact; make it trivially reversible.
- **Behaviour before:** `processLocationUpdate`'s guard could early-return when not
  driving.
- **Behaviour after:** A new field `private val DEBUG_BYPASS_TRIP_GATE = true` is
  prepended to the guard as `if (!DEBUG_BYPASS_TRIP_GATE && ...)`. With the flag
  `true`, the `&&` short-circuits and the early return **never** fires, so locations
  flow into `SpeedManager.processLocation` exactly as before §4.4 existed.
- **Affected modules/files:** `foreground/SafetyConnectService.kt` only (one field +
  one guard condition; +10 / −1).
- **Important implementation details:** `TripGate` is still constructed, `start()`ed,
  and tracks `isDriving`; only the consumption of that state is bypassed. **This flag
  is currently `true` on `main`.**
- **Assumptions/limitations:** This is a **debug experiment, not a production fix.**
  While `true`, `gateOnInVehicle` and `isDriving` have no effect on the pipeline.
- **Intentional deviations from original:** Intentionally disables §4.4's gate at
  runtime while leaving its code present.

---

## 5. Known Limitations (code-level, verifiable in this repo)

These are established directly from the current code. They are structural/code
facts; observed **runtime** symptoms are separated into §8.

1. **Host activity held weakly.** `SafetyConnectSDK.activity` is a
   `WeakReference<Activity>`. `SafetyConnectService.onCreate` calls `stopSelf()` if
   `activity?.get() == null`; several service paths depend on `activity?.get()`.
   A recreated service with a collected activity reference cannot start.
2. **Service started via `startService`, not `startForegroundService`.**
   `SafetyConnectSDK.startService(activity)` calls `activity.startService(intent)`.
3. **GPS-only location, single subscription.** `CurrentLocation` uses
   `LocationManager.GPS_PROVIDER` (not Fused), requests updates once, and never
   re-subscribes; `onProviderDisabled` only fires `notifyAllTurnOnGpsLocationListener()`.
4. **No re-subscription after pause within an instance.** `cleanup()`/`pauseService()`
   do not null `locationManager`; `startSpeedService()` only creates it when
   `locationManager == null`, so after `ACTION_PAUSE_SERVICE` the same instance does
   not re-request updates.
5. **Notification cancel path.** `NotificationManager.showNotification("","")`
   posts then `cancel(NOTIFICATION_ID)`; it is called from `runServiceForFirstTime`.
   With `isEMFDetectionEnabled == false`, `EmfDetector.processSensorEvent` returns
   `null`, so the EMF path does not re-post the notification — notification refresh
   then depends on the speed handlers.
6. **Swallowed exceptions.** `Manager` (`ThreadPoolExecutor`) has no
   `afterExecute` handler, so EMF-path exceptions are swallowed;
   `ServiceLifecycleManager.startForeground` and `NotificationManager.showNotification`
   catch and only log.
7. **Non-volatile cross-thread state.** `isFirstRun` / `serviceKilled` are written on
   the `Manager` pool thread and read on the main thread without `@Volatile`.
8. **`Valid` requires 5 accepted readings;** any `Stationary` dip clears the window.
9. **`START_STICKY` restart delivers a null intent;** `handleServiceAction(null)`
   matches no branch (no sensor/location re-registration on that path).
10. **EMF disabled by default** (`isEMFDetectionEnabled = false`) and additionally
    suppressed while moving faster than `disableEmfMinimumThresholdInKm`.
11. **No SMS / telephony / push-messaging code** exists anywhere in the repository.

---

## 6. Debug-only Changes

- **`DEBUG_BYPASS_TRIP_GATE`** in `SafetyConnectService` (see §4.5). **Currently
  `true` on `main`**, which means the IN_VEHICLE trip gate is **inert** in this
  repository's current state: the speed/harsh pipeline runs regardless of
  `gateOnInVehicle` / `TripGate.isDriving`.
- **To restore gated behaviour:** set `DEBUG_BYPASS_TRIP_GATE = false`, or delete
  the field and the `!DEBUG_BYPASS_TRIP_GATE &&` line in `processLocationUpdate`.
- This change was intentionally merged to keep this reference/testing repo in sync
  with a debug change applied to the production repo. It is not a production fix.

---

## 7. Future Extension Points (seams that exist in the current code)

1. **Host-driven driving state.** `TripGate._isDriving` is private and set only from
   its own AR transitions; there is currently **no** host → SDK setter. A
   `SafetyConnectSDK.setDrivingState(Boolean)` (feeding `TripGate`) would let a host
   that already detects driving drive the gate. *(Seam: the gate consumes
   `tripGate?.isDriving?.value` in one place — `processLocationUpdate`.)*
2. **Location provider.** `CurrentLocation` is a single class encapsulating
   `LocationManager`/`GPS_PROVIDER`; it is the seam for swapping in
   `FusedLocationProviderClient`.
3. **Gate tuning.** `gateOnInVehicle` and `inVehicleSustainSeconds` are config
   fields, already runtime-tunable.
4. **New alert types.** `SafetyConnectCommunicator` + the `notifyAll*` fan-out is the
   established surface for adding host-facing alerts.
5. **Feature toggles.** EMF, speed, crash, harsh, and image detection each gate on
   their own `SensorFilters` flags, so features can be enabled independently.

---

## 8. Not Verifiable In This Repository (explicitly stated)

The following are **outside** this repo and were **not** confirmed from code here;
they must not be treated as established by this document:

1. **The true upstream / production SDK.** Only the initial commit `e11224c` exists
   here as a baseline. "Deviations from the original SDK" in §4 are measured against
   that baseline, not against the production source.
2. **The host application (e.g. "AW").** Its behaviour, the actual `SensorFilters`
   values it passes (e.g. whether it enables EMF), how it implements the callbacks,
   and whatever drives its "vehicle icon" are not in this repository.
3. **Runtime symptoms.** Reports such as "speed stays N/A," "no overspeed callbacks,"
   or "notification disappears after ~1 minute" are runtime observations. This repo
   contains **no logs, traces, or non-template tests** (only the stock
   `ExampleUnitTest`/`ExampleInstrumentedTest`), so those symptoms cannot be proven
   from code here — only reasoned about via the paths in §2/§5.
4. **Whether `DEBUG_BYPASS_TRIP_GATE` should remain enabled in production.** The code
   marks it debug-only; the intended production value is a decision, not a code fact.
5. **`capturelibrary` internals** and the full crash/network/image pipelines
   (`sdkinit`, `repoimpl`, `network`) were not audited line-by-line for this document;
   §1.3 is an overview.

---

*Maintenance note: when code changes, update the affected §4 entry and re-verify §3
defaults and §5 limitations against source. Treat symbol names as the anchors;
re-derive line numbers as needed.*
