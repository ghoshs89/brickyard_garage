package com.brickyard.engine.cars;

import com.brickyard.engine.util.Json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Everything in content/garage.json: bays, repairs, fixtures, vehicles, brick parts, tracks. */
public final class GarageCatalog {
    public static final String[] SLOTS = {"CHASSIS", "ENGINE", "WHEELS", "AERO", "UTILITY", "COLOR"};

    public static final class Repair {
        public String id, name, desc, unlocksSlot, story;
        public int stars, bay, order;
    }

    public static final class Design {
        public String id, name;
        public int price;
        public int look; // index into the renderer's variants for this fixture
    }

    public static final class Fixture {
        public String id, name, repair; // available once this repair is done
        public int bay;
        public final List<Design> designs = new ArrayList<>();
    }

    public static final class Bay {
        public String id, name, vehicle, intro, outro;
        public int number;
        public final List<Repair> repairs = new ArrayList<>();
        public final List<Fixture> fixtures = new ArrayList<>();
    }

    public static final class Part {
        public String id, name, slot, vehicle, source, blurb;
        public int price, speed, grip, boost;
        public int look;
        public boolean starter; // installed free when its slot unlocks
    }

    public static final class Vehicle {
        public String id, name, nickname, bay;
        public int speed, grip, boost;
    }

    public static final class Segment {
        public String type; // ramp, straight, mud, hill
        public double length;
    }

    public static final class Track {
        public String id, name, vehicle;
        public final List<Segment> segments = new ArrayList<>();
        public double gold, silver, bronze; // seconds
    }

    public final List<Bay> bays = new ArrayList<>();
    public final Map<String, Repair> repairs = new LinkedHashMap<>();
    public final Map<String, Fixture> fixtures = new LinkedHashMap<>();
    public final Map<String, Vehicle> vehicles = new LinkedHashMap<>();
    public final Map<String, Part> parts = new LinkedHashMap<>();
    public final Map<String, Track> tracks = new LinkedHashMap<>();

    public static GarageCatalog parse(String json) {
        Map<String, Object> m = Json.parseObject(json);
        GarageCatalog g = new GarageCatalog();
        int bayNo = 0;
        for (Object o : Json.getList(m, "bays")) {
            Map<String, Object> bm = map(o);
            Bay b = new Bay();
            b.id = req(bm, "id");
            b.name = Json.getString(bm, "name", b.id);
            b.vehicle = Json.getString(bm, "vehicle", "");
            b.intro = Json.getString(bm, "intro", "");
            b.outro = Json.getString(bm, "outro", "");
            b.number = ++bayNo;
            int order = 0;
            for (Object ro : Json.getList(bm, "repairs")) {
                Map<String, Object> rm = map(ro);
                Repair r = new Repair();
                r.id = req(rm, "id");
                r.name = Json.getString(rm, "name", r.id);
                r.desc = Json.getString(rm, "desc", "");
                r.story = Json.getString(rm, "story", "");
                r.unlocksSlot = Json.getString(rm, "unlocksSlot", "");
                r.stars = Json.getInt(rm, "stars", 1);
                r.bay = b.number;
                r.order = order++;
                b.repairs.add(r);
                put(g.repairs, r.id, r);
            }
            for (Object fo : Json.getList(bm, "fixtures")) {
                Map<String, Object> fm = map(fo);
                Fixture f = new Fixture();
                f.id = req(fm, "id");
                f.name = Json.getString(fm, "name", f.id);
                f.repair = Json.getString(fm, "repair", "");
                f.bay = b.number;
                int look = 0;
                for (Object dobj : Json.getList(fm, "designs")) {
                    Map<String, Object> dm = map(dobj);
                    Design d = new Design();
                    d.id = req(dm, "id");
                    d.name = Json.getString(dm, "name", d.id);
                    d.price = Json.getInt(dm, "price", 0);
                    d.look = look++;
                    f.designs.add(d);
                }
                b.fixtures.add(f);
                put(g.fixtures, f.id, f);
            }
            g.bays.add(b);
        }
        for (Object o : Json.getList(m, "vehicles")) {
            Map<String, Object> vm = map(o);
            Vehicle v = new Vehicle();
            v.id = req(vm, "id");
            v.name = Json.getString(vm, "name", v.id);
            v.nickname = Json.getString(vm, "nickname", "");
            v.bay = Json.getString(vm, "bay", "");
            v.speed = Json.getInt(vm, "speed", 3);
            v.grip = Json.getInt(vm, "grip", 3);
            v.boost = Json.getInt(vm, "boost", 3);
            put(g.vehicles, v.id, v);
        }
        Map<String, Integer> looks = new LinkedHashMap<>();
        for (Object o : Json.getList(m, "parts")) {
            Map<String, Object> pm = map(o);
            Part p = new Part();
            p.id = req(pm, "id");
            p.name = Json.getString(pm, "name", p.id);
            p.slot = Json.getString(pm, "slot", "");
            p.vehicle = Json.getString(pm, "vehicle", "any");
            p.source = Json.getString(pm, "source", "shop");
            p.blurb = Json.getString(pm, "blurb", "");
            p.price = Json.getInt(pm, "price", 0);
            p.speed = Json.getInt(pm, "speed", 0);
            p.grip = Json.getInt(pm, "grip", 0);
            p.boost = Json.getInt(pm, "boost", 0);
            p.starter = Json.getBool(pm, "starter", false);
            String key = p.vehicle + "/" + p.slot;
            Integer k = looks.get(key);
            p.look = k == null ? 0 : k;
            looks.put(key, p.look + 1);
            put(g.parts, p.id, p);
        }
        for (Object o : Json.getList(m, "tracks")) {
            Map<String, Object> tm = map(o);
            Track t = new Track();
            t.id = req(tm, "id");
            t.name = Json.getString(tm, "name", t.id);
            t.vehicle = Json.getString(tm, "vehicle", "");
            for (Object so : Json.getList(tm, "segments")) {
                Map<String, Object> sm = map(so);
                Segment s = new Segment();
                s.type = Json.getString(sm, "type", "straight");
                s.length = Json.getDouble(sm, "length", 10);
                t.segments.add(s);
            }
            List<Object> med = Json.getList(tm, "medals");
            t.gold = ((Number) med.get(0)).doubleValue();
            t.silver = ((Number) med.get(1)).doubleValue();
            t.bronze = ((Number) med.get(2)).doubleValue();
            put(g.tracks, t.id, t);
        }
        return g;
    }

    public Bay bay(int number) {
        return bays.get(number - 1);
    }

    public List<Part> partsFor(String vehicle, String slot) {
        List<Part> out = new ArrayList<>();
        for (Part p : parts.values())
            if (p.slot.equals(slot) && (p.vehicle.equals(vehicle) || p.vehicle.equals("any"))) out.add(p);
        return out;
    }

    /** All parts for a slot across every vehicle (used in Cross-Fit Brick Mix mode). */
    public List<Part> allPartsForSlot(String slot) {
        List<Part> out = new ArrayList<>();
        for (Part p : parts.values())
            if (p.slot.equals(slot)) out.add(p);
        return out;
    }

    public Part starter(String vehicle, String slot) {
        for (Part p : partsFor(vehicle, slot)) if (p.starter) return p;
        return null;
    }

    public int totalRepairStars() {
        int s = 0;
        for (Repair r : repairs.values()) s += r.stars;
        return s;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object o) {
        return (Map<String, Object>) o;
    }

    private static String req(Map<String, Object> m, String k) {
        String v = Json.getString(m, k, null);
        if (v == null) throw new IllegalArgumentException("garage.json: missing " + k);
        return v;
    }

    private static <V> void put(Map<String, V> m, String k, V v) {
        if (m.containsKey(k)) throw new IllegalArgumentException("garage.json: duplicate id " + k);
        m.put(k, v);
    }
}
