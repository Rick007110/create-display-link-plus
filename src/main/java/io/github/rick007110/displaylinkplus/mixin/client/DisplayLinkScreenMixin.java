package io.github.rick007110.displaylinkplus.mixin.client;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkConfigurationPacket;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkScreen;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.ModularGuiLine;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import java.util.List;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import io.github.rick007110.displaylinkplus.DlpConfig;
import io.github.rick007110.displaylinkplus.client.EntryColors;
import io.github.rick007110.displaylinkplus.link.DisplayLinkEntryView;
import io.github.rick007110.displaylinkplus.link.EntryOps;
import io.github.rick007110.displaylinkplus.link.LinkEntry;
import io.github.rick007110.displaylinkplus.link.MultiLink;
import io.github.rick007110.displaylinkplus.network.ConfigureEntryPacket;
import io.github.rick007110.displaylinkplus.network.ModifyEntriesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds entry tabs (left of the window) and add/remove buttons (in the empty left strip) to Create's Display Link
 * screen. Each entry is edited in a fresh, unmodified DisplayLinkScreen: entry 0 on the real block entity, the others
 * on a {@link DisplayLinkEntryView}.
 */
@Mixin(DisplayLinkScreen.class)
public abstract class DisplayLinkScreenMixin extends AbstractSimiScreen {
    @Shadow
    private DisplayLinkBlockEntity blockEntity;
    @Shadow
    List<DisplaySource> sources;
    @Shadow
    ScrollInput sourceTypeSelector;
    @Shadow
    ScrollInput targetLineSelector;
    @Shadow
    Couple<ModularGuiLine> configWidgets;

    protected DisplayLinkScreenMixin() {
        super();
    }

    @Unique
    private DisplayLinkBlockEntity dlp$link() {
        return blockEntity instanceof DisplayLinkEntryView view ? view.link : blockEntity;
    }

    @Unique
    private int dlp$index() {
        return blockEntity instanceof DisplayLinkEntryView view ? view.entryIndex : 0;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void dlp$addEntryWidgets(CallbackInfo ci) {
        DisplayLinkBlockEntity link = dlp$link();
        MultiLink multiLink = MultiLink.of(link);
        int count = multiLink.dlp$entryCount();
        int current = dlp$index();
        if (current >= count) // entry vanished through a sync while switching
            return;
        int x = guiLeft;
        int y = guiTop;

        for (int i = 0; i < count; i++) {
            int index = i;
            IconButton tab = new IconButton(x - 20, y + 1 + i * 20, dlp$numberIcon(i, !EntryOps.conflicts(link, i).isEmpty()));
            tab.green = i == current;
            dlp$entryTooltip(tab, multiLink, i);
            if (i != current)
                tab.withCallback(() -> dlp$switchTo(index, true));
            addRenderableWidget(tab);
        }

        IconButton add = new IconButton(x + 5, y + 18, AllIcons.I_ADD);
        add.setToolTip(Component.translatable("create_displaylinkplus.gui.add"));
        add.getToolTip().add(Component.translatable("create_displaylinkplus.gui.add.hint").withStyle(ChatFormatting.GRAY));
        add.getToolTip().add(Component.translatable("create_displaylinkplus.gui.add.hint_other").withStyle(ChatFormatting.DARK_GRAY));
        add.active = EntryOps.canAdd(link);
        if (!add.active)
            add.getToolTip().add(Component.translatable("create_displaylinkplus.gui.full", DlpConfig.maxEntries()).withStyle(ChatFormatting.RED));
        add.withCallback(() -> {
            dlp$saveCurrent();
            PacketDistributor.sendToServer(new ModifyEntriesPacket(link.getBlockPos(), true, current));
            int added = EntryOps.addCopy(link, current);
            if (added != -1)
                dlp$switchTo(added, false);
        });
        addRenderableWidget(add);

        IconButton remove = new IconButton(x + 5, y + 111, AllIcons.I_TRASH);
        remove.setToolTip(Component.translatable("create_displaylinkplus.gui.remove", current + 1));
        remove.active = count > 1;
        if (!remove.active)
            remove.getToolTip().add(Component.translatable("create_displaylinkplus.gui.remove.last").withStyle(ChatFormatting.GRAY));
        remove.withCallback(() -> {
            PacketDistributor.sendToServer(new ModifyEntriesPacket(link.getBlockPos(), false, current));
            if (multiLink.dlp$removeEntry(current))
                dlp$switchTo(Math.max(0, current - 1), false);
        });
        addRenderableWidget(remove);
    }

    /** Create's onClose only knows entry 0; extra entries go through our packet instead. */
    @Inject(method = "onClose", at = @At("HEAD"), cancellable = true)
    private void dlp$closeExtraEntry(CallbackInfo ci) {
        if (!(blockEntity instanceof DisplayLinkEntryView))
            return;
        super.onClose();
        dlp$saveCurrent();
        ci.cancel();
    }

    @Unique
    private void dlp$switchTo(int index, boolean save) {
        if (save)
            dlp$saveCurrent();
        DisplayLinkBlockEntity link = dlp$link();
        if (index >= MultiLink.of(link).dlp$entryCount())
            index = 0;
        minecraft.setScreen(new DisplayLinkScreen(index == 0 ? link : new DisplayLinkEntryView(link, index)));
    }

    /**
     * Sends the current GUI state to the server (same data Create's onClose builds) and applies it to the client's
     * copy right away, so reopening the entry before the server syncs shows the edit.
     */
    @Unique
    private void dlp$saveCurrent() {
        CompoundTag sourceData = new CompoundTag();
        if (!sources.isEmpty()) {
            DisplaySource source = sources.get(sourceTypeSelector == null ? 0 : sourceTypeSelector.getState());
            ResourceLocation id = CreateBuiltInRegistries.DISPLAY_SOURCE.getKey(source);
            if (id != null)
                sourceData.putString("Id", id.toString());
            configWidgets.forEach(s -> s.saveValues(sourceData));
        }
        int line = targetLineSelector == null ? 0 : targetLineSelector.getState();

        DisplayLinkBlockEntity link = dlp$link();
        int index = dlp$index();
        BlockPos pos = link.getBlockPos();
        if (index == 0)
            CatnipServices.NETWORK.sendToServer(new DisplayLinkConfigurationPacket(pos, sourceData, line));
        else
            PacketDistributor.sendToServer(new ConfigureEntryPacket(pos, index, sourceData, line));

        MultiLink multiLink = MultiLink.of(link);
        if (index < multiLink.dlp$entryCount())
            multiLink.dlp$runWithEntry(index, () -> EntryOps.applyConfig(link, sourceData.copy(), line));
    }

    @Unique
    private ScreenElement dlp$numberIcon(int index, boolean conflicted) {
        String text = String.valueOf(index + 1);
        int color = EntryColors.of(index);
        return (graphics, x, y) -> {
            graphics.drawString(font, text, x + 8 - font.width(text) / 2, y + 4, color, true);
            if (conflicted)
                graphics.drawString(font, "!", x + 12, y - 1, 0xFF5555, true);
        };
    }

    @Unique
    private void dlp$entryTooltip(IconButton tab, MultiLink link, int index) {
        LinkEntry entry = link.dlp$snapshot(index);
        tab.setToolTip(Component.translatable("create_displaylinkplus.gui.entry", index + 1).withColor(EntryColors.of(index)));
        List<Component> tooltip = tab.getToolTip();
        tooltip.add(entry.activeSource != null
                ? entry.activeSource.getName().copy().withStyle(ChatFormatting.GRAY)
                : Component.translatable("create_displaylinkplus.gui.no_source").withStyle(ChatFormatting.DARK_GRAY));
        if (minecraft == null || minecraft.level == null)
            return;
        BlockPos targetPos = dlp$link().getBlockPos().offset(entry.targetOffset);
        Component targetName = minecraft.level.getBlockState(targetPos).getBlock().getName();
        DisplayTarget target = DisplayTarget.get(minecraft.level, targetPos);
        tooltip.add(target != null
                ? Component.translatable("create_displaylinkplus.gui.target", targetName, target.getLineOptionText(entry.targetLine)).withStyle(ChatFormatting.GRAY)
                : targetName.copy().withStyle(ChatFormatting.DARK_GRAY));
        for (int other : EntryOps.conflicts(dlp$link(), index))
            tooltip.add(Component.translatable("create_displaylinkplus.gui.conflict", other + 1).withStyle(ChatFormatting.RED));
    }
}
