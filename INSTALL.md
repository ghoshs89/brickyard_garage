# Installing Brickyard Garage on a Fire tablet

The app is `dist/BrickyardGarage.apk`. It has **no permissions at all**: no internet, no accounts, no ads, no purchases. It runs fully offline.

The player uses an **Amazon Kids** profile, which is why this guide is longer than "tap the APK". Getting an app into a Kids profile is the part nobody has confirmed on your tablet yet, so the guide starts with a 5-minute check.

> **What is verified and what is not (as of 2026-11-27)**
> - Verified on emulators: the APK installs, launches in airplane mode, keeps progress across an update, and has a valid v1/v2/v3 signature.
> - **Not verified on a real Fire tablet yet:** every step involving Amazon Kids. Amazon's help pages don't say whether a sideloaded app can be shared into a Kids profile. Forum reports conflict: some say it works, some say the app shows greyed out. Gemini's plan says it never works. Treat all three paths below as things to try, in order, and note what happens.

---

## Before you start: find the Fire OS version

On the tablet, in **your** (parent) profile: **Settings → Device Options → About Fire Tablet**. Note the Fire OS version (for example 5.x, 7.x or 8.x). The game supports all of them (Android 5.1 and up).

---

## Path 0 (try first, about 5 minutes): sideload in the parent profile, then share to Kids

1. In the **parent** profile: **Settings → Security & Privacy → Apps from Unknown Sources**. Allow it for the **Files** app (or **Silk Browser**, if you'll download it).
2. Get `BrickyardGarage.apk` onto the tablet:
   - **USB:** plug into the Mac and copy it into the tablet's `Download` folder (the free *Android File Transfer* app or OpenMTP works on macOS).
   - **Or** email or cloud-drive it to yourself and download it in Silk.
3. Open **Files → Downloads**, tap the APK, then tap **Install**.
4. Open the app once in the parent profile to check it runs.
5. Share it into the child.s profile: open **Amazon Kids** (or Parent Dashboard on the tablet), choose his profile, then **Add Content → Share Content → Apps**. Look for **Brickyard Garage**.
6. Switch to his profile and check it appears and opens.
7. Turn **Apps from Unknown Sources** back off.

If Brickyard Garage is listed and opens in the child.s profile, you're done. Amazon Kids daily time limits should then apply to it like any shared app; confirm that once as well.

If it isn't listed, or it's greyed out, use Path 1 or Path 2.

---

## Path 1 (recommended by Gemini's plan): Amazon Appstore Live App Testing

Live App Testing (LAT) is free. It delivers a private build through the Amazon Appstore to testers you invite by email, with no public listing. Because the app then arrives through the Appstore, it is the route most likely to appear in **Add Content → Apps** and to count toward Amazon Kids time limits. That is still **unconfirmed** for Kids profiles; Amazon's documentation mentions child profiles only for testing purchases.

1. Create a free account at <https://developer.amazon.com> (with the Amazon account that owns the tablet, or any account you prefer).
2. **Apps & Services → My Apps → Add New App → Android**. Give it a title (Brickyard Garage) and a category (Games → Puzzle).
3. Upload `dist/BrickyardGarage.apk` under **App Files**. Amazon will list supported devices; Fire tablets should be ticked.
4. Fill in only what Live App Testing requires (a short description and a few screenshots; `docs/screenshots/` has some you can use). You do **not** need to submit for public release.
5. Open **Live App Testing**, create a test, and add your household Amazon email as a tester. Start the test.
6. On the tablet, in the parent profile, accept the invitation in the **Amazon Appstore** (look under *Live App Testing* or the email link) and install it.
7. Share it to the child.s profile: **Add Content → Share Content → Apps**.

**Updates:** upload the new APK to the same LAT test. The Appstore updates the app and his progress stays, because the signing key is the same.

---

## Path 2: USB cable with ADB (fallback; always works for the parent profile)

This is Amazon's documented developer installation method. The script also tries to install the app directly into the child profile's Android user.

1. On the tablet: **Settings → Device Options → About Fire Tablet**. Tap **Serial Number** 7 times to turn on Developer Options.
2. **Settings → Device Options → Developer Options → USB debugging: On**.
3. Connect the USB cable and accept the "Allow USB debugging?" prompt on the tablet.
4. On the Mac, from this folder:

   ```bash
   scripts/install_fire_child.sh
   ```

   The script:
   - installs or updates the APK in the parent profile (`adb install -r`, which keeps progress);
   - lists the tablet's Android users so you can see which is the child.s profile;
   - tries `install-existing` for each child user, which makes the app available inside that profile without a second copy.

5. If the app still doesn't show in his profile, add it with **Add Content → Share Content → Apps** (from Path 0), then turn USB debugging back off.

> **Screen time with Path 2.** If Amazon Kids doesn't track a sideloaded app, use the game's own **Shop Closing Time**:
> - Find it at **Parents** (top right of the garage) → answer the maths question → enter the PIN.
> - Choose 20 minutes, 30 minutes, or 5 levels.
> - The game finishes the current level first, then Grandpa Bo closes the shop until tomorrow.
> - You can reopen the shop from the same screen.

---

## Updating later (keeps all progress)

Every future version is signed with the same private key, so it installs **over** the old one and keeps stars, coins, repairs, cars and any in-progress board:

- Path 0: install the new APK the same way; Android offers **Update**.
- Path 1: upload the new APK to the same Live App Testing test.
- Path 2: run `scripts/install_fire_child.sh` again.

Never uninstall first: uninstalling deletes the saved game. To be extra safe, use **Parents → Export backup** before updating. That writes `brickyard-backup.json` into the app's own folder on the tablet, which you can copy off over USB.

---

## The signing key (please back this up)

- Keystore: `~/.android-keys/brickyard-garage.jks`
- Passwords: `~/.gradle/gradle.properties` (`BRICKYARD_STORE_PASSWORD`, `BRICKYARD_KEY_PASSWORD`)

Copy **both** to the TrueNAS (or another safe place). If the key is lost, no future update can install over the saved game; it would have to be uninstalled, which wipes the progress.

---

## First-time parent setup in the game

1. Tap **Parents** (top right of the garage).
2. Answer the multiplication question.
3. Choose a 4-digit PIN. A child who knows the times tables still needs the PIN.
4. Set **Together partner** ((pick from the presets or type a name)), **Shop Closing Time**, and sound and music.
