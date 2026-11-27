package com.brickyard.engine.profile;

import com.brickyard.engine.util.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** content/campaign.json: the order of story levels and what unlocks Blueprints, gear puzzles and Track Day. */
public final class Campaign {
    public static final class Unlockable {
        public String id;
        public String unlockAfter;   // a story level id (Blueprints) or a repair id (gear puzzles)
        public int rewardCoins;
        public int rewardTool = -1;  // Kinds.TOOL_*
        public String rewardPart;    // optional brick part granted on first solve
    }

    public final List<String> story = new ArrayList<>();
    public final List<Unlockable> blueprints = new ArrayList<>();
    public final List<Unlockable> gears = new ArrayList<>();
    public String trackDayUnlock = "";
    public int levelsPerBay = 15;

    public static Campaign parse(String json) {
        Map<String, Object> m = Json.parseObject(json);
        Campaign c = new Campaign();
        for (Object o : Json.getList(m, "story")) c.story.add((String) o);
        c.levelsPerBay = Json.getInt(m, "levelsPerBay", 15);
        c.trackDayUnlock = Json.getString(m, "trackDayUnlock", "");
        for (Object o : Json.getList(m, "blueprints")) c.blueprints.add(unl(o, "unlockAfter"));
        for (Object o : Json.getList(m, "gears")) c.gears.add(unl(o, "unlockRepair"));
        return c;
    }

    private static Unlockable unl(Object o, String key) {
        @SuppressWarnings("unchecked") Map<String, Object> m = (Map<String, Object>) o;
        Unlockable u = new Unlockable();
        u.id = Json.getString(m, "id", null);
        u.unlockAfter = Json.getString(m, key, "");
        u.rewardCoins = Json.getInt(m, "rewardCoins", 0);
        String t = Json.getString(m, "rewardTool", "");
        u.rewardTool = t.equals("WRENCH") ? 0 : t.equals("CLAW") ? 1 : t.equals("BROOM") ? 2 : -1;
        u.rewardPart = Json.getString(m, "rewardPart", null);
        return u;
    }

    public int storyIndex(String id) {
        return story.indexOf(id);
    }

    /** 1-based bay a story level belongs to. */
    public int bayOf(String id) {
        int i = storyIndex(id);
        return i < 0 ? 0 : i / levelsPerBay + 1;
    }

    public Unlockable blueprint(String id) {
        for (Unlockable u : blueprints) if (u.id.equals(id)) return u;
        return null;
    }

    public Unlockable gear(String id) {
        for (Unlockable u : gears) if (u.id.equals(id)) return u;
        return null;
    }
}
