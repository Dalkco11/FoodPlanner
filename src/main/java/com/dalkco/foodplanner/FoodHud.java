package com.dalkco.foodplanner;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/** "~23 min" above the hunger bar with the food to eat next, and a frame around that food in the hotbar. */
public final class FoodHud {
    public static final int GOLD = 0xFFFFC857;
    public static final int GREEN = 0xFF7CE08A;
    public static final int YELLOW = 0xFFFFD36B;
    public static final int RED = 0xFFFF6A6A;

    private FoodHud() {
    }

    /** "~23 min", "~1 h 20 min", "∞". */
    public static Component time(float minutes) {
        if (Float.isInfinite(minutes) || minutes > 99 * 60) {
            return Component.translatable("foodplanner.time.inf");
        }
        int m = Math.max(0, Math.round(minutes));
        if (m < 60) {
            return Component.translatable("foodplanner.time.min", m);
        }
        return Component.translatable("foodplanner.time.hm", m / 60, m % 60);
    }

    public static int color(float minutes) {
        if (minutes >= 15) {
            return GREEN;
        }
        return minutes >= FoodConfig.get().lowMinutes ? YELLOW : RED;
    }

    /** The item to show next to the time: what to eat now, otherwise the biggest food stock. */
    static ItemStack icon() {
        if (Snapshot.best != null) {
            return Snapshot.best.stack();
        }
        Map<net.minecraft.world.item.Item, float[]> stock = Foods.stock(Snapshot.foods);
        for (net.minecraft.world.item.Item item : stock.keySet()) {
            return new ItemStack(item);
        }
        return ItemStack.EMPTY;
    }

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        FoodConfig.Data c = FoodConfig.get();
        if (p == null || p.isCreative() || p.isSpectator() || mc.gui.screen() != null) {
            return;
        }
        Font font = mc.font;
        int w = g.guiWidth();
        int h = g.guiHeight();
        long now = System.currentTimeMillis();

        if (c.hotbarMark && Snapshot.best != null && Snapshot.best.slot() < 9) {
            int x = w / 2 - 91 + 3 + Snapshot.best.slot() * 20;
            int y = h - 19;
            float pulse = 0.55f + 0.45f * (float) Math.sin(now / 250.0);
            outline(g, x - 1, y - 1, 18, 18, ((int) (0xFF * pulse) << 24) | (GOLD & 0xFFFFFF));
        }

        if (!c.hud) {
            return;
        }
        float minutes = Snapshot.minutes;
        boolean none = Snapshot.carried <= 0;
        Component text = none ? Component.translatable("foodplanner.hud.none") : time(minutes);
        int color = none ? RED : color(minutes);
        if (color == RED && (now / 400) % 2 == 0) {
            color = 0xFFFFB0B0;
        }
        ItemStack icon = icon();
        int tw = font.width(text);
        int bw = 4 + (icon.isEmpty() ? 0 : 13) + tw + 4;
        int right = w / 2 + 91;
        int x = right - bw;
        int y = h - 39 - 13 + c.hudOffsetY;
        g.fill(x, y, x + bw, y + 12, 0x90000000);
        int tx = x + 4;
        if (!icon.isEmpty()) {
            g.pose().pushMatrix();
            g.pose().translate(x + 2, y);
            g.pose().scale(0.75f, 0.75f);
            g.item(icon, 0, 0);
            g.pose().popMatrix();
            tx += 13;
        }
        g.text(font, text, tx, y + 2, color, false);
    }

    static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }
}
