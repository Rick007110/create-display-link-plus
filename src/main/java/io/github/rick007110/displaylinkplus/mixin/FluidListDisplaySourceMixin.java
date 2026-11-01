package io.github.rick007110.displaylinkplus.mixin;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.FluidListDisplaySource;
import com.simibubi.create.content.trains.display.FlapDisplayBlockEntity;
import com.simibubi.create.content.trains.display.FlapDisplayLayout;
import io.github.rick007110.displaylinkplus.link.RowFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The fluid list overrides the board layout itself, so it needs its own row format hook. */
@Mixin(FluidListDisplaySource.class)
public abstract class FluidListDisplaySourceMixin {
    @Inject(method = "loadFlapDisplayLayout", at = @At("HEAD"), cancellable = true)
    private void dlp$formattedFlapLayout(DisplayLinkContext context, FlapDisplayBlockEntity flapDisplay, FlapDisplayLayout layout, CallbackInfo ci) {
        if (RowFormat.applyLayout(context, flapDisplay, layout))
            ci.cancel();
    }
}
