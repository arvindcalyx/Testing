# Journey Risk Management — Delivery Roadmap
### 3-Month and 6-Month Plan

**Prepared for:** Leadership review
**Status date:** current
**Owner:** TPM

---

## 1. Where we are today

The investigation into the SafetyConnect telematics SDK is complete, and the first
workstream — **reducing the false-positive flood at the event-generation layer** —
has been implemented and is pending review.

| Item | Status |
|---|---|
| SDK code review (5 root-cause defects identified) | ✅ Complete |
| Fix set 1 — 4 targeted false-positive fixes | ✅ Implemented, PR raised |
| Peer review (Android lead) | ⏳ **Pending — critical path** |
| Validation build (test APK) | ⏳ **Pending — critical path** |
| Field validation (measured FP delta) | 🔲 Blocked on validation build |
| Production rollout | 🔲 Blocked on field validation |

**What Fix Set 1 delivers:** an expected **60–80% reduction in false-positive
violations** (currently 60–80K/day, largely ignored due to alert fatigue), plus
resolution of the **public-transport false-alert NFR**. This directly addresses the
BRD's core complaints about data accuracy, false positives, and alert fatigue.

### ⚠️ Critical-path dependency (needs a decision in this meeting)

The entire roadmap below is sequenced *after* Fix Set 1 is validated. We cannot
credibly build the next layers until we have measured proof the detection fixes
work in the field. **We need the Android lead's review and a validation build to
unblock the field test.** This is the single most time-sensitive ask.

---

## 2. Roadmap at a glance

```
        NOW ──────────► 3 MONTHS ──────────► 6 MONTHS
        │                    │                     │
  Fix Set 1           Phase 1:              Phase 2:
  (pending            In-journey            Pre-journey +
   validation)        detection quality     governance + analytics
                      + core workflow       + full BRD coverage
                                            
  Parallel tracks (span both, do not complete in 6mo):
    • Helmet / seatbelt computer vision  → vendor or ML programme
    • Automatic crash detection          → vendor evaluation
```

---

## 3. Phase 1 — Months 0–3: Detection quality + core in-journey workflow

**Theme:** Get the false-positive fix validated and shipped, then close the
remaining in-journey detection gaps and stand up the workflow essentials.

| # | Deliverable | BRD ref | Effort | Confidence | Dependency |
|---|---|---|---|---|---|
| 1.1 | **Close out Fix Set 1** — review, validation build, field test, measure FP delta, production rollout | F2.2, transit NFR | (done) + validation | High | **Android lead review + test APK** |
| 1.2 | **Sensor reorientation to vehicle frame** — kills the largest remaining FP class (phone in pocket/cupholder/loose mount) | F2.2 | 1–2 wks | High | Fix Set 1 merged |
| 1.3 | **Modern location stack** — migrate to FusedLocationProviderClient; better accuracy in tunnels/urban canyons, better battery | (quality) | 1 wk | High | — |
| 1.4 | **Context-aware speed limits** — replace fixed 45/60 with per-road posted limit via map API; "limit unknown" fallback | **F2.1** | 3 days eng | Medium | **Google Maps/Roads API key** |
| 1.5 | **Fatigue timer** — continuous-driving time + mandatory rest break | F2.4 | 2 days | High | — |
| 1.6 | **In-cabin nudges** — audio/haptic self-correction alerts | F2.5 | 2 days | High | — |
| 1.7 | **Manual SOS** — persistent button + live-location dispatch to emergency contacts | F5.3a | 3 days | High | — |
| 1.8 | **Manager dashboard v1** — team risk overview, live journey map, per-driver event timeline | F4.1 | 3–4 wks | High | Clean events from 1.1 |
| 1.9 | **Driver risk score (foundation)** — composite score from validated event data | F4.2 | 2 wks | High (eng) / Med (validity) | 1.1 must land first — garbage-in otherwise |

**Phase 1 exit criteria:** false positives measurably down ≥60% in the field;
speed limits are road-accurate; managers have a live dashboard; drivers get
real-time nudges and an SOS button.

**Phase 1 hard dependencies to resolve now:**
- Android lead review + validation build (blocks 1.1 → everything)
- Google Maps Platform / Roads API key provisioning (blocks 1.4)
- Decision on where the manager dashboard is hosted (blocks 1.8)

---

## 4. Phase 2 — Months 3–6: Pre-journey controls + governance + analytics

**Theme:** Add the pre-journey decision gates and the governance/analytics layer
that turns clean event data into behaviour change.

| # | Deliverable | BRD ref | Effort | Confidence | Dependency |
|---|---|---|---|---|---|
| 2.1 | **Home-login geofence** — GPS-verified check-in near registered home to capture the highest-risk commute segment | F1.1 | 1 wk | High | — |
| 2.2 | **Pre-trip vehicle-type selection** — 2W / 4W / public / cab; drives detection thresholds | F1.2 | 3 days | High | — |
| 2.3 | **Pre-journey risk gating** — weather + route + blackspot check; vehicle-specific block/warn; Low/Med/High risk display | F1.5 | 2–3 wks | High | Weather + map APIs; internal blackspot dataset |
| 2.4 | **Night-driving policy block** | F1.6 | 3 days | High | Policy definition from safety team |
| 2.5 | **Contextual micro-learning gate** — poor risk profile triggers mandatory coaching before journey unblocks | F1.7 | 1 wk eng | High | Learning content (safety team) |
| 2.6 | **Document & vehicle compliance** — RC / Insurance / PUC / DL tracking + expiry alerts | F1.8 | 1–2 wks | High | — |
| 2.7 | **Consequence engine** — rule-based warnings → micro-learning → line-manager escalation | F3.1 | 2–3 wks | High | Consequence policy (HR/safety) |
| 2.8 | **Evidence vault** — telemetry logs, pre-journey artifacts, tamper-evident storage for audit | F3.2 | 2 wks | High | — |
| 2.9 | **Gamification / leaderboards** — safest rider/driver recognition | F4.3 | 1–2 wks | High | Anti-gaming rules |
| 2.10 | **Roles & escalation hierarchy** — field → line manager → circle safety officer | F5.1 | 2 wks | High | HR org-data feed |
| 2.11 | **Safety broadcasts** — targeted push alerts by role/location | F5.2 | 1 wk | High | — |
| 2.12 | **Automated MIS reports** — daily/weekly/monthly compliance + LTI trend emails | F6.1 | 1 wk | High | — |
| 2.13 | **Phone-usage detection** — screen-on + interaction during trip (relaxed scope, see notes) | F2.3 | 3–5 days | Medium | — |

**Phase 2 exit criteria:** pre-journey gates live for PPE-adjacent risk controls;
consequence engine automating escalations; full analytics + MIS operational;
governance hierarchy mapped.

---

## 5. Parallel tracks — start now, complete beyond 6 months

These are genuinely hard (data-scarce or deep-tech) and must NOT be promised as
Phase 1/2 line items. They run in parallel on their own timeline.

| Track | BRD ref | Why it's a separate track | Recommended path |
|---|---|---|---|
| **Helmet detection (CV)** — presence + certification mark + chin strap + liveness | F1.3 | No public dataset for the certification mark; liveness is anti-spoofing research; needs a labelled-image data-collection programme first | Vendor evaluation OR fund an in-house CV data + model programme (multi-month) |
| **Seatbelt detection (CV)** | F1.4 | Single-occupant is tractable; multi-occupant from front camera is essentially infeasible | Scope to driver-only v1; defer multi-occupant |
| **Automatic crash detection** | F5.3b | Cannot ethically generate crash training data; ~300 historical incidents is a validation set, not a training set; specialist vendors own this via insurance-claim data | **Vendor** (evaluate Sentiance / CMT / Bosch-class). Manual SOS (1.7) covers the gap in the interim |

**Milestone for this track in the 6-month window:** vendor shortlist + PoC
integration for auto-crash; data-collection programme kicked off for helmet CV.
Neither *ships* in 6 months — but both are *de-risked and scoped* by month 6.

---

## 6. What we need from leadership (decisions/asks)

| Ask | Blocks | Urgency |
|---|---|---|
| Android lead review + validation build for Fix Set 1 | The entire roadmap's starting gun | **This week** |
| Google Maps Platform / Roads API key (Mobility tier) | Context-aware speed limits (F2.1) | Before month 1 |
| Decision: build helmet/seatbelt CV in-house vs vendor | Parallel track planning + budget | Before month 2 |
| Budget approval for auto-crash vendor evaluation | F5.3b track | Before month 2 |
| Safety/HR inputs: consequence policy, night-driving policy, micro-learning content | Phase 2 items 2.4, 2.5, 2.7 | Before month 3 |
| Dashboard hosting + data-residency sign-off | Manager dashboard (F4.1) | Before month 2 |

---

## 7. Honest risk callouts

1. **The roadmap's start is gated on Fix Set 1 validation.** Until the test APK is
   built and field-tested, we are proceeding on *expected* impact, not *measured*.
   Getting that validation done is the highest-leverage action available.
2. **Speed-limit data coverage** on rural roads is an external map-vendor limitation,
   not an engineering gap. We handle it with a "limit unknown" fallback — but full
   national coverage is not guaranteed by any vendor.
3. **Helmet CV, multi-occupant seatbelt, and auto-crash** are the three BRD items
   that cannot be delivered in 6 months by in-house engineering alone. Setting that
   expectation now prevents an over-promise later.
4. **Risk score and consequence engine are only as good as the event quality
   underneath them.** This is *why* the false-positive fix is sequenced first —
   everything downstream inherits its accuracy.

---

## 8. One-line summary for the meeting

> "Fix Set 1 (false-positive reduction) is built and pending validation — that's the
> starting gun. Once it's field-proven, Phase 1 (months 0–3) closes the in-journey
> detection gaps and stands up the dashboard + core workflow; Phase 2 (months 3–6)
> adds pre-journey controls, governance, and analytics for near-full BRD coverage.
> Three items — helmet CV, multi-occupant seatbelt, and auto-crash — are deep-tech
> and run as parallel vendor/ML tracks that get de-risked, not shipped, in 6 months.
> The one thing blocking everything today is getting Fix Set 1 reviewed and a
> validation build produced."
