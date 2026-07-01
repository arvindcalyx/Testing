# Business Requirements Reference — Journey Risk Management (Generic)

Abstracted reference for the JRM-class telematics program. Use to prioritise work.
No company names, credentials, or internal infrastructure.

## Context

A large mobile field workforce drives daily for work. Road incidents are the #1
cause of workplace injuries. The existing in-app telematics module produces tens
of thousands of alert events/day, mostly false positives → alert fatigue → users
ignore alerts. The program's job: fix false positives so the alerting layer
regains trust and the downstream safety pipeline (dashboard, consequence engine,
risk scoring) gets clean signal.

## Capability list

Classification: **POC** (current scope) · **Defer** (real, later) · **ML**
(needs labelled data/model) · **Vendor** (buy) · **DONE** (shipped in fix set 1).

### Pre-journey controls
| # | Capability | Class |
|---|---|---|
| F1.1 | Geofenced home-login check | Defer |
| F1.2 | Vehicle-type selection (2W/4W/public/cab) | POC (minimal) |
| F1.3 | Helmet CV (presence + certification mark + chin strap + liveness) | ML / Vendor |
| F1.4 | Seatbelt CV (driver + passenger count) | ML / Vendor (multi-occupant infeasible from front cam) |
| F1.5 | Pre-journey weather + route + blackspot gating | Defer |
| F1.6 | Night-driving policy block | Defer |
| F1.7 | Micro-learning gate on poor risk profile | Defer |
| F1.8 | Vehicle-document compliance tracking | Defer |

### In-journey monitoring (core fix area)
| # | Capability | Class |
|---|---|---|
| F2.1 | Context-aware speed limits via map API | POC |
| F2.2 | Harsh accel/brake/cornering, 2W/4W thresholds | DONE (braking/accel); cornering after sensor reorientation |
| F2.3 | Phone-usage detection | POC (relaxed: screen-on + interaction during trip) |
| F2.4 | Fatigue timer + mandatory rest | POC |
| F2.5 | In-cabin audio/haptic nudges | POC (host-app side) |
| NFR | Public-transport suppression | DONE (Activity Recognition gate) |

### Consequences, analytics, governance
| # | Capability | Class |
|---|---|---|
| F3.1 | Consequence rule engine | Defer (backend) |
| F3.2 | Evidence / artifact storage | Defer (backend) |
| F4.1 | Manager dashboard | POC (Phase 1) |
| F4.2 | Driver risk score | POC (needs clean events first) |
| F4.3 | Gamification / leaderboards | Defer |
| F5.1 | Roles & escalation hierarchy | Defer |
| F5.2 | Centralised safety broadcasts | Defer |
| F5.3a | Manual SOS + location dispatch | POC |
| F5.3b | Automatic crash detection | Vendor |
| F6.1 | Automated MIS emails | Defer |

## The three genuinely hard items (vendor / multi-month ML)

- **F1.3 helmet certification-mark CV** — no public dataset; liveness is research
- **F1.4 multi-occupant seatbelt CV** — infeasible from front phone camera
- **F5.3b automatic crash detection** — crash training data cannot be generated in-house; vendor owns this moat

Everything else is mainstream engineering. This is not a deep-tech program — it's
a mainstream program with a small deep-tech tail.
