package com.brickyard.tools;

import com.brickyard.engine.model.Board;
import com.brickyard.engine.model.Kinds;
import com.brickyard.engine.model.Level;
import com.brickyard.engine.rules.MatchFinder;
import com.brickyard.engine.util.Json;
import com.brickyard.engine.util.Rng;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

/**
 * Searches seeded candidate boards for Blueprint Puzzles and keeps one only when the solver proves:
 *   1. it can be won in exactly N moves and NOT in N-1 (the move count is tight),
 *   2. a half-random, half-greedy player wins it less than 25% of the time,
 *   3. at most about a third of the first moves still lead to a win (there is a "key move").
 * Accepted boards are written to content/blueprints with their witness line.
 */
public final class BlueprintForge {
    static final class Spec {
        final int k, rows, cols, colors, depth;
        final String obstacle; // CRATE1, CRATE2, OIL, CLAMP1, CLAMP2, RUST, MIX
        final int count, powers;
        final String title, bo, lesson;
        Spec(int k, int rows, int cols, int colors, int depth, String obstacle, int count, int powers, String title, String bo, String lesson) {
            this.k = k; this.rows = rows; this.cols = cols; this.colors = colors; this.depth = depth; this.obstacle = obstacle;
            this.count = count; this.powers = powers; this.title = title; this.bo = bo; this.lesson = lesson;
        }
    }

    static final Spec[] SPECS = {
        new Spec(1, 6, 6, 4, 2, "CRATE1", 3, 0, "Two-Step Crates", "Two moves, three crates. Look before you swipe.", "The first move can set up the second."),
        new Spec(2, 6, 6, 4, 2, "CRATE1", 4, 1, "Nitro Shortcut", "There's a Nitro on the board already. Where does it do the most good?", "Power-ups are best saved for the right line."),
        new Spec(3, 6, 6, 4, 3, "CLAMP1", 4, 0, "Clamp Puzzle", "Clamped pieces can't move, but they can still match.", "Bring the pieces to the clamps."),
        new Spec(4, 6, 6, 4, 3, "CRATE2", 3, 1, "Golden Engine Blueprint", "This is how I designed the Golden Piston. Three moves, not one more.", "Hit the same crate twice by planning two matches beside it."),
        new Spec(5, 6, 6, 4, 3, "OIL", 3, 0, "Oil Check", "Oil spreads if you miss. Every move has to clean some.", "Order matters: clean the spill that would spread first."),
        new Spec(6, 6, 6, 4, 3, "RUST", 6, 0, "Rust Map", "Rust only goes when the piece on top goes.", "Make matches that sit right on the rust."),
        new Spec(7, 6, 6, 4, 3, "MIX", 4, 1, "Crates and Clamps", "Two kinds of job. Which one first?", "Choosing the order is part of the plan."),
        new Spec(8, 6, 6, 4, 3, "OIL", 4, 1, "Clockwork Blueprint", "The Clockwork Hybrid ran on exactly this trick. Find the key move.", "One key move unlocks the rest."),
        new Spec(9, 6, 6, 4, 3, "CRATE2", 4, 2, "Combo Crates", "Two power-ups side by side... what happens if they meet?", "Swapping two power-ups makes a combo."),
        new Spec(10, 6, 6, 4, 3, "RUST", 8, 1, "Polish Plan", "Watch the refill queue at the top. You know what's coming!", "The visible queue lets you plan the cascade."),
        new Spec(11, 6, 6, 4, 4, "CLAMP2", 3, 1, "Double Clamps", "Double clamps need two hits each. Count carefully.", "Count the hits each obstacle needs."),
        new Spec(12, 6, 6, 4, 4, "MIX", 4, 1, "Fusion Core Blueprint", "My hardest blueprint. Four moves. Take as long as you like.", "Big plans are many small plans in a row."),
    };

    public static void forgeAll(Path contentDir, List<String> only) throws IOException {
        Path out = contentDir.resolve("blueprints");
        Files.createDirectories(out);
        for (Spec s : SPECS) {
            String id = String.format("bp-%02d", s.k);
            if (!only.isEmpty() && !only.contains(id)) continue;
            long t0 = System.currentTimeMillis();
            Map<String, Object> found = search(s, id);
            if (found == null) { System.out.println(id + ": NOT FOUND"); continue; }
            Files.write(out.resolve(id + ".json"), (Json.pretty(found) + "\n").getBytes(StandardCharsets.UTF_8));
            System.out.printf("%s: forged in %d ms (seed %s)%n", id, System.currentTimeMillis() - t0, found.get("seed"));
        }
    }

    static Map<String, Object> search(Spec s, String id) {
        // Deterministic: seeds are tried in chunks and the lowest accepted seed wins.
        for (long base = 0; base < 40000; base += 256) {
            java.util.concurrent.ConcurrentSkipListMap<Long, Map<String, Object>> ok = new java.util.concurrent.ConcurrentSkipListMap<>();
            LongStream.range(base, base + 256).parallel().forEach(seed -> {
                Map<String, Object> m = tryCandidate(s, id, seed);
                if (m != null) ok.put(seed, m);
            });
            if (!ok.isEmpty()) return ok.firstEntry().getValue();
        }
        return null;
    }

    static Map<String, Object> tryCandidate(Spec s, String id, long seed) {
        {
            Map<String, Object> m = candidate(s, id, seed);
            if (m == null) return null;
            Level L;
            try { L = Level.fromMap(m); } catch (RuntimeException e) { return null; }
            Board b;
            try { b = Board.createUnchecked(L, L.seed); } catch (RuntimeException e) { return null; }
            if (MatchFinder.hasAnyMatch(b) || !com.brickyard.engine.rules.Moves.hasLegalMove(b)) return null;
            for (int g = 0; g < L.goals.size(); g++) if (b.remaining(g) <= 0) return null;
            if (s.depth > 1 && new BlueprintSolver().solve(L, s.depth - 1).solved) return null; // must not be shorter
            BlueprintSolver.Result r = new BlueprintSolver().solve(L, s.depth);
            if (!r.solved) return null;
            if (r.firstMoveWins * 3 > r.firstMoves + 2) return null; // needs a key move
            int wins = 0;
            for (int k = 0; k < 200; k++) if (Main.noisyWin(L, k)) wins++;
            if (wins >= 50) return null; // under 25%
            List<Object> w = new ArrayList<>();
            for (com.brickyard.engine.rules.Action a : r.line) {
                List<Object> e = new ArrayList<>();
                for (int x : a.encode()) e.add((long) x);
                w.add(e);
            }
            m.put("witness", w);
            m.put("forgeStats", "first moves that win " + r.firstMoveWins + "/" + r.firstMoves + ", noisy play wins " + (wins / 2) + "%");
            return m;
        }
    }

    /** A random fully specified board with obstacles and optional pre-placed power-ups. */
    static Map<String, Object> candidate(Spec s, String id, long seed) {
        Rng rng = new Rng(seed * 7477 + s.k * 131);
        char[][] lay = new char[s.rows][s.cols];
        char[][] flo = new char[s.rows][s.cols];
        char[][] ov = new char[s.rows][s.cols];
        for (int r = 0; r < s.rows; r++)
            for (int c = 0; c < s.cols; c++) { flo[r][c] = '.'; ov[r][c] = '.'; }
        // colours without ready-made matches
        for (int r = 0; r < s.rows; r++)
            for (int c = 0; c < s.cols; c++) {
                char ch;
                int guard = 0;
                do {
                    ch = Kinds.COLOR_LETTERS.charAt(rng.nextInt(s.colors));
                    guard++;
                } while (guard < 30 && ((c >= 2 && lay[r][c - 1] == ch && lay[r][c - 2] == ch)
                        || (r >= 2 && lay[r - 1][c] == ch && lay[r - 2][c] == ch)
                        || (r >= 1 && c >= 1 && lay[r - 1][c] == ch && lay[r][c - 1] == ch && lay[r - 1][c - 1] == ch)));
                lay[r][c] = ch;
            }
        List<Object> goals = new ArrayList<>();
        int placed = 0, tries = 0;
        boolean crates = false, clamps = false, oil = false, rust = false;
        while (placed < s.count && tries++ < 200) {
            int r = 1 + rng.nextInt(s.rows - 1), c = rng.nextInt(s.cols);
            String kind = s.obstacle.equals("MIX") ? (rng.nextInt(2) == 0 ? "CRATE1" : "CLAMP1") : s.obstacle;
            switch (kind) {
                case "CRATE1": case "CRATE2":
                    if (!Character.isLetter(lay[r][c]) || ov[r][c] != '.') continue;
                    lay[r][c] = kind.equals("CRATE2") ? '2' : '1'; crates = true; break;
                case "OIL":
                    if (!Character.isLetter(lay[r][c])) continue;
                    lay[r][c] = '~'; oil = true; break;
                case "CLAMP1": case "CLAMP2":
                    if (!Character.isLetter(lay[r][c]) || ov[r][c] != '.') continue;
                    ov[r][c] = kind.equals("CLAMP2") ? '2' : '1'; clamps = true; break;
                case "RUST":
                    if (flo[r][c] != '.') continue;
                    flo[r][c] = 'r'; rust = true; break;
                default: continue;
            }
            placed++;
        }
        for (int p = 0; p < s.powers; p++) {
            int r = rng.nextInt(s.rows), c = rng.nextInt(s.cols);
            if (!Character.isLetter(lay[r][c]) || ov[r][c] != '.') continue;
            lay[r][c] = "hvua".charAt(rng.nextInt(4));
        }
        if (crates) goals.add(goal("CRATE"));
        if (clamps) goals.add(goal("CLAMP"));
        if (oil) goals.add(goal("OIL"));
        if (rust) goals.add(goal("RUST"));
        if (goals.isEmpty()) return null;
        List<Object> refills = new ArrayList<>();
        for (int c = 0; c < s.cols; c++) {
            StringBuilder q = new StringBuilder();
            for (int k = 0; k < 14; k++) q.append(Kinds.COLOR_LETTERS.charAt(rng.nextInt(s.colors)));
            refills.add(q.toString());
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("kind", "BLUEPRINT");
        m.put("index", (long) s.k);
        m.put("title", s.title);
        m.put("moves", (long) s.depth);
        m.put("exactMoves", (long) s.depth);
        m.put("colors", (long) s.colors);
        m.put("seed", seed);
        m.put("baseCoins", 0L);
        m.put("boNote", s.bo);
        m.put("tutorial", s.lesson);
        m.put("layout", rows(lay));
        m.put("floor", rows(flo));
        m.put("overlay", rows(ov));
        m.put("refills", refills);
        m.put("goals", goals);
        return m;
    }

    private static Map<String, Object> goal(String t) {
        Map<String, Object> g = new LinkedHashMap<>();
        g.put("type", t);
        return g;
    }

    private static List<Object> rows(char[][] g) {
        List<Object> out = new ArrayList<>();
        for (char[] r : g) out.add(new String(r));
        return out;
    }
}
