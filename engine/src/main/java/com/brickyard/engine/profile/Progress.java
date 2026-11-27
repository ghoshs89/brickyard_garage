package com.brickyard.engine.profile;

import com.brickyard.engine.cars.GarageCatalog;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The rules of progression and the economy. The app calls these; the tools replay them to prove
 * the invariants (45 stars exactly finish the garage, replays never award a second star, coins
 * along the story path can buy something for every slot).
 */
public final class Progress {
    public static final int[] MEDAL_COINS = {0, 5, 10, 15}; // first time reaching bronze / silver / gold

    public final Profile p;
    public final Campaign campaign;
    public final GarageCatalog catalog;

    public Progress(Profile p, Campaign campaign, GarageCatalog catalog) {
        this.p = p;
        this.campaign = campaign;
        this.catalog = catalog;
    }

    // ------------------------------------------------------------------ unlocks

    public boolean storyUnlocked(String id) {
        int i = campaign.storyIndex(id);
        if (p.debugUnlock && i >= 0) return true;
        return i == 0 || (i > 0 && p.won.contains(campaign.story.get(i - 1)));
    }

    public String nextStoryLevel() {
        for (String id : campaign.story) if (!p.won.contains(id)) return id;
        return null;
    }

    public boolean blueprintUnlocked(String id) {
        Campaign.Unlockable u = campaign.blueprint(id);
        return u != null && (p.debugUnlock || u.unlockAfter.isEmpty() || p.won.contains(u.unlockAfter));
    }

    public boolean gearUnlocked(String id) {
        Campaign.Unlockable u = campaign.gear(id);
        return u != null && (p.debugUnlock || u.unlockAfter.isEmpty() || p.repairs.contains(u.unlockAfter));
    }

    public boolean trackDayUnlocked() {
        return p.debugUnlock || (!campaign.trackDayUnlock.isEmpty() && p.repairs.contains(campaign.trackDayUnlock));
    }

    public boolean storyComplete() {
        for (String id : campaign.story) if (!p.won.contains(id)) return false;
        return true;
    }

    // ------------------------------------------------------------------ attempts & safe failure

    /** Records a finished attempt; returns the previous best (for the "closest yet" ghost marker). */
    public double recordAttempt(String levelId, double progress) {
        Integer a = p.attempts.get(levelId);
        p.attempts.put(levelId, a == null ? 1 : a + 1);
        Double prev = p.best.get(levelId);
        double before = prev == null ? 0 : prev;
        if (progress > before) p.best.put(levelId, progress);
        return before;
    }

    public int attempts(String levelId) {
        Integer a = p.attempts.get(levelId);
        return a == null ? 0 : a;
    }

    /** The hint bulb appears after three attempts on the same board. */
    public boolean hintAvailable(String levelId) {
        return attempts(levelId) >= 3;
    }

    // ------------------------------------------------------------------ rewards

    public static final class Reward {
        public boolean star;
        public int coins;
        public int tool = -1;
        public String part;
        public boolean firstTime;
    }

    /** A story level was won. One star the first time only; replays pay half coins. */
    public Reward winStory(String levelId, int coinsAwarded) {
        Reward r = new Reward();
        if (!p.won.contains(levelId)) {
            p.won.add(levelId);
            p.stars++;
            p.starsEarned++;
            r.star = true;
            r.firstTime = true;
            r.coins = coinsAwarded;
        } else {
            r.coins = coinsAwarded / 2;
        }
        p.coins += r.coins;
        return r;
    }

    public Reward winBlueprint(String id) {
        return firstSolve(id, campaign.blueprint(id));
    }

    public Reward solveGear(String id) {
        return firstSolve(id, campaign.gear(id));
    }

    private Reward firstSolve(String id, Campaign.Unlockable u) {
        Reward r = new Reward();
        if (u == null || p.won.contains(id)) return r;
        p.won.add(id);
        r.firstTime = true;
        r.coins = u.rewardCoins;
        p.coins += r.coins;
        if (u.rewardTool >= 0) { p.tools[u.rewardTool]++; r.tool = u.rewardTool; }
        if (u.rewardPart != null && catalog.parts.containsKey(u.rewardPart)) {
            p.ownedParts.add(u.rewardPart);
            r.part = u.rewardPart;
        }
        return r;
    }

    /** Test Track result: coins the first time each medal tier is reached for this car and track. */
    public int recordTrackRun(String vehicle, String track, int medal, double seconds) {
        String key = vehicle + "|" + track;
        Integer prev = p.medals.get(key);
        int before = prev == null ? 0 : prev;
        int coins = 0;
        for (int m = before + 1; m <= medal; m++) coins += MEDAL_COINS[m];
        if (medal > before) p.medals.put(key, medal);
        Double bt = p.bestTimes.get(key);
        if (bt == null || seconds < bt) p.bestTimes.put(key, seconds);
        p.coins += coins;
        return coins;
    }

    // ------------------------------------------------------------------ repairs

    public boolean repairDone(String id) {
        return p.repairs.contains(id);
    }

    /** Next repair in order within its bay, and the previous bay finished. */
    public boolean repairAvailable(GarageCatalog.Repair r) {
        if (p.repairs.contains(r.id)) return false;
        GarageCatalog.Bay bay = catalog.bay(r.bay);
        if (r.order > 0 && !p.repairs.contains(bay.repairs.get(r.order - 1).id)) return false;
        if (r.bay > 1) for (GarageCatalog.Repair prev : catalog.bay(r.bay - 1).repairs) if (!p.repairs.contains(prev.id)) return false;
        return true;
    }

    public boolean bayComplete(int bay) {
        for (GarageCatalog.Repair r : catalog.bay(bay).repairs) if (!p.repairs.contains(r.id)) return false;
        return true;
    }

    public boolean garageComplete() {
        for (GarageCatalog.Repair r : catalog.repairs.values()) if (!p.repairs.contains(r.id)) return false;
        return true;
    }

    /** Spends stars on a repair. Unlocks the vehicle slots it opens (with free starter parts) and its fixtures. */
    public boolean doRepair(String id) {
        GarageCatalog.Repair r = catalog.repairs.get(id);
        if (r == null || !repairAvailable(r) || p.stars < r.stars) return false;
        p.stars -= r.stars;
        p.repairs.add(id);
        String vehicle = catalog.bay(r.bay).vehicle;
        if (!r.unlocksSlot.isEmpty()) for (String slot : r.unlocksSlot.split(",")) {
            slot = slot.trim();
            GarageCatalog.Part starter = catalog.starter(vehicle, slot);
            if (starter != null) {
                p.ownedParts.add(starter.id);
                equippedFor(vehicle).put(slot, starter.id);
            }
        }
        for (GarageCatalog.Fixture f : catalog.fixtures.values())
            if (f.repair.equals(id) && !f.designs.isEmpty()) {
                GarageCatalog.Design d = f.designs.get(0);
                p.ownedDesigns.add(d.id);
                p.fixtureChoice.put(f.id, d.id);
            }
        return true;
    }

    // ------------------------------------------------------------------ shop

    public boolean slotUnlocked(String vehicle, String slot) {
        return equippedFor(vehicle).containsKey(slot);
    }

    public boolean fixtureUnlocked(GarageCatalog.Fixture f) {
        return f.repair.isEmpty() || p.repairs.contains(f.repair);
    }

    public boolean buyDesign(String fixtureId, String designId) {
        GarageCatalog.Fixture f = catalog.fixtures.get(fixtureId);
        if (f == null || !fixtureUnlocked(f)) return false;
        GarageCatalog.Design d = null;
        for (GarageCatalog.Design x : f.designs) if (x.id.equals(designId)) d = x;
        if (d == null || p.ownedDesigns.contains(d.id) || p.coins < d.price) return false;
        p.coins -= d.price;
        p.ownedDesigns.add(d.id);
        p.fixtureChoice.put(fixtureId, d.id);
        return true;
    }

    /** Switching between owned designs is always free. */
    public boolean chooseDesign(String fixtureId, String designId) {
        if (!p.ownedDesigns.contains(designId)) return false;
        p.fixtureChoice.put(fixtureId, designId);
        return true;
    }

    public boolean buyPart(String partId) {
        GarageCatalog.Part part = catalog.parts.get(partId);
        if (part == null || p.ownedParts.contains(partId) || !part.source.equals("shop") || p.coins < part.price) return false;
        String vehicle = part.vehicle.equals("any") ? null : part.vehicle;
        if (vehicle != null && !slotUnlocked(vehicle, part.slot)) return false;
        p.coins -= part.price;
        p.ownedParts.add(partId);
        return true;
    }

    /** Snapping an owned part onto a vehicle is free. In Cross-Fit Brick Mix mode, parts fit any car. */
    public boolean equip(String vehicle, String partId) {
        GarageCatalog.Part part = catalog.parts.get(partId);
        if (part == null || !p.ownedParts.contains(partId)) return false;
        if (!p.mixParts && !part.vehicle.equals("any") && !part.vehicle.equals(vehicle)) return false;
        if (!slotUnlocked(vehicle, part.slot)) return false;
        equippedFor(vehicle).put(part.slot, partId);
        return true;
    }

    /** Records a Ghost Car build for this vehicle + track so the next run races against it. */
    public void saveGhostBuild(String vehicle, String track) {
        String key = vehicle + "|" + track;
        p.ghostBuilds.put(key, new LinkedHashMap<String, String>(equippedFor(vehicle)));
    }

    public Map<String, String> equippedFor(String vehicle) {
        Map<String, String> m = p.equipped.get(vehicle);
        if (m == null) {
            m = new LinkedHashMap<>();
            p.equipped.put(vehicle, m);
        }
        return m;
    }
}
