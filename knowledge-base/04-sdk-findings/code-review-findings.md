# SafetyConnect SDK — Code Review Findings

Independent review of the telematics SDK. Five root-cause defects plus three
architectural limitations explain the bulk of the false-positive volume.
Package paths shown as `com.test.*` (scrubbed reference namespace).

---

## The five defects

### Bug 1 — GPS coordinates rounded to ~1 km before processing  ⭐ highest leverage
- **File:** `foreground/CurrentLocation.kt` (~line 97) + `foreground/util/LocationExtensions.kt`
- **Problem:** `onLocationChanged()` applied `roundToTwoDecimals()` to lat/lng before any consumer saw it. 2 decimal places ≈ 1.1 km. Every downstream speed/distance calc ran on a ~1 km grid.
- **Effect:** consecutive points collapse (false "stationary") or jump ~1 km (phantom ~1800 km/h → fake events / rejections).
- **Fix:** remove the rounding from the data path; delete `LocationExtensions.kt`.
- **Expected FP reduction:** 20–40%.

### Bug 2 — Harsh detector ignores accelerometer; threshold ~10× too sensitive
- **File:** `foreground/harsh/HarshDrivingDetector.kt`
- **Problem:** used GPS speed only; 3 km/h Δ over 3s ≈ 0.28 m/s² (gentle pedal). Real harsh braking ≥ 3 m/s². The correct sensor (`TYPE_LINEAR_ACCELERATION`, in `sensor/Accelerometer.java`) was wired only to the crash blob.
- **Fix:** add `AccelerometerWindow` ring buffer; require GPS Δspeed AND accel magnitude to agree in a 2s window; raise threshold to ~3 m/s² / 12 km/h; register `TYPE_LINEAR_ACCELERATION`.
- **Expected FP reduction:** 20–30%.

### Bug 3 — Activity Recognition present but dead code
- **File:** `activityrecognition/DetectedActivitiesIntentService.kt`
- **Problem:** deprecated `IntentService` + `LocalBroadcastManager` pattern, never wired into the service. Detection ran while walking / on metro / on bus.
- **Fix:** replace with `ActivityRecognitionClient.requestActivityTransitionUpdates`; gate detection on `IN_VEHICLE ≥ 30s`; graceful fallback if runtime permission denied.
- **Expected FP reduction:** 15–25%, plus resolves the public-transport NFR.

### Bug 4 — Crash detection has no on-device gate (server-side black box)
- **Files:** `sdkinit/AccidentDetector.kt`, `repoimpl/SensorInteractImpl.kt`, `repoimpl/DataInteractImpl.kt`
- **Problem:** raw sensor batches shipped to backend every 15–30s regardless of plausibility; data in device frame (can't tell "phone dropped" from "impact"); user asked to confirm a crash via photo banner.
- **Disposition:** **vendor track.** Do not fix in-house — labelled crash data doesn't exist. Ship manual SOS in v1.

### Bug 5 — Stationary threshold 10 km/h wipes history in traffic
- **File:** `foreground/speed/SpeedManager.kt` (~line 19)
- **Problem:** speed < 10 km/h treated as stationary → clears location history. In stop-and-go traffic this is constant, leaving GPS-jump validation no context. (`HarshDrivingDetector` inconsistently used 3 km/h.)
- **Fix:** unify to a single `sensorFilters.stationarySpeedKmh` (default 2 km/h).
- **Expected FP reduction:** 5–10%.

**Cumulative (not additive — fixes overlap): 60–80%.**

---

## Three architectural limitations

| # | Limitation | Effect | Fix effort |
|---|---|---|---|
| A | No sensor reorientation to vehicle frame | Phone position (pocket/cupholder/loose mount) bleeds gravity into horizontal axes → biggest remaining FP class | 1–2 wks (FrameRotator: gravity + GPS-heading calibration) |
| B | Legacy `LocationManager.GPS_PROVIDER`, 1 m update distance | Worse accuracy in tunnels/canyons, battery drain, no fallback | 1 wk (migrate to FusedLocationProviderClient) |
| C | `CopyOnWriteArrayList` hot-path, `BigDecimal` in conversions, `Timer`/`TimerTask` | Battery drain, GC churn, Doze-unaware | 1 wk (coroutines + ring buffer) |

---

## Security finding (separate track)

- **`network/NetworkModule.kt`** contained a hardcoded HTTP Basic Auth header with a weak default credential, sent to the production gateway on every call. **Rotate, remove from source, move to runtime config.** (In the scrubbed reference repo this is replaced with a `test:test` placeholder + TODO.)

---

## Fundamental telematics limits (no code fully solves)

GPS loss in tunnels/canyons · aggressive OEM background-kill (device-matrix problem) · Doze under thermal load · ground-truth scarcity for harsh-event validation (needs dashcam pilot) · crash-data scarcity · rural posted-limit coverage gaps.

---

## Why prior improvement attempts failed field validation

They tuned thresholds on top of Bug 1 (1 km-quantised coordinates). No downstream logic recovers signal from corrupted input. The fixes are upstream — and must be proven with a replay harness + field drive, not faith.
