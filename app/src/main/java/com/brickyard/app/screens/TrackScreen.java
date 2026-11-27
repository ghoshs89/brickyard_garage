package com.brickyard.app.screens;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import com.brickyard.app.art.CarArt;
import com.brickyard.app.art.Draw;
import com.brickyard.app.art.Pal;
import com.brickyard.app.game.Game;
import com.brickyard.app.ui.Button;
import com.brickyard.app.ui.Screen;
import com.brickyard.app.ui.Ui;
import com.brickyard.engine.cars.BrickVehicleModel;
import com.brickyard.engine.cars.GarageCatalog;

import java.util.Locale;
import java.util.Map;

/**
 * Interactive Test Track: watch your brick car launch across ramps, mud pits and hill climbs,
 * race side-by-side against a translucent Ghost Car of your previous build, honk the horn,
 * and fire turbo flame bursts!
 */
public final class TrackScreen extends Screen {
    private final String vehicle;
    private final GarageCatalog.Track track;
    private final BrickVehicleModel model;
    private final BrickVehicleModel.Run run;
    private final BrickVehicleModel.Run ghostRun;
    private final Map<String, String> ghostEquipped;
    private float t;
    private boolean finished;
    private int coinsWon = -1;
    private double prevBest;
    private int boostLeft;
    private float boostFlameT, honkPopT;
    private final Button again, back, honkBtn, boostBtn;
    private static final float PX_PER_M = 26;

    public TrackScreen(final Game g, String vehicle) {
        this(g, vehicle, defaultTrackId(g, vehicle));
    }

    public TrackScreen(final Game g, String vehicle, String trackId) {
        super(g);
        this.vehicle = vehicle;
        GarageCatalog.Track tr = g.content.garage.tracks.get(trackId);
        if (tr == null) tr = g.content.garage.tracks.values().iterator().next();
        this.track = tr;
        this.model = new BrickVehicleModel(g.content.garage, vehicle, g.progress.equippedFor(vehicle));
        this.run = model.run(track);
        this.boostLeft = Math.max(1, model.boost / 2);

        String key = vehicle + "|" + track.id;
        Map<String, String> prevGhost = g.profile.ghostBuilds.get(key);
        if (prevGhost != null && !prevGhost.isEmpty()) {
            this.ghostEquipped = prevGhost;
            this.ghostRun = new BrickVehicleModel(g.content.garage, vehicle, prevGhost).run(track);
        } else {
            this.ghostEquipped = null;
            this.ghostRun = null;
        }

        Double pb = g.profile.bestTimes.get(key);
        prevBest = pb == null ? 0 : pb;
        again = new Button("Run again", 0xFFE03B2E, new Runnable() { @Override public void run() { g.replace(new TrackScreen(g, TrackScreen.this.vehicle, track.id)); } });
        back = new Button("Back to bench", 0xFFBDB6A8, new Runnable() { @Override public void run() { g.pop(); } });
        honkBtn = new Button("Honk!", 0xFFFFE066, new Runnable() {
            @Override public void run() {
                g.audio.play("horn", 0.9f, 1f);
                honkPopT = 0.7f;
            }
        });
        boostBtn = new Button("Turbo!", 0xFF3FA9F5, new Runnable() {
            @Override public void run() {
                if (boostLeft > 0) {
                    boostLeft--;
                    boostFlameT = 0.9f;
                    g.audio.play("nitro", 0.8f, 1.15f);
                }
            }
        });
        honkBtn.textSize = 26;
        boostBtn.textSize = 26;
        buttons.add(again);
        buttons.add(back);
        buttons.add(honkBtn);
        buttons.add(boostBtn);
        g.audio.play("rev");
    }

    private static String defaultTrackId(Game g, String vehicle) {
        for (GarageCatalog.Track x : g.content.garage.tracks.values()) if (x.vehicle.equals(vehicle)) return x.id;
        return g.content.garage.tracks.keySet().iterator().next();
    }

    @Override
    public void layout(float vw, float vh) {
        super.layout(vw, vh);
        again.at(vw / 2 + 20, vh - 120, 300, 96);
        back.at(vw / 2 - 320, vh - 120, 300, 96);
        honkBtn.at(30, vh - 114, 180, 88);
        boostBtn.at(230, vh - 114, 230, 88);
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        if (boostFlameT > 0) boostFlameT = Math.max(0, boostFlameT - dt);
        if (honkPopT > 0) honkPopT = Math.max(0, honkPopT - dt);
        if (!finished) {
            t += dt;
            if (t >= run.totalSeconds) {
                finished = true;
                coinsWon = g.progress.recordTrackRun(vehicle, track.id, run.medal, run.totalSeconds);
                g.progress.saveGhostBuild(vehicle, track.id);
                g.save();
                g.audio.play(run.medal > 0 ? "win" : "retry");
                if (coinsWon > 0) g.audio.play("coin");
            }
        }
        again.visible = back.visible = finished;
        honkBtn.visible = boostBtn.visible = !finished;
        boostBtn.enabled = boostLeft > 0;
        boostBtn.label = boostLeft > 0 ? "Turbo! (" + boostLeft + ")" : "Turbo empty";
    }

    private static float[] sample(BrickVehicleModel.Run r, float time) {
        float[] a = r.keyframes.get(0);
        for (int k = 1; k < r.keyframes.size(); k++) {
            float[] b = r.keyframes.get(k);
            if (b[0] >= time) {
                float f = (time - a[0]) / Math.max(1e-4f, b[0] - a[0]);
                return new float[]{a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f, a[3] + (b[3] - a[3]) * f};
            }
            a = b;
        }
        float[] last = r.keyframes.get(r.keyframes.size() - 1);
        return new float[]{last[1], last[2], last[3]};
    }

    @Override
    public void draw(Canvas c) {
        Draw.rrectGrad(c, 0, 0, vw, vh, 0, Pal.SKY, 0xFFE9F6FB, 0);
        float[] p = sample(run, Math.min(t, (float) run.totalSeconds));
        float camX = p[0] * PX_PER_M - vw * 0.35f;
        float groundY = vh - 220;
        for (int k = -1; k < 6; k++) {
            float hx = k * 420 - (camX * 0.3f) % 420;
            Draw.circle(c, hx, groundY + 40, 220, 0xFFA7D7A0, 0);
        }
        c.save();
        c.translate(-camX, 0);
        double x = 0, base = 0;
        for (GarageCatalog.Segment s : track.segments) {
            float x0 = (float) (x * PX_PER_M), x1 = (float) ((x + s.length) * PX_PER_M);
            float y0 = groundY - (float) base * PX_PER_M, y1 = s.type.equals("hill") ? y0 - 6 * PX_PER_M : y0;
            Path ground = new Path();
            ground.moveTo(x0, y0);
            if (s.type.equals("ramp")) {
                float xr = x0 + (x1 - x0) * 0.6f;
                ground.lineTo(xr, y0 - 2 * PX_PER_M);
                ground.lineTo(xr, y0);
                ground.lineTo(x1, y0);
            } else ground.lineTo(x1, y1);
            ground.lineTo(x1, vh + 10);
            ground.lineTo(x0, vh + 10);
            ground.close();
            int col = s.type.equals("mud") ? 0xFF6B4A2B : s.type.equals("hill") ? 0xFF7FB069 : s.type.equals("ramp") ? 0xFFE0B13A : 0xFF555555;
            Draw.path(c, ground, col, 4);
            if (s.type.equals("straight")) for (float dx = x0 + 20; dx < x1; dx += 70) c.drawRect(dx, y0 + 30, dx + 36, y0 + 38, Draw.fill(0xFFFFFFFF));
            if (s.type.equals("mud")) for (int k = 0; k < (x1 - x0) / 40; k++) Draw.circle(c, x0 + 20 + k * 40, y0 + 12 + (k % 2) * 6, 8, 0xFF4E3520, 0);
            String name = s.type.equals("ramp") ? "Ramp" : s.type.equals("mud") ? "Mud pit" : s.type.equals("hill") ? "Hill climb" : "Straight";
            Draw.rrect(c, x0 + 10, y0 - 170, x0 + 210, y0 - 110, 10, Pal.PANEL, 3);
            Ui.label(c, name + " " + (int) s.length + " m", x0 + 20, y0 - 130, 26, Pal.INK, Paint.Align.LEFT);
            c.drawLine(x0 + 30, y0 - 110, x0 + 30, y0, Draw.line(Pal.STEEL_DARK, 5));
            x += s.length;
            if (s.type.equals("hill")) base += 6;
        }
        // Finish line + continuing asphalt runoff road so the track never ends in a sky cliff!
        float fx = (float) (x * PX_PER_M), fy = groundY - (float) base * PX_PER_M;
        c.drawRect(fx, fy, fx + 1400, vh + 10, Draw.fill(0xFF555555));
        c.drawLine(fx, fy, fx + 1400, fy, Draw.line(Pal.INK, 4));
        for (float dx = fx + 40; dx < fx + 1350; dx += 70) c.drawRect(dx, fy + 30, dx + 36, fy + 38, Draw.fill(0xFFFFFFFF));
        for (int k = 0; k < 8; k++) c.drawRect(fx, fy - 200 + k * 25, fx + 25, fy - 175 + k * 25, Draw.fill(k % 2 == 0 ? 0xFF000000 : 0xFFFFFFFF));

        int num = g.profile.carNumbers.containsKey(vehicle) ? g.profile.carNumbers.get(vehicle) : 0;
        int stripe = g.profile.carStripes.containsKey(vehicle) ? g.profile.carStripes.get(vehicle) : 0;
        float carW = 260;

        // Ghost Car of previous build racing alongside!
        if (ghostRun != null && ghostEquipped != null) {
            float[] gp = sample(ghostRun, Math.min(t, (float) ghostRun.totalSeconds));
            int layer = c.saveLayerAlpha(gp[0] * PX_PER_M - carW, groundY - gp[1] * PX_PER_M - carW,
                    gp[0] * PX_PER_M + carW, groundY - gp[1] * PX_PER_M + carW, 125);
            c.save();
            c.translate(gp[0] * PX_PER_M, groundY - gp[1] * PX_PER_M - 10);
            c.rotate(gp[2] * 0.6f);
            CarArt.draw(c, g.content.garage, vehicle, ghostEquipped, num, stripe, -carW / 2, -carW * 0.45f, carW, t * 12, t);
            Ui.center(c, "PREV", 0, -carW * 0.48f, 22, 0xFF1E5FB0);
            c.restore();
            c.restoreToCount(layer);
        }

        // Current car
        c.save();
        c.translate(p[0] * PX_PER_M, groundY - p[1] * PX_PER_M);
        c.rotate(p[2] * 0.6f);
        if (boostFlameT > 0) {
            float fl = 40 + boostFlameT * 85;
            Path flame = new Path();
            flame.moveTo(-carW * 0.46f, -26);
            flame.lineTo(-carW * 0.46f - fl, -18);
            flame.lineTo(-carW * 0.46f, -8);
            flame.close();
            Draw.path(c, flame, 0xFF3FA9F5, 0);
            Draw.circle(c, -carW * 0.48f - fl * 0.35f, -18, 12, 0xFFFFE066, 0);
        }
        CarArt.draw(c, g.content.garage, vehicle, g.progress.equippedFor(vehicle), num, stripe, -carW / 2, -carW * 0.45f, carW, t * (boostFlameT > 0 ? 22 : 12), t);
        if (honkPopT > 0) {
            Draw.rrect(c, 50, -carW * 0.62f, 185, -carW * 0.42f, 14, 0xFFFFF6DC, 3);
            Ui.center(c, "HONK!", 118, -carW * 0.48f, 26, Pal.INK);
        }
        c.restore();
        c.restore();

        // HUD: the clock
        Draw.rrect(c, 30, 24, 330, 110, 20, Pal.PANEL, 4);
        Ui.label(c, String.format(Locale.ROOT, "%.1f s", Math.min(t, run.totalSeconds)), 60, 86, 52, Pal.INK, Paint.Align.LEFT);
        Ui.label(c, track.name, vw - 40, 70, 36, Pal.INK, Paint.Align.RIGHT);
        Ui.label(c, String.format(Locale.ROOT, "Gold %.1f s   Silver %.1f s   Bronze %.1f s", track.gold, track.silver, track.bronze), vw - 40, 106, 22, 0xFF5A4A36, Paint.Align.RIGHT);
        if (ghostRun != null && !finished) {
            Ui.label(c, String.format(Locale.ROOT, "Ghost (prev build): %.1f s", ghostRun.totalSeconds), 356, 78, 24, 0xFF1E5FB0, Paint.Align.LEFT);
        }
        if (finished) drawResults(c);
        drawButtons(c);
    }

    private void drawResults(Canvas c) {
        float l = vw / 2 - 500, t0 = 150, r = vw / 2 + 500;
        Ui.panel(c, l, t0, r, vh - 140, Pal.PANEL);
        String[] medal = {"No medal yet", "Bronze!", "Silver!", "GOLD!"};
        int[] mcol = {0xFF8A8072, 0xFFB87333, 0xFF9AA6B2, Pal.GOLD};
        Draw.circle(c, l + 90, t0 + 80, 50, mcol[run.medal], 5);
        if (run.medal > 0) Draw.star(c, l + 90, t0 + 80, 30, 0xFFFFFFFF, 3);
        Ui.label(c, String.format(Locale.ROOT, "%.1f seconds   %s", run.totalSeconds, medal[run.medal]), l + 170, t0 + 96, 44, Pal.INK, Paint.Align.LEFT);
        if (prevBest > 0) {
            double diff = prevBest - run.totalSeconds;
            String s = diff > 0.05 ? String.format(Locale.ROOT, "%.1f s faster than your best (%.1f s)!", diff, prevBest)
                    : diff < -0.05 ? String.format(Locale.ROOT, "Your best is still %.1f s. Try a different part?", prevBest) : "Exactly your best time.";
            Ui.label(c, s, l + 170, t0 + 136, 26, 0xFF5A4A36, Paint.Align.LEFT);
        }
        if (coinsWon > 0) { Draw.coin(c, r - 200, t0 + 80, 22); Ui.label(c, "+" + coinsWon, r - 170, t0 + 92, 36, Pal.INK, Paint.Align.LEFT); }
        float y = t0 + 200;
        Ui.label(c, "Section", l + 50, y, 26, 0xFF8A6A40, Paint.Align.LEFT);
        Ui.label(c, "Length", l + 330, y, 26, 0xFF8A6A40, Paint.Align.LEFT);
        Ui.label(c, "Time", l + 520, y, 26, 0xFF8A6A40, Paint.Align.LEFT);
        Ui.label(c, "Speed", l + 700, y, 26, 0xFF8A6A40, Paint.Align.LEFT);
        for (int k = 0; k < run.segments.size(); k++) {
            double[] s = run.segments.get(k);
            String type = track.segments.get(k).type;
            String name = type.equals("ramp") ? "Ramp" : type.equals("mud") ? "Mud pit" : type.equals("hill") ? "Hill climb" : "Straight";
            float yy = y + 46 + k * 44;
            Ui.label(c, name, l + 50, yy, 28, Pal.INK, Paint.Align.LEFT);
            Ui.label(c, (int) s[1] + " m", l + 330, yy, 28, Pal.INK, Paint.Align.LEFT);
            Ui.label(c, String.format(Locale.ROOT, "%.1f s", s[2]), l + 520, yy, 28, Pal.INK, Paint.Align.LEFT);
            Ui.label(c, String.format(Locale.ROOT, "%.0f m/s", s[3]), l + 700, yy, 28, Pal.INK, Paint.Align.LEFT);
        }
    }
}
