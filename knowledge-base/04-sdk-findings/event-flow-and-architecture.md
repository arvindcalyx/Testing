# SafetyConnect SDK — Event Flow & Architecture

How detections travel from sensor to consumer. Package paths scrubbed (`com.test.*`).

---

## The SDK is a library inside a host app, not a standalone service

The SDK compiles into the host app and runs in its process. "The SDK detects
something" = code inside the host app's process detects it. The SDK **does not
show user alerts** — it fires callbacks to listeners the host app registered.

---

## Who defines vs who enforces (overspeed example)

- **Host app DEFINES the threshold** — passes `SensorFilters(maxSpeedThreshold = X)` at `initSDK`.
- **SDK ENFORCES the comparison** — on each GPS reading: `if (medianSpeed >= maxSpeedThreshold) fire`.

The SDK is a dumb comparator. It has no concept of "which road am I on" — hence
the fixed-limit problem that F2.1 (context-aware speed limits) exists to fix.

---

## How speed is calculated

1. Android GPS chip provides `location.speed` (m/s, Doppler-based) — **not** computed by the SDK.
2. SDK converts m/s → km/h (× 3.6).
3. `SpeedManager` takes a rolling **median of the last 5 readings** to smooth noise.
4. Sanity-rejects bad readings (accuracy > 50 m, speed > 140 km/h, implausible jump via distance÷time + bearing check).
5. Manual distance÷time is used **only as a validation cross-check**, not as the reported speed.

(Bug 1 corrupted step 4's cross-check by rounding coordinates first.)

---

## The public interface

**One interface:** `SafetyConnectCommunicator` (9 callbacks). Host app implements all 9.

`notifyAll*` methods on `SafetyConnectSDK` (8 total) each iterate the registered
listeners and invoke the matching callback:

| Fired by | Method | Callback |
|---|---|---|
| Overspeed | `notifyAllOverSpeedDetectedListener` | `overSpeedDetected(location, edge)` |
| Harsh brake/accel | `notifyAllHarshDrivingListener` | `onHarshDrivingDetected(speed, edge, eventType)` |
| Crash | `notifyAllOnCrashFallDetectedListener` | `onCrashFallDetected(response, edge)` |
| EMF | `notifyAllMagneticFieldDetectedListener` | `magneticFieldDetected(sqrt, edge)` |
| Permission/GPS states | 4 more | (permission/GPS callbacks) |

**Registry design flaw:** keyed by a 3-value enum (`SafetyTypes`), so **max 3 listeners**, and registering twice under one key **silently overwrites**. Flag for v2.

---

## Buffering vs callback

- Overspeed / harsh: **purely callback-based, zero persistence.** If the host app doesn't handle the callback, the event is gone. No offline queue, no replay, no dedup.
- Crash: raw sensor samples buffered in-memory and flushed to backend every 15–30s **even with no listener registered** — and can show the photo banner regardless of listener state.

---

## Which downstream systems improve when FPs drop

Everything downstream of the callback improves automatically (~60–80% less volume):
dashboard, IVR alerts, MIS reports, risk scoring, consequence engine.

**Caveats:** risk-score weights and consequence-rule thresholds were calibrated
against FP-inflated volume — they need **recalibration** post-rollout or they'll
under-trigger. And the host app's own logic between callback and user (e.g. IVR
cadence) may still need relaxing — the SDK fix is necessary, not sufficient.

**Not improved:** crash detection (out of scope), helmet/seatbelt CV (different
subsystem), speed accuracy on roads with no map data (external dependency).
