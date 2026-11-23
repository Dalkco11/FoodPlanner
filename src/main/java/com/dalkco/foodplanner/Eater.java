package com.dalkco.foodplanner;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;

/**
 * "Eat the best food": picks it (also from the main inventory), holds "use" until it is eaten and puts everything
 * back the way it was. Also the optional auto-eat.
 */
public final class Eater {
    private static boolean eating;
    private static boolean began;
    private static int previousSlot = -1;
    private static int hotbarSlot = -1;
    private static int swappedFrom = -1;
    private static long startedAt;
    private static long lastAuto;

    private Eater() {
    }

    public static boolean eating() {
        return eating;
    }

    public static void eatBest(Minecraft mc, boolean manual) {
        LocalPlayer p = mc.player;
        if (p == null || mc.gameMode == null || eating || mc.gui.screen() != null || p.isUsingItem()) {
            return;
        }
        Foods.Food best = Foods.best(p, Foods.carried(p));
        if (best == null) {
            if (manual) {
                p.sendOverlayMessage(Component.translatable(p.getFoodData().getFoodLevel() >= 20
                        ? "foodplanner.eat.full" : "foodplanner.eat.nothing").withStyle(ChatFormatting.YELLOW));
            }
            return;
        }
        Inventory inv = p.getInventory();
        previousSlot = inv.getSelectedSlot();
        if (best.slot() < 9) {
            hotbarSlot = best.slot();
            swappedFrom = -1;
            inv.setSelectedSlot(hotbarSlot);
        } else {
            // bring it from the main inventory into the selected hotbar slot (inventory menu slot index = inventory index)
            hotbarSlot = previousSlot;
            swappedFrom = best.slot();
            mc.gameMode.handleContainerInput(p.inventoryMenu.containerId, swappedFrom, hotbarSlot, ContainerInput.SWAP, p);
        }
        eating = true;
        began = false;
        startedAt = System.currentTimeMillis();
    }

    public static void tick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null) {
            eating = false;
            return;
        }
        if (eating) {
            long now = System.currentTimeMillis();
            if (mc.gui.screen() != null || now - startedAt > 5000) {
                finish(mc);
                return;
            }
            mc.options.keyUse.setDown(true);
            if (p.isUsingItem()) {
                began = true;
            } else if (began || now - startedAt > 1500) {
                finish(mc);
            }
            return;
        }
        FoodConfig.Data c = FoodConfig.get();
        if (c.autoEat && !p.isCreative() && !p.isSpectator() && p.getFoodData().getFoodLevel() <= c.autoEatAt
                && mc.gui.screen() == null && !p.isUsingItem() && !mc.options.keyAttack.isDown()
                && System.currentTimeMillis() - lastAuto > 3000) {
            lastAuto = System.currentTimeMillis();
            eatBest(mc, false);
        }
    }

    private static void finish(Minecraft mc) {
        LocalPlayer p = mc.player;
        eating = false;
        mc.options.keyUse.setDown(false);
        if (p == null || mc.gameMode == null) {
            return;
        }
        if (swappedFrom >= 0) {
            // put the hotbar item back and the rest of the food where it came from
            mc.gameMode.handleContainerInput(p.inventoryMenu.containerId, swappedFrom, hotbarSlot, ContainerInput.SWAP, p);
        } else if (previousSlot >= 0) {
            p.getInventory().setSelectedSlot(previousSlot);
        }
        swappedFrom = -1;
        previousSlot = -1;
    }
}
