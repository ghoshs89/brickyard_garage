package com.brickyard.app.art;

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * Grandpa Bo, Juno and Sprocket, drawn from simple shapes in a 200 x 260 box (portrait bust).
 * Moods change eyes and mouth: 0 happy, 1 thinking, 2 surprised, 3 proud.
 */
public final class CharacterArt {
    private CharacterArt() {}

    public static final int BO = 0, JUNO = 1, SPROCKET = 2;
    public static final String[] NAMES = {"Grandpa Bo", "Juno", "Sprocket"};

    /** Draws a character with its box's top-left at (x, y), scaled so the box is `h` tall. */
    public static void draw(Canvas c, int who, float x, float y, float h, int mood, float t) {
        c.save();
        c.translate(x, y);
        float s = h / 260f;
        c.scale(s, s);
        float bob = (float) Math.sin(t * 2.2) * 2;
        c.translate(0, bob);
        switch (who) {
            case BO: bo(c, mood, t); break;
            case JUNO: juno(c, mood, t); break;
            default: sprocket(c, mood, t);
        }
        c.restore();
    }

    private static void eyes(Canvas c, float lx, float rx, float y, int mood, float t) {
        boolean blink = (t % 4f) > 3.85f;
        for (float ex : new float[]{lx, rx}) {
            if (blink) c.drawLine(ex - 6, y, ex + 6, y, Draw.line(Pal.INK, 3.5f));
            else if (mood == 2) Draw.circle(c, ex, y, 7, Pal.INK, 0);
            else if (mood == 3) c.drawArc(new RectF(ex - 7, y - 5, ex + 7, y + 7), 200, 140, false, Draw.line(Pal.INK, 4));
            else {
                Draw.circle(c, ex, y, 5.5f, Pal.INK, 0);
                Draw.circle(c, ex - 1.8f, y - 2, 1.8f, 0xFFFFFFFF, 0);
            }
        }
    }

    private static void mouth(Canvas c, float x, float y, float w, int mood) {
        if (mood == 2) Draw.circle(c, x, y + 4, w * 0.25f, Pal.INK, 0);
        else if (mood == 1) c.drawLine(x - w * 0.4f, y + 4, x + w * 0.4f, y + 1, Draw.line(Pal.INK, 4));
        else c.drawArc(new RectF(x - w / 2, y - w * 0.3f, x + w / 2, y + w * 0.4f), 20, 140, false, Draw.line(Pal.INK, 4));
    }

    private static void bo(Canvas c, int mood, float t) {
        int skin = 0xFFE8B48C, denim = 0xFF3B5B92, beard = 0xFFE4E4E4;
        // body: denim overalls over a plaid shirt
        Path body = new Path();
        body.moveTo(30, 260); body.cubicTo(30, 190, 60, 170, 100, 170); body.cubicTo(140, 170, 170, 190, 170, 260); body.close();
        Draw.path(c, body, 0xFFC0392B, 4);
        for (int k = 0; k < 6; k++) c.drawLine(40 + k * 24, 185, 40 + k * 24, 260, Draw.line(0x55FFFFFF, 3));
        Draw.rrect(c, 62, 200, 138, 260, 10, denim, 4);
        Draw.circle(c, 72, 206, 5, Pal.GOLD, 2);
        Draw.circle(c, 128, 206, 5, Pal.GOLD, 2);
        Draw.rrect(c, 88, 224, 112, 240, 3, Pal.lighter(denim, 0.2f), 2);
        // head
        Draw.circle(c, 100, 112, 52, skin, 4);
        // beard
        Path b = new Path();
        b.moveTo(52, 112); b.cubicTo(52, 175, 148, 175, 148, 112); b.cubicTo(140, 140, 60, 140, 52, 112); b.close();
        Draw.path(c, b, beard, 4);
        Draw.rrect(c, 78, 128, 122, 140, 6, beard, 3); // moustache
        eyes(c, 82, 118, 104, mood, t);
        c.drawLine(72, 90, 92, 92, Draw.line(beard, 6));
        c.drawLine(108, 92, 128, 90, Draw.line(beard, 6));
        mouth(c, 100, 146, 22, mood);
        Draw.circle(c, 100, 120, 9, Pal.darker(skin, 0.9f), 3); // nose
        // cap with magnifying visor
        Path cap = new Path();
        cap.moveTo(46, 92); cap.cubicTo(46, 40, 154, 40, 154, 92); cap.close();
        Draw.path(c, cap, 0xFF2E4F7A, 4);
        Draw.rrect(c, 40, 86, 160, 98, 6, 0xFF203A5C, 4);
        Draw.circle(c, 132, 70, 14, 0x9969C8F0, 4);
        c.drawLine(122, 80, 112, 88, Draw.line(Pal.INK, 4));
    }

    private static void juno(Canvas c, int mood, float t) {
        int skin = 0xFFC98B5E, hair = 0xFF3A2416, vest = 0xFF2A9D8F;
        Path body = new Path();
        body.moveTo(40, 260); body.cubicTo(40, 195, 66, 178, 100, 178); body.cubicTo(134, 178, 160, 195, 160, 260); body.close();
        Draw.path(c, body, 0xFFF4D35E, 4);
        // brick-belt utility vest
        Draw.rrect(c, 52, 190, 86, 260, 8, vest, 4);
        Draw.rrect(c, 114, 190, 148, 260, 8, vest, 4);
        for (int k = 0; k < 4; k++) Draw.brick(c, 50 + k * 26, 236, 72 + k * 26, 252, Pal.PIECE[k], 1, 2);
        // neck connecting shoulders to head
        Draw.rrect(c, 84, 152, 116, 188, 10, Pal.darker(skin, 0.92f), 4);
        // ponytail
        Draw.circle(c, 150, 92, 22, hair, 4);
        Draw.circle(c, 100, 110, 50, skin, 4);
        Path h = new Path();
        h.moveTo(50, 108); h.cubicTo(48, 50, 152, 50, 150, 108); h.cubicTo(130, 78, 80, 76, 50, 108); h.close();
        Draw.path(c, h, hair, 4);
        eyes(c, 83, 117, 112, mood, t);
        mouth(c, 100, 136, 24, mood);
        Draw.circle(c, 74, 126, 6, 0x55E06666, 0);
        Draw.circle(c, 126, 126, 6, 0x55E06666, 0);
        // pencil behind the ear
        c.drawLine(56, 92, 40, 70, Draw.line(Pal.GOLD, 6));
    }

    private static void sprocket(Canvas c, int mood, float t) {
        int fur = 0xFFD9B98C, furDark = 0xFF9C7A50;
        Path body = new Path();
        body.moveTo(50, 260); body.cubicTo(44, 200, 70, 184, 100, 184); body.cubicTo(130, 184, 156, 200, 150, 260); body.close();
        Draw.path(c, body, fur, 4);
        // neck tuft behind bandana
        Draw.rrect(c, 82, 162, 118, 194, 10, fur, 3);
        // red bandana
        Path band = new Path();
        band.moveTo(62, 186); band.lineTo(138, 186); band.lineTo(100, 222); band.close();
        Draw.path(c, band, Pal.RED_BAD, 4);
        Draw.circle(c, 88, 196, 3, 0xFFFFFFFF, 0);
        Draw.circle(c, 110, 200, 3, 0xFFFFFFFF, 0);
        // floppy ears that wag
        float wag = (float) Math.sin(t * 6) * 6;
        Path le = new Path();
        le.moveTo(58, 96); le.cubicTo(30, 100, 28 + wag, 150, 50, 160); le.cubicTo(62, 140, 66, 118, 70, 104); le.close();
        Draw.path(c, le, furDark, 4);
        Path re = new Path();
        re.moveTo(142, 96); re.cubicTo(170, 100, 172 - wag, 150, 150, 160); re.cubicTo(138, 140, 134, 118, 130, 104); re.close();
        Draw.path(c, re, furDark, 4);
        Draw.circle(c, 100, 124, 50, fur, 4);
        // scruffy tufts
        for (int k = 0; k < 5; k++) Draw.circle(c, 76 + k * 12, 78 + (k % 2) * 4, 9, fur, 0);
        Draw.rrect(c, 74, 132, 126, 168, 18, Pal.lighter(fur, 0.35f), 4); // muzzle
        Draw.circle(c, 100, 140, 9, Pal.INK, 0); // nose
        eyes(c, 82, 118, 116, mood, t);
        mouth(c, 100, 156, 18, mood);
        if (mood != 1) {
            Path tongue = new Path();
            tongue.addRoundRect(new RectF(94, 160, 106, 176), 6, 6, Path.Direction.CW);
            Draw.path(c, tongue, 0xFFE8707A, 2.5f);
        }
    }
}
