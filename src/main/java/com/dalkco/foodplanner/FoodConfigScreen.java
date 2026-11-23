package com.dalkco.foodplanner;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Settings: what is shown on the left, warnings and eating on the right. */
public class FoodConfigScreen extends Screen {
    private static final int COL_W = 160;
    private static final int H = 20;
    private static final int GAP = 22;

    private final Screen parent;
    private int top;

    public FoodConfigScreen(Screen parent) {
        super(Component.translatable("foodplanner.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        FoodConfig.Data c = FoodConfig.get();
        top = Math.max(34, this.height / 2 - 3 * GAP);
        int lx = this.width / 2 - COL_W - 4;
        int rx = this.width / 2 + 4;

        int y = top;
        y = toggle(lx, y, "hud", c.hud, v -> c.hud = v);
        addRenderableWidget(tip(new IntSlider(lx, y, COL_W, H, -60, 60, 1, c.hudOffsetY,
                v -> Component.translatable("foodplanner.config.offset", v), v -> c.hudOffsetY = v), "offset"));
        y += GAP;
        y = toggle(lx, y, "hotbar", c.hotbarMark, v -> c.hotbarMark = v);
        y = toggle(lx, y, "marks", c.inventoryMarks, v -> c.inventoryMarks = v);
        toggle(lx, y, "panel", c.panel, v -> c.panel = v);

        y = top;
        y = toggle(rx, y, "warnings", c.warnings, v -> c.warnings = v);
        y = toggle(rx, y, "sound", c.warnSound, v -> c.warnSound = v);
        addRenderableWidget(tip(new IntSlider(rx, y, COL_W, H, 1, 30, 1, c.lowMinutes,
                v -> Component.translatable("foodplanner.config.low", v), v -> c.lowMinutes = v), "low"));
        y += GAP;
        y = toggle(rx, y, "auto_eat", c.autoEat, v -> c.autoEat = v);
        addRenderableWidget(tip(new IntSlider(rx, y, COL_W, H, 6, 19, 1, c.autoEatAt,
                v -> Component.translatable("foodplanner.config.auto_eat_at", v), v -> c.autoEatAt = v), "auto_eat_at"));

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(this.width / 2 - 100, top + 5 * GAP + 16, 200, H).build());
    }

    private int toggle(int x, int y, String key, boolean value, Consumer<Boolean> setter) {
        addRenderableWidget(tip(CycleButton.onOffBuilder(value)
                .create(x, y, COL_W, H, Component.translatable("foodplanner.config." + key), (b, v) -> {
                    setter.accept(v);
                    FoodConfig.save();
                }), key));
        return y + GAP;
    }

    private static <T extends AbstractWidget> T tip(T widget, String key) {
        widget.setTooltip(Tooltip.create(Component.translatable("foodplanner.config." + key + ".tooltip")));
        return widget;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        g.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);
        g.text(this.font, Component.translatable("foodplanner.config.show"), this.width / 2 - COL_W - 4, top - 12,
                0xFFFF9F43, false);
        g.text(this.font, Component.translatable("foodplanner.config.help"), this.width / 2 + 4, top - 12,
                0xFFFF9F43, false);
    }

    @Override
    public void onClose() {
        FoodConfig.save();
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(parent);
        }
    }
}
