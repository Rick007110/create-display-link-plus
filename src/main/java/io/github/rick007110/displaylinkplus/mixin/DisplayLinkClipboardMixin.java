package io.github.rick007110.displaylinkplus.mixin;

import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import io.github.rick007110.displaylinkplus.link.EntryOps;
import io.github.rick007110.displaylinkplus.link.LinkEntry;
import io.github.rick007110.displaylinkplus.link.MultiLink;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Create's Clipboard can copy (right-click) and paste (left-click) Display Link settings.
 * Pasting gives each existing entry the copied information type, settings and line, but keeps its own display.
 * Copied entries beyond the link's own count are added with the copied relative target, so an identical setup
 * built next to the original is reproduced completely.
 */
@Mixin(DisplayLinkBlockEntity.class)
public abstract class DisplayLinkClipboardMixin implements ClipboardCloneable {
    @Unique
    private static final String ENTRIES_KEY = "Entries";

    @Unique
    private DisplayLinkBlockEntity dlp$self() {
        return (DisplayLinkBlockEntity) (Object) this;
    }

    @Override
    public String getClipboardKey() {
        return "DisplayLink";
    }

    @Override
    public boolean writeToClipboard(@NotNull HolderLookup.Provider registries, CompoundTag tag, Direction side) {
        MultiLink link = MultiLink.of(dlp$self());
        ListTag entries = new ListTag();
        for (int i = 0; i < link.dlp$entryCount(); i++)
            entries.add(link.dlp$snapshot(i).write(false));
        tag.put(ENTRIES_KEY, entries);
        return true;
    }

    @Override
    public boolean readFromClipboard(@NotNull HolderLookup.Provider registries, CompoundTag tag, Player player, Direction side, boolean simulate) {
        if (!tag.contains(ENTRIES_KEY))
            return false;
        if (simulate)
            return true;

        DisplayLinkBlockEntity be = dlp$self();
        MultiLink link = MultiLink.of(be);
        int range = AllConfigs.server().logistics.displayLinkRange.get();
        ListTag entries = tag.getList(ENTRIES_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            LinkEntry copied = LinkEntry.read(entries.getCompound(i), false);
            if (i < link.dlp$entryCount()) {
                link.dlp$runWithEntry(i, () -> {
                    be.activeSource = copied.activeSource;
                    be.setSourceConfig(copied.sourceConfig.copy());
                    be.targetLine = copied.targetLine;
                });
            } else if (EntryOps.canAdd(be) && !copied.targetOffset.equals(BlockPos.ZERO)
                    && copied.targetOffset.closerThan(BlockPos.ZERO, range)) {
                link.dlp$extraEntries().add(new LinkEntry(copied.targetOffset, copied.activeSource, copied.sourceConfig.copy(), null, copied.targetLine));
            }
        }
        be.updateGatheredData();
        be.notifyUpdate();
        return true;
    }
}
