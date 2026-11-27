# Handoff: verified state on 2026-11-27 (`v1.1.0`)

## Deliverable
- `dist/BrickyardGarage.apk` (`versionName 1.1`, `versionCode 2`): 3,525,663 bytes, SHA-256 `aded02ddd225245ab94cafc1f3553cc3a09a1884fa043294d48c90a61d058e08`.
  - `apksigner verify`: v1, v2 and v3 all verified. Signer certificate SHA-256 `a270bed4…1b533` (identical key to `v1.0.0`, so `v1.1.0` installs over `v1.0.0` keeping all saved progress).
  - `minSdk 22`, `targetSdk 35`, **0 permissions** (`aapt2 dump permissions`).
- `INSTALL.md`: install paths for the Amazon Kids profile, the USB script `scripts/install_fire_child.sh`, save-preserving updates, and key backup.

## Automated checks (run on 2026-11-27 for `v1.1.0`)
- `./gradlew :engine:test`: **PASS** (50 tests, 0 failures — includes 6 new regression tests for wall containment, diagonal wall slides, Wrench no-op protection, Magnetic Claw power-up combos, Nitro Track Baseplate spreading, and `BlueprintHint` solver/dead-end detection).
- `./gradlew verifyAllContent`: **PASS**, 0 problems (33.6 s).
  - 45 story levels inside their 1,000-run bot bands (`b3-01`, `b3-03`, `b3-07`, `b3-11`, `b3-13` re-tuned after wall & track-spread fixes).
  - 12 Blueprints proven exact, with random/greedy play winning 0 to 19%.
  - 9 gear puzzles proven solvable.
  - 45 stars exactly finish the garage; no star is awarded twice.
  - Guaranteed coins of 450 cover the 444 needed for one module in every slot.
  - All 3 tracks verified: Repair-1 (`CHASSIS + WHEELS`) builds earn bronze, full starter builds earn bronze, best builds earn gold.
  - 30 of 30 Track Day rounds are winnable.

## Emulator results (Apple-silicon Mac, arm64 images, release APK)

| Emulator | Android | Screen | Verified |
|---|---|---|---|
| firehd10_api30 (`v1.1.0`) | 11 (API 30, Fire OS 8 level) | 1920×1200 hdpi | Clean install + 4-level autopilot run + `adb install -r` update preserving mid-board save. Visual inspection across Hub (neck fix, custom `#7` + stripes, `Levels` & `Manual` buttons), Shop Manual (Power-Ups + 10 Combos), Levels replay grid, Brick Bench (gold coin price pills, locked slot unlock hint, racing numbers/stripes, Brick Mix toggle, track selector), Workshop Ledger (exact 10-disk stacks + stat delta), Test Track (asphalt runoff, Bronze on early build, Turbo flame, Honk, Ghost Car), Gear Fix-Its (meshed teeth, `↻ ×1` badge), Free-Build Gear Pegboard, and Blueprint board (`Undo` button). 0 crashes in logcat. |
| fire7_api28 (`v1.0.0`) | 9 (API 28, Fire OS 7 level) | 1024×600 mdpi | Full 45-level campaign by autopilot, airplane mode launch, Together mode handoff, update-over-save. |
| fire7_api22 (`v1.0.0`) | 5.1.1 (API 22, Fire OS 5 level) | 1024×600 | Install, 3 levels + 2 repairs, mid-cascade kill rollback to settled board. |

## Not verified
- **Anything on a physical Fire tablet.** The model and Fire OS version are unknown.
- **Whether Amazon Kids lists the app** after sideloading (Path 0) or after Live App Testing (Path 1), and whether Kids time limits apply to it.
- **Audio by ear.** The emulators ran with `-no-audio`. All 26 SFX (including the new car horn) and 3 music loops synthesize and bundle cleanly, and `AssetFileDescriptor` handles now close properly, but nobody has listened on hardware yet.
- **Hand-played difficulty.** Bands are measured with a one-move-lookahead bot, which is a conservative proxy.

## Next steps for the parent
1. Back up `~/.android-keys/brickyard-garage.jks` and `~/.gradle/gradle.properties` to safe off-device storage.
2. Install `v1.1.0` using `INSTALL.md` Path 0 (or update over `v1.0.0` without uninstalling) and record what happens with Amazon Kids.
3. Configure player & Together-mode partner names and optional Shop Closing Time in **Parents** settings.
4. Watch a session and note reactions to Blueprint Undo, Ghost Car racing, and the Free-Build Gear Pegboard.
