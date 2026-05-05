package cn.starry.soundrecord.fabric;

import cn.starry.soundrecord.common.RecordMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.io.IOException;

public final class SoundRecordScreen extends Screen {
    private final Screen parent;
    private EditBox fileNameField;
    private Button modeButton;
    private Button recordButton;
    private String status = "";

    public SoundRecordScreen(Screen parent) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.translatable("soundrecord.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int center = width / 2;
        fileNameField = new EditBox(font, center - 100, height / 2 - 40, 200, 20, Component.translatable("soundrecord.field.tick_name"));
        fileNameField.setHint(Component.translatable("soundrecord.field.tick_name").withColor(0x555555));
        fileNameField.setValue(SoundRecordClient.CONFIG.fileName());
        fileNameField.setResponder(SoundRecordClient.CONFIG::fileName);
        addRenderableWidget(fileNameField);

        modeButton = Button.builder(modeText(), button -> cycleMode())
                .bounds(center - 100, height / 2 - 12, 200, 20)
                .build();
        addRenderableWidget(modeButton);

        recordButton = Button.builder(recordText(), button -> toggleRecording())
                .bounds(center - 100, height / 2 + 16, 200, 20)
                .build();
        addRenderableWidget(recordButton);

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> minecraft.setScreen(parent))
                .bounds(center - 100, height / 2 + 44, 200, 20)
                .build());
    }

    private void cycleMode() {
        RecordMode[] modes = RecordMode.values();
        RecordMode next = modes[(SoundRecordClient.CONFIG.mode().ordinal() + 1) % modes.length];
        SoundRecordClient.CONFIG.mode(next);
        modeButton.setMessage(modeText());
    }

    private void toggleRecording() {
        if (SoundRecordClient.RECORDER.recording()) {
            try {
                var path = SoundRecordClient.RECORDER.stopAndSave(fileNameField.getValue());
                sendSavedMessage(path.toAbsolutePath().toString());
                status = Component.translatable("soundrecord.message.saved_status", path.getFileName().toString()).getString();
            } catch (IOException e) {
                status = "Save failed: " + e.getMessage();
            }
        } else {
            SoundRecordClient.RECORDER.start();
            sendStartMessage();
            status = Component.translatable("soundrecord.message.started").getString();
        }
        recordButton.setMessage(recordText());
    }

    private Component modeText() {
        return Component.translatable("soundrecord.mode.line", modeName(SoundRecordClient.CONFIG.mode()));
    }

    private Component modeName(RecordMode mode) {
        return switch (mode) {
            case ALL_SOUNDS -> Component.translatable("soundrecord.mode.all_sound");
            case ALL_SOUNDS_MODERN -> Component.translatable("soundrecord.mode.modern_format",
                    Component.translatable("soundrecord.mode.all_sound"),
                    Component.translatable("soundrecord.mode.modern"));
            case MUSIC_MODE -> Component.translatable("soundrecord.mode.music");
            case MUSIC_MODE_MODERN -> Component.translatable("soundrecord.mode.modern_format",
                    Component.translatable("soundrecord.mode.music"),
                    Component.translatable("soundrecord.mode.modern"));
        };
    }

    private Component recordText() {
        return Component.translatable(SoundRecordClient.RECORDER.recording()
                ? "soundrecord.button.stop_save"
                : "soundrecord.button.start_recording");
    }

    private void sendStartMessage() {
        if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.translatable("soundrecord.message.started").withStyle(ChatFormatting.GREEN));
        }
    }

    private void sendSavedMessage(String path) {
        if (minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.translatable("soundrecord.message.saved")
                    .withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(path).withStyle(ChatFormatting.AQUA)));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, height / 2 - 70, 0xFFFFFF);
        graphics.centeredText(font, Component.literal(status), width / 2, height / 2 + 72, 0xA0FFA0);
    }
}
