package com.dalkco.foodplanner;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/** Short warnings above the hotbar, each only when the situation starts (no spam). */
public final class Warnings {
    private static int lastFood = -1;
    private static boolean lowWarned;
    private static boolean noneWarned;
    private static boolean regenWarned;

    private Warnings() {
    }

    public static void reset() {
        lastFood = -1;
        lowWarned = false;
        noneWarned = false;
        regenWarned = false;
    }

    public static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        FoodConfig.Data c = FoodConfig.get();
        if (p == null || p.isCreative() || p.isSpectator()) {
            return;
        }
        int food = p.getFoodData().getFoodLevel();
        if (!c.warnings) {
            lastFood = food;
            return;
        }
        // sprinting stops at 6
        if (lastFood > 8 && food <= 8) {
            warn(mc, Component.translatable("foodplanner.warn.sprint", food), ChatFormatting.GOLD);
        }
        // natural regeneration needs 18
        boolean regenStopped = p.isHurt() && food < 18;
        if (regenStopped && !regenWarned && lastFood >= 18) {
            regenWarned = true;
            warn(mc, Component.translatable("foodplanner.warn.regen"), ChatFormatting.GOLD);
        } else if (!regenStopped) {
            regenWarned = false;
        }
        // food runs out soon at this pace
        float minutes = Snapshot.minutes;
        if (Pace.measured() && minutes < c.lowMinutes && Snapshot.carried > 0) {
            if (!lowWarned) {
                lowWarned = true;
                warn(mc, Component.translatable("foodplanner.warn.low", Math.max(1, Math.round(minutes))),
                        ChatFormatting.RED);
            }
        } else if (minutes > c.lowMinutes + 2) {
            lowWarned = false;
        }
        // nothing to eat at all
        if (Snapshot.carried <= 0 && food <= 12) {
            if (!noneWarned) {
                noneWarned = true;
                warn(mc, Component.translatable("foodplanner.warn.none"), ChatFormatting.RED);
            }
        } else if (Snapshot.carried > 0) {
            noneWarned = false;
        }
        lastFood = food;
    }

    private static void warn(Minecraft mc, Component text, ChatFormatting color) {
        if (mc.player == null) {
            return;
        }
        mc.player.sendOverlayMessage(Component.literal("⚠ ").append(text).withStyle(color));
        if (FoodConfig.get().warnSound) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.value(), 0.7f, 0.8f));
        }
    }
}
