package com.dalkco.foodplanner;

import com.dalkco.foodplanner.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** In inventories: frame around the food to eat, red mark on bad food; in the player inventory a "Food" panel. */
public final class InventoryOverlay {
    public static final int W = 136;
    private static final int GRAY = 0xFF9A9AA6;

    private InventoryOverlay() {
    }

    /** Frames and marks on slots of the player's own inventory (in any container screen). */
    public static void marks(AbstractContainerScreen<?> screen, GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || !FoodConfig.get().inventoryMarks || p.isCreative()) {
            return;
        }
        AbstractContainerScreenAccessor a = (AbstractContainerScreenAccessor) screen;
        long now = System.currentTimeMillis();
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container != p.getInventory()) {
                continue;
            }
            int idx = slot.getContainerSlot();
            int x = a.foodplanner$leftPos() + slot.x;
            int y = a.foodplanner$topPos() + slot.y;
            if (Snapshot.best != null && Snapshot.best.slot() == idx) {
                float pulse = 0.55f + 0.45f * (float) Math.sin(now / 250.0);
                FoodHud.outline(g, x - 1, y - 1, 18, 18, ((int) (0xFF * pulse) << 24) | (FoodHud.GOLD & 0xFFFFFF));
            }
            Foods.Food f = Foods.analyze(slot.getItem(), idx);
            if (f != null && f.kind() == Foods.Kind.BAD) {
                g.fill(x, y, x + 4, y + 4, 0xE0FF4040);
            }
        }
    }

    /** Where the panel goes next to the player inventory. */
    public static int[] rect(InventoryScreen screen) {
        AbstractContainerScreenAccessor a = (AbstractContainerScreenAccessor) screen;
        int right = a.foodplanner$leftPos() + a.foodplanner$imageWidth() + 4;
        int x = right + W <= screen.width - 2 ? right : a.foodplanner$leftPos() - 4 - W;
        return new int[]{Math.max(2, x), a.foodplanner$topPos(), W, a.foodplanner$imageHeight()};
    }

    /** Places the two planner buttons inside the panel (or hides them). */
    public static void placeButtons(InventoryScreen screen, CycleButton<?> activity, CycleButton<?> duration) {
        boolean show = FoodConfig.get().panel && Minecraft.getInstance().player != null
                && !Minecraft.getInstance().player.isCreative();
        activity.visible = show;
        duration.visible = show;
        if (!show) {
            return;
        }
        int[] r = rect(screen);
        activity.setX(r[0] + 5);
        activity.setY(r[1] + 78);
        duration.setX(r[0] + 5);
        duration.setY(r[1] + 98);
    }

    public static void panel(InventoryScreen screen, GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        FoodConfig.Data c = FoodConfig.get();
        if (p == null || !c.panel || p.isCreative()) {
            return;
        }
        Font font = mc.font;
        int[] r = rect(screen);
        int x = r[0];
        int y = r[1];
        int h = r[3];
        g.fill(x - 1, y - 1, x + W + 1, y + h + 1, 0x60FF9F43);
        g.fill(x, y, x + W, y + h, 0xF0121014);

        g.item(new ItemStack(Items.COOKED_BEEF), x + 4, y + 4);
        g.text(font, Component.translatable("foodplanner.panel.title"), x + 24, y + 5, 0xFFFFFFFF, false);
        Component pace = Pace.measured()
                ? Component.translatable("foodplanner.panel.pace", String.format(Locale.ROOT, "%.1f", Pace.rate()))
                : Component.translatable("foodplanner.panel.pace_learning");
        g.text(font, trim(font, pace.getString(), W - 26), x + 24, y + 15, GRAY, false);

        int ty = y + 30;
        Component lasts = Snapshot.carried <= 0 ? Component.translatable("foodplanner.hud.none")
                : Component.translatable("foodplanner.panel.lasts", FoodHud.time(Snapshot.minutes));
        g.text(font, trim(font, lasts.getString(), W - 8), x + 5, ty, Snapshot.carried <= 0 ? FoodHud.RED
                : FoodHud.color(Snapshot.minutes), false);
        ty += 13;
        if (Snapshot.best != null) {
            g.text(font, Component.translatable("foodplanner.panel.eat_now"), x + 5, ty + 4, GRAY, false);
            int lw = font.width(Component.translatable("foodplanner.panel.eat_now")) + 8;
            g.item(Snapshot.best.stack(), x + lw, ty);
            g.text(font, trim(font, Snapshot.best.stack().getHoverName().getString(), W - lw - 22), x + lw + 18, ty + 4,
                    FoodHud.GOLD, false);
        } else {
            Component msg = p.getFoodData().getFoodLevel() >= 20 ? Component.translatable("foodplanner.eat.full")
                    : Component.translatable("foodplanner.eat.nothing");
            g.text(font, trim(font, msg.getString(), W - 8), x + 5, ty + 4, GRAY, false);
        }

        ty = y + 66;
        g.fill(x + 5, ty - 3, x + W - 5, ty - 2, 0x30FFFFFF);
        g.text(font, Component.translatable("foodplanner.panel.trip"), x + 5, ty, 0xFFFF9F43, false);

        // planner result under the two buttons
        ty = y + 122;
        for (Component line : plan(c)) {
            g.text(font, trim(font, line.getString(), W - 8), x + 5, ty, 0xFFE6E6EE, false);
            ty += 11;
            if (ty > y + h - 10) {
                break;
            }
        }
    }

    /** "Need ~45 · have 30", "Take: ~2 × Steak", "or ~4 × Bread" / "Enough ✓". */
    private static List<Component> plan(FoodConfig.Data c) {
        List<Component> out = new ArrayList<>();
        float need = c.activity.pace() * c.duration;
        float have = Snapshot.carried + Snapshot.body;
        out.add(Component.translatable("foodplanner.plan.need", Math.round(need), Math.round(have)));
        float missing = need - have;
        if (missing <= 0) {
            out.add(Component.translatable("foodplanner.plan.enough"));
            return out;
        }
        Map<Item, float[]> stock = Foods.stock(Snapshot.foods);
        List<Map.Entry<Item, float[]>> options = new ArrayList<>(stock.entrySet());
        options.sort((a, b) -> Float.compare(b.getValue()[1], a.getValue()[1]));
        int shown = 0;
        for (Map.Entry<Item, float[]> e : options) {
            if (shown >= 2) {
                break;
            }
            int n = Foods.howMany(missing, e.getValue()[1]);
            out.add(Component.translatable(shown == 0 ? "foodplanner.plan.take" : "foodplanner.plan.or", n,
                    new ItemStack(e.getKey()).getHoverName()));
            shown++;
        }
        if (shown == 0) {
            // nothing to compare with: suggest the classics
            out.add(Component.translatable("foodplanner.plan.take", Foods.howMany(missing, 8 + 12.8f),
                    new ItemStack(Items.COOKED_BEEF).getHoverName()));
            out.add(Component.translatable("foodplanner.plan.or", Foods.howMany(missing, 5 + 6f),
                    new ItemStack(Items.BREAD).getHoverName()));
        }
        return out;
    }

    static String trim(Font font, String s, int max) {
        if (font.width(s) <= max) {
            return s;
        }
        return font.plainSubstrByWidth(s, Math.max(0, max - font.width("…"))) + "…";
    }
}
