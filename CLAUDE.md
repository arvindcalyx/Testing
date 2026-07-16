# CLAUDE.md — Sandbox Operating Model

## What this repository is
A **personal AI-reasoning sandbox**, not a production repository. Its purpose is to
reason through BRDs, investigate SDK behaviour, evaluate architecture options, validate
solutions, and produce **implementation-ready decisions**.

The **office repository** is the production implementation and follows the organisation's
normal engineering process (Cursor implementation → engineering review → QA → approvals →
merge). This sandbox optimises for **reasoning continuity, not production governance**.

**Hard rules**
- No office code, diffs, commits, credentials, or proprietary information is ever stored here.
- Only the **implementation status** of decisions is tracked (see `DECISIONS_LOG.md`).
- Anything decided here must be expressed **portably** — enough to re-implement in the
  office repo via Cursor, without relying on sandbox-only names/paths.

## Permanent document set (the only operating-model docs)
| Doc | Role |
|---|---|
| `CLAUDE.md` | This file. The protocol. |
| `CURRENT_IMPLEMENTATION.md` | The factual as-is baseline of the SDK under study. |
| `DECISIONS_LOG.md` | Validated decisions + rationale + the **Portability Tracker**. |
| `HANDOFF.md` | Single **rolling** handoff = current state for the next session. |
| `WATCHPOINTS.md` | Durable gotchas/cautions that persist across sessions. |

Reference input (not part of the spine): `docs/BRD.md`.
Everything else is folded into the above or removed; prior versions remain in git history.

## Session start (every session, in order)
1. Load this `CLAUDE.md`.
2. Follow this orientation protocol.
3. Read `HANDOFF.md` — where we are.
4. Read `DECISIONS_LOG.md` — what's decided and outstanding.
5. Read `WATCHPOINTS.md` — what to watch.
6. Read `CURRENT_IMPLEMENTATION.md` **only as required** for the task.

**Then, before doing any work, ask the user exactly one question:**
> "Since the previous session, have any sandbox decisions or implementations been carried
> into the office repository?"

If **yes** → update the **Portability Tracker** in `DECISIONS_LOG.md` before continuing.

## Session wrap (on "wrap the session")
- Append any new decisions to `DECISIONS_LOG.md` (decision + rationale + portability status).
- Update the **Portability Tracker** table.
- Add any new durable gotchas to `WATCHPOINTS.md`.
- Regenerate `HANDOFF.md` (overwrite; git history keeps the prior one).
- Update `CURRENT_IMPLEMENTATION.md` **only if the SDK baseline actually changed**.
- Keep the doc set to the five above (+ `docs/BRD.md`). Fold or remove anything else.
- **Durable = merged to `main` via PR.** A doc on a branch is not yet the record.

## Sandbox ↔ office boundary
- Designs and decisions are the deliverable; you re-implement them in the office repo.
- Track only **status** here: designed / validated / implemented-in-office.
- Never paste office code, diffs, commit contents, or proprietary values into this repo.
