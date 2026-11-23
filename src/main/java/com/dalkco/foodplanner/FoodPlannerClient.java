package com.dalkco.foodplanner;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class FoodPlannerClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("foodplanner");

    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("foodplanner", "foodplanner"));

    private static KeyMapping eatKey;
    /** screen to open on the next tick (after chat has closed) */
    private static Screen pending;

    @Override
    public void onInitializeClient() {
        FoodConfig.load();
        eatKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.foodplanner.eat",
                InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (pending != null && client.gui.screen() == null) {
                Screen s = pending;
                pending = null;
                client.gui.setScreen(s);
            }
            if (client.player == null) {
                return;
            }
            Pace.tick(client.player);
            Snapshot.update(client.player);
            Warnings.tick(client);
            while (eatKey.consumeClick()) {
                Eater.eatBest(client, true);
            }
            Eater.tick(client);
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            Pace.reset();
            Warnings.reset();
        });

        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("foodplanner", "hud"), FoodHud::render);

        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof AbstractContainerScreen<?> cs)) {
                return;
            }
            if (screen instanceof InventoryScreen inv) {
                FoodConfig.Data c = FoodConfig.get();
                CycleButton<FoodConfig.Activity> activity = CycleButton.<FoodConfig.Activity>builder(
                                a -> Component.translatable("foodplanner.activity." + a.name().toLowerCase(Locale.ROOT)),
                                c.activity)
                        .withValues(FoodConfig.Activity.values())
                        .create(0, 0, InventoryOverlay.W - 10, 18, Component.translatable("foodplanner.panel.activity"),
                                (b, v) -> { c.activity = v; FoodConfig.save(); });
                activity.setTooltip(Tooltip.create(Component.translatable("foodplanner.panel.activity.tooltip")));
                CycleButton<Integer> duration = CycleButton.<Integer>builder(
                                d -> d < 60 ? Component.translatable("foodplanner.duration.min", d)
                                        : Component.translatable("foodplanner.duration.h", d / 60), c.duration)
                        .withValues(5, 15, 30, 60, 120, 240)
                        .create(0, 0, InventoryOverlay.W - 10, 18, Component.translatable("foodplanner.panel.duration"),
                                (b, v) -> { c.duration = v; FoodConfig.save(); });
                Screens.getWidgets(screen).add(activity);
                Screens.getWidgets(screen).add(duration);
                ScreenEvents.beforeExtract(screen).register((s, g, mx, my, d) ->
                        InventoryOverlay.placeButtons(inv, activity, duration));
                ScreenEvents.afterBackground(screen).register((s, g, mx, my, d) -> InventoryOverlay.panel(inv, g));
            }
            ScreenEvents.afterExtract(screen).register((s, g, mx, my, d) -> InventoryOverlay.marks(cs, g));
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommands.literal("foodplanner").executes(ctx -> {
                    pending = new FoodConfigScreen(null);
                    return 1;
                })));
    }
}
