# KindnessDice

## What This Is

KindnessDice is an Android app that runs as a persistent background service and triggers a daily act-of-kindness suggestion when the user shakes their phone for at least 5.5 seconds. It calls the Gemini API on release and displays the result as a fullscreen immersive overlay. The experience is entirely physical — no buttons, no browsing, just shake and receive.

## Core Value

Shake your phone and instantly receive one specific, concrete act of kindness — the physical gesture is the entire interface.

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] Background ForegroundService runs persistently, survives phone restarts via boot receiver
- [ ] Shake detection: linear accelerometer, aT ≥ 20 m/s² sustained for ≥ 5.5s continuously; resets on drop below threshold
- [ ] Continuous vibration + looping dice-roll sound during active shake
- [ ] Haptic settling pattern on release (5-step decreasing buzz sequence)
- [ ] Gemini API call on release; prompt: "Suggest one specific, warm, and concrete act of kindness I can do today. Be direct, one sentence, no preamble."
- [ ] Fullscreen overlay with warm-neutrals design (cream/amber, rounded/serif font); dismisses on tap
- [ ] If Gemini fails: show error message on overlay ("Couldn't reach kindness today. Try again.")
- [ ] Re-shake while overlay shown: dismiss overlay, restart shake timer
- [ ] Settings screen: toggle to enable/disable service
- [ ] First-launch screen: Samsung battery whitelist prompt + permission requests (FOREGROUND_SERVICE, RECEIVE_BOOT_COMPLETED, VIBRATE, POST_NOTIFICATIONS)
- [ ] Persistent foreground notification: "Kindness Dice is active."
- [ ] Gemini API key in .env, gitignored, never hardcoded

### Out of Scope

- iOS support — Android-only by design; iOS lacks equivalent ForegroundService + boot receiver pattern
- Kindness history / suggestion log — ephemeral by design, no storage
- Scheduled/timed suggestions — shake-triggered only
- Social sharing — no social features
- Custom shake thresholds in settings — fixed sensitivity by design

## Context

- Codebase is a fresh React Native 0.85.1 scaffold (New Architecture enabled, Hermes, minSdk 24 / targetSdk 36)
- All app logic must be written from scratch — only safe-area-context and the default screen component exist
- Android-specific native modules required: ForegroundService (Kotlin), BroadcastReceiver (boot), accelerometer sensor
- Samsung devices have aggressive battery optimization that kills background services; must prompt user to whitelist on first launch
- Gemini API is the sole external dependency; key stored in .env via react-native-dotenv or similar

## Constraints

- **Platform**: Android only — iOS build infrastructure exists in scaffold but is not a target
- **API key**: Must never be committed; .env pattern required from day one
- **Background**: Android ForegroundService mandatory for persistent background operation (system requirement)
- **Sensor**: Linear accelerometer (TYPE_LINEAR_ACCELERATION) not total accelerometer — excludes gravity from readings
- **minSdk**: 24 (Android 7.0+) — set in gradle, do not lower

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| 5.5s continuous threshold | Prevents accidental triggers from drops/bumps (high spike, brief) | — Pending |
| Linear accelerometer not gravity-inclusive | Gravity-inclusive would add ~9.8 m/s² baseline; linear gives clean motion reading | — Pending |
| ForegroundService not WorkManager | WorkManager is deferrable; shake detection needs always-on real-time sensor access | — Pending |
| Warm neutrals overlay (cream/amber) | Feels warm and human, not clinical or alarming | — Pending |
| No history storage | Ephemeral by design — each suggestion is a moment, not a log | — Pending |
| Re-shake dismisses overlay | Allows immediate retry without manual dismiss tap | — Pending |
| Gemini error shows message (not silent fail) | User deserves to know something went wrong; silent fail feels broken | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-04-14 after initialization*
