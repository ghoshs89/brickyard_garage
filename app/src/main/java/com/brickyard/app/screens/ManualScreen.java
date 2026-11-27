package com.brickyard.app.screens;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.brickyard.app.GameView;
import com.brickyard.app.art.Draw;
import com.brickyard.app.art.Pal;
import com.brickyard.app.game.Game;
import com.brickyard.app.ui.Button;
import com.brickyard.app.ui.Screen;
import com.brickyard.app.ui.Ui;
import com.brickyard.engine.model.Kinds;

/**
 * Grandpa Bo's Shop Manual: visual, icon-first guide to power-up shapes and combos
 * so a young strategist can study setups like a chess chart.
 */
public final class ManualScreen extends Screen implements GameView.Overlay {
    private final Button close, tabShapes, tabCombos;
    private boolean showCombos;

    public ManualScreen(final Game g) {
        super(g);
        close = new Button("Got it!", Pal.GREEN_OK, new Runnable() { @Override public void run() { g.pop(); } });
        tabShapes = new Button("Power-Ups", 0xFFFFE066, new Runnable() { @Override public void run() { showCombos = false; } });
        tabCombos = new Button("Combos", 0xFFE8C89A, new Runnable() { @Override public void run() { showCombos = true; } });
        tabShapes.textSize = 26;
        tabCombos.textSize = 26;
        buttons.add(tabShapes);
        buttons.add(tabCombos);
        buttons.add(close);
    }

    @Override
    public void layout(float vw, float vh) {
        super.layout(vw, vh);
        float l = vw / 2 - 580;
        tabShapes.at(l + 40, 108, 220, 72);
        tabCombos.at(l + 280, 108, 220, 72);
        close.at(vw / 2 - 140, vh - 118, 280, 86);
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        tabShapes.color = !showCombos ? 0xFFFFE066 : 0xFFE8C89A;
        tabCombos.color = showCombos ? 0xFFFFE066 : 0xFFE8C89A;
    }

    @Override
    public void draw(Canvas c) {
        c.drawRect(0, 0, vw, vh, Draw.fill(0x99000000));
        float l = vw / 2 - 580, r = vw / 2 + 580;
        Ui.panel(c, l, 26, r, vh - 24, Pal.PANEL);
        Ui.label(c, "Grandpa Bo's Shop Manual", l + 40, 82, 42, Pal.INK, Paint.Align.LEFT);
        if (!showCombos) drawShapes(c, l, r);
        else drawCombos(c, l, r);
        drawButtons(c);
    }

    private void drawShapes(Canvas c, float l, float r) {
        float w = (r - l - 100) / 2f;
        drawShapeCard(c, l + 40, 200, w, 215, Kinds.NITRO_H, "Nitro Booster", "Match 4 in a line", "Clears a full row or column. Tap it in place or swap!", new int[][]{{0,1},{1,1},{2,1},{3,1}});
        drawShapeCard(c, l + 60 + w, 200, w, 215, Kinds.BUGGY, "RC Brick Buggy", "Match a 2 × 2 square", "Pops 4 neighbours, then drives straight to a goal!", new int[][]{{1,1},{2,1},{1,2},{2,2}});
        drawShapeCard(c, l + 40, 434, w, 215, Kinds.AIRBAG, "Shockwave Airbag", "Match 5 in a T or L", "Blasts a big diamond all around it. Tap or swap!", new int[][]{{0,1},{1,1},{2,1},{1,0},{1,2}});
        drawShapeCard(c, l + 60 + w, 434, w, 215, Kinds.SPRAYER, "Color Sprayer", "Match 5 in a row", "Swap with any brick to zap ALL bricks of that colour!", new int[][]{{0,1},{1,1},{2,1},{3,1},{4,1}});
    }

    private void drawShapeCard(Canvas c, float x, float y, float w, float h, int kind, String title, String rule, String desc, int[][] cells) {
        Draw.rrect(c, x, y, x + w, y + h, 18, 0xFFFFFFFF, 3.5f);
        // mini brick pattern
        float bx = x + 18, by = y + 36, cs = 22;
        for (int[] pt : cells) {
            Draw.brick(c, bx + pt[0] * cs, by + pt[1] * cs, bx + (pt[0] + 1) * cs - 2, by + (pt[1] + 1) * cs - 2, Pal.PIECE[0], 1, 1.5f);
        }
        Ui.center(c, "→", bx + 128, by + 48, 34, Pal.GOLD_DARK);
        c.drawBitmap(g.pieces.power(kind), null, new RectF(bx + 150, by - 4, bx + 236, by + 82), null);
        Ui.label(c, title, x + 266, y + 52, 28, Pal.INK, Paint.Align.LEFT);
        Ui.label(c, rule, x + 266, y + 86, 22, Pal.TEAL, Paint.Align.LEFT);
        Ui.wrap(c, desc, x + 20, y + 146, w - 40, 22, 0xFF5A4A36, Paint.Align.LEFT);
    }

    private void drawCombos(Canvas c, float l, float r) {
        int[][] pairs = {
                {Kinds.NITRO_H, Kinds.NITRO_V},
                {Kinds.AIRBAG, Kinds.NITRO_H},
                {Kinds.AIRBAG, Kinds.AIRBAG},
                {Kinds.BUGGY, Kinds.BUGGY},
                {Kinds.BUGGY, Kinds.NITRO_V},
                {Kinds.BUGGY, Kinds.AIRBAG},
                {Kinds.SPRAYER, Kinds.NITRO_H},
                {Kinds.SPRAYER, Kinds.AIRBAG},
                {Kinds.SPRAYER, Kinds.BUGGY},
                {Kinds.SPRAYER, Kinds.SPRAYER}
        };
        String[] labels = {
                "Cross Blast (row + column)",
                "Giant 3-Row × 3-Col Cross",
                "Mega Diamond Blast",
                "3 Smart RC Buggies!",
                "Buggy carries Nitro to goal",
                "Buggy drops Airbag on goal",
                "Most common color → Nitros!",
                "Most common color → Airbags!",
                "Most common color → Buggies!",
                "Board-Wide Mega Clear!"
        };
        float colW = (r - l - 100) / 2f;
        for (int k = 0; k < pairs.length; k++) {
            int col = k / 5, row = k % 5;
            float x = l + 40 + col * (colW + 20);
            float y = 198 + row * 92;
            Draw.rrect(c, x, y, x + colW, y + 80, 16, 0xFFFFFFFF, 3);
            c.drawBitmap(g.pieces.power(pairs[k][0]), null, new RectF(x + 12, y + 10, x + 72, y + 70), null);
            Ui.center(c, "+", x + 86, y + 52, 30, Pal.GOLD_DARK);
            c.drawBitmap(g.pieces.power(pairs[k][1]), null, new RectF(x + 100, y + 10, x + 160, y + 70), null);
            Ui.label(c, labels[k], x + 176, y + 50, 23, Pal.INK, Paint.Align.LEFT);
        }
    }
}
