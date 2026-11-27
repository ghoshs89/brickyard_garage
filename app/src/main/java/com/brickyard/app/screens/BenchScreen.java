package com.brickyard.app.screens;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.brickyard.app.art.CarArt;
import com.brickyard.app.art.Draw;
import com.brickyard.app.art.Pal;
import com.brickyard.app.game.Game;
import com.brickyard.app.ui.Button;
import com.brickyard.app.ui.Screen;
import com.brickyard.app.ui.Ui;
import com.brickyard.engine.cars.BrickVehicleModel;
import com.brickyard.engine.cars.GarageCatalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Snap brick parts onto any vehicle, customize racing numbers & stripes, toggle Cross-Fit Brick Mix,
 * compare Speed / Grip / Boost, pick any of the 3 tracks, and launch a Test Drive!
 */
public final class BenchScreen extends Screen {
    private static final int[] NUMBERS = {0, 1, 7, 11, 24, 42, 77, 88, 99};
    private static final String[] STRIPE_NAMES = {"Plain", "Racing", "Checkered", "Lightning"};

    private String vehicle;
    private String slot = "CHASSIS";
    private int trackIdx;
    private int partPage;
    private final List<GarageCatalog.Track> trackList;
    private final List<Button> slotBtns = new ArrayList<>();
    private final List<Button> partBtns = new ArrayList<>();
    private final List<GarageCatalog.Part> visibleParts = new ArrayList<>();
    private final List<GarageCatalog.Part> allSlotParts = new ArrayList<>();
    private final Button back, drive, buy, cancel, mixBtn, numBtn, stripeBtn, trackBtn, prevPage, nextPage;
    private final Button[] carTabs = new Button[3];
    private GarageCatalog.Part focus, pendingBuy;

    public BenchScreen(final Game g, String vehicle) {
        super(g);
        this.vehicle = vehicle;
        this.trackList = new ArrayList<>(g.content.garage.tracks.values());
        this.trackIdx = defaultTrackIndex(vehicle);

        for (final String s : GarageCatalog.SLOTS) {
            Button b = new Button(s.charAt(0) + s.substring(1).toLowerCase(), 0xFFE8C89A, new Runnable() {
                @Override public void run() { slot = s; focus = null; partPage = 0; rebuildParts(); }
            });
            b.textSize = 23;
            b.icon = new Button.Icon() { @Override public void draw(Canvas c, float x, float y, float sz) { CarArt.slotIcon(c, s, x, y, sz, 0xFFE03B2E); } };
            slotBtns.add(b);
            buttons.add(b);
        }
        mixBtn = new Button("Brick Mix", 0xFF9FD3E8, new Runnable() {
            @Override public void run() {
                g.profile.mixParts = !g.profile.mixParts;
                g.save();
                partPage = 0;
                rebuildParts();
            }
        });
        mixBtn.textSize = 22;
        numBtn = new Button("# --", 0xFFE8C89A, new Runnable() { @Override public void run() { cycleNumber(); } });
        numBtn.textSize = 22;
        stripeBtn = new Button("Stripe", 0xFFE8C89A, new Runnable() { @Override public void run() { cycleStripe(); } });
        stripeBtn.textSize = 22;
        trackBtn = new Button("Track", 0xFF9FD3E8, new Runnable() {
            @Override public void run() { trackIdx = (trackIdx + 1) % trackList.size(); }
        });
        trackBtn.textSize = 22;
        prevPage = new Button("◀", 0xFFE8C89A, new Runnable() { @Override public void run() { if (partPage > 0) { partPage--; rebuildParts(); } } });
        nextPage = new Button("▶", 0xFFE8C89A, new Runnable() { @Override public void run() { if ((partPage + 1) * 3 < allSlotParts.size()) { partPage++; rebuildParts(); } } });
        prevPage.textSize = 24;
        nextPage.textSize = 24;

        back = new Button("Garage", 0xFFBDB6A8, new Runnable() { @Override public void run() { g.pop(); } });
        drive = new Button("Test Drive!", 0xFFE03B2E, new Runnable() {
            @Override public void run() {
                GarageCatalog.Track tr = trackList.get(trackIdx);
                g.push(new TrackScreen(g, BenchScreen.this.vehicle, tr.id));
            }
        });
        buy = new Button("Buy it!", Pal.GREEN_OK, new Runnable() { @Override public void run() { confirmBuy(); } });
        cancel = new Button("Not now", 0xFFBDB6A8, new Runnable() { @Override public void run() { pendingBuy = null; } });

        buttons.add(mixBtn);
        buttons.add(numBtn);
        buttons.add(stripeBtn);
        buttons.add(trackBtn);
        buttons.add(prevPage);
        buttons.add(nextPage);
        buttons.add(back);
        buttons.add(drive);
        int k = 0;
        for (final GarageCatalog.Vehicle v : g.content.garage.vehicles.values()) {
            carTabs[k] = new Button(v.name.replace("The ", ""), 0xFFE8C89A, new Runnable() {
                @Override public void run() {
                    BenchScreen.this.vehicle = v.id;
                    trackIdx = defaultTrackIndex(v.id);
                    focus = null;
                    partPage = 0;
                    rebuildParts();
                }
            });
            carTabs[k].textSize = 24;
            buttons.add(carTabs[k]);
            k++;
        }
        buttons.add(buy);
        buttons.add(cancel);
        rebuildParts();
    }

    private int defaultTrackIndex(String veh) {
        for (int i = 0; i < trackList.size(); i++) if (trackList.get(i).vehicle.equals(veh)) return i;
        return 0;
    }

    private void cycleNumber() {
        int cur = g.profile.carNumbers.containsKey(vehicle) ? g.profile.carNumbers.get(vehicle) : 0;
        int next = NUMBERS[0];
        for (int i = 0; i < NUMBERS.length; i++) if (NUMBERS[i] == cur) { next = NUMBERS[(i + 1) % NUMBERS.length]; break; }
        g.profile.carNumbers.put(vehicle, next);
        g.save();
        g.audio.play("snap_3", 0.7f, 1.1f);
    }

    private void cycleStripe() {
        int cur = g.profile.carStripes.containsKey(vehicle) ? g.profile.carStripes.get(vehicle) : 0;
        g.profile.carStripes.put(vehicle, (cur + 1) % STRIPE_NAMES.length);
        g.save();
        g.audio.play("snap_2", 0.7f, 1.2f);
    }

    @Override
    public void enter() {
        g.audio.music("show");
    }

    private void rebuildParts() {
        buttons.removeAll(partBtns);
        partBtns.clear();
        visibleParts.clear();
        allSlotParts.clear();
        if (g.profile.mixParts) {
            // Current car's parts first, then any owned parts from other vehicles for cross-fit brick mixing.
            allSlotParts.addAll(g.content.garage.partsFor(vehicle, slot));
            for (GarageCatalog.Part p : g.content.garage.allPartsForSlot(slot)) {
                if (!p.vehicle.equals(vehicle) && g.profile.ownedParts.contains(p.id)) allSlotParts.add(p);
            }
        } else {
            allSlotParts.addAll(g.content.garage.partsFor(vehicle, slot));
        }
        int maxPage = Math.max(0, (allSlotParts.size() - 1) / 3);
        if (partPage > maxPage) partPage = maxPage;
        int start = partPage * 3, end = Math.min(allSlotParts.size(), start + 3);
        for (int i = start; i < end; i++) {
            final GarageCatalog.Part p = allSlotParts.get(i);
            String label = (!p.vehicle.equals(vehicle) && !p.vehicle.equals("any")) ? p.name + " ★" : p.name;
            Button b = new Button(label, 0xFFFFFFFF, new Runnable() { @Override public void run() { tapPart(p); } });
            b.textSize = 23;
            partBtns.add(b);
            visibleParts.add(p);
        }
        buttons.addAll(buttons.size() - 2, partBtns);
        layout(vw, vh);
    }

    private void tapPart(GarageCatalog.Part p) {
        focus = p;
        if (!g.progress.slotUnlocked(vehicle, slot)) {
            g.audio.play("bounce", 0.6f, 1f);
            return;
        }
        if (g.profile.ownedParts.contains(p.id)) {
            if (g.progress.equip(vehicle, p.id)) {
                g.save();
                g.audio.play("snap_" + (2 + (int) (Math.random() * 3)));
            }
        } else if (p.source.equals("shop")) {
            pendingBuy = p;
        }
    }

    private void confirmBuy() {
        if (pendingBuy == null) return;
        if (g.progress.buyPart(pendingBuy.id)) {
            g.progress.equip(vehicle, pendingBuy.id);
            g.save();
            g.audio.play("coin");
            g.audio.play("snap_4");
        }
        pendingBuy = null;
    }

    @Override
    public void layout(float vw, float vh) {
        super.layout(vw, vh);
        for (int k = 0; k < slotBtns.size(); k++) slotBtns.get(k).at(24, 112 + k * 95, 230, 84);
        mixBtn.at(24, vh - 102, 230, 76);
        for (int k = 0; k < partBtns.size(); k++) partBtns.get(k).at(vw - 400, 158 + k * 114, 370, 96);
        prevPage.at(vw - 400, 504, 80, 54);
        nextPage.at(vw - 110, 504, 80, 54);
        back.at(24, 20, 180, 78);
        float cx = 280, cw = vw - 710;
        numBtn.at(cx, 434, cw * 0.28f - 6, 58);
        stripeBtn.at(cx + cw * 0.28f + 4, 434, cw * 0.34f - 6, 58);
        trackBtn.at(cx + cw * 0.62f + 8, 434, cw * 0.38f - 8, 58);
        drive.at(cx + cw / 2 - 150, vh - 114, 300, 92);
        for (int k = 0; k < 3; k++) if (carTabs[k] != null) carTabs[k].at(cx + k * ((cw - 24) / 3f + 12), 20, (cw - 24) / 3f, 74);
        buy.at(vw / 2 + 16, vh - 126, 250, 96);
        cancel.at(vw / 2 - 266, vh - 126, 250, 96);
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        boolean unlocked = g.progress.slotUnlocked(vehicle, slot);
        Map<String, String> eq = g.progress.equippedFor(vehicle);
        for (int k = 0; k < slotBtns.size(); k++) {
            String s = GarageCatalog.SLOTS[k];
            Button b = slotBtns.get(k);
            boolean slotOpen = g.progress.slotUnlocked(vehicle, s);
            // Keep locked slots tappable so kids can see which repair unlocks them!
            b.enabled = true;
            b.locked = !slotOpen;
            b.color = s.equals(slot) ? 0xFFFFE066 : 0xFFE8C89A;
            b.visible = pendingBuy == null;
        }
        mixBtn.visible = pendingBuy == null;
        mixBtn.label = g.profile.mixParts ? "Brick Mix: ON" : "Brick Mix: OFF";
        mixBtn.color = g.profile.mixParts ? 0xFF7FDBFF : 0xFFBDB6A8;

        int num = g.profile.carNumbers.containsKey(vehicle) ? g.profile.carNumbers.get(vehicle) : 0;
        int stripe = g.profile.carStripes.containsKey(vehicle) ? g.profile.carStripes.get(vehicle) : 0;
        numBtn.label = num == 0 ? "No. --" : "No. " + num;
        stripeBtn.label = STRIPE_NAMES[stripe % STRIPE_NAMES.length];
        trackBtn.label = trackList.get(trackIdx).name.replace(" Strip", "").replace(" Sprint", "");
        numBtn.visible = stripeBtn.visible = trackBtn.visible = pendingBuy == null;

        boolean paged = allSlotParts.size() > 3 && pendingBuy == null;
        prevPage.visible = nextPage.visible = paged;
        prevPage.enabled = partPage > 0;
        nextPage.enabled = (partPage + 1) * 3 < allSlotParts.size();

        for (int k = 0; k < partBtns.size(); k++) {
            GarageCatalog.Part p = visibleParts.get(k);
            Button b = partBtns.get(k);
            boolean owned = g.profile.ownedParts.contains(p.id), on = p.id.equals(eq.get(slot));
            b.visible = pendingBuy == null;
            b.enabled = unlocked && (owned || p.source.equals("shop"));
            b.locked = !unlocked;
            b.color = on ? 0xFFB9F0B0 : focus == p ? 0xFFFFF2B3 : 0xFFFFFFFF;
            b.priceBadge = (!owned && p.source.equals("shop")) ? String.valueOf(p.price) : null;
            b.badge = owned ? null : p.source.equals("blueprint") ? "BP" : p.source.equals("gear") ? "Gear" : null;
        }
        int k = 0;
        for (GarageCatalog.Vehicle v : g.content.garage.vehicles.values()) {
            carTabs[k].enabled = g.progress.slotUnlocked(v.id, "CHASSIS");
            carTabs[k].color = v.id.equals(vehicle) ? 0xFFFFE066 : 0xFFE8C89A;
            carTabs[k].visible = pendingBuy == null;
            k++;
        }
        drive.visible = pendingBuy == null;
        drive.enabled = g.progress.slotUnlocked(vehicle, "WHEELS");
        back.visible = pendingBuy == null;
        buy.visible = cancel.visible = pendingBuy != null;
        buy.enabled = pendingBuy != null && g.profile.coins >= pendingBuy.price;
    }

    @Override
    public void draw(Canvas c) {
        Draw.rrectGrad(c, 0, 0, vw, vh, 0, 0xFF5E6E7A, 0xFF34404A, 0);
        for (float x = 20; x < vw; x += 40) for (float y = 20; y < vh; y += 40) Draw.circle(c, x, y, 3, 0x33000000, 0);
        GarageCatalog.Vehicle v = g.content.garage.vehicles.get(vehicle);
        Map<String, String> eq = new LinkedHashMap<>(g.progress.equippedFor(vehicle));
        float cx = 280, cw = vw - 710;
        int num = g.profile.carNumbers.containsKey(vehicle) ? g.profile.carNumbers.get(vehicle) : 0;
        int stripe = g.profile.carStripes.containsKey(vehicle) ? g.profile.carStripes.get(vehicle) : 0;

        c.drawOval(new android.graphics.RectF(cx + 20, 360, cx + cw - 20, 424), Draw.fill(0xFF2B2B2B));
        CarArt.draw(c, g.content.garage, vehicle, eq, num, stripe, cx + 40, 180, cw - 80, time * 2, time);
        Ui.center(c, v.name, cx + cw / 2, 134, 42, 0xFFFFFFFF);
        Ui.center(c, v.nickname, cx + cw / 2, 168, 24, 0xFFDDE3E8);

        BrickVehicleModel now = new BrickVehicleModel(g.content.garage, vehicle, eq);
        BrickVehicleModel with = now;
        if (focus != null && g.progress.slotUnlocked(vehicle, slot)) {
            Map<String, String> trial = new LinkedHashMap<>(eq);
            trial.put(slot, focus.id);
            with = new BrickVehicleModel(g.content.garage, vehicle, trial);
        }
        float sy = 516;
        Ui.panel(c, cx, sy - 14, cx + cw, sy + 148, Pal.PANEL);
        Ui.statBar(c, "Speed", with.speed, with.speed - now.speed, cx + 26, sy, cw - 52);
        Ui.statBar(c, "Grip", with.grip, with.grip - now.grip, cx + 26, sy + 46, cw - 52);
        Ui.statBar(c, "Boost", with.boost, with.boost - now.boost, cx + 26, sy + 92, cw - 52);

        // parts column
        Ui.panel(c, vw - 420, 104, vw - 12, vh - 114, Pal.PANEL);
        String sname = slot.charAt(0) + slot.substring(1).toLowerCase();
        Ui.label(c, sname + " parts", vw - 400, 142, 28, Pal.INK, Paint.Align.LEFT);
        if (!g.progress.slotUnlocked(vehicle, slot)) {
            String req = unlockRepairName(vehicle, slot);
            Ui.wrap(c, "Locked! Opens with repair:\n" + req, vw - 400, 566, 370, 23, Pal.RED_BAD, Paint.Align.LEFT);
        } else if (focus != null) {
            String d = focus.blurb + "  " + statDeltaString(focus);
            if (!g.profile.ownedParts.contains(focus.id) && !focus.source.equals("shop"))
                d = focus.source.equals("blueprint") ? "Earned by solving a Blueprint Puzzle." : "Earned by finishing a Gear Fix-It.";
            Ui.wrap(c, d, vw - 400, 574, 370, 21, 0xFF5A4A36, Paint.Align.LEFT);
        }
        if (pendingBuy == null) Ui2.topBar(c, g.profile.stars, g.profile.coins, vw - 434, vh - 96);
        drawButtons(c);
        if (pendingBuy != null) {
            c.drawRect(0, 0, vw, vh, Draw.fill(0x77000000));
            Ui2.ledger(c, pendingBuy.name + " (" + pendingBuy.price + " coins)", statDeltaString(pendingBuy),
                    g.profile.coins, pendingBuy.price, vw / 2 - 440, vh - 444, 880);
            buy.draw(c);
            cancel.draw(c);
        }
    }

    private String unlockRepairName(String veh, String targetSlot) {
        for (GarageCatalog.Bay bay : g.content.garage.bays) {
            if (!bay.vehicle.equals(veh)) continue;
            for (GarageCatalog.Repair r : bay.repairs) {
                if (r.unlocksSlot.contains(targetSlot)) return "\"" + r.name + "\" (" + bay.name + ")";
            }
        }
        return "a garage repair";
    }

    private static String statDeltaString(GarageCatalog.Part p) {
        return (delta("Speed", p.speed) + delta("Grip", p.grip) + delta("Boost", p.boost)).trim();
    }

    private static String delta(String n, int d) {
        return d == 0 ? "" : n + (d > 0 ? " +" : " ") + d + "  ";
    }

    @Override
    public boolean onDown(float x, float y) {
        if (pendingBuy != null) {
            if (buy.hit(x, y) || cancel.hit(x, y)) return super.onDown(x, y);
            return true;
        }
        return super.onDown(x, y);
    }
}
