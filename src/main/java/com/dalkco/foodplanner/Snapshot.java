package com.dalkco.foodplanner;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.food.FoodData;

import java.util.List;

/** Food state computed once per tick and read by the HUD, the inventory panel and the warnings. */
public final class Snapshot {
    public static List<Foods.Food> foods = List.of();
    public static Foods.Food best;
    /** food + saturation points carried in normal food */
    public static float carried;
    /** points in the body right now (food level + saturation) */
    public static float body;
    /** minutes everything lasts at the current pace */
    public static float minutes = Float.POSITIVE_INFINITY;

    private Snapshot() {
    }

    public static void update(LocalPlayer player) {
        foods = Foods.carried(player);
        best = Foods.best(player, foods);
        carried = Foods.fuel(foods);
        FoodData d = player.getFoodData();
        body = d.getFoodLevel() + d.getSaturationLevel();
        minutes = Pace.minutes(carried + body);
    }
}
