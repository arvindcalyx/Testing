# JRM Knowledge Base

Consolidated strategy, advisory, requirements, roadmap, and SDK findings for the
Journey Risk Management telematics work. **This is the context layer** — the
"why," the judgment, the plan. The code lives in the SDK repo; this is everything
that isn't code.

> **Scrubbed.** No company names, credentials, internal URLs, IPs, or personal
> names. Package paths shown as `com.test.*`. Safe to keep alongside the scrubbed
> SDK reference repo.

## How to use this with a fresh AI session

Point any new Claude / Cursor / Codex session at this folder and tell it which
file to read for the task at hand. For *code/fix* work it also needs the SDK repo
+ `docs/SESSION_HANDOFF.md`. For *strategy/roadmap/advisory* work, this folder is
the context.

## Contents

### 00-strategy/ — the "why" and the decisions
| File | What it is | Read it when |
|---|---|---|
| `feasibility-and-decision-log.md` | The full running analysis: build-vs-buy vs vendor (Sentiance/CMT/HyperTrack), what's achievable solo + AI-assisted, the parallel-engine POC decision, sizing. The complete decision log. | You need the reasoning behind any recommendation, or to brief someone on the whole picture. |
| `executive-feasibility-assessment.md` | CTO-reviewer-style exec assessment: BRD table, SDK findings, 4–8wk / 2–4mo realism, build-vs-enhance-vs-buy, leadership recommendations. | Sponsor/CIO/leadership review. |
| `stakeholder-brief.md` | Plain-English version for non-technical readers: what's solvable, what's genuinely hard and why, what just takes time. | Circulating to a broad or non-technical audience. |
| `poc-handoff-brief.md` | Self-contained brief to build a parallel detection-engine POC (architecture, modules, week-by-week, JSONL schemas, definition of done). | Kicking off a POC build session. |

### 01-requirements/
| File | What it is |
|---|---|
| `BRD-reference.md` | Generic abstracted BRD with per-item classification (POC / Defer / ML / Vendor / DONE). The prioritisation source of truth. |

### 02-roadmap/
| File | What it is |
|---|---|
| `roadmap-3-and-6-month.md` | Phase 1 (0–3mo) + Phase 2 (3–6mo) delivery plan mapped to BRD, with critical-path callouts, leadership asks, and honest risks. The meeting deliverable. |

### 03-execution/ — how to actually run the fix work
| File | What it is |
|---|---|
| `fix-prompts-and-workflow.md` | The 5 paste-ready AI-session prompts to (re)implement the 4 false-positive fixes, one bug per commit, with cost estimates and the do-not-touch list. |
| `repo-setup-and-handoff-docs.md` | Merge + docs setup prompt, plus the exact content of `SESSION_HANDOFF.md` / `ROADMAP.md` / `BRD.md` that live in the SDK repo. |

### 04-sdk-findings/ — what the code review found
| File | What it is |
|---|---|
| `code-review-findings.md` | The 5 root-cause defects + 3 architectural limitations + security finding, with file references, fixes, and expected FP-reduction per bug. |
| `event-flow-and-architecture.md` | How detections flow (SDK vs host app), how speed is calculated, the listener interface, buffering vs callback, and which downstream systems improve when FPs drop. |

## The one-paragraph summary of everything

The existing telematics SDK's false-positive flood (60–80K/day, driving alert
fatigue) is caused by five identifiable bugs and three architectural shortcuts —
not a deep-tech gap. Fixing them (done: Bugs 1, 5, 2, 3 → ~60–80% FP reduction)
is 4–6 weeks of in-house work, validated via a parallel POC + field drive, not a
vendor purchase. Of the full BRD, ~two-thirds is mainstream engineering
deliverable over 3–6 months; three items (helmet CV, multi-occupant seatbelt,
automatic crash detection) are genuinely hard and belong on vendor / multi-month
ML tracks. The critical path today is getting fix set 1 reviewed and field-
validated — everything else sequences behind that proof.

## Provenance

Produced across an advisory session covering: SDK source review, build-vs-buy
analysis, BRD assessment, fix implementation planning, and delivery roadmapping.
Update `02-roadmap/` and `01-requirements/` as work lands; treat `00-strategy/`
as a stable decision record.
