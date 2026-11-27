package com.brickyard.tools;

import com.brickyard.engine.model.Board;
import com.brickyard.engine.model.Kinds;
import com.brickyard.engine.model.Level;
import com.brickyard.engine.rules.Action;
import com.brickyard.engine.rules.Moves;
import com.brickyard.engine.rules.TurnEngine;
import com.brickyard.engine.rules.TurnResult;
import com.brickyard.engine.util.Rng;

import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.stream.IntStream;

/**
 * Monte Carlo difficulty bot. A 1-ply tactical player: it tries every legal move on a copy of the
 * board and picks the one that makes the most goal progress, favouring power-ups and obstacle
 * clears, with a little noise so runs differ.
 *
 * Because the bot never looks at the move counter, one batch of runs played with a huge move
 * budget gives the win rate for every possible move limit at once: a run that needed k moves wins
 * under any limit >= k. That is how levels are tuned to their difficulty band.
 */
public final class DifficultyBot {
    public static final int CAP = 99;

    /** movesNeeded[run] = moves the bot needed to win, or Integer.MAX_VALUE if it failed within CAP. */
    public static int[] movesNeeded(Level level, int runs, double epsilon) {
        int[] out = new int[runs];
        IntStream.range(0, runs).parallel().forEach(k -> out[k] = playOne(level, level.seed * 1000003L + k, epsilon, CAP));
        return out;
    }

    public static double winRate(int[] needed, int moves) {
        int w = 0;
        for (int x : needed) if (x <= moves) w++;
        return w / (double) needed.length;
    }

    /** Plays one game; returns moves used to win, or MAX_VALUE. */
    public static int playOne(Level level, long seed, double epsilon, int budget) {
        Board b = Board.create(level, seed);
        b.movesLeft = budget;
        Rng noise = new Rng(seed ^ 0x5EEDL);
        for (int t = 0; t < budget; t++) {
            Action a = choose(b, noise, epsilon);
            if (a == null) return Integer.MAX_VALUE;
            TurnResult r = TurnEngine.play(b, a, false);
            if (r.status == TurnResult.Status.WON) return b.movesUsed;
            if (r.status != TurnResult.Status.PLAYING) return Integer.MAX_VALUE;
        }
        return Integer.MAX_VALUE;
    }

    /** Same idea with an exact budget (used for Blueprint non-triviality checks). */
    public static boolean wins(Level level, long seed, double epsilon) {
        Board b = Board.create(level, seed);
        Rng noise = new Rng(seed ^ 0xB1E55L);
        while (true) {
            Action a = choose(b, noise, epsilon);
            if (a == null) return false;
            TurnResult r = TurnEngine.play(b, a, false);
            if (r.status == TurnResult.Status.WON) return true;
            if (r.status != TurnResult.Status.PLAYING) return false;
        }
    }

    public static Action choose(Board b, Rng noise, double epsilon) {
        List<Action> legal = Moves.legal(b);
        if (legal.isEmpty()) return null;
        if (epsilon > 0 && noise.nextDouble() < epsilon) return legal.get(noise.nextInt(legal.size()));
        Action best = null;
        double bestScore = -1e18;
        for (Action a : legal) {
            Board c = b.copy();
            TurnResult r = TurnEngine.play(c, a, false);
            double s = score(b, c, r) + noise.nextDouble() * 0.5;
            if (s > bestScore) { bestScore = s; best = a; }
        }
        return best;
    }

    static double score(Board before, Board after, TurnResult r) {
        if (r.status == TurnResult.Status.WON) return 1e9 - after.movesUsed;
        double s = 0;
        for (int g = 0; g < before.level.goals.size(); g++) {
            int b0 = Math.max(0, before.remaining(g)), b1 = Math.max(0, after.remaining(g));
            int init = Math.max(1, before.initialRemaining[g]);
            s += 100.0 * (b0 - b1) / init;
        }
        s += 4 * (powerCount(after) - powerCount(before));
        s += 1.5 * r.obstaclesCleared;
        return s;
    }

    private static int powerCount(Board b) {
        int k = 0;
        for (int i = 0; i < b.n; i++) if (b.entity[i] == Kinds.PIECE && b.power[i] != Kinds.NONE) k++;
        return k;
    }
}
