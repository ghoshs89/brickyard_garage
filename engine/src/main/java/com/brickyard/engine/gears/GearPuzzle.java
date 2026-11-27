package com.brickyard.engine.gears;

import com.brickyard.engine.util.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A gear-train mini-game: fixed gears (one motor, one or more outputs), blocked pegs, belts between
 * fixed gears, and a tray of gears to place. Solved when every output turns the required way (and
 * at the required speed, if one is set) with nothing jammed.
 */
public final class GearPuzzle {
    public static final class Fixed {
        public final GearTrainSimulator.Gear gear;
        public final String role;    // "motor", "output", "fixed", or "axle" (an empty output axle: he picks the gear)
        public final int wantDir;    // outputs: CW/CCW, or 0 for either
        public final double wantSpeed; // outputs: required speed, or 0 for any
        public final String label;
        Fixed(GearTrainSimulator.Gear g, String role, int wantDir, double wantSpeed, String label) {
            this.gear = g; this.role = role; this.wantDir = wantDir; this.wantSpeed = wantSpeed; this.label = label;
        }
    }

    public String id;
    public int index;
    public int bay;
    public String title = "";
    public String prompt = "";
    public String boNote = "";
    public String lesson = "";      // the one idea this puzzle teaches, shown after solving
    public int w, h;                // pegboard size in half-units
    public final List<Fixed> fixed = new ArrayList<>();
    public final List<GearTrainSimulator.Belt> belts = new ArrayList<>();
    public final List<int[]> blocked = new ArrayList<>(); // {x0, y0, x1, y1} rectangles in half-units
    public int[] tray = new int[3];                       // counts of 8T, 16T, 24T to place
    public int maxPlace = 3;
    public int rewardCoins = 15;
    public int rewardTool = -1;
    public List<int[]> witness;                           // verifier's recorded solution: {x, y, teeth}

    public int motorIndex() {
        int k = 0;
        for (Fixed f : fixed) {
            if (f.role.equals("axle")) continue;
            if (f.role.equals("motor")) return k;
            k++;
        }
        return -1;
    }

    public boolean isAxle(Fixed f) {
        return f.role.equals("axle");
    }

    /** Can a gear of this size sit here (inside the board, off blocked areas, overlapping nothing)? */
    public boolean canPlace(List<GearTrainSimulator.Gear> placed, GearTrainSimulator.Gear g) {
        int r = g.radius();
        if (g.x - r < 0 || g.y - r < 0 || g.x + r > w || g.y + r > h) return false;
        for (int[] b : blocked)
            if (g.x + r > b[0] && g.x - r < b[2] && g.y + r > b[1] && g.y - r < b[3]) return false;
        for (Fixed f : fixed) {
            if (isAxle(f)) continue;
            if (GearTrainSimulator.relation(f.gear, g) == -1) return false;
        }
        for (GearTrainSimulator.Gear p : placed) if (GearTrainSimulator.relation(p, g) == -1) return false;
        return true;
    }

    public List<GearTrainSimulator.Gear> all(List<GearTrainSimulator.Gear> placed) {
        List<GearTrainSimulator.Gear> g = new ArrayList<>();
        for (Fixed f : fixed) if (!isAxle(f)) g.add(f.gear);
        g.addAll(placed);
        return g;
    }

    public GearTrainSimulator.Result run(List<GearTrainSimulator.Gear> placed) {
        return GearTrainSimulator.simulate(all(placed), belts, motorIndex());
    }

    /** Index in all(placed) of the gear acting as output f, or -1 (an empty axle). */
    public int outputIndex(List<GearTrainSimulator.Gear> placed, Fixed f) {
        int k = 0;
        for (Fixed x : fixed) {
            if (isAxle(x)) continue;
            if (x == f) return k;
            k++;
        }
        for (int j = 0; j < placed.size(); j++)
            if (placed.get(j).x == f.gear.x && placed.get(j).y == f.gear.y) return k + j;
        return -1;
    }

    public boolean solved(List<GearTrainSimulator.Gear> placed) {
        GearTrainSimulator.Result r = run(placed);
        if (r.jammed) return false;
        boolean any = false;
        for (Fixed f : fixed) {
            if (!f.role.equals("output") && !isAxle(f)) continue;
            any = true;
            int i = outputIndex(placed, f);
            if (i < 0 || r.direction[i] == GearTrainSimulator.STILL) return false;
            if (f.wantDir != 0 && r.direction[i] != f.wantDir) return false;
            if (f.wantSpeed > 0 && Math.abs(r.speed[i] - f.wantSpeed) > 1e-6) return false;
        }
        return any;
    }

    // ------------------------------------------------------------------ parsing

    public static GearPuzzle parse(String json) {
        Map<String, Object> m = Json.parseObject(json);
        GearPuzzle p = new GearPuzzle();
        p.id = Json.getString(m, "id", null);
        if (p.id == null) throw new IllegalArgumentException("gear puzzle missing id");
        p.index = Json.getInt(m, "index", 0);
        p.bay = Json.getInt(m, "bay", 1);
        p.title = Json.getString(m, "title", "");
        p.prompt = Json.getString(m, "prompt", "");
        p.boNote = Json.getString(m, "boNote", "");
        p.lesson = Json.getString(m, "lesson", "");
        p.w = Json.getInt(m, "w", 16);
        p.h = Json.getInt(m, "h", 10);
        p.maxPlace = Json.getInt(m, "maxPlace", 3);
        p.rewardCoins = Json.getInt(m, "rewardCoins", 15);
        String tool = Json.getString(m, "rewardTool", "");
        p.rewardTool = tool.equals("WRENCH") ? 0 : tool.equals("CLAW") ? 1 : tool.equals("BROOM") ? 2 : -1;
        for (Object o : Json.getList(m, "fixed")) {
            @SuppressWarnings("unchecked") Map<String, Object> f = (Map<String, Object>) o;
            String role = Json.getString(f, "role", "fixed");
            // An axle has no gear of its own; 8T stands in for its position only.
            GearTrainSimulator.Gear g = new GearTrainSimulator.Gear(Json.getInt(f, "x", 0), Json.getInt(f, "y", 0),
                    role.equals("axle") ? 8 : Json.getInt(f, "teeth", 16));
            String dir = Json.getString(f, "wantDir", "");
            int wd = dir.equals("CW") ? GearTrainSimulator.CW : dir.equals("CCW") ? GearTrainSimulator.CCW : 0;
            p.fixed.add(new Fixed(g, role, wd, Json.getDouble(f, "wantSpeed", 0), Json.getString(f, "label", "")));
        }
        for (Object o : Json.getList(m, "belts")) {
            @SuppressWarnings("unchecked") Map<String, Object> b = (Map<String, Object>) o;
            p.belts.add(new GearTrainSimulator.Belt(Json.getInt(b, "a", 0), Json.getInt(b, "b", 0), Json.getBool(b, "crossed", false)));
        }
        for (Object o : Json.getList(m, "blocked")) {
            @SuppressWarnings("unchecked") List<Object> r = (List<Object>) o;
            p.blocked.add(new int[]{((Number) r.get(0)).intValue(), ((Number) r.get(1)).intValue(), ((Number) r.get(2)).intValue(), ((Number) r.get(3)).intValue()});
        }
        List<Object> tray = Json.getList(m, "tray");
        for (int k = 0; k < 3 && k < tray.size(); k++) p.tray[k] = ((Number) tray.get(k)).intValue();
        if (m.containsKey("witness")) {
            p.witness = new ArrayList<>();
            for (Object o : Json.getList(m, "witness")) {
                @SuppressWarnings("unchecked") List<Object> g = (List<Object>) o;
                p.witness.add(new int[]{((Number) g.get(0)).intValue(), ((Number) g.get(1)).intValue(), ((Number) g.get(2)).intValue()});
            }
        }
        if (p.motorIndex() < 0) throw new IllegalArgumentException(p.id + ": needs a motor");
        return p;
    }
}
