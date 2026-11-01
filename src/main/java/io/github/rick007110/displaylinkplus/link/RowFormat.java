package io.github.rick007110.displaylinkplus.link;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.trains.display.FlapDisplayBlockEntity;
import com.simibubi.create.content.trains.display.FlapDisplayLayout;
import com.simibubi.create.content.trains.display.FlapDisplaySection;
import org.jetbrains.annotations.Nullable;

/** The optional row format of list sources, stored in the source config next to Create's own settings. */
public final class RowFormat {
    public static final String CONFIG_KEY = "RowFormat";
    private static final String LAYOUT_KEY = "DlpRowFormat";

    private RowFormat() {
    }

    /** The row format, or null if none is set (Create's own layout is used). */
    @Nullable
    public static String of(DisplayLinkContext context) {
        String format = context.sourceConfig().getString(CONFIG_KEY);
        return LabelTemplate.hasPlaceholder(format) ? format : null;
    }

    /** One full-width text section per row. Returns false (leave it to Create) when no row format is set. */
    public static boolean applyLayout(DisplayLinkContext context, FlapDisplayBlockEntity flapDisplay, FlapDisplayLayout layout) {
        if (of(context) == null)
            return false;
        if (!layout.isLayout(LAYOUT_KEY))
            layout.configure(LAYOUT_KEY, ImmutableList.of(new FlapDisplaySection(flapDisplay.getMaxCharCount() * 7.0F, "alphabet", false, false)));
        return true;
    }
}
