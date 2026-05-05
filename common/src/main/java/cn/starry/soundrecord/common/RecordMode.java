package cn.starry.soundrecord.common;

import java.util.Locale;
public enum RecordMode {
    ALL_SOUNDS,
    ALL_SOUNDS_MODERN,
    MUSIC_MODE,
    MUSIC_MODE_MODERN;

    public boolean accepts(String soundId) {
        String normalized = soundId.toLowerCase(Locale.ROOT);
        return switch (this) {
            case ALL_SOUNDS, ALL_SOUNDS_MODERN -> true;
            case MUSIC_MODE, MUSIC_MODE_MODERN -> isNoteBlock(normalized);
        };
    }

    public boolean modern() {
        return this == ALL_SOUNDS_MODERN || this == MUSIC_MODE_MODERN;
    }

    private static boolean isNoteBlock(String soundId) {
        return soundId.startsWith("minecraft:block.note_block.")
                || soundId.startsWith("minecraft:block.noteblock.");
    }

    public static RecordMode byId(String id) {
        for (RecordMode mode : values()) {
            if (mode.name().equalsIgnoreCase(id)) {
                return mode;
            }
        }
        if ("UNCONVENTIONAL".equalsIgnoreCase(id) || "NOTE_BLOCK".equalsIgnoreCase(id)) {
            return MUSIC_MODE;
        }
        return ALL_SOUNDS;
    }
}
