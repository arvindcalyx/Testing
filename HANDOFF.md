# Handoff (Rolling)

**Last updated by:** Session 3 · **Date:** 2026-07-17
Single rolling handoff — overwritten each wrap. Previous handoffs are in git history.

## Current state
- **Portability resolved (S3):** D1 (four telematics fixes) confirmed **in office**; the gate
  bypass (D3, `DEBUG_BYPASS_TRIP_GATE`, sandbox PR #1) is **also in office as a stopgap** —
  office runs with the IN_VEHICLE gate **neutralised, not fixed**. Portability Tracker updated.
- **D4 deliverable (S3):** the context-aware road-speed feature was **re-derived from first
  principles** into a full RFC — `docs/rfc/context-aware-speed-limits.md` (nine sections).
  Minimal design: one new class `RoadSpeedLimitTracker`; comparand becomes
  `currentRoadLimit ?: maxSpeedThreshold`; fail-safe fallback (can only reduce false
  positives, never regress). 2 config fields, a 2-rule refresh policy (time cadence + a
  load-bearing staleness cap). **Specified, not prototyped.**
- **Hard findings (S3):** Speed Limits still **Asset-Tracking-licence-gated** in 2026; the
  Roads key is **not Android-restrictable** → a **backend proxy** is the production posture;
  the **Navigation SDK is not a drop-in limit source** (percentage/severity verdict only,
  needs active navigation).
- **SDK baseline unchanged** this session — `CURRENT_IMPLEMENTATION.md` is still current (no
  code changed; the RFC is design only).
- **Doc-set note:** the RFC is kept as a standalone `docs/rfc/` doc (the S3 deliverable the
  user asked for and merged), a deliberate deviation from D5's "fold everything into the
  spine" rule. Its decision + status live in `DECISIONS_LOG.md` D4.

## Where the work lives
- Branch `claude/session-s3-42vp40`, **merged to `main` via PR at the S3 wrap** (per user
  request). Once merged, the RFC + doc updates are the durable record.

## Open items for the next session (Session 4)
1. **Production fix for the trip gate (D2/D3) — still the priority.** Office is live on the
   debug bypass. The road-speed RFC is **orthogonal** (it does not fix the gate). Options
   unchanged: relax/remove the gate · add a host→SDK driving-state ingestion API · fix
   Activity-Recognition delivery. No decision yet.
2. **Prototype / validate the road-speed RFC (D4).** Feasibility is gated on the Asset
   Tracking licence; validate the tracker and the fail-safe (especially the **staleness cap**,
   which is what keeps the no-regression guarantee true) before any office transfer.
3. **Backend proxy for Roads** (holds the licensed key server-side, cross-user
   `placeId → limit` cache) — needs a backend owner.
4. **Decide:** restore `REFERENCE_IMPLEMENTATION.md` to the tree, or keep it in git history
   only (it is currently cited as a source but is not in the tree — WATCHPOINT 10).

## Mandatory question at session start
Before any work, ask the user:
> "Since the previous session, have any sandbox decisions or implementations been carried
> into the office repository?"

If yes → update the Portability Tracker in `DECISIONS_LOG.md` first. (D1/D3 were resolved at
S3; ask about any further transfers, including the pending trip-gate fix and the road-speed
feature.)
