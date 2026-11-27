package com.brickyard.engine.gears;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure mechanical model of the gear-train mini-games.
 *
 * Coordinates are in half-units on a pegboard. A gear's radius in half-units is teeth / 8
 * (8T = 1, 16T = 2, 24T = 3), so two gears mesh exactly when the distance between their axles
 * equals the sum of their radii: dx^2 + dy^2 == (rA + rB)^2 (straight lines and 3-4-5 diagonals).
 * Closer than that and they collide; farther and they do not touch.
 *
 * Meshing reverses direction and scales speed by teethA / teethB. A straight belt keeps direction,
 * a crossed belt reverses it (equal pulleys, so speed is unchanged). A train is jammed when some
 * gear would have to turn two ways at once or at two speeds at once (for example an odd loop of gears).
 */
public final class GearTrainSimulator {
    private GearTrainSimulator() {}

    public static final int CW = 1, CCW = -1, STILL = 0;

    public static final class Gear {
        public final int x, y, teeth;
        public Gear(int x, int y, int teeth) {
            if (teeth != 8 && teeth != 16 && teeth != 24) throw new IllegalArgumentException("gears are 8T, 16T or 24T");
            this.x = x;
            this.y = y;
            this.teeth = teeth;
        }
        public int radius() { return teeth / 8; }
    }

    public static final class Belt {
        public final int a, b;
        public final boolean crossed;
        public Belt(int a, int b, boolean crossed) { this.a = a; this.b = b; this.crossed = crossed; }
    }

    public static final class Result {
        public int[] direction;    // per gear: CW, CCW or STILL
        public double[] speed;     // per gear: turns per motor turn (always >= 0)
        public boolean jammed;
        public final List<int[]> collisions = new ArrayList<>(); // pairs of overlapping gears
        public final List<int[]> meshes = new ArrayList<>();     // pairs that mesh (for drawing teeth contact)
        public int jamGear = -1;
    }

    /** Relationship between two gears: 1 = mesh, 0 = apart, -1 = overlap/collide. */
    public static int relation(Gear a, Gear b) {
        int dx = a.x - b.x, dy = a.y - b.y;
        int d2 = dx * dx + dy * dy;
        int s = a.radius() + b.radius();
        if (d2 == s * s) return 1;
        if (d2 < s * s) return -1;
        return 0;
    }

    /** Runs the train with gears.get(motor) driven clockwise at speed 1. */
    public static Result simulate(List<Gear> gears, List<Belt> belts, int motor) {
        int n = gears.size();
        Result res = new Result();
        res.direction = new int[n];
        res.speed = new double[n];
        List<List<int[]>> adj = new ArrayList<>(); // {other, flip(1 reverse / 0 keep), isBelt}
        for (int i = 0; i < n; i++) adj.add(new ArrayList<int[]>());
        for (int i = 0; i < n; i++)
            for (int j = i + 1; j < n; j++) {
                int rel = relation(gears.get(i), gears.get(j));
                if (rel == 1) {
                    res.meshes.add(new int[]{i, j});
                    adj.get(i).add(new int[]{j, 1, 0});
                    adj.get(j).add(new int[]{i, 1, 0});
                } else if (rel == -1) res.collisions.add(new int[]{i, j});
            }
        if (belts != null) for (Belt bt : belts) {
            adj.get(bt.a).add(new int[]{bt.b, bt.crossed ? 1 : 0, 1});
            adj.get(bt.b).add(new int[]{bt.a, bt.crossed ? 1 : 0, 1});
        }
        if (motor < 0 || motor >= n) return res;
        // Signed angular velocity: + clockwise.
        double[] w = new double[n];
        boolean[] seen = new boolean[n];
        ArrayDeque<Integer> q = new ArrayDeque<>();
        w[motor] = 1;
        seen[motor] = true;
        q.add(motor);
        while (!q.isEmpty()) {
            int i = q.poll();
            for (int[] e : adj.get(i)) {
                int j = e[0];
                double wj;
                if (e[2] == 1) wj = e[1] == 1 ? -w[i] : w[i];
                else wj = -w[i] * gears.get(i).teeth / (double) gears.get(j).teeth;
                if (!seen[j]) {
                    seen[j] = true;
                    w[j] = wj;
                    q.add(j);
                } else if (Math.abs(w[j] - wj) > 1e-9) {
                    res.jammed = true;
                    res.jamGear = j;
                }
            }
        }
        if (!res.collisions.isEmpty()) res.jammed = true;
        for (int i = 0; i < n; i++) {
            if (res.jammed || !seen[i]) { res.direction[i] = STILL; res.speed[i] = 0; continue; }
            res.direction[i] = w[i] > 0 ? CW : CCW;
            res.speed[i] = Math.abs(w[i]);
        }
        return res;
    }
}
