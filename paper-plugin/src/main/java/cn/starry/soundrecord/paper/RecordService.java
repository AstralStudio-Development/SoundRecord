package cn.starry.soundrecord.paper;

import cn.starry.soundrecord.common.RecordFile;
import cn.starry.soundrecord.common.RecordedSound;
import cn.starry.soundrecord.paper.api.SoundRecordApi;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class RecordService implements SoundRecordApi {
    private static final int MAX_SOUNDS_PER_TICK = 256;

    private final SoundRecordPlugin plugin;
    private final Path recordsDirectory;
    private final Map<UUID, PlaybackSession> sessions = new ConcurrentHashMap<>();
    private final Set<String> warnedInvalidSounds = ConcurrentHashMap.newKeySet();

    public RecordService(SoundRecordPlugin plugin, Path recordsDirectory) {
        this.plugin = plugin;
        this.recordsDirectory = recordsDirectory;
    }

    @Override
    public Collection<String> listRecords() {
        if (!Files.isDirectory(recordsDirectory)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(recordsDirectory)) {
            return stream
                    .filter(path -> path.getFileName().toString().endsWith(RecordFile.EXTENSION))
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            plugin.getLogger().warning("Unable to list records: " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public Optional<Path> findRecord(String fileName) {
        Path path = recordsDirectory.resolve(RecordFile.sanitizeName(fileName)).normalize();
        if (!path.startsWith(recordsDirectory.normalize()) || !Files.isRegularFile(path)) {
            return Optional.empty();
        }
        return Optional.of(path);
    }

    @Override
    public List<RecordedSound> loadRecord(String fileName) throws IOException {
        Path path = findRecord(fileName).orElseThrow(() -> new IOException("Record not found: " + fileName));
        return RecordFile.read(path);
    }

    @Override
    public void play(String fileName, Location origin) throws IOException {
        play(fileName, origin, new ArrayList<>(Bukkit.getOnlinePlayers()));
    }

    @Override
    public void play(String fileName, Location origin, Collection<? extends Player> listeners) throws IOException {
        List<ScheduledSound> sounds = loadScheduled(fileName);
        for (Player player : listeners) {
            startSession(fileName, player, sounds, () -> origin);
        }
    }

    @Override
    public void playFollowing(String fileName, Player originPlayer) throws IOException {
        playFor(fileName, originPlayer);
    }

    @Override
    public void playFor(String fileName, Player player) throws IOException {
        startSession(fileName, player, loadScheduled(fileName), player::getLocation);
    }

    public void playForAll(String fileName) throws IOException {
        List<ScheduledSound> sounds = loadScheduled(fileName);
        for (Player player : Bukkit.getOnlinePlayers()) {
            startSession(fileName, player, sounds, player::getLocation);
        }
    }

    public Optional<String> status(Player player) {
        PlaybackSession session = sessions.get(player.getUniqueId());
        return session == null ? Optional.empty() : Optional.of(session.displayName);
    }

    public void stop(Player player) {
        PlaybackSession session = sessions.remove(player.getUniqueId());
        if (session != null) {
            session.cancel();
        }
    }

    @Override
    public void stop() {
        for (PlaybackSession session : sessions.values()) {
            session.cancel();
        }
        sessions.clear();
    }

    private List<ScheduledSound> loadScheduled(String fileName) throws IOException {
        return loadRecord(fileName).stream()
                .sorted(Comparator.comparingLong(RecordedSound::delayMillis))
                .map(this::schedule)
                .toList();
    }

    private ScheduledSound schedule(RecordedSound sound) {
        return new ScheduledSound(
                Math.max(0L, Math.round(sound.delayMillis() / 50.0D)),
                sound.soundId(),
                parseCategory(sound.category()),
                sound.x(),
                sound.y(),
                sound.z(),
                sound.volume(),
                sound.pitch()
        );
    }

    private void startSession(String fileName, Player player, List<ScheduledSound> sounds, Supplier<Location> originSupplier) {
        stop(player);
        PlaybackSession session = new PlaybackSession(player.getUniqueId(), displayName(fileName), sounds, originSupplier);
        sessions.put(player.getUniqueId(), session);
        session.task = Bukkit.getScheduler().runTaskTimer(plugin, session::tick, 0L, 1L);
    }

    private void finish(UUID playerId, PlaybackSession session) {
        if (sessions.remove(playerId, session)) {
            session.cancel();
        }
    }

    private void playSound(Location origin, Player player, ScheduledSound sound) {
        Location location = new Location(
                origin.getWorld(),
                origin.getX() + sound.x,
                origin.getY() + sound.y,
                origin.getZ() + sound.z
        );
        try {
            player.playSound(location, sound.soundId, sound.category, sound.volume, sound.pitch);
        } catch (IllegalArgumentException e) {
            if (warnedInvalidSounds.add(sound.soundId)) {
                plugin.getLogger().warning("Unable to play sound " + sound.soundId + ": " + e.getMessage());
            }
        }
    }

    private SoundCategory parseCategory(String category) {
        for (SoundCategory value : SoundCategory.values()) {
            if (value.name().equalsIgnoreCase(category)) {
                return value;
            }
        }
        return SoundCategory.MASTER;
    }

    public static String displayName(String fileName) {
        return fileName.toLowerCase().endsWith(RecordFile.EXTENSION)
                ? fileName.substring(0, fileName.length() - RecordFile.EXTENSION.length())
                : fileName;
    }

    private record ScheduledSound(
            long tick,
            String soundId,
            SoundCategory category,
            double x,
            double y,
            double z,
            float volume,
            float pitch
    ) {
    }

    private final class PlaybackSession {
        private final UUID playerId;
        private final String displayName;
        private final List<ScheduledSound> sounds;
        private final Supplier<Location> originSupplier;
        private BukkitTask task;
        private int index;
        private long elapsedTicks;

        private PlaybackSession(UUID playerId, String displayName, List<ScheduledSound> sounds, Supplier<Location> originSupplier) {
            this.playerId = playerId;
            this.displayName = displayName;
            this.sounds = sounds;
            this.originSupplier = originSupplier;
        }

        private void tick() {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                finish(playerId, this);
                return;
            }
            Location origin = originSupplier.get();
            if (origin == null || origin.getWorld() == null) {
                finish(playerId, this);
                return;
            }

            int played = 0;
            while (index < sounds.size() && sounds.get(index).tick <= elapsedTicks && played < MAX_SOUNDS_PER_TICK) {
                playSound(origin, player, sounds.get(index));
                index++;
                played++;
            }
            if (index >= sounds.size()) {
                finish(playerId, this);
                return;
            }
            elapsedTicks++;
        }

        private void cancel() {
            if (task != null) {
                task.cancel();
                task = null;
            }
        }
    }
}
