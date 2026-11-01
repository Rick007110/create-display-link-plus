package io.github.rick007110.displaylinkplus.mixin;

import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import io.github.rick007110.displaylinkplus.link.EntryOps;
import io.github.rick007110.displaylinkplus.link.MultiLink;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Create reserves display lines per Display Link position, so a multi-line entry (a list) runs straight over a sibling
 * entry of the same link. Also record which entry reserved a line, and treat a sibling's reservation like another
 * link's: the list stops there, exactly as it would for two separate links.
 */
@Mixin(DisplayTarget.class)
public abstract class DisplayTargetMixin {
    @Unique
    private static final String ENTRY_KEY = "DlpEntry";

    @Inject(method = "reserve", at = @At("RETURN"))
    private static void dlp$rememberEntry(int line, BlockEntity target, DisplayLinkContext context, CallbackInfo ci) {
        if (line == 0)
            return;
        CompoundTag tag = target.getPersistentData();
        CompoundTag compound = tag.getCompound("DisplayLink");
        compound.putInt(ENTRY_KEY + line, Math.max(0, MultiLink.of(context.blockEntity()).dlp$activeEntry()));
        tag.put("DisplayLink", compound);
    }

    @Inject(method = "isReserved", at = @At("HEAD"), cancellable = true)
    private void dlp$reservedBySibling(int line, BlockEntity target, DisplayLinkContext context, CallbackInfoReturnable<Boolean> cir) {
        CompoundTag compound = target.getPersistentData().getCompound("DisplayLink");
        if (!compound.contains("Line" + line) || !compound.contains(ENTRY_KEY + line))
            return;
        DisplayLinkBlockEntity be = context.blockEntity();
        if (!BlockPos.of(compound.getLong("Line" + line)).equals(be.getBlockPos()))
            return; // another link's reservation: Create handles it
        MultiLink link = MultiLink.of(be);
        int owner = compound.getInt(ENTRY_KEY + line);
        int self = Math.max(0, link.dlp$activeEntry());
        if (owner == self || owner >= link.dlp$entryCount())
            return; // own or stale reservation: Create clears it
        // Only valid while the owning entry still writes to this display
        if (EntryOps.displayOf(be, owner).equals(target.getBlockPos()))
            cir.setReturnValue(true);
    }
}
