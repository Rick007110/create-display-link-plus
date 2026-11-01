package io.github.rick007110.displaylinkplus.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import io.github.rick007110.displaylinkplus.DisplayLinkPlus;
import io.github.rick007110.displaylinkplus.link.EntryOps;
import io.github.rick007110.displaylinkplus.link.MultiLink;

/** Adds a copy of an entry (next line, same display) or removes an entry. */
public record ModifyEntriesPacket(BlockPos pos, boolean add, int entry) implements CustomPacketPayload {
    public static final Type<ModifyEntriesPacket> TYPE = new Type<>(DisplayLinkPlus.asResource("modify_entries"));
    public static final StreamCodec<ByteBuf, ModifyEntriesPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ModifyEntriesPacket::pos,
            ByteBufCodecs.BOOL, ModifyEntriesPacket::add,
            ByteBufCodecs.VAR_INT, ModifyEntriesPacket::entry,
            ModifyEntriesPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ModifyEntriesPacket packet, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player))
            return;
        DlpPackets.withDisplayLink(player, packet.pos, be -> {
            MultiLink link = MultiLink.of(be);
            if (packet.add) {
                int added = EntryOps.addCopy(be, packet.entry);
                if (added == -1)
                    return;
                link.dlp$runWithEntry(added, be::updateGatheredData);
            } else if (!link.dlp$removeEntry(packet.entry)) {
                return;
            }
            be.notifyUpdate();
        });
    }
}
