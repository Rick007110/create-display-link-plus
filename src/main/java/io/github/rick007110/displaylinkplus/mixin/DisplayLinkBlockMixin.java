package io.github.rick007110.displaylinkplus.mixin;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import io.github.rick007110.displaylinkplus.link.LinkEntry;
import io.github.rick007110.displaylinkplus.link.MultiLink;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Create only notifies links whose entry 0 has a source, and only hands entry 0's source to callbacks
 * (item throughput, accumulated count, redstone power). Extend both to the extra entries.
 */
@Mixin(DisplayLinkBlock.class)
public abstract class DisplayLinkBlockMixin {

    @Inject(method = "notifyGatherers", at = @At("RETURN"))
    private static void dlp$notifyLinksWithOnlyExtraSources(LevelAccessor level, BlockPos pos, CallbackInfo ci) {
        dlp$forEachAttachedLink(level, pos, be -> {
            // Links with a source on entry 0 were already handled by Create (and that updates every entry)
            if (be.activeSource == null && dlp$hasExtraSource(MultiLink.of(be).dlp$extraEntries()))
                be.tickSource();
        });
    }

    @Inject(method = "sendToGatherers", at = @At("RETURN"))
    private static <T extends DisplaySource> void dlp$sendToExtraEntries(LevelAccessor level, BlockPos pos,
                                                                        BiConsumer<DisplayLinkBlockEntity, T> callback,
                                                                        Class<T> type, CallbackInfo ci) {
        dlp$forEachAttachedLink(level, pos, be -> {
            MultiLink link = MultiLink.of(be);
            List<LinkEntry> extra = link.dlp$extraEntries();
            for (int i = 1; i <= extra.size(); i++) {
                if (!type.isInstance(extra.get(i - 1).activeSource))
                    continue;
                link.dlp$runWithEntry(i, () -> callback.accept(be, type.cast(be.activeSource)));
            }
        });
    }

    @Unique
    private static boolean dlp$hasExtraSource(List<LinkEntry> entries) {
        for (LinkEntry entry : entries)
            if (entry.activeSource != null)
                return true;
        return false;
    }

    /** Same lookup as Create's private forEachAttachedGatherer, minus the entry 0 source requirement. */
    @Unique
    private static void dlp$forEachAttachedLink(LevelAccessor level, BlockPos pos, Consumer<DisplayLinkBlockEntity> callback) {
        for (Direction d : Iterate.directions) {
            BlockPos offsetPos = pos.relative(d);
            if (!AllBlocks.DISPLAY_LINK.has(level.getBlockState(offsetPos)))
                continue;
            if (!(level.getBlockEntity(offsetPos) instanceof DisplayLinkBlockEntity be))
                continue;
            if (be.getDirection() != d.getOpposite() || MultiLink.of(be).dlp$extraEntries().isEmpty())
                continue;
            callback.accept(be);
        }
    }
}
