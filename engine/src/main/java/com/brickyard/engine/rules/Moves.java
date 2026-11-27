package com.brickyard.engine.rules;

import com.brickyard.engine.model.Board;
import com.brickyard.engine.model.Kinds;

import java.util.ArrayList;
import java.util.List;

/** Swap legality and legal-move enumeration (used by deadlock detection, hints, bot and solver). */
public final class Moves {
    private Moves() {}

    public static boolean adjacent(Board b, int a, int c) {
        int ra = a / b.cols, ca = a % b.cols, rc = c / b.cols, cc = c % b.cols;
        return Math.abs(ra - rc) + Math.abs(ca - cc) == 1;
    }

    public static boolean wallBetween(Board b, int a, int c) {
        int ra = a / b.cols, ca = a % b.cols, rc = c / b.cols, cc = c % b.cols;
        if (ra == rc) return b.level.wallRight[ra * b.cols + Math.min(ca, cc)];
        return b.level.wallBelow[Math.min(ra, rc) * b.cols + ca];
    }

    /** Physically allowed to exchange these two cells (ignores whether a match results). */
    public static boolean canExchange(Board b, int a, int c) {
        return a >= 0 && c >= 0 && a < b.n && c < b.n && adjacent(b, a, c) && !wallBetween(b, a, c)
                && b.movable(a) && b.movable(c);
    }

    public static boolean isPower(Board b, int i) {
        return b.entity[i] == Kinds.PIECE && b.power[i] != Kinds.NONE;
    }

    /** Tappable power-ups: Nitro, Buggy, Airbag (the Sprayer only fires by swapping). */
    public static boolean isTappable(Board b, int i) {
        return isPower(b, i) && b.clamp[i] == 0 && b.power[i] != Kinds.SPRAYER;
    }

    static void exchange(Board b, int a, int c) {
        byte t;
        t = b.entity[a]; b.entity[a] = b.entity[c]; b.entity[c] = t;
        t = b.color[a]; b.color[a] = b.color[c]; b.color[c] = t;
        t = b.power[a]; b.power[a] = b.power[c]; b.power[c] = t;
        t = b.layers[a]; b.layers[a] = b.layers[c]; b.layers[c] = t;
        int u = b.uid[a]; b.uid[a] = b.uid[c]; b.uid[c] = u;
    }

    /** Would swapping a and c be accepted (power-up involved, or a match forms)? */
    public static boolean swapIsValid(Board b, int a, int c) {
        if (!canExchange(b, a, c)) return false;
        if (isPower(b, a) || isPower(b, c)) return true;
        exchange(b, a, c);
        boolean ok = MatchFinder.matchesAt(b, a) || MatchFinder.matchesAt(b, c);
        exchange(b, a, c);
        return ok;
    }

    public static boolean hasLegalMove(Board b) {
        for (int i = 0; i < b.n; i++) {
            if (!b.level.active[i]) continue;
            if (isTappable(b, i)) return true;
            int r = i / b.cols, c = i % b.cols;
            if (c + 1 < b.cols && swapIsValid(b, i, i + 1)) return true;
            if (r + 1 < b.rows && swapIsValid(b, i, i + b.cols)) return true;
        }
        return false;
    }

    /** Every distinct move-costing action available now (taps and valid swaps). */
    public static List<Action> legal(Board b) {
        List<Action> out = new ArrayList<>();
        for (int i = 0; i < b.n; i++) {
            if (!b.level.active[i]) continue;
            if (isTappable(b, i)) out.add(Action.tap(i));
            int r = i / b.cols, c = i % b.cols;
            if (c + 1 < b.cols && swapIsValid(b, i, i + 1)) addSwap(b, out, i, i + 1);
            if (r + 1 < b.rows && swapIsValid(b, i, i + b.cols)) addSwap(b, out, i, i + b.cols);
        }
        return out;
    }

    private static void addSwap(Board b, List<Action> out, int i, int j) {
        // A power-up's swap direction matters (it fires at the destination), so both directions are distinct.
        if (isPower(b, i) || isPower(b, j)) {
            out.add(Action.swap(i, j));
            out.add(Action.swap(j, i));
        } else out.add(Action.swap(i, j));
    }
}
