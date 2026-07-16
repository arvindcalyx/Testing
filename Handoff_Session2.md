# Handoff — Session 2

**Repo:** `arvindcalyx/Testing` — a cleaned-up reference/test copy of the SafetyConnect SDK. The production ("AW") repo may differ.
**Working branch:** `claude/speed-detection-timeout-issue-abg3s4`
**`main` HEAD at handoff:** `7461093`
**Date:** 2026-07-16

---

## TL;DR for Session 3
The speed/overspeed regression was root-caused to the **TripGate IN_VEHICLE gate** (added in fix commit `087e0a9`). A **debug flag `DEBUG_BYPASS_TRIP_GATE = true` is now on `main`** — it makes the speed pipeline work again but is a **DEBUG experiment, not the production fix**. The real fix decision is still open. Baseline docs and a road-speed RFC are merged to `main`.

## What Session 2 did
1. **Root-caused the regression.** `SafetyConnectService.processLocationUpdate` gates the speed pipeline on `gateOnInVehicle && TripGate.isDriving`. `gateOnInVehicle` defaults `true`; `isDriving` only becomes true after a 30 s-sustained IN_VEHICLE Activity-Recognition transition → pipeline suppressed → "speed N/A" + no overspeed. The regression is fix `087e0a9`; the other three fixes (`e81ab27`, `ffb4dad`, `afcab25`) are **not** on the overspeed path.
2. **Added the debug bypass.** `DEBUG_BYPASS_TRIP_GATE` neutralizes only the gate's early `return`, leaving TripGate otherwise intact. Merged via **PR #1**. User confirmed SpeedManager resumed producing speeds after the bypass.
3. **Established SDK↔host independence.** TripGate runs its **own** Activity-Recognition subscription + `isDriving` StateFlow; there is **no API for the host (AW) to feed driving state into the SDK**. AW's "vehicle icon" is a separate flow and cannot influence `TripGate.isDriving`.
4. **Verified Google Roads API behaviour** (for the road-speed feature): `placeId` is the road-segment identity; a single Speed-Limits call with a `path` returns snapped points **and** the limit; ≤100 points/request; units KPH; the Speed-Limits endpoint requires a Google Maps **Asset Tracking** license.
5. **Produced baseline + RFC docs** (all merged to `main`).

## Documents on `main` (source of truth)
| File | Purpose | PR |
|---|---|---|
| `CURRENT_IMPLEMENTATION.md` | As-is engineering baseline (architecture, pipeline, speed/overspeed, state, config, network, limitations, Facts-vs-Assumptions). | #3 |
| `docs/current-overspeed-sequence.md` | One-page overspeed sequence diagram. | #3 (Mermaid fixed in #4) |
| `REFERENCE_IMPLEMENTATION.md` | Reference of implemented changes incl. the 4 fixes + the debug bypass. | #2 |
| `docs/rfc/road-aware-speed-limits.md` | RFC: Context-Aware Road Speed Limits (BRD F2.1). | #2 |

PR history: #1 debug bypass → `345fe50`; #2 reference + RFC → `c3a21a0`; #3 baseline + diagram → `9451b62`; #4 Mermaid fix → `7461093`.

## Key facts to carry forward
- **Regression = TripGate gate (`087e0a9`).** `DEBUG_BYPASS_TRIP_GATE = true` is currently on `main`, so the gate is **inert**.
- **Overspeed is fully on-device:** GPS `location.speed` → ×3.6 (km/h) → median of last 5 accepted readings → compare `maxSpeedThreshold` (default 60) → throttle `speedCallBackFrequency` (default 30 s) → `overSpeedDetected`. No network.
- **Config propagation gap:** `stationarySpeedKmh`, `gateOnInVehicle`, `inVehicleSustainSeconds` are **not copied** in `initializeSensorFilter`, so they always use defaults (`CURRENT_IMPLEMENTATION.md` §9).
- **Roads API:** `placeId` = road identity; a road transition = `placeId` change; one call snaps and returns the limit.

## Open items for Session 3 (start here)
1. **Decide the production fix for the gate.** The debug flag is not it. Options discussed (no decision): relax/remove the gate; add a host→SDK driving-state ingestion API; or keep the gate and fix Activity-Recognition delivery. 
2. **Fold the "minimal design" into the RFC.** The committed RFC still describes the original **6-component** design. In Session 2 chat I proposed a much simpler **single `RoadSpeedLimitTracker` class** (reuse `placeId`; drop the separate provider/cache/scheduler/policy) plus a **refresh policy** (hybrid: time cadence + min/max distance floor+cap + heading-change trigger + segment-stability backoff). **These were delivered in chat only and were NOT written into the RFC file.** Decide whether to update `road-aware-speed-limits.md` with them.
3. **Backend proxy for the Roads API key** (security + cross-user `placeId` cache for scale) — flagged; needs a backend owner.

## Cautions
- This is the **test repo**; verify against the production SDK before shipping anything.
- `DEBUG_BYPASS_TRIP_GATE = true` on `main` is a **debug experiment**, not the fix — don't mistake it.
- New docs are only visible on `main` **after merge**; a doc committed to the branch won't appear on the default branch until a PR is merged.
- If the branch's PRs are all merged, start new work from a fresh branch off `main`.
