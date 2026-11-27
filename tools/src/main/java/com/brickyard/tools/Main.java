package com.brickyard.tools;

import com.brickyard.engine.cars.BrickVehicleModel;
import com.brickyard.engine.cars.GarageCatalog;
import com.brickyard.engine.gears.GearPuzzle;
import com.brickyard.engine.model.Board;
import com.brickyard.engine.model.GoalSpec;
import com.brickyard.engine.model.Kinds;
import com.brickyard.engine.model.Level;
import com.brickyard.engine.profile.Campaign;
import com.brickyard.engine.profile.Profile;
import com.brickyard.engine.profile.Progress;
import com.brickyard.engine.rules.Action;
import com.brickyard.engine.trackday.TrackDayGenerator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Brickyard content tools.
 *   verify <content>            the release gate (fails the build on any problem)
 *   tune <content> [ids...]     set each story level's move limit from bot data (centre of its band)
 *   solve <content> [ids...]    prove Blueprints and write their witness lines
 *   gears <content>             prove gear puzzles and write their witness placements
 *   bot <content> <id> [runs]   print a level's win-rate curve
 */
public final class Main {
    static final double WARMUP_MIN = 0.75, WARMUP_MAX = 0.95, CHALLENGE_MIN = 0.40, CHALLENGE_MAX = 0.65;
    static final int VERIFY_RUNS = 1000, TUNE_RUNS = 600;

    private final List<String> errors = new ArrayList<>();
    private final StringBuilder report = new StringBuilder();

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        if (args.length < 2) {
            System.err.println("usage: verify|tune|solve|gears|bot <contentDir> [...]");
            System.exit(2);
        }
        Path dir = Paths.get(args[1]);
        Main m = new Main();
        List<String> rest = Arrays.asList(args).subList(2, args.length);
        switch (args[0]) {
            case "verify": System.exit(m.verify(dir) ? 0 : 1); break;
            case "tune": m.tune(dir, rest); break;
            case "solve": m.solve(dir, rest); break;
            case "gears": m.gears(dir); break;
            case "bot": m.botCurve(dir, rest); break;
            case "trace": m.trace(dir, rest); break;
            case "forge": BlueprintForge.forgeAll(dir, rest); break;
            default: System.err.println("unknown command " + args[0]); System.exit(2);
        }
    }

    private void err(String s) {
        errors.add(s);
        line("  FAIL " + s);
    }

    private void line(String s) {
        report.append(s).append('\n');
        System.out.println(s);
    }

    static double[] band(Level L) {
        if (L.botMin >= 0) return new double[]{L.botMin, L.botMax};
        return L.difficulty == Level.Difficulty.CHALLENGE ? new double[]{CHALLENGE_MIN, CHALLENGE_MAX} : new double[]{WARMUP_MIN, WARMUP_MAX};
    }

    // =================================================================== verify

    boolean verify(Path dir) throws IOException {
        long t0 = System.currentTimeMillis();
        Content c;
        try {
            c = new Content(dir);
        } catch (RuntimeException e) {
            err("content failed to load: " + e.getMessage());
            return finish(dir, t0);
        }
        line("== Structure");
        checkStructure(c);
        line("== Words");
        for (String t : c.rawTexts) {
            String low = t.toLowerCase(Locale.ROOT);
            if (low.contains("lego") || low.contains("technic")) err("brand word found in content (use bricks / gear puzzles)");
        }
        line("== Economy");
        checkEconomy(c);
        line("== Gear puzzles");
        checkGears(c);
        line("== Blueprint puzzles");
        checkBlueprints(c);
        line("== Track Day");
        checkTrackDay();
        line("== Story difficulty (" + VERIFY_RUNS + " bot runs per level)");
        checkBands(c);
        return finish(dir, t0);
    }

    private boolean finish(Path dir, long t0) throws IOException {
        line(String.format("== %s: %d problem(s), %.1f s", errors.isEmpty() ? "PASS" : "FAIL", errors.size(), (System.currentTimeMillis() - t0) / 1000.0));
        Path out = dir.getParent().resolve("tools/build/verify-report.txt");
        Files.createDirectories(out.getParent());
        Files.write(out, report.toString().getBytes(StandardCharsets.UTF_8));
        return errors.isEmpty();
    }

    void checkStructure(Content c) {
        Campaign camp = c.campaign;
        if (camp.story.size() != 45) err("campaign must list 45 story levels, has " + camp.story.size());
        if (camp.blueprints.size() != 12) err("campaign must list 12 Blueprints, has " + camp.blueprints.size());
        if (camp.gears.size() != 9) err("campaign must list 9 gear puzzles, has " + camp.gears.size());
        for (int i = 0; i < camp.story.size(); i++) {
            String id = camp.story.get(i);
            Level L = c.levels.get(id);
            if (L == null) { err("story level missing: " + id); continue; }
            if (L.kind != Level.Kind.STORY) err(id + ": listed as story but kind is " + L.kind);
            if (L.bay != camp.bayOf(id)) err(id + ": bay " + L.bay + " but campaign position says bay " + camp.bayOf(id));
            if (L.difficulty == Level.Difficulty.CHALLENGE && L.boNote.isEmpty()) err(id + ": Builder Challenge needs a Grandpa Bo note");
            checkLevel(L);
        }
        for (Campaign.Unlockable u : camp.blueprints) {
            Level L = c.levels.get(u.id);
            if (L == null) { err("blueprint missing: " + u.id); continue; }
            if (L.kind != Level.Kind.BLUEPRINT) err(u.id + ": kind must be BLUEPRINT");
            if (L.exactMoves <= 0 || L.exactMoves != L.moves) err(u.id + ": exactMoves must be set and equal moves");
            if (L.refills == null) err(u.id + ": Blueprints need visible refill queues");
            if (!camp.story.contains(u.unlockAfter)) err(u.id + ": unlockAfter " + u.unlockAfter + " is not a story level");
            if (u.rewardPart != null) {
                GarageCatalog.Part p = c.garage.parts.get(u.rewardPart);
                if (p == null || !p.source.equals("blueprint")) err(u.id + ": rewardPart must be a blueprint part");
            }
            checkLevel(L);
        }
        for (Campaign.Unlockable u : camp.gears) {
            if (!c.gears.containsKey(u.id)) err("gear puzzle missing: " + u.id);
            if (!c.garage.repairs.containsKey(u.unlockAfter)) err(u.id + ": unlockRepair " + u.unlockAfter + " is not a repair");
            if (u.rewardPart != null) {
                GarageCatalog.Part p = c.garage.parts.get(u.rewardPart);
                if (p == null || !p.source.equals("gear")) err(u.id + ": rewardPart must be a gear part");
            }
        }
        // Every reward part is actually given by something.
        for (GarageCatalog.Part p : c.garage.parts.values()) {
            if (!p.source.equals("blueprint") && !p.source.equals("gear")) continue;
            boolean given = false;
            for (Campaign.Unlockable u : camp.blueprints) if (p.id.equals(u.rewardPart)) given = true;
            for (Campaign.Unlockable u : camp.gears) if (p.id.equals(u.rewardPart)) given = true;
            if (!given) err("reward part " + p.id + " is never awarded");
        }
        if (!c.garage.repairs.containsKey(camp.trackDayUnlock)) err("trackDayUnlock must be a repair id");
        line("  " + c.levels.size() + " levels/blueprints, " + c.gears.size() + " gear puzzles checked");
    }

    void checkLevel(Level L) {
        try {
            Board b = Board.create(L, L.seed);
            if (L.goals.isEmpty()) err(L.id + ": no goals");
            for (int g = 0; g < L.goals.size(); g++) {
                GoalSpec s = L.goals.get(g);
                if (s.type == GoalSpec.Type.COLOR && s.color >= L.colors) err(L.id + ": colour goal outside the level's " + L.colors + " colours");
                if (b.remaining(g) <= 0) err(L.id + ": goal " + s.type + " is already complete at the start");
                if (s.type == GoalSpec.Type.PART) {
                    boolean anyExit = false, anyDisp = false;
                    for (int i = 0; i < b.n; i++) { anyExit |= L.exit[i]; anyDisp |= L.dispenser[i]; }
                    if (!anyExit) err(L.id + ": part goal without an exit");
                    int need = s.count == GoalSpec.ALL ? L.partsTotal : s.count;
                    if (L.partsTotal < need) err(L.id + ": partsTotal smaller than the part goal");
                    if (!anyDisp && b.partsSpawned < need) err(L.id + ": not enough parts can ever appear");
                }
            }
            for (int i = 0; i < b.n; i++) {
                if (L.dispenser[i] && L.feeder[i] != Level.SPAWN) err(L.id + ": dispenser at cell " + i + " is not a refill point");
                if ((L.dispenser[i] || L.exit[i]) && !L.active[i]) err(L.id + ": dispenser/exit on a hole");
            }
            for (int k = 0; k < L.portalFrom.length; k++)
                if (!L.active[L.portalFrom[k]] || !L.active[L.portalTo[k]]) err(L.id + ": portal end on a hole");
            for (int[] loop : L.conveyors)
                for (int j = 0; j < loop.length; j++) {
                    int a = loop[j], d = loop[(j + 1) % loop.length];
                    if (!L.active[a]) err(L.id + ": conveyor on a hole");
                    int dr = Math.abs(a / L.cols - d / L.cols), dc = Math.abs(a % L.cols - d % L.cols);
                    if (dr + dc != 1) err(L.id + ": conveyor loop cells must be neighbours");
                }
        } catch (RuntimeException e) {
            err(L.id + ": " + e.getMessage());
        }
    }

    void checkEconomy(Content c) {
        GarageCatalog g = c.garage;
        int stars = g.totalRepairStars();
        if (stars != c.campaign.story.size()) err("repairs cost " + stars + " stars but there are " + c.campaign.story.size() + " story levels");
        // Every vehicle has a starter part for every slot, and the bay's repairs unlock all six slots.
        for (GarageCatalog.Bay bay : g.bays) {
            List<String> unlocked = new ArrayList<>();
            for (GarageCatalog.Repair r : bay.repairs) if (!r.unlocksSlot.isEmpty()) for (String s : r.unlocksSlot.split(",")) unlocked.add(s.trim());
            for (String slot : GarageCatalog.SLOTS) {
                if (!unlocked.contains(slot)) err(bay.id + ": no repair unlocks slot " + slot);
                if (g.starter(bay.vehicle, slot) == null) err(bay.vehicle + ": no starter part for " + slot);
            }
        }
        // Play the whole campaign as the rules allow: win levels in order, replay a level, repair when affordable.
        Profile p = new Profile();
        Progress pr = new Progress(p, c.campaign, g);
        int minCoins = 0;
        for (String id : c.campaign.story) {
            if (!pr.storyUnlocked(id)) err("story order broken at " + id);
            Level L = c.levels.get(id);
            Progress.Reward r = pr.winStory(id, L.baseCoins);
            minCoins += L.baseCoins;
            if (!r.star) err(id + ": first win gave no star");
            Progress.Reward again = pr.winStory(id, L.baseCoins);
            if (again.star) err(id + ": a replay awarded a second star");
            p.coins -= again.coins; // keep the coin count to the guaranteed minimum
            boolean progressed = true;
            while (progressed) {
                progressed = false;
                for (GarageCatalog.Repair rep : g.repairs.values())
                    if (pr.repairAvailable(rep) && p.stars >= rep.stars && pr.doRepair(rep.id)) progressed = true;
            }
        }
        if (!pr.garageComplete()) err("45 stars did not finish the garage");
        if (p.stars != 0) err("stars left over after finishing: " + p.stars);
        if (p.starsEarned != 45) err("stars earned " + p.starsEarned + ", expected 45");
        // Coins: the guaranteed minimum (base coins only, no leftover moves) buys one module for every slot.
        int cheapest = 0;
        for (GarageCatalog.Vehicle v : g.vehicles.values())
            for (String slot : GarageCatalog.SLOTS) {
                int best = Integer.MAX_VALUE;
                for (GarageCatalog.Part part : g.partsFor(v.id, slot)) if (part.source.equals("shop")) best = Math.min(best, part.price);
                if (best == Integer.MAX_VALUE) err(v.id + "/" + slot + ": nothing to buy");
                else cheapest += best;
            }
        for (GarageCatalog.Fixture f : g.fixtures.values()) {
            int best = Integer.MAX_VALUE;
            for (GarageCatalog.Design d : f.designs) if (d.price > 0) best = Math.min(best, d.price);
            if (f.designs.size() != 3) err(f.id + ": needs 3 designs");
            if (f.designs.get(0).price != 0) err(f.id + ": first design must be free");
            cheapest += best;
        }
        line("  guaranteed story coins " + minCoins + ", one module for every vehicle and bay slot costs " + cheapest);
        if (minCoins < cheapest) err("guaranteed coins " + minCoins + " cannot buy one module for every slot (" + cheapest + ")");
        // Track medals are reachable and ordered.
        for (GarageCatalog.Track t : g.tracks.values()) {
            if (!(t.gold < t.silver && t.silver < t.bronze)) err(t.id + ": medal times must be gold < silver < bronze");
            Map<String, String> starter = new LinkedHashMap<>(), best = new LinkedHashMap<>();
            for (String slot : GarageCatalog.SLOTS) {
                starter.put(slot, g.starter(t.vehicle, slot).id);
                GarageCatalog.Part top = null;
                for (GarageCatalog.Part part : g.partsFor(t.vehicle, slot))
                    if (top == null || part.speed + part.grip + part.boost > top.speed + top.grip + top.boost) top = part;
                best.put(slot, top.id);
            }
            BrickVehicleModel.Run s = new BrickVehicleModel(g, t.vehicle, starter).run(t);
            BrickVehicleModel.Run b = new BrickVehicleModel(g, t.vehicle, best).run(t);
            line(String.format("  %s: starter %.1fs (medal %d), best build %.1fs (medal %d)", t.id, s.totalSeconds, s.medal, b.totalSeconds, b.medal));
            if (s.medal < BrickVehicleModel.BRONZE) err(t.id + ": the starter build cannot earn bronze");
            if (b.medal != BrickVehicleModel.GOLD) err(t.id + ": the best build cannot earn gold");
        }
    }

    void checkGears(Content c) {
        for (Campaign.Unlockable u : c.campaign.gears) {
            GearPuzzle p = c.gears.get(u.id);
            if (p == null) continue;
            GearVerifier.Result r = GearVerifier.verify(p);
            if (r.solvedWithoutPlacing) err(p.id + ": already solved before placing any gear");
            if (!r.solvable) { err(p.id + ": no solution with the tray"); continue; }
            if (p.witness != null && !GearVerifier.replay(p, p.witness)) err(p.id + ": recorded witness does not solve it");
            line(String.format("  %s solvable with %d gear(s), %d solution(s) at that size", p.id, r.minGears, r.solutionsAtMin));
        }
    }

    void checkBlueprints(Content c) {
        for (Campaign.Unlockable u : c.campaign.blueprints) {
            Level L = c.levels.get(u.id);
            if (L == null) continue;
            BlueprintSolver.Result r = new BlueprintSolver().solve(L, L.exactMoves);
            if (!r.solved) { err(L.id + ": no win within " + L.exactMoves + " moves"); continue; }
            if (L.witness != null && !BlueprintSolver.replay(L, L.witness)) err(L.id + ": recorded witness line does not win");
            // The board is fixed; variety comes from a half-random, half-greedy player seeded per run.
            int wins = 0, runs = 400;
            for (int k = 0; k < runs; k++) if (noisyWin(L, k)) wins++;
            double rate = wins / (double) runs;
            line(String.format("  %s: proven in %d (limit %d), %d of %d first moves can still win, random/greedy wins %.0f%%",
                    L.id, r.depth, L.exactMoves, r.firstMoveWins, r.firstMoves, rate * 100));
            if (rate >= 0.25) err(L.id + ": too easy, random/greedy play wins " + Math.round(rate * 100) + "%");
        }
    }

    static boolean noisyWin(Level L, int k) {
        Board b = Board.create(L, L.seed);
        com.brickyard.engine.util.Rng noise = new com.brickyard.engine.util.Rng(L.seed * 31 + k);
        while (true) {
            Action a = DifficultyBot.choose(b, noise, 0.5);
            if (a == null) return false;
            com.brickyard.engine.rules.TurnResult r = com.brickyard.engine.rules.TurnEngine.play(b, a, false);
            if (r.status == com.brickyard.engine.rules.TurnResult.Status.WON) return true;
            if (r.status != com.brickyard.engine.rules.TurnResult.Status.PLAYING) return false;
        }
    }

    void checkTrackDay() {
        int ok = 0;
        for (int round = 0; round < 30; round++) {
            Level L = TrackDayGenerator.generate(round);
            try {
                checkLevel(L);
                int[] need = DifficultyBot.movesNeeded(L, 60, 0);
                double rate = DifficultyBot.winRate(need, L.moves);
                if (rate < 0.5) err("Track Day round " + round + ": bot wins only " + Math.round(rate * 100) + "%");
                else ok++;
            } catch (RuntimeException e) {
                err("Track Day round " + round + ": " + e.getMessage());
            }
        }
        line("  " + ok + " of 30 generated rounds are playable and winnable");
    }

    void checkBands(Content c) {
        for (Level L : c.story()) {
            if (L == null) continue;
            int[] need = DifficultyBot.movesNeeded(L, VERIFY_RUNS, 0);
            double rate = DifficultyBot.winRate(need, L.moves);
            double[] band = band(L);
            boolean in = rate >= band[0] && rate <= band[1];
            line(String.format("  %-7s %-9s moves %2d  bot wins %5.1f%%  band %2.0f-%2.0f%%  %s",
                    L.id, L.difficulty, L.moves, rate * 100, band[0] * 100, band[1] * 100, in ? "ok" : "OUT OF BAND"));
            if (!in) err(L.id + ": bot win rate " + Math.round(rate * 100) + "% outside its band");
        }
    }

    // =================================================================== tune

    void tune(Path dir, List<String> only) throws IOException {
        Content c = new Content(dir);
        for (Level L : c.story()) {
            if (L == null || (!only.isEmpty() && !only.contains(L.id))) continue;
            int[] need = DifficultyBot.movesNeeded(L, TUNE_RUNS, 0);
            double[] band = band(L);
            double target = (band[0] + band[1]) / 2;
            int bestM = -1;
            double bestDiff = 9;
            for (int m = 5; m <= 60; m++) {
                double r = DifficultyBot.winRate(need, m);
                double diff = Math.abs(r - target);
                if (diff < bestDiff - 1e-9) { bestDiff = diff; bestM = m; }
            }
            double got = DifficultyBot.winRate(need, bestM);
            System.out.printf("%-7s %-9s moves %2d -> %2d  (bot %.0f%%, target %.0f%%)%s%n", L.id, L.difficulty, L.moves, bestM, got * 100, target * 100,
                    got < band[0] || got > band[1] ? "  !! cannot reach band: redesign" : "");
            rewriteInt(c.levelFiles.get(L.id), "moves", bestM);
        }
    }

    static void rewriteInt(Path f, String key, int value) throws IOException {
        String s = Content.read(f);
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*-?\\d+").matcher(s);
        if (!m.find()) throw new IllegalStateException(f + ": no " + key);
        s = s.substring(0, m.start()) + "\"" + key + "\": " + value + s.substring(m.end());
        Files.write(f, s.getBytes(StandardCharsets.UTF_8));
    }

    /** Replaces (or appends) a top-level array value, finding its end by bracket counting. */
    static void rewriteRaw(Path f, String key, String json) throws IOException {
        String s = Content.read(f);
        int k = s.indexOf("\"" + key + "\"");
        if (k >= 0) {
            int open = s.indexOf('[', k), depth = 0, end = open;
            for (; end < s.length(); end++) {
                char ch = s.charAt(end);
                if (ch == '[') depth++;
                else if (ch == ']' && --depth == 0) break;
            }
            s = s.substring(0, k) + "\"" + key + "\": " + json + s.substring(end + 1);
        } else {
            int end = s.lastIndexOf('}');
            s = s.substring(0, end).replaceAll("\\s+$", "") + ",\n  \"" + key + "\": " + json + "\n}\n";
        }
        Files.write(f, s.getBytes(StandardCharsets.UTF_8));
    }

    // =================================================================== solve & gears

    void solve(Path dir, List<String> only) throws IOException {
        Content c = new Content(dir);
        for (Campaign.Unlockable u : c.campaign.blueprints) {
            if (!only.isEmpty() && !only.contains(u.id)) continue;
            Level L = c.levels.get(u.id);
            if (L == null) continue;
            long t0 = System.currentTimeMillis();
            BlueprintSolver.Result r = new BlueprintSolver().solve(L, L.exactMoves);
            int wins = 0;
            for (int k = 0; k < 200; k++) if (noisyWin(L, k)) wins++;
            System.out.printf("%s: %s depth %d, nodes %d, first moves winning %d/%d, random/greedy %d%%, %d ms%n",
                    L.id, r.solved ? "SOLVED" : "NOT SOLVED", r.depth, r.nodes, r.firstMoveWins, r.firstMoves, wins / 2, System.currentTimeMillis() - t0);
            if (r.solved) {
                StringBuilder sb = new StringBuilder("[");
                for (int k = 0; k < r.line.size(); k++) {
                    int[] e = r.line.get(k).encode();
                    if (k > 0) sb.append(", ");
                    sb.append('[').append(e[0]).append(", ").append(e[1]).append(", ").append(e[2]).append(", ").append(e[3]).append(']');
                }
                sb.append(']');
                rewriteRaw(c.levelFiles.get(L.id), "witness", sb.toString());
            }
        }
    }

    void gears(Path dir) throws IOException {
        Content c = new Content(dir);
        for (GearPuzzle p : c.gears.values()) {
            GearVerifier.Result r = GearVerifier.verify(p);
            System.out.printf("%s: %s with %d gear(s), %d solution(s)%s%n", p.id, r.solvable ? "solvable" : "UNSOLVABLE", r.minGears, r.solutionsAtMin,
                    r.solvedWithoutPlacing ? "  !! already solved" : "");
            if (r.solvable) {
                StringBuilder sb = new StringBuilder("[");
                for (int k = 0; k < r.witness.size(); k++) {
                    int[] w = r.witness.get(k);
                    if (k > 0) sb.append(", ");
                    sb.append('[').append(w[0]).append(", ").append(w[1]).append(", ").append(w[2]).append(']');
                }
                sb.append(']');
                rewriteRaw(c.gearFiles.get(p.id), "witness", sb.toString());
            }
        }
    }

    void trace(Path dir, List<String> args) throws IOException {
        Content c = new Content(dir);
        Level L = c.levels.get(args.get(0));
        long seed = args.size() > 1 ? Long.parseLong(args.get(1)) : L.seed;
        Board b = Board.create(L, seed);
        b.movesLeft = 60;
        com.brickyard.engine.util.Rng noise = new com.brickyard.engine.util.Rng(seed);
        System.out.println(b.dump());
        for (int t = 0; t < 60; t++) {
            Action a = DifficultyBot.choose(b, noise, 0);
            int[] before = new int[L.goals.size()];
            for (int g = 0; g < before.length; g++) before[g] = b.remaining(g);
            com.brickyard.engine.rules.TurnResult r = com.brickyard.engine.rules.TurnEngine.play(b, a, true);
            StringBuilder sb = new StringBuilder();
            for (int g = 0; g < before.length; g++) sb.append(L.goals.get(g).type).append(' ').append(before[g]).append("->").append(b.remaining(g)).append("  ");
            java.util.Map<String, Integer> kinds = new java.util.TreeMap<>();
            for (com.brickyard.engine.rules.Step st : r.steps) kinds.merge(st.type + (st.type == com.brickyard.engine.rules.Step.Type.POWERUP_FIRE ? "/" + st.kind : ""), 1, Integer::sum);
            System.out.println("move " + (t + 1) + " " + a + "  " + sb + r.praise + " " + kinds);
            if (r.status != com.brickyard.engine.rules.TurnResult.Status.PLAYING) { System.out.println(r.status); break; }
        }
        System.out.println(b.dump());
    }

    void botCurve(Path dir, List<String> args) throws IOException {
        Content c = new Content(dir);
        Level L = c.levels.get(args.get(0));
        int runs = args.size() > 1 ? Integer.parseInt(args.get(1)) : 400;
        int[] need = DifficultyBot.movesNeeded(L, runs, 0);
        for (int m = 5; m <= 50; m += 1) System.out.printf("moves %2d: %5.1f%%%n", m, DifficultyBot.winRate(need, m) * 100);
    }
}
