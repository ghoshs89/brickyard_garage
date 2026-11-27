package com.brickyard.engine.rules;

/** A player action. Encoded as int[]{type, a, b, tool} for solver witnesses and saves. */
public final class Action {
    public static final int SWAP = 0;
    public static final int TAP = 1;
    public static final int TOOL = 2;

    public final int type, a, b, tool;

    private Action(int type, int a, int b, int tool) {
        this.type = type;
        this.a = a;
        this.b = b;
        this.tool = tool;
    }

    /** Swap: the piece at {@code from} moves into {@code to} (the swipe destination). */
    public static Action swap(int from, int to) {
        return new Action(SWAP, from, to, -1);
    }

    public static Action tap(int cell) {
        return new Action(TAP, cell, -1, -1);
    }

    /** Wrench and Broom use only {@code a}; the Claw swaps {@code a} and {@code b}. */
    public static Action tool(int tool, int a, int b) {
        return new Action(TOOL, a, b, tool);
    }

    public int[] encode() {
        return new int[]{type, a, b, tool};
    }

    public static Action decode(int[] e) {
        return new Action(e[0], e[1], e.length > 2 ? e[2] : -1, e.length > 3 ? e[3] : -1);
    }

    @Override
    public String toString() {
        switch (type) {
            case SWAP: return "swap(" + a + "->" + b + ")";
            case TAP: return "tap(" + a + ")";
            default: return "tool" + tool + "(" + a + "," + b + ")";
        }
    }
}
