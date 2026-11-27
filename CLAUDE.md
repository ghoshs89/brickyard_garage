# Brickyard Garage

Offline match-3 + brick-car building game for one young player who loves cars and brick building (plays on an Amazon Fire tablet in a child profile; model unknown). Scope is `docs/PLAN.md` (Gemini's plan, adopted as-is by the parent on 2026-11-26). Read `docs/HANDOFF.md` first, then `docs/ROADMAP.md`, then `docs/DEVLOG.md`.

## Rules
- Build what `docs/PLAN.md` says. Record any deviation in `docs/DEVLOG.md` with the reason. Log the parent's decisions in their own words, or "not stated" if they gave none.
- Verify before claiming: run the checks below and say what was not verified. Keep emulator results separate from physical Fire tablet results.
- Confirm with the parent before committing or pushing.
- Never use "LEGO" or "Technic" in game text, art or the app name. Say bricks, studs, gear puzzles.
- No network permission, no ads, no purchases, no accounts, no timers on levels, no lives.
- Praise process ("Planned a 2x2 Buggy!"), never traits ("so smart").

## Toolchain
- JDK 17: `export JAVA_HOME=/opt/homebrew/opt/openjdk@17`
- Android SDK: `/opt/homebrew/share/android-commandlinetools` (see `local.properties`)
- Gradle wrapper 8.11.1, AGP 8.7.3, compileSdk 35, minSdk 22.

## Modules
- `engine/`: pure Java 11 language level, no Android imports, no streams/Optional/java.time (API 22). Board, match rules, power-ups, combos, gravity, saves, gear-train and brick-car models.
- `tools/`: JVM tools. Content validator, Monte Carlo difficulty bot, IDDFS blueprint solver, gear verifier, audio synth. `./gradlew verifyAllContent` gates release builds.
- `app/`: Android app (Canvas views, single activity).
- `content/`: levels, blueprints and gear puzzles as JSON, bundled into the APK as assets.

## Checks
```bash
./gradlew :engine:test            # engine unit tests
./gradlew verifyAllContent        # content gate: schema, bot bands, solver proofs, economy
./gradlew :app:assembleRelease    # signed APK (runs the content gate first)
```

## Signing
Keystore at `~/.android-keys/brickyard-garage.jks`. Passwords live in `~/.gradle/gradle.properties` (`BRICKYARD_STORE_PASSWORD`, `BRICKYARD_KEY_PASSWORD`). Never commit either. Losing the key means updates can no longer install over the saved game.
