package io.github.rick007110.displaylinkplus.mixin;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.FluidListDisplaySource;
import com.simibubi.create.content.redstone.displayLink.source.ValueListDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.content.trains.display.FlapDisplayBlockEntity;
import com.simibubi.create.content.trains.display.FlapDisplayLayout;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.foundation.utility.FluidFormatter;
import java.util.List;
import java.util.stream.Stream;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.IntAttached;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import io.github.rick007110.displaylinkplus.link.LabelTemplate;
import io.github.rick007110.displaylinkplus.link.RowFormat;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * List sources (List matching Items/Fluids, Factory Gauge status, Scoreboard) get an optional row format such as
 * "{name}: {count}". Each row is then one piece of text, and one full-width section on Display Boards.
 */
@Mixin(ValueListDisplaySource.class)
public abstract class ValueListDisplaySourceMixin extends DisplaySource {
    @Shadow
    protected abstract Stream<IntAttached<MutableComponent>> provideEntries(DisplayLinkContext context, int maxRows);

    @Shadow
    protected abstract boolean shortenNumbers(DisplayLinkContext context);

    @Shadow
    private List<MutableComponent> condensePages(List<MutableComponent> list) {
        throw new AssertionError();
    }

    @Inject(method = "provideText", at = @At("HEAD"), cancellable = true)
    private void dlp$formattedRows(DisplayLinkContext context, DisplayTargetStats stats, CallbackInfoReturnable<List<MutableComponent>> cir) {
        String format = RowFormat.of(context);
        if (format == null)
            return;
        boolean isBook = context.getTargetBlockEntity() instanceof LecternBlockEntity;
        List<MutableComponent> rows = provideEntries(context, stats.maxRows() * (isBook ? 8 : 1))
                .map(entry -> dlp$row(context, format, entry))
                .toList();
        cir.setReturnValue(isBook ? condensePages(rows) : rows);
    }

    @Inject(method = "provideFlapDisplayText", at = @At("HEAD"), cancellable = true)
    private void dlp$formattedFlapRows(DisplayLinkContext context, DisplayTargetStats stats, CallbackInfoReturnable<List<List<MutableComponent>>> cir) {
        String format = RowFormat.of(context);
        if (format == null)
            return;
        cir.setReturnValue(provideEntries(context, stats.maxRows())
                .map(entry -> (List<MutableComponent>) ImmutableList.of(dlp$row(context, format, entry)))
                .toList());
    }

    @Inject(method = "loadFlapDisplayLayout", at = @At("HEAD"), cancellable = true)
    private void dlp$formattedFlapLayout(DisplayLinkContext context, FlapDisplayBlockEntity flapDisplay, FlapDisplayLayout layout, CallbackInfo ci) {
        if (RowFormat.applyLayout(context, flapDisplay, layout))
            ci.cancel();
    }

    @Unique
    private MutableComponent dlp$row(DisplayLinkContext context, String format, IntAttached<MutableComponent> entry) {
        int amount = entry.getFirst();
        boolean shorten = shortenNumbers(context);
        MutableComponent count;
        @Nullable MutableComponent unit = null;
        if ((Object) this instanceof FluidListDisplaySource) {
            Couple<MutableComponent> formatted = FluidFormatter.asComponents(amount, shorten);
            count = formatted.getFirst().copy().append(" ").append(formatted.getSecond());
            unit = FluidFormatter.asComponents(0, false).getSecond();
        } else {
            count = shorten ? dlp$shortened(amount) : Component.literal(String.valueOf(amount));
        }
        return LabelTemplate.fillRow(format, entry.getSecond(), count, amount, unit);
    }

    /** Same output as Create's private ValueListDisplaySource#shorten. */
    @Unique
    private static MutableComponent dlp$shortened(int number) {
        if (number >= 1000000)
            return Component.literal(String.valueOf(number / 1000000)).append(CreateLang.translateDirect("display_source.value_list.million"));
        if (number >= 1000)
            return Component.literal(String.valueOf(number / 1000)).append(CreateLang.translateDirect("display_source.value_list.thousand"));
        return Component.literal(String.valueOf(number));
    }
}
