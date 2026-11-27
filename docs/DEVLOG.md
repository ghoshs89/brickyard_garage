# Dev log

Append-only. Newest last. Each entry says who decided: **Parent decision** (with the parent's own words, or "not stated") or **Assistant choice** (with the reason).

## 2026-11-26: plan chosen

- **Parent decision:** build Gemini's *Brickyard Garage* plan as-is. Parent's words: "Use Gemini's as-is". Alternatives offered: ChatGPT's *Lantern House*, Claude's *Sprocket Street Garage*, or a merge of Gemini's design staged into a v1 plus updates.
- Earlier parent answers in the same session that still apply: the player uses an **Amazon Kids profile**; art is **drawn in code**; all four support features wanted (safe failure, coins and prices, puzzle levels, grandparent co-op). All four are in Gemini's plan.
- **Parent decision:** the assistant may accept the Android SDK license agreements on the parent's behalf (answer: "Yes, accept").

## 2026-11-26: toolchain

- **Assistant choice:** Homebrew `openjdk@17` instead of the `temurin@17` cask the plan names. Both are OpenJDK 17 builds; the Temurin cask runs a macOS installer package that needs an administrator password, which the assistant cannot type.
- **Assistant choice:** Gradle wrapper 8.11.1 with AGP 8.7.3 (known-good pair supporting compileSdk 35 and minSdk 22). The Homebrew Gradle is 9.9, newer than this AGP supports.
- Finding: arm64 emulator images exist for API 22, 28 and 30, so Fire OS 5's Android level can be emulated on this Mac after all. All three are installed.

## 2026-11-26: engine rules (Phase 2)

Assistant choices where the plan left a rule open, chosen to match the genre and keep the engine deterministic:
- Engine is compiled at Java 11 language level and avoids streams, `Optional` and `java.time`, because those APIs are missing on API 22 without desugaring.
- Pieces fall straight through holes (cutouts). Pieces blocked by an immovable tile slide diagonally from an upper neighbour that is resting. A thin wall ("Steel Bulkhead") blocks swaps across its edge and, when below a cell, blocks falling across it; the cell under a bulkhead becomes a refill point unless a portal ("Drop Chute") feeds it.
- Clamped pieces match in place: the match removes one clamp layer and the piece stays. Clamped pieces cannot be swapped or fall.
- Adjacent damage: each crate or oil spill next to a match group takes one hit per group.
- Rust Plates clear when the tile above is cleared or when a blast hits the cell.
- Track Baseplates spread to every cell of a match that touches one, and to every cell a power-up hits when it fires from a baseplate cell.
- Assembly Line conveyors are closed loops whose contents step one place after every move-spending turn; matches that result cascade for free.
- Oil spreads by one cell after a move-spending turn that cleared no oil.
- Mechanic Tools cost no move and do not trigger conveyors or oil spread. Tools are disabled on Blueprint Puzzles so a solver-proven "win in N" stays meaningful.
- The victory lap is cosmetic and runs only when the app records animation steps; coins are `baseCoins + 2 * movesLeft` either way.
- Blueprint refill queues are visible strings per column; after a queue runs out, refills are random from the seeded RNG.

## 2026-11-26: content pipeline (Phase 2)

- **Assistant choice: refill damping.** The first bot runs showed 5 to 8 cascade waves per move and many levels won in 5 to 8 moves, which makes wins mostly luck. A random refill that would complete a match on the spot is now re-rolled with probability 0.8 (up to 3 tries; per-level `damping`). Blueprint refill queues are never altered. Cascades still happen, but planning matters more, which suits the plan's process-over-luck aim.
- **Assistant choice:** goal sizes were roughly tripled after the first tuning pass so story levels take 11 to 34 moves (bot-tuned) instead of 5 to 8.
- **Assistant choice:** level move limits are set by `tools tune`: 600 bot runs per level, picking the limit whose bot win rate is closest to the centre of the band (warm-up 75 to 95%, Builder Challenge 40 to 65%). The bot never reads the move counter, so one batch of runs gives the whole win-rate curve. `verifyAllContent` re-checks with 1,000 runs.
- **Assistant choice:** Blueprints are found by `tools forge`, a deterministic seeded search that keeps a board only if the solver proves a win in exactly N moves and not in N-1, noisy half-random play wins under 25%, and only about a third of first moves still win. Witness lines are stored in each file and replayed by the gate.
- **Assistant choice:** a plain gear train's output speed depends only on its first and last gear, so speed puzzles use an empty output **axle** where he chooses the gear.
- **Assistant choice:** story replays pay half coins and never a star (the plan says only that stars are once per level). Repairs cost 1, 2, 3, 4 and 5 stars per bay (15 per bay, 45 total); the plan gives the total but not the split.
- **Assistant choice:** repair order per bay unlocks the car in stages: chassis and wheels, engine, colour, aero, utility. The first design of each fixture is free once its repair is done.
- Gate result on 2026-11-26: PASS, 0 problems, 37 s (45 story levels in band at 1,000 runs each, 12 Blueprints proven, 9 gear puzzles proven, economy invariants hold, 30 Track Day rounds winnable).

## 2026-11-27: Android app, delivery and verification (Phases 3 to 6)

- **Parent decision** (earlier in this session): accept the Android SDK licences on the parent's behalf. Answer: "Yes, accept".
- **Assistant choice:** the app ID is `com.brickyard.garage` (the plan names only the Java package `com.brickyard.app`). It must never change, or updates stop installing over his save.
- **Assistant choice:** the release keystore was created at `~/.android-keys/brickyard-garage.jks` (PKCS12, RSA 4096, 20,000-day validity) with a random password in `~/.gradle/gradle.properties`, as the plan specifies. v1, v2 and v3 signing are on; v1 is needed for Fire OS 5.
- **Assistant choice:** the parent gate is the plan's "2-step multiplication/PIN gate": a multiplication question, then a 4-digit PIN set on the first visit. The PIN step matters because the player can do the multiplication.
- **Assistant choice:** Mechanic Tools are not usable on Blueprints (explained in the engine section). Story replays pay half coins and no star.
- **Assistant choice:** bronze medal times were recalibrated so bronze is reachable once chassis, wheels and engine are fitted; before this, nothing below a full six-slot car could earn one. Silver still needs shop parts and gold the Blueprint or gear reward parts.
- **Assistant choice:** Shop Closing Time counts completed levels and wall-clock minutes per app session. It only triggers after a win or a repair finishes, closes the shop until the next calendar day, and can be reopened from Parent Settings.
- **Assistant choice:** a test-only autopilot, enabled solely by an adb intent extra, plays hint moves, does repairs, retries and logs to logcat tag `BrickyardAuto`. It is used for the full-campaign acceptance run.
- **Assistant choice:** the board is saved at level start and after every settled turn. A kill during an animation resumes from the last settled board, so the interrupted move is rolled back.
- **Assistant choice:** `INSTALL.md` adds a "Path 0" (sideload in the parent profile, then Amazon Kids Share Content) before the plan's two paths. It takes five minutes to test, and nobody has verified whether Amazon Kids accepts sideloaded apps on this tablet; Gemini's claim that it never works is not backed by Amazon documentation.
- Fixes found by emulator screenshots: wrapped text was invisible (a shared Paint was reset while measuring); piece icons were blurry (cache built at 8 px before layout); the back button overlapped the board; the turn banner covered the Blueprint refill queue; Track Day appeared twice after the story.
