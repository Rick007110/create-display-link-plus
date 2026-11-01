package io.github.rick007110.displaylinkplus.mixin.client;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.ScoreboardDisplaySource;
import com.simibubi.create.content.redstone.displayLink.source.ValueListDisplaySource;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import io.github.rick007110.displaylinkplus.link.RowFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the "Row format" text box to the second config line of list sources. Item/fluid lists and the factory gauge
 * leave that line empty; the scoreboard uses its left part for the number format, so the box goes to the right.
 */
@Mixin({ValueListDisplaySource.class, ScoreboardDisplaySource.class})
public abstract class ValueListConfigMixin {
    @Inject(method = "initConfigurationWidgets", at = @At("TAIL"))
    private void dlp$addRowFormatInput(DisplayLinkContext context, ModularGuiLineBuilder builder, boolean isFirstLine, CallbackInfo ci) {
        if (isFirstLine)
            return;
        boolean scoreboard = (Object) this instanceof ScoreboardDisplaySource;
        builder.addTextInput(scoreboard ? 78 : 0, scoreboard ? 59 : 137, (box, tooltip) -> {
            box.setValue("");
            box.setMaxLength(64);
            tooltip.withTooltip(List.of(
                    Component.translatable("create_displaylinkplus.gui.row_format").withColor(0x5391E1),
                    Component.translatable("create_displaylinkplus.gui.row_format.hint").withStyle(ChatFormatting.GRAY),
                    Component.translatable("create_displaylinkplus.gui.row_format.formats").withStyle(ChatFormatting.GRAY),
                    Component.translatable("create.gui.schedule.lmb_edit").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)));
        }, RowFormat.CONFIG_KEY);
    }
}
