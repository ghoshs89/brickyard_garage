package com.brickyard.app.game;

import android.util.Log;

import com.brickyard.app.screens.ClosingScreen;
import com.brickyard.app.screens.HubScreen;
import com.brickyard.app.screens.PlayScreen;
import com.brickyard.app.screens.ResultScreen;
import com.brickyard.app.ui.Screen;
import com.brickyard.engine.cars.GarageCatalog;

/**
 * Test autopilot for scripted emulator runs only (enabled by an adb intent extra, never by a player).
 * It plays the hint move, retries losses, does repairs as soon as they are affordable, skips
 * conversations, and logs every result to logcat under the tag "BrickyardAuto".
 */
public final class Autopilot {
    public static final String TAG = "BrickyardAuto";
    private final Game g;
    private final int maxWins;
    private float cooldown;
    private int wins, losses;
    private boolean done;

    public Autopilot(Game g, int maxWins) {
        this.g = g;
        this.maxWins = maxWins;
        Log.i(TAG, "autopilot on, stop after " + maxWins + " new wins");
    }

    public void tick(float dt) {
        cooldown -= dt;
        if (cooldown > 0 || done) return;
        cooldown = 0.12f;
        if (g.talking()) { g.skipLines(); return; }
        Screen top = g.top();
        if (top instanceof PlayScreen) { ((PlayScreen) top).autoStep(); return; }
        if (top instanceof ResultScreen) {
            ResultScreen rs = (ResultScreen) top;
            if (rs.won()) wins++; else losses++;
            Log.i(TAG, (rs.won() ? "WON " : "LOST ") + rs.levelId() + " stars=" + g.profile.stars + " earned=" + g.profile.starsEarned
                    + " coins=" + g.profile.coins + " repairs=" + g.profile.repairs.size());
            rs.autoContinue();
            cooldown = 0.5f;
            return;
        }
        if (top instanceof ClosingScreen) {
            Log.i(TAG, "CLOSING TIME shown after " + wins + " wins (mode " + g.profile.closingTime + ", closed=" + g.shopClosed() + ")");
            g.reopenShop();
            g.pop();
            return;
        }
        if (!(top instanceof HubScreen)) { g.home(); return; }
        for (GarageCatalog.Repair r : g.content.garage.repairs.values()) {
            if (g.progress.repairAvailable(r) && g.profile.stars >= r.stars) {
                g.progress.doRepair(r.id);
                g.save();
                Log.i(TAG, "REPAIR " + r.id + " stars left=" + g.profile.stars);
                if (g.progress.garageComplete()) {
                    Log.i(TAG, "FINALE reached: garage complete, track day unlocked=" + g.progress.trackDayUnlocked());
                    g.say(Story.finale(g.profile.playerName), null);
                }
                return;
            }
        }
        String next = g.progress.nextStoryLevel();
        if (next == null || wins >= maxWins) {
            done = true;
            Log.i(TAG, "AUTOPILOT DONE wins=" + wins + " losses=" + losses + " earned=" + g.profile.starsEarned
                    + " stars=" + g.profile.stars + " repairs=" + g.profile.repairs.size() + " coins=" + g.profile.coins
                    + " storyComplete=" + g.progress.storyComplete() + " garageComplete=" + g.progress.garageComplete());
            return;
        }
        if (g.profile.inProgress != null) g.resumeSaved();
        else g.startLevel(g.level(next), false);
    }
}
