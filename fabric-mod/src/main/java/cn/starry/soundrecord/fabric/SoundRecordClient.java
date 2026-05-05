package cn.starry.soundrecord.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.Identifier;

public final class SoundRecordClient implements ClientModInitializer {
    public static final String MOD_ID = "soundrecord";
    public static final Identifier UPLOAD_CHANNEL = Identifier.fromNamespaceAndPath(MOD_ID, "upload");
    public static final SoundRecorder RECORDER = new SoundRecorder();
    public static final SoundRecordConfig CONFIG = new SoundRecordConfig();

    @Override
    public void onInitializeClient() {
        CONFIG.load();
        PayloadTypeRegistry.serverboundPlay().register(SoundRecorder.UploadPayload.TYPE, SoundRecorder.UploadPayload.STREAM_CODEC);
    }
}
