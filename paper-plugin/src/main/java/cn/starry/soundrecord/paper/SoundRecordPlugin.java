package cn.starry.soundrecord.paper;

import cn.starry.soundrecord.paper.api.SoundRecordProvider;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SoundRecordPlugin extends JavaPlugin {
    public static final String UPLOAD_CHANNEL = "soundrecord:upload";
    private RecordService recordService;

    @Override
    public void onEnable() {
        Path records = getDataFolder().toPath().resolve("records");
        try {
            Files.createDirectories(records);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create records directory", e);
        }
        recordService = new RecordService(this, records);
        SoundRecordProvider.register(recordService);

        PluginCommand command = getCommand("record");
        if (command != null) {
            RecordCommand executor = new RecordCommand(recordService);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        UploadListener uploadListener = new UploadListener(this, records);
        getServer().getMessenger().registerIncomingPluginChannel(this, UPLOAD_CHANNEL, uploadListener);
        getServer().getMessenger().registerOutgoingPluginChannel(this, UPLOAD_CHANNEL);
        getLogger().info("SoundRecord enabled. Records directory: " + records.toAbsolutePath());
    }

    @Override
    public void onDisable() {
        if (recordService != null) {
            recordService.stop();
            SoundRecordProvider.unregister(recordService);
        }
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
    }
}
