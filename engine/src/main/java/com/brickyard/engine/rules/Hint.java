package com.brickyard.engine.rules;

import com.brickyard.engine.model.Board;
import com.brickyard.engine.model.Kinds;

import java.util.List;

/**
 * The hint bulb: finds a productive move by trying each legal move on a copy of the board
 * (one move of lookahead, the same idea the difficulty bot uses). It only points; it never plays.
 */
public final class Hint {
    private Hint() {}

    public static Action best(Board b) {
        List<Action> legal = Moves.legal(b);
        Action best = null;
        double bestScore = -1e18;
        for (Action a : legal) {
            Board c = b.copy();
            TurnResult r = TurnEngine.play(c, a, false);
            double s = score(b, c, r);
            if (s > bestScore) { bestScore = s; best = a; }
        }
        return best;
    }

    public static double score(Board before, Board after, TurnResult r) {
        if (r.status == TurnResult.Status.WON) return 1e9 - after.movesUsed;
        double s = 0;
        for (int g = 0; g < before.level.goals.size(); g++) {
            int b0 = Math.max(0, before.remaining(g)), b1 = Math.max(0, after.remaining(g));
            s += 100.0 * (b0 - b1) / Math.max(1, before.initialRemaining[g]);
        }
        int p0 = 0, p1 = 0;
        for (int i = 0; i < before.n; i++) {
            if (before.entity[i] == Kinds.PIECE && before.power[i] != Kinds.NONE) p0++;
            if (after.entity[i] == Kinds.PIECE && after.power[i] != Kinds.NONE) p1++;
        }
        s += 4 * (p1 - p0) + 1.5 * r.obstaclesCleared;
        return s;
    }
}
