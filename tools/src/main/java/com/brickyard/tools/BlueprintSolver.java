package com.brickyard.tools;

import com.brickyard.engine.model.Board;
import com.brickyard.engine.model.Level;
import com.brickyard.engine.rules.Action;
import com.brickyard.engine.rules.Moves;
import com.brickyard.engine.rules.TurnEngine;
import com.brickyard.engine.rules.TurnResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Iterative-deepening DFS with a transposition table. Proves a Blueprint can be won within N moves
 * and records the witness line. Blueprints are fully deterministic (fixed layout, visible refill
 * queues, seeded RNG), so a proof on the JVM holds on the tablet.
 */
public final class BlueprintSolver {
    public static final class Result {
        public boolean solved;
        public int depth = -1;              // shortest win found
        public List<Action> line = new ArrayList<>();
        public long nodes;
        public int firstMoveWins;           // how many distinct first moves lead to a win within N
        public int firstMoves;              // legal first moves
    }

    private long nodes;
    private final Map<Long, Integer> failedAt = new HashMap<>(); // hash -> deepest remaining depth proven to fail

    public static Board start(Level L) {
        return Board.create(L, L.seed);
    }

    public Result solve(Level L, int maxDepth) {
        Result res = new Result();
        Board root = start(L);
        for (int d = 1; d <= maxDepth; d++) {
            List<Action> line = new ArrayList<>();
            if (dfs(root, d, line)) {
                res.solved = true;
                res.depth = d;
                res.line = line;
                break;
            }
        }
        if (res.solved) {
            // Breadth of the puzzle: how many first moves still win within the full budget?
            List<Action> first = Moves.legal(root);
            res.firstMoves = first.size();
            for (Action a : first) {
                Board c = root.copy();
                TurnResult r = TurnEngine.play(c, a, false);
                if (r.status == TurnResult.Status.WON || (r.status == TurnResult.Status.PLAYING && dfs(c, maxDepth - 1, new ArrayList<Action>())))
                    res.firstMoveWins++;
            }
        }
        res.nodes = nodes;
        return res;
    }

    private boolean dfs(Board b, int depth, List<Action> line) {
        if (depth <= 0) return false;
        long h = b.stateHash();
        Integer f = failedAt.get(h);
        if (f != null && f >= depth) return false;
        for (Action a : Moves.legal(b)) {
            nodes++;
            Board c = b.copy();
            TurnResult r = TurnEngine.play(c, a, false);
            if (r.status == TurnResult.Status.WON) { line.add(0, a); return true; }
            if (r.status == TurnResult.Status.PLAYING && depth > 1 && dfs(c, depth - 1, line)) { line.add(0, a); return true; }
        }
        failedAt.put(h, depth);
        return false;
    }

    /** Replays a recorded witness line and checks that it wins. */
    public static boolean replay(Level L, List<int[]> witness) {
        Board b = start(L);
        for (int[] e : witness) {
            TurnResult r = TurnEngine.play(b, Action.decode(e), false);
            if (r.status == TurnResult.Status.WON) return true;
            if (r.status != TurnResult.Status.PLAYING) return false;
        }
        return false;
    }
}
