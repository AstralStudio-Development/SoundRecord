package cn.starry.soundrecord.paper;

import cn.starry.soundrecord.common.RecordFile;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class UploadListener implements PluginMessageListener {
    private final SoundRecordPlugin plugin;
    private final Path recordsDirectory;
    private final Map<UUID, UploadState> uploads = new HashMap<>();

    public UploadListener(SoundRecordPlugin plugin, Path recordsDirectory) {
        this.plugin = plugin;
        this.recordsDirectory = recordsDirectory;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!SoundRecordPlugin.UPLOAD_CHANNEL.equals(channel) || !player.hasPermission("soundrecord.upload")) {
            return;
        }
        try (DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(message))) {
            handle(player, in);
        } catch (IOException e) {
            plugin.getLogger().warning("Invalid SoundRecord upload from " + player.getName() + ": " + e.getMessage());
        }
    }

    private void handle(Player player, DataInputStream in) throws IOException {
        String action = readString(in);
        String fileName = RecordFile.sanitizeName(readString(in));
        int index = readVarInt(in);
        int total = readVarInt(in);
        byte[] data = readByteArray(in);

        switch (action) {
            case "BEGIN" -> uploads.put(player.getUniqueId(), new UploadState(fileName, total));
            case "CHUNK" -> {
                UploadState state = uploads.get(player.getUniqueId());
                if (state == null || !state.fileName.equals(fileName) || index < 0 || index >= state.total) {
                    throw new IOException("Unexpected chunk " + index + " for " + fileName);
                }
                state.buffer.write(data);
                state.received++;
            }
            case "END" -> finish(player, fileName);
            default -> throw new IOException("Unknown action " + action);
        }
    }

    private void finish(Player player, String fileName) throws IOException {
        UploadState state = uploads.remove(player.getUniqueId());
        if (state == null || !state.fileName.equals(fileName) || state.received != state.total) {
            throw new IOException("Incomplete upload for " + fileName);
        }
        Path path = recordsDirectory.resolve(fileName).normalize();
        if (!path.startsWith(recordsDirectory.normalize())) {
            throw new IOException("Invalid target path");
        }
        Files.createDirectories(recordsDirectory);
        Files.write(path, state.buffer.toByteArray());
        plugin.getLogger().info("Saved SoundRecord upload " + fileName + " from " + player.getName());
        player.sendRichMessage("<green>SoundRecord uploaded to server: <white>" + fileName + "</white>");
    }

    private static int readVarInt(DataInputStream in) throws IOException {
        int value = 0;
        int position = 0;
        byte currentByte;
        do {
            currentByte = in.readByte();
            value |= (currentByte & 0x7F) << position;
            position += 7;
            if (position >= 32) {
                throw new IOException("VarInt is too big");
            }
        } while ((currentByte & 0x80) == 0x80);
        return value;
    }

    private static String readString(DataInputStream in) throws IOException {
        int length = readVarInt(in);
        if (length < 0 || length > 32767) {
            throw new IOException("Invalid string length " + length);
        }
        byte[] data = new byte[length];
        in.readFully(data);
        return new String(data, StandardCharsets.UTF_8);
    }

    private static byte[] readByteArray(DataInputStream in) throws IOException {
        int length = readVarInt(in);
        if (length < 0 || length > 32_000) {
            throw new IOException("Invalid chunk length " + length);
        }
        byte[] data = new byte[length];
        in.readFully(data);
        return data;
    }

    private static final class UploadState {
        private final String fileName;
        private final int total;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private int received;

        private UploadState(String fileName, int total) {
            this.fileName = fileName;
            this.total = total;
        }
    }
}
