package com.brickyard.app.screens;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;

import com.brickyard.app.art.CharacterArt;
import com.brickyard.app.art.Draw;
import com.brickyard.app.art.Pal;
import com.brickyard.app.game.Game;
import com.brickyard.app.game.Story;
import com.brickyard.app.ui.Button;
import com.brickyard.app.ui.Screen;
import com.brickyard.app.ui.Ui;
import com.brickyard.engine.gears.GearPuzzle;
import com.brickyard.engine.gears.GearTrainSimulator;
import com.brickyard.engine.gears.GearTrainSimulator.Gear;
import com.brickyard.engine.model.Kinds;
import com.brickyard.engine.profile.Progress;

import java.util.ArrayList;
import java.util.List;

/** Gear Fix-It: drag gears from the tray onto the pegboard; they snap, mesh and turn live. */
public final class GearScreen extends Screen {
    private final GearPuzzle p;
    private final List<Gear> placed = new ArrayList<>();
    private GearTrainSimulator.Result sim;
    private float unit, ox, oy;
    private int dragTeeth;           // 0 = not dragging
    private float dragX, dragY;
    private boolean solvedShown;
    private final Button back, reset;
    private final float[] angles = new float[32];

    public GearScreen(final Game g, GearPuzzle p) {
        super(g);
        this.p = p;
        back = new Button("Garage", 0xFFBDB6A8, new Runnable() { @Override public void run() { g.pop(); } });
        reset = new Button("Clear", 0xFFFF8A1F, new Runnable() { @Override public void run() { placed.clear(); simulate(); solvedShown = false; } });
        buttons.add(back);
        buttons.add(reset);
        simulate();
    }

    @Override
    public void enter() {
        g.audio.music("focus");
        if (!g.profile.seen.contains(p.id)) {
            g.profile.seen.add(p.id);
            List<Story.Line> l = new ArrayList<>();
            l.add(new Story.Line(CharacterArt.BO, 1, p.boNote));
            g.say(l, null);
        }
    }

    @Override
    public void layout(float vw, float vh) {
        super.layout(vw, vh);
        unit = Math.min((vw - 460) / p.w, (vh - 240) / p.h);
        ox = 40 + (vw - 460 - unit * p.w) / 2;
        oy = 170 + (vh - 220 - unit * p.h) / 2;
        back.at(24, 20, 180, 80);
        reset.at(vw - 400, vh - 120, 200, 90);
    }

    private void simulate() {
        sim = p.run(placed);
        if (p.solved(placed) && !solvedShown) {
            solvedShown = true;
            Progress.Reward r = g.progress.solveGear(p.id);
            g.save();
            g.audio.play("win");
            List<Story.Line> l = new ArrayList<>();
            l.add(new Story.Line(CharacterArt.JUNO, 3, "It works! " + p.lesson));
            if (r.firstTime) {
                String reward = "Reward: " + r.coins + " coins";
                if (r.tool >= 0) reward += " and a " + Kinds.TOOL_NAMES[r.tool];
                if (r.part != null) reward += ", plus a new brick part: " + g.content.garage.parts.get(r.part).name;
                l.add(new Story.Line(CharacterArt.BO, 3, reward + ". Well reasoned, Chief Engineer."));
            }
            g.say(l, null);
        } else if (sim.jammed) g.audio.play("jam");
    }

    private int trayLeft(int t) {
        int used = 0;
        for (Gear gg : placed) if (gg.teeth == 8 * (t + 1)) used++;
        return p.tray[t] - used;
    }

    private float sx(float x) { return ox + x * unit; }
    private float sy(float y) { return oy + y * unit; }

    private float trayX() { return vw - 400; }
    private float trayY(int t) { return 200 + t * 170; }

    @Override
    public boolean onDown(float x, float y) {
        if (super.onDown(x, y)) return true;
        // pick from the tray
        for (int t = 0; t < 3; t++) {
            if (trayLeft(t) <= 0) continue;
            float tx = trayX() + 90, ty = trayY(t) + 70;
            if (Math.hypot(x - tx, y - ty) < 80) { dragTeeth = 8 * (t + 1); dragX = x; dragY = y; return true; }
        }
        // pick up a placed gear
        for (int k = placed.size() - 1; k >= 0; k--) {
            Gear gg = placed.get(k);
            if (Math.hypot(x - sx(gg.x), y - sy(gg.y)) < gg.radius() * unit) {
                placed.remove(k);
                dragTeeth = gg.teeth;
                dragX = x;
                dragY = y;
                simulate();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onMove(float x, float y) {
        super.onMove(x, y);
        if (dragTeeth > 0) { dragX = x; dragY = y; }
    }

    @Override
    public void onUp(float x, float y) {
        super.onUp(x, y);
        if (dragTeeth == 0) return;
        Gear ghost = snapped();
        if (ghost != null && p.canPlace(placed, ghost) && placed.size() < p.maxPlace + 3) {
            placed.add(ghost);
            g.audio.play("mesh");
        } else g.audio.play("tap", 0.5f, 0.8f);
        dragTeeth = 0;
        simulate();
    }

    private Gear snapped() {
        int gx = Math.round((dragX - ox) / unit), gy = Math.round((dragY - oy) / unit);
        if (gx < 0 || gy < 0 || gx > p.w || gy > p.h) return null;
        return new Gear(gx, gy, dragTeeth);
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        List<Gear> all = p.all(placed);
        for (int i = 0; i < all.size() && i < angles.length; i++) {
            if (sim.jammed || sim.direction.length <= i) continue;
            angles[i] += sim.direction[i] * (float) sim.speed[i] * dt * 1.6f;
        }
    }

    @Override
    public void draw(Canvas c) {
        Draw.rrectGrad(c, 0, 0, vw, vh, 0, 0xFFE8D2A8, 0xFFC9A877, 0);
        Ui.label(c, p.title, 230, 70, 44, Pal.INK, Paint.Align.LEFT);
        Ui.wrap(c, p.prompt, 230, 112, vw - 700, 26, 0xFF5A4A36, Paint.Align.LEFT);
        // pegboard
        Draw.rrect(c, sx(0) - 20, sy(0) - 20, sx(p.w) + 20, sy(p.h) + 20, 16, 0xFFD9B98C, 5);
        for (int x = 0; x <= p.w; x++) for (int y = 0; y <= p.h; y++) Draw.circle(c, sx(x), sy(y), Math.max(2, unit * 0.08f), 0xFF7A5A36, 0);
        for (int[] b : p.blocked) { // toolboxes in the way
            Draw.rrect(c, sx(b[0]), sy(b[1]), sx(b[2]), sy(b[3]), 10, 0xFFC0392B, 4);
            Draw.rrect(c, sx(b[0]) + 10, sy(b[1]) + 10, sx(b[2]) - 10, sy(b[1]) + 24, 6, 0xFFA93226, 2);
        }
        List<Gear> all = p.all(placed);
        // belts between fixed gears
        for (GearTrainSimulator.Belt bt : p.belts) {
            Gear a = all.get(bt.a), b = all.get(bt.b);
            float ra = a.radius() * unit * 0.7f, rb = b.radius() * unit * 0.7f;
            Paint pl = Draw.line(0xFF2B2B2B, 6);
            if (bt.crossed) {
                c.drawLine(sx(a.x), sy(a.y) - ra, sx(b.x), sy(b.y) + rb, pl);
                c.drawLine(sx(a.x), sy(a.y) + ra, sx(b.x), sy(b.y) - rb, pl);
            } else {
                c.drawLine(sx(a.x), sy(a.y) - ra, sx(b.x), sy(b.y) - rb, pl);
                c.drawLine(sx(a.x), sy(a.y) + ra, sx(b.x), sy(b.y) + rb, pl);
            }
        }
        float shake = sim.jammed ? (float) Math.sin(time * 60) * 3 : 0;
        int idx = 0;
        for (GearPuzzle.Fixed f : p.fixed) {
            if (p.isAxle(f)) {
                Paint dash = Draw.line(0xFF2E4F7A, 4);
                dash.setPathEffect(new DashPathEffect(new float[]{10, 8}, time * 20));
                c.drawCircle(sx(f.gear.x), sy(f.gear.y), unit * 1.2f, dash);
                dash.setPathEffect(null);
                Draw.circle(c, sx(f.gear.x), sy(f.gear.y), unit * 0.25f, Pal.INK, 0);
                target(c, f, sx(f.gear.x), sy(f.gear.y) - unit * 3.4f);
                continue;
            }
            int col = f.role.equals("motor") ? 0xFFE03B2E : f.role.equals("output") ? 0xFF2F6FD6 : Pal.STEEL;
            drawGear(c, f.gear, col, idx, shake);
            Ui.center(c, f.label, sx(f.gear.x), sy(f.gear.y) + f.gear.radius() * unit + 34, 24, Pal.INK);
            if (f.role.equals("output")) target(c, f, sx(f.gear.x), sy(f.gear.y) - f.gear.radius() * unit - 50);
            idx++;
        }
        for (Gear gg : placed) { drawGear(c, gg, 0xFFF2B630, idx, shake); idx++; }
        if (dragTeeth > 0) {
            Gear ghost = snapped();
            if (ghost != null) {
                boolean ok = p.canPlace(placed, ghost);
                Draw.circle(c, sx(ghost.x), sy(ghost.y), ghost.radius() * unit, ok ? 0x6634A853 : 0x66E03B2E, 0);
            }
            Draw.gear(c, dragX, dragY, dragTeeth / 8f * unit, dragTeeth, 0, 0xCCF2B630, 3);
        }
        // tray
        Ui.panel(c, trayX() - 20, 150, vw - 20, 690, Pal.PANEL);
        Ui.label(c, "Gear tray", trayX(), 186, 28, Pal.INK, Paint.Align.LEFT);
        for (int t = 0; t < 3; t++) {
            int left = trayLeft(t);
            float tx = trayX() + 90, ty = trayY(t) + 70;
            Draw.gear(c, tx, ty, 22 + t * 18, 8 * (t + 1), time * 0.3f, left > 0 ? 0xFFF2B630 : 0xFFD6CFC0, 3);
            Ui.label(c, (8 * (t + 1)) + " teeth", tx + 90, ty - 6, 28, Pal.INK, Paint.Align.LEFT);
            Ui.label(c, "× " + left, tx + 90, ty + 30, 28, left > 0 ? Pal.TEAL : 0xFF9A9284, Paint.Align.LEFT);
        }
        if (sim.jammed) Ui.wrap(c, "Jammed! These gears are being pushed two ways at once. Try removing one.", ox, vh - 56, vw - 520, 24, Pal.RED_BAD, Paint.Align.LEFT);
        else if (solvedShown) Ui.wrap(c, "Solved! " + p.lesson, ox, vh - 56, vw - 520, 24, Pal.GREEN_OK, Paint.Align.LEFT);
        drawButtons(c);
    }

    private void drawGear(Canvas c, Gear gg, int col, int idx, float shake) {
        float a = idx < angles.length ? angles[idx] : 0;
        Draw.gear(c, sx(gg.x) + shake, sy(gg.y), gg.radius() * unit * 0.98f, gg.teeth, a, col, 3);
    }

    /** What an output must do: a curved arrow for direction, and the speed it needs. */
    private void target(Canvas c, GearPuzzle.Fixed f, float x, float y) {
        String want = "";
        if (f.wantDir == GearTrainSimulator.CW) want = "clockwise";
        else if (f.wantDir == GearTrainSimulator.CCW) want = "anticlockwise";
        if (f.wantSpeed > 0) want += (want.isEmpty() ? "" : ", ") + (f.wantSpeed >= 1 ? "×" + fmt(f.wantSpeed) + " speed" : fmt(f.wantSpeed) + " speed");
        if (want.isEmpty()) want = "any way";
        float w = Ui.width(want, 24) + 30;
        Draw.rrect(c, x - w / 2, y - 30, x + w / 2, y + 8, 12, 0xFFFFF6DC, 3);
        Ui.center(c, want, x, y - 4, 24, Pal.INK);
        if (f.wantDir != 0) {
            Path arc = new Path();
            boolean cw = f.wantDir == GearTrainSimulator.CW;
            arc.addArc(new android.graphics.RectF(x + w / 2 + 6, y - 34, x + w / 2 + 46, y + 6), cw ? 200 : 340, cw ? 270 : -270);
            c.drawPath(arc, Draw.line(Pal.TEAL, 5));
        }
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((int) v) : v == 0.5 ? "½" : String.valueOf(v);
    }
}
