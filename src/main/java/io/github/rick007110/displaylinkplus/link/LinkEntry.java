package io.github.rick007110.displaylinkplus.link;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * One extra source->target pairing of a Display Link. Mirrors the fields Create keeps on
 * {@code DisplayLinkBlockEntity}; entry 0 is always Create's own fields and never stored here.
 * NBT keys match Create's so the format stays familiar.
 */
public class LinkEntry {
    public BlockPos targetOffset = BlockPos.ZERO;
    @Nullable
    public DisplaySource activeSource;
    public CompoundTag sourceConfig = new CompoundTag();
    @Nullable
    public DisplayTarget activeTarget;
    public int targetLine;
    public int refreshTicks;

    public LinkEntry() {
    }

    public LinkEntry(BlockPos targetOffset, @Nullable DisplaySource activeSource, CompoundTag sourceConfig,
                     @Nullable DisplayTarget activeTarget, int targetLine) {
        this.targetOffset = targetOffset;
        this.activeSource = activeSource;
        this.sourceConfig = sourceConfig;
        this.activeTarget = activeTarget;
        this.targetLine = targetLine;
    }

    public LinkEntry copy() {
        return new LinkEntry(targetOffset, activeSource, sourceConfig.copy(), activeTarget, targetLine);
    }

    public CompoundTag write(boolean clientPacket) {
        CompoundTag tag = new CompoundTag();
        tag.put("TargetOffset", NbtUtils.writeBlockPos(targetOffset));
        tag.putInt("TargetLine", targetLine);
        if (activeSource != null) {
            CompoundTag data = sourceConfig.copy();
            ResourceLocation id = CreateBuiltInRegistries.DISPLAY_SOURCE.getKey(activeSource);
            if (id != null)
                data.putString("Id", id.toString());
            tag.put("Source", data);
        }
        if (clientPacket && activeTarget != null) {
            ResourceLocation id = CreateBuiltInRegistries.DISPLAY_TARGET.getKey(activeTarget);
            if (id != null)
                tag.putString("TargetType", id.toString());
        }
        return tag;
    }

    public static LinkEntry read(CompoundTag tag, boolean clientPacket) {
        LinkEntry entry = new LinkEntry();
        entry.targetOffset = NBTHelper.readBlockPos(tag, "TargetOffset");
        entry.targetLine = tag.getInt("TargetLine");
        if (clientPacket && tag.contains("TargetType"))
            entry.activeTarget = DisplayTarget.get(ResourceLocation.tryParse(tag.getString("TargetType")));
        if (tag.contains("Source")) {
            CompoundTag data = tag.getCompound("Source");
            entry.activeSource = DisplaySource.get(ResourceLocation.tryParse(data.getString("Id")));
            if (entry.activeSource != null)
                entry.sourceConfig = data.copy();
        }
        return entry;
    }
}
