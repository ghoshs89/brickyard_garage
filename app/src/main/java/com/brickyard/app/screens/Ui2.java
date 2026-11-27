package com.brickyard.app.screens;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.brickyard.app.art.Draw;
import com.brickyard.app.art.Pal;
import com.brickyard.app.art.PieceArt;
import com.brickyard.app.ui.Ui;
import com.brickyard.engine.model.GoalSpec;
import com.brickyard.engine.model.Kinds;

/** Screen-level helpers shared by several screens: goal icons, top bar, ledger card. */
final class Ui2 {
    private Ui2() {}

    static void number(Canvas c, String s, float x, float y, float size) {
        Ui.center(c, s, x, y, size, Pal.INK);
    }

    /** Draws a goal's icon in a square. */
    static void goalIcon(Canvas c, PieceArt art, GoalSpec g, float x, float y, float size) {
        RectF r = new RectF(x, y, x + size, y + size);
        switch (g.type) {
            case COLOR: c.drawBitmap(art.piece(g.color), null, r, null); break;
            case CRATE: c.drawBitmap(art.crate(1), null, r, null); break;
            case OIL: c.drawBitmap(art.oil(), null, r, null); break;
            case PART: c.drawBitmap(art.part(), null, r, null); break;
            case CLAMP: c.drawBitmap(art.clamp(1), null, r, null); break;
            case RUST:
                Draw.rrect(c, x + 4, y + 4, x + size - 4, y + size - 4, 6, Pal.RUST, 3);
                Draw.circle(c, x + size * 0.35f, y + size * 0.4f, size * 0.1f, Pal.RUST_DARK, 0);
                break;
            case TRACK: {
                float h = (size - 8) / 4;
                for (int a = 0; a < 4; a++) for (int b = 0; b < 4; b++)
                    c.drawRect(x + 4 + a * h, y + 4 + b * h, x + 4 + (a + 1) * h, y + 4 + (b + 1) * h, Draw.fill((a + b) % 2 == 0 ? 0xFFF4F1E8 : 0xFF2B2B2B));
                c.drawRect(x + 4, y + 4, x + size - 4, y + size - 4, Draw.line(Pal.INK, 3));
                break;
            }
            default:
        }
    }

    static void checkmark(Canvas c, float x, float y, float s) {
        Path p = new Path();
        p.moveTo(x, y + s * 0.5f);
        p.lineTo(x + s * 0.38f, y + s * 0.88f);
        p.lineTo(x + s, y + s * 0.1f);
        c.drawPath(p, Draw.line(Pal.GREEN_OK, s * 0.22f));
    }

    /** Stars and coins in the top-left corner. */
    static void topBar(Canvas c, int stars, int coins, float x, float y) {
        Draw.rrect(c, x, y, x + 190, y + 64, 32, Pal.PANEL, 4);
        Draw.star(c, x + 36, y + 32, 22, Pal.GOLD, 3);
        Ui.label(c, String.valueOf(stars), x + 70, y + 45, 36, Pal.INK, Paint.Align.LEFT);
        Draw.rrect(c, x + 206, y, x + 420, y + 64, 32, Pal.PANEL, 4);
        Draw.coin(c, x + 242, y + 32, 20);
        Ui.label(c, String.valueOf(coins), x + 276, y + 45, 36, Pal.INK, Paint.Align.LEFT);
    }

    /**
     * Workshop Ledger: exact place-value coin stacks (hundreds blocks, true 10-coin stacks, singles)
     * plus an integrated item header and "Have 74 - Cost 45 = 29 left" mental-math receipt.
     */
    static void ledger(Canvas c, int have, int cost, float l, float t, float w) {
        ledger(c, "Workshop Ledger", "", have, cost, l, t, w);
    }

    static void ledger(Canvas c, String itemTitle, String statNote, int have, int cost, float l, float t, float w) {
        Ui.panel(c, l, t, l + w, t + 290, 0xFFFFF6DC);
        Ui.label(c, itemTitle, l + 30, t + 48, 34, Pal.INK, Paint.Align.LEFT);
        if (statNote != null && !statNote.isEmpty()) {
            Ui.label(c, statNote, l + w - 30, t + 48, 26, Pal.TEAL, Paint.Align.RIGHT);
        }
        // True place-value visualization: gold 100-bricks, exact 10-disk stacks, and single coins.
        int hundreds = have / 100, tens = (have % 100) / 10, ones = have % 10;
        float x = l + 36, base = t + 186;
        int drawnH = Math.min(hundreds, 5);
        for (int k = 0; k < drawnH; k++) {
            Draw.brick(c, x, base - 58, x + 56, base + 8, Pal.GOLD, 2, 2.5f);
            Ui.center(c, "100", x + 28, base - 16, 20, Pal.INK);
            x += 64;
        }
        if (hundreds > drawnH) {
            Ui.label(c, "+" + (hundreds - drawnH) + "×100", x, base - 16, 22, Pal.GOLD_DARK, Paint.Align.LEFT);
            x += 82;
        }
        for (int k = 0; k < tens; k++) {
            // Exactly 10 crisp coin disks per stack so counting matches "tens"!
            for (int j = 0; j < 10; j++) {
                Draw.rrect(c, x, base - j * 8, x + 36, base - j * 8 + 7, 3, j % 2 == 0 ? Pal.GOLD : Pal.GOLD_DARK, 1.5f);
            }
            x += 42;
        }
        for (int k = 0; k < ones; k++) {
            Draw.coin(c, x + 13, base - 4, 11);
            x += 25;
        }
        String place = hundreds > 0
                ? hundreds + " hundred" + (hundreds == 1 ? "" : "s") + ", " + tens + " ten" + (tens == 1 ? "" : "s") + " and " + ones + " one" + (ones == 1 ? "" : "s")
                : tens + " ten" + (tens == 1 ? "" : "s") + " and " + ones + " one" + (ones == 1 ? "" : "s");
        Ui.label(c, place, l + 36, t + 224, 24, 0xFF7A7062, Paint.Align.LEFT);
        boolean ok = have >= cost;
        String eq = ok ? "Have " + have + "  −  Cost " + cost + "  =  " + (have - cost) + " left"
                : "Have " + have + ", need " + cost + ": " + (cost - have) + " more coins";
        Ui.label(c, eq, l + 36, t + 268, 32, ok ? Pal.INK : Pal.RED_BAD, Paint.Align.LEFT);
    }
}
