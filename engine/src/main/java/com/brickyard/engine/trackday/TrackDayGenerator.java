package com.brickyard.engine.trackday;

import com.brickyard.engine.model.Level;
import com.brickyard.engine.util.Rng;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Endless "Track Day" rounds, unlocked after the story so he never hits a dead end.
 * Round n always produces the same board (seeded), with a shape template and one featured
 * mechanic rotating through crates, oil, rust, track baseplates and car parts. Move limits are
 * generous: Track Day is for fun and coins, and the tools check every round is winnable.
 */
public final class TrackDayGenerator {
    private TrackDayGenerator() {}

    private static final String[][] SHAPES = {
            {"........", "........", "........", "........", "........", "........", "........", "........"},
            {"#......#", "........", "........", "........", "........", "........", "........", "#......#"},
            {"........", "........", "........", "...##...", "...##...", "........", "........", "........"},
            {"##....##", "#......#", "........", "........", "........", "........", "#......#", "##....##"},
            {"........", ".#....#.", "........", "........", "........", "........", ".#....#.", "........"},
    };

    public static Level generate(int round) {
        Rng rng = new Rng(9001L + round * 7919L);
        String[] shape = SHAPES[(round / 5) % SHAPES.length];
        char[][] lay = new char[8][];
        char[][] flo = new char[8][];
        for (int r = 0; r < 8; r++) {
            lay[r] = shape[r].toCharArray();
            flo[r] = "........".toCharArray();
        }
        int feature = round % 5;
        int colors = round % 4 == 3 ? 6 : 5;
        List<Object> goals = new ArrayList<>();
        Map<String, Object> m = new LinkedHashMap<>();
        String[] colorKeys = {"RED", "BLUE", "YELLOW", "GREEN", "ORANGE"};
        int moves;
        switch (feature) {
            case 0: { // stud crates
                int placed = 0;
                while (placed < 8) {
                    int r = 2 + rng.nextInt(6), c = rng.nextInt(8);
                    if (lay[r][c] != '.') continue;
                    lay[r][c] = (char) ('1' + rng.nextInt(2));
                    placed++;
                }
                goals.add(goal("CRATE", null, -1));
                goals.add(goal("COLOR", colorKeys[rng.nextInt(5)], 18));
                moves = 26;
                break;
            }
            case 1: { // oil spills
                int placed = 0;
                while (placed < 3) {
                    int r = 3 + rng.nextInt(5), c = rng.nextInt(8);
                    if (lay[r][c] != '.') continue;
                    lay[r][c] = '~';
                    placed++;
                }
                goals.add(goal("OIL", null, -1));
                moves = 24;
                break;
            }
            case 2: { // rust plates
                int r0 = 3 + rng.nextInt(3);
                for (int r = r0; r < r0 + 3; r++) for (int c = 1; c < 7; c++) if (lay[r][c] == '.') flo[r][c] = 'r';
                goals.add(goal("RUST", null, -1));
                goals.add(goal("COLOR", colorKeys[rng.nextInt(5)], 14));
                moves = 26;
                break;
            }
            case 3: { // track baseplates
                int c0 = 2 + rng.nextInt(4);
                flo[7][c0] = 't';
                flo[6][c0] = 't';
                goals.add(goal("TRACK", null, 30));
                moves = 28;
                break;
            }
            default: { // rare car parts
                List<Object> disp = new ArrayList<>(), exits = new ArrayList<>();
                for (int c = 0; c < 8; c++) {
                    if (lay[0][c] == '.') disp.add(rc(0, c));
                    if (lay[7][c] == '.') exits.add(rc(7, c));
                }
                m.put("dispensers", disp);
                m.put("exits", exits);
                m.put("partsTotal", 3L);
                goals.add(goal("PART", null, 3));
                goals.add(goal("COLOR", colorKeys[rng.nextInt(5)], 12));
                moves = 26;
            }
        }
        if (colors == 6) moves += 4;
        List<Object> layout = new ArrayList<>(), floor = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            layout.add(new String(lay[r]));
            floor.add(new String(flo[r]));
        }
        m.put("id", "trackday-" + round);
        m.put("kind", "TRACK_DAY");
        m.put("title", "Track Day, Round " + (round + 1));
        m.put("moves", (long) moves);
        m.put("colors", (long) colors);
        m.put("seed", 424242L + round);
        m.put("baseCoins", 8L);
        m.put("layout", layout);
        m.put("floor", floor);
        m.put("goals", goals);
        return Level.fromMap(m);
    }

    private static Map<String, Object> goal(String type, String color, int count) {
        Map<String, Object> g = new LinkedHashMap<>();
        g.put("type", type);
        if (color != null) g.put("color", color);
        if (count >= 0) g.put("count", (long) count);
        return g;
    }

    private static List<Object> rc(int r, int c) {
        List<Object> l = new ArrayList<>();
        l.add((long) r);
        l.add((long) c);
        return l;
    }
}
