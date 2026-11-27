package com.brickyard.engine.rules;

import com.brickyard.engine.model.Board;
import com.brickyard.engine.model.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bounded iterative-deepening solver for Blueprint Puzzles (runs on-device in milliseconds).
 * Returns a guaranteed winning first move from the current board state within {@code movesLeft},
 * or {@code null} if no winning line remains from here (so the UI can suggest Undo).
 */
public final class BlueprintHint {
    private static final int MAX_NODES = 30000;
    private int nodes;
    private final Map<Long, Integer> failedAt = new HashMap<>();

    private BlueprintHint() {}

    public static Action solveNext(Board board) {
        Level L = board.level;
        if (board.movesUsed == 0 && L.witness != null && !L.witness.isEmpty()) {
            Action first = Action.decode(L.witness.get(0));
            Board verify = board.copy();
            if (TurnEngine.play(verify, first, false).status != TurnResult.Status.INVALID) return first;
        }
        int budget = Math.min(board.movesLeft, 5);
        if (budget <= 0) return null;
        BlueprintHint solver = new BlueprintHint();
        for (int depth = 1; depth <= budget; depth++) {
            Action found = solver.searchRoot(board, depth);
            if (found != null) return found;
            if (solver.nodes >= MAX_NODES) break;
        }
        return null;
    }

    private Action searchRoot(Board root, int depth) {
        List<Action> legal = Moves.legal(root);
        for (Action a : legal) {
            if (nodes++ >= MAX_NODES) return null;
            Board c = root.copy();
            TurnResult r = TurnEngine.play(c, a, false);
            if (r.status == TurnResult.Status.WON) return a;
            if (r.status == TurnResult.Status.PLAYING && depth > 1 && dfs(c, depth - 1)) return a;
        }
        return null;
    }

    private boolean dfs(Board b, int depth) {
        if (depth <= 0 || nodes >= MAX_NODES) return false;
        long h = b.stateHash();
        Integer f = failedAt.get(h);
        if (f != null && f >= depth) return false;
        for (Action a : Moves.legal(b)) {
            if (nodes++ >= MAX_NODES) return false;
            Board c = b.copy();
            TurnResult r = TurnEngine.play(c, a, false);
            if (r.status == TurnResult.Status.WON) return true;
            if (r.status == TurnResult.Status.PLAYING && depth > 1 && dfs(c, depth - 1)) return true;
        }
        failedAt.put(h, depth);
        return false;
    }
}
