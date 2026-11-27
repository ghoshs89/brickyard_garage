package com.brickyard.engine.profile;

import com.brickyard.engine.util.Json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Everything that is saved: stars, coins, wins, repairs, décor, brick parts, tools, medals, settings,
 * and the in-progress board. Serialised as one JSON file written atomically by the app.
 */
public final class Profile {
    public static final int VERSION = 1;

    public int stars;              // unspent
    public int starsEarned;        // lifetime, equals the number of distinct story levels won
    public int coins;
    public final Set<String> won = new LinkedHashSet<>();            // story levels, blueprints, gear puzzles
    public final Map<String, Double> best = new LinkedHashMap<>();   // best goal progress 0..1 per level
    public final Map<String, Integer> attempts = new LinkedHashMap<>();
    public final Set<String> repairs = new LinkedHashSet<>();
    public final Set<String> ownedDesigns = new LinkedHashSet<>();
    public final Map<String, String> fixtureChoice = new LinkedHashMap<>(); // fixture -> design
    public final Set<String> ownedParts = new LinkedHashSet<>();
    public final Map<String, Map<String, String>> equipped = new LinkedHashMap<>(); // vehicle -> slot -> part
    public final Map<String, Integer> medals = new LinkedHashMap<>(); // "vehicle|track" -> best medal
    public final Map<String, Double> bestTimes = new LinkedHashMap<>();
    public int[] tools = new int[3];
    public final Set<String> seen = new LinkedHashSet<>(); // tutorials and story scenes already shown
    public int trackDayRound;      // endless mode: next generated round
    public int trackDayBest;
    public final Map<String, Integer> carNumbers = new LinkedHashMap<>();  // vehicle -> 0..99 racing number
    public final Map<String, Integer> carStripes = new LinkedHashMap<>();  // vehicle -> 0 none, 1 racing, 2 checkered, 3 lightning
    public final Map<String, Map<String, String>> ghostBuilds = new LinkedHashMap<>(); // "vehicle|track" -> previous equipped map
    public boolean mixParts;       // Cross-Fit Brick Mix: allow snapping owned parts across cars

    // settings
    public boolean sound = true, music = true;
    public String playerName = "Player";
    public String partnerName = "Grandma";
    public String pin = "";        // parent PIN, set on first visit to Parent Settings
    public String closingTime = "OFF"; // OFF, MIN20, MIN30, LEVELS5
    public long closedUntilDay = -1;   // "Shop Closing Time": closed until this local day number
    public long sessionDay = -1;       // persisted daily session tracker for Shop Closing Time
    public long sessionMillis;
    public int sessionLevels;
    public boolean debugUnlock;        // parent testing: every level and puzzle open

    // in-progress board (saved after every settled move)
    public Map<String, Object> inProgress; // {level, together, turnPlayer, board}

    public Map<String, Object> toJson() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("version", (long) VERSION);
        m.put("stars", (long) stars);
        m.put("starsEarned", (long) starsEarned);
        m.put("coins", (long) coins);
        m.put("won", new ArrayList<Object>(won));
        m.put("best", new LinkedHashMap<String, Object>(best));
        m.put("attempts", new LinkedHashMap<String, Object>(attempts));
        m.put("repairs", new ArrayList<Object>(repairs));
        m.put("ownedDesigns", new ArrayList<Object>(ownedDesigns));
        m.put("fixtureChoice", new LinkedHashMap<String, Object>(fixtureChoice));
        m.put("ownedParts", new ArrayList<Object>(ownedParts));
        Map<String, Object> eq = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, String>> e : equipped.entrySet()) eq.put(e.getKey(), new LinkedHashMap<String, Object>(e.getValue()));
        m.put("equipped", eq);
        m.put("medals", new LinkedHashMap<String, Object>(medals));
        m.put("bestTimes", new LinkedHashMap<String, Object>(bestTimes));
        List<Object> t = new ArrayList<>();
        for (int x : tools) t.add((long) x);
        m.put("tools", t);
        m.put("seen", new ArrayList<Object>(seen));
        m.put("trackDayRound", (long) trackDayRound);
        m.put("trackDayBest", (long) trackDayBest);
        m.put("carNumbers", new LinkedHashMap<String, Object>(carNumbers));
        m.put("carStripes", new LinkedHashMap<String, Object>(carStripes));
        Map<String, Object> gb = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, String>> e : ghostBuilds.entrySet()) gb.put(e.getKey(), new LinkedHashMap<String, Object>(e.getValue()));
        m.put("ghostBuilds", gb);
        m.put("mixParts", mixParts);
        m.put("sound", sound);
        m.put("music", music);
        m.put("playerName", playerName);
        m.put("partnerName", partnerName);
        m.put("pin", pin);
        m.put("closingTime", closingTime);
        m.put("closedUntilDay", closedUntilDay);
        m.put("sessionDay", sessionDay);
        m.put("sessionMillis", sessionMillis);
        m.put("sessionLevels", (long) sessionLevels);
        m.put("debugUnlock", debugUnlock);
        m.put("inProgress", inProgress);
        return m;
    }

    @SuppressWarnings("unchecked")
    public static Profile fromJson(Map<String, Object> m) {
        Profile p = new Profile();
        p.stars = Json.getInt(m, "stars", 0);
        p.starsEarned = Json.getInt(m, "starsEarned", 0);
        p.coins = Json.getInt(m, "coins", 0);
        for (Object o : Json.getList(m, "won")) p.won.add((String) o);
        for (Map.Entry<String, Object> e : Json.getMap(m, "best").entrySet()) p.best.put(e.getKey(), ((Number) e.getValue()).doubleValue());
        for (Map.Entry<String, Object> e : Json.getMap(m, "attempts").entrySet()) p.attempts.put(e.getKey(), ((Number) e.getValue()).intValue());
        for (Object o : Json.getList(m, "repairs")) p.repairs.add((String) o);
        for (Object o : Json.getList(m, "ownedDesigns")) p.ownedDesigns.add((String) o);
        for (Map.Entry<String, Object> e : Json.getMap(m, "fixtureChoice").entrySet()) p.fixtureChoice.put(e.getKey(), (String) e.getValue());
        for (Object o : Json.getList(m, "ownedParts")) p.ownedParts.add((String) o);
        for (Map.Entry<String, Object> e : Json.getMap(m, "equipped").entrySet()) {
            Map<String, String> slots = new LinkedHashMap<>();
            for (Map.Entry<String, Object> s : ((Map<String, Object>) e.getValue()).entrySet()) slots.put(s.getKey(), (String) s.getValue());
            p.equipped.put(e.getKey(), slots);
        }
        for (Map.Entry<String, Object> e : Json.getMap(m, "medals").entrySet()) p.medals.put(e.getKey(), ((Number) e.getValue()).intValue());
        for (Map.Entry<String, Object> e : Json.getMap(m, "bestTimes").entrySet()) p.bestTimes.put(e.getKey(), ((Number) e.getValue()).doubleValue());
        List<Object> t = Json.getList(m, "tools");
        for (int k = 0; k < 3 && k < t.size(); k++) p.tools[k] = ((Number) t.get(k)).intValue();
        for (Object o : Json.getList(m, "seen")) p.seen.add((String) o);
        p.trackDayRound = Json.getInt(m, "trackDayRound", 0);
        p.trackDayBest = Json.getInt(m, "trackDayBest", 0);
        for (Map.Entry<String, Object> e : Json.getMap(m, "carNumbers").entrySet()) p.carNumbers.put(e.getKey(), ((Number) e.getValue()).intValue());
        for (Map.Entry<String, Object> e : Json.getMap(m, "carStripes").entrySet()) p.carStripes.put(e.getKey(), ((Number) e.getValue()).intValue());
        for (Map.Entry<String, Object> e : Json.getMap(m, "ghostBuilds").entrySet()) {
            Map<String, String> slots = new LinkedHashMap<>();
            for (Map.Entry<String, Object> s : ((Map<String, Object>) e.getValue()).entrySet()) slots.put(s.getKey(), (String) s.getValue());
            p.ghostBuilds.put(e.getKey(), slots);
        }
        p.mixParts = Json.getBool(m, "mixParts", false);
        p.sound = Json.getBool(m, "sound", true);
        p.music = Json.getBool(m, "music", true);
        p.playerName = Json.getString(m, "playerName", "Player");
        p.partnerName = Json.getString(m, "partnerName", "Grandma");
        p.pin = Json.getString(m, "pin", "");
        p.closingTime = Json.getString(m, "closingTime", "OFF");
        p.closedUntilDay = Json.getLong(m, "closedUntilDay", -1);
        p.sessionDay = Json.getLong(m, "sessionDay", -1);
        p.sessionMillis = Json.getLong(m, "sessionMillis", 0);
        p.sessionLevels = Json.getInt(m, "sessionLevels", 0);
        p.debugUnlock = Json.getBool(m, "debugUnlock", false);
        Object ip = m.get("inProgress");
        p.inProgress = ip instanceof Map ? (Map<String, Object>) ip : null;
        return p;
    }
}
