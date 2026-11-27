package com.brickyard.engine.util;

/**
 * SplitMix64. Tiny, fast and fully reproducible: the whole game state is a pure
 * function of (level, seed, moves), which is what lets saves resume exactly and
 * lets the bot and solver replay any game.
 */
public final class Rng {
    private long state;

    public Rng(long seed) {
        this.state = seed;
    }

    public long state() {
        return state;
    }

    public void setState(long s) {
        this.state = s;
    }

    public long nextLong() {
        long z = (state += 0x9E3779B97F4A7C15L);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** Uniform int in [0, bound). */
    public int nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("bound must be positive");
        long r = (nextLong() >>> 1) % bound;
        return (int) r;
    }

    public double nextDouble() {
        return (nextLong() >>> 11) * 0x1.0p-53;
    }

    public static long mix(long a, long b) {
        Rng r = new Rng(a * 31 + b);
        return r.nextLong();
    }
}
