package com.brickyard.app.ui;

import android.graphics.Canvas;
import android.graphics.RectF;

import com.brickyard.app.art.Draw;
import com.brickyard.app.art.Pal;

/** A big, chunky, brick-styled button. Children's fingers: everything is at least 80 units tall. */
public class Button {
    public final RectF r = new RectF();
    public String label;
    public int color;
    public Runnable action;
    public boolean enabled = true;
    public boolean visible = true;
    public boolean pressed;
    public String badge;     // small text in a corner bubble (e.g. counts)
    public Icon icon;
    public float textSize = 34;

    public interface Icon {
        void draw(Canvas c, float x, float y, float size);
    }

    public Button(String label, int color, Runnable action) {
        this.label = label;
        this.color = color;
        this.action = action;
    }

    public Button at(float l, float t, float w, float h) {
        r.set(l, t, l + w, t + h);
        return this;
    }

    public boolean hit(float x, float y) {
        return visible && r.contains(x, y);
    }

    public void draw(Canvas c) {
        if (!visible) return;
        int col = enabled ? color : 0xFFBDB6A8;
        float dy = pressed ? 5 : 0;
        Draw.rrect(c, r.left, r.top + 8, r.right, r.bottom + 8, 22, Pal.darker(col, 0.62f), 0);
        Draw.rrectGrad(c, r.left, r.top + dy, r.right, r.bottom + dy, 22, Pal.lighter(col, 0.18f), col, 5);
        // studs along the top edge make every button a brick
        int studs = Math.max(1, (int) (r.width() / 70));
        float sw = r.width() / studs;
        for (int k = 0; k < studs; k++) Draw.stud(c, r.left + sw * (k + 0.5f), r.top + dy + 2, Math.min(11, r.height() * 0.12f), col);
        float tx = r.centerX();
        if (icon != null) {
            float isz = r.height() * 0.62f;
            if (label == null || label.isEmpty()) icon.draw(c, r.centerX() - isz / 2, r.centerY() - isz / 2 + dy + 4, isz);
            else {
                icon.draw(c, r.left + 14, r.centerY() - isz / 2 + dy + 4, isz);
                tx = r.centerX() + isz * 0.45f;
            }
        }
        if (label != null && !label.isEmpty()) {
            int tc = enabled ? (isLight(col) ? Pal.INK : 0xFFFFFFFF) : 0xFF7A7468;
            float size = textSize;
            float maxW = r.width() - (icon != null ? r.height() * 0.8f : 0) - 24;
            while (Ui.width(label, size) > maxW && size > 16) size -= 2;
            Ui.center(c, label, tx, r.centerY() + size * 0.36f + dy + 4, size, tc);
        }
        if (badge != null) {
            float bx = r.right - 6, by = r.top + 6;
            float bw = Math.max(38, Ui.width(badge, 24) + 20);
            Draw.rrect(c, bx - bw / 2, by - 20, bx + bw / 2, by + 20, 20, Pal.RED_BAD, 4);
            Ui.center(c, badge, bx, by + 9, 24, 0xFFFFFFFF);
        }
    }

    static boolean isLight(int col) {
        int r = (col >> 16) & 0xFF, g = (col >> 8) & 0xFF, b = col & 0xFF;
        return 0.299 * r + 0.587 * g + 0.114 * b > 170;
    }
}
