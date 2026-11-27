package com.brickyard.app.screens;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.brickyard.app.art.Draw;
import com.brickyard.app.art.Pal;
import com.brickyard.app.game.Game;
import com.brickyard.app.ui.Button;
import com.brickyard.app.ui.Screen;
import com.brickyard.app.ui.Ui;
import com.brickyard.engine.model.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Story Level Picker: replay any unlocked level across all 3 bays in Solo or Together Mode.
 */
public final class LevelSelectScreen extends Screen {
    private int bay;
    private final Button back;
    private final Button[] bayTabs = new Button[3];
    private final List<Button> cards = new ArrayList<>();
    private final List<String> levelIds = new ArrayList<>();

    public LevelSelectScreen(final Game g, int initialBay) {
        super(g);
        this.bay = Math.max(1, Math.min(3, initialBay));
        back = new Button("Garage", 0xFFBDB6A8, new Runnable() { @Override public void run() { g.pop(); } });
        buttons.add(back);
        for (int k = 0; k < 3; k++) {
            final int n = k + 1;
            bayTabs[k] = new Button("Bay " + n, 0xFFE8C89A, new Runnable() {
                @Override public void run() { bay = n; rebuildCards(); }
            });
            bayTabs[k].textSize = 26;
            buttons.add(bayTabs[k]);
        }
        rebuildCards();
    }

    private void rebuildCards() {
        buttons.removeAll(cards);
        cards.clear();
        levelIds.clear();
        int start = (bay - 1) * 15;
        for (int i = 0; i < 15 && start + i < g.content.campaign.story.size(); i++) {
            final String id = g.content.campaign.story.get(start + i);
            final Level L = g.content.levels.get(id);
            final int num = start + i + 1;
            Button b = new Button("Level " + num, L.difficulty == Level.Difficulty.CHALLENGE ? 0xFFFF8A1F : Pal.GREEN_OK, new Runnable() {
                @Override public void run() { g.push(new BriefingScreen(g, L)); }
            });
            b.textSize = 26;
            cards.add(b);
            levelIds.add(id);
            buttons.add(b);
        }
        layout(vw, vh);
    }

    @Override
    public void layout(float vw, float vh) {
        super.layout(vw, vh);
        back.at(24, 20, 180, 78);
        for (int k = 0; k < 3; k++) bayTabs[k].at(vw / 2 - 260 + k * 180, 20, 165, 72);
        int cols = 5;
        float cw = Math.min(220, (vw - 120) / cols - 20), ch = 122;
        float x0 = (vw - cols * (cw + 20) + 20) / 2;
        for (int k = 0; k < cards.size(); k++) {
            cards.get(k).at(x0 + (k % cols) * (cw + 20), 152 + (k / cols) * (ch + 62), cw, ch);
        }
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        for (int k = 0; k < 3; k++) bayTabs[k].color = (bay == k + 1) ? 0xFFFFE066 : 0xFFE8C89A;
        for (int k = 0; k < cards.size(); k++) {
            String id = levelIds.get(k);
            boolean unlocked = g.progress.storyUnlocked(id);
            boolean won = g.profile.won.contains(id);
            Button b = cards.get(k);
            b.enabled = unlocked && !g.shopClosed();
            b.locked = !unlocked;
            b.badge = won ? "★" : null;
        }
    }

    @Override
    public void draw(Canvas c) {
        int bg = bay == 2 ? 0xFF4F6B57 : bay == 3 ? 0xFF3E5566 : 0xFF7A4A38;
        Draw.rrectGrad(c, 0, 0, vw, vh, 0, Pal.lighter(bg, 0.14f), bg, 0);
        Ui.label(c, g.content.garage.bay(bay).name + " — Replay Levels", 230, 126, 30, 0xFFFDFBF5, Paint.Align.LEFT);
        drawButtons(c);
        for (int k = 0; k < cards.size(); k++) {
            Level L = g.content.levels.get(levelIds.get(k));
            Button b = cards.get(k);
            Ui.wrap(c, L.title, b.r.centerX(), b.r.bottom + 32, b.r.width() + 10, 19, 0xFFFDFBF5, Paint.Align.CENTER);
        }
    }
}
