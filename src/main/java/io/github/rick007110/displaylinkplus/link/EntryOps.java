package io.github.rick007110.displaylinkplus.link;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.content.trains.display.FlapDisplayBlockEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import io.github.rick007110.displaylinkplus.DlpConfig;

/**
 * Entry mutations shared by the server (authoritative) and the client (applied right away so switching tabs
 * shows the edit before the server's sync arrives). None of these trigger display updates by themselves.
 */
public final class EntryOps {
    private EntryOps() {
    }

    /**
     * Applies a GUI result to whichever entry is currently swapped into {@code be}. Same rules as Create's
     * {@code DisplayLinkConfigurationPacket#applySettings}. Returns true if the source was set.
     */
    public static boolean applyConfig(DisplayLinkBlockEntity be, CompoundTag configData, int targetLine) {
        be.targetLine = targetLine;
        if (!configData.contains("Id"))
            return false;
        DisplaySource source = DisplaySource.get(ResourceLocation.tryParse(configData.getString("Id")));
        if (source == null)
            return false;
        if (be.activeSource != null && be.activeSource == source) {
            be.getSourceConfig().merge(configData);
        } else {
            be.activeSource = source;
            be.setSourceConfig(configData.copy());
        }
        return true;
    }

    public static boolean canAdd(DisplayLinkBlockEntity be) {
        return MultiLink.of(be).dlp$entryCount() < DlpConfig.maxEntries();
    }

    /**
     * Adds a copy of entry {@code index} aimed at the next free line of the same display. Returns the new entry's index,
     * or -1 if the cap is reached or the index is invalid.
     */
    public static int addCopy(DisplayLinkBlockEntity be, int index) {
        MultiLink link = MultiLink.of(be);
        if (!canAdd(be) || index < 0 || index >= link.dlp$entryCount())
            return -1;
        LinkEntry copy = link.dlp$snapshot(index);
        copy.targetLine = nextLine(be, index, copy.targetLine);
        link.dlp$extraEntries().add(copy);
        return link.dlp$entryCount() - 1;
    }

    /** Next line after {@code line} that no other entry on the same display starts on; clamps to the last row. */
    private static int nextLine(DisplayLinkBlockEntity be, int index, int line) {
        int[] rows = {Integer.MAX_VALUE};
        MultiLink.of(be).dlp$runWithEntry(index, () -> {
            if (be.getLevel() == null)
                return;
            DisplayTarget target = DisplayTarget.get(be.getLevel(), be.getTargetPosition());
            if (target == null)
                return;
            DisplayTargetStats stats = target.provideStats(new DisplayLinkContext(be.getLevel(), be));
            rows[0] = stats.maxRows();
        });
        int last = Math.max(0, rows[0] - 1);
        Set<Integer> used = usedLines(be, index);
        for (int candidate = line + 1; candidate <= last && candidate < line + 64; candidate++)
            if (!used.contains(candidate))
                return candidate;
        return Math.min(line + 1, last);
    }

    /** Start lines of all entries that write to the same display as entry {@code index} (including itself). */
    private static Set<Integer> usedLines(DisplayLinkBlockEntity be, int index) {
        Set<Integer> used = new HashSet<>();
        MultiLink link = MultiLink.of(be);
        BlockPos display = displayOf(be, index);
        for (int i = 0; i < link.dlp$entryCount(); i++)
            if (display.equals(displayOf(be, i)))
                used.add(link.dlp$snapshot(i).targetLine);
        return used;
    }

    /** Other entries that start on the same line of the same display as entry {@code index}; they overwrite each other. */
    public static List<Integer> conflicts(DisplayLinkBlockEntity be, int index) {
        MultiLink link = MultiLink.of(be);
        LinkEntry entry = link.dlp$snapshot(index);
        BlockPos display = displayOf(be, index);
        List<Integer> conflicts = new ArrayList<>();
        for (int i = 0; i < link.dlp$entryCount(); i++) {
            if (i == index || link.dlp$snapshot(i).targetLine != entry.targetLine)
                continue;
            if (display.equals(displayOf(be, i)))
                conflicts.add(i);
        }
        return conflicts;
    }

    /** Position identifying the display entry {@code index} writes to (a Display Board's controller, else the block). */
    public static BlockPos displayOf(DisplayLinkBlockEntity be, int index) {
        BlockPos pos = be.getBlockPos().offset(MultiLink.of(be).dlp$snapshot(index).targetOffset);
        return displayAt(be.getLevel(), pos);
    }

    public static BlockPos displayAt(@Nullable Level level, BlockPos pos) {
        if (level != null && level.getBlockEntity(pos) instanceof FlapDisplayBlockEntity flap) {
            FlapDisplayBlockEntity controller = flap.getController();
            if (controller != null)
                return controller.getBlockPos();
        }
        return pos;
    }
}
