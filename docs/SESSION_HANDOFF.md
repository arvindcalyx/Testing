# Session Handoff — SafetyConnect SDK (personal sandbox)

**Purpose of this doc:** every Claude Code / Cursor / Codex session that
opens this repo should read this file FIRST. It sets the standing rules
and the project state.

## Repo purpose

This is a personal scrubbed copy of an Android telematics SDK, used as a
sandbox to develop and validate fixes that are ultimately applied to a
real (company-internal) version. The package paths (`com.test.*`) and
network configuration (`api.example.com`, `Basic dGVzdDp0ZXN0`) are
intentionally generic. Replace before any non-trivial use.

## Branch model

- `main` — the working trunk. Contains the scrubbed SDK plus all
  accepted fix commits. Never modify directly except via merged PRs.
- Feature branches — every new fix or experiment branches off `main`,
  work happens there, and merges back via PR after human review.

## Standing operating rules for any AI session in this repo

1. **ONE bug per commit.** No combining. No "tidying" unrelated code.
   If you find a different problem mid-task, note it and stop — do not
   fix it in this commit.

2. **Do NOT touch these files** (out of scope for any fix work):
   - `safetyconnect/.../sdkinit/AccidentDetector.kt`
   - `safetyconnect/.../repoimpl/SensorInteractImpl.kt`
   - `safetyconnect/.../repoimpl/DataInteractImpl.kt`
   - `safetyconnect/.../network/NetworkModule.kt`
   - Anything under `capturelibrary/`
   Crash detection is a separate vendor-evaluation track. capturelibrary
   is mature image-handling code unrelated to detection. NetworkModule
   has placeholder credentials and shouldn't be modified without auth
   changes being in scope.

3. **Public SDK API stays backward-compatible.**
   - `SafetyConnectCommunicator` interface (in `SafetyConnectSDK.kt`):
     do not change signatures, do not remove methods.
   - `SensorFilters` data class: new fields must have safe defaults.
     Do not rename or remove existing fields.
   - Public methods on `SafetyConnectSDK` companion object: no signature
     changes.

4. **NO unit tests unless explicitly asked.** Field validation against
   recorded sensor traces is the proof for this class of code, not unit
   tests. Adding test files inflates the diff and slows review.

5. **`./gradlew :safetyconnect:assembleDebug` may not run** in your
   environment (no JDK 17 / no Android SDK / corporate proxy blocking
   downloads). If it runs, use it. If it doesn't, skip the build check
   and rely on lint + grep + careful reading. Note it in the commit
   body so reviewers know.

6. **Branch from `main`, never modify main directly.** Create a feature
   branch like `fix/sensor-reorientation`, push to that branch only,
   open a PR for human review before merging back.

7. **Be terse.** Only read files explicitly named in the task. Do not
   re-explore the repo. Do not summarise what you just did unless asked.

8. **If you encounter the company name or any specific brand reference
   in the code or commits — STOP.** This repo is supposed to be fully
   scrubbed. Report the leak immediately; do not commit anything that
   references the real company.

## How to start a new fix session

Use this short starter prompt (saves tokens vs re-deriving everything):

    Starting a new bug-fix session for the SDK in this repo.

    READ FIRST: docs/SESSION_HANDOFF.md and docs/ROADMAP.md.

    TASK: <pick an item from ROADMAP.md "Next up" list, OR describe
    the new BRD requirement to implement>.

    Branch off `main`, create `fix/<short-name>`.

    Constraints from docs/SESSION_HANDOFF.md apply. Be terse.

    Reply "Ready" and wait for go-ahead.
