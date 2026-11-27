package com.brickyard.app.screens;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.brickyard.app.GameView;
import com.brickyard.app.art.CarArt;
import com.brickyard.app.art.Draw;
import com.brickyard.app.art.Pal;
import com.brickyard.app.game.Game;
import com.brickyard.app.game.Story;
import com.brickyard.app.ui.Button;
import com.brickyard.app.ui.Screen;
import com.brickyard.app.ui.Ui;
import com.brickyard.engine.cars.GarageCatalog;

import java.util.ArrayList;
import java.util.List;

/** Spend stars on the bay's five repairs, in order. Each repair tells a tiny story and unlocks a car slot. */
public final class RepairScreen extends Screen implements GameView.Overlay {
    private final HubScreen hub;
    private final List<Button> doBtns = new ArrayList<>();
    private final Button close;
    private final GarageCatalog.Bay bay;

    public RepairScreen(final Game g, HubScreen hub) {
        super(g);
        this.hub = hub;
        this.bay = g.content.garage.bay(hub.currentBay());
        hub.bay = bay.number;
        for (final GarageCatalog.Repair r : bay.repairs) {
            Button b = new Button("Repair", Pal.GREEN_OK, new Runnable() { @Override public void run() { repair(r); } });
            b.textSize = 28;
            doBtns.add(b);
            buttons.add(b);
        }
        close = new Button("Done", 0xFFBDB6A8, new Runnable() { @Override public void run() { g.pop(); } });
        buttons.add(close);
    }

    private void repair(final GarageCatalog.Repair r) {
        if (!g.progress.doRepair(r.id)) return;
        g.save();
        g.audio.play("ratchet");
        g.audio.play("star", 0.6f, 1f);
        final int bayNo = r.bay;
        final boolean bayDone = g.progress.bayComplete(bayNo);
        List<Story.Line> lines = Story.repair(r.story);
        if (!r.unlocksSlot.isEmpty())
            lines.add(new Story.Line(1, 3, "New on the Brick Bench: " + r.unlocksSlot.replace(",", " and ").toLowerCase() + " for " + g.content.garage.vehicles.get(bay.vehicle).name + "!"));
        if (bayDone) lines.addAll(Story.bayDone(bay.outro, bayNo));
        if (bayDone && bayNo == 3) lines.addAll(Story.finale(g.profile.playerName));
        g.pop();
        g.say(lines, new Runnable() {
            @Override public void run() {
                if (bayDone && bayNo < 3) { hub.bay = bayNo + 1; }
                g.checkClosingTime();
            }
        });
    }

    @Override
    public void layout(float vw, float vh) {
        super.layout(vw, vh);
        float l = vw / 2 - 560;
        for (int k = 0; k < doBtns.size(); k++) doBtns.get(k).at(l + 860, 168 + k * 104, 220, 84);
        close.at(vw / 2 - 120, vh - 120, 240, 90);
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        for (int k = 0; k < bay.repairs.size(); k++) {
            GarageCatalog.Repair r = bay.repairs.get(k);
            Button b = doBtns.get(k);
            boolean done = g.progress.repairDone(r.id), avail = g.progress.repairAvailable(r);
            b.visible = !done && avail;
            b.enabled = g.profile.stars >= r.stars;
            b.label = b.enabled ? "Repair!" : "Need " + (r.stars - g.profile.stars) + " more";
        }
    }

    @Override
    public void draw(Canvas c) {
        c.drawRect(0, 0, vw, vh, Draw.fill(0x88000000));
        float l = vw / 2 - 560, r = vw / 2 + 560;
        Ui.panel(c, l, 30, r, vh - 30, Pal.PANEL);
        Ui.label(c, bay.name + ": repairs", l + 40, 96, 44, Pal.INK, Paint.Align.LEFT);
        Draw.star(c, r - 170, 82, 26, Pal.GOLD, 3);
        Ui.label(c, g.profile.stars + " to spend", r - 136, 96, 34, Pal.INK, Paint.Align.LEFT);
        for (int k = 0; k < bay.repairs.size(); k++) {
            GarageCatalog.Repair rep = bay.repairs.get(k);
            float y = 160 + k * 104;
            boolean done = g.progress.repairDone(rep.id), avail = g.progress.repairAvailable(rep);
            Draw.rrect(c, l + 30, y, r - 30, y + 96, 18, done ? 0xFFE3F4E1 : avail ? 0xFFFFFFFF : 0xFFEDE6D6, 3);
            for (int s = 0; s < rep.stars; s++) Draw.star(c, l + 66 + s * 34, y + 48, 15, done ? 0xFFBDB6A8 : Pal.GOLD, 2.5f);
            Ui.label(c, rep.name, l + 250, y + 42, 30, done || avail ? Pal.INK : 0xFF9A9284, Paint.Align.LEFT);
            Ui.label(c, rep.desc, l + 250, y + 76, 22, 0xFF8A8072, Paint.Align.LEFT);
            if (!rep.unlocksSlot.isEmpty()) {
                String[] slots = rep.unlocksSlot.split(",");
                for (int j = 0; j < slots.length; j++) CarArt.slotIcon(c, slots[j].trim(), l + 760 + j * 46, y + 26, 44, 0xFFE03B2E);
            }
            if (done) Ui2.checkmark(c, l + 900, y + 26, 44);
            else if (!avail) Ui.label(c, "Locked", l + 970, y + 58, 26, 0xFF9A9284, Paint.Align.CENTER);
        }
        drawButtons(c);
    }
}
