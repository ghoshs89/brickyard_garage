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
     * Workshop Ledger: the money maths, shown before every purchase.
     * Coins drawn as stacks of ten plus singles, then "Have 74 - Cost 45 = 29 left".
     */
    static void ledger(Canvas c, int have, int cost, float l, float t, float w) {
        Ui.panel(c, l, t, l + w, t + 250, 0xFFFFF6DC);
        Ui.label(c, "Workshop Ledger", l + 30, t + 52, 34, Pal.INK, Paint.Align.LEFT);
        // coin stacks: one stack per ten
        int tens = have / 10, ones = have % 10;
        float x = l + 40, base = t + 150;
        for (int k = 0; k < Math.min(tens, 12); k++) {
            for (int j = 0; j < 5; j++) Draw.rrect(c, x, base - j * 9, x + 40, base - j * 9 + 10, 5, j % 2 == 0 ? Pal.GOLD : Pal.GOLD_DARK, 2);
            x += 46;
        }
        for (int k = 0; k < ones; k++) { Draw.coin(c, x + 16, base - 4, 12); x += 28; }
        Ui.label(c, tens + " tens and " + ones + " ones", l + 40, t + 190, 24, 0xFF8A8072, Paint.Align.LEFT);
        boolean ok = have >= cost;
        String eq = ok ? "Have " + have + "  −  Cost " + cost + "  =  " + (have - cost) + " left"
                : "Have " + have + ", need " + cost + ": " + (cost - have) + " more coins";
        Ui.label(c, eq, l + 40, t + 232, 32, ok ? Pal.INK : Pal.RED_BAD, Paint.Align.LEFT);
    }
}
