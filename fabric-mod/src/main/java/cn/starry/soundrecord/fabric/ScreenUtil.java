package cn.starry.soundrecord.fabric;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class ScreenUtil {
    private ScreenUtil() {
    }

    public static void openConfig(Screen parent) {
        Minecraft.getInstance().setScreen(new SoundRecordScreen(parent));
    }
}
