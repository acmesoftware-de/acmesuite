package de.acmesoftware.acmesuite.shared;

/**
 * Deterministic randomness (SplitMix64 mixing) over {@code (seed, key, step, salt)} — no
 * {@code Math.random}, so that results stay reproducible for the same seed. Shared across modules
 * (e.g. movement and daily workload).
 */
public final class Rng {

    private Rng() {
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    /** Uniformly distributed in {@code [0,1)}. */
    public static double unit(long seed, String key, long step, int salt) {
        long h = mix(seed * 0x9E3779B97F4A7C15L + key.hashCode());
        h = mix(h + step * 0xD1B54A32D192ED03L);
        h = mix(h + salt);
        return (h >>> 11) * 0x1.0p-53;
    }

    /** Integer in {@code [0, bound)}. */
    public static int intIn(long seed, String key, long step, int salt, int bound) {
        return (int) (unit(seed, key, step, salt) * bound);
    }
}
