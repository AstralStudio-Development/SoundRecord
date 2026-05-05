package cn.starry.soundrecord.fabric.mixin;

import cn.starry.soundrecord.fabric.ScreenUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
    protected GameMenuScreenMixin(Component title) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void soundrecord$addButton(CallbackInfo ci) {
        int buttonWidth = 204;
        int x = width / 2 - buttonWidth / 2;
        int y = height / 4 + 144;
        addRenderableWidget(Button.builder(Component.translatable("soundrecord.button.pause"), button -> ScreenUtil.openConfig(this))
                .bounds(x, y, buttonWidth, 20)
                .build());
    }
}
