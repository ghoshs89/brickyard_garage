package com.brickyard.engine.rules;

import com.brickyard.engine.model.Board;
import com.brickyard.engine.model.GoalSpec;
import com.brickyard.engine.model.Kinds;
import com.brickyard.engine.model.Level;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Executes one player action and resolves the whole turn: matches, power-ups and combos, gravity
 * (through holes, portals and diagonally around blockers), part collection, cascades, conveyor
 * shift, oil spread, win/lose, victory lap and deadlock shuffle.
 *
 * With record = true every beat is captured as a {@link Step} with a board snapshot for animation.
 * The board is only ever saved between turns, so a turn is atomic for persistence.
 */
public final class TurnEngine {
    private final Board b;
    private final Level L;
    private final boolean record;
    private TurnResult res;
    private final ArrayDeque<int[]> fireQ = new ArrayDeque<>(); // {cell, power, color, carried, track}
    private int oilCleared;
    private boolean[] reserved;

    public TurnEngine(Board board, boolean record) {
        this.b = board;
        this.L = board.level;
        this.record = record;
    }

    public static TurnResult play(Board board, Action a, boolean record) {
        return new TurnEngine(board, record).apply(a);
    }

    public static boolean isOver(Board board) {
        return board.allGoalsDone() || board.movesLeft <= 0;
    }

    // =================================================================== entry point

    public TurnResult apply(Action a) {
        res = new TurnResult();
        oilCleared = 0;
        reserved = new boolean[b.n];
        if (isOver(b)) { res.status = TurnResult.Status.INVALID; return res; }

        int[] swapCells = null;
        boolean claw = false;
        boolean spent;

        switch (a.type) {
            case Action.SWAP: {
                int from = a.a, to = a.b;
                if (!Moves.canExchange(b, from, to)) { res.status = TurnResult.Status.INVALID; return res; }
                boolean pf = Moves.isPower(b, from), pt = Moves.isPower(b, to);
                if (pf && pt) {
                    combo(from, to);
                } else if (pf || pt) {
                    int pc = pf ? from : to, other = pf ? to : from;
                    if (b.power[pc] == Kinds.SPRAYER) {
                        int col = b.color[other];
                        Moves.exchange(b, from, to);
                        add(new Step(Step.Type.SWAP, null, from, to, 0));
                        int at = pf ? to : from; // the sprayer's new cell
                        enqueueFireColored(at, col);
                    } else {
                        Moves.exchange(b, from, to);
                        add(new Step(Step.Type.SWAP, null, from, to, 0));
                        enqueueFire(pf ? to : from);
                    }
                } else {
                    Moves.exchange(b, from, to);
                    if (!MatchFinder.matchesAt(b, from) && !MatchFinder.matchesAt(b, to)) {
                        Moves.exchange(b, from, to);
                        res.bounced = true;
                        add(new Step(Step.Type.SWAP_BOUNCE, null, from, to, 0));
                        return res; // invalid swap: pieces return, no move spent
                    }
                    add(new Step(Step.Type.SWAP, null, from, to, 0));
                    swapCells = new int[]{to, from};
                }
                spent = true;
                break;
            }
            case Action.TAP: {
                if (a.a < 0 || a.a >= b.n || !Moves.isTappable(b, a.a)) { res.status = TurnResult.Status.INVALID; return res; }
                enqueueFire(a.a);
                spent = true;
                break;
            }
            case Action.TOOL: {
                if (L.isBlueprint() || a.tool < 0 || a.tool > 2 || b.tools[a.tool] <= 0 || a.a < 0 || a.a >= b.n || !L.active[a.a]) {
                    res.status = TurnResult.Status.INVALID;
                    return res;
                }
                if (a.tool == Kinds.TOOL_CLAW) {
                    if (!Moves.canExchange(b, a.a, a.b)) { res.status = TurnResult.Status.INVALID; return res; }
                    Moves.exchange(b, a.a, a.b);
                    add(new Step(Step.Type.TOOL_USE, new int[]{a.a, a.b}, a.a, a.b, a.tool));
                    swapCells = new int[]{a.b, a.a};
                    claw = true;
                } else if (a.tool == Kinds.TOOL_WRENCH) {
                    add(new Step(Step.Type.TOOL_USE, new int[]{a.a}, a.a, -1, a.tool));
                    hit(a.a);
                } else {
                    int r = a.a / b.cols;
                    int[] row = new int[b.cols];
                    for (int c = 0; c < b.cols; c++) row[c] = r * b.cols + c;
                    add(new Step(Step.Type.TOOL_USE, row, a.a, -1, a.tool));
                    for (int c : row) hit(c);
                }
                b.tools[a.tool]--;
                spent = false;
                break;
            }
            default:
                res.status = TurnResult.Status.INVALID;
                return res;
        }

        if (spent) {
            b.movesLeft--;
            b.movesUsed++;
            res.moveSpent = true;
        }
        cascade(swapCells, claw);
        endTurn(spent);
        return res;
    }

    // =================================================================== cascade loop

    private void cascade(int[] swapCells, boolean claw) {
        for (int guard = 0; guard < 500; guard++) {
            boolean did = false;
            if (!fireQ.isEmpty()) { processFires(); did = true; }
            if (settle()) did = true;
            List<MatchFinder.Group> groups = MatchFinder.find(b);
            if (!groups.isEmpty()) {
                clearGroups(groups, swapCells, claw);
                swapCells = null;
                claw = false;
                continue;
            }
            if (!did) break;
        }
    }

    private void clearGroups(List<MatchFinder.Group> groups, int[] swapCells, boolean claw) {
        List<Integer> popped = new ArrayList<>();
        List<int[]> spawns = new ArrayList<>();
        for (MatchFinder.Group g : groups) {
            int spawn = -1;
            if (g.power != Kinds.NONE) {
                if (swapCells != null) for (int s : swapCells) if (spawn < 0 && g.cells.contains(s) && b.clamp[s] == 0) spawn = s;
                if (spawn < 0 && g.intersection >= 0 && b.clamp[g.intersection] == 0) spawn = g.intersection;
                if (spawn < 0) {
                    int mid = g.cells.get(g.cells.size() / 2);
                    if (b.clamp[mid] == 0) spawn = mid;
                    else for (int c : g.cells) if (b.clamp[c] == 0) { spawn = c; break; }
                }
            }
            boolean track = false;
            for (int c : g.cells) if (b.floor[c] == Kinds.FLOOR_TRACK) track = true;

            boolean[] inGroup = new boolean[b.n];
            for (int c : g.cells) inGroup[c] = true;
            for (int c : g.cells) {
                popped.add(c);
                if (c == spawn) continue;
                hitMatched(c);
            }
            if (spawn >= 0) {
                countColor(b.color[spawn]);
                clearRust(spawn);
                b.power[spawn] = g.power;
                b.color[spawn] = -1;
                spawns.add(new int[]{spawn, g.power});
                if (swapCells != null && (spawn == swapCells[0] || spawn == swapCells[1])) {
                    if (claw && g.power == Kinds.SPRAYER) addPraise(TurnResult.Praise.SMART_CLAW);
                    else if (!claw) addPraise(TurnResult.Praise.SETUP_CRAFTED);
                }
            }
            if (track) for (int c : g.cells) paintTrack(c);
            // Each adjacent crate or oil spill takes one hit per match group.
            boolean[] hitOnce = new boolean[b.n];
            for (int c : g.cells)
                for (int nb : neighbors(c)) {
                    if (inGroup[nb] || hitOnce[nb]) continue;
                    if (b.entity[nb] == Kinds.CRATE || b.entity[nb] == Kinds.OIL) {
                        hitOnce[nb] = true;
                        hit(nb);
                    }
                }
        }
        add(new Step(Step.Type.MATCH_POP, toArray(popped), -1, -1, 0));
        for (int[] s : spawns) add(new Step(Step.Type.POWERUP_SPAWN, new int[]{s[0]}, s[0], -1, s[1]));
    }

    // =================================================================== damage model

    /** A matched piece: a clamp loses a layer (piece stays), otherwise the piece is collected. */
    private void hitMatched(int i) {
        if (b.clamp[i] > 0) { unclamp(i); return; }
        removePiece(i);
    }

    /** One hit from a blast, a neighbouring match, a tool or a buggy. */
    void hit(int i) {
        if (i < 0 || i >= b.n || !L.active[i]) return;
        switch (b.entity[i]) {
            case Kinds.PIECE:
                if (b.clamp[i] > 0) { unclamp(i); return; }
                if (b.power[i] != Kinds.NONE) { enqueueFire(i); return; }
                removePiece(i);
                return;
            case Kinds.CRATE:
                b.layers[i]--;
                if (b.layers[i] <= 0) {
                    b.entity[i] = Kinds.EMPTY;
                    b.layers[i] = 0;
                    b.uid[i] = 0;
                    countGoal(GoalSpec.Type.CRATE);
                    res.obstaclesCleared++;
                    clearRust(i);
                }
                return;
            case Kinds.OIL:
                b.entity[i] = Kinds.EMPTY;
                b.uid[i] = 0;
                oilCleared++;
                res.obstaclesCleared++;
                clearRust(i);
                return;
            case Kinds.PART:
                return; // indestructible: parts are routed to exits, never destroyed
            default:
                clearRust(i);
        }
    }

    private void unclamp(int i) {
        b.clamp[i]--;
        if (b.clamp[i] == 0) {
            countGoal(GoalSpec.Type.CLAMP);
            res.obstaclesCleared++;
        }
    }

    private void removePiece(int i) {
        countColor(b.color[i]);
        b.entity[i] = Kinds.EMPTY;
        b.color[i] = -1;
        b.power[i] = Kinds.NONE;
        b.uid[i] = 0;
        clearRust(i);
    }

    private void clearRust(int i) {
        if (b.floor[i] == Kinds.FLOOR_RUST) {
            b.floor[i] = Kinds.FLOOR_NONE;
            res.obstaclesCleared++;
        }
    }

    private void paintTrack(int i) {
        if (L.active[i] && b.floor[i] == Kinds.FLOOR_NONE) b.floor[i] = Kinds.FLOOR_TRACK;
    }

    private void countColor(int color) {
        if (color < 0) return;
        for (int g = 0; g < L.goals.size(); g++) {
            GoalSpec s = L.goals.get(g);
            if (s.type == GoalSpec.Type.COLOR && s.color == color) b.collected[g]++;
        }
    }

    private void countGoal(GoalSpec.Type t) {
        for (int g = 0; g < L.goals.size(); g++) if (L.goals.get(g).type == t) b.collected[g]++;
    }

    // =================================================================== power-ups

    private void enqueueFire(int i) {
        fireQ.add(new int[]{i, b.power[i], -1, Kinds.NONE, b.floor[i] == Kinds.FLOOR_TRACK ? 1 : 0});
        takeOut(i);
    }

    private void enqueueFireColored(int i, int color) {
        fireQ.add(new int[]{i, b.power[i], color, Kinds.NONE, b.floor[i] == Kinds.FLOOR_TRACK ? 1 : 0});
        takeOut(i);
    }

    /** Removes a power-up piece from the board as it fires. */
    private void takeOut(int i) {
        b.entity[i] = Kinds.EMPTY;
        b.power[i] = Kinds.NONE;
        b.color[i] = -1;
        b.uid[i] = 0;
        clearRust(i);
    }

    private void processFires() {
        int guard = 0;
        while (!fireQ.isEmpty() && guard++ < 1000) {
            int[] f = fireQ.poll();
            fireOne(f[0], (byte) f[1], f[2], (byte) f[3], f[4] == 1);
        }
    }

    private void fireOne(int cell, byte type, int color, byte carried, boolean track) {
        List<Integer> cells = new ArrayList<>();
        int r = cell / b.cols, c = cell % b.cols;
        switch (type) {
            case Kinds.NITRO_H:
                for (int k = 0; k < b.cols; k++) cells.add(r * b.cols + k);
                break;
            case Kinds.NITRO_V:
                for (int k = 0; k < b.rows; k++) cells.add(k * b.cols + c);
                break;
            case Kinds.AIRBAG:
                cells.addAll(diamond(cell, 2));
                break;
            case Kinds.SPRAYER: {
                int col = color >= 0 ? color : mostCommonColor();
                cells.add(cell);
                for (int i = 0; i < b.n; i++)
                    if (L.active[i] && b.entity[i] == Kinds.PIECE && b.power[i] == Kinds.NONE && b.color[i] == col) cells.add(i);
                break;
            }
            case Kinds.BUGGY: {
                cells.add(cell);
                for (int nb : neighbors(cell)) cells.add(nb);
                add(new Step(Step.Type.POWERUP_FIRE, toArray(cells), cell, -1, type));
                for (int t : cells) { hit(t); if (track) paintTrack(t); }
                buggyFly(cell, carried, track);
                return;
            }
            default:
                return;
        }
        add(new Step(Step.Type.POWERUP_FIRE, toArray(cells), cell, -1, type));
        for (int t : cells) { hit(t); if (track) paintTrack(t); }
    }

    private void buggyFly(int from, byte carried, boolean track) {
        int target = chooseBuggyTarget(from);
        add(new Step(Step.Type.BUGGY_FLIGHT, target >= 0 ? new int[]{target} : null, from, target, carried));
        if (target < 0) return;
        if (carried != Kinds.NONE) {
            fireOne(target, carried, -1, Kinds.NONE, track);
        } else {
            hit(target);
            if (track) paintTrack(target);
        }
    }

    /** RC Buggy homing priority: 1 oil, 2 layered/locked goal obstacles, 3 remaining collection tiles. */
    int chooseBuggyTarget(int from) {
        List<Integer> cand = new ArrayList<>();
        for (int i = 0; i < b.n; i++) if (ok(i, from) && b.entity[i] == Kinds.OIL) cand.add(i);
        if (cand.isEmpty()) {
            for (int g = 0; g < L.goals.size(); g++) {
                if (b.remaining(g) <= 0) continue;
                GoalSpec.Type t = L.goals.get(g).type;
                for (int i = 0; i < b.n; i++) {
                    if (!ok(i, from)) continue;
                    if (t == GoalSpec.Type.CRATE && b.entity[i] == Kinds.CRATE) cand.add(i);
                    else if (t == GoalSpec.Type.CLAMP && b.clamp[i] > 0) cand.add(i);
                    else if (t == GoalSpec.Type.RUST && b.floor[i] == Kinds.FLOOR_RUST) cand.add(i);
                }
            }
        }
        if (cand.isEmpty()) {
            for (int g = 0; g < L.goals.size(); g++) {
                if (b.remaining(g) <= 0) continue;
                GoalSpec s = L.goals.get(g);
                for (int i = 0; i < b.n; i++) {
                    if (!ok(i, from)) continue;
                    if (s.type == GoalSpec.Type.COLOR && b.entity[i] == Kinds.PIECE && b.power[i] == Kinds.NONE && b.color[i] == s.color) cand.add(i);
                    else if (s.type == GoalSpec.Type.TRACK && b.floor[i] == Kinds.FLOOR_NONE && b.entity[i] != Kinds.PART) cand.add(i);
                }
            }
        }
        if (cand.isEmpty())
            for (int i = 0; i < b.n; i++) if (ok(i, from) && b.isPlainPiece(i)) cand.add(i);
        if (cand.isEmpty()) return -1;
        int t = cand.get(b.rng.nextInt(cand.size()));
        reserved[t] = true;
        return t;
    }

    private boolean ok(int i, int from) {
        return L.active[i] && i != from && !reserved[i] && b.entity[i] != Kinds.EMPTY && b.entity[i] != Kinds.PART;
    }

    private int mostCommonColor() {
        int[] cnt = new int[Kinds.NUM_COLORS];
        for (int i = 0; i < b.n; i++)
            if (L.active[i] && b.entity[i] == Kinds.PIECE && b.power[i] == Kinds.NONE && b.color[i] >= 0) cnt[b.color[i]]++;
        int best = 0;
        for (int k = 1; k < cnt.length; k++) if (cnt[k] > cnt[best]) best = k;
        return best;
    }

    // =================================================================== combos

    private void combo(int from, int to) {
        byte pf = b.power[from], pt = b.power[to];
        boolean track = b.floor[from] == Kinds.FLOOR_TRACK || b.floor[to] == Kinds.FLOOR_TRACK;
        int d = to;
        takeOut(from);
        takeOut(to);
        addPraise(TurnResult.Praise.COMBO_CHAIN);
        byte lo = (byte) Math.min(pf, pt), hi = (byte) Math.max(pf, pt);
        boolean nitroA = Kinds.isNitro(pf), nitroB = Kinds.isNitro(pt);
        List<Integer> cells = new ArrayList<>();
        int r = d / b.cols, c = d % b.cols;

        if (pf == Kinds.SPRAYER && pt == Kinds.SPRAYER) {
            // Every piece cleared and one obstacle layer stripped board-wide.
            for (int i = 0; i < b.n; i++) if (L.active[i]) cells.add(i);
            add(new Step(Step.Type.COMBO_DETONATE, toArray(cells), from, to, 55));
            for (int i : cells) { hit(i); if (track) paintTrack(i); }
            return;
        }
        if (pf == Kinds.SPRAYER || pt == Kinds.SPRAYER) {
            byte other = pf == Kinds.SPRAYER ? pt : pf;
            int col = mostCommonColor();
            List<Integer> converted = new ArrayList<>();
            for (int i = 0; i < b.n; i++)
                if (L.active[i] && b.entity[i] == Kinds.PIECE && b.power[i] == Kinds.NONE && b.color[i] == col && b.clamp[i] == 0) {
                    byte p = other;
                    if (Kinds.isNitro(other)) p = b.rng.nextInt(2) == 0 ? Kinds.NITRO_H : Kinds.NITRO_V;
                    countColor(b.color[i]);
                    b.power[i] = p;
                    b.color[i] = -1;
                    converted.add(i);
                }
            add(new Step(Step.Type.COMBO_DETONATE, toArray(converted), from, to, 50 + other));
            for (int i : converted) enqueueFire(i);
            return;
        }
        if (pf == Kinds.AIRBAG && pt == Kinds.AIRBAG) {
            cells.addAll(diamond(d, 4));
        } else if ((pf == Kinds.AIRBAG && nitroB) || (pt == Kinds.AIRBAG && nitroA)) {
            for (int rr = r - 1; rr <= r + 1; rr++)
                if (rr >= 0 && rr < b.rows) for (int k = 0; k < b.cols; k++) cells.add(rr * b.cols + k);
            for (int cc = c - 1; cc <= c + 1; cc++)
                if (cc >= 0 && cc < b.cols) for (int k = 0; k < b.rows; k++) cells.add(k * b.cols + cc);
        } else if (nitroA && nitroB) {
            for (int k = 0; k < b.cols; k++) cells.add(r * b.cols + k);
            for (int k = 0; k < b.rows; k++) cells.add(k * b.cols + c);
        } else if (pf == Kinds.BUGGY && pt == Kinds.BUGGY) {
            cells.add(d);
            cells.addAll(neighbors(d));
            add(new Step(Step.Type.COMBO_DETONATE, toArray(cells), from, to, 33));
            for (int i : cells) { hit(i); if (track) paintTrack(i); }
            for (int k = 0; k < 3; k++) buggyFly(d, Kinds.NONE, track);
            return;
        } else if (pf == Kinds.BUGGY || pt == Kinds.BUGGY) {
            byte carried = pf == Kinds.BUGGY ? pt : pf;
            cells.add(d);
            cells.addAll(neighbors(d));
            add(new Step(Step.Type.COMBO_DETONATE, toArray(cells), from, to, 30 + carried));
            for (int i : cells) { hit(i); if (track) paintTrack(i); }
            buggyFly(d, carried, track);
            return;
        }
        add(new Step(Step.Type.COMBO_DETONATE, toArray(dedupe(cells)), from, to, lo * 10 + hi));
        for (int i : dedupe(cells)) { hit(i); if (track) paintTrack(i); }
    }

    // =================================================================== gravity

    /** Gravity, refills and part collection until nothing moves. */
    private boolean settle() {
        boolean any = false;
        for (int guard = 0; guard < 100; guard++) {
            boolean g = gravity();
            boolean p = collectParts();
            if (!g && !p) break;
            any = true;
        }
        return any;
    }

    private boolean gravity() {
        boolean any = false;
        int[] feeder = L.feeder;
        for (int iter = 0; iter < 400; iter++) {
            boolean changed = false;
            for (int r = b.rows - 1; r >= 0; r--)
                for (int c = 0; c < b.cols; c++) {
                    int i = r * b.cols + c;
                    if (!L.active[i] || b.entity[i] != Kinds.EMPTY) continue;
                    int f = feeder[i];
                    if (f == Level.SPAWN) { spawnAt(i); changed = true; }
                    else if (b.movable(f)) { moveEntity(f, i); changed = true; }
                }
            if (changed) { any = true; continue; }
            // Nothing can fall straight down: slide one piece diagonally around a blocker.
            boolean slid = false;
            for (int r = b.rows - 1; r >= 1 && !slid; r--)
                for (int c = 0; c < b.cols && !slid; c++) {
                    int i = r * b.cols + c;
                    if (!L.active[i] || b.entity[i] != Kinds.EMPTY) continue;
                    for (int dc = -1; dc <= 1 && !slid; dc += 2) {
                        int sc = c + dc;
                        if (sc < 0 || sc >= b.cols) continue;
                        int s = (r - 1) * b.cols + sc, below = r * b.cols + sc;
                        if (!b.movable(s) || L.wallBelow[s]) continue;
                        if (L.active[below] && b.entity[below] == Kinds.EMPTY) continue; // s is still falling
                        moveEntity(s, i);
                        slid = true;
                    }
                }
            if (!slid) break;
            any = true;
        }
        if (any) add(new Step(Step.Type.GRAVITY_DROP, null, -1, -1, 0));
        return any;
    }

    private void spawnAt(int i) {
        int c = i % b.cols;
        int partsOnBoard = b.count(Kinds.PART);
        if (L.dispenser[i] && b.partsSpawned < L.partsTotal && partsOnBoard < L.partsMaxOnBoard
                && (b.spawnsSinceLastPart >= 6 || b.rng.nextDouble() < 0.25)) {
            b.entity[i] = Kinds.PART;
            b.color[i] = -1;
            b.partsSpawned++;
            b.spawnsSinceLastPart = 0;
        } else {
            b.entity[i] = Kinds.PIECE;
            b.power[i] = Kinds.NONE;
            b.color[i] = (byte) b.nextRefillColor(c);
            // Cascade damping: a random refill that would complete a match on the spot is usually
            // re-rolled, so wins come from planning more than from lucky chain reactions.
            // Blueprint refill queues are never altered.
            if (L.refills == null) {
                for (int t = 0; t < 3 && MatchFinder.matchesAt(b, i) && b.rng.nextDouble() < L.damping; t++)
                    b.color[i] = (byte) b.rng.nextInt(L.colors);
            }
            b.spawnsSinceLastPart++;
        }
        b.power[i] = Kinds.NONE;
        b.layers[i] = 0;
        b.uid[i] = b.nextUid++;
    }

    private void moveEntity(int f, int i) {
        b.entity[i] = b.entity[f];
        b.color[i] = b.color[f];
        b.power[i] = b.power[f];
        b.layers[i] = b.layers[f];
        b.uid[i] = b.uid[f];
        b.entity[f] = Kinds.EMPTY;
        b.color[f] = -1;
        b.power[f] = Kinds.NONE;
        b.layers[f] = 0;
        b.uid[f] = 0;
    }

    private boolean collectParts() {
        List<Integer> got = new ArrayList<>();
        for (int i = 0; i < b.n; i++)
            if (L.exit[i] && b.entity[i] == Kinds.PART) {
                b.entity[i] = Kinds.EMPTY;
                b.uid[i] = 0;
                countGoal(GoalSpec.Type.PART);
                got.add(i);
            }
        if (!got.isEmpty()) add(new Step(Step.Type.PART_COLLECT, toArray(got), -1, -1, 0));
        return !got.isEmpty();
    }

    // =================================================================== end of turn

    private void endTurn(boolean spent) {
        if (spent && L.conveyors.length > 0 && !b.allGoalsDone()) {
            shiftConveyors();
            cascade(null, false);
        }
        if (spent && oilCleared == 0 && !b.allGoalsDone() && b.count(Kinds.OIL) > 0) spreadOil();
        if (res.obstaclesCleared >= 3) addPraise(TurnResult.Praise.BIG_CLEAR);

        if (b.allGoalsDone()) {
            res.status = TurnResult.Status.WON;
            res.movesLeftAtWin = b.movesLeft;
            res.coinsAwarded = L.baseCoins + 2 * b.movesLeft;
            if (b.movesLeft >= 3) addPraise(TurnResult.Praise.MOVES_SAVED);
            if (record) victoryLap();
        } else if (b.movesLeft <= 0) {
            res.status = TurnResult.Status.LOST;
        } else {
            res.status = TurnResult.Status.PLAYING;
            if (!Moves.hasLegalMove(b)) shuffle();
        }
    }

    private void shiftConveyors() {
        List<Integer> moved = new ArrayList<>();
        for (int[] loop : L.conveyors) {
            int k = loop.length;
            if (k < 2) continue;
            int last = loop[k - 1];
            byte e = b.entity[last], co = b.color[last], p = b.power[last], ly = b.layers[last], cl = b.clamp[last];
            int u = b.uid[last];
            for (int j = k - 1; j > 0; j--) {
                int to = loop[j], from = loop[j - 1];
                b.entity[to] = b.entity[from];
                b.color[to] = b.color[from];
                b.power[to] = b.power[from];
                b.layers[to] = b.layers[from];
                b.clamp[to] = b.clamp[from];
                b.uid[to] = b.uid[from];
            }
            int first = loop[0];
            b.entity[first] = e; b.color[first] = co; b.power[first] = p; b.layers[first] = ly; b.clamp[first] = cl; b.uid[first] = u;
            for (int x : loop) moved.add(x);
        }
        add(new Step(Step.Type.CONVEYOR_SHIFT, toArray(moved), -1, -1, 0));
    }

    private void spreadOil() {
        List<int[]> opts = new ArrayList<>();
        for (int i = 0; i < b.n; i++) {
            if (!L.active[i] || b.entity[i] != Kinds.OIL) continue;
            for (int nb : neighbors(i)) if (b.isPlainPiece(nb) && b.clamp[nb] == 0) opts.add(new int[]{i, nb});
        }
        if (opts.isEmpty()) return;
        int[] o = opts.get(b.rng.nextInt(opts.size()));
        b.entity[o[1]] = Kinds.OIL;
        b.color[o[1]] = -1;
        b.uid[o[1]] = b.nextUid++;
        add(new Step(Step.Type.OIL_SPREAD, new int[]{o[1]}, o[0], o[1], 0));
    }

    /** Each unused move becomes a Nitro or RC Buggy, then they all fire in a celebration. Cosmetic. */
    private void victoryLap() {
        List<Integer> made = new ArrayList<>();
        for (int k = 0; k < b.movesLeft; k++) {
            List<Integer> plain = new ArrayList<>();
            for (int i = 0; i < b.n; i++) if (b.isPlainPiece(i) && b.clamp[i] == 0) plain.add(i);
            if (plain.isEmpty()) break;
            int i = plain.get(b.rng.nextInt(plain.size()));
            int pick = b.rng.nextInt(3);
            b.power[i] = pick == 0 ? Kinds.NITRO_H : pick == 1 ? Kinds.NITRO_V : Kinds.BUGGY;
            b.color[i] = -1;
            made.add(i);
        }
        add(new Step(Step.Type.VICTORY_LAP, toArray(made), -1, -1, 0));
        for (int i : made) if (b.power[i] != Kinds.NONE && b.entity[i] == Kinds.PIECE) enqueueFire(i);
        processFires();
        settle();
    }

    /** Free reshuffle when no legal move exists. Pieces keep their identity and fly to new cells. */
    private void shuffle() {
        List<Integer> cells = new ArrayList<>();
        for (int i = 0; i < b.n; i++) if (b.isPlainPiece(i) && b.clamp[i] == 0) cells.add(i);
        int m = cells.size();
        byte[] cols = new byte[m];
        int[] uids = new int[m];
        for (int k = 0; k < m; k++) { cols[k] = b.color[cells.get(k)]; uids[k] = b.uid[cells.get(k)]; }
        for (int attempt = 0; attempt < 200; attempt++) {
            for (int k = m - 1; k > 0; k--) {
                int j = b.rng.nextInt(k + 1);
                byte t = cols[k]; cols[k] = cols[j]; cols[j] = t;
                int u = uids[k]; uids[k] = uids[j]; uids[j] = u;
            }
            if (attempt > 100) for (int k = 0; k < m; k++) cols[k] = (byte) b.rng.nextInt(L.colors);
            for (int k = 0; k < m; k++) { b.color[cells.get(k)] = cols[k]; b.uid[cells.get(k)] = uids[k]; }
            if (!MatchFinder.hasAnyMatch(b) && Moves.hasLegalMove(b)) break;
        }
        res.shuffled = true;
        add(new Step(Step.Type.DEADLOCK_SHUFFLE, toArray(cells), -1, -1, 0));
    }

    // =================================================================== helpers

    private List<Integer> neighbors(int i) {
        List<Integer> out = new ArrayList<>(4);
        int r = i / b.cols, c = i % b.cols;
        if (r > 0 && L.active[i - b.cols]) out.add(i - b.cols);
        if (r + 1 < b.rows && L.active[i + b.cols]) out.add(i + b.cols);
        if (c > 0 && L.active[i - 1]) out.add(i - 1);
        if (c + 1 < b.cols && L.active[i + 1]) out.add(i + 1);
        return out;
    }

    private List<Integer> diamond(int center, int radius) {
        List<Integer> out = new ArrayList<>();
        int r = center / b.cols, c = center % b.cols;
        for (int rr = r - radius; rr <= r + radius; rr++)
            for (int cc = c - radius; cc <= c + radius; cc++) {
                if (rr < 0 || rr >= b.rows || cc < 0 || cc >= b.cols) continue;
                if (Math.abs(rr - r) + Math.abs(cc - c) <= radius) out.add(rr * b.cols + cc);
            }
        return out;
    }

    private static List<Integer> dedupe(List<Integer> l) {
        List<Integer> out = new ArrayList<>();
        java.util.HashSet<Integer> seen = new java.util.HashSet<>();
        for (int x : l) if (seen.add(x)) out.add(x);
        return out;
    }

    private static int[] toArray(List<Integer> l) {
        int[] a = new int[l.size()];
        for (int k = 0; k < a.length; k++) a[k] = l.get(k);
        return a;
    }

    private void addPraise(TurnResult.Praise p) {
        if (!res.praise.contains(p)) res.praise.add(p);
    }

    private void add(Step s) {
        if (!record) return;
        s.after = new Step.Snapshot(b);
        res.steps.add(s);
    }
}
