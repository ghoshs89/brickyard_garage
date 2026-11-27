package com.brickyard.engine.cars;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A modular brick vehicle: base stats plus whatever parts are snapped into its slots.
 * Stats are shown as 1-10 bars (Speed, Grip, Boost) and drive the Test Track run.
 */
public final class BrickVehicleModel {
    public final GarageCatalog.Vehicle vehicle;
    public final int speed, grip, boost;

    public BrickVehicleModel(GarageCatalog catalog, String vehicleId, Map<String, String> equipped) {
        this.vehicle = catalog.vehicles.get(vehicleId);
        if (vehicle == null) throw new IllegalArgumentException("unknown vehicle " + vehicleId);
        int s = vehicle.speed, g = vehicle.grip, b = vehicle.boost;
        if (equipped != null) for (String partId : equipped.values()) {
            GarageCatalog.Part p = catalog.parts.get(partId);
            if (p == null) continue;
            s += p.speed;
            g += p.grip;
            b += p.boost;
        }
        speed = clamp(s);
        grip = clamp(g);
        boost = clamp(b);
    }

    public BrickVehicleModel(GarageCatalog.Vehicle v, int speed, int grip, int boost) {
        this.vehicle = v;
        this.speed = clamp(speed);
        this.grip = clamp(grip);
        this.boost = clamp(boost);
    }

    private static int clamp(int x) {
        return Math.max(1, Math.min(10, x));
    }

    // ------------------------------------------------------------------ Test Track

    public static final int NO_MEDAL = 0, BRONZE = 1, SILVER = 2, GOLD = 3;

    public static final class Run {
        public double totalSeconds;              // rounded to 0.1 s, as shown to him
        public final List<double[]> segments = new ArrayList<>(); // {start metres, length, seconds, speed m/s}
        public final List<float[]> keyframes = new ArrayList<>(); // {t, x metres, y metres, angle degrees}
        public int medal;
        public double topSpeed;                  // m/s, for the results card
    }

    /** Segment speed in metres per second from the car's stats. Deterministic, so a run is replayable. */
    public double segmentSpeed(String type) {
        switch (type) {
            case "ramp": return 5 + 1.6 * speed + 0.8 * boost;
            case "mud": return 2.5 + 1.5 * grip + 0.2 * speed;
            case "hill": return 2.5 + 1.2 * boost + 0.6 * grip;
            default: return 6 + 2.4 * speed + 0.3 * boost;
        }
    }

    public Run run(GarageCatalog.Track track) {
        Run run = new Run();
        double x = 0, t = 0, base = 0;
        for (GarageCatalog.Segment seg : track.segments) {
            double v = segmentSpeed(seg.type);
            double dt = seg.length / v;
            run.segments.add(new double[]{x, seg.length, dt, v});
            run.topSpeed = Math.max(run.topSpeed, v);
            // Sample keyframes every 0.05 s along this segment.
            int steps = Math.max(2, (int) Math.ceil(dt / 0.05));
            for (int k = 0; k < steps; k++) {
                double f = k / (double) steps;
                double px = x + f * seg.length, py = base, ang = 0;
                if (seg.type.equals("ramp")) {
                    // Up the ramp for the first 60%, then a jump arc that lands at the base.
                    if (f < 0.6) { py = base + 2.0 * (f / 0.6); ang = -18; }
                    else {
                        double u = (f - 0.6) / 0.4;
                        double peak = 1.0 + 0.15 * boost;
                        py = base + 2.0 * (1 - u) + 4 * peak * u * (1 - u);
                        ang = (float) (-18 + 36 * u);
                    }
                } else if (seg.type.equals("hill")) {
                    py = base + 6.0 * f;
                    ang = -20;
                } else if (seg.type.equals("mud")) {
                    py = base - 0.15 * Math.abs(Math.sin(f * 40));
                }
                run.keyframes.add(new float[]{(float) (t + f * dt), (float) px, (float) py, (float) ang});
            }
            x += seg.length;
            t += dt;
            if (seg.type.equals("hill")) base += 6.0;
        }
        run.keyframes.add(new float[]{(float) t, (float) x, (float) base, 0});
        run.totalSeconds = Math.round(t * 10) / 10.0;
        run.medal = run.totalSeconds <= track.gold ? GOLD : run.totalSeconds <= track.silver ? SILVER
                : run.totalSeconds <= track.bronze ? BRONZE : NO_MEDAL;
        return run;
    }
}
