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
