package com.dalkco.foodplanner.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Where the container GUI is, so the log panel can sit next to it. */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int foodplanner$leftPos();

    @Accessor("topPos")
    int foodplanner$topPos();

    @Accessor("imageWidth")
    int foodplanner$imageWidth();

    @Accessor("imageHeight")
    int foodplanner$imageHeight();
}
