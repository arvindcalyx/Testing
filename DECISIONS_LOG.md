# Decisions Log

Validated decisions, their rationale, and their portability status. This is the sandbox's
primary deliverable — it answers **what was designed, what was validated, and what has
actually been implemented in the office repository.** No office code, diffs, commits, or
proprietary values are stored here — only status.

## Portability Tracker

| # | Decision | Sandbox | Office | Transferred | Notes |
|---|----------|---------|--------|-------------|-------|
| D1 | Four telematics fixes (see D1) | ✓ in baseline | ✓ implemented | ✓ confirmed S3 | Confirmed present in office at Session 3 start (user confirmation). |
| D2 | Regression root cause = IN_VEHICLE trip gate | ✓ analysed | n/a | — | Analysis, not a transferable change. |
| D3 | `DEBUG_BYPASS_TRIP_GATE` debug flag | ✓ in sandbox | ⚠️ present (stopgap) | ✓ confirmed S3 | Transferred despite the will-not-transfer intent: office mirrors the debug bypass (sandbox PR #1, whose body notes it mirrors a prod debug change). Stopgap, **not** the production fix — office now runs with the gate neutralised; must be superseded by the real trip-gate fix (HANDOFF open item 1). |
| D4 | Road-aware speed limits (minimal design + refresh policy) | ◐ designed, not validated | ✗ not implemented | — | Prototype/validate before any office transfer. |
| D5 | Lean sandbox operating model | ✓ complete | n/a | — | Sandbox meta; not an office change. |

Legend: ✓ done · ◐ partial · ✗ not started · 🚫 will-not-transfer · ⚠️ transferred as stopgap (caution) · ❓ unknown · n/a not applicable

## Decision Records

### D1 — Four telematics fixes (historical, pre-this-session)
- **Decision:** the codebase carries four fixes on top of the copied baseline (`e11224c`):
  (1) stop rounding GPS coordinates (`e81ab27`); (2) unify stationary threshold, default
  2 km/h (`ffb4dad`); (3) harsh driving requires accelerometer + GPS agreement at 3 m/s²
  (`afcab25`); (4) modern Activity Recognition + IN_VEHICLE gate (`087e0a9`).
- **Rationale:** false-positive reduction on telematics alerts.
- **Status:** present in the sandbox baseline. **Confirmed implemented in the office repo** at
  Session 3 start (user confirmation). Portability Tracker updated — D1 transferred.

### D2 — Regression root cause: the IN_VEHICLE trip gate
- **Decision:** the "speed stays N/A / no overspeed / notification disappears" regression is
  caused by fix (4)'s trip gate. `processLocationUpdate` returns early while
  `gateOnInVehicle == true` and `TripGate.isDriving != true`; `isDriving` requires a
  30 s-sustained IN_VEHICLE Activity-Recognition transition, so the speed pipeline is
  suppressed. Fixes (1)–(3) are not on the overspeed path.
- **Validated:** yes — the debug bypass (D3) restored speed updates.
- **Status:** analysis complete. A **production fix is still open** (see HANDOFF).

### D3 — `DEBUG_BYPASS_TRIP_GATE` debug flag
- **Decision:** add a flag in `SafetyConnectService` that neutralises only the gate's early
  return, leaving TripGate otherwise intact, to confirm the root cause.
- **Rationale:** isolate the regression without reverting any fix.
- **Status:** in the sandbox (`true` on `main`). It was **intended** as a debug experiment that
  must not be transferred to office as the fix — but at Session 3 start the user confirmed the
  bypass is **present in the office/production repo** as a stopgap (sandbox PR #1 mirrored a
  debug change already applied to prod). Consequence: office currently runs with the IN_VEHICLE
  gate **neutralised**, not fixed. The real fix (relax the gate / add a host driving-state
  ingestion API / fix AR delivery) is now the **priority open item** (HANDOFF item 1) and must
  supersede the bypass in office.

### D4 — Road-aware (context-aware) speed limits
- **Decision (direction):** replace the fixed `maxSpeedThreshold` comparand with the road's
  posted limit (from Google Roads Speed Limits), falling back to the fixed threshold when
  unavailable.
- **Chosen design (minimal):** a single new class (`RoadSpeedLimitTracker`) that reuses
  Google's `placeId` as road identity (no bespoke road abstraction), holds the current
  segment's limit, resolves asynchronously via the existing network stack, and is read
  synchronously in `handleValidSpeed`. Reuse `SpeedManager`, `CurrentLocation`, `Manager`,
  `NetworkModule`, and the existing callbacks. (Supersedes an earlier 6-component design.)
- **Refresh policy (recommended):** hybrid — time cadence (~15 s) with a min-distance floor
  (~120 m) and a max-distance cap, a heading-change early trigger for turns/exits, and
  segment-stability backoff; fall back to the fixed threshold on any gap.
- **Key API facts:** `placeId` = road-segment identity; one Speed-Limits call with a `path`
  returns snapped points **and** the limit; ≤100 points/request; units KPH; the endpoint
  requires a Google Maps **Asset Tracking** license.
- **Status:** **designed, not yet validated/prototyped.** The full exploration is retained in
  git history (former `docs/rfc/road-aware-speed-limits.md`).

### D5 — Lean sandbox operating model
- **Decision:** adopt a 5-document spine (`CLAUDE.md`, `CURRENT_IMPLEMENTATION.md`,
  `DECISIONS_LOG.md`, `HANDOFF.md`, `WATCHPOINTS.md`) + `docs/BRD.md` as input; fold/remove
  all other operating-model artefacts (prior handoffs, the reference doc, the road-speed RFC,
  the `knowledge-base/` tree, `docs/ROADMAP.md`).
- **Rationale:** the sandbox produces reasoning, not shipping code; heavy production
  governance is unnecessary. Optimise for reasoning continuity.
- **Status:** complete (Session 2 wrap). Removed docs remain in git history.
