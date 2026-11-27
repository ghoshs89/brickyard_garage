package com.brickyard.app.art;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

/** Shared drawing primitives: outlined shapes, brick studs, bevels. All sizes in the caller's units. */
public final class Draw {
    private Draw() {}

    public static final Paint FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
    public static final Paint LINE = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final RectF R = new RectF();

    static {
        FILL.setStyle(Paint.Style.FILL);
        LINE.setStyle(Paint.Style.STROKE);
        LINE.setStrokeJoin(Paint.Join.ROUND);
        LINE.setStrokeCap(Paint.Cap.ROUND);
    }

    public static Paint fill(int color) {
        FILL.setShader(null);
        FILL.setColor(color);
        return FILL;
    }

    public static Paint line(int color, float w) {
        LINE.setShader(null);
        LINE.setColor(color);
        LINE.setStrokeWidth(w);
        return LINE;
    }

    public static void rrect(Canvas c, float l, float t, float r, float b, float rad, int color, float outline) {
        R.set(l, t, r, b);
        c.drawRoundRect(R, rad, rad, fill(color));
        if (outline > 0) c.drawRoundRect(R, rad, rad, line(Pal.INK, outline));
    }

    private static final java.util.LinkedHashMap<Long, LinearGradient> GRAD_CACHE =
            new java.util.LinkedHashMap<Long, LinearGradient>(32, 0.75f, true) {
                @Override protected boolean removeEldestEntry(java.util.Map.Entry<Long, LinearGradient> e) { return size() > 64; }
            };

    public static void rrectGrad(Canvas c, float l, float t, float r, float b, float rad, int top, int bottom, float outline) {
        R.set(l, t, r, b);
        long key = (((long) Float.floatToIntBits(t) * 31L + Float.floatToIntBits(b)) * 31L + top) * 31L + bottom;
        LinearGradient g = GRAD_CACHE.get(key);
        if (g == null) {
            g = new LinearGradient(0, t, 0, b, top, bottom, Shader.TileMode.CLAMP);
            GRAD_CACHE.put(key, g);
        }
        FILL.setShader(g);
        c.drawRoundRect(R, rad, rad, FILL);
        FILL.setShader(null);
        if (outline > 0) c.drawRoundRect(R, rad, rad, line(Pal.INK, outline));
    }

    public static void circle(Canvas c, float x, float y, float rad, int color, float outline) {
        c.drawCircle(x, y, rad, fill(color));
        if (outline > 0) c.drawCircle(x, y, rad, line(Pal.INK, outline));
    }

    public static void path(Canvas c, Path p, int color, float outline) {
        c.drawPath(p, fill(color));
        if (outline > 0) c.drawPath(p, line(Pal.INK, outline));
    }

    /** A cylindrical brick stud seen from above at an angle: rim, top highlight, soft shadow (zero allocations). */
    public static void stud(Canvas c, float x, float y, float rad, int color) {
        c.drawCircle(x + rad * 0.12f, y + rad * 0.18f, rad * 1.02f, fill(Pal.alpha(0xFF000000, 60)));
        c.drawCircle(x, y, rad, fill(Pal.darker(color, 0.78f)));
        c.drawCircle(x, y - rad * 0.08f, rad * 0.86f, fill(color));
        c.drawCircle(x - rad * 0.22f, y - rad * 0.28f, rad * 0.36f, fill(Pal.lighter(color, 0.42f)));
        c.drawCircle(x, y - rad * 0.08f, rad * 0.86f, line(Pal.darker(color, 0.6f), rad * 0.09f));
    }

    /** A brick block with bevelled top edge and studs along the top. */
    public static void brick(Canvas c, float l, float t, float r, float b, int color, int studs, float outline) {
        float h = b - t;
        rrectGrad(c, l, t, r, b, h * 0.12f, Pal.lighter(color, 0.12f), Pal.darker(color, 0.85f), outline);
        c.drawRect(l + outline, t + outline, r - outline, t + h * 0.14f, fill(Pal.lighter(color, 0.3f)));
        if (studs > 0) {
            float w = (r - l) / studs;
            float sr = Math.min(w * 0.3f, h * 0.22f);
            for (int k = 0; k < studs; k++) stud(c, l + w * (k + 0.5f), t - sr * 0.15f, sr, color);
        }
    }

    public static void star(Canvas c, float x, float y, float rad, int color, float outline) {
        Path p = new Path();
        for (int k = 0; k < 10; k++) {
            double a = -Math.PI / 2 + k * Math.PI / 5;
            float rr = k % 2 == 0 ? rad : rad * 0.45f;
            float px = x + (float) Math.cos(a) * rr, py = y + (float) Math.sin(a) * rr;
            if (k == 0) p.moveTo(px, py); else p.lineTo(px, py);
        }
        p.close();
        path(c, p, color, outline);
    }

    public static void coin(Canvas c, float x, float y, float rad) {
        circle(c, x, y + rad * 0.12f, rad, Pal.GOLD_DARK, 0);
        circle(c, x, y, rad, Pal.GOLD, rad * 0.12f);
        circle(c, x, y, rad * 0.62f, Pal.lighter(Pal.GOLD, 0.25f), 0);
        stud(c, x, y, rad * 0.4f, Pal.GOLD);
    }

    /** Gear outline path centred at (x,y). */
    public static Path gearPath(float x, float y, float rOuter, float rInner, int teeth, float angle) {
        Path p = new Path();
        int n = teeth * 4;
        for (int k = 0; k <= n; k++) {
            double a = angle + k * 2 * Math.PI / n;
            int phase = k % 4;
            float rr = (phase == 1 || phase == 2) ? rOuter : rInner;
            float px = x + (float) Math.cos(a) * rr, py = y + (float) Math.sin(a) * rr;
            if (k == 0) p.moveTo(px, py); else p.lineTo(px, py);
        }
        p.close();
        return p;
    }

    public static void gear(Canvas c, float x, float y, float pitchRad, int teeth, float angle, int color, float outline) {
        // Addendum extends past pitchRad and dedendum dips inside so meshing gears visibly interlock.
        float toothH = Math.min(pitchRad * 0.22f, (float) (2.1 * Math.PI * pitchRad / (teeth * 2.4)));
        Path p = gearPath(x, y, pitchRad + toothH * 0.55f, pitchRad - toothH * 0.75f, teeth, angle);
        path(c, p, color, outline);
        circle(c, x, y, pitchRad * 0.56f, Pal.lighter(color, 0.2f), outline * 0.8f);
        circle(c, x, y, pitchRad * 0.18f, Pal.INK, 0);
        // spokes show rotation clearly
        for (int k = 0; k < 3; k++) {
            double a = angle + k * 2 * Math.PI / 3;
            c.drawLine(x, y, x + (float) Math.cos(a) * pitchRad * 0.5f, y + (float) Math.sin(a) * pitchRad * 0.5f, line(Pal.darker(color, 0.6f), Math.max(2f, pitchRad * 0.08f)));
        }
    }

    public static void shadowEllipse(Canvas c, float x, float y, float w, float h) {
        R.set(x - w / 2, y - h / 2, x + w / 2, y + h / 2);
        c.drawOval(R, fill(Pal.SHADOW));
    }
}
