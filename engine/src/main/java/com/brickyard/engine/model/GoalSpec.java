package com.brickyard.engine.model;

/** A level goal as authored. count == ALL means "clear every one on the board". */
public final class GoalSpec {
    public enum Type { COLOR, CRATE, OIL, RUST, TRACK, PART, CLAMP }

    public static final int ALL = -1;

    public final Type type;
    public final int color;  // COLOR goals only
    public final int count;  // or ALL

    public GoalSpec(Type type, int color, int count) {
        this.type = type;
        this.color = color;
        this.count = count;
    }

    public String label() {
        switch (type) {
            case COLOR: return Kinds.COLOR_NAMES[color] + "s";
            case CRATE: return "Stud Crates";
            case OIL: return "Oil Spills";
            case RUST: return "Rust Plates";
            case TRACK: return "Track Baseplates";
            case PART: return "Rare Car Parts";
            case CLAMP: return "Wheel Clamps";
            default: return type.name();
        }
    }
}
