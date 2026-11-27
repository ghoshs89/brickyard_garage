# Roadmap

Scope is `docs/PLAN.md` (Gemini's plan, adopted as-is). Status is updated only from verified repo state; see `docs/HANDOFF.md` for the latest evidence.

| Phase | What | Status |
|---|---|---|
| 1 | Toolchain (JDK 17, Android SDK, emulators), Gradle multi-module scaffold, docs, git, external keystore | Done 2026-11-26 |
| 2 | `:engine` (shaped boards, walls, portals, conveyors, baseplates, crates, clamps, oil, rust, parts, 4 power-ups + 10 combos, tools, victory lap, saves, gear trains, brick cars, Track Day) and `:tools` (validator, Monte Carlo bot, IDDFS solver, gear verifier, forge, audio synth, icon renderer), JUnit suite | Done 2026-11-26 |
| 3 | Playable board, safe-failure screens, Together mode, Comet bench and Test Track | Done 2026-11-27 |
| 4 | Three bays with repairs, fixtures and story; three vehicles with 54 parts; Workshop Ledger; gear mini-game UI | Done 2026-11-27 |
| 5 | 45 story levels (bot-tuned), 12 solver-proven Blueprints, 9 proven gear puzzles, endless Track Day | Done 2026-11-27 |
| 6 | Synthesised audio, multi-resolution emulator QA, signed APK, `INSTALL.md` | Done on emulators; **physical Fire tablet test pending (parent)** |

## Parent checkpoints

- [ ] Install on the Fire tablet using `INSTALL.md` Path 0, then Path 1 or 2 if needed. Note which path put the app in the child profile and whether Amazon Kids time limits apply to it.
- [ ] Watch him play the first few levels and a Builder Challenge. Note how he reacts to losing, the "closest yet" bar and the hint bulb.
- [ ] Try Together mode with a grandparent.
- [ ] Back up `~/.android-keys/brickyard-garage.jks` and `~/.gradle/gradle.properties` to the TrueNAS.

## Possible next steps (not started; for the parent to choose)

- Tune difficulty from what you see on the tablet. The bot bands are a starting point, not his experience.
- More content: a fourth bay, or more Blueprints and gear puzzles. The forge and verifier make this quick.
- Voice-over for the conversations.
