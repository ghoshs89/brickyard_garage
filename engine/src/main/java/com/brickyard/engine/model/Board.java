package com.brickyard.engine.model;

import com.brickyard.engine.util.Json;
import com.brickyard.engine.util.Rng;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mutable game state for one level attempt. All arrays are indexed by cell = r * cols + c.
 * Topology (holes, walls, portals, feeders) lives in the immutable {@link Level}.
 * copy() is cheap so the solver and bot can branch freely.
 */
public final class Board {
    public final Level level;
    public final int rows, cols, n;

    public byte[] entity;   // Kinds.EMPTY / PIECE / CRATE / OIL / PART
    public byte[] color;    // piece colour, or -1
    public byte[] power;    // Kinds.NONE / NITRO_H / ...
    public byte[] layers;   // crate layers
    public byte[] clamp;    // wheel clamp layers on a piece
    public byte[] floor;    // Kinds.FLOOR_*
    public int[] uid;       // stable identity per entity, for animation

    public int movesLeft;
    public int movesUsed;
    public final Rng rng;
    public int nextUid = 1;
    public int partsSpawned;
    public int spawnsSinceLastPart;
    public int[] refillPos;           // blueprint: position in each column's refill string
    public int[] collected;           // per goal: amount collected so far (count-type goals)
    public int[] initialRemaining;    // per goal: remaining at the start, for "closest yet" progress
    public int[] tools = new int[3];  // mechanic tools available during this attempt

    private Board(Level level, long seed) {
        this.level = level;
        this.rows = level.rows;
        this.cols = level.cols;
        this.n = rows * cols;
        this.rng = new Rng(seed);
        entity = new byte[n];
        color = new byte[n];
        power = new byte[n];
        layers = new byte[n];
        clamp = new byte[n];
        floor = new byte[n];
        uid = new int[n];
        refillPos = new int[cols];
        collected = new int[level.goals.size()];
        initialRemaining = new int[level.goals.size()];
    }

    // ------------------------------------------------------------------ creation

    /** Builds the starting board. Random cells are filled without ready-made matches and with a legal move. */
    public static Board create(Level level, long seed) {
        for (int attempt = 0; attempt < 200; attempt++) {
            Board b = new Board(level, Rng.mix(seed, attempt));
            b.fillFromLayout();
            if (!com.brickyard.engine.rules.MatchFinder.hasAnyMatch(b)
                    && com.brickyard.engine.rules.Moves.hasLegalMove(b)) {
                b.movesLeft = level.moves;
                for (int g = 0; g < level.goals.size(); g++) b.initialRemaining[g] = b.remaining(g);
                return b;
            }
        }
        throw new IllegalStateException(level.id + ": could not build a starting board without matches and with a legal move");
    }

    /** Builds the board exactly as laid out, without the no-match / legal-move checks (tests, solver fixtures). */
    public static Board createUnchecked(Level level, long seed) {
        Board b = new Board(level, seed);
        b.fillFromLayout();
        b.movesLeft = level.moves;
        for (int g = 0; g < level.goals.size(); g++) b.initialRemaining[g] = b.remaining(g);
        return b;
    }

    private void fillFromLayout() {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                int i = level.idx(r, c);
                color[i] = -1;
                if (!level.active[i]) continue;
                char ch = level.layout[r].charAt(c);
                char fl = level.floor[r].charAt(c);
                char ov = level.overlay[r].charAt(c);
                floor[i] = fl == 't' ? Kinds.FLOOR_TRACK : fl == 'r' ? Kinds.FLOOR_RUST : Kinds.FLOOR_NONE;
                int ci = Kinds.COLOR_LETTERS.indexOf(ch);
                if (ch == '.') {
                    entity[i] = Kinds.PIECE;
                    color[i] = (byte) safeColor(r, c);
                } else if (ci >= 0) {
                    entity[i] = Kinds.PIECE;
                    color[i] = (byte) ci;
                } else if (ch >= '1' && ch <= '3') {
                    entity[i] = Kinds.CRATE;
                    layers[i] = (byte) (ch - '0');
                } else if (ch == '~') {
                    entity[i] = Kinds.OIL;
                } else if (ch == '*') {
                    entity[i] = Kinds.PART;
                    partsSpawned++;
                } else if ("hvuas".indexOf(ch) >= 0) {
                    entity[i] = Kinds.PIECE;
                    power[i] = (byte) ("hvuas".indexOf(ch) + 1);
                } else throw new IllegalArgumentException(level.id + ": unknown layout char '" + ch + "'");
                if (ov == '1' || ov == '2') clamp[i] = (byte) (ov - '0');
                uid[i] = nextUid++;
            }
    }

    /** A random colour that does not complete a line of three with the cells above/left. */
    private int safeColor(int r, int c) {
        for (int t = 0; t < 20; t++) {
            int col = rng.nextInt(level.colors);
            if (c >= 2 && sameColorAt(r, c - 1, col) && sameColorAt(r, c - 2, col)) continue;
            if (r >= 2 && sameColorAt(r - 1, c, col) && sameColorAt(r - 2, c, col)) continue;
            if (r >= 1 && c >= 1 && sameColorAt(r - 1, c, col) && sameColorAt(r, c - 1, col) && sameColorAt(r - 1, c - 1, col)) continue;
            return col;
        }
        return rng.nextInt(level.colors);
    }

    private boolean sameColorAt(int r, int c, int col) {
        if (!level.inside(r, c)) return false;
        int i = level.idx(r, c);
        return entity[i] == Kinds.PIECE && power[i] == Kinds.NONE && color[i] == col;
    }

    /** Colour of the next new piece for column c (visible refill queue on Blueprints, random otherwise). */
    public int nextRefillColor(int c) {
        if (level.refills != null) {
            String q = level.refills[c];
            if (refillPos[c] < q.length()) {
                int col = Kinds.COLOR_LETTERS.indexOf(q.charAt(refillPos[c]++));
                if (col >= 0) return col;
            }
        }
        return rng.nextInt(level.colors);
    }

    // ------------------------------------------------------------------ queries

    public boolean active(int i) {
        return level.active[i];
    }

    public boolean isPlainPiece(int i) {
        return entity[i] == Kinds.PIECE && power[i] == Kinds.NONE;
    }

    /** Can this cell's content be swapped or fall? */
    public boolean movable(int i) {
        return level.active[i] && (entity[i] == Kinds.PIECE || entity[i] == Kinds.PART) && clamp[i] == 0;
    }

    public int count(byte kind) {
        int k = 0;
        for (int i = 0; i < n; i++) if (level.active[i] && entity[i] == kind) k++;
        return k;
    }

    public int countFloor(byte f) {
        int k = 0;
        for (int i = 0; i < n; i++) if (level.active[i] && floor[i] == f) k++;
        return k;
    }

    public int countClamps() {
        int k = 0;
        for (int i = 0; i < n; i++) if (clamp[i] > 0) k++;
        return k;
    }

    /** What is still left for goal g. 0 or less means the goal is done. */
    public int remaining(int g) {
        GoalSpec s = level.goals.get(g);
        switch (s.type) {
            case OIL: return count(Kinds.OIL);
            case RUST: return countFloor(Kinds.FLOOR_RUST);
            case TRACK: {
                int need = s.count == GoalSpec.ALL ? activeCells() : s.count;
                return need - countFloor(Kinds.FLOOR_TRACK);
            }
            case CRATE:
                if (s.count == GoalSpec.ALL) return count(Kinds.CRATE);
                return s.count - collected[g];
            case CLAMP:
                if (s.count == GoalSpec.ALL) return countClamps();
                return s.count - collected[g];
            case PART:
                return (s.count == GoalSpec.ALL ? level.partsTotal : s.count) - collected[g];
            default:
                return s.count - collected[g];
        }
    }

    public int activeCells() {
        int k = 0;
        for (int i = 0; i < n; i++) if (level.active[i]) k++;
        return k;
    }

    public boolean allGoalsDone() {
        for (int g = 0; g < level.goals.size(); g++) if (remaining(g) > 0) return false;
        return true;
    }

    /** Fraction of the total goal work completed, 0..1 (the "Closest-Yet" bar). */
    public double progress() {
        double done = 0, total = 0;
        for (int g = 0; g < level.goals.size(); g++) {
            int init = Math.max(initialRemaining[g], 1);
            int rem = Math.max(remaining(g), 0);
            total += init;
            done += Math.max(0, init - Math.min(rem, init));
        }
        return total == 0 ? 1 : done / total;
    }

    // ------------------------------------------------------------------ copying

    public Board copy() {
        Board b = new Board(level, 0);
        b.rng.setState(rng.state());
        b.entity = entity.clone();
        b.color = color.clone();
        b.power = power.clone();
        b.layers = layers.clone();
        b.clamp = clamp.clone();
        b.floor = floor.clone();
        b.uid = uid.clone();
        b.movesLeft = movesLeft;
        b.movesUsed = movesUsed;
        b.nextUid = nextUid;
        b.partsSpawned = partsSpawned;
        b.spawnsSinceLastPart = spawnsSinceLastPart;
        b.refillPos = refillPos.clone();
        b.collected = collected.clone();
        b.initialRemaining = initialRemaining.clone();
        b.tools = tools.clone();
        return b;
    }

    /** Stable hash of the full visible state (solver transposition table). */
    public long stateHash() {
        long h = 1469598103934665603L;
        for (int i = 0; i < n; i++) {
            h = (h ^ entity[i]) * 1099511628211L;
            h = (h ^ color[i]) * 1099511628211L;
            h = (h ^ power[i]) * 1099511628211L;
            h = (h ^ (layers[i] | clamp[i] << 3 | floor[i] << 6)) * 1099511628211L;
        }
        for (int c = 0; c < cols; c++) h = (h ^ refillPos[c]) * 1099511628211L;
        for (int g : collected) h = (h ^ g) * 1099511628211L;
        h = (h ^ movesLeft) * 1099511628211L;
        h = (h ^ rng.state()) * 1099511628211L;
        return h;
    }

    // ------------------------------------------------------------------ save / restore

    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("level", level.id);
        m.put("entity", bytes(entity));
        m.put("color", bytes(color));
        m.put("power", bytes(power));
        m.put("layers", bytes(layers));
        m.put("clamp", bytes(clamp));
        m.put("floor", bytes(floor));
        m.put("uid", ints(uid));
        m.put("movesLeft", movesLeft);
        m.put("movesUsed", movesUsed);
        m.put("rng", Long.toString(rng.state()));
        m.put("nextUid", nextUid);
        m.put("partsSpawned", partsSpawned);
        m.put("spawnsSinceLastPart", spawnsSinceLastPart);
        m.put("refillPos", ints(refillPos));
        m.put("collected", ints(collected));
        m.put("initialRemaining", ints(initialRemaining));
        m.put("tools", ints(tools));
        return m;
    }

    public static Board fromJson(Level level, Map<String, Object> m) {
        if (!level.id.equals(Json.getString(m, "level", ""))) throw new IllegalArgumentException("save is for another level");
        Board b = new Board(level, 0);
        b.entity = toBytes(Json.getList(m, "entity"), b.n);
        b.color = toBytes(Json.getList(m, "color"), b.n);
        b.power = toBytes(Json.getList(m, "power"), b.n);
        b.layers = toBytes(Json.getList(m, "layers"), b.n);
        b.clamp = toBytes(Json.getList(m, "clamp"), b.n);
        b.floor = toBytes(Json.getList(m, "floor"), b.n);
        b.uid = toInts(Json.getList(m, "uid"), b.n);
        b.movesLeft = Json.getInt(m, "movesLeft", 0);
        b.movesUsed = Json.getInt(m, "movesUsed", 0);
        b.rng.setState(Long.parseLong(Json.getString(m, "rng", "0")));
        b.nextUid = Json.getInt(m, "nextUid", 1);
        b.partsSpawned = Json.getInt(m, "partsSpawned", 0);
        b.spawnsSinceLastPart = Json.getInt(m, "spawnsSinceLastPart", 0);
        b.refillPos = toInts(Json.getList(m, "refillPos"), b.cols);
        b.collected = toInts(Json.getList(m, "collected"), level.goals.size());
        b.initialRemaining = toInts(Json.getList(m, "initialRemaining"), level.goals.size());
        b.tools = toInts(Json.getList(m, "tools"), 3);
        return b;
    }

    private static List<Object> bytes(byte[] a) {
        List<Object> l = new ArrayList<>(a.length);
        for (byte x : a) l.add((long) x);
        return l;
    }

    private static List<Object> ints(int[] a) {
        List<Object> l = new ArrayList<>(a.length);
        for (int x : a) l.add((long) x);
        return l;
    }

    private static byte[] toBytes(List<Object> l, int n) {
        if (l.size() != n) throw new IllegalArgumentException("save array has wrong length");
        byte[] a = new byte[n];
        for (int k = 0; k < n; k++) a[k] = ((Number) l.get(k)).byteValue();
        return a;
    }

    private static int[] toInts(List<Object> l, int n) {
        if (l.size() != n) throw new IllegalArgumentException("save array has wrong length");
        int[] a = new int[n];
        for (int k = 0; k < n; k++) a[k] = ((Number) l.get(k)).intValue();
        return a;
    }

    /** Compact text dump for tests and debugging. */
    public String dump() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int i = level.idx(r, c);
                char ch;
                if (!level.active[i]) ch = '#';
                else if (entity[i] == Kinds.EMPTY) ch = '_';
                else if (entity[i] == Kinds.CRATE) ch = (char) ('0' + layers[i]);
                else if (entity[i] == Kinds.OIL) ch = '~';
                else if (entity[i] == Kinds.PART) ch = '*';
                else if (power[i] != Kinds.NONE) ch = "?hvuas".charAt(power[i]);
                else ch = Kinds.COLOR_LETTERS.charAt(color[i]);
                sb.append(ch);
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
