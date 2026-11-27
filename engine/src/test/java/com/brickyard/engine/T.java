package com.brickyard.engine;

import com.brickyard.engine.model.Board;
import com.brickyard.engine.model.Level;
import com.brickyard.engine.rules.Step;
import com.brickyard.engine.rules.TurnResult;
import com.brickyard.engine.util.Json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Test helpers: a match-free base pattern and a tiny level builder. */
final class T {
    static final String P0 = "BYGOBYGO";
    static final String P1 = "GOBYGOBY";

    /** 8x8 board that contains no match and in which no two horizontal neighbours across rows line up. */
    static String[] base() {
        String[] rows = new String[8];
        for (int r = 0; r < 8; r++) rows[r] = r % 2 == 0 ? P0 : P1;
        return rows;
    }

    static String[] set(String[] rows, int r, int c, char ch) {
        String[] out = rows.clone();
        char[] a = out[r].toCharArray();
        a[c] = ch;
        out[r] = new String(a);
        return out;
    }

    static final class LB {
        final Map<String, Object> m = new LinkedHashMap<>();

        LB(String[] layout) {
            m.put("id", "test");
            m.put("moves", 20);
            m.put("colors", 6);
            m.put("layout", list(layout));
            // Refills alternate two colours per column, with a different pair per column, so refilled
            // cells rarely start accidental cascades in the tests.
            String[] pairs = {"PR", "RP", "PB", "BP", "RG", "GR", "PY", "YP", "RO"};
            List<Object> ref = new ArrayList<>();
            for (int c = 0; c < layout[0].length(); c++) {
                StringBuilder s = new StringBuilder();
                for (int k = 0; k < 40; k++) s.append(pairs[c % pairs.length].charAt(k % 2));
                ref.add(s.toString());
            }
            m.put("refills", ref);
        }

        LB put(String k, Object v) { m.put(k, v); return this; }
        LB floor(String[] f) { return put("floor", list(f)); }
        LB overlay(String[] o) { return put("overlay", list(o)); }

        LB goal(String type, String color, int count) {
            @SuppressWarnings("unchecked") List<Object> g = (List<Object>) m.computeIfAbsent("goals", k -> new ArrayList<>());
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("type", type);
            if (color != null) e.put("color", color);
            if (count >= 0) e.put("count", (long) count);
            g.add(e);
            return this;
        }

        Level level() {
            if (!m.containsKey("goals")) goal("COLOR", "PURPLE", 999);
            return Level.parse(Json.write(m));
        }

        Board board() {
            return Board.createUnchecked(level(), 7);
        }
    }

    static List<Object> list(String[] a) {
        return new ArrayList<>(Arrays.asList((Object[]) a));
    }

    static List<Object> rc(int r, int c) {
        List<Object> l = new ArrayList<>();
        l.add((long) r);
        l.add((long) c);
        return l;
    }

    static int idx(int r, int c) {
        return r * 8 + c;
    }

    static Step first(TurnResult res, Step.Type t) {
        for (Step s : res.steps) if (s.type == t) return s;
        return null;
    }

    static int countSteps(TurnResult res, Step.Type t) {
        int k = 0;
        for (Step s : res.steps) if (s.type == t) k++;
        return k;
    }
}
