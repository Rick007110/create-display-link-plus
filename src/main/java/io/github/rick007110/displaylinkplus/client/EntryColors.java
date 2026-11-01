package io.github.rick007110.displaylinkplus.client;

/** One color per entry, shared by the GUI tabs and the in-world target overlay. */
public final class EntryColors {
    private static final int[] COLORS = {
            0xFFD86A, // 1 yellow (Create's selection color family)
            0x6AC4FF, // 2 blue
            0x7DE07D, // 3 green
            0xFF8A65, // 4 orange
            0xC792EA, // 5 purple
            0x4DD0C8, // 6 teal
            0xFF6F91, // 7 pink
            0xE0E0E0, // 8 white
    };

    private EntryColors() {
    }

    public static int of(int entryIndex) {
        return COLORS[Math.floorMod(entryIndex, COLORS.length)];
    }
}
