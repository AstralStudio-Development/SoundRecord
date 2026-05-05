package cn.starry.soundrecord.common;

public record RecordedSound(
        long delayMillis,
        String soundId,
        String category,
        double x,
        double y,
        double z,
        float volume,
        float pitch
) {
}
