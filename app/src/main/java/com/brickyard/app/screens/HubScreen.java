package com.brickyard.app.screens;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.brickyard.app.art.CarArt;
import com.brickyard.app.art.CharacterArt;
import com.brickyard.app.art.Draw;
import com.brickyard.app.art.GarageArt;
import com.brickyard.app.art.Pal;
import com.brickyard.app.game.Game;
import com.brickyard.app.game.Story;
import com.brickyard.app.ui.Button;
import com.brickyard.app.ui.Screen;
import com.brickyard.app.ui.Ui;
import com.brickyard.engine.cars.GarageCatalog;
import com.brickyard.engine.model.Level;

import java.util.HashMap;
import java.util.Map;

/** The garage: the bay being restored, the car on its lift, and every way into the game. */
public final class HubScreen extends Screen {
    public int bay = 1;
    /** DecorScreen previews a design here before it is bought ("try it in the room"). */
    public final Map<String, Integer> preview = new HashMap<>();
    private final Button play, repairs, decorate, bench, blueprints, gears, trackDay, parents;
    private final Button[] bayTabs = new Button[3];
    private float sceneX, sceneY, sceneS;
    private boolean userPickedBay;

    public HubScreen(final Game g) {
        super(g);
        play = new Button("Play", Pal.GREEN_OK, new Runnable() { @Override public void run() { playNext(); } });
        play.textSize = 40;
        repairs = new Button("Repairs", Pal.GOLD, new Runnable() { @Override public void run() { g.push(new RepairScreen(g, HubScreen.this)); } });
        repairs.icon = new Button.Icon() { @Override public void draw(Canvas c, float x, float y, float s) { Draw.star(c, x + s / 2, y + s / 2, s / 2, Pal.GOLD, 3); } };
        decorate = new Button("Decorate", 0xFF9FD3E8, new Runnable() { @Override public void run() { g.push(new DecorScreen(g, HubScreen.this)); } });
        bench = new Button("Brick Bench", 0xFFFF8A1F, new Runnable() { @Override public void run() { g.push(new BenchScreen(g, vehicleFor(bay))); } });
        blueprints = new Button("Blueprints", 0xFF5B8FD9, new Runnable() { @Override public void run() { g.push(new PuzzleListScreen(g, true)); } });
        gears = new Button("Gear Fix-Its", 0xFFB58CFF, new Runnable() { @Override public void run() { g.push(new PuzzleListScreen(g, false)); } });
        trackDay = new Button("Track Day", 0xFFE03B2E, new Runnable() {
            @Override public void run() { g.push(new BriefingScreen(g, g.level("trackday-" + g.profile.trackDayRound))); }
        });
        parents = new Button("Parents", 0xFFBDB6A8, new Runnable() { @Override public void run() { g.push(new SettingsScreen(g)); } });
        parents.textSize = 24;
        for (Button b : new Button[]{play, repairs, decorate, bench, blueprints, gears, trackDay, parents}) buttons.add(b);
        for (int k = 0; k < 3; k++) {
            final int n = k + 1;
            bayTabs[k] = new Button("Bay " + n, 0xFFE8C89A, new Runnable() { @Override public void run() { bay = n; userPickedBay = true; bayIntroIfNew(); } });
            bayTabs[k].textSize = 26;
            buttons.add(bayTabs[k]);
        }
    }

    @Override
    public void enter() {
        g.audio.music("workshop");
        if (!userPickedBay) bay = currentBay();
        if (!g.profile.seen.contains("intro")) {
            g.profile.seen.add("intro");
            g.save();
            g.say(Story.intro(g.profile.playerName), new Runnable() { @Override public void run() { bayIntroIfNew(); } });
        } else bayIntroIfNew();
    }

    private void bayIntroIfNew() {
        String key = "bay" + bay + "-intro";
        if (g.profile.seen.contains(key)) return;
        g.profile.seen.add(key);
        g.save();
        g.say(Story.bayIntro(bay, g.content.garage.bay(bay).intro), null);
    }

    /** The bay he is working on: the first one with unfinished repairs (never ahead of the story). */
    int currentBay() {
        for (int b = 1; b <= 3; b++) if (!g.progress.bayComplete(b)) return b;
        return 3;
    }

    boolean bayOpen(int b) {
        return b == 1 || g.progress.bayComplete(b - 1) || g.profile.debugUnlock;
    }

    String vehicleFor(int b) {
        return g.content.garage.bay(b).vehicle;
    }

    private void playNext() {
        if (g.profile.inProgress != null) {
            String id = (String) g.profile.inProgress.get("level");
            Level L = g.level(id);
            if (L != null) {
                // resume the saved board
                g.push(new ContinueScreen(g, L));
                return;
            }
        }
        String next = g.progress.nextStoryLevel();
        if (next == null) { g.push(new BriefingScreen(g, g.level("trackday-" + g.profile.trackDayRound))); return; }
        g.push(new BriefingScreen(g, g.level(next)));
    }

    @Override
    public void layout(float vw, float vh) {
        super.layout(vw, vh);
        float colW = 360, colX = vw - colW - 30;
        float areaW = vw - colW - 80, areaH = vh - 120;
        sceneS = Math.min(areaW / 1000f, areaH / 560f);
        sceneX = 30 + (areaW - 1000 * sceneS) / 2;
        sceneY = 100 + (areaH - 560 * sceneS) / 2;
        play.at(colX, 110, colW, 130);
        repairs.at(colX, 262, colW, 92);
        decorate.at(colX, 368, colW / 2 - 8, 92);
        bench.at(colX + colW / 2 + 8, 368, colW / 2 - 8, 92);
        blueprints.at(colX, 474, colW / 2 - 8, 92);
        gears.at(colX + colW / 2 + 8, 474, colW / 2 - 8, 92);
        trackDay.at(colX, 580, colW, 92);
        parents.at(vw - 170, 14, 150, 70);
        for (int k = 0; k < 3; k++) bayTabs[k].at(470 + k * 160, 18, 150, 66);
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        boolean closed = g.shopClosed();
        String next = g.progress.nextStoryLevel();
        if (g.profile.inProgress != null) play.label = "Continue";
        else if (next == null) play.label = "Track Day";
        else play.label = "Play Level " + (g.content.campaign.storyIndex(next) + 1);
        play.enabled = !closed;
        trackDay.visible = g.progress.trackDayUnlocked() && next != null; // after the story, the big Play button is Track Day
        trackDay.enabled = !closed;
        blueprints.enabled = !closed;
        gears.enabled = !closed;
        int affordable = 0;
        for (GarageCatalog.Repair r : g.content.garage.repairs.values()) if (g.progress.repairAvailable(r) && g.profile.stars >= r.stars) affordable++;
        repairs.badge = affordable > 0 ? "!" : null;
        repairs.color = affordable > 0 && ((int) (time * 2)) % 2 == 0 ? Pal.lighter(Pal.GOLD, 0.3f) : Pal.GOLD;
        bench.enabled = g.progress.slotUnlocked(vehicleFor(bay), "CHASSIS");
        decorate.enabled = true;
        int bpOpen = 0, gearOpen = 0;
        for (com.brickyard.engine.profile.Campaign.Unlockable u : g.content.campaign.blueprints) if (g.progress.blueprintUnlocked(u.id) && !g.profile.won.contains(u.id)) bpOpen++;
        for (com.brickyard.engine.profile.Campaign.Unlockable u : g.content.campaign.gears) if (g.progress.gearUnlocked(u.id) && !g.profile.won.contains(u.id)) gearOpen++;
        blueprints.badge = bpOpen > 0 ? String.valueOf(bpOpen) : null;
        gears.badge = gearOpen > 0 ? String.valueOf(gearOpen) : null;
        for (int k = 0; k < 3; k++) {
            bayTabs[k].enabled = bayOpen(k + 1);
            bayTabs[k].color = bay == k + 1 ? 0xFFFFE066 : 0xFFE8C89A;
        }
    }

    @Override
    public boolean onDown(float x, float y) {
        if (super.onDown(x, y)) return true;
        // tapping the car opens the Brick Bench
        float cx = sceneX + (GarageArt.LIFT_X) * sceneS, cy = sceneY + GarageArt.LIFT_Y * sceneS;
        if (x > cx && x < cx + GarageArt.LIFT_W * sceneS && y > cy && y < cy + 180 * sceneS && bench.enabled) {
            g.audio.play("rev", 0.6f, 1f);
            g.push(new BenchScreen(g, vehicleFor(bay)));
            return true;
        }
        return false;
    }

    @Override
    public void draw(Canvas c) {
        Draw.rrectGrad(c, 0, 0, vw, vh, 0, 0xFFF3E6C8, 0xFFE2CBA0, 0);
        c.save();
        c.translate(sceneX, sceneY);
        c.scale(sceneS, sceneS);
        Draw.rrect(c, -8, -8, 1008, 568, 16, Pal.INK, 0);
        c.save();
        c.clipRect(0, 0, 1000, 560);
        GarageArt.drawBay(c, bay, g.profile.repairs, new GarageArt.Looks() {
            @Override public int look(String fixtureId) { return lookOf(fixtureId); }
        }, time);
        String v = vehicleFor(bay);
        CarArt.draw(c, g.content.garage, v, g.profile.equipped.get(v), GarageArt.LIFT_X, GarageArt.LIFT_Y - (g.progress.repairDone("b" + bay + "-r2") ? 16 + (float) Math.sin(time) * 3 : 0), GarageArt.LIFT_W, 0, time);
        // the team, peeking in at the bottom
        CharacterArt.draw(c, CharacterArt.BO, 40, 360, 200, 0, time);
        CharacterArt.draw(c, CharacterArt.SPROCKET, 210, 420, 140, 0, time + 1);
        CharacterArt.draw(c, CharacterArt.JUNO, 820, 360, 200, 0, time + 2);
        if (!bayOpen(bay)) {
            c.drawRect(0, 0, 1000, 560, Draw.fill(0xAA000000));
            Ui.center(c, "Finish Bay " + (bay - 1) + " to open this bay", 500, 290, 40, 0xFFFFFFFF);
        }
        c.restore();
        c.restore();
        Ui2.topBar(c, g.profile.stars, g.profile.coins, 24, 18);
        Ui.label(c, g.content.garage.bay(bay).name, sceneX, sceneY + 560 * sceneS + 40, 30, Pal.INK, Paint.Align.LEFT);
        int done = 0;
        for (GarageCatalog.Repair r : g.content.garage.bay(bay).repairs) if (g.progress.repairDone(r.id)) done++;
        Ui.label(c, done + " of 5 repairs done", sceneX + 1000 * sceneS, sceneY + 560 * sceneS + 40, 26, 0xFF8A6A40, Paint.Align.RIGHT);
        drawButtons(c);
        if (g.shopClosed()) {
            Draw.rrect(c, vw - 400, vh - 110, vw - 30, vh - 20, 20, 0xFF1B1B2F, 4);
            Ui.center(c, "Shop closed until tomorrow", vw - 215, vh - 55, 26, 0xFFFF7AD9);
        }
    }

    int lookOf(String fixtureId) {
        Integer p = preview.get(fixtureId);
        if (p != null) return p;
        GarageCatalog.Fixture f = g.content.garage.fixtures.get(fixtureId);
        if (f == null || !g.progress.fixtureUnlocked(f)) return -1;
        String chosen = g.profile.fixtureChoice.get(fixtureId);
        if (chosen == null) return -1;
        for (GarageCatalog.Design d : f.designs) if (d.id.equals(chosen)) return d.look;
        return 0;
    }

    @Override
    public boolean animating() {
        return true;
    }
}
