package io.github.rick007110.displaylinkplus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import io.github.rick007110.displaylinkplus.DisplayLinkPlus;
import io.github.rick007110.displaylinkplus.link.LinkEntry;
import io.github.rick007110.displaylinkplus.link.MultiLink;

/**
 * Looking at a Display Link while holding a wrench, Display Link or clipboard (or wearing goggles) outlines every
 * target of every entry, draws a line to it, and labels it with the entry number and line.
 */
@EventBusSubscriber(modid = DisplayLinkPlus.MOD_ID, value = Dist.CLIENT)
public final class TargetOverlay {
    private record Label(Vec3 pos, Component text, int color) {
    }

    private static final List<Label> LABELS = new ArrayList<>();

    private TargetOverlay() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        LABELS.clear();
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null || mc.screen != null || !shouldShow(player))
            return;
        if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK)
            return;
        BlockPos linkPos = hit.getBlockPos();
        if (!AllBlocks.DISPLAY_LINK.has(level.getBlockState(linkPos)))
            return;
        if (!(level.getBlockEntity(linkPos) instanceof DisplayLinkBlockEntity be))
            return;

        MultiLink link = MultiLink.of(be);
        Vec3 from = Vec3.atCenterOf(linkPos);
        // Several entries can share one display; stack their labels instead of drawing them on top of each other
        Map<BlockPos, Integer> labelsPerTarget = new HashMap<>();
        for (int i = 0; i < link.dlp$entryCount(); i++) {
            LinkEntry entry = link.dlp$snapshot(i);
            if (entry.targetOffset.equals(BlockPos.ZERO))
                continue;
            BlockPos targetPos = linkPos.offset(entry.targetOffset);
            DisplayTarget target = DisplayTarget.get(level, targetPos);
            AABB bounds = target != null ? target.getMultiblockBounds(level, targetPos) : new AABB(targetPos);
            int color = EntryColors.of(i);

            Outliner.getInstance().showAABB("dlp_target_" + i, bounds.inflate(1 / 64f)).colored(color).lineWidth(1 / 16f);
            Outliner.getInstance().showLine("dlp_line_" + i, from, bounds.getCenter()).colored(color).lineWidth(1 / 32f);

            int stacked = labelsPerTarget.merge(targetPos, 1, Integer::sum) - 1;
            Component lineText = target != null ? target.getLineOptionText(entry.targetLine) : Component.empty();
            Component text = Component.translatable("create_displaylinkplus.overlay.label", i + 1, lineText);
            LABELS.add(new Label(new Vec3(bounds.getCenter().x, bounds.maxY + 0.3 + stacked * 0.3, bounds.getCenter().z), text, color));
        }
    }

    private static boolean shouldShow(LocalPlayer player) {
        ItemStack held = player.getMainHandItem();
        return AllItems.WRENCH.isIn(held)
                || held.getItem() instanceof DisplayLinkBlockItem
                || AllBlocks.CLIPBOARD.isIn(held)
                || GogglesItem.isWearingGoggles(player);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || LABELS.isEmpty())
            return;
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack ms = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        for (Label label : LABELS) {
            ms.pushPose();
            ms.translate(label.pos.x - camera.x, label.pos.y - camera.y, label.pos.z - camera.z);
            ms.mulPose(event.getCamera().rotation());
            ms.scale(0.025f, -0.025f, 0.025f);
            float x = -font.width(label.text) / 2f;
            font.drawInBatch(label.text, x, 0, 0xFF000000 | label.color, false, ms.last().pose(), buffer,
                    Font.DisplayMode.SEE_THROUGH, 0x60000000, LightTexture.FULL_BRIGHT);
            ms.popPose();
        }
        buffer.endBatch();
    }
}
