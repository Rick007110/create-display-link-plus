package io.github.rick007110.displaylinkplus.mixin;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.content.trains.display.FlapDisplayBlockEntity;
import com.simibubi.create.content.trains.display.FlapDisplayLayout;
import com.simibubi.create.content.trains.display.FlapDisplaySection;
import java.util.List;
import net.minecraft.network.chat.MutableComponent;
import io.github.rick007110.displaylinkplus.link.LabelTemplate;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Create always writes "label value". When the label contains {@code {}}, write the label with the value in its
 * place instead, on signs/lecterns and as a single full-width section on Display Boards.
 */
@Mixin(SingleLineDisplaySource.class)
public abstract class SingleLineDisplaySourceMixin extends DisplaySource {
    @Shadow
    protected abstract MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats);

    @Shadow
    protected abstract boolean allowsLabeling(DisplayLinkContext context);

    @Shadow
    protected abstract String getFlapDisplayLayoutName(DisplayLinkContext context);

    @Unique
    @Nullable
    private String dlp$template(DisplayLinkContext context) {
        if (!allowsLabeling(context))
            return null;
        return LabelTemplate.templateOf(context.sourceConfig().getString("Label"));
    }

    @Inject(method = "provideText", at = @At("HEAD"), cancellable = true)
    private void dlp$templateText(DisplayLinkContext context, DisplayTargetStats stats, CallbackInfoReturnable<List<MutableComponent>> cir) {
        String template = dlp$template(context);
        if (template == null)
            return;
        MutableComponent value = provideLine(context, stats);
        cir.setReturnValue(value == EMPTY_LINE ? EMPTY : ImmutableList.of(LabelTemplate.fill(template, value)));
    }

    @Inject(method = "provideFlapDisplayText", at = @At("HEAD"), cancellable = true)
    private void dlp$templateFlapText(DisplayLinkContext context, DisplayTargetStats stats, CallbackInfoReturnable<List<List<MutableComponent>>> cir) {
        String template = dlp$template(context);
        if (template == null)
            return;
        MutableComponent value = provideLine(context, stats);
        MutableComponent text = value == EMPTY_LINE ? EMPTY_LINE : LabelTemplate.fill(template, value);
        cir.setReturnValue(ImmutableList.of(ImmutableList.of(text)));
    }

    @Inject(method = "loadFlapDisplayLayout", at = @At("HEAD"), cancellable = true)
    private void dlp$templateFlapLayout(DisplayLinkContext context, FlapDisplayBlockEntity flapDisplay, FlapDisplayLayout layout, CallbackInfo ci) {
        if (dlp$template(context) == null)
            return;
        String layoutKey = "Template_" + getFlapDisplayLayoutName(context);
        if (!layout.isLayout(layoutKey))
            layout.configure(layoutKey, ImmutableList.of(new FlapDisplaySection(flapDisplay.getMaxCharCount() * 7.0F, "alphabet", false, false)));
        ci.cancel();
    }
}
