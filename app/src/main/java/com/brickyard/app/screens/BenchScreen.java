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

/** Snap brick parts onto a vehicle, compare Speed / Grip / Boost, and take it to the Test Track. */
public final class BenchScreen extends Screen {
    private String vehicle;
    private String slot = "CHASSIS";
    private final List<Button> slotBtns = new ArrayList<>();
    private final List<Button> partBtns = new ArrayList<>();
    private final List<GarageCatalog.Part> partList = new ArrayList<>();
    private final Button back, drive, buy, cancel;
    private final Button[] carTabs = new Button[3];
    private GarageCatalog.Part focus, pendingBuy;

    public BenchScreen(final Game g, String vehicle) {
        super(g);
        this.vehicle = vehicle;
        for (final String s : GarageCatalog.SLOTS) {
            Button b = new Button(s.charAt(0) + s.substring(1).toLowerCase(), 0xFFE8C89A, new Runnable() { @Override public void run() { slot = s; focus = null; rebuildParts(); } });
            b.textSize = 24;
            b.icon = new Button.Icon() { @Override public void draw(Canvas c, float x, float y, float sz) { CarArt.slotIcon(c, s, x, y, sz, 0xFFE03B2E); } };
            slotBtns.add(b);
            buttons.add(b);
        }
        back = new Button("Garage", 0xFFBDB6A8, new Runnable() { @Override public void run() { g.pop(); } });
        drive = new Button("Test Drive!", 0xFFE03B2E, new Runnable() { @Override public void run() { g.push(new TrackScreen(g, BenchScreen.this.vehicle)); } });
        buy = new Button("Buy it!", Pal.GREEN_OK, new Runnable() { @Override public void run() { confirmBuy(); } });
        cancel = new Button("Not now", 0xFFBDB6A8, new Runnable() { @Override public void run() { pendingBuy = null; } });
        buttons.add(back);
        buttons.add(drive);
        int k = 0;
        for (final GarageCatalog.Vehicle v : g.content.garage.vehicles.values()) {
            carTabs[k] = new Button(v.name.replace("The ", ""), 0xFFE8C89A, new Runnable() { @Override public void run() { BenchScreen.this.vehicle = v.id; focus = null; rebuildParts(); } });
            carTabs[k].textSize = 24;
            buttons.add(carTabs[k]);
            k++;
        }
        buttons.add(buy);
        buttons.add(cancel);
        rebuildParts();
    }

    @Override
    public void enter() {
        g.audio.music("show");
    }

    private void rebuildParts() {
        buttons.removeAll(partBtns);
        partBtns.clear();
        partList.clear();
        for (final GarageCatalog.Part p : g.content.garage.partsFor(vehicle, slot)) {
            Button b = new Button(p.name, 0xFFFFFFFF, new Runnable() { @Override public void run() { tapPart(p); } });
            b.textSize = 24;
            partBtns.add(b);
            partList.add(p);
        }
        buttons.addAll(buttons.size() - 2, partBtns);
        layout(vw, vh);
    }

    private void tapPart(GarageCatalog.Part p) {
        focus = p;
        if (!g.progress.slotUnlocked(vehicle, slot)) return;
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
        for (int k = 0; k < slotBtns.size(); k++) slotBtns.get(k).at(24, 120 + k * 100, 230, 88);
        for (int k = 0; k < partBtns.size(); k++) partBtns.get(k).at(vw - 400, 160 + k * 120, 370, 100);
        back.at(24, 20, 180, 80);
        drive.at(vw / 2 - 110, vh - 130, 300, 104);
        for (int k = 0; k < 3; k++) if (carTabs[k] != null) carTabs[k].at(vw / 2 - 330 + k * 230, 20, 210, 74);
        buy.at(vw / 2 + 10, vh - 150, 250, 100);
        cancel.at(vw / 2 - 270, vh - 150, 250, 100);
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        boolean unlocked = g.progress.slotUnlocked(vehicle, slot);
        Map<String, String> eq = g.progress.equippedFor(vehicle);
        for (int k = 0; k < slotBtns.size(); k++) {
            String s = GarageCatalog.SLOTS[k];
            Button b = slotBtns.get(k);
            b.enabled = g.progress.slotUnlocked(vehicle, s);
            b.color = s.equals(slot) ? 0xFFFFE066 : 0xFFE8C89A;
            b.visible = pendingBuy == null;
        }
        for (int k = 0; k < partBtns.size(); k++) {
            GarageCatalog.Part p = partList.get(k);
            Button b = partBtns.get(k);
            boolean owned = g.profile.ownedParts.contains(p.id), on = p.id.equals(eq.get(slot));
            b.visible = pendingBuy == null;
            b.enabled = unlocked && (owned || p.source.equals("shop"));
            b.color = on ? 0xFFB9F0B0 : focus == p ? 0xFFFFF2B3 : 0xFFFFFFFF;
            b.badge = owned ? null : p.source.equals("shop") ? String.valueOf(p.price) : p.source.equals("blueprint") ? "BP" : "Gear";
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
        // pegboard backdrop
        for (float x = 20; x < vw; x += 40) for (float y = 20; y < vh; y += 40) Draw.circle(c, x, y, 3, 0x33000000, 0);
        GarageCatalog.Vehicle v = g.content.garage.vehicles.get(vehicle);
        Map<String, String> eq = new LinkedHashMap<>(g.progress.equippedFor(vehicle));
        float cx = 300, cw = vw - 760;
        // turntable
        c.drawOval(new android.graphics.RectF(cx + 20, 470, cx + cw - 20, 560), Draw.fill(0xFF2B2B2B));
        CarArt.draw(c, g.content.garage, vehicle, eq, cx, 200, cw, time * 2, time);
        Ui.center(c, v.name, cx + cw / 2, 150, 46, 0xFFFFFFFF);
        Ui.center(c, v.nickname, cx + cw / 2, 188, 26, 0xFFDDE3E8);
        // stats with a preview of the focused part
        BrickVehicleModel now = new BrickVehicleModel(g.content.garage, vehicle, eq);
        BrickVehicleModel with = now;
        if (focus != null && g.progress.slotUnlocked(vehicle, slot)) {
            Map<String, String> trial = new LinkedHashMap<>(eq);
            trial.put(slot, focus.id);
            with = new BrickVehicleModel(g.content.garage, vehicle, trial);
        }
        float sy = vh - 330;
        Ui.panel(c, cx, sy - 20, cx + cw, sy + 170, Pal.PANEL);
        Ui.statBar(c, "Speed", with.speed, with.speed - now.speed, cx + 30, sy, cw - 60);
        Ui.statBar(c, "Grip", with.grip, with.grip - now.grip, cx + 30, sy + 50, cw - 60);
        Ui.statBar(c, "Boost", with.boost, with.boost - now.boost, cx + 30, sy + 100, cw - 60);
        // parts column
        Ui.panel(c, vw - 420, 110, vw - 10, vh - 160, Pal.PANEL);
        String sname = slot.charAt(0) + slot.substring(1).toLowerCase();
        Ui.label(c, sname + " parts", vw - 400, 146, 30, Pal.INK, Paint.Align.LEFT);
        if (!g.progress.slotUnlocked(vehicle, slot)) Ui.wrap(c, "This slot opens with a repair in " + g.content.garage.bays.get(bayIndex(vehicle)).name + ".", vw - 400, vh - 260, 370, 24, Pal.RED_BAD, Paint.Align.LEFT);
        else if (focus != null) {
            String d = focus.blurb + "  " + delta("Speed", focus.speed) + delta("Grip", focus.grip) + delta("Boost", focus.boost);
            if (!g.profile.ownedParts.contains(focus.id) && !focus.source.equals("shop"))
                d = focus.source.equals("blueprint") ? "Earned by solving a Blueprint Puzzle." : "Earned by finishing a Gear Fix-It.";
            Ui.wrap(c, d, vw - 400, vh - 260, 370, 22, 0xFF5A4A36, Paint.Align.LEFT);
        }
        Ui2.topBar(c, g.profile.stars, g.profile.coins, vw - 440, vh - 100 + 0 * 1);
        drawButtons(c);
        if (pendingBuy != null) {
            c.drawRect(0, 0, vw, vh, Draw.fill(0x66000000));
            Ui2.ledger(c, g.profile.coins, pendingBuy.price, vw / 2 - 420, vh - 430, 840);
            Ui.center(c, pendingBuy.name + ": " + pendingBuy.price + " coins", vw / 2, vh - 450, 32, 0xFFFFFFFF);
            buy.draw(c);
            cancel.draw(c);
        }
    }

    private int bayIndex(String vehicle) {
        for (int k = 0; k < g.content.garage.bays.size(); k++) if (g.content.garage.bays.get(k).vehicle.equals(vehicle)) return k;
        return 0;
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
