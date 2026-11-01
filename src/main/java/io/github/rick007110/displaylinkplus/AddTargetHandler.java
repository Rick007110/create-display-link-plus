package io.github.rick007110.displaylinkplus;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.redstone.displayLink.ClickToLinkBlockItem.ClickToLinkData;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockItem;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import io.github.rick007110.displaylinkplus.link.EntryOps;
import io.github.rick007110.displaylinkplus.link.LinkEntry;
import io.github.rick007110.displaylinkplus.link.MultiLink;

/**
 * Select a display with a Display Link item as usual, then right-click an existing Display Link with that item:
 * instead of opening its GUI, the display is added to that link as a new entry.
 */
@EventBusSubscriber(modid = DisplayLinkPlus.MOD_ID)
public final class AddTargetHandler {
    private AddTargetHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (!(stack.getItem() instanceof DisplayLinkBlockItem) || !stack.has(AllDataComponents.CLICK_TO_LINK_DATA))
            return;
        // Sneaking keeps Create's behaviour (clears the selection)
        if (player.isShiftKeyDown() || !AllBlocks.DISPLAY_LINK.has(level.getBlockState(pos)))
            return;
        if (!(level.getBlockEntity(pos) instanceof DisplayLinkBlockEntity be))
            return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (level.isClientSide)
            return;

        ClickToLinkData data = stack.get(AllDataComponents.CLICK_TO_LINK_DATA);
        BlockPos selected = data.selectedPos();
        if (selected.equals(pos))
            return;
        int range = AllConfigs.server().logistics.displayLinkRange.get();
        if (!data.selectedDim().equals(level.dimension().location()) || !selected.closerThan(pos, range)) {
            player.displayClientMessage(Component.translatable("create_displaylinkplus.add_target.too_far").withStyle(ChatFormatting.RED), true);
            return;
        }

        MultiLink link = MultiLink.of(be);
        if (be.getTargetPosition().equals(pos)) {
            // Link without any target yet: the selection becomes entry 0
            be.target(selected);
            be.updateGatheredData();
        } else if (EntryOps.canAdd(be)) {
            // New entries start with entry 0's source settings so the display shows something right away
            LinkEntry first = link.dlp$snapshot(0);
            link.dlp$extraEntries().add(new LinkEntry(selected.subtract(pos), first.activeSource, first.sourceConfig, null, 0));
            link.dlp$runWithEntry(link.dlp$entryCount() - 1, be::updateGatheredData);
        } else {
            player.displayClientMessage(Component.translatable("create_displaylinkplus.add_target.full", link.dlp$entryCount())
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        be.notifyUpdate();
        stack.remove(AllDataComponents.CLICK_TO_LINK_DATA);
        player.displayClientMessage(Component.translatable("create_displaylinkplus.add_target.success", link.dlp$entryCount())
                .withStyle(ChatFormatting.GREEN), true);
    }
}
