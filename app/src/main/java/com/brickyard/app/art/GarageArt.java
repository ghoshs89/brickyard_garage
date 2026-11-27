package com.brickyard.app.art;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import java.util.Set;

/**
 * The three garage bays, drawn in a 1000 x 560 box. Every repair changes something you can see,
 * and every fixture draws whichever of its three designs is chosen. The vehicle is drawn by the
 * caller in the lift area (centre, ground at y = 470).
 */
public final class GarageArt {
    private GarageArt() {}

    public interface Looks {
        /** Chosen design index for a fixture id, or -1 if the fixture is not unlocked yet. */
        int look(String fixtureId);
    }

    public static final float LIFT_X = 330, LIFT_Y = 300, LIFT_W = 340; // car box placement

    public static void drawBay(Canvas c, int bay, Set<String> repairs, Looks looks, float t) {
        String p = "b" + bay + "-r";
        boolean r1 = repairs.contains(p + "1"), r2 = repairs.contains(p + "2"), r3 = repairs.contains(p + "3"),
                r4 = repairs.contains(p + "4"), r5 = repairs.contains(p + "5");
        int wall = bay == 1 ? 0xFFC8664A : bay == 2 ? 0xFF6E8B74 : 0xFFDCE6EA;
        int wallDark = Pal.darker(wall, 0.8f);
        // ---- wall
        Draw.rrectGrad(c, 0, 0, 1000, 470, 0, wall, wallDark, 0);
        if (bay == 1) { // brick courses
            for (int row = 0; row < 16; row++)
                for (int col = 0; col < 14; col++) {
                    float x = col * 76 + (row % 2) * 38 - 38, y = row * 30;
                    c.drawRect(x + 2, y + 2, x + 74, y + 28, Draw.line(Pal.darker(wall, 0.82f), 2));
                }
        } else if (bay == 2) { // corrugated panels
            for (int k = 0; k < 50; k++) c.drawLine(k * 20, 0, k * 20, 470, Draw.line(Pal.darker(wall, 0.88f), 4));
        } else { // clean lab tiles
            for (int k = 0; k < 20; k++) c.drawLine(k * 50, 0, k * 50, 470, Draw.line(0x22000000, 1.5f));
            for (int k = 0; k < 10; k++) c.drawLine(0, k * 50, 1000, k * 50, Draw.line(0x22000000, 1.5f));
        }
        // ---- big bay door (r5 opens it to daylight / night sky with neon)
        float dl = 650, dr = 960, dt = 60, db = 470;
        if (r5) {
            Draw.rrectGrad(c, dl, dt, dr, db, 0, bay == 3 ? Pal.NIGHT : Pal.SKY, bay == 3 ? 0xFF3B4A8A : 0xFFE9F6FB, 5);
            if (bay == 3) for (int k = 0; k < 14; k++) Draw.circle(c, dl + 20 + (k * 97) % 290, dt + 20 + (k * 53) % 160, 2, 0xFFFFFFFF, 0);
            else { Draw.circle(c, dl + 240, dt + 70, 28, 0xFFFFE066, 0); Draw.rrect(c, dl, db - 60, dr, db, 0, 0xFF7FB069, 0); }
            for (int k = 0; k < 3; k++) Draw.rrect(c, dl, dt + k * 14, dr, dt + 10 + k * 14, 0, Pal.STEEL, 3);
        } else {
            Draw.rrect(c, dl, dt, dr, db, 0, Pal.STEEL_DARK, 5);
            for (int k = 0; k < 12; k++) c.drawLine(dl, dt + 34 * k, dr, dt + 34 * k, Draw.line(Pal.darker(Pal.STEEL_DARK, 0.8f), 4));
            c.drawLine(dl + 40, dt + 40, dl + 120, db - 30, Draw.line(0x55000000, 3)); // scratches
        }
        // ---- sign above (BRICKYARD neon after r5)
        Draw.rrect(c, 300, 14, 640, 74, 10, r5 ? 0xFF1B1B2F : 0xFF5A5148, 5);
        Paint sp = text(34, r5 ? (((int) (t * 2)) % 7 == 0 ? 0xFFFF7AD9 : 0xFFFF4FC3) : 0xFF8A8072);
        if (r5) { sp.setShadowLayer(14, 0, 0, 0xFFFF4FC3); }
        c.drawText(bay == 1 ? "BRICKYARD" : bay == 2 ? "RESCUE RIGS" : "PROTO LAB", 470, 56, sp);
        sp.clearShadowLayer();

        // ---- floor
        int floor = Pal.FLOOR;
        int fl = looks.look("b1-floor");
        if (bay == 1 && fl == 1) floor = 0xFFE9E4D8;
        Draw.rrect(c, 0, 470, 1000, 560, 0, floor, 0);
        if (bay == 1 && fl == 1) for (int k = 0; k < 25; k++) for (int j = 0; j < 3; j++) if ((k + j) % 2 == 0) c.drawRect(k * 40, 470 + j * 30, k * 40 + 40, 500 + j * 30, Draw.fill(0xFF2B2B2B));
        if (bay == 1 && fl == 2) for (int k = 0; k < 40; k++) for (int j = 0; j < 3; j++) Draw.stud(c, 12 + k * 25, 482 + j * 28, 7, 0xFF8FA0AE);
        c.drawLine(0, 470, 1000, 470, Draw.line(Pal.INK, 5));
        if (!r1 || (bay == 1 && fl == 0)) {
            // oil stains
            c.drawOval(new RectF(120, 500, 230, 530), Draw.fill(0x33000000));
            c.drawOval(new RectF(760, 510, 860, 540), Draw.fill(0x33000000));
        }

        // ---- bay-specific repairs
        if (bay == 1) bay1(c, r1, r2, r3, r4, looks, t);
        else if (bay == 2) bay2(c, r1, r2, r3, r4, looks, t);
        else bay3(c, r1, r2, r3, r4, looks, t);

        // ---- before the first repair: dust, cobwebs, junk
        if (!r1) {
            c.drawRect(0, 0, 1000, 560, Draw.fill(0x40706050));
            web(c, 0, 0, 1);
            web(c, 1000, 0, -1);
            for (int k = 0; k < 6; k++) Draw.brick(c, 40 + k * 26, 440 - (k % 3) * 22, 80 + k * 26, 470 - (k % 3) * 22, 0xFF8A7F70, 2, 3);
            Draw.rrect(c, 820, 400, 900, 470, 4, 0xFF9C8A6E, 4);
            Draw.rrect(c, 860, 360, 940, 430, 4, 0xFF8C7A5E, 4);
        }
    }

    private static void bay1(Canvas c, boolean r1, boolean r2, boolean r3, boolean r4, Looks looks, float t) {
        // Hydraulic lift (r2): posts either side of the car
        int post = r2 ? 0xFFE0B13A : 0xFF7D7462;
        Draw.rrect(c, 300, 230, 330, 470, 4, post, 4);
        Draw.rrect(c, 670, 230, 700, 470, 4, post, 4);
        if (!r2) { c.drawLine(300, 300, 330, 330, Draw.line(Pal.RUST, 6)); Draw.circle(c, 690, 350, 10, Pal.RUST, 0); }
        // workbench (fixture, after r2)
        int bench = looks.look("b1-bench");
        if (bench >= 0) {
            int top = bench == 0 ? Pal.WOOD : bench == 1 ? 0xFFC0392B : 0xFF2F6FD6;
            if (bench == 2) Draw.brick(c, 40, 360, 260, 390, top, 7, 4); else Draw.rrect(c, 40, 360, 260, 386, 4, top, 4);
            Draw.rrect(c, 52, 386, 72, 470, 2, Pal.darker(top, 0.7f), 4);
            Draw.rrect(c, 228, 386, 248, 470, 2, Pal.darker(top, 0.7f), 4);
            Draw.rrect(c, 90, 330, 150, 360, 4, 0xFF555555, 3); // vice
            Draw.circle(c, 200, 345, 14, Pal.PIECE[1], 3);
        }
        // paint booth (r3): curtain + spray light
        if (r3) {
            Draw.rrect(c, 720, 120, 760, 470, 4, 0xFFF4F1E8, 4);
            Draw.circle(c, 740, 140, 12, 0xFFFFE066, 3);
        } else {
            Path torn = new Path();
            torn.moveTo(720, 120); torn.lineTo(760, 120); torn.lineTo(750, 300); torn.lineTo(730, 260); torn.close();
            Draw.path(c, torn, 0xFFBDB4A0, 4);
        }
        // tool wall (fixture after r4)
        int wallLook = looks.look("b1-wall");
        if (wallLook >= 0) toolWall(c, 40, 110, 260, 320, wallLook);
        else if (!r4) for (int k = 0; k < 4; k++) c.drawLine(60 + k * 50, 140, 70 + k * 50, 300, Draw.line(0x33000000, 3));
    }

    private static void bay2(Canvas c, boolean r1, boolean r2, boolean r3, boolean r4, Looks looks, float t) {
        // skylights (r4)
        for (int k = 0; k < 3; k++) {
            float x = 100 + k * 180;
            Draw.rrect(c, x, 90, x + 120, 150, 4, r4 ? 0xFFBFE7F5 : 0xFF46504A, 4);
            if (!r4) c.drawLine(x + 10, 100, x + 100, 145, Draw.line(0xFF222222, 3));
        }
        // crane (fixture after r2) on a roof rail
        Draw.rrect(c, 0, 170, 1000, 186, 0, Pal.STEEL_DARK, 4);
        int crane = looks.look("b2-crane");
        float hookY = 250 + (float) Math.sin(t) * 6;
        if (crane >= 0) {
            int col = crane == 0 ? 0xFF8C8C8C : crane == 1 ? 0xFFF5C518 : 0xFFE03B2E;
            if (crane == 2) Draw.brick(c, 420, 186, 580, 214, col, 6, 4); else Draw.rrect(c, 430, 186, 570, 212, 4, col, 4);
            c.drawLine(500, 212, 500, hookY, Draw.line(Pal.INK, 4));
            c.drawArc(new RectF(488, hookY - 4, 512, hookY + 20), 0, 200, false, Draw.line(Pal.INK, 5));
        } else {
            c.drawLine(500, 186, 520, 260, Draw.line(0xFF555555, 4));
        }
        // rescue stripes on the wall (r3)
        if (r3) for (int k = 0; k < 12; k++) {
            Path s = new Path();
            float x = k * 60;
            s.moveTo(x, 420); s.lineTo(x + 30, 420); s.lineTo(x + 60, 450); s.lineTo(x + 30, 450); s.close();
            Draw.path(c, s, k % 2 == 0 ? 0xFFFF7A1A : 0xFF222222, 0);
        }
        // lift (fixture after r1)
        int lift = looks.look("b2-lift");
        if (lift == 0) { Draw.rrect(c, 300, 230, 330, 470, 4, 0xFF5D7F9E, 4); Draw.rrect(c, 670, 230, 700, 470, 4, 0xFF5D7F9E, 4); }
        else if (lift == 1) { Draw.rrect(c, 320, 440, 680, 458, 4, 0xFF3A6EA5, 4); c.drawLine(340, 458, 660, 470, Draw.line(Pal.INK, 5)); c.drawLine(340, 470, 660, 458, Draw.line(Pal.INK, 5)); }
        else if (lift == 2) { Path ramp = new Path(); ramp.moveTo(260, 470); ramp.lineTo(330, 430); ramp.lineTo(670, 430); ramp.lineTo(740, 470); ramp.close(); Draw.path(c, ramp, 0xFFFF7A1A, 4); }
        // parts shelves (fixture after r4)
        int shelves = looks.look("b2-shelves");
        if (shelves >= 0) {
            for (int k = 0; k < 3; k++) {
                float y = 260 + k * 70;
                Draw.rrect(c, 30, y, 250, y + 10, 2, shelves == 1 ? Pal.STEEL : Pal.WOOD, 3);
                for (int j = 0; j < 4; j++) {
                    if (shelves == 2) Draw.brick(c, 40 + j * 52, y - 34, 84 + j * 52, y, Pal.PIECE[(j + k) % 6], 2, 3);
                    else Draw.rrect(c, 40 + j * 52, y - 34, 84 + j * 52, y, 3, shelves == 0 ? Pal.WOOD_DARK : 0xFF6F7C88, 3);
                }
            }
        }
        // mud before r1
        if (!r1) c.drawOval(new RectF(200, 480, 800, 550), Draw.fill(0xCC5B4026));
    }

    private static void bay3(Canvas c, boolean r1, boolean r2, boolean r3, boolean r4, Looks looks, float t) {
        // wind tunnel ring on the left with the fan (fixture after r1)
        Draw.circle(c, 150, 260, 130, 0xFF9AA6B2, 5);
        Draw.circle(c, 150, 260, 110, 0xFF2B3440, 4);
        int fan = looks.look("b3-fan");
        float spin = r4 ? t * 8 : 0;
        if (fan >= 0) {
            int blades = fan == 0 ? 2 : fan == 1 ? 3 : 6;
            for (int k = 0; k < blades; k++) {
                c.save();
                c.rotate((float) Math.toDegrees(spin) + k * 360f / blades, 150, 260);
                Path b = new Path();
                b.moveTo(150, 260); b.cubicTo(170, 220, 190, 180, 160, 160); b.cubicTo(140, 180, 140, 220, 150, 260); b.close();
                Draw.path(c, b, fan == 2 ? 0xFF7FDBFF : 0xFFDCE6EA, 3);
                c.restore();
            }
            if (fan == 2) c.drawCircle(150, 260, 100, Draw.line(0xFF7FDBFF, 5));
            Draw.circle(c, 150, 260, 16, Pal.STEEL_DARK, 3);
        }
        if (r4) for (int k = 0; k < 4; k++) { // smoke streams
            float y = 180 + k * 50, ph = (t * 0.5f + k * 0.25f) % 1f;
            c.drawLine(280, y, 280 + ph * 340, y + (float) Math.sin(ph * 6 + k) * 10, Draw.line(0x55FFFFFF, 8));
        }
        // dyno rollers (fixture after r2) under the car
        int dyno = looks.look("b3-dyno");
        if (dyno >= 0) {
            int col = dyno == 0 ? Pal.STEEL : dyno == 1 ? 0xFFE03B2E : 0xFF7FDBFF;
            for (float x : new float[]{410, 620}) {
                Draw.rrect(c, x - 50, 466, x + 50, 486, 8, col, 4);
                if (dyno == 1) for (int k = 0; k < 4; k++) c.drawLine(x - 40 + k * 25, 468, x - 30 + k * 25, 484, Draw.line(0xFFFFFFFF, 4));
            }
        }
        // clean room glass (r3)
        if (r3) Draw.rrect(c, 700, 120, 980, 300, 6, 0x5596D8F0, 4);
        // showcase plinth (fixture after r3)
        int stand = looks.look("b3-stand");
        if (stand >= 0) {
            if (stand == 0) Draw.brick(c, 760, 400, 940, 470, 0xFF8FA0AE, 6, 4);
            else if (stand == 1) { Draw.rrect(c, 760, 440, 940, 470, 30, 0xFF555555, 4); Draw.rrect(c, 780, 425, 920, 445, 20, 0xFF888888, 4); }
            else {
                Draw.rrect(c, 760, 440, 940, 470, 4, 0xFF333333, 4);
                Path beam = new Path(); beam.moveTo(850, 120); beam.lineTo(790, 440); beam.lineTo(910, 440); beam.close();
                c.drawPath(beam, Draw.fill(0x33FFF4A8));
                Draw.circle(c, 850, 120, 14, 0xFFFFF4A8, 3);
            }
            Draw.star(c, 850, 380, 22, Pal.GOLD, 3);
        }
        if (!r2) for (int k = 0; k < 5; k++) c.drawLine(380 + k * 60, 470, 400 + k * 60, 440, Draw.line(0xFF3A3A3A, 3)); // loose wires
    }

    private static void toolWall(Canvas c, float l, float t, float r, float b, int look) {
        if (look == 0) {
            Draw.rrect(c, l, t, r, b, 4, 0xFFD9B98C, 4);
            for (float x = l + 14; x < r; x += 22) for (float y = t + 14; y < b; y += 22) Draw.circle(c, x, y, 2.5f, 0xFF7A5A36, 0);
        } else if (look == 1) {
            Draw.rrect(c, l + 30, t + 40, r - 30, b, 6, 0xFFC0392B, 4);
            for (int k = 0; k < 5; k++) Draw.rrect(c, l + 40, t + 52 + k * 36, r - 40, t + 80 + k * 36, 3, 0xFFA93226, 3);
        } else {
            Draw.rrect(c, l, t + 60, r, t + 78, 4, 0xFF333333, 4);
        }
        // tools hang on all three designs
        int[] cols = {Pal.PIECE[5], Pal.STEEL, Pal.PIECE[0], Pal.PIECE[2]};
        for (int k = 0; k < 4; k++) {
            float x = l + 30 + k * (r - l - 60) / 3f;
            c.drawLine(x, t + 70, x, t + 150, Draw.line(cols[k], 10));
            Draw.circle(c, x, t + 66, 12, cols[k], 3);
        }
    }

    private static void web(Canvas c, float x, float y, int dir) {
        for (int k = 0; k < 5; k++) c.drawLine(x, y, x + dir * (40 + k * 30), y + (140 - k * 30), Draw.line(0x88FFFFFF, 2));
        for (int r = 1; r < 4; r++) c.drawArc(new RectF(x - r * 50, y - r * 50, x + r * 50, y + r * 50), dir > 0 ? 0 : 90, 90, false, Draw.line(0x66FFFFFF, 2));
    }

    static Paint text(float size, int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(size);
        p.setColor(color);
        p.setFakeBoldText(true);
        p.setTextAlign(Paint.Align.CENTER);
        p.setLetterSpacing(0.08f);
        return p;
    }
}
