package com.dalkco.foodplanner;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.food.FoodData;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The player's real hunger pace: how many food + saturation points disappear per minute, measured over the
 * last minutes of play (eating is ignored).
 */
public final class Pace {
    private static final long WINDOW = 10 * 60_000L;
    /** used until there is enough data: a calm mix of walking, mining and building */
    public static final float DEFAULT_RATE = 1.0f;

    private record Drop(long time, float amount) {
    }

    private static final Deque<Drop> drops = new ArrayDeque<>();
    private static float last = -1;
    private static long since;

    private Pace() {
    }

    public static void reset() {
        drops.clear();
        last = -1;
        since = 0;
    }

    public static void tick(LocalPlayer player) {
        FoodData data = player.getFoodData();
        float now = data.getFoodLevel() + data.getSaturationLevel();
        long t = System.currentTimeMillis();
        if (last < 0) {
            last = now;
            since = t;
            return;
        }
        if (now < last) {
            drops.addLast(new Drop(t, last - now));
        }
        last = now;
        while (!drops.isEmpty() && t - drops.peekFirst().time() > WINDOW) {
            drops.removeFirst();
        }
    }

    /** True once a couple of minutes have been watched. */
    public static boolean measured() {
        return since > 0 && System.currentTimeMillis() - since > 120_000L;
    }

    /** Points per minute. */
    public static float rate() {
        if (!measured()) {
            return DEFAULT_RATE;
        }
        long t = System.currentTimeMillis();
        float sum = 0;
        for (Drop d : drops) {
            sum += d.amount();
        }
        double minutes = Math.min(WINDOW, t - since) / 60_000.0;
        return (float) (sum / Math.max(1.0, minutes));
    }

    /** Minutes the given points last at the current pace (Float.POSITIVE_INFINITY when you barely spend any). */
    public static float minutes(float points) {
        float r = rate();
        return r < 0.02f ? Float.POSITIVE_INFINITY : points / r;
    }
}
