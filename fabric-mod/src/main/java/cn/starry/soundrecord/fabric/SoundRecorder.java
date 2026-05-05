package cn.starry.soundrecord.fabric;

import cn.starry.soundrecord.common.RecordFile;
import cn.starry.soundrecord.common.RecordedSound;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class SoundRecorder {
    private static final int CHUNK_SIZE = 24_000;
    private final List<RecordedSound> sounds = new ArrayList<>();
    private boolean recording;
    private long startNanos;
    private Vec3 origin = Vec3.ZERO;

    public boolean recording() {
        return recording;
    }

    public int size() {
        return sounds.size();
    }

    public void start() {
        sounds.clear();
        startNanos = System.nanoTime();
        origin = currentPlayerPosition();
        recording = true;
    }

    public Path stopAndSave(String requestedName) throws IOException {
        recording = false;
        String fileName = RecordFile.sanitizeName(requestedName.isBlank() ? defaultName() : requestedName);
        Path localPath = FabricLoader.getInstance().getGameDir().resolve("soundrecord").resolve("records").resolve(fileName);
        RecordFile.write(localPath, normalizedSounds());
        uploadToServer(fileName, localPath);
        return localPath;
    }

    public void capture(SoundInstance instance) {
        if (!recording) {
            return;
        }
        Identifier id = instance.getIdentifier();
        if (id == null || isClickSound(id.toString()) || !SoundRecordClient.CONFIG.mode().accepts(id.toString())) {
            return;
        }
        SoundSource category = instance.getSource();
        Vec3 pos = resolvePosition(instance).subtract(origin);
        long delayMillis = Math.max(0L, (System.nanoTime() - startNanos) / 1_000_000L);
        sounds.add(new RecordedSound(
                delayMillis,
                id.toString(),
                category == null ? SoundSource.MASTER.getName() : category.getName(),
                pos.x,
                pos.y,
                pos.z,
                safeVolume(instance),
                safePitch(instance)
        ));
    }

    private float safeVolume(SoundInstance instance) {
        try {
            return Math.max(0.0F, instance.getVolume());
        } catch (NullPointerException ignored) {
            return 1.0F;
        }
    }

    private float safePitch(SoundInstance instance) {
        try {
            return Math.max(0.0F, instance.getPitch());
        } catch (NullPointerException ignored) {
            return 1.0F;
        }
    }

    private Vec3 resolvePosition(SoundInstance instance) {
        if (instance.isRelative()) {
            return currentPlayerPosition();
        }
        return new Vec3(instance.getX(), instance.getY(), instance.getZ());
    }

    private Vec3 currentPlayerPosition() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            return client.player.position();
        }
        return Vec3.ZERO;
    }

    private void uploadToServer(String fileName, Path localPath) throws IOException {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null || !ClientPlayNetworking.canSend(UploadPayload.TYPE)) {
            return;
        }
        byte[] bytes = Files.readAllBytes(localPath);
        int chunks = Math.max(1, (int) Math.ceil(bytes.length / (double) CHUNK_SIZE));
        ClientPlayNetworking.send(new UploadPayload("BEGIN", fileName, 0, chunks, new byte[0]));
        for (int i = 0; i < chunks; i++) {
            int start = i * CHUNK_SIZE;
            int end = Math.min(bytes.length, start + CHUNK_SIZE);
            byte[] chunk = new byte[end - start];
            System.arraycopy(bytes, start, chunk, 0, chunk.length);
            ClientPlayNetworking.send(new UploadPayload("CHUNK", fileName, i, chunks, chunk));
        }
        ClientPlayNetworking.send(new UploadPayload("END", fileName, chunks, chunks, new byte[0]));
    }

    private List<RecordedSound> normalizedSounds() {
        List<RecordedSound> copy = List.copyOf(sounds);
        if (!SoundRecordClient.CONFIG.mode().modern()) {
            return copy;
        }
        long start = copy.stream()
                .filter(sound -> !isClickSound(sound.soundId()))
                .mapToLong(RecordedSound::delayMillis)
                .findFirst()
                .orElse(0L);
        if (start <= 0L) {
            return copy;
        }
        return copy.stream()
                .map(sound -> new RecordedSound(
                        Math.max(0L, sound.delayMillis() - start),
                        sound.soundId(),
                        sound.category(),
                        sound.x(),
                        sound.y(),
                        sound.z(),
                        sound.volume(),
                        sound.pitch()
                ))
                .toList();
    }

    private boolean isClickSound(String soundId) {
        String normalized = soundId.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("click") || normalized.equals("minecraft:ui.button.click");
    }

    private static String defaultName() {
        return Long.toString(System.currentTimeMillis()) + RecordFile.EXTENSION;
    }

    public record UploadPayload(String action, String fileName, int index, int total, byte[] data) implements CustomPacketPayload {
        public static final Type<UploadPayload> TYPE = new Type<>(SoundRecordClient.UPLOAD_CHANNEL);
        public static final StreamCodec<RegistryFriendlyByteBuf, UploadPayload> STREAM_CODEC = CustomPacketPayload.codec(UploadPayload::write, UploadPayload::read);

        private static UploadPayload read(RegistryFriendlyByteBuf buf) {
            String action = buf.readUtf();
            String fileName = buf.readUtf();
            int index = buf.readVarInt();
            int total = buf.readVarInt();
            byte[] data = buf.readByteArray();
            return new UploadPayload(action, fileName, index, total, data);
        }

        private void write(RegistryFriendlyByteBuf buf) {
            buf.writeUtf(action);
            buf.writeUtf(fileName);
            buf.writeVarInt(index);
            buf.writeVarInt(total);
            buf.writeByteArray(data);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
