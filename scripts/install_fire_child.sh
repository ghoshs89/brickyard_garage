#!/usr/bin/env bash
# Installs (or updates) Brickyard Garage on a Fire tablet over USB and tries to make it
# available inside each child (Amazon Kids) profile. Progress is kept: this never uninstalls.
#
# Needs: USB debugging on the tablet (see INSTALL.md, Path 2) and adb on this Mac.
set -euo pipefail

PKG="com.brickyard.garage"
HERE="$(cd "$(dirname "$0")/.." && pwd)"
APK="${1:-$HERE/dist/BrickyardGarage.apk}"
ADB="${ADB:-$(command -v adb || echo /opt/homebrew/share/android-commandlinetools/platform-tools/adb)}"

[ -f "$APK" ] || { echo "APK not found: $APK"; exit 1; }
[ -x "$ADB" ] || { echo "adb not found. Install Android platform-tools or set ADB=/path/to/adb"; exit 1; }

echo "== Devices"
"$ADB" devices -l
COUNT=$("$ADB" devices | awk 'NR>1 && $2=="device"' | wc -l | tr -d ' ')
if [ "$COUNT" != "1" ]; then
  echo "Connect exactly one tablet with USB debugging allowed (found $COUNT)."
  exit 1
fi

SDK=$("$ADB" shell getprop ro.build.version.sdk | tr -d '\r')
FIREOS=$("$ADB" shell getprop ro.build.version.name | tr -d '\r')
echo "== Tablet: Android API $SDK, $FIREOS"

echo "== Installing / updating for the parent profile (keeps saved progress)"
"$ADB" install -r "$APK"

echo "== Android users on this tablet"
"$ADB" shell pm list users | tr -d '\r'

# Child profiles are separate Android users. Make the already-installed app available in each.
for U in $("$ADB" shell pm list users | tr -d '\r' | sed -n 's/.*UserInfo{\([0-9]*\):.*/\1/p'); do
  [ "$U" = "0" ] && continue
  echo "== Making the app available to user $U"
  if [ "$SDK" -ge 24 ]; then
    "$ADB" shell cmd package install-existing --user "$U" "$PKG" || echo "   (install-existing refused for user $U)"
  else
    "$ADB" shell pm install-existing --user "$U" "$PKG" 2>/dev/null \
      || "$ADB" shell pm install --user "$U" -r "/data/app/$PKG-1/base.apk" 2>/dev/null \
      || echo "   (could not add to user $U on this Fire OS version; use Add Content in Amazon Kids)"
  fi
done

echo
echo "Done. If Brickyard Garage does not appear in the child's profile, open Amazon Kids in the"
echo "parent profile: Add Content -> Share Content -> Apps -> Brickyard Garage."
echo "Remember to turn USB debugging off afterwards."
