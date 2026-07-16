# Current Overspeed Sequence Diagram

> Extracted from `CURRENT_IMPLEMENTATION.md` (§4–§5). Describes the **current**
> behaviour only, as of `main`. No proposals or redesign.

## GPS fix to overspeed callback

```mermaid
sequenceDiagram
    autonumber
    participant OS as Android GPS (LocationManager)
    participant CL as CurrentLocation
    participant SVC as SafetyConnectService
    participant SM as SpeedManager
    participant SDK as SafetyConnectSDK
    participant HOST as Host Communicator

    OS->>CL: onLocationChanged(location) on main thread
    CL->>SVC: getLocation.onLocationChanged(location)
    alt serviceKilled is true
        SVC-->>SVC: hideNotification() then return
    else not killed
        SVC->>SVC: processLocationUpdate(location)
        Note over SVC: Gate skipped because DEBUG_BYPASS_TRIP_GATE is true,<br/>so every location proceeds
        SVC->>SM: processLocation(location)
        Note over SM: reject if accuracy over 50 m or no speed<br/>currentSpeed = location.speed × 3.6 (km/h)<br/>under 2 km/h then Stationary and clear window<br/>jump-validate vs median history (reject over 140 km/h)<br/>collect until 5 readings then median of 5
        SM-->>SVC: SpeedResult
        alt result is Valid (currentSpeed, medianSpeed)
            SVC->>SVC: handleValidSpeed(...)
            alt medianSpeed ≥ maxSpeedThreshold (default 60)
                SVC->>SVC: fireOverSpeedingEvent (sets location.speed = medianSpeed / 3.6)
                Note over SVC: fire only if now minus lastOverSpeedDetected ≥ speedCallBackFrequency (default 30 s)
                SVC->>SDK: notifyAllOverSpeedDetectedListener(location, speedDetectionEdge)
                SDK->>HOST: overSpeedDetected(location, speedDetectionEdge)
            end
            SVC->>SVC: showNotification(emf, "Speed N km/hr")
        else Collecting or Stationary
            SVC->>SVC: showNotification(emf, current speed text)
        else Rejected or null
            SVC-->>SVC: log only
        end
    end
```

## Key facts (as implemented)

- **Speed input:** Android GPS `location.speed` (m/s), converted to km/h via `×18/5` (2 dp). The SDK does not compute the reported speed from coordinates.
- **Smoothing:** median of the last **5** accepted km/h readings (`SpeedManager`).
- **Decision:** `medianSpeed` at or above `maxSpeedThreshold` (default **60 km/h**).
- **Throttle:** `speedCallBackFrequency` (default **30000 ms**); `lastOverSpeedDetected` is reset on every `onStartCommand`.
- **Stationary cutoff:** `stationarySpeedKmh` (default **2 km/h**) produces `Stationary`, which clears `locationHistory` and `speedReadings`.
- **Trip gate:** implemented (`TripGate.isDriving`) but **bypassed** in the current tree (`DEBUG_BYPASS_TRIP_GATE = true`).
- **Network:** none in this path — overspeed is fully on-device.

**Call chain:** `CurrentLocation.onLocationChanged` to `SafetyConnectService.processLocationUpdate` to `SpeedManager.processLocation` to `SafetyConnectService.handleValidSpeed` to `fireOverSpeedingEvent` to `SafetyConnectSDK.notifyAllOverSpeedDetectedListener` to `SafetyConnectCommunicator.overSpeedDetected`.
