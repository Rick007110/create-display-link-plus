package io.github.rick007110.displaylinkplus.mixin.client;

import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.foundation.gui.widget.TooltipArea;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Mentions the {} placeholder in the label box tooltip and leaves room for longer template labels. */
@Mixin(SingleLineDisplaySource.class)
public abstract class SingleLineDisplaySourceClientMixin {
    @Unique
    private static final int MAX_LABEL_LENGTH = 64;

    @ModifyArg(method = "addLabelingTextBox", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/foundation/gui/ModularGuiLineBuilder;addTextInput(IILjava/util/function/BiConsumer;Ljava/lang/String;)Lcom/simibubi/create/foundation/gui/ModularGuiLineBuilder;"),
            index = 2)
    private BiConsumer<EditBox, TooltipArea> dlp$addTemplateHint(BiConsumer<EditBox, TooltipArea> original) {
        return (box, area) -> {
            original.accept(box, area);
            box.setMaxLength(MAX_LABEL_LENGTH);
            List<Component> tooltip = new ArrayList<>(area.getToolTip());
            tooltip.add(Component.translatable("create_displaylinkplus.gui.label_template").withStyle(ChatFormatting.GRAY));
            area.withTooltip(tooltip);
        };
    }
}
