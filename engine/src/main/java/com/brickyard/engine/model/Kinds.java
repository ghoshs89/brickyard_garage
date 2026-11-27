package com.brickyard.engine.model;

/** Small integer codes stored in the board's byte arrays (cheap to copy for the solver and bot). */
public final class Kinds {
    private Kinds() {}

    // ---- entities (what occupies a cell)
    public static final byte EMPTY = 0;
    public static final byte PIECE = 1;   // a coloured piece, possibly carrying a power-up
    public static final byte CRATE = 2;   // Stud Crate, 1..3 layers, immovable
    public static final byte OIL = 3;     // Oil Spill, immovable, spreads if a turn clears none
    public static final byte PART = 4;    // Rare Car Part: falls, swappable, never matches, collected at exits

    // ---- piece colours (distinct silhouette per colour, so shape alone identifies a piece)
    public static final int RED_BRICK = 0;
    public static final int BLUE_GEAR = 1;
    public static final int YELLOW_PLUG = 2;
    public static final int GREEN_TIRE = 3;
    public static final int ORANGE_CONE = 4;
    public static final int PURPLE_WRENCH = 5;
    public static final int NUM_COLORS = 6;
    public static final int NO_COLOR = -1;
    public static final String COLOR_LETTERS = "RBYGOP";
    public static final String[] COLOR_NAMES = {"Red Brick", "Blue Gear", "Yellow Spark Plug", "Green Tire", "Orange Cone", "Purple Wrench"};
    public static final String[] COLOR_KEYS = {"RED", "BLUE", "YELLOW", "GREEN", "ORANGE", "PURPLE"};

    // ---- power-ups
    public static final byte NONE = 0;
    public static final byte NITRO_H = 1;  // clears its row
    public static final byte NITRO_V = 2;  // clears its column
    public static final byte BUGGY = 3;    // RC Brick Buggy: clears + neighbours, then homes to a target
    public static final byte AIRBAG = 4;   // Shockwave Airbag: diamond blast radius 2
    public static final byte SPRAYER = 5;  // Master Color Sprayer: clears one colour (swap only)
    public static final String[] POWER_NAMES = {"", "Nitro Booster", "Nitro Booster", "RC Brick Buggy", "Shockwave Airbag", "Master Color Sprayer"};

    // ---- floor (under the entity)
    public static final byte FLOOR_NONE = 0;
    public static final byte FLOOR_TRACK = 1; // Checkered Track Baseplate (spreads, goal = cover)
    public static final byte FLOOR_RUST = 2;  // Rust Plate (cleared when the tile above is cleared)

    // ---- mechanic tools (earned, cost 0 moves)
    public static final int TOOL_WRENCH = 0; // Torque Wrench: smash one tile / one layer
    public static final int TOOL_CLAW = 1;   // Magnetic Claw: swap any two neighbours, no match needed
    public static final int TOOL_BROOM = 2;  // Shop Broom: clear a row
    public static final String[] TOOL_NAMES = {"Torque Wrench", "Magnetic Claw", "Shop Broom"};

    public static boolean isNitro(byte p) {
        return p == NITRO_H || p == NITRO_V;
    }

    public static int colorFromKey(String k) {
        for (int i = 0; i < COLOR_KEYS.length; i++) if (COLOR_KEYS[i].equalsIgnoreCase(k)) return i;
        throw new IllegalArgumentException("unknown colour " + k);
    }
}
