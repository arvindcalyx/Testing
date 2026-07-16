# Handoff (Rolling)

**Last updated by:** Session 2 · **Date:** 2026-07-16
Single rolling handoff — overwritten each wrap. Previous handoffs are in git history.

## Current state
- The **lean sandbox operating model** is now in place — see `CLAUDE.md`. Doc spine:
  `CLAUDE.md` · `CURRENT_IMPLEMENTATION.md` · `DECISIONS_LOG.md` · `HANDOFF.md` ·
  `WATCHPOINTS.md` (+ `docs/BRD.md` as reference input).
- The SDK as-is baseline is documented in `CURRENT_IMPLEMENTATION.md`.
- `DEBUG_BYPASS_TRIP_GATE = true` is on `main` — the trip gate is **inert** (debug state,
  not the production fix; see `DECISIONS_LOG.md` D3).

## Open items for the next session (Session 3)
1. **Production fix for the trip gate** (D2/D3). The debug flag is not it. Options discussed:
   relax/remove the gate, add a host→SDK driving-state ingestion API, or fix
   Activity-Recognition delivery. No decision yet.
2. **Prototype / validate the road-speed design** (D4): single `RoadSpeedLimitTracker` +
   hybrid refresh policy. Design is captured in `DECISIONS_LOG.md` D4; validate before any
   office transfer.
3. **Backend proxy for the Roads API key** (security + cross-user `placeId` cache) — needs a
   backend owner; flagged, not decided.

## Mandatory question at session start
Before any work, ask the user:
> "Since the previous session, have any sandbox decisions or implementations been carried
> into the office repository?"

If yes → update the Portability Tracker in `DECISIONS_LOG.md` first. **Currently D1's office
status is unknown** and should be resolved.
