package com.brickyard.engine.rules;

import com.brickyard.engine.model.Board;
import com.brickyard.engine.model.Kinds;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds match groups and classifies them by Playrix's shape precedence:
 * 5-in-a-line (Sprayer) > T/L (Airbag) > 2x2 square (Buggy) > 4-in-a-line (Nitro) > 3-in-a-line.
 * Clamped pieces match in place. Power-ups are colourless and never match.
 */
public final class MatchFinder {
    private MatchFinder() {}

    public static final class Group {
        public int color;
        public final List<Integer> cells = new ArrayList<>();
        public byte power = Kinds.NONE;
        public int intersection = -1; // T/L corner, preferred Airbag spawn cell
        public boolean square;
    }

    static boolean matchable(Board b, int i) {
        return b.level.active[i] && b.entity[i] == Kinds.PIECE && b.power[i] == Kinds.NONE && b.color[i] >= 0;
    }

    private static boolean same(Board b, int r, int c, int col) {
        if (r < 0 || r >= b.rows || c < 0 || c >= b.cols) return false;
        int i = r * b.cols + c;
        return matchable(b, i) && b.color[i] == col;
    }

    public static List<Group> find(Board b) {
        int n = b.n, cols = b.cols, rows = b.rows;
        int[] parent = new int[n];
        boolean[] in = new boolean[n];
        boolean[] inSquare = new boolean[n];
        int[] hLen = new int[n], vLen = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;

        // Horizontal runs
        for (int r = 0; r < rows; r++) {
            int c = 0;
            while (c < cols) {
                int i = r * cols + c;
                if (!matchable(b, i)) { c++; continue; }
                int col = b.color[i], e = c;
                while (e + 1 < cols && same(b, r, e + 1, col)) e++;
                int len = e - c + 1;
                if (len >= 3) for (int k = c; k <= e; k++) {
                    int j = r * cols + k;
                    in[j] = true;
                    hLen[j] = len;
                    union(parent, i, j);
                }
                c = e + 1;
            }
        }
        // Vertical runs
        for (int c = 0; c < cols; c++) {
            int r = 0;
            while (r < rows) {
                int i = r * cols + c;
                if (!matchable(b, i)) { r++; continue; }
                int col = b.color[i], e = r;
                while (e + 1 < rows && same(b, e + 1, c, col)) e++;
                int len = e - r + 1;
                if (len >= 3) for (int k = r; k <= e; k++) {
                    int j = k * cols + c;
                    in[j] = true;
                    vLen[j] = len;
                    union(parent, i, j);
                }
                r = e + 1;
            }
        }
        // 2x2 squares
        for (int r = 0; r + 1 < rows; r++)
            for (int c = 0; c + 1 < cols; c++) {
                int i = r * cols + c;
                if (!matchable(b, i)) continue;
                int col = b.color[i];
                if (same(b, r, c + 1, col) && same(b, r + 1, c, col) && same(b, r + 1, c + 1, col)) {
                    int[] q = {i, i + 1, i + cols, i + cols + 1};
                    for (int j : q) { in[j] = true; inSquare[j] = true; union(parent, i, j); }
                }
            }

        List<Group> groups = new ArrayList<>();
        int[] groupOf = new int[n];
        for (int i = 0; i < n; i++) groupOf[i] = -1;
        for (int i = 0; i < n; i++) {
            if (!in[i]) continue;
            int root = find(parent, i);
            if (groupOf[root] < 0) {
                Group g = new Group();
                g.color = b.color[i];
                groupOf[root] = groups.size();
                groups.add(g);
            }
            groups.get(groupOf[root]).cells.add(i);
        }
        for (Group g : groups) classify(g, hLen, vLen, inSquare);
        return groups;
    }

    private static void classify(Group g, int[] hLen, int[] vLen, boolean[] inSquare) {
        int maxH = 0, maxV = 0;
        for (int i : g.cells) {
            maxH = Math.max(maxH, hLen[i]);
            maxV = Math.max(maxV, vLen[i]);
            if (inSquare[i]) g.square = true;
            if (hLen[i] >= 3 && vLen[i] >= 3 && g.intersection < 0) g.intersection = i;
        }
        if (maxH >= 5 || maxV >= 5) g.power = Kinds.SPRAYER;
        else if (g.intersection >= 0) g.power = Kinds.AIRBAG;
        else if (g.square) g.power = Kinds.BUGGY;
        else if (maxH == 4) g.power = Kinds.NITRO_V;  // 4 in a row -> vertical Nitro
        else if (maxV == 4) g.power = Kinds.NITRO_H;  // 4 in a column -> horizontal Nitro
    }

    public static boolean hasAnyMatch(Board b) {
        for (int i = 0; i < b.n; i++) if (matchable(b, i) && matchesAt(b, i)) return true;
        return false;
    }

    /** Does the piece at i currently sit in a line of 3+ or a 2x2 square? */
    public static boolean matchesAt(Board b, int i) {
        if (!matchable(b, i)) return false;
        int r = i / b.cols, c = i % b.cols, col = b.color[i];
        int h = 1, k = c - 1;
        while (same(b, r, k, col)) { h++; k--; }
        k = c + 1;
        while (same(b, r, k, col)) { h++; k++; }
        if (h >= 3) return true;
        int v = 1;
        k = r - 1;
        while (same(b, k, c, col)) { v++; k--; }
        k = r + 1;
        while (same(b, k, c, col)) { v++; k++; }
        if (v >= 3) return true;
        for (int dr = -1; dr <= 0; dr++)
            for (int dc = -1; dc <= 0; dc++)
                if (same(b, r + dr, c + dc, col) && same(b, r + dr + 1, c + dc, col)
                        && same(b, r + dr, c + dc + 1, col) && same(b, r + dr + 1, c + dc + 1, col)) return true;
        return false;
    }

    private static int find(int[] p, int x) {
        while (p[x] != x) { p[x] = p[p[x]]; x = p[x]; }
        return x;
    }

    private static void union(int[] p, int a, int b) {
        int ra = find(p, a), rb = find(p, b);
        if (ra != rb) p[ra] = rb;
    }
}
