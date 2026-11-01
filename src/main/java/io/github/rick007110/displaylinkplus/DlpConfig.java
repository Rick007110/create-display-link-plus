package io.github.rick007110.displaylinkplus;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class DlpConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.IntValue MAX_ENTRIES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        MAX_ENTRIES = builder
                .comment("Maximum number of link entries (source -> display line pairs) a single Display Link can hold.")
                .defineInRange("maxEntriesPerLink", 8, 1, 8);
        SPEC = builder.build();
    }

    private DlpConfig() {
    }

    public static int maxEntries() {
        return SPEC.isLoaded() ? MAX_ENTRIES.get() : MAX_ENTRIES.getDefault();
    }
}
