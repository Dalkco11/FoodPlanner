package com.dalkco.foodplanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Mth;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Settings, saved to config/foodplanner.json. */
public final class FoodConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("foodplanner.json");

    /** Trip planner activities with a typical pace (food + saturation points per minute). */
    public enum Activity {
        MY_PACE(0), EXPLORE(4.0f), FIGHT(5.0f), MINE(1.5f), BUILD(1.0f);

        public final float rate;

        Activity(float rate) {
            this.rate = rate;
        }

        public float pace() {
            return this == MY_PACE ? Pace.rate() : rate;
        }
    }

    public static final int[] DURATIONS = {5, 15, 30, 60, 120, 240};

    public static final class Data {
        public boolean hud = true;
        public int hudOffsetY = 0;
        public boolean hotbarMark = true;
        public boolean inventoryMarks = true;
        public boolean panel = true;
        public boolean warnings = true;
        public boolean warnSound = true;
        public int lowMinutes = 5;
        public boolean autoEat = false;
        public int autoEatAt = 14;
        public Activity activity = Activity.EXPLORE;
        public int duration = 60;
    }

    private static Data data = new Data();

    private FoodConfig() {
    }

    public static Data get() {
        return data;
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                Data d = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), Data.class);
                if (d != null) {
                    data = d;
                }
            }
        } catch (Exception e) {
            FoodPlannerClient.LOGGER.warn("Failed to read {}", FILE, e);
        }
        normalize();
    }

    private static void normalize() {
        if (data.activity == null) {
            data.activity = Activity.EXPLORE;
        }
        data.hudOffsetY = Mth.clamp(data.hudOffsetY, -60, 60);
        data.lowMinutes = Mth.clamp(data.lowMinutes, 1, 30);
        data.autoEatAt = Mth.clamp(data.autoEatAt, 6, 19);
        boolean ok = false;
        for (int d : DURATIONS) {
            ok |= d == data.duration;
        }
        if (!ok) {
            data.duration = 60;
        }
    }

    public static void save() {
        normalize();
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(data), StandardCharsets.UTF_8);
        } catch (Exception e) {
            FoodPlannerClient.LOGGER.warn("Failed to write {}", FILE, e);
        }
    }
}
