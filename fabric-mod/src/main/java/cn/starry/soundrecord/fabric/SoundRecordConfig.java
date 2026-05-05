package cn.starry.soundrecord.fabric;

import cn.starry.soundrecord.common.RecordMode;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class SoundRecordConfig {
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("soundrecord.properties");
    private RecordMode mode = RecordMode.ALL_SOUNDS;
    private String fileName = "";

    public RecordMode mode() {
        return mode;
    }

    public void mode(RecordMode mode) {
        this.mode = mode;
        save();
    }

    public String fileName() {
        return fileName;
    }

    public void fileName(String fileName) {
        this.fileName = fileName;
        save();
    }

    public void load() {
        Properties properties = new Properties();
        if (Files.exists(PATH)) {
            try (var reader = Files.newBufferedReader(PATH)) {
                properties.load(reader);
            } catch (IOException ignored) {
            }
        }
        mode = RecordMode.byId(properties.getProperty("mode", mode.name()));
        fileName = properties.getProperty("fileName", fileName);
    }

    public void save() {
        Properties properties = new Properties();
        properties.setProperty("mode", mode.name());
        properties.setProperty("fileName", fileName);
        try {
            Files.createDirectories(PATH.getParent());
            try (var writer = Files.newBufferedWriter(PATH)) {
                properties.store(writer, "SoundRecord client config");
            }
        } catch (IOException ignored) {
        }
    }
}
