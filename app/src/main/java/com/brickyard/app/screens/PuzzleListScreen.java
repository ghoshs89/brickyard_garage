package com.brickyard.app.screens;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.brickyard.app.art.Draw;
import com.brickyard.app.art.Pal;
import com.brickyard.app.game.Game;
import com.brickyard.app.ui.Button;
import com.brickyard.app.ui.Screen;
import com.brickyard.app.ui.Ui;
import com.brickyard.engine.cars.GarageCatalog;
import com.brickyard.engine.gears.GearPuzzle;
import com.brickyard.engine.model.Level;
import com.brickyard.engine.profile.Campaign;

import java.util.ArrayList;
import java.util.List;

/** The 12 Blueprint Puzzles or the 9 Gear Fix-Its, with what unlocks each. */
public final class PuzzleListScreen extends Screen {
    private final boolean blueprints;
    private final List<Button> cards = new ArrayList<>();
    private final List<Campaign.Unlockable> items;
    private final Button back, sandboxBtn;

    public PuzzleListScreen(final Game g, final boolean blueprints) {
        super(g);
        this.blueprints = blueprints;
        items = blueprints ? g.content.campaign.blueprints : g.content.campaign.gears;
        for (final Campaign.Unlockable u : items) {
            String title = blueprints ? g.content.levels.get(u.id).title : g.content.gears.get(u.id).title;
            Button b = new Button(title, blueprints ? 0xFF5B8FD9 : 0xFFB58CFF, new Runnable() {
                @Override public void run() {
                    if (blueprints) g.push(new BriefingScreen(g, g.content.levels.get(u.id)));
                    else g.push(new GearScreen(g, g.content.gears.get(u.id)));
                }
            });
            b.textSize = 24;
            cards.add(b);
            buttons.add(b);
        }
        back = new Button("Garage", 0xFFBDB6A8, new Runnable() { @Override public void run() { g.pop(); } });
        sandboxBtn = new Button("Free-Build Pegboard", 0xFFFF8A1F, new Runnable() {
            @Override public void run() { g.push(GearScreen.sandbox(g)); }
        });
        sandboxBtn.textSize = 24;
        sandboxBtn.visible = !blueprints;
        buttons.add(back);
        buttons.add(sandboxBtn);
    }

    @Override
    public void enter() {
        g.audio.music("focus");
    }

    @Override
    public void layout(float vw, float vh) {
        super.layout(vw, vh);
        int cols = blueprints ? 4 : 3;
        float cw = Math.min(300, (vw - 120) / cols - 20), ch = 108;
        float x0 = (vw - cols * (cw + 24)) / 2;
        for (int k = 0; k < cards.size(); k++) cards.get(k).at(x0 + (k % cols) * (cw + 24), 154 + (k / cols) * (ch + 74), cw, ch);
        back.at(24, 20, 180, 80);
        sandboxBtn.at(vw - 320, 20, 296, 80);
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        for (int k = 0; k < items.size(); k++) {
            Campaign.Unlockable u = items.get(k);
            boolean open = blueprints ? g.progress.blueprintUnlocked(u.id) : g.progress.gearUnlocked(u.id);
            cards.get(k).enabled = open && !g.shopClosed();
            cards.get(k).badge = g.profile.won.contains(u.id) ? "✓" : null;
        }
    }

    @Override
    public void draw(Canvas c) {
        int bg = blueprints ? 0xFF1F3B5C : 0xFF4A3B5C;
        Draw.rrectGrad(c, 0, 0, vw, vh, 0, Pal.lighter(bg, 0.15f), bg, 0);
        for (float x = 0; x < vw; x += 40) c.drawLine(x, 0, x, vh, Draw.line(0x22FFFFFF, 1.5f));
        for (float y = 0; y < vh; y += 40) c.drawLine(0, y, vw, y, Draw.line(0x22FFFFFF, 1.5f));
        Ui.label(c, blueprints ? "Blueprint Puzzles" : "Gear Fix-Its", 230, 76, 48, 0xFFFFFFFF, Paint.Align.LEFT);
        Ui.label(c, blueprints ? "See what falls next and win in exactly the moves given." : "Mesh gears to make the machines turn the right way.", 232, 116, 26, 0xFFDDE3E8, Paint.Align.LEFT);
        drawButtons(c);
        for (int k = 0; k < items.size(); k++) {
            Campaign.Unlockable u = items.get(k);
            Button b = cards.get(k);
            boolean open = blueprints ? g.progress.blueprintUnlocked(u.id) : g.progress.gearUnlocked(u.id);
            String sub;
            if (!open) {
                if (blueprints) sub = "Opens after level " + (g.content.campaign.storyIndex(u.unlockAfter) + 1);
                else {
                    GarageCatalog.Repair r = g.content.garage.repairs.get(u.unlockAfter);
                    sub = "Opens with: " + (r == null ? "a repair" : r.name);
                }
            } else if (blueprints) {
                Level L = g.content.levels.get(u.id);
                sub = "Win in exactly " + L.exactMoves;
            } else {
                GearPuzzle gp = g.content.gears.get(u.id);
                sub = "Bay " + gp.bay;
            }
            Ui.wrap(c, sub, b.r.centerX(), b.r.bottom + 36, b.r.width() + 10, 20, 0xFFFFFFFF, Paint.Align.CENTER);
        }
    }
}
