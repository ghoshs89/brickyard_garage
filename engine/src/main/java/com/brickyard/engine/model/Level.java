package com.brickyard.engine.model;

import com.brickyard.engine.util.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Static, immutable level data parsed from content/levels/*.json or content/blueprints/*.json.
 *
 * Layout legend (one char per cell, rows top to bottom):
 *   '#' or ' ' hole            '.' random piece
 *   R B Y G O P fixed colour   '1' '2' '3' Stud Crate with that many layers
 *   '~' Oil Spill              '*' Rare Car Part
 *   'h' Nitro (row)  'v' Nitro (column)  'u' RC Buggy  'a' Airbag  's' Color Sprayer
 * Floor legend: '.' none, 't' Track Baseplate, 'r' Rust Plate.
 * Overlay legend: '.' none, '1' '2' Wheel Clamp layers.
 */
public final class Level {
    public enum Kind { STORY, BLUEPRINT, TRACK_DAY }
    public enum Difficulty { WARMUP, CHALLENGE }

    public static final int SPAWN = -1;

    public String id;
    public Kind kind = Kind.STORY;
    public int bay;          // 1..3 (0 for Track Day)
    public int index;        // order within its list
    public String title = "";
    public Difficulty difficulty = Difficulty.WARMUP;
    public int moves;
    public int colors = 5;
    public long seed;
    public int rows, cols;
    public String[] layout;
    public String[] floor;
    public String[] overlay;
    public boolean[] active;
    public boolean[] wallBelow;
    public boolean[] wallRight;
    public int[] portalFrom = new int[0]; // parallel arrays: pieces leaving portalFrom[k] enter portalTo[k]
    public int[] portalTo = new int[0];
    public boolean[] dispenser;
    public boolean[] exit;
    public int[][] conveyors = new int[0][]; // closed loops of cell indices; contents step one place per turn
    public int partsTotal;
    public int partsMaxOnBoard = 2;
    public final List<GoalSpec> goals = new ArrayList<>();
    public int baseCoins = 10;
    public String boNote = "";           // Grandpa Bo's Workshop Note (shown before Builder Challenges)
    public String tutorial = "";         // short first-time teaching line
    public int exactMoves;               // Blueprint: proven solvable in <= exactMoves
    public String[] refills;             // Blueprint: visible refill queue per column (top of string = next)
    public double botMin = -1, botMax = -1; // optional per-level override of the bot band
    public double damping = 0.8;         // chance a refill that would match on the spot is re-rolled
    public List<int[]> witness;          // Blueprint: solver's recorded winning line (optional)

    /** feeder[i]: the cell that falls into i, or SPAWN when new pieces appear at i. Computed once. */
    public int[] feeder;

    public int idx(int r, int c) {
        return r * cols + c;
    }

    public boolean inside(int r, int c) {
        return r >= 0 && r < rows && c >= 0 && c < cols;
    }

    public boolean isBlueprint() {
        return kind == Kind.BLUEPRINT;
    }

    // ------------------------------------------------------------------ parsing

    public static Level parse(String json) {
        return fromMap(Json.parseObject(json));
    }

    public static Level fromMap(Map<String, Object> m) {
        Level L = new Level();
        L.id = Json.getString(m, "id", null);
        if (L.id == null) throw new IllegalArgumentException("level missing id");
        L.kind = Kind.valueOf(Json.getString(m, "kind", "STORY"));
        L.bay = Json.getInt(m, "bay", 0);
        L.index = Json.getInt(m, "index", 0);
        L.title = Json.getString(m, "title", "");
        L.difficulty = Difficulty.valueOf(Json.getString(m, "difficulty", "WARMUP"));
        L.moves = Json.getInt(m, "moves", 20);
        L.colors = Json.getInt(m, "colors", 5);
        L.seed = Json.getLong(m, "seed", L.id.hashCode());
        L.baseCoins = Json.getInt(m, "baseCoins", 10);
        L.boNote = Json.getString(m, "boNote", "");
        L.tutorial = Json.getString(m, "tutorial", "");
        L.exactMoves = Json.getInt(m, "exactMoves", 0);
        L.partsTotal = Json.getInt(m, "partsTotal", 0);
        L.partsMaxOnBoard = Json.getInt(m, "partsMaxOnBoard", 2);
        L.damping = Json.getDouble(m, "damping", 0.8);
        List<Object> band = Json.getList(m, "botBand");
        if (band.size() == 2) {
            L.botMin = ((Number) band.get(0)).doubleValue();
            L.botMax = ((Number) band.get(1)).doubleValue();
        }

        L.layout = strings(Json.getList(m, "layout"));
        L.rows = L.layout.length;
        if (L.rows == 0) throw new IllegalArgumentException(L.id + ": empty layout");
        L.cols = L.layout[0].length();
        if (L.rows > 9 || L.cols > 9) throw new IllegalArgumentException(L.id + ": board larger than 9x9");
        for (String row : L.layout)
            if (row.length() != L.cols) throw new IllegalArgumentException(L.id + ": ragged layout row '" + row + "'");
        L.floor = m.containsKey("floor") ? strings(Json.getList(m, "floor")) : blank(L.rows, L.cols);
        L.overlay = m.containsKey("overlay") ? strings(Json.getList(m, "overlay")) : blank(L.rows, L.cols);
        checkShape(L, L.floor, "floor");
        checkShape(L, L.overlay, "overlay");

        int n = L.rows * L.cols;
        L.active = new boolean[n];
        L.wallBelow = new boolean[n];
        L.wallRight = new boolean[n];
        L.dispenser = new boolean[n];
        L.exit = new boolean[n];
        for (int r = 0; r < L.rows; r++)
            for (int c = 0; c < L.cols; c++) {
                char ch = L.layout[r].charAt(c);
                L.active[L.idx(r, c)] = ch != '#' && ch != ' ';
            }

        for (Object o : Json.getList(m, "walls")) {
            @SuppressWarnings("unchecked") Map<String, Object> w = (Map<String, Object>) o;
            int r = Json.getInt(w, "r", -1), c = Json.getInt(w, "c", -1);
            String side = Json.getString(w, "side", "below");
            if (!L.inside(r, c)) throw new IllegalArgumentException(L.id + ": wall outside board");
            if (side.equals("below")) L.wallBelow[L.idx(r, c)] = true;
            else if (side.equals("right")) L.wallRight[L.idx(r, c)] = true;
            else throw new IllegalArgumentException(L.id + ": wall side must be below/right");
        }
        List<Object> portals = Json.getList(m, "portals");
        L.portalFrom = new int[portals.size()];
        L.portalTo = new int[portals.size()];
        for (int k = 0; k < portals.size(); k++) {
            @SuppressWarnings("unchecked") Map<String, Object> p = (Map<String, Object>) portals.get(k);
            L.portalFrom[k] = cell(L, Json.getList(p, "from"));
            L.portalTo[k] = cell(L, Json.getList(p, "to"));
        }
        for (Object o : Json.getList(m, "dispensers")) L.dispenser[cell(L, asList(o))] = true;
        for (Object o : Json.getList(m, "exits")) L.exit[cell(L, asList(o))] = true;
        List<Object> conv = Json.getList(m, "conveyors");
        L.conveyors = new int[conv.size()][];
        for (int k = 0; k < conv.size(); k++) {
            List<Object> loop = asList(conv.get(k));
            L.conveyors[k] = new int[loop.size()];
            for (int j = 0; j < loop.size(); j++) L.conveyors[k][j] = cell(L, asList(loop.get(j)));
        }
        for (Object o : Json.getList(m, "goals")) {
            @SuppressWarnings("unchecked") Map<String, Object> g = (Map<String, Object>) o;
            GoalSpec.Type t = GoalSpec.Type.valueOf(Json.getString(g, "type", ""));
            int color = t == GoalSpec.Type.COLOR ? Kinds.colorFromKey(Json.getString(g, "color", "")) : -1;
            int count = Json.getInt(g, "count", GoalSpec.ALL);
            L.goals.add(new GoalSpec(t, color, count));
        }
        if (m.containsKey("refills")) {
            L.refills = strings(Json.getList(m, "refills"));
            if (L.refills.length != L.cols) throw new IllegalArgumentException(L.id + ": need one refill string per column");
        }
        if (m.containsKey("witness")) {
            L.witness = new ArrayList<>();
            for (Object o : Json.getList(m, "witness")) {
                List<Object> mv = asList(o);
                int[] a = new int[mv.size()];
                for (int j = 0; j < a.length; j++) a[j] = ((Number) mv.get(j)).intValue();
                L.witness.add(a);
            }
        }
        L.computeFeeders();
        return L;
    }

    private void computeFeeders() {
        int n = rows * cols;
        feeder = new int[n];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                int i = idx(r, c);
                if (!active[i]) { feeder[i] = SPAWN; continue; }
                int f = SPAWN;
                // Portals override the normal column feed.
                for (int k = 0; k < portalTo.length; k++) if (portalTo[k] == i) f = portalFrom[k];
                if (f == SPAWN) {
                    int k = r - 1;
                    while (k >= 0) {
                        if (wallBelow[idx(k, c)]) break; // a bulkhead blocks falling across this edge
                        if (active[idx(k, c)]) { f = idx(k, c); break; }
                        k--; // pieces fall straight through holes
                    }
                    // A portal mouth only feeds through its portal, never straight down.
                    if (f != SPAWN) for (int pf : portalFrom) if (pf == f) { f = SPAWN; break; }
                }
                feeder[i] = f;
            }
    }

    // ------------------------------------------------------------------ helpers

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object o) {
        if (!(o instanceof List)) throw new IllegalArgumentException("expected [r, c]");
        return (List<Object>) o;
    }

    private static int cell(Level L, List<Object> rc) {
        if (rc.size() != 2) throw new IllegalArgumentException(L.id + ": expected [r, c]");
        int r = ((Number) rc.get(0)).intValue(), c = ((Number) rc.get(1)).intValue();
        if (!L.inside(r, c)) throw new IllegalArgumentException(L.id + ": cell [" + r + "," + c + "] outside board");
        return L.idx(r, c);
    }

    private static String[] strings(List<Object> l) {
        String[] a = new String[l.size()];
        for (int k = 0; k < a.length; k++) a[k] = (String) l.get(k);
        return a;
    }

    private static String[] blank(int rows, int cols) {
        String[] a = new String[rows];
        StringBuilder b = new StringBuilder();
        for (int c = 0; c < cols; c++) b.append('.');
        for (int r = 0; r < rows; r++) a[r] = b.toString();
        return a;
    }

    private static void checkShape(Level L, String[] g, String name) {
        if (g.length != L.rows) throw new IllegalArgumentException(L.id + ": " + name + " has wrong row count");
        for (String row : g)
            if (row.length() != L.cols) throw new IllegalArgumentException(L.id + ": " + name + " row width mismatch");
    }
}
