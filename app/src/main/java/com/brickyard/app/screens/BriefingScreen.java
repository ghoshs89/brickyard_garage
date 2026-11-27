package com.brickyard.app.screens;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.brickyard.app.GameView;
import com.brickyard.app.art.CharacterArt;
import com.brickyard.app.art.Draw;
import com.brickyard.app.art.Pal;
import com.brickyard.app.game.Game;
import com.brickyard.app.ui.Button;
import com.brickyard.app.ui.Screen;
import com.brickyard.app.ui.Ui;
import com.brickyard.engine.model.GoalSpec;
import com.brickyard.engine.model.Level;

/** Level start card: goals, moves, Grandpa Bo's Workshop Note, and the Solo / Together toggle. */
public final class BriefingScreen extends Screen implements GameView.Overlay {
    private final Level L;
    private boolean together;
    private final Button play, back, solo, duo;

    public BriefingScreen(final Game g, final Level L) {
        super(g);
        this.L = L;
        play = new Button("Play!", Pal.GREEN_OK, new Runnable() { @Override public void run() { g.pop(); g.startLevel(L, together); } });
        back = new Button("Back", 0xFFBDB6A8, new Runnable() { @Override public void run() { g.pop(); } });
        solo = new Button("Solo", 0xFFFF8A1F, new Runnable() { @Override public void run() { together = false; } });
        duo = new Button("Together", 0xFF2A9D8F, new Runnable() { @Override public void run() { together = true; } });
        buttons.add(play);
        buttons.add(back);
        buttons.add(solo);
        buttons.add(duo);
    }

    @Override
    public void layout(float vw, float vh) {
        super.layout(vw, vh);
        float l = vw / 2 - 520;
        play.at(vw / 2 + 180, vh - 190, 320, 116);
        back.at(l + 40, vh - 180, 220, 96);
        solo.at(vw / 2 - 210, vh - 180, 170, 96);
        duo.at(vw / 2 - 30, vh - 180, 190, 96);
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        solo.color = together ? 0xFFE8DCC4 : 0xFFFF8A1F;
        duo.color = together ? 0xFF2A9D8F : 0xFFE8DCC4;
    }

    @Override
    public void draw(Canvas c) {
        c.drawRect(0, 0, vw, vh, Draw.fill(0x88000000));
        float l = vw / 2 - 520, t = 40, r = vw / 2 + 520, b = vh - 50;
        Ui.panel(c, l, t, r, b, Pal.PANEL);
        Ui.label(c, L.title, l + 40, t + 76, 54, Pal.INK, Paint.Align.LEFT);
        String sub = L.isBlueprint() ? "Blueprint Puzzle: the refills are shown, win in exactly " + L.exactMoves + " moves"
                : L.kind == Level.Kind.TRACK_DAY ? "Track Day: just for fun and coins"
                : "Bay " + L.bay + ", level " + ((L.index - 1) % 15 + 1) + (L.difficulty == Level.Difficulty.CHALLENGE ? "  •  Builder Challenge" : "");
        Ui.label(c, sub, l + 40, t + 118, 28, 0xFF8A6A40, Paint.Align.LEFT);
        // goals
        Ui.label(c, "Goals", l + 40, t + 180, 30, Pal.INK, Paint.Align.LEFT);
        for (int k = 0; k < L.goals.size(); k++) {
            GoalSpec gs = L.goals.get(k);
            float x = l + 40 + k * 230;
            Ui2.goalIcon(c, g.pieces, gs, x, t + 196, 80);
            String n = gs.count == GoalSpec.ALL ? "all" : String.valueOf(gs.count);
            Ui.label(c, n, x + 92, t + 238, 36, Pal.INK, Paint.Align.LEFT);
            Ui.label(c, gs.label(), x + 92, t + 270, 20, 0xFF8A8072, Paint.Align.LEFT);
        }
        Ui.label(c, L.moves + " moves", r - 40, t + 76, 44, Pal.INK, Paint.Align.RIGHT);
        float y = t + 330;
        if (!L.boNote.isEmpty()) {
            CharacterArt.draw(c, CharacterArt.BO, l + 30, y - 10, 170, 1, time);
            Draw.rrect(c, l + 200, y, r - 40, y + 140, 20, 0xFFFFF6DC, 4);
            Ui.label(c, "Grandpa Bo's Workshop Note", l + 226, y + 38, 26, 0xFF2E4F7A, Paint.Align.LEFT);
            Ui.wrap(c, L.boNote, l + 226, y + 76, r - l - 300, 28, Pal.INK, Paint.Align.LEFT);
            y += 160;
        } else if (!L.tutorial.isEmpty()) {
            Ui.wrap(c, L.tutorial, l + 40, y + 30, r - l - 80, 30, 0xFF5A4A36, Paint.Align.LEFT);
            y += 100;
        }
        int attempts = g.progress.attempts(L.id);
        Double best = g.profile.best.get(L.id);
        if (attempts > 0) {
            String s = "Tries so far: " + attempts + (best != null ? "   Best: " + Math.round(best * 100) + "% of the goals" : "");
            Ui.label(c, s, l + 40, Math.min(y + 30, b - 200), 26, 0xFF8A8072, Paint.Align.LEFT);
        }
        if (together) Ui.label(c, g.profile.playerName + " and " + g.profile.partnerName + " take turns, sharing the moves.", vw / 2 - 210, vh - 200, 24, Pal.TEAL, Paint.Align.LEFT);
        drawButtons(c);
    }
}
