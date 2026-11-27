package com.brickyard.tools;

import com.brickyard.engine.gears.GearPuzzle;
import com.brickyard.engine.gears.GearTrainSimulator;
import com.brickyard.engine.gears.GearTrainSimulator.Gear;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Proves each gear puzzle is solvable with the tray it gives, and measures how many placements
 * solve it. Only positions that mesh with an existing gear are tried (a gear touching nothing
 * cannot change the outcome), which keeps the search small.
 */
public final class GearVerifier {
    public static final class Result {
        public boolean solvable;
        public int minGears = -1;
        public List<int[]> witness = new ArrayList<>();
        public int solutionsAtMin;
        public boolean solvedWithoutPlacing;
    }

    public static Result verify(GearPuzzle p) {
        Result res = new Result();
        res.solvedWithoutPlacing = p.solved(new ArrayList<Gear>());
        for (int k = 1; k <= p.maxPlace && !res.solvable; k++) {
            Set<String> seen = new HashSet<>();
            search(p, new ArrayList<Gear>(), p.tray.clone(), k, res, seen);
            if (res.solutionsAtMin > 0) { res.solvable = true; res.minGears = k; }
        }
        return res;
    }

    private static void search(GearPuzzle p, List<Gear> placed, int[] tray, int left, Result res, Set<String> seen) {
        if (left == 0) {
            if (p.solved(placed)) {
                String key = key(placed);
                if (seen.add("S" + key)) {
                    if (res.witness.isEmpty()) for (Gear g : placed) res.witness.add(new int[]{g.x, g.y, g.teeth});
                    res.solutionsAtMin++;
                }
            }
            return;
        }
        if (!seen.add(key(placed))) return;
        List<Gear> existing = p.all(placed);
        for (int t = 0; t < 3; t++) {
            if (tray[t] <= 0) continue;
            int teeth = 8 * (t + 1);
            Set<Long> tried = new HashSet<>();
            for (Gear e : existing) {
                int s = e.radius() + teeth / 8;
                for (int dx = -s; dx <= s; dx++)
                    for (int dy = -s; dy <= s; dy++) {
                        if (dx * dx + dy * dy != s * s) continue;
                        int x = e.x + dx, y = e.y + dy;
                        if (!tried.add(((long) x << 32) ^ (y & 0xffffffffL))) continue;
                        Gear g = new Gear(x, y, teeth);
                        if (!p.canPlace(placed, g)) continue;
                        placed.add(g);
                        tray[t]--;
                        search(p, placed, tray, left - 1, res, seen);
                        tray[t]++;
                        placed.remove(placed.size() - 1);
                    }
            }
        }
    }

    private static String key(List<Gear> placed) {
        List<String> k = new ArrayList<>();
        for (Gear g : placed) k.add(g.x + ":" + g.y + ":" + g.teeth);
        k.sort(null);
        return String.join(",", k);
    }

    public static boolean replay(GearPuzzle p, List<int[]> witness) {
        List<Gear> placed = new ArrayList<>();
        for (int[] w : witness) {
            Gear g = new Gear(w[0], w[1], w[2]);
            if (!p.canPlace(placed, g)) return false;
            placed.add(g);
        }
        GearTrainSimulator.Result r = p.run(placed);
        return !r.jammed && p.solved(placed);
    }
}
