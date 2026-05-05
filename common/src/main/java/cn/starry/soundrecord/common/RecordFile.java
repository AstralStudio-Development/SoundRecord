package cn.starry.soundrecord.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class RecordFile {
    public static final String EXTENSION = ".srd";
    private static final int MAGIC = 0x53524431;
    private static final int VERSION = 1;

    private RecordFile() {
    }

    public static void write(Path path, List<RecordedSound> sounds) throws IOException {
        Files.createDirectories(path.getParent());
        try (OutputStream file = Files.newOutputStream(path);
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(file))) {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            out.writeInt(sounds.size());
            for (RecordedSound sound : sounds) {
                out.writeLong(sound.delayMillis());
                out.writeUTF(sound.soundId());
                out.writeUTF(sound.category());
                out.writeDouble(sound.x());
                out.writeDouble(sound.y());
                out.writeDouble(sound.z());
                out.writeFloat(sound.volume());
                out.writeFloat(sound.pitch());
            }
        }
    }

    public static List<RecordedSound> read(Path path) throws IOException {
        try (InputStream file = Files.newInputStream(path);
             DataInputStream in = new DataInputStream(new BufferedInputStream(file))) {
            int magic = in.readInt();
            if (magic != MAGIC) {
                throw new IOException("Not a SoundRecord file");
            }
            int version = in.readInt();
            if (version != VERSION) {
                throw new IOException("Unsupported SoundRecord version " + version);
            }
            int count = in.readInt();
            List<RecordedSound> sounds = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                sounds.add(new RecordedSound(
                        in.readLong(),
                        in.readUTF(),
                        in.readUTF(),
                        in.readDouble(),
                        in.readDouble(),
                        in.readDouble(),
                        in.readFloat(),
                        in.readFloat()
                ));
            }
            return sounds;
        }
    }

    public static String sanitizeName(String name) {
        String clean = name.replace('\\', '/');
        int slash = clean.lastIndexOf('/');
        if (slash >= 0) {
            clean = clean.substring(slash + 1);
        }
        clean = clean.replaceAll("[^A-Za-z0-9._-]", "_");
        if (!clean.endsWith(EXTENSION)) {
            clean = clean + EXTENSION;
        }
        return clean.isBlank() ? "recording" + EXTENSION : clean;
    }
}
