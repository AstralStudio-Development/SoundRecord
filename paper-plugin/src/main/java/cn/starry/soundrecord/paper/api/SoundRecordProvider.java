package cn.starry.soundrecord.paper.api;

public final class SoundRecordProvider {
    private static SoundRecordApi api;

    private SoundRecordProvider() {
    }

    public static SoundRecordApi get() {
        if (api == null) {
            throw new IllegalStateException("SoundRecord plugin is not enabled");
        }
        return api;
    }

    public static void register(SoundRecordApi api) {
        SoundRecordProvider.api = api;
    }

    public static void unregister(SoundRecordApi api) {
        if (SoundRecordProvider.api == api) {
            SoundRecordProvider.api = null;
        }
    }
}
