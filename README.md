# SafetyConnect SDK (Reference)

An Android telematics SDK that detects vehicle-related safety events on-device — overspeed, harsh braking / acceleration, EMF anomalies — and consumes a backend for crash detection.

> **Status:** reference / sample. URLs, ports, and credentials in this repository are intentionally generic placeholders (`api.example.com`, `127.0.0.1`, `Basic dGVzdDp0ZXN0` = `test:test`). Replace before any non-trivial use.

## Modules

| Module | Purpose |
|---|---|
| `safetyconnect/` | The SDK library. Foreground service, sensor pipeline, detectors, network layer. |
| `capturelibrary/` | Image capture + cropping (used for the safety-equipment / crash-confirmation flow). |
| `app/` | Demo host application showing SDK integration. |

## What the SDK detects

| Detection | Path | Where the work happens |
|---|---|---|
| Overspeed | GPS → `SpeedManager` → threshold check in `SafetyConnectService` → listener callback | On-device |
| Harsh braking / acceleration | GPS → `HarshDrivingDetector` → listener callback | On-device |
| EMF anomaly | Magnetometer → `EmfDetector` → listener callback | On-device |
| Crash | Accelerometer + gyroscope + magnetometer → server-side classifier → listener callback | Server-side (raw samples shipped every 15–30s) |

## How a host app integrates

```kotlin
SafetyConnectSDK.initSDK(
    sensorFilters = SensorFilters(
        isSpeedDetectionEnabled = true,
        maxSpeedThreshold = 60f,
        harshDrivingCaptureEnabled = true,
        // ... other config
        safetyType = SafetyTypes.SPEED_DETECTION
    ),
    activity = this,
    registerForCallBack = object : SafetyConnectCommunicator {
        override fun overSpeedDetected(location: Location?, edge: String?) { /* … */ }
        override fun onHarshDrivingDetected(speed: Float?, edge: String?, eventType: String) { /* … */ }
        override fun onCrashFallDetected(response: SensorResponse, edge: String?) { /* … */ }
        // … other callbacks (see SafetyConnectCommunicator interface)
    }
)
SafetyConnectSDK.startService(this)
```

See `app/src/main/java/com/test/agile/safetyconnect/MainActivity.kt` for the canonical example.

## Build

JDK 17. Android Gradle Plugin 8.x. Standard `./gradlew :safetyconnect:assembleDebug`.

## Replacing the placeholders before real use

| File | What to change |
|---|---|
| `safetyconnect/src/main/java/com/test/safetyconnect/network/NetworkModule.kt` | `BASE_URL` (currently `https://api.example.com/...`) and the `Authorization` header (currently `Basic dGVzdDp0ZXN0`). Move auth to runtime config; do not commit real credentials. |

## License

See [LICENSE](./LICENSE).
