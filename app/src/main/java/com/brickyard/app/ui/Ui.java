package com.brickyard.app.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

import com.brickyard.app.art.Draw;
import com.brickyard.app.art.Pal;

import java.util.ArrayList;
import java.util.List;

/** Text and widget helpers. All coordinates are virtual units (the screen is ~1280-1600 x 800). */
public final class Ui {
    private Ui() {}

    private static final Paint T = new Paint(Paint.ANTI_ALIAS_FLAG);

    static {
        T.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    }

    public static Paint text(float size, int color, Paint.Align align) {
        T.setTextSize(size);
        T.setColor(color);
        T.setTextAlign(align);
        T.clearShadowLayer();
        return T;
    }

    public static void label(Canvas c, String s, float x, float y, float size, int color, Paint.Align align) {
        c.drawText(s, x, y, text(size, color, align));
    }

    public static void center(Canvas c, String s, float x, float y, float size, int color) {
        label(c, s, x, y, size, color, Paint.Align.CENTER);
    }

    public static float width(String s, float size) {
        return text(size, 0, Paint.Align.LEFT).measureText(s);
    }

    /** Word-wraps text into a box; returns the y after the last line. */
    public static float wrap(Canvas c, String s, float x, float y, float maxW, float size, int color, Paint.Align align) {
        // Measure first: lines() reuses the shared Paint, so the drawing paint is set up afterwards.
        List<String> ls = lines(s, maxW, size);
        Paint p = text(size, color, align);
        float lineH = size * 1.28f;
        for (String line : ls) {
            if (c != null) c.drawText(line, x, y, p);
            y += lineH;
        }
        return y;
    }

    public static List<String> lines(String s, float maxW, float size) {
        Paint p = text(size, 0, Paint.Align.LEFT);
        List<String> out = new ArrayList<>();
        for (String para : s.split("\n")) {
            StringBuilder line = new StringBuilder();
            for (String w : para.split(" ")) {
                String trial = line.length() == 0 ? w : line + " " + w;
                if (p.measureText(trial) > maxW && line.length() > 0) {
                    out.add(line.toString());
                    line = new StringBuilder(w);
                } else line = new StringBuilder(trial);
            }
            out.add(line.toString());
        }
        return out;
    }

    public static void panel(Canvas c, RectF r, int color) {
        Draw.rrect(c, r.left + 6, r.top + 8, r.right + 6, r.bottom + 8, 26, Pal.SHADOW, 0);
        Draw.rrect(c, r.left, r.top, r.right, r.bottom, 26, color, 5);
    }

    public static void panel(Canvas c, float l, float t, float r, float b, int color) {
        panel(c, new RectF(l, t, r, b), color);
    }

    /** Horizontal progress bar with an optional golden "best" marker (the Closest-Yet ghost). */
    public static void bar(Canvas c, float l, float t, float r, float b, float frac, int color, float best) {
        Draw.rrect(c, l, t, r, b, (b - t) / 2, 0xFFE6DCC8, 4);
        float w = (r - l - 8) * Math.max(0, Math.min(1, frac));
        if (w > 4) Draw.rrect(c, l + 4, t + 4, l + 4 + w, b - 4, (b - t - 8) / 2, color, 0);
        if (best > 0) {
            float bx = l + 4 + (r - l - 8) * Math.min(1, best);
            c.drawLine(bx, t - 10, bx, b + 10, Draw.line(Pal.GOLD_DARK, 6));
            Draw.star(c, bx, t - 14, 12, Pal.GOLD, 3);
        }
    }

    public static void statBar(Canvas c, String name, int value, int delta, float x, float y, float w) {
        label(c, name, x, y + 22, 26, Pal.INK, Paint.Align.LEFT);
        float bx = x + 120, cell = (w - 120) / 10f;
        for (int k = 0; k < 10; k++) {
            int col = k < value ? (k >= value - Math.max(0, delta) && delta > 0 ? Pal.GREEN_OK : 0xFFFF8A1F) : 0xFFE6DCC8;
            if (delta < 0 && k >= value && k < value - delta) col = 0xFFF2A49C;
            Draw.brick(c, bx + k * cell + 2, y + 6, bx + (k + 1) * cell - 2, y + 30, col, 1, 2.5f);
        }
    }
}
