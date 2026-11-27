package com.brickyard.engine.rules;

import com.brickyard.engine.model.Board;

/**
 * One animation beat of a turn. When the engine records steps, each carries a snapshot of the
 * board after the beat; BoardView animates entities by uid from the previous snapshot to this one.
 */
public final class Step {
    public enum Type {
        SWAP, SWAP_BOUNCE, MATCH_POP, POWERUP_SPAWN, POWERUP_FIRE, COMBO_DETONATE, BUGGY_FLIGHT,
        GRAVITY_DROP, PORTAL_WARP, CONVEYOR_SHIFT, OIL_SPREAD, PART_COLLECT, DEADLOCK_SHUFFLE,
        VICTORY_LAP, TOOL_USE
    }

    public final Type type;
    public final int[] cells;  // cells involved (cleared, spawned, collected...)
    public final int a, b;     // e.g. swap endpoints, buggy from/to
    public final int kind;     // power-up kind for POWERUP_* steps, tool index for TOOL_USE
    public Snapshot after;

    public Step(Type type, int[] cells, int a, int b, int kind) {
        this.type = type;
        this.cells = cells == null ? new int[0] : cells;
        this.a = a;
        this.b = b;
        this.kind = kind;
    }

    /** A frozen copy of the visible board. */
    public static final class Snapshot {
        public final byte[] entity, color, power, layers, clamp, floor;
        public final int[] uid;
        public final int[] remaining;
        public final int movesLeft;

        public Snapshot(Board bd) {
            entity = bd.entity.clone();
            color = bd.color.clone();
            power = bd.power.clone();
            layers = bd.layers.clone();
            clamp = bd.clamp.clone();
            floor = bd.floor.clone();
            uid = bd.uid.clone();
            remaining = new int[bd.level.goals.size()];
            for (int g = 0; g < remaining.length; g++) remaining[g] = Math.max(0, bd.remaining(g));
            movesLeft = bd.movesLeft;
        }
    }
}
