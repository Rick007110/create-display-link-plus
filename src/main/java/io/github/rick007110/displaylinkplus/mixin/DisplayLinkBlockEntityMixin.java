package io.github.rick007110.displaylinkplus.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import io.github.rick007110.displaylinkplus.DisplayLinkPlus;
import io.github.rick007110.displaylinkplus.link.LinkEntry;
import io.github.rick007110.displaylinkplus.link.MultiLink;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisplayLinkBlockEntity.class)
public abstract class DisplayLinkBlockEntityMixin implements MultiLink {
    @Shadow
    protected BlockPos targetOffset;
    @Shadow
    public DisplaySource activeSource;
    @Shadow
    private CompoundTag sourceConfig;
    @Shadow
    public DisplayTarget activeTarget;
    @Shadow
    public int targetLine;
    @Shadow
    public int refreshTicks;

    @Unique
    private final List<LinkEntry> dlp$extra = new ArrayList<>();
    @Unique
    private int dlp$active = -1;

    @Unique
    private DisplayLinkBlockEntity dlp$self() {
        return (DisplayLinkBlockEntity) (Object) this;
    }

    // ---- MultiLink ----

    @Override
    public List<LinkEntry> dlp$extraEntries() {
        return dlp$extra;
    }

    @Override
    public int dlp$activeEntry() {
        return dlp$active;
    }

    @Override
    public void dlp$runWithEntry(int index, Runnable action) {
        if (dlp$active != -1) {
            action.run();
            return;
        }
        LinkEntry entry = index > 0 ? dlp$extra.get(index - 1) : null;
        dlp$active = index;
        if (entry != null)
            dlp$swap(entry);
        try {
            action.run();
        } finally {
            if (entry != null)
                dlp$swap(entry);
            dlp$active = -1;
        }
    }

    @Override
    public LinkEntry dlp$snapshot(int index) {
        // While entry N is swapped in, Create's fields hold entry N and slot N-1 of the list holds entry 0
        int inFields = Math.max(dlp$active, 0);
        if (index == inFields)
            return new LinkEntry(targetOffset, activeSource, sourceConfig.copy(), activeTarget, targetLine);
        return dlp$extra.get((index == 0 ? inFields : index) - 1).copy();
    }

    @Override
    public boolean dlp$removeEntry(int index) {
        if (dlp$extra.isEmpty() || index < 0 || index > dlp$extra.size())
            return false;
        if (index > 0) {
            dlp$extra.remove(index - 1);
            return true;
        }
        LinkEntry promoted = dlp$extra.remove(0);
        targetOffset = promoted.targetOffset;
        activeSource = promoted.activeSource;
        sourceConfig = promoted.sourceConfig;
        activeTarget = promoted.activeTarget;
        targetLine = promoted.targetLine;
        refreshTicks = promoted.refreshTicks;
        return true;
    }

    /** Exchanges Create's per-link fields with the entry's. Calling it twice restores both. */
    @Unique
    private void dlp$swap(LinkEntry entry) {
        BlockPos offset = targetOffset;
        targetOffset = entry.targetOffset;
        entry.targetOffset = offset;

        DisplaySource source = activeSource;
        activeSource = entry.activeSource;
        entry.activeSource = source;

        CompoundTag config = sourceConfig;
        sourceConfig = entry.sourceConfig;
        entry.sourceConfig = config;

        DisplayTarget target = activeTarget;
        activeTarget = entry.activeTarget;
        entry.activeTarget = target;

        int line = targetLine;
        targetLine = entry.targetLine;
        entry.targetLine = line;

        int ticks = refreshTicks;
        refreshTicks = entry.refreshTicks;
        entry.refreshTicks = ticks;
    }

    // ---- Runtime ----

    /** Outside callers (source changed, redstone, config packet for entry 0) update every entry. */
    @WrapMethod(method = "updateGatheredData")
    private void dlp$updateAllEntries(Operation<Void> original) {
        if (dlp$active != -1) {
            original.call();
            return;
        }
        dlp$runWithEntry(0, original::call);
        for (int i = 1; i <= dlp$extra.size(); i++) {
            try {
                dlp$runWithEntry(i, original::call);
            } catch (Exception e) {
                DisplayLinkPlus.LOGGER.error("Failed to update display link entry {} at {}", i, dlp$self().getBlockPos(), e);
            }
        }
    }

    /** Create's passive refresh belongs to entry 0 only; extra entries refresh on their own timer below. */
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/redstone/displayLink/DisplayLinkBlockEntity;tickSource()V"))
    private void dlp$passiveRefreshEntryZero(DisplayLinkBlockEntity be, Operation<Void> original) {
        dlp$runWithEntry(0, () -> original.call(be));
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void dlp$tickExtraEntries(CallbackInfo ci) {
        DisplayLinkBlockEntity self = dlp$self();
        if (self.isVirtual() || self.getLevel() == null || self.getLevel().isClientSide)
            return;
        for (int i = 1; i <= dlp$extra.size(); i++) {
            LinkEntry entry = dlp$extra.get(i - 1);
            if (entry.activeSource == null)
                continue;
            entry.refreshTicks++;
            if (entry.refreshTicks < entry.activeSource.getPassiveRefreshTicks() || !entry.activeSource.shouldPassiveReset())
                continue;
            try {
                dlp$runWithEntry(i, self::tickSource);
            } catch (Exception e) {
                entry.refreshTicks = 0;
                DisplayLinkPlus.LOGGER.error("Failed to refresh display link entry {} at {}", i, self.getBlockPos(), e);
            }
        }
    }

    @Inject(method = "tickSource", at = @At("HEAD"))
    private void dlp$resetAllTimers(CallbackInfo ci) {
        if (dlp$active == -1)
            dlp$extra.forEach(e -> e.refreshTicks = 0);
    }

    @Inject(method = "onNoLongerPowered", at = @At("HEAD"))
    private void dlp$signalResetExtraEntries(CallbackInfo ci) {
        DisplayLinkBlockEntity self = dlp$self();
        for (int i = 1; i <= dlp$extra.size(); i++) {
            if (dlp$extra.get(i - 1).activeSource == null)
                continue;
            dlp$runWithEntry(i, () -> {
                refreshTicks = 0;
                activeSource.onSignalReset(new DisplayLinkContext(self.getLevel(), self));
            });
        }
        // Create only refreshes when entry 0 has a source; make sure the other entries still update
        if (activeSource == null && !dlp$extra.isEmpty())
            self.updateGatheredData();
    }

    // ---- Persistence ----

    @Inject(method = "write", at = @At("RETURN"))
    private void dlp$writeEntries(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        dlp$writeEntries(tag, clientPacket);
    }

    @Inject(method = "writeSafe", at = @At("RETURN"))
    private void dlp$writeEntriesSafe(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        dlp$writeEntries(tag, false);
    }

    @Unique
    private void dlp$writeEntries(CompoundTag tag, boolean clientPacket) {
        if (dlp$extra.isEmpty())
            return;
        ListTag list = new ListTag();
        for (LinkEntry entry : dlp$extra)
            list.add(entry.write(clientPacket));
        tag.put(NBT_KEY, list);
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void dlp$readEntries(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        dlp$extra.clear();
        for (Tag t : tag.getList(NBT_KEY, Tag.TAG_COMPOUND))
            dlp$extra.add(LinkEntry.read((CompoundTag) t, clientPacket));
    }

    @Inject(method = "transform", at = @At("RETURN"))
    private void dlp$transformEntries(BlockEntity be, StructureTransform transform, CallbackInfo ci) {
        for (LinkEntry entry : dlp$extra)
            entry.targetOffset = transform.applyWithoutOffset(entry.targetOffset);
    }
}
