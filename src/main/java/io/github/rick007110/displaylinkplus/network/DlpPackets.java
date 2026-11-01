package io.github.rick007110.displaylinkplus.network;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.foundation.utility.AdventureUtil;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class DlpPackets {
    private static final String VERSION = "1";
    /** Same interaction range Create uses for its block entity configuration packets. */
    private static final double MAX_RANGE = 20;

    private DlpPackets() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToServer(ConfigureEntryPacket.TYPE, ConfigureEntryPacket.STREAM_CODEC, ConfigureEntryPacket::handle);
        registrar.playToServer(ModifyEntriesPacket.TYPE, ModifyEntriesPacket.STREAM_CODEC, ModifyEntriesPacket::handle);
    }

    /** Mirrors Create's BlockEntityConfigurationPacket checks, then runs the action on the display link. */
    static void withDisplayLink(ServerPlayer player, BlockPos pos, Consumer<DisplayLinkBlockEntity> action) {
        if (player == null || player.isSpectator() || AdventureUtil.isAdventure(player))
            return;
        Level level = player.level();
        if (!level.isLoaded(pos) || !player.canInteractWithBlock(pos, MAX_RANGE))
            return;
        if (level.getBlockEntity(pos) instanceof DisplayLinkBlockEntity be)
            action.accept(be);
    }
}
