# Watchpoints

Durable cautions that persist across sessions. (Ephemeral, session-specific state lives in
`HANDOFF.md`; design decisions live in `DECISIONS_LOG.md`.)

1. **This is a sandbox, not production.** It was copied from the office repo with proprietary
   info, credentials, and IP removed. Never store office code, diffs, commits, or real
   credentials here. The office repo may have diverged from this copy.
2. **`DEBUG_BYPASS_TRIP_GATE = true` is on `main`** — the IN_VEHICLE trip gate is inert. It
   was a debug experiment, but **office/production also carries it as a stopgap** (confirmed
   S3; sandbox PR #1 mirrored a prod debug change). It is **not** the production fix; the real
   trip-gate fix is still open and must supersede it in office.
3. **Config-propagation gap:** `stationarySpeedKmh`, `gateOnInVehicle`, and
   `inVehicleSustainSeconds` are not copied in `initializeSensorFilter`, so they always use
   their defaults regardless of caller input (`CURRENT_IMPLEMENTATION.md` §9). **Any new
   `SensorFilters` field must be added to that copy-list or it silently stays default.**
4. **Overspeed is fully on-device**; the SDK uses `LocationManager.GPS_PROVIDER` only (not
   fused). No network dependency in the speed path.
5. **Google Speed-Limits API requires an Asset Tracking license** (confirmed still gated in
   2026; sales-negotiated, not self-serve) — the feasibility gate for the road-speed feature
   (D4). Also: a Roads API **key cannot be Android-app-restricted** (web-service key,
   IP-only), so the licensed key must **not** be embedded on-device — route via a backend
   proxy.
6. **GitHub Mermaid** breaks on `;`, `<=`/`>=`, `|`, `{}`, and `<br/>` inside `participant`
   aliases. Keep sequence-diagram text free of these tokens.
7. **"Durable" means merged to `main` via PR.** A doc committed only to a branch is not
   visible on `main` and is not yet the record.
8. **Working branch (S3):** `claude/session-s3-42vp40`. If its PR is merged, start new work
   from a fresh branch off `main`.
9. **Google Navigation SDK is not a drop-in speed-limit source.** It exposes only
   `SpeedingListener` (percentage-over-limit + MAJOR/MINOR/NONE severity), **not** the raw
   posted limit or the current road `placeId`, and its speed alerts require an active
   turn-by-turn navigation session. Adopting it means replacing the overspeed pipeline
   wholesale, not a comparand swap (D4 / RFC §8.8).
10. **`REFERENCE_IMPLEMENTATION.md` is not in the working tree** — it was folded/removed in the
    D5 lean migration and lives only in git history (recover via
    `git show 1b212fc^:REFERENCE_IMPLEMENTATION.md`), even if it is cited as a source of truth.
    `CURRENT_IMPLEMENTATION.md` is the in-tree baseline. Decide whether to restore it if it is
    to be treated as authoritative.
11. **`developers.google.com` is egress-blocked in this sandbox** (HTTP 403 on WebFetch /
    CONNECT). Verify Google docs via the web-search index or on an unrestricted network; treat
    exact prices/quotas as `[VERIFY LIVE]`.
