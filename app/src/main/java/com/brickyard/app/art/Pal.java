package com.brickyard.app.art;

import android.graphics.Color;

/** One palette for the whole game: warm vintage-enamel garage colours plus bright brick colours. */
public final class Pal {
    private Pal() {}

    public static final int INK = 0xFF2B2118;        // outlines and text
    public static final int CREAM = 0xFFFBF3E0;
    public static final int PAPER = 0xFFF3E6C8;
    public static final int WALL = 0xFFE8C89A;
    public static final int WALL_DARK = 0xFFC79E6B;
    public static final int FLOOR = 0xFF8C8A84;
    public static final int FLOOR_DARK = 0xFF6B6963;
    public static final int STEEL = 0xFF9AA6B2;
    public static final int STEEL_DARK = 0xFF5D6B78;
    public static final int RUST = 0xFFB5552B;
    public static final int RUST_DARK = 0xFF7D3417;
    public static final int WOOD = 0xFFB07A45;
    public static final int WOOD_DARK = 0xFF7A4E26;
    public static final int OIL = 0xFF1E1B24;
    public static final int GOLD = 0xFFF2B630;
    public static final int GOLD_DARK = 0xFFB27C10;
    public static final int SKY = 0xFF9FD3E8;
    public static final int NIGHT = 0xFF1D2547;
    public static final int GREEN_OK = 0xFF3FA34D;
    public static final int RED_BAD = 0xFFD9483B;
    public static final int TEAL = 0xFF2A9D8F;
    public static final int PANEL = 0xF2FFF8EA;
    public static final int SHADOW = 0x40000000;

    /** Piece colours, index = Kinds colour. Saturated, distinct in value as well as hue. */
    public static final int[] PIECE = {0xFFE03B2E, 0xFF2F6FD6, 0xFFF5C518, 0xFF34A853, 0xFFFF8A1F, 0xFF8E44C7};
    public static final int[] PIECE_DARK = {0xFF9C2119, 0xFF1C4593, 0xFFB38B00, 0xFF1F6E35, 0xFFB85A00, 0xFF5B2585};

    /** Brick colour packs for cars, by part name keyword. */
    public static int carColor(String name) {
        if (name == null) return 0xFFE03B2E;
        String n = name.toLowerCase();
        if (n.contains("red")) return 0xFFE03B2E;
        if (n.contains("ocean") || n.contains("blue")) return 0xFF2F6FD6;
        if (n.contains("gold")) return 0xFFF2B630;
        if (n.contains("orange")) return 0xFFFF7A1A;
        if (n.contains("forest") || n.contains("green")) return 0xFF2E8B57;
        if (n.contains("sand")) return 0xFFD8B26E;
        if (n.contains("silver")) return 0xFFB8C2CC;
        if (n.contains("purple")) return 0xFF5B2A86;
        if (n.contains("lime")) return 0xFF9BE22E;
        return 0xFFE03B2E;
    }

    public static int darker(int c, float f) {
        return Color.argb(Color.alpha(c), (int) (Color.red(c) * f), (int) (Color.green(c) * f), (int) (Color.blue(c) * f));
    }

    public static int lighter(int c, float f) {
        return Color.argb(Color.alpha(c), (int) (Color.red(c) + (255 - Color.red(c)) * f),
                (int) (Color.green(c) + (255 - Color.green(c)) * f), (int) (Color.blue(c) + (255 - Color.blue(c)) * f));
    }

    public static int alpha(int c, int a) {
        return (c & 0x00FFFFFF) | (a << 24);
    }
}
