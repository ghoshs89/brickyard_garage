package com.brickyard.app.screens;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.brickyard.app.GameView;
import com.brickyard.app.art.Draw;
import com.brickyard.app.art.Pal;
import com.brickyard.app.game.Game;
import com.brickyard.app.ui.Button;
import com.brickyard.app.ui.Screen;
import com.brickyard.app.ui.Ui;
import com.brickyard.engine.cars.GarageCatalog;

import java.util.ArrayList;
import java.util.List;

/**
 * Redesign the bay's three fixtures. Tapping a design previews it in the room behind; owned designs
 * switch for free; buying shows the Workshop Ledger first.
 */
public final class DecorScreen extends Screen implements GameView.Overlay {
    private final HubScreen hub;
    private final GarageCatalog.Bay bay;
    private final List<Button> cards = new ArrayList<>();
    private final List<GarageCatalog.Design> cardDesign = new ArrayList<>();
    private final List<GarageCatalog.Fixture> cardFixture = new ArrayList<>();
    private final Button close, buy, cancel;
    private GarageCatalog.Fixture pendingF;
    private GarageCatalog.Design pendingD;

    public DecorScreen(final Game g, HubScreen hub) {
        super(g);
        this.hub = hub;
        this.bay = g.content.garage.bay(hub.bay);
        for (final GarageCatalog.Fixture f : bay.fixtures)
            for (final GarageCatalog.Design d : f.designs) {
                Button b = new Button(d.name, 0xFFFFFFFF, new Runnable() { @Override public void run() { pick(f, d); } });
                b.textSize = 22;
                cards.add(b);
                cardDesign.add(d);
                cardFixture.add(f);
                buttons.add(b);
            }
        close = new Button("Done", 0xFFBDB6A8, new Runnable() { @Override public void run() { hubReset(); g.pop(); } });
        buy = new Button("Buy it!", Pal.GREEN_OK, new Runnable() { @Override public void run() { confirmBuy(); } });
        cancel = new Button("Not now", 0xFFBDB6A8, new Runnable() { @Override public void run() { pendingD = null; hubReset(); } });
        buttons.add(close);
        buttons.add(buy);
        buttons.add(cancel);
    }

    private void hubReset() {
        hub.preview.clear();
    }

    private void pick(GarageCatalog.Fixture f, GarageCatalog.Design d) {
        if (!g.progress.fixtureUnlocked(f)) return;
        if (g.profile.ownedDesigns.contains(d.id)) {
            g.progress.chooseDesign(f.id, d.id);
            g.save();
            hub.preview.clear();
            g.audio.play("snap_2");
            return;
        }
        // try it in the room first
        hub.preview.clear();
        hub.preview.put(f.id, d.look);
        pendingF = f;
        pendingD = d;
    }

    private void confirmBuy() {
        if (pendingD == null) return;
        if (g.progress.buyDesign(pendingF.id, pendingD.id)) {
            g.save();
            g.audio.play("coin");
            g.audio.play("snap_4");
        }
        pendingD = null;
        hub.preview.clear();
    }

    @Override
    public void layout(float vw, float vh) {
        super.layout(vw, vh);
        float l = vw - 640;
        for (int k = 0; k < cards.size(); k++) cards.get(k).at(l + 30 + (k % 3) * 196, 170 + (k / 3) * 180, 180, 110);
        close.at(vw - 300, vh - 120, 240, 90);
        buy.at(vw / 2 - 30, vh - 150, 250, 100);
        cancel.at(vw / 2 - 300, vh - 150, 250, 100);
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        for (int k = 0; k < cards.size(); k++) {
            GarageCatalog.Design d = cardDesign.get(k);
            GarageCatalog.Fixture f = cardFixture.get(k);
            Button b = cards.get(k);
            boolean unlocked = g.progress.fixtureUnlocked(f);
            boolean owned = g.profile.ownedDesigns.contains(d.id);
            boolean chosen = d.id.equals(g.profile.fixtureChoice.get(f.id));
            b.enabled = unlocked;
            b.visible = pendingD == null;
            b.color = chosen ? 0xFFB9F0B0 : pendingD == d ? 0xFFFFE066 : 0xFFFFFFFF;
            b.badge = !unlocked ? null : owned ? null : String.valueOf(d.price);
        }
        buy.visible = cancel.visible = pendingD != null;
        buy.enabled = pendingD != null && g.profile.coins >= pendingD.price;
        close.visible = pendingD == null;
    }

    @Override
    public void draw(Canvas c) {
        if (pendingD != null) {
            Ui2.ledger(c, g.profile.coins, pendingD.price, vw / 2 - 420, vh - 430, 840);
            Ui.center(c, "Trying: " + pendingD.name + " (" + pendingD.price + " coins)", vw / 2, vh - 450, 32, 0xFFFFFFFF);
            drawButtons(c);
            return;
        }
        float l = vw - 640;
        Ui.panel(c, l, 30, vw - 20, vh - 30, Pal.PANEL);
        Ui.label(c, "Decorate " + bay.name, l + 30, 90, 36, Pal.INK, Paint.Align.LEFT);
        Ui2.topBar(c, g.profile.stars, g.profile.coins, l + 30, 104 - 0);
        for (int k = 0; k < bay.fixtures.size(); k++) {
            GarageCatalog.Fixture f = bay.fixtures.get(k);
            float y = 160 + k * 180;
            Ui.label(c, f.name + (g.progress.fixtureUnlocked(f) ? "" : "  (unlocks with a repair)"), l + 34, y, 24, 0xFF8A6A40, Paint.Align.LEFT);
        }
        drawButtons(c);
        Ui.label(c, "Tap a design to try it in the room. Owned designs switch for free.", l + 30, vh - 60, 20, 0xFF8A8072, Paint.Align.LEFT);
    }
}
