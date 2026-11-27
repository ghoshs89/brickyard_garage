package com.brickyard.app.screens;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Gear Fix-It & Free-Build Gear Sandbox: drag gears from the tray onto the pegboard;
 * teeth physically interlock, mesh pegs glow green while dragging, outputs display live speed/direction,
 * and tapping a placed gear removes it cleanly.
 */
public final class GearScreen extends Screen {
    private final GearPuzzle p;
    private final boolean sandbox;
    private final List<Gear> placed = new ArrayList<>();
    private final List<GearTrainSimulator.Belt> sandboxBelts = new ArrayList<>();
    private GearTrainSimulator.Result sim;
    private float unit, ox, oy;
    private int dragTeeth;           // 0 = not dragging
    private boolean draggingPlaced;
    private float downX, downY, dragX, dragY;
    private int beltMode;            // sandbox only: 0=off, 1=straight belt, 2=crossed belt
    private int beltFirst = -1;
    private boolean solvedShown;
    private boolean together;
    private int turnPlayer;
    private final Button back, reset, beltBtn, duoBtn;
    private final float[] phase = new float[48];
    private float motorAngle;

    public GearScreen(final Game g, GearPuzzle p) {
        this(g, p, false);
    }

    public static GearScreen sandbox(Game g) {
        GearPuzzle sp = new GearPuzzle();
        sp.id = "gear-sandbox";
        sp.title = "Free-Build Gear Pegboard";
        sp.prompt = "Build your own machine! Snap gears anywhere and connect belts.";
        sp.w = 18;
        sp.h = 10;
        sp.tray = new int[]{12, 12, 12};
        sp.maxPlace = 24;
        sp.fixed.add(new GearPuzzle.Fixed(new Gear(3, 5, 16), "motor", 0, 0, "Motor"));
        return new GearScreen(g, sp, true);
    }

    private GearScreen(final Game g, GearPuzzle p, boolean sandbox) {
        super(g);
        this.p = p;
        this.sandbox = sandbox;
        back = new Button("Garage", 0xFFBDB6A8, new Runnable() { @Override public void run() { g.pop(); } });
        reset = new Button("Clear", 0xFFFF8A1F, new Runnable() {
            @Override public void run() {
                placed.clear();
                sandboxBelts.clear();
                beltFirst = -1;
                simulate();
                solvedShown = false;
            }
        });
        beltBtn = new Button("Belt: Off", 0xFF9FD3E8, new Runnable() {
            @Override public void run() {
                beltMode = (beltMode + 1) % 3;
                beltFirst = -1;
            }
        });
        beltBtn.textSize = 22;
        duoBtn = new Button("Solo", 0xFFE8C89A, new Runnable() {
            @Override public void run() { together = !together; turnPlayer = 0; }
        });
        duoBtn.textSize = 22;
        buttons.add(back);
        buttons.add(reset);
        buttons.add(beltBtn);
        buttons.add(duoBtn);
        simulate();
    }

    @Override
    public void enter() {
        g.audio.music("focus");
        if (!sandbox && !p.boNote.isEmpty() && !g.profile.seen.contains(p.id)) {
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
        back.at(24, 20, 180, 78);
        duoBtn.at(vw - 410, 20, 185, 72);
        beltBtn.at(vw - 210, 20, 190, 72);
        reset.at(vw - 400, vh - 110, 200, 86);
    }

    private List<GearTrainSimulator.Belt> activeBelts() {
        if (!sandbox) return p.belts;
        return sandboxBelts;
    }

    private void simulate() {
        List<Gear> all = p.all(placed);
        sim = GearTrainSimulator.simulate(all, activeBelts(), p.motorIndex());
        computeToothPhases(all);
        if (!sandbox && p.solved(placed) && !solvedShown) {
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

    /** Aligns tooth phases across meshing pairs so teeth interlock cleanly in the valleys. */
    private void computeToothPhases(List<Gear> all) {
        int n = Math.min(all.size(), phase.length);
        boolean[] seen = new boolean[n];
        for (int i = 0; i < n; i++) phase[i] = 0;
        int motor = p.motorIndex();
        if (motor < 0 || motor >= n) return;
        ArrayDeque<Integer> q = new ArrayDeque<>();
        seen[motor] = true;
        q.add(motor);
        while (!q.isEmpty()) {
            int i = q.poll();
            Gear gi = all.get(i);
            for (int[] m : sim.meshes) {
                int j = m[0] == i ? m[1] : m[1] == i ? m[0] : -1;
                if (j < 0 || j >= n || seen[j]) continue;
                Gear gj = all.get(j);
                double theta = Math.atan2(gj.y - gi.y, gj.x - gi.x);
                double contactPhaseI = (theta - phase[i]) * gi.teeth;
                double contactAngleJ = theta + Math.PI;
                // Offset by half a tooth (PI) so gear j's tooth sits inside gear i's valley.
                phase[j] = (float) (contactAngleJ - (-contactPhaseI + Math.PI) / gj.teeth);
                seen[j] = true;
                q.add(j);
            }
        }
    }

    private int trayLeft(int t) {
        if (sandbox) return 9;
        int used = 0;
        for (Gear gg : placed) if (gg.teeth == 8 * (t + 1)) used++;
        return p.tray[t] - used;
    }

    private float sx(float x) { return ox + x * unit; }
    private float sy(float y) { return oy + y * unit; }
    private float trayX() { return vw - 400; }
    private float trayY(int t) { return 200 + t * 165; }

    private int gearAt(float x, float y) {
        List<Gear> all = p.all(placed);
        for (int i = all.size() - 1; i >= 0; i--) {
            Gear gg = all.get(i);
            if (Math.hypot(x - sx(gg.x), y - sy(gg.y)) < gg.radius() * unit * 1.05f) return i;
        }
        return -1;
    }

    @Override
    public boolean onDown(float x, float y) {
        if (super.onDown(x, y)) return true;
        if (sandbox && beltMode > 0) {
            int hit = gearAt(x, y);
            if (hit >= 0) {
                if (beltFirst < 0) beltFirst = hit;
                else if (beltFirst != hit) {
                    sandboxBelts.add(new GearTrainSimulator.Belt(beltFirst, hit, beltMode == 2));
                    beltFirst = -1;
                    g.audio.play("mesh");
                    simulate();
                } else beltFirst = -1;
                return true;
            }
        }
        for (int t = 0; t < 3; t++) {
            if (trayLeft(t) <= 0) continue;
            float tx = trayX() + 90, ty = trayY(t) + 70;
            if (Math.hypot(x - tx, y - ty) < 82) {
                dragTeeth = 8 * (t + 1);
                draggingPlaced = false;
                downX = dragX = x;
                downY = dragY = y;
                return true;
            }
        }
        for (int k = placed.size() - 1; k >= 0; k--) {
            Gear gg = placed.get(k);
            if (Math.hypot(x - sx(gg.x), y - sy(gg.y)) < gg.radius() * unit * 1.05f) {
                placed.remove(k);
                pruneInvalidBelts();
                dragTeeth = gg.teeth;
                draggingPlaced = true;
                downX = dragX = x;
                downY = dragY = y;
                simulate();
                return true;
            }
        }
        return false;
    }

    private void pruneInvalidBelts() {
        if (!sandbox) return;
        int n = p.all(placed).size();
        for (int i = sandboxBelts.size() - 1; i >= 0; i--) {
            GearTrainSimulator.Belt b = sandboxBelts.get(i);
            if (b.a >= n || b.b >= n) sandboxBelts.remove(i);
        }
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
        // Tap on an already-placed gear removes it back to the tray cleanly.
        if (draggingPlaced && Math.hypot(x - downX, y - downY) < 18) {
            dragTeeth = 0;
            draggingPlaced = false;
            g.audio.play("tap", 0.6f, 0.85f);
            simulate();
            return;
        }
        Gear ghost = snapped();
        if (ghost != null && p.canPlace(placed, ghost) && placed.size() < p.maxPlace + 3) {
            placed.add(ghost);
            if (together) turnPlayer = 1 - turnPlayer;
            g.audio.play("mesh");
        } else g.audio.play("tap", 0.5f, 0.8f);
        dragTeeth = 0;
        draggingPlaced = false;
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
        if (!sim.jammed) motorAngle += dt * 1.5f;
        beltBtn.visible = sandbox && !g.talking();
        beltBtn.label = beltMode == 0 ? "Belt: Off" : beltMode == 1 ? "Straight Belt" : "Crossed Belt";
        beltBtn.color = beltMode == 0 ? 0xFFBDB6A8 : 0xFF7FDBFF;
        duoBtn.visible = !g.talking();
        duoBtn.label = together ? "Together: ON" : "Solo";
        duoBtn.color = together ? 0xFF2A9D8F : 0xFFE8C89A;
        reset.visible = !g.talking();
        back.visible = !g.talking();
    }

    @Override
    public void draw(Canvas c) {
        Draw.rrectGrad(c, 0, 0, vw, vh, 0, 0xFFE8D2A8, 0xFFC9A877, 0);
        Ui.label(c, p.title, 230, 68, 42, Pal.INK, Paint.Align.LEFT);
        Ui.wrap(c, p.prompt, 230, 108, vw - 680, 25, 0xFF5A4A36, Paint.Align.LEFT);
        if (together && !g.talking()) {
            String who = turnPlayer == 0 ? g.profile.playerName : g.profile.partnerName;
            int col = turnPlayer == 0 ? 0xFFFF8A1F : 0xFF2A9D8F;
            Draw.rrect(c, ox, 126, ox + 360, 162, 18, col, 3);
            Ui.center(c, "Your turn, " + who + "!", ox + 180, 152, 22, 0xFFFFFFFF);
        }

        // pegboard
        Draw.rrect(c, sx(0) - 20, sy(0) - 20, sx(p.w) + 20, sy(p.h) + 20, 16, 0xFFD9B98C, 5);
        for (int x = 0; x <= p.w; x++) for (int y = 0; y <= p.h; y++) Draw.circle(c, sx(x), sy(y), Math.max(2, unit * 0.08f), 0xFF7A5A36, 0);
        for (int[] b : p.blocked) {
            Draw.rrect(c, sx(b[0]), sy(b[1]), sx(b[2]), sy(b[3]), 10, 0xFFC0392B, 4);
            Draw.rrect(c, sx(b[0]) + 10, sy(b[1]) + 10, sx(b[2]) - 10, sy(b[1]) + 24, 6, 0xFFA93226, 2);
        }

        List<Gear> all = p.all(placed);
        // Highlight valid meshing pegs while dragging a gear (huge help on diagonal 3-4-5 puzzles!).
        if (dragTeeth > 0) {
            int rad = dragTeeth / 8;
            for (int gx = rad; gx <= p.w - rad; gx++) {
                for (int gy = rad; gy <= p.h - rad; gy++) {
                    Gear cand = new Gear(gx, gy, dragTeeth);
                    if (!p.canPlace(placed, cand)) continue;
                    boolean meshes = false;
                    for (Gear ex : all) if (GearTrainSimulator.relation(ex, cand) == 1) { meshes = true; break; }
                    if (meshes) Draw.circle(c, sx(gx), sy(gy), unit * 0.28f, 0x9934A853, 2);
                }
            }
        }

        // belts
        for (GearTrainSimulator.Belt bt : activeBelts()) {
            if (bt.a >= all.size() || bt.b >= all.size()) continue;
            Gear a = all.get(bt.a), b = all.get(bt.b);
            float ra = a.radius() * unit * 0.72f, rb = b.radius() * unit * 0.72f;
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
            if (f.role.equals("motor")) {
                drawDirPill(c, sx(f.gear.x), sy(f.gear.y) - f.gear.radius() * unit - 32, GearTrainSimulator.CW, 1.0, true);
            } else if (f.role.equals("output")) {
                target(c, f, sx(f.gear.x), sy(f.gear.y) - f.gear.radius() * unit - 52);
                if (idx < sim.direction.length && sim.direction[idx] != GearTrainSimulator.STILL) {
                    drawDirPill(c, sx(f.gear.x), sy(f.gear.y) + f.gear.radius() * unit + 64, sim.direction[idx], sim.speed[idx], false);
                }
            }
            idx++;
        }
        for (Gear gg : placed) {
            drawGear(c, gg, 0xFFF2B630, idx, shake);
            if (idx < sim.direction.length && sim.direction[idx] != GearTrainSimulator.STILL) {
                drawDirPill(c, sx(gg.x), sy(gg.y) + gg.radius() * unit + 28, sim.direction[idx], sim.speed[idx], false);
            }
            idx++;
        }
        // Meshing tooth sparks so contact points feel tactile
        for (int[] m : sim.meshes) {
            if (m[0] < all.size() && m[1] < all.size()) {
                Gear a = all.get(m[0]), b = all.get(m[1]);
                float frac = a.radius() / (float) (a.radius() + b.radius());
                float mx = sx(a.x + (b.x - a.x) * frac), my = sy(a.y + (b.y - a.y) * frac);
                Draw.circle(c, mx, my, 5, sim.jammed ? Pal.RED_BAD : 0xFFFFF4A8, 0);
            }
        }
        if (beltFirst >= 0 && beltFirst < all.size()) {
            Gear bg = all.get(beltFirst);
            c.drawCircle(sx(bg.x), sy(bg.y), bg.radius() * unit + 8, Draw.line(0xFF7FDBFF, 5));
        }

        if (dragTeeth > 0) {
            Gear ghost = snapped();
            if (ghost != null) {
                boolean ok = p.canPlace(placed, ghost);
                Draw.circle(c, sx(ghost.x), sy(ghost.y), ghost.radius() * unit, ok ? 0x6634A853 : 0x66E03B2E, 0);
            }
            Draw.gear(c, dragX, dragY, dragTeeth / 8f * unit, dragTeeth, 0, 0xCCF2B630, 3);
        }

        // tray
        Ui.panel(c, trayX() - 20, 146, vw - 20, 680, Pal.PANEL);
        Ui.label(c, "Gear tray", trayX(), 182, 28, Pal.INK, Paint.Align.LEFT);
        for (int t = 0; t < 3; t++) {
            int left = trayLeft(t);
            float tx = trayX() + 90, ty = trayY(t) + 70;
            Draw.gear(c, tx, ty, 20 + t * 16, 8 * (t + 1), time * 0.3f, left > 0 ? 0xFFF2B630 : 0xFFD6CFC0, 3);
            Ui.label(c, (8 * (t + 1)) + " teeth", tx + 90, ty - 6, 28, Pal.INK, Paint.Align.LEFT);
            Ui.label(c, sandbox ? "∞" : ("× " + left), tx + 90, ty + 30, 28, left > 0 ? Pal.TEAL : 0xFF9A9284, Paint.Align.LEFT);
        }
        if (!g.talking()) {
            if (sim.jammed) Ui.wrap(c, "Jammed! These gears are being pushed two ways at once. Tap a gear to remove it.", ox, vh - 56, vw - 520, 24, Pal.RED_BAD, Paint.Align.LEFT);
            else if (solvedShown) Ui.wrap(c, "Solved! " + p.lesson, ox, vh - 56, vw - 520, 24, Pal.GREEN_OK, Paint.Align.LEFT);
            else if (sandbox) Ui.wrap(c, "Tip: tap any placed gear to remove it, or use Belt mode to connect two gears!", ox, vh - 56, vw - 520, 23, 0xFF5A4A36, Paint.Align.LEFT);
        }
        drawButtons(c);
    }

    private void drawGear(Canvas c, Gear gg, int col, int idx, float shake) {
        float dir = idx < sim.direction.length ? sim.direction[idx] : 0;
        float spd = idx < sim.speed.length ? (float) sim.speed[idx] : 0;
        float ph = idx < phase.length ? phase[idx] : 0;
        float a = ph + dir * spd * motorAngle;
        Draw.gear(c, sx(gg.x) + shake, sy(gg.y), gg.radius() * unit, gg.teeth, a, col, 3);
    }

    private void drawDirPill(Canvas c, float x, float y, int dir, double speed, boolean isMotor) {
        String txt = (dir == GearTrainSimulator.CW ? "↻ " : "↺ ") + "×" + fmt(speed);
        float w = Ui.width(txt, 20) + 22;
        Draw.rrect(c, x - w / 2, y - 22, x + w / 2, y + 8, 12, isMotor ? 0xFFFFE5DC : 0xFFE6F4F1, 2.5f);
        Ui.center(c, txt, x, y, 20, isMotor ? Pal.RED_BAD : Pal.TEAL);
    }

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
            arc.addArc(new RectF(x + w / 2 + 6, y - 34, x + w / 2 + 46, y + 6), cw ? 200 : 340, cw ? 270 : -270);
            c.drawPath(arc, Draw.line(Pal.TEAL, 5));
        }
    }

    private static String fmt(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-4) return String.valueOf((int) Math.rint(v));
        if (Math.abs(v - 0.5) < 1e-4) return "½";
        if (Math.abs(v - 1.5) < 1e-4) return "1.5";
        if (Math.abs(v - 1.0 / 3.0) < 1e-3) return "⅓";
        if (Math.abs(v - 2.0 / 3.0) < 1e-3) return "⅔";
        return String.format(Locale.ROOT, "%.1f", v);
    }
}
