# Business Requirements Reference — Journey Risk Management

This is a generic abstracted reference for the JRM-class telematics
program that this SDK is built for. Use it to prioritise fix work in
ROADMAP.md.

## Context

A large mobile field workforce drives daily as part of their job. Road
incidents are the #1 cause of workplace injuries. The existing in-app
telematics module produces tens of thousands of alert events per day,
the vast majority false positives. Users have learned to ignore the
resulting alerts (alert fatigue). The program's job is to fix this so
the alerting layer regains user trust and the safety pipeline (manager
dashboard, consequence engine, risk scoring) gets clean signal.

## Capability list

Classification:
- **POC** = scope for current fix-work program in this repo
- **Defer** = real, but not in scope for now
- **ML** = requires labelled data / model training / ML specialists
- **Vendor** = better solved by a third-party SDK

### Pre-journey controls
| # | Capability | Class | Notes |
|---|---|---|---|
| F1.1 | Geofenced home-login check before trip start | Defer | Android Geofencing API; trivial later. |
| F1.2 | Pre-trip vehicle-type selection (2W / 4W / public / cab) | POC (minimal) | Drives threshold branching. |
| F1.3 | Helmet detection via CV (presence, certification mark, chin strap, liveness) | ML / Vendor | No public dataset for the certification mark; multi-month ML project. |
| F1.4 | Seatbelt detection via CV (driver + passenger count) | ML / Vendor | Multi-occupant from front camera essentially infeasible. |
| F1.5 | Pre-journey weather + route + blackspot risk gating | Defer | API stitching + rules. |
| F1.6 | Night-driving policy block (e.g., 19:00–06:00) | Defer | Trivial rules. |
| F1.7 | Mandatory micro-learning gate on poor risk profile | Defer | Workflow + content. |
| F1.8 | Vehicle-document compliance tracking | Defer | Standard CRUD. |

### In-journey monitoring (the core fix area)
| # | Capability | Class | Notes |
|---|---|---|---|
| F2.1 | Context-aware speed limits via map API | POC | ROADMAP item 3. |
| F2.2 | Harsh acceleration / braking / cornering, 2W/4W thresholds | DONE | Bug 2 + Bug 5 commits cover braking/acceleration. Cornering uses lateral component once sensor reorientation lands (ROADMAP item 1). |
| F2.3 | Phone-usage detection during driving | POC (relaxed) | ROADMAP item 5. Spec relaxed to "screen-on + interaction events during trip" because post-Android 10 you cannot observe other apps' content without intrusive accessibility permissions. |
| F2.4 | Continuous-driving fatigue timer + mandatory rest | Defer | Trivial timer. |
| F2.5 | In-cabin audio/haptic nudges on unsafe behaviour | Defer | TTS + vibration. Out of scope for this SDK; lives in host app. |
| NFR | Public-transport suppression (no false alerts on metro/bus/train) | DONE | Bug 3 (Activity Recognition + IN_VEHICLE gate). |

### Consequences & governance
| # | Capability | Class | Notes |
|---|---|---|---|
| F3.1 | Rule-based consequence engine | Defer | Backend, not SDK. |
| F3.2 | Evidence / artifact storage | Defer | Backend. |
| F4.1 | Manager dashboard | Defer | Web app. |
| F4.2 | Driver risk score | Defer | Backend; garbage-in if F2.x FPs not fixed first — which is why FP fixes are the priority. |
| F4.3 | Gamification / leaderboards | Defer | UI + cron. |
| F5.1 | Roles & escalation hierarchy | Defer | RBAC. |
| F5.2 | Centralised safety broadcasts | Defer | Push notifications. |
| F5.3a | Manual SOS button + location dispatch | Defer | Trivial; lives in host app. |
| F5.3b | Automatic crash detection | Vendor | Deferred per ROADMAP "Deferred" section. |
| F6.1 | Automated MIS emails | Defer | Cron + templates. |

## Summary

- **Already done in this repo (main):** Bug 1, 5, 2, 3 → ~60–80%
  cumulative false-positive reduction, plus the transit-suppression NFR.
- **POC scope for this repo's ongoing fix work:** F2.1, F2.3, sensor
  reorientation (closes F2.2 cornering), FusedLocation migration,
  modern foreground lifecycle. All listed in ROADMAP.md.
- **Out of scope (vendor / multi-month ML / different system):** F1.3,
  F1.4, F5.3b, all F3/F4/F5 backend or host-app items.
