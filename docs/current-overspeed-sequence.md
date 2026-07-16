# Current Overspeed Sequence Diagram

> Extracted from `CURRENT_IMPLEMENTATION.md` (§4–§5). Describes the **current**
> behaviour only, as of `main` `c3a21a0`. No proposals or redesign.

## GPS fix → overspeed callback

```mermaid
sequenceDiagram
    autonumber
    participant OS as Android LocationManager<br/>(GPS_PROVIDER)
    participant CL as CurrentLocation
    participant SVC as SafetyConnectService
    participant SM as SpeedManager
    participant SDK as SafetyConnectSDK
    participant HOST as Host<br/>(SafetyConnectCommunicator)

    OS->>CL: onLocationChanged(location)  [main thread; 2 s / 1 m request]
    CL->>SVC: getLocation.onLocationChanged(location)
    alt serviceKilled == true
        SVC-->>SVC: notificationManager.hideNotification(); return
    else
        SVC->>SVC: processLocationUpdate(location)
        Note over SVC: Gate = !DEBUG_BYPASS_TRIP_GATE && gateOnInVehicle && isDriving!=true<br/>DEBUG_BYPASS_TRIP_GATE = true → gate skipped, always proceeds
        SVC->>SM: processLocation(location)
        Note over SM: reject if accuracy > 50 m or !hasSpeed()<br/>currentSpeed = location.speed × 3.6 (km/h, 2 dp)<br/>if < 2 km/h → Stationary (clears history + readings)<br/>jump-validate vs median history (reject > 140 km/h anomalies)<br/>collect until 5 readings, then median of 5
        SM-->>SVC: SpeedResult
        alt Valid(currentSpeed, medianSpeed, location)
            SVC->>SVC: handleValidSpeed(...)
            alt maxSpeedThreshold (default 60) <= medianSpeed
                SVC->>SVC: fireOverSpeedingEvent(location.speed = medianSpeed / 3.6)
                Note over SVC: throttle — fire only if now − lastOverSpeedDetected ≥ speedCallBackFrequency (default 30 s)
                SVC->>SDK: notifyAllOverSpeedDetectedListener(location, speedDetectionEdge)
                SDK->>HOST: overSpeedDetected(location, speedDetectionEdge)
            end
            SVC->>SVC: notificationManager.showNotification(emf, "Speed: N km/hr")
        else Collecting or Stationary
            SVC->>SVC: notificationManager.showNotification(emf, "Speed: …")
        else Rejected or null
            SVC-->>SVC: log only (no callback, no notification change)
        end
    end
```

## Key facts (as implemented)

- **Speed input:** Android GPS `location.speed` (m/s) → `× 18/5` → km/h (2 dp). The SDK does not compute the reported speed from coordinates.
- **Smoothing:** median of the last **5** accepted km/h readings (`SpeedManager`).
- **Decision:** `maxSpeedThreshold` (default **60 km/h**) `<=` `medianSpeed`.
- **Throttle:** `speedCallBackFrequency` (default **30 000 ms**); `lastOverSpeedDetected` is reset on every `onStartCommand`.
- **Stationary cutoff:** `stationarySpeedKmh` (default **2 km/h**) → `Stationary`, which clears `locationHistory` and `speedReadings`.
- **Trip gate:** implemented (`TripGate.isDriving`) but **bypassed** in the current tree (`DEBUG_BYPASS_TRIP_GATE = true`).
- **Network:** none in this path — overspeed is fully on-device.

**Call chain:** `CurrentLocation.onLocationChanged` → `SafetyConnectService.processLocationUpdate` → `SpeedManager.processLocation` → `SafetyConnectService.handleValidSpeed` → `fireOverSpeedingEvent` → `SafetyConnectSDK.notifyAllOverSpeedDetectedListener` → `SafetyConnectCommunicator.overSpeedDetected`.
