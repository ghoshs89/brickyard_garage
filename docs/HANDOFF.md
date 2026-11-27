# Handoff: verified state on 2026-11-27

## Deliverable
- `dist/BrickyardGarage.apk`: 3,489,935 bytes, SHA-256 `b799d3d8b82414cfbce5f5c4dc5432cd56d70ad3a3d1c7d7061f0f305e72532d`.
  - `apksigner verify`: v1, v2 and v3 all verified. Signer certificate SHA-256 `a270bed4…1b533`.
  - minSdk 22, targetSdk 35, **0 permissions** (`aapt2 dump permissions`).
- `INSTALL.md`: install paths for the Amazon Kids profile, the USB script `scripts/install_fire_child.sh`, updates, and key backup.
- The repo is **not committed yet**. All files are untracked, and the parent's rules say to confirm before committing.

## Automated checks (run on 2026-11-27)
- `./gradlew :engine:test`: 44 tests, 0 failures.
- `./gradlew verifyAllContent`: PASS, 0 problems.
  - 45 story levels inside their bot bands at 1,000 runs each.
  - 12 Blueprints proven exact, with random/greedy play winning 0 to 19%.
  - 9 gear puzzles proven solvable.
  - 45 stars exactly finish the garage; no star is awarded twice.
  - Guaranteed coins of 450 cover the 444 needed for one module in every slot.
  - Starter builds earn bronze and the best builds earn gold.
  - 30 of 30 Track Day rounds are winnable.

## Emulator results (Apple-silicon Mac, arm64 images, release APK)

| Emulator | Android | Screen | Verified |
|---|---|---|---|
| fire7_api28 | 9 (API 28, Fire OS 7 level) | 1024×600 mdpi | Launch in airplane mode. Intro conversation. Level 1 swipe match. Update installed over a saved board, resuming exactly (11 moves, 37 reds). **Full campaign by autopilot:** 45/45 levels won (19 retries), 45 stars earned, 15/15 repairs, 0 stars left over, finale shown, Track Day unlocked, 904 coins, in 484 s at 6× speed. Blueprint in Together mode with the turn handoff. Finished bays render. |
| firehd10_api30 | 11 (API 30, Fire OS 8 level) | 1920×1200 hdpi | Hub, Brick Bench, Workshop Ledger purchase, Test Track run and results table, gear puzzle solved by drag and drop, Blueprint briefing and board, Together banner, parent gate (maths, then PIN), settings menu, Shop Closing Time triggering after 5 levels. |
| fire7_api22 | 5.1.1 (API 22, Fire OS 5 level) | 1024×600 | Install, 3 levels and 2 repairs by autopilot, a kill during a cascade rolling back to the last settled board. |

## Not verified
- **Anything on a physical Fire tablet.** The model and Fire OS version are unknown.
- **Whether Amazon Kids lists the app** after sideloading (Path 0) or after Live App Testing (Path 1), and whether Kids time limits apply to it.
- **Audio by ear.** The emulators ran with `-no-audio`. The sound files are generated and bundled, and the code paths run, but nobody has listened to them.
- **Hand-played difficulty.** Bands are measured with a one-move-lookahead bot, which is a conservative proxy, not the real player.

## Next steps for the parent
1. Back up `~/.android-keys/brickyard-garage.jks` and `~/.gradle/gradle.properties` to the TrueNAS.
2. Install with `INSTALL.md` Path 0 and record what happens with Amazon Kids.
3. Watch a session and note his reaction to losses, the "closest yet" bar and the hint bulb.
4. Decide whether to commit the repo, and whether to push it to a private GitHub repository.
