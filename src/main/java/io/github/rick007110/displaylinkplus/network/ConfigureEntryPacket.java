package io.github.rick007110.displaylinkplus.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import io.github.rick007110.displaylinkplus.DisplayLinkPlus;
import io.github.rick007110.displaylinkplus.link.EntryOps;
import io.github.rick007110.displaylinkplus.link.MultiLink;

/** The GUI result for one entry: Create's DisplayLinkConfigurationPacket, plus an entry index. */
public record ConfigureEntryPacket(BlockPos pos, int entry, CompoundTag configData, int targetLine) implements CustomPacketPayload {
    public static final Type<ConfigureEntryPacket> TYPE = new Type<>(DisplayLinkPlus.asResource("configure_entry"));
    public static final StreamCodec<ByteBuf, ConfigureEntryPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ConfigureEntryPacket::pos,
            ByteBufCodecs.VAR_INT, ConfigureEntryPacket::entry,
            ByteBufCodecs.COMPOUND_TAG, ConfigureEntryPacket::configData,
            ByteBufCodecs.VAR_INT, ConfigureEntryPacket::targetLine,
            ConfigureEntryPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ConfigureEntryPacket packet, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player))
            return;
        DlpPackets.withDisplayLink(player, packet.pos, be -> {
            MultiLink link = MultiLink.of(be);
            if (packet.entry < 0 || packet.entry >= link.dlp$entryCount())
                return;
            link.dlp$runWithEntry(packet.entry, () -> {
                if (EntryOps.applyConfig(be, packet.configData, packet.targetLine))
                    be.updateGatheredData();
            });
            be.notifyUpdate();
        });
    }
}
