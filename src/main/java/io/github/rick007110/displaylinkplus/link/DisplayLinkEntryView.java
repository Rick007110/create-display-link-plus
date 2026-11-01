package io.github.rick007110.displaylinkplus.link;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import net.minecraft.core.BlockPos;

/**
 * Client-only stand-in that holds one extra entry of a real Display Link, so Create's unmodified
 * {@code DisplayLinkScreen} can edit it. Never placed in the world.
 */
public class DisplayLinkEntryView extends DisplayLinkBlockEntity {
    public final DisplayLinkBlockEntity link;
    public final int entryIndex;

    public DisplayLinkEntryView(DisplayLinkBlockEntity link, int entryIndex) {
        super(AllBlockEntityTypes.DISPLAY_LINK.get(), link.getBlockPos(), link.getBlockState());
        this.link = link;
        this.entryIndex = entryIndex;
        setLevel(link.getLevel());

        LinkEntry entry = MultiLink.of(link).dlp$snapshot(entryIndex);
        targetOffset = entry.targetOffset;
        activeSource = entry.activeSource;
        setSourceConfig(entry.sourceConfig);
        activeTarget = entry.activeTarget;
        targetLine = entry.targetLine;
    }

    /** Factory gauge links live on the real block entity's behaviour. */
    @Override
    public BlockPos getSourcePosition() {
        return link.getSourcePosition();
    }
}
