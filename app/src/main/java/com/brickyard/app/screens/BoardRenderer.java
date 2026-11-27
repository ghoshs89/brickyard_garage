package com.brickyard.app.screens;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.brickyard.app.art.Draw;
import com.brickyard.app.art.Pal;
import com.brickyard.app.art.PieceArt;
import com.brickyard.engine.model.Kinds;
import com.brickyard.engine.model.Level;
import com.brickyard.engine.rules.Step;

import java.util.HashMap;
import java.util.Map;

/** Draws a board snapshot, animating entities by uid between two snapshots. */
final class BoardRenderer {
    final Level L;
    float x0, y0, tile;
    private final RectF dst = new RectF();
    private final Paint bmp = new Paint(Paint.FILTER_BITMAP_FLAG);

    BoardRenderer(Level L) {
        this.L = L;
    }

    void layout(float x0, float y0, float tile) {
        this.x0 = x0;
        this.y0 = y0;
        this.tile = tile;
    }

    float cx(int i) { return x0 + (i % L.cols) * tile + tile / 2; }
    float cy(int i) { return y0 + (i / L.cols) * tile + tile / 2; }

    int cellAt(float x, float y) {
        int c = (int) Math.floor((x - x0) / tile), r = (int) Math.floor((y - y0) / tile);
        if (r < 0 || c < 0 || r >= L.rows || c >= L.cols) return -1;
        int i = r * L.cols + c;
        return L.active[i] ? i : -1;
    }

    /** Static parts: cell backgrounds, floor, walls, portals, conveyors, dispensers and exits. */
    void drawBase(Canvas c, Step.Snapshot s, float t) {
        // frame around active cells
        for (int i = 0; i < L.rows * L.cols; i++) {
            if (!L.active[i]) continue;
            float l = x0 + (i % L.cols) * tile, tp = y0 + (i / L.cols) * tile;
            c.drawRect(l - 7, tp - 7, l + tile + 7, tp + tile + 7, Draw.fill(0xFF3A3328));
        }
        for (int i = 0; i < L.rows * L.cols; i++) {
            if (!L.active[i]) continue;
            int r = i / L.cols, col = i % L.cols;
            float l = x0 + col * tile, tp = y0 + r * tile;
            int bg = (r + col) % 2 == 0 ? 0xFF5E6E7A : 0xFF566571;
            c.drawRect(l, tp, l + tile, tp + tile, Draw.fill(bg));
            byte f = s.floor[i];
            if (f == Kinds.FLOOR_TRACK) {
                float h = tile / 4;
                for (int a = 0; a < 4; a++) for (int b = 0; b < 4; b++)
                    c.drawRect(l + a * h, tp + b * h, l + (a + 1) * h, tp + (b + 1) * h, Draw.fill((a + b) % 2 == 0 ? 0xFFF4F1E8 : 0xFF2B2B2B));
            } else if (f == Kinds.FLOOR_RUST) {
                c.drawRect(l + 2, tp + 2, l + tile - 2, tp + tile - 2, Draw.fill(Pal.RUST));
                c.drawCircle(l + tile * 0.3f, tp + tile * 0.35f, tile * 0.12f, Draw.fill(Pal.RUST_DARK));
                c.drawCircle(l + tile * 0.7f, tp + tile * 0.65f, tile * 0.09f, Draw.fill(Pal.RUST_DARK));
                for (float[] rv : new float[][]{{0.12f, 0.12f}, {0.88f, 0.12f}, {0.12f, 0.88f}, {0.88f, 0.88f}})
                    c.drawCircle(l + tile * rv[0], tp + tile * rv[1], tile * 0.04f, Draw.fill(0xFF8C8C8C));
            }
        }
        // conveyors: belt under the cells and arrows toward the next cell
        for (int[] loop : L.conveyors) {
            for (int j = 0; j < loop.length; j++) {
                int a = loop[j], b = loop[(j + 1) % loop.length];
                float ax = cx(a), ay = cy(a), bx = cx(b), by = cy(b);
                c.drawRect(ax - tile / 2 + 3, ay - tile / 2 + 3, ax + tile / 2 - 3, ay + tile / 2 - 3, Draw.line(0xFF2B2B2B, 4));
                float ph = (t * 0.8f) % 1f;
                float mx = ax + (bx - ax) * (0.3f + 0.4f * ph), my = ay + (by - ay) * (0.3f + 0.4f * ph);
                float ang = (float) Math.atan2(by - ay, bx - ax);
                Path arr = new Path();
                float k = tile * 0.13f;
                arr.moveTo(mx + (float) Math.cos(ang) * k, my + (float) Math.sin(ang) * k);
                arr.lineTo(mx + (float) Math.cos(ang + 2.4) * k, my + (float) Math.sin(ang + 2.4) * k);
                arr.lineTo(mx + (float) Math.cos(ang - 2.4) * k, my + (float) Math.sin(ang - 2.4) * k);
                arr.close();
                c.drawPath(arr, Draw.fill(0xCCFFD23F));
            }
        }
        for (int i = 0; i < L.rows * L.cols; i++) {
            if (!L.active[i]) continue;
            float l = x0 + (i % L.cols) * tile, tp = y0 + (i / L.cols) * tile;
            if (L.dispenser[i]) {
                Draw.rrect(c, l + tile * 0.25f, tp - tile * 0.32f, l + tile * 0.75f, tp + tile * 0.05f, 6, Pal.GOLD, 3);
                Draw.circle(c, l + tile / 2, tp - tile * 0.14f, tile * 0.08f, Pal.INK, 0);
            }
            if (L.exit[i]) {
                Path arr = new Path();
                arr.moveTo(l + tile * 0.3f, tp + tile + 4);
                arr.lineTo(l + tile * 0.7f, tp + tile + 4);
                arr.lineTo(l + tile / 2, tp + tile + tile * 0.3f);
                arr.close();
                Draw.path(c, arr, Pal.GREEN_OK, 3);
            }
            if (L.wallBelow[i]) Draw.rrect(c, l - 4, tp + tile - 6, l + tile + 4, tp + tile + 6, 5, Pal.STEEL, 3);
            if (L.wallRight[i]) Draw.rrect(c, l + tile - 6, tp - 4, l + tile + 6, tp + tile + 4, 5, Pal.STEEL, 3);
        }
        for (int k = 0; k < L.portalFrom.length; k++) {
            int a = L.portalFrom[k], b = L.portalTo[k];
            float ax = cx(a), ay = cy(a) + tile / 2, bx = cx(b), by = cy(b) - tile / 2;
            int col = k % 2 == 0 ? 0xFF7FDBFF : 0xFFFF9FF3;
            Draw.rrect(c, ax - tile * 0.3f, ay - 8, ax + tile * 0.3f, ay + 8, 8, col, 3);
            Draw.rrect(c, bx - tile * 0.3f, by - 8, bx + tile * 0.3f, by + 8, 8, col, 3);
            Ui2.number(c, String.valueOf(k + 1), ax, ay + 5, 16);
            Ui2.number(c, String.valueOf(k + 1), bx, by + 5, 16);
        }
    }

    /**
     * Entities of `after`, moved from their place in `before` by uid. p in 0..1. Entities that
     * vanish shrink away at their old cell; new ones drop in from above (or pop in on spawn steps).
     */
    void drawEntities(Canvas c, PieceArt art, Step.Snapshot before, Step.Snapshot after, float p, Step.Type type, float t, int selected) {
        Map<Integer, Integer> prevCell = new HashMap<>();
        if (before != null) for (int i = 0; i < before.uid.length; i++) if (before.uid[i] != 0) prevCell.put(before.uid[i], i);
        float e = ease(p);
        int[] newPerCol = new int[L.cols];
        if (before != null) for (int i = 0; i < after.uid.length; i++)
            if (after.uid[i] != 0 && !prevCell.containsKey(after.uid[i])) newPerCol[i % L.cols]++;
        int[] seenNew = new int[L.cols];
        // vanishing entities
        if (before != null && p < 1) {
            java.util.HashSet<Integer> alive = new java.util.HashSet<>();
            for (int u : after.uid) if (u != 0) alive.add(u);
            for (int i = 0; i < before.uid.length; i++) {
                if (before.uid[i] == 0 || alive.contains(before.uid[i])) continue;
                float sc = 1 - e;
                drawEntity(c, art, before, i, cx(i), cy(i), sc, 255, t, false);
            }
        }
        for (int r = L.rows - 1; r >= 0; r--)
            for (int col = 0; col < L.cols; col++) {
                int i = r * L.cols + col;
                if (!L.active[i] || after.uid[i] == 0) continue;
                float x = cx(i), y = cy(i), sc = 1;
                Integer from = before == null ? null : prevCell.get(after.uid[i]);
                if (from != null && from != i && p < 1) {
                    x = cx(from) + (x - cx(from)) * e;
                    y = cy(from) + (y - cy(from)) * e;
                } else if (from == null && before != null && p < 1) {
                    if (type == Step.Type.GRAVITY_DROP) {
                        seenNew[col]++;
                        float startY = y0 - tile * (newPerCol[col] - seenNew[col] + 1) + tile / 2 - (i / L.cols) * 0;
                        startY = Math.min(startY, y - tile);
                        y = startY + (y - startY) * e;
                    } else sc = e;
                }
                drawEntity(c, art, after, i, x, y, sc, 255, t, i == selected);
            }
    }

    void drawEntity(Canvas c, PieceArt art, Step.Snapshot s, int i, float x, float y, float scale, int alpha, float t, boolean selected) {
        if (scale <= 0.02f) return;
        Bitmap b;
        byte e = s.entity[i];
        if (e == Kinds.PIECE) b = s.power[i] != Kinds.NONE ? art.power(s.power[i]) : art.piece(Math.max(0, s.color[i]));
        else if (e == Kinds.CRATE) b = art.crate(Math.max(1, s.layers[i]));
        else if (e == Kinds.OIL) b = art.oil();
        else if (e == Kinds.PART) b = art.part();
        else return;
        float h = tile * 0.92f * scale / 2;
        float wob = s.power[i] != Kinds.NONE && e == Kinds.PIECE ? (float) Math.sin(t * 5 + i) * tile * 0.03f : 0;
        if (selected) {
            Draw.rrect(c, x - tile / 2 + 3, y - tile / 2 + 3, x + tile / 2 - 3, y + tile / 2 - 3, 12, 0x66FFFFFF, 0);
            h *= 1.08f + 0.04f * (float) Math.sin(t * 10);
        }
        dst.set(x - h, y - h + wob, x + h, y + h + wob);
        bmp.setAlpha(alpha);
        c.drawBitmap(b, null, dst, bmp);
        if (s.clamp[i] > 0) {
            dst.set(x - tile * 0.48f, y - tile * 0.48f, x + tile * 0.48f, y + tile * 0.48f);
            c.drawBitmap(art.clamp(s.clamp[i]), null, dst, bmp);
        }
    }

    void ring(Canvas c, int i, int color, float t, boolean dashed) {
        Paint p = Draw.line(color, 6);
        if (dashed) p.setPathEffect(new DashPathEffect(new float[]{14, 10}, t * 40));
        float r = tile * (0.5f + 0.05f * (float) Math.sin(t * 6));
        c.drawCircle(cx(i), cy(i), r, p);
        p.setPathEffect(null);
    }

    static float ease(float p) {
        p = Math.max(0, Math.min(1, p));
        return p * p * (3 - 2 * p);
    }
}
