# Brickyard Garage

An offline match-3 and brick-car building game for Android and Fire tablets, made for one young player. You win puzzle boards to earn stars, spend stars to restore Grandpa Bo's three garage bays, snap brick parts onto three cars, and test them on a track. There are also Blueprint puzzles where the refills are visible and the move count is exact, gear-train puzzles, and a Together mode for playing with a grandparent.

- **Install:** see [INSTALL.md](INSTALL.md). The APK is `dist/BrickyardGarage.apk`.
- **Scope:** [docs/PLAN.md](docs/PLAN.md). Status: [docs/ROADMAP.md](docs/ROADMAP.md). Decisions: [docs/DEVLOG.md](docs/DEVLOG.md). Latest verified state: [docs/HANDOFF.md](docs/HANDOFF.md).
- **No permissions:** no internet, accounts, ads, purchases, lives or level timers.

## Build

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew :engine:test          # engine unit tests
./gradlew verifyAllContent      # content gate (about a minute)
./gradlew :app:distApk          # signed release APK -> dist/BrickyardGarage.apk (runs the gate first)
```

## Content tools

```bash
python3 scripts/gen/levels.py                     # story levels + campaign (edit the script, not the JSON)
python3 scripts/gen/garage.py                     # bays, repairs, fixtures, vehicles, parts, tracks
python3 scripts/gen/gears.py                      # gear puzzles
./gradlew :tools:run --args="tune content"        # set move limits from 600 bot runs per level
./gradlew :tools:run --args="forge content"       # search for solver-proven Blueprints
./gradlew :tools:run --args="gears content"       # prove gear puzzles, record witnesses
./gradlew :tools:run --args="trace content b1-08" # watch the bot play a level
```

## Emulator test hook

`adb shell am start -n com.brickyard.garage/com.brickyard.app.MainActivity --ez autopilot true --ei autopilotLevels 5 --ef autopilotSpeed 6`

This plays hint moves, repairs, retries and skips conversations, logging to logcat tag `BrickyardAuto`. It is only reachable through an intent extra sent over adb.
