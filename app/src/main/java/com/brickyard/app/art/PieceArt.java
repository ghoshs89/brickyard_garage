package com.brickyard.app.art;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;

import com.brickyard.engine.model.Kinds;

import java.util.HashMap;
import java.util.Map;

/**
 * Board tiles. Each colour has its own silhouette (brick, gear, spark plug, tyre, cone, wrench),
 * so a piece is readable by shape alone. Tiles are rendered once per size into bitmaps.
 */
public final class PieceArt {
    private final Map<String, Bitmap> cache = new HashMap<>();
    private int px = 128;

    /** Physical pixel size of one tile; clears the cache when it changes. */
    public void setTilePixels(int p) {
        p = Math.max(128, p); // never smaller than 128 px, so icons stay sharp on every screen
        if (p == px) return;
        for (Bitmap b : cache.values()) b.recycle();
        cache.clear();
        px = p;
    }

    public Bitmap piece(int color) { return get("p" + color); }
    public Bitmap power(int kind) { return get("u" + kind); }
    public Bitmap crate(int layers) { return get("c" + layers); }
    public Bitmap oil() { return get("oil"); }
    public Bitmap part() { return get("part"); }
    public Bitmap clamp(int layers) { return get("k" + layers); }

    private Bitmap get(String key) {
        Bitmap b = cache.get(key);
        if (b == null) {
            b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            c.scale(px / 100f, px / 100f);
            char k = key.charAt(0);
            if (key.equals("oil")) drawOil(c);
            else if (key.equals("part")) drawPart(c);
            else if (k == 'p') drawPiece(c, Integer.parseInt(key.substring(1)));
            else if (k == 'u') drawPower(c, Integer.parseInt(key.substring(1)));
            else if (k == 'c') drawCrate(c, Integer.parseInt(key.substring(1)));
            else if (k == 'k') drawClamp(c, Integer.parseInt(key.substring(1)));
            cache.put(key, b);
        }
        return b;
    }

    // All drawing below is in a 100 x 100 box.

    public static void drawPiece(Canvas c, int color) {
        int col = Pal.PIECE[color], dark = Pal.PIECE_DARK[color];
        float o = 3.2f;
        switch (color) {
            case Kinds.RED_BRICK: {
                Draw.brick(c, 14, 30, 86, 84, col, 2, o);
                break;
            }
            case Kinds.BLUE_GEAR: {
                Draw.path(c, Draw.gearPath(50, 52, 40, 31, 10, 0.1f), col, o);
                Draw.circle(c, 50, 52, 18, Pal.lighter(col, 0.3f), o);
                Draw.circle(c, 50, 52, 7, Pal.INK, 0);
                break;
            }
            case Kinds.YELLOW_PLUG: {
                Path body = new Path();
                body.moveTo(36, 18); body.lineTo(64, 18); body.lineTo(60, 46); body.lineTo(40, 46); body.close();
                Draw.path(c, body, Pal.CREAM, o);
                Path hex = new Path();
                hex.moveTo(28, 46); hex.lineTo(72, 46); hex.lineTo(76, 58); hex.lineTo(72, 70); hex.lineTo(28, 70); hex.lineTo(24, 58); hex.close();
                Draw.path(c, hex, col, o);
                c.drawLine(40, 50, 40, 66, Draw.line(dark, 3));
                c.drawLine(60, 50, 60, 66, Draw.line(dark, 3));
                Draw.rrect(c, 40, 70, 60, 84, 3, col, o);
                c.drawLine(50, 84, 50, 93, Draw.line(Pal.STEEL_DARK, 5));
                Draw.rrect(c, 44, 8, 56, 19, 3, Pal.STEEL, o);
                break;
            }
            case Kinds.GREEN_TIRE: {
                Draw.circle(c, 50, 52, 41, 0xFF2A2A2A, o);
                for (int k = 0; k < 12; k++) {
                    double a = k * Math.PI / 6;
                    c.drawLine(50 + (float) Math.cos(a) * 34, 52 + (float) Math.sin(a) * 34, 50 + (float) Math.cos(a) * 41, 52 + (float) Math.sin(a) * 41, Draw.line(0xFF4A4A4A, 4));
                }
                Draw.circle(c, 50, 52, 24, col, o);
                Draw.circle(c, 50, 52, 9, Pal.lighter(col, 0.4f), o * 0.8f);
                for (int k = 0; k < 5; k++) {
                    double a = k * 2 * Math.PI / 5;
                    Draw.circle(c, 50 + (float) Math.cos(a) * 16, 52 + (float) Math.sin(a) * 16, 3, dark, 0);
                }
                break;
            }
            case Kinds.ORANGE_CONE: {
                Path cone = new Path();
                cone.moveTo(50, 10); cone.lineTo(74, 78); cone.lineTo(26, 78); cone.close();
                Draw.path(c, cone, col, o);
                Path stripe = new Path();
                stripe.moveTo(41, 36); stripe.lineTo(59, 36); stripe.lineTo(64, 50); stripe.lineTo(36, 50); stripe.close();
                Draw.path(c, stripe, Pal.CREAM, 0);
                Draw.rrect(c, 16, 76, 84, 90, 4, dark, o);
                break;
            }
            default: { // PURPLE_WRENCH
                c.save();
                c.rotate(-40, 50, 50);
                Path w = new Path();
                w.addRoundRect(new RectF(42, 26, 58, 92), 7, 7, Path.Direction.CW);
                Path head = new Path();
                head.addCircle(50, 22, 19, Path.Direction.CW);
                w.op(head, Path.Op.UNION);
                Path jaw = new Path();
                jaw.addRect(43, -2, 57, 20, Path.Direction.CW);
                w.op(jaw, Path.Op.DIFFERENCE);
                Draw.path(c, w, col, o);
                Draw.circle(c, 50, 78, 6, dark, 0);
                c.drawLine(50, 40, 50, 66, Draw.line(Pal.lighter(col, 0.35f), 4));
                c.restore();
            }
        }
    }

    public static void drawPower(Canvas c, int kind) {
        float o = 3.2f;
        switch (kind) {
            case Kinds.NITRO_H:
            case Kinds.NITRO_V: {
                c.save();
                if (kind == Kinds.NITRO_V) c.rotate(90, 50, 50);
                // double-ended arrow so the clearing direction is obvious
                Path arrow = new Path();
                arrow.moveTo(4, 50); arrow.lineTo(18, 38); arrow.lineTo(18, 62); arrow.close();
                arrow.moveTo(96, 50); arrow.lineTo(82, 38); arrow.lineTo(82, 62); arrow.close();
                Draw.path(c, arrow, Pal.GOLD, o * 0.8f);
                Draw.rrectGrad(c, 18, 34, 82, 66, 14, 0xFF56B4FF, 0xFF1E5FB0, o);
                Draw.rrect(c, 30, 34, 38, 66, 0, Pal.CREAM, 0);
                Draw.rrect(c, 62, 34, 70, 66, 0, Pal.CREAM, 0);
                c.drawText("N", 50, 58, textPaint(22));
                c.restore();
                break;
            }
            case Kinds.BUGGY: {
                Draw.shadowEllipse(c, 50, 82, 70, 10);
                Draw.brick(c, 18, 44, 82, 72, 0xFFFF5A36, 3, o);
                Draw.rrect(c, 34, 30, 66, 46, 6, 0xFF8FD3FF, o);
                Draw.circle(c, 32, 76, 11, 0xFF222222, o);
                Draw.circle(c, 68, 76, 11, 0xFF222222, o);
                Draw.circle(c, 32, 76, 4, Pal.STEEL, 0);
                Draw.circle(c, 68, 76, 4, Pal.STEEL, 0);
                c.drawLine(70, 44, 82, 14, Draw.line(Pal.INK, 3));
                Draw.circle(c, 82, 13, 4, Pal.RED_BAD, 0);
                break;
            }
            case Kinds.AIRBAG: {
                Draw.star(c, 50, 50, 46, Pal.GOLD, o * 0.8f);
                Draw.circle(c, 50, 50, 28, Pal.CREAM, o);
                Draw.circle(c, 50, 50, 16, Pal.RED_BAD, o * 0.7f);
                Draw.circle(c, 45, 45, 5, 0xFFFFFFFF, 0);
                break;
            }
            default: { // SPRAYER
                int[] rainbow = Pal.PIECE;
                for (int k = 0; k < 6; k++) {
                    float a = (float) (k * Math.PI / 3);
                    Draw.circle(c, 50 + (float) Math.cos(a) * 30, 50 + (float) Math.sin(a) * 30, 15, rainbow[k], o * 0.7f);
                }
                Draw.circle(c, 50, 50, 22, Pal.CREAM, o);
                Draw.rrect(c, 40, 36, 60, 64, 5, Pal.STEEL, o * 0.8f);
                Draw.circle(c, 50, 42, 4, Pal.INK, 0);
            }
        }
    }

    static android.graphics.Paint textPaint(float size) {
        android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        p.setColor(Pal.INK);
        p.setTextSize(size);
        p.setFakeBoldText(true);
        p.setTextAlign(android.graphics.Paint.Align.CENTER);
        return p;
    }

    public static void drawCrate(Canvas c, int layers) {
        float o = 3.2f;
        Draw.rrectGrad(c, 6, 10, 94, 94, 8, Pal.WOOD, Pal.WOOD_DARK, o);
        c.drawLine(10, 52, 90, 52, Draw.line(Pal.WOOD_DARK, 4));
        c.drawLine(12, 14, 88, 90, Draw.line(Pal.darker(Pal.WOOD, 0.8f), 5));
        // studs on top show how many hits are left
        float[] xs = layers == 1 ? new float[]{50} : layers == 2 ? new float[]{35, 65} : new float[]{24, 50, 76};
        for (float x : xs) Draw.stud(c, x, 32, 11, 0xFFE8B04A);
    }

    public static void drawOil(Canvas c) {
        Path p = new Path();
        p.moveTo(20, 60);
        p.cubicTo(5, 30, 40, 8, 58, 20);
        p.cubicTo(80, 8, 98, 40, 86, 62);
        p.cubicTo(96, 86, 60, 96, 46, 86);
        p.cubicTo(24, 98, 6, 82, 20, 60);
        p.close();
        Draw.path(c, p, Pal.OIL, 3);
        // rainbow sheen
        int[] sheen = {0x806FD3FF, 0x80B58CFF, 0x80FFD86F};
        for (int k = 0; k < 3; k++) c.drawArc(new RectF(30 + k * 4, 34 + k * 4, 70 - k * 4, 66 - k * 4), 200, 110, false, Draw.line(sheen[k], 3));
        Draw.circle(c, 38, 40, 4, 0x66FFFFFF, 0);
    }

    public static void drawPart(Canvas c) {
        Draw.rrect(c, 8, 74, 92, 92, 4, Pal.WOOD, 3);
        Draw.path(c, Draw.gearPath(50, 46, 34, 27, 9, 0.2f), Pal.GOLD, 3.2f);
        Draw.circle(c, 50, 46, 15, Pal.lighter(Pal.GOLD, 0.35f), 3);
        Draw.star(c, 50, 46, 9, 0xFFFFFFFF, 0);
    }

    public static void drawClamp(Canvas c, int layers) {
        int col = layers >= 2 ? 0xFFFF9F1C : 0xFFFFD23F;
        Draw.rrect(c, 4, 40, 96, 60, 6, col, 3);
        Draw.rrect(c, 40, 4, 60, 96, 6, col, 3);
        Draw.circle(c, 50, 50, 15, Pal.darker(col, 0.8f), 3);
        Draw.circle(c, 50, 50, 6, Pal.INK, 0);
        if (layers >= 2) {
            Draw.rrect(c, 14, 44, 30, 56, 3, Pal.STEEL, 2);
            Draw.rrect(c, 70, 44, 86, 56, 3, Pal.STEEL, 2);
        }
    }
}
