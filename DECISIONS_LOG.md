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
| D4 | Context-aware road speed limits (re-derived RFC) | ◐ specified, not prototyped | ✗ not implemented | — | Full RFC in `docs/rfc/context-aware-speed-limits.md`. Feasibility-gated on the Asset Tracking licence; backend proxy is the production posture. Prototype/validate before any office transfer. |
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

### D4 — Context-aware road speed limits
- **Decision (direction):** replace the fixed `maxSpeedThreshold` comparand with the road's
  posted limit, falling back to the fixed threshold when unavailable.
- **Deliverable (S3):** a re-derived, first-principles RFC — `docs/rfc/context-aware-speed-limits.md`
  — with all nine sections (gap analysis, architecture, component responsibilities, sequence
  diagrams, API changes, config, file-by-file inventory, risks, minimum-viable rationale).
  Supersedes the earlier 6-component draft (removed in D5; git history only).
- **Chosen design (minimal):** one new class `RoadSpeedLimitTracker` — async-resolves the
  posted limit off the main thread (reusing OkHttp/Gson via a **dedicated short-timeout
  client**, not the crash client), caches it in a single `@Volatile Float?`, read
  **synchronously** at `handleValidSpeed` as `currentRoadLimit ?: maxSpeedThreshold`.
  **Fail-safe:** unknown / stale / disabled / unlicensed / timeout → `null` → today's exact
  behaviour (can only reduce false positives, never regress). No `SpeedManager` /
  `CurrentLocation` / `TripGate` / callback changes; **no dependency on
  `DEBUG_BYPASS_TRIP_GATE`**; manifest already has `INTERNET`.
- **Minimum-architecture pass (S3, reconciled with an independent review):** 2 config fields
  (`isRoadSpeedLimitEnabled`, `roadSpeedLimitEndpoint`) + their copy-list entries; a 2-rule
  refresh policy (time cadence + a **load-bearing staleness cap** that expires to `null`);
  trimmed DTOs. Consciously deferred to validation: tolerance band, on-device API-key field,
  distance/heading signals, placeId-stability backoff, Collecting-phase warm-up.
- **Production posture:** the Roads key **cannot be Android-app-restricted** (web-service
  key, IP-only) and Speed Limits needs a licensed key, so the SDK is endpoint-agnostic and
  production routes through a **backend proxy** (holds the key server-side, mirrors the
  Google response shape, enables cross-user `placeId → limit` caching). The proxy is a
  separate backend deliverable (open item).
- **Rejected alternatives (in the RFC):** mutating `maxSpeedThreshold` in place — destroys
  the fallback, `null → 0f` fires on every reading, couples config with runtime state (§3.4);
  the **Navigation SDK** as a drop-in limit source — it exposes only a percentage-over /
  severity verdict (`SpeedingListener`), **not** the raw limit or a road `placeId`, and needs
  an active navigation session (§8.8).
- **Why a refresh policy at all:** road identity (`placeId`) is a **server-side output of a
  billable Google call**, not a locally observable trigger, so an on-device cadence is
  unavoidable; `placeId`-change is post-call feedback, not an initiating trigger (§6.3).
- **Key API facts (verified July 2026 via search index — `developers.google.com` is
  egress-blocked here):** Speed Limits still **Asset-Tracking-licence-gated** (sales-
  negotiated, not self-serve); one `path` call returns snapped points + limit + `placeId`;
  ≤100 points/request; units KPH; `snapToRoads`/`nearestRoads` are unlicensed and return
  `placeId`. Exact prices/quota are flagged **[VERIFY LIVE]** in the RFC.
- **Status:** **specified (RFC complete), not prototyped/validated.** Feasibility gated on
  the Asset Tracking licence. Office: not implemented.

### D5 — Lean sandbox operating model
- **Decision:** adopt a 5-document spine (`CLAUDE.md`, `CURRENT_IMPLEMENTATION.md`,
  `DECISIONS_LOG.md`, `HANDOFF.md`, `WATCHPOINTS.md`) + `docs/BRD.md` as input; fold/remove
  all other operating-model artefacts (prior handoffs, the reference doc, the road-speed RFC,
  the `knowledge-base/` tree, `docs/ROADMAP.md`).
- **Rationale:** the sandbox produces reasoning, not shipping code; heavy production
  governance is unnecessary. Optimise for reasoning continuity.
- **Status:** complete (Session 2 wrap). Removed docs remain in git history.
