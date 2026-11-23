package com.dalkco.foodplanner;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.item.consume_effects.TeleportRandomlyConsumeEffect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** What food the player carries, which is best to eat now, and how much "fuel" it all is. */
public final class Foods {
    /** GOOD: normal food. PRECIOUS: has useful effects (golden apples) — kept for emergencies. BAD: poison, hunger, teleport. */
    public enum Kind { GOOD, PRECIOUS, BAD }

    /** One food stack in the inventory; slot is the Inventory index (0–8 hotbar, 9–35 main, 40 offhand). */
    public record Food(int slot, ItemStack stack, int nutrition, float saturation, float seconds, Kind kind) {
        /** Food + saturation points this one item gives (before waste). */
        public float value() {
            return nutrition + saturation;
        }
    }

    private Foods() {
    }

    public static Food analyze(ItemStack stack, int slot) {
        if (stack.isEmpty()) {
            return null;
        }
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null) {
            return null;
        }
        Consumable consumable = stack.get(DataComponents.CONSUMABLE);
        Kind kind = Kind.GOOD;
        float seconds = 1.6f;
        if (consumable != null) {
            seconds = consumable.consumeSeconds();
            for (ConsumeEffect effect : consumable.onConsumeEffects()) {
                if (effect instanceof TeleportRandomlyConsumeEffect) {
                    kind = Kind.BAD;
                } else if (effect instanceof ApplyStatusEffectsConsumeEffect apply) {
                    for (MobEffectInstance e : apply.effects()) {
                        if (!e.getEffect().value().isBeneficial()) {
                            kind = Kind.BAD;
                        } else if (kind != Kind.BAD) {
                            kind = Kind.PRECIOUS;
                        }
                    }
                }
            }
        }
        return new Food(slot, stack, food.nutrition(), food.saturation(), seconds, kind);
    }

    /** Every food in the hotbar, main inventory and offhand. */
    public static List<Food> carried(LocalPlayer player) {
        List<Food> out = new ArrayList<>();
        Inventory inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            Food f = analyze(inv.getItem(i), i);
            if (f != null) {
                out.add(f);
            }
        }
        Food off = analyze(inv.getItem(Inventory.SLOT_OFFHAND), Inventory.SLOT_OFFHAND);
        if (off != null) {
            out.add(off);
        }
        return out;
    }

    /** Total points (food + saturation) in the normal food the player carries. */
    public static float fuel(List<Food> foods) {
        float sum = 0;
        for (Food f : foods) {
            if (f.kind() == Kind.GOOD) {
                sum += f.value() * f.stack().getCount();
            }
        }
        return sum;
    }

    /** Normal food grouped by item: item → [count, value per item]. Biggest stock first. */
    public static Map<Item, float[]> stock(List<Food> foods) {
        Map<Item, float[]> out = new LinkedHashMap<>();
        for (Food f : foods) {
            if (f.kind() == Kind.GOOD) {
                float[] v = out.computeIfAbsent(f.stack().getItem(), k -> new float[]{0, f.value()});
                v[0] += f.stack().getCount();
            }
        }
        return out;
    }

    /**
     * The food worth eating now: fills what is missing without wasting much, prefers saturation when hurt
     * (regeneration runs on it), never suggests poisonous food or golden apples. Null when full or nothing fits.
     */
    public static Food best(LocalPlayer player, List<Food> foods) {
        FoodData data = player.getFoodData();
        int food = data.getFoodLevel();
        float sat = data.getSaturationLevel();
        int missing = 20 - food;
        if (missing <= 0) {
            return null;
        }
        boolean hurt = player.isHurt();
        Food best = null;
        double bestScore = -1e9;
        for (Food f : foods) {
            if (f.kind() != Kind.GOOD || f.slot() == Inventory.SLOT_OFFHAND) {
                continue;
            }
            int gainFood = Math.min(f.nutrition(), missing);
            int newFood = food + gainFood;
            float newSat = Math.min(sat + f.saturation(), newFood);
            float gainSat = Math.max(0, newSat - sat);
            float waste = (f.nutrition() - gainFood) + Math.max(0, sat + f.saturation() - newSat);
            double score = gainFood + gainSat * (hurt ? 1.2 : 0.7) - waste * 0.8 - Math.max(0, f.seconds() - 1.6) * 0.5;
            // hotbar food is quicker to reach: tiny bonus so ties go to it
            if (f.slot() < 9) {
                score += 0.05;
            }
            if (score > bestScore) {
                bestScore = score;
                best = f;
            }
        }
        return best;
    }

    /** How many of an item cover {@code points}. */
    public static int howMany(float points, float perItem) {
        return perItem <= 0 ? 0 : (int) Math.ceil(points / perItem);
    }
}
