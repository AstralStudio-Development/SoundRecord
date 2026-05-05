package cn.starry.soundrecord.paper.api;

import cn.starry.soundrecord.common.RecordedSound;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SoundRecordApi {
    Collection<String> listRecords();

    Optional<Path> findRecord(String fileName);

    List<RecordedSound> loadRecord(String fileName) throws IOException;

    void play(String fileName, Location origin) throws IOException;

    void play(String fileName, Location origin, Collection<? extends Player> listeners) throws IOException;

    void playFollowing(String fileName, Player originPlayer) throws IOException;

    void playFor(String fileName, Player player) throws IOException;

    void stop();
}
