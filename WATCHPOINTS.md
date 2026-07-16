# Watchpoints

Durable cautions that persist across sessions. (Ephemeral, session-specific state lives in
`HANDOFF.md`; design decisions live in `DECISIONS_LOG.md`.)

1. **This is a sandbox, not production.** It was copied from the office repo with proprietary
   info, credentials, and IP removed. Never store office code, diffs, commits, or real
   credentials here. The office repo may have diverged from this copy.
2. **`DEBUG_BYPASS_TRIP_GATE = true` is on `main`** — the IN_VEHICLE trip gate is inert. This
   is a debug experiment, **not** the production fix. Do not carry it to office.
3. **Config-propagation gap:** `stationarySpeedKmh`, `gateOnInVehicle`, and
   `inVehicleSustainSeconds` are not copied in `initializeSensorFilter`, so they always use
   their defaults regardless of caller input (`CURRENT_IMPLEMENTATION.md` §9).
4. **Overspeed is fully on-device**; the SDK uses `LocationManager.GPS_PROVIDER` only (not
   fused). No network dependency in the speed path.
5. **Google Speed-Limits API requires an Asset Tracking license** — a real feasibility gate
   for the road-speed feature (D4).
6. **GitHub Mermaid** breaks on `;`, `<=`/`>=`, `|`, `{}`, and `<br/>` inside `participant`
   aliases. Keep sequence-diagram text free of these tokens.
7. **"Durable" means merged to `main` via PR.** A doc committed only to a branch is not
   visible on `main` and is not yet the record.
8. **Working branch:** `claude/speed-detection-timeout-issue-abg3s4`. If its PRs are all
   merged, start new work from a fresh branch off `main`.
